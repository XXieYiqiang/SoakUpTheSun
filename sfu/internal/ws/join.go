package ws

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"sfu/internal/logger"
	"time"

	"github.com/bytedance/sonic"
	"github.com/gorilla/websocket"
	"github.com/pion/rtcp"
	"github.com/pion/rtp"
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
func HandleWS(room *Room, user *User) {
	defer func() {
		cleanupUserV2(room, user)
	}()

	// 读取循环
	for {
		_, raw, err := user.WS.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				logger.Log.Sugar().Errorf("读取ws消息失败,uid:%s,err:%v", user.UID, err)
			}
			return // 关闭用户连接
		}
		var msg SignalMessage
		if err := sonic.Unmarshal(raw, &msg); err != nil {
			logger.Log.Sugar().Errorf("解析信号消息失败,uid:%s,err:%v", user.UID, err)
			continue
		}

		switch msg.Event {
		case "up_offer":
			var p UpOfferPayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析up_offer数据失败,uid:%s,err:%v", user.UID, err)
				continue
			}
			go handleUpOffer(room, user, p.SDP)

		case "up_candidate":
			var p CandidatePayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析up_candidate数据失败,uid:%s,err:%v", user.UID, err)
				continue
			}
			user.mu.Lock()
			pc := user.UpPC
			user.mu.Unlock()
			if pc != nil {
				if err := pc.AddICECandidate(p.Candidate); err != nil {
					logger.Log.Sugar().Errorf("添加up_candidate失败,uid:%s,err:%v", user.UID, err)
				}
			}

		case "down_answer":
			var p SDPPayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析down_answer数据失败,uid:%s,err:%v", user.UID, err)
				continue
			}
			if err := setDownAnswer(user, p.SDP); err != nil {
				logger.Log.Sugar().Errorf("设置down_answer失败,uid:%s,err:%v", user.UID, err)
			}

		case "down_candidate":
			var p CandidatePayload
			if err := sonic.Unmarshal(msg.Data, &p); err != nil {
				logger.Log.Sugar().Errorf("解析down_candidate数据失败,uid:%s,err:%v", user.UID, err)
				continue
			}
			user.mu.Lock()
			pc := user.DownPC
			user.mu.Unlock()
			if pc != nil {
				if err := pc.AddICECandidate(p.Candidate); err != nil {
					logger.Log.Sugar().Errorf("添加down_candidate失败,uid:%s,err:%v", user.UID, err)
				}
			}

		case "subscribe":
		default:
			logger.Log.Sugar().Errorf("未知信号事件:%s,uid:%s", msg.Event, user.UID)
		}
	}
}

// 处理客户端发送的 offer (client -> server)
func handleUpOffer(room *Room, user *User, sdp string) {
	// 1. 创建 PeerConnection
	api := webrtc.NewAPI()
	pc, err := api.NewPeerConnection(webrtc.Configuration{})
	if err != nil {
		logger.Log.Sugar().Errorf("创建UpPC失败,uid:%s,err:%v", user.UID, err)
		return
	}

	// 2. 存储 PC (需要加锁)
	user.mu.Lock()
	if user.UpPC != nil {
		_ = user.UpPC.Close()
		logger.Log.Sugar().Infof("关闭已存在UpPC,uid:%s", user.UID)
	}
	user.UpPC = pc
	user.mu.Unlock()

	// 3. 设置 ICE 候选回调
	pc.OnICECandidate(func(c *webrtc.ICECandidate) {
		if c == nil {
			return
		}

		/**
		 * @author lml
		 *  ✅ 统一使用带锁的发送函数
		 */
		sendToWS(user, user.WS, "up_candidate", map[string]webrtc.ICECandidateInit{"candidate": c.ToJSON()})
	})

	// 4. 设置媒体轨道回调
	pc.OnTrack(func(remote *webrtc.TrackRemote, receiver *webrtc.RTPReceiver) {

		//接收 RTCP (重要：用于控制和质量反馈)
		// go func() {
		// 	b := make([]byte, 1500)
		// 	for {
		// 		if _, _, err = receiver.Read(b); err != nil {
		// 			return
		// 		}
		// 		// TODO 完整的 SFU 应该在这里解析 RTCP 包，如 PLI/NACK，并响应或转发。
		// 	}
		// }()
		/**
		 *@author lml
		 *添加 RTCP 反馈处理和 PLI 定时发送
		 */
		go func() {
			// 创建一个定时器，每 3 秒请求一次关键帧
			// 这样可以确保新加入的订阅者能在最多 3 秒内看到画面
			ticker := time.NewTicker(time.Second * 3)
			defer ticker.Stop()

			// 同时我们要消耗来自推流端的 RTCP 包（如接收报告等）
			// 这是 Pion 库的要求，否则会导致缓存满而停止处理
			go func() {
				b := make([]byte, 1500)
				for {
					if _, _, err := receiver.Read(b); err != nil {
						return
					}
				}
			}()

			for {
				select {
				case <-ticker.C:
					// 如果推流连接已经关闭，退出协程
					if pc.ConnectionState() == webrtc.PeerConnectionStateClosed {
						return
					}

					// 只有视频轨道需要请求关键帧 (PLI)
					if remote.Kind() == webrtc.RTPCodecTypeVideo {
						// 向推流端发送 Picture Loss Indication (PLI)
						err := pc.WriteRTCP([]rtcp.Packet{
							&rtcp.PictureLossIndication{MediaSSRC: uint32(remote.SSRC())},
						})
						if err != nil {
							logger.Log.Sugar().Debugf("发送 PLI 失败: %v", err)
							return
						}
					}
				}
			}
		}()

		// 创建本地 track (TrackLocalStaticRTP)
		capability := remote.Codec().RTPCodecCapability
		var local *webrtc.TrackLocalStaticRTP
		local, err = webrtc.NewTrackLocalStaticRTP(capability, remote.ID(), user.UID)
		if err != nil {
			logger.Log.Sugar().Errorf("创建TrackLocalStaticRTP失败,uid:%s,err:%v", user.UID, err)
			return
		}

		// 存储本地 track 到房间
		room.Mu.Lock()
		ut := room.Tracks[user.UID]
		if ut == nil {
			ut = &UserTracks{}
			room.Tracks[user.UID] = ut
		}
		if remote.Kind() == webrtc.RTPCodecTypeAudio {
			ut.Audio = local
		} else if remote.Kind() == webrtc.RTPCodecTypeVideo {
			ut.Video = local
		}
		room.Mu.Unlock()

		//RTP 转发循环 (remote -> local)
		go func() {
			buf := make([]byte, 1500)
			pkt := &rtp.Packet{}
			for {
				n, _, readErr := remote.Read(buf)
				if readErr != nil {
					if !errors.Is(readErr, io.EOF) {
						logger.Log.Sugar().Errorf("remote track read error for %s: %v", user.UID, readErr)
					}
					break
				}
				if err = pkt.Unmarshal(buf[:n]); err != nil {
					logger.Log.Sugar().Errorf("rtp unmarshal error for %s: %v", user.UID, err)
					continue
				}
				pkt.Extension = false
				pkt.Extensions = nil
				if err = local.WriteRTP(pkt); err != nil {
					break
				}
			}

			//Track 结束清理
			room.Mu.Lock()
			if trks, ok := room.Tracks[user.UID]; ok {
				if remote.Kind() == webrtc.RTPCodecTypeAudio {
					trks.Audio = nil
				} else {
					trks.Video = nil
				}
			}
			room.Mu.Unlock()
		}()

		//将新轨道分发给所有订阅者
		distributeTrackToAllSubscribersV2(room, user.UID, local)
	})

	// 设置远端描述并创建 Answer
	offer := webrtc.SessionDescription{
		Type: webrtc.SDPTypeOffer,
		SDP:  sdp,
	}
	if err = pc.SetRemoteDescription(offer); err != nil {
		logger.Log.Sugar().Errorf("设置remote description失败,uid:%s,err:%v", user.UID, err)
		return
	}
	// 应用缓冲的 ICE Candidate
	go func() {
		user.candidateMu.Lock()
		candidates := user.UpCandidateQueue
		user.UpCandidateQueue = nil // 清空队列
		user.candidateMu.Unlock()

		for _, cand := range candidates {
			if err = pc.AddICECandidate(cand); err != nil {
				logger.Log.Sugar().Errorf("添加buffered candidate失败,uid:%s,err:%v", user.UID, err)
			}
		}
	}()
	answer, err := pc.CreateAnswer(nil)
	if err != nil {
		logger.Log.Sugar().Errorf("创建answer失败,uid:%s,err:%v", user.UID, err)
		return
	}
	if err := pc.SetLocalDescription(answer); err != nil {
		logger.Log.Sugar().Errorf("设置local description for answer失败,uid:%s,err:%v", user.UID, err)
		return
	}

	// 6. 向客户端发送 answer
	sendToWS(user, user.WS, "up_answer", map[string]string{"sdp": answer.SDP})

	go func() {
		time.Sleep(200 * time.Millisecond) // 延迟 200ms 等待自己的推流连接稳定
		distributeAllExistingTracksToNewSubscriber(room, user)
	}()
}

/**
 * @author lml 【新增函数】
 * 分发所有已存在的轨道给新的订阅者
 */
func distributeAllExistingTracksToNewSubscriber(room *Room, newSubscriber *User) {
	room.Mu.RLock()
	// 获取所有正在发布的 Track
	tracks := room.Tracks
	room.Mu.RUnlock()

	if len(tracks) == 0 {
		return // 房间内当前没有发布者
	}

	if err := ensureDownPC(newSubscriber); err != nil {
		logger.Log.Sugar().Errorf("为新订阅者 %s 确保DownPC失败: %v", newSubscriber.UID, err)
		return
	}

	newSubscriber.mu.Lock()
	downPC := newSubscriber.DownPC
	newSubscriber.mu.Unlock()

	// 遍历所有发布者的所有轨道
	for publisherUID, ut := range tracks {
		if publisherUID == newSubscriber.UID {
			continue // 不订阅自己的流
		}

		// 尝试添加音频轨道
		if ut.Audio != nil {
			if _, err := downPC.AddTrack(ut.Audio); err != nil {
				logger.Log.Sugar().Errorf("新订阅者 %s 添加音频 Track 失败: %v", newSubscriber.UID, err)
			}
		}
		// 尝试添加视频轨道
		if ut.Video != nil {
			if _, err := downPC.AddTrack(ut.Video); err != nil {
				logger.Log.Sugar().Errorf("新订阅者 %s 添加视频 Track 失败: %v", newSubscriber.UID, err)
			}
		}
	}

	// 完成所有 Track 添加后，创建并发送 DownOffer 进行一次性协商
	if len(tracks) > 0 { // 只有房间内有流才需要发送 Offer
		logger.Log.Sugar().Infof("新订阅者 %s 订阅了 %d 个已存在的流，发送 DownOffer", newSubscriber.UID, len(tracks))
		// ⚠️ 这里的 publisherUID 只是一个占位符，不重要，因为 SDP 中包含所有流信息
		createAndSendDownOffer(newSubscriber, newSubscriber.UID)
	}
}

// 为每个订阅者添加本地 track (local -> downPC)
// Deprecated: 使用 distributeTrackToAllSubscribersV2,该方法会导致前端使用getDisplayMedia获取到的轨道无法播放问题
func distributeTrackToAllSubscribers(room *Room, publisherUID string, track webrtc.TrackLocal) {
	room.Mu.RLock()
	users := make([]*User, 0, len(room.Users))
	for _, u := range room.Users {
		users = append(users, u)
	}
	room.Mu.RUnlock()

	for _, u := range users {
		if u.UID == publisherUID {
			continue
		}
		if err := ensureDownPC(u); err != nil {
			logger.Log.Sugar().Errorf("确保DownPC失败,subscriber %s: %v", u.UID, err)
			continue
		}

		u.mu.Lock()
		pc := u.DownPC
		u.mu.Unlock()

		// 尝试添加 Track。在 Unified Plan 下，如果 AddTrack 成功，
		// 则表示需要发送一个新的 Offer 来通知订阅者新轨道的到来。
		if _, err := pc.AddTrack(track); err != nil {
			logger.Log.Sugar().Errorf("添加track到DownPC失败,subscriber %s: %v", u.UID, err)
			continue
		}

		// 添加 Track 成功，触发重新协商
		go createAndSendDownOffer(u, publisherUID)
	}
}

// distributeTrackToAllSubscribersV2 为每个订阅者添加本地 track (local -> downPC)
// 设置 RTCP 监听，以接收和转发 PLI (关键帧请求)。
func distributeTrackToAllSubscribersV2(room *Room, publisherUID string, track webrtc.TrackLocal) {
	room.Mu.RLock()
	// 创建用户列表的副本，以便在遍历时可以安全地释放锁
	users := make([]*User, 0, len(room.Users))
	for _, u := range room.Users {
		users = append(users, u)
	}
	room.Mu.RUnlock()

	for _, u := range users {
		// 排除发布者自己
		if u.UID == publisherUID {
			continue
		}

		// 确保订阅者 (Subscriber) 的 DownPC 存在并初始化
		if err := ensureDownPC(u); err != nil {
			logger.Log.Sugar().Errorf("确保DownPC失败,subscriber %s: %v", u.UID, err)
			continue
		}

		u.mu.Lock()
		pc := u.DownPC
		u.mu.Unlock()

		var sender *webrtc.RTPSender
		var err error

		// AddTrack 返回 RTPSender，需要它来监听 RTCP (PLI)
		if sender, err = pc.AddTrack(track); err != nil {
			// 如果轨道已经添加过 (例如，由于并发调用)，则忽略错误并继续。
			logger.Log.Sugar().Errorf("添加track到DownPC失败,subscriber %s: %v", u.UID, err)
			continue
		}
		logger.Log.Sugar().Infof("成功向订阅者 %s 的 DownPC 添加轨道 (来自 %s)", u.UID, publisherUID)

		// 监听 Sender 返回的 RTCP (即订阅者发来的 PLI 请求)
		go func(sender *webrtc.RTPSender, publisherUID string, subscriberUID string) {
			for {
				pkts, _, readErr := sender.ReadRTCP()
				if readErr != nil {
					// DownPC 关闭或错误时退出
					return
				}

				// 检查是否是 PLI 请求
				for _, p := range pkts {
					if _, ok := p.(*rtcp.PictureLossIndication); ok {
						logger.Log.Sugar().Infof("收到订阅者 %s 的 PLI 请求 (发布者: %s)。准备转发...", subscriberUID, publisherUID)

						// 转发 PLI 给发布者 UpPC
						room.Mu.RLock()
						pubUser := room.Users[publisherUID]
						room.Mu.RUnlock()

						if pubUser != nil && pubUser.UpPC != nil {
							// 遍历发布者 UpPC 的所有接收器 (Receiver)
							for _, receiver := range pubUser.UpPC.GetReceivers() {
								track := receiver.Track()
								// 找到正确的 Receiver 来发送 PLI (通常通过 SSRC 匹配)
								if track != nil {
									// 通过 WriteRTCP 发送 PLI 请求给发布者客户端
									_ = pubUser.UpPC.WriteRTCP([]rtcp.Packet{
										&rtcp.PictureLossIndication{
											MediaSSRC: uint32(track.SSRC()),
										},
									})
									logger.Log.Sugar().Infof("PLI 已转发给发布者 %s", publisherUID)
									break // 找到并发送后退出 receiver 循环
								}
							}
						}
					}
				}
			}
		}(sender, publisherUID, u.UID)

		// 触发重新协商 (发送 Down Offer)
		// AddTrack 成功后，必须发送 Offer 通知客户端新轨道的到来
		go createAndSendDownOffer(u, publisherUID)
	}
}

// 用户确保存在 DownPC（每个订阅者只有一个 DownPC）
func ensureDownPC(user *User) error {
	user.mu.Lock()
	defer user.mu.Unlock()

	if user.DownPC != nil {
		return nil
	}

	api := webrtc.NewAPI()
	pc, err := api.NewPeerConnection(webrtc.Configuration{})
	if err != nil {
		return err
	}
	user.DownPC = pc

	pc.OnICECandidate(func(c *webrtc.ICECandidate) {
		if c == nil {
			return
		}
		/***
		 * @author lml
		 *  ✅ 统一使用带锁的发送函数
		 */
		// candidateJSON, _ := json.Marshal(c.ToJSON())
		// sendRawToWS(user.WS, "down_candidate", json.RawMessage(`{"candidate":`+string(candidateJSON)+`}`))
		sendToWS(user, user.WS, "down_candidate", map[string]webrtc.ICECandidateInit{"candidate": c.ToJSON()})
		//======================================================================================
	})

	pc.OnConnectionStateChange(func(s webrtc.PeerConnectionState) {
		logger.Log.Sugar().Infof("订阅者 %s DownPC state changed: %s", user.UID, s.String())
		if s == webrtc.PeerConnectionStateFailed || s == webrtc.PeerConnectionStateClosed {
			logger.Log.Sugar().Infof("订阅者 %s DownPC failed or closed.", user.UID)
		}
	})

	return nil
}

// 从服务端的 downPC 创建 Offer 并发送给订阅者（客户端需回复 down_answer 消息）
func createAndSendDownOffer(subscriber *User, publisherUID string) {
	subscriber.mu.Lock()
	pc := subscriber.DownPC
	if pc == nil {
		subscriber.mu.Unlock()
		return
	}

	// 🚨 关键：如果当前状态不是 Stable，说明上一次协商还没完
	/**
	 *@author: lml
	 *保证了服务器在向订阅者推送多个媒体流时，能够有条不紊地排队发送，避免因为信令步调不一致导致的连接中断。
	 */
	if pc.SignalingState() != webrtc.SignalingStateStable {
		subscriber.mu.Unlock()
		logger.Log.Sugar().Warnf("订阅者 %s 状态忙 (%s), 500ms 后重试...", subscriber.UID, pc.SignalingState().String())

		// 延迟 500ms 递归调用
		time.AfterFunc(500*time.Millisecond, func() {
			createAndSendDownOffer(subscriber, publisherUID)
		})
		return
	}
	subscriber.mu.Unlock()

	offer, err := pc.CreateOffer(nil)
	if err != nil {
		logger.Log.Sugar().Errorf("创建DownPC offer失败,uid:%s,err:%v", subscriber.UID, err)
		return
	}
	if err := pc.SetLocalDescription(offer); err != nil {
		logger.Log.Sugar().Errorf("设置local description失败,uid:%s,err:%v", subscriber.UID, err)
		return
	}

	payload := DownOfferPayload{
		From: publisherUID,
		SDP:  offer.SDP,
	}

	/**
	*✅ 统一使用带锁的发送函数
	 */
	sendToWS(subscriber, subscriber.WS, "down_offer", payload)
}

// 当客户端返回 down_answer 消息时，将其设为远端描述（Remote Description）
func setDownAnswer(user *User, sdp string) error {
	user.mu.Lock()
	pc := user.DownPC
	user.mu.Unlock()

	if pc == nil {
		return errors.New("no down pc to set answer")
	}
	ans := webrtc.SessionDescription{
		Type: webrtc.SDPTypeAnswer,
		SDP:  sdp,
	}
	if err := pc.SetRemoteDescription(ans); err != nil {
		return err
	}
	return nil
}

func sendToWS(user *User, conn *websocket.Conn, event string, data any) {
	payload, err := json.Marshal(data)
	if err != nil {
		logger.Log.Sugar().Errorf("序列化数据失败,uid:%s,event:%s,err:%v", user.UID, event, err)
		return
	}
	user.wsMu.Lock()
	defer user.wsMu.Unlock()
	sendRawToWS(conn, event, payload)
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
func cleanupUser(room *Room, user *User) {
	room.Mu.Lock()
	_, isPublisher := room.Tracks[user.UID]
	delete(room.Users, user.UID)

	// 删除 Tracks 记录，否则房间状态不准确
	delete(room.Tracks, user.UID)

	logger.Log.Sugar().Infof("用户 %s 从房间 %s 移除.", user.UID, room.ID)

	/**
	* @authot lml
	* ✅ 如果该用户是发布者，复制房间内的剩余用户列表
	 */
	var remainingUsers []*User
	if isPublisher {
		remainingUsers = make([]*User, 0, len(room.Users))
		for _, u := range room.Users {
			remainingUsers = append(remainingUsers, u)
		}
	}

	room.Mu.Unlock()

	/**
	 * @author lml
	 * 知所有订阅者用户离开了
	 */
	if isPublisher {
		// 通知所有订阅者用户离开了
		payload := map[string]string{"uid": user.UID}
		for _, subscriber := range remainingUsers {
			sendToWS(subscriber, subscriber.WS, "user_leave", payload)
		}
	}
}
