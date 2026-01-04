package ws

import (
	"encoding/json"
	"errors"
	"maps"
	"net/http"
	"sfu/internal/logger"

	"github.com/bytedance/sonic"
	"github.com/gorilla/websocket"
	"github.com/pion/rtcp"
	"github.com/pion/webrtc/v4"
)

var Upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

type SignalMessage struct {
	Event string          `json:"event"`
	Data  json.RawMessage `json:"data"`
}

// 客户端向服务器发送的 offer
type UpOfferPayload struct {
	UID string `json:"uid"`
	SDP string `json:"sdp"`
}

// 通用 SDP 载荷 (用于 up_answer 或 down_answer 内部)
type SDPPayload struct {
	SDP string `json:"sdp"`
}

// 客户端向服务器发送的 ICE 候选
type CandidatePayload struct {
	Candidate webrtc.ICECandidateInit `json:"candidate"`
}

// 服务器向客户端发送的 offer 载荷
type DownOfferPayload struct {
	From string `json:"from"` // publisher UID
	SDP  string `json:"sdp"`
}

// 处理用户的 WebSocket 消息
func HandleWS(room *Room, peer *Peer) {
	defer func() {
		cleanupPeer(room, peer)
	}()

	for {
		_, raw, err := peer.WS.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				logger.Log.Sugar().Errorf("读取ws消息失败,uid:%s,err:%v", peer.UID, err)
			}
			return
		}
		var msg SignalMessage
		if err := sonic.Unmarshal(raw, &msg); err != nil {
			logger.Log.Sugar().Errorf("解析信号消息失败,uid:%s,err:%v", peer.UID, err)
			continue
		}

		switch msg.Event {
		case "up_offer":
			var p UpOfferPayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析up_offer数据失败,uid:%s,err:%v", peer.UID, err)
				continue
			}
			go handleUpOffer(room, peer, p.SDP)

		case "up_candidate":
			var p CandidatePayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析up_candidate数据失败,uid:%s,err:%v", peer.UID, err)
				continue
			}
			peer.mu.Lock()
			pc := peer.UpPC
			peer.mu.Unlock()
			if pc != nil {
				_ = pc.AddICECandidate(p.Candidate)
			} else {
				// 防止因为 pc 还没创建而丢失 Candidate
				// 在这里也存入 UpCandidateQueue
				peer.candidateMu.Lock()
				peer.UpCandidateQueue = append(peer.UpCandidateQueue, p.Candidate)
				peer.candidateMu.Unlock()
			}

		case "down_answer":
			var p SDPPayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析down_answer数据失败,uid:%s,err:%v", peer.UID, err)
				continue
			}
			if err := setDownAnswer(peer, p.SDP); err != nil {
				logger.Log.Sugar().Errorf("设置down_answer失败,uid:%s,err:%v", peer.UID, err)
			}

		case "down_candidate":
			var p CandidatePayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析down_candidate数据失败,uid:%s,err:%v", peer.UID, err)
				continue
			}
			peer.mu.Lock()
			pc := peer.DownPC
			peer.mu.Unlock()
			if pc != nil {
				if err := pc.AddICECandidate(p.Candidate); err != nil {
					logger.Log.Sugar().Errorf("添加down_candidate失败,uid:%s,err:%v", peer.UID, err)
				}
			}

		case "subscribe":
		default:
			logger.Log.Sugar().Errorf("未知信号事件:%s,uid:%s", msg.Event, peer.UID)
		}
	}
}

// 处理客户端发送的 offer (client -> server)
func handleUpOffer(room *Room, peer *Peer, sdp string) {
	// 创建 PeerConnection
	api := webrtc.NewAPI()
	pc, err := api.NewPeerConnection(webrtc.Configuration{})
	if err != nil {
		logger.Log.Sugar().Errorf("创建UpPC失败,uid:%s,err:%v", peer.UID, err)
		return
	}

	// 存储 PC
	peer.mu.Lock()
	if peer.UpPC != nil {
		_ = peer.UpPC.Close()
		logger.Log.Sugar().Infof("关闭已存在UpPC,uid:%s", peer.UID)
	}
	peer.UpPC = pc
	peer.mu.Unlock()

	// 设置 ICE 候选回调
	pc.OnICECandidate(func(c *webrtc.ICECandidate) {
		if c == nil {
			return
		}
		sendToWS(peer, "up_candidate", map[string]webrtc.ICECandidateInit{"candidate": c.ToJSON()})
	})

	// 设置媒体轨道回调
	pc.OnTrack(func(remote *webrtc.TrackRemote, receiver *webrtc.RTPReceiver) {
		go func() {
			// 处理来自推流端的 RTCP 包（如接收报告等）
			go func() {
				b := make([]byte, 1500)
				for {
					if _, _, err = receiver.Read(b); err != nil {
						return
					}
				}
			}()

		}()

		// 创建本地 track (TrackLocalStaticRTP)
		capability := remote.Codec().RTPCodecCapability
		var local *webrtc.TrackLocalStaticRTP
		local, err = webrtc.NewTrackLocalStaticRTP(capability, remote.ID(), peer.UID)
		if err != nil {
			logger.Log.Sugar().Errorf("创建TrackLocalStaticRTP失败,uid:%s,err:%v", peer.UID, err)
			return
		}

		// 存储本地 track 到房间
		room.Mu.Lock()
		ut := room.Tracks[peer.UID]
		if ut == nil {
			ut = &PeerTracks{}
			room.Tracks[peer.UID] = ut
		}
		if remote.Kind() == webrtc.RTPCodecTypeAudio {
			ut.Audio = local
		} else if remote.Kind() == webrtc.RTPCodecTypeVideo {
			ut.Video = local
		}
		room.Mu.Unlock()

		//RTP 转发循环 (remote -> local)
		go func() {
			for {
				pkt, _, readErr := remote.ReadRTP()
				if readErr != nil {
					break
				}
				// 只有在确有必要时才修改扩展位
				pkt.Extension = false
				if err = local.WriteRTP(pkt); err != nil {
					break
				}
			}

			room.Mu.Lock()
			if trks, ok := room.Tracks[peer.UID]; ok {
				if remote.Kind() == webrtc.RTPCodecTypeAudio {
					trks.Audio = nil
				} else {
					trks.Video = nil
				}
			}
			room.Mu.Unlock()
		}()

		//将新轨道分发给所有订阅者
		distributeTrackToAllSubscribers(room, peer.UID, local)
	})

	// 设置远端描述并创建 Answer
	offer := webrtc.SessionDescription{
		Type: webrtc.SDPTypeOffer,
		SDP:  sdp,
	}
	if err = pc.SetRemoteDescription(offer); err != nil {
		logger.Log.Sugar().Errorf("设置remote description失败,uid:%s,err:%v", peer.UID, err)
		return
	}
	// 应用缓冲的 ICE Candidate
	go func() {
		peer.candidateMu.Lock()
		candidates := peer.UpCandidateQueue
		peer.UpCandidateQueue = nil // 清空队列
		peer.candidateMu.Unlock()

		for _, cand := range candidates {
			if err = pc.AddICECandidate(cand); err != nil {
				logger.Log.Sugar().Errorf("添加buffered candidate失败,uid:%s,err:%v", peer.UID, err)
			}
		}
	}()
	answer, err := pc.CreateAnswer(nil)
	if err != nil {
		logger.Log.Sugar().Errorf("创建answer失败,uid:%s,err:%v", peer.UID, err)
		return
	}
	if err := pc.SetLocalDescription(answer); err != nil {
		logger.Log.Sugar().Errorf("设置local description for answer失败,uid:%s,err:%v", peer.UID, err)
		return
	}

	// 向客户端发送 answer
	sendToWS(peer, "up_answer", map[string]string{"sdp": answer.SDP})

	go distributeAllExistingTracksToNewSubscriber(room, peer)
}

// 分发所有已存在的轨道给新的订阅者
func distributeAllExistingTracksToNewSubscriber(room *Room, newSubscriber *Peer) {
	room.Mu.RLock()
	tracks := make(map[string]*PeerTracks, len(room.Tracks))
	maps.Copy(tracks, room.Tracks)
	// 获取发布者的 Peer 对象映射
	publishers := make(map[string]*Peer)
	for uid := range tracks {
		publishers[uid] = room.Users[uid]
	}
	room.Mu.RUnlock()

	if err := ensureDownPC(newSubscriber); err != nil {
		return
	}

	newSubscriber.mu.Lock()
	downPC := newSubscriber.DownPC
	newSubscriber.mu.Unlock()
	hasTrack := false

	for pubUID, ut := range tracks {
		if pubUID == newSubscriber.UID {
			continue
		}

		pubPeer := publishers[pubUID]
		if pubPeer == nil {
			continue
		}

		// 处理音频
		if ut.Audio != nil {
			if _, err := downPC.AddTrack(ut.Audio); err == nil {
				hasTrack = true
			}
		}
		// 处理视频
		if ut.Video != nil {
			sender, err := downPC.AddTrack(ut.Video)
			if err == nil {
				// 监听反馈
				monitorRTCP(sender, pubPeer)
				// 新人加入，立即请求一次关键帧防止黑屏
				pubPeer.RequestKeyFrame()
				hasTrack = true
			}
		}
	}
	if hasTrack {
		createAndSendDownOffer(newSubscriber)
	}
}

// 为每个订阅者添加本地 track (local -> downPC)
func distributeTrackToAllSubscribers(room *Room, publisherUID string, track webrtc.TrackLocal) {
	room.Mu.RLock()
	publisher := room.Users[publisherUID]
	peers := make([]*Peer, 0, len(room.Users))
	for _, u := range room.Users {
		peers = append(peers, u)
	}
	room.Mu.RUnlock()

	if publisher == nil {
		return
	}

	for _, u := range peers {
		if u.UID == publisherUID {
			continue
		}
		if err := ensureDownPC(u); err != nil {
			continue
		}

		u.mu.Lock()
		sender, err := u.DownPC.AddTrack(track)
		u.mu.Unlock()

		if err != nil {
			logger.Log.Sugar().Errorf("AddTrack失败: %v", err)
			continue
		}

		monitorRTCP(sender, publisher)

		go createAndSendDownOffer(u)
	}

	// 新轨道产生，让发布者发一个初始关键帧
	publisher.RequestKeyFrame()
}

// 用户确保存在 DownPC（每个订阅者只有一个 DownPC）
func ensureDownPC(peer *Peer) error {
	peer.mu.Lock()
	defer peer.mu.Unlock()

	if peer.DownPC != nil {
		return nil
	}

	api := webrtc.NewAPI()
	pc, err := api.NewPeerConnection(webrtc.Configuration{})
	if err != nil {
		return err
	}
	peer.DownPC = pc

	pc.OnICECandidate(func(c *webrtc.ICECandidate) {
		if c == nil {
			return
		}

		sendToWS(peer, "down_candidate", map[string]webrtc.ICECandidateInit{"candidate": c.ToJSON()})
	})

	pc.OnConnectionStateChange(func(s webrtc.PeerConnectionState) {
		logger.Log.Sugar().Infof("订阅者 %s DownPC state changed: %s", peer.UID, s.String())
		if s == webrtc.PeerConnectionStateFailed || s == webrtc.PeerConnectionStateClosed {
			logger.Log.Sugar().Infof("订阅者 %s DownPC failed or closed.", peer.UID)
		}
	})

	return nil
}

// 从服务端的 downPC 创建 Offer 并发送给订阅者（客户端需回复 down_answer 消息）
func createAndSendDownOffer(subscriber *Peer) {
	// 如果当前正在协商中，标记 '协商结束后需要再次运行'，然后退出
	if !subscriber.downNegotiating.CompareAndSwap(false, true) {
		subscriber.downNeedRetry.Store(true)
		logger.Log.Sugar().Debugf("订阅者 %s 正在协商中，标记重试并跳过", subscriber.UID)
		return
	}

	// 核心协商逻辑
	go func() {
		// 确保函数退出时处理重试逻辑
		defer func() {
			subscriber.downNegotiating.Store(false)
			// 如果在协商期间有新的 Track 变动，再次触发协商
			if subscriber.downNeedRetry.CompareAndSwap(true, false) {
				logger.Log.Sugar().Infof("订阅者 %s 协商期间有变动，触发补偿协商", subscriber.UID)
				createAndSendDownOffer(subscriber)
			}
		}()

		subscriber.mu.Lock()
		pc := subscriber.DownPC
		if pc == nil || pc.SignalingState() != webrtc.SignalingStateStable {
			subscriber.mu.Unlock()
			return
		}

		offer, err := pc.CreateOffer(nil)
		if err != nil {
			subscriber.mu.Unlock()
			logger.Log.Sugar().Errorf("创建 DownOffer 失败: %v", err)
			return
		}

		if err := pc.SetLocalDescription(offer); err != nil {
			subscriber.mu.Unlock()
			logger.Log.Sugar().Errorf("设置 LocalDescription 失败: %v", err)
			return
		}
		subscriber.mu.Unlock()

		// 发送给客户端
		sendToWS(subscriber, "down_offer", DownOfferPayload{
			SDP: offer.SDP,
		})
	}()
}

// 修改 distributeTrackToAllSubscribers 或处理 AddTrack 的地方
func monitorRTCP(sender *webrtc.RTPSender, publisher *Peer) {
	go func() {
		for {
			// ReadRTCP 会阻塞，直到收到订阅者的反馈（如 ACK, PLI, FIR）
			pkts, _, err := sender.ReadRTCP()
			if err != nil {
				return
			}

			for _, pkt := range pkts {
				switch pkt.(type) {
				case *rtcp.PictureLossIndication:
					// 例如:
					// 订阅者说：我卡了，看不见画面
					// SFU 转告发布者：请发个关键帧
					publisher.RequestKeyFrame()
				}
			}
		}
	}()
}

// 当客户端返回 down_answer 消息时，将其设为远端描述（Remote Description）
func setDownAnswer(peer *Peer, sdp string) error {
	peer.mu.Lock()
	pc := peer.DownPC
	peer.mu.Unlock()

	if pc == nil {
		return errors.New("no down pc")
	}

	ans := webrtc.SessionDescription{
		Type: webrtc.SDPTypeAnswer,
		SDP:  sdp,
	}

	// 设置远端描述，这会使 SignalingState 回到 Stable
	if err := pc.SetRemoteDescription(ans); err != nil {
		return err
	}

	return nil
}

func sendToWS(peer *Peer, event string, data any) {
	payload, err := json.Marshal(data)
	if err != nil {
		logger.Log.Sugar().Errorf("序列化数据失败,uid:%s,event:%s,err:%v", peer.UID, event, err)
		return
	}
	peer.wsMu.Lock()
	defer peer.wsMu.Unlock()
	sendRawToWS(peer.WS, event, payload)
}

func sendRawToWS(conn *websocket.Conn, event string, raw json.RawMessage) {
	msg := SignalMessage{
		Event: event,
		Data:  raw,
	}
	b, err := json.Marshal(msg)
	if err != nil {
		logger.Log.Sugar().Errorf("序列化信号消息失败,event:%s,err:%v", event, err)
		return
	}
	if err := conn.WriteMessage(websocket.TextMessage, b); err != nil {
		logger.Log.Sugar().Errorf("发送信号消息失败,event:%s,err:%v", event, err)
	}
}

// 清理
func cleanupPeer(room *Room, peer *Peer) {
	peer.mu.Lock()
	if peer.UpPC != nil {
		_ = peer.UpPC.Close()
	}
	if peer.DownPC != nil {
		_ = peer.DownPC.Close()
	}
	peer.mu.Unlock()
	room.Mu.Lock()
	_, isPublisher := room.Tracks[peer.UID]
	delete(room.Users, peer.UID)

	// 删除 Tracks 记录，否则房间状态不准确
	delete(room.Tracks, peer.UID)

	logger.Log.Sugar().Infof("用户 %s 从房间 %s 移除.", peer.UID, room.ID)

	// @authot lml
	// 如果该用户是发布者，复制房间内的剩余用户列表
	var remainingUsers []*Peer
	if isPublisher {
		remainingUsers = make([]*Peer, 0, len(room.Users))
		for _, u := range room.Users {
			remainingUsers = append(remainingUsers, u)
		}
	}

	room.Mu.Unlock()

	// 通知所有订阅者用户离开了
	if isPublisher {
		payload := map[string]string{"uid": peer.UID}
		for _, subscriber := range remainingUsers {
			sendToWS(subscriber, "user_leave", payload)
		}

		// 通知所有订阅者，移除该用户（发布者）的track
		// 只要该用户是发布者，就通知其他用户进行清理
		notifySubscribersToRemoveTracks(room, peer.UID)
	}
}

// 通知所有订阅者移除指定发布者的track
func notifySubscribersToRemoveTracks(room *Room, publisherUID string) {
	room.Mu.RLock()
	users := make([]*Peer, 0, len(room.Users))
	for _, u := range room.Users {
		users = append(users, u)
	}
	room.Mu.RUnlock()

	for _, u := range users {
		if u.UID == publisherUID {
			continue
		}

		u.mu.Lock()
		pc := u.DownPC
		u.mu.Unlock()

		if pc == nil {
			continue
		}

		var needsNegotiation bool

		for _, sender := range pc.GetSenders() {
			track := sender.Track()
			if track == nil {
				continue
			}

			if track.StreamID() == publisherUID {
				if err := pc.RemoveTrack(sender); err != nil {
					logger.Log.Sugar().Errorf("从订阅者 %s 的 DownPC 移除轨道失败 (发布者: %s), 错误: %v", u.UID, publisherUID, err)
					continue
				}
				needsNegotiation = true
				logger.Log.Sugar().Infof("成功从订阅者 %s 移除发布者 %s 的轨道。", u.UID, publisherUID)
			}
		}

		if needsNegotiation {
			// 当使用 RemoveTrack 成功后，需要发送新的 Offer
			go createAndSendDownOffer(u)
		}
	}
}
