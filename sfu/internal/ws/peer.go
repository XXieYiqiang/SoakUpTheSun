package ws

import (
	"sync"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"
	"github.com/pion/rtcp"
	"github.com/pion/webrtc/v4"
)

// Peer 代表一个连接到 SFU 的客户端
type Peer struct {
	UID                string
	WS                 *websocket.Conn
	UpPC               *webrtc.PeerConnection    // 上行 PC (Publisher)
	DownPC             *webrtc.PeerConnection    // 下行 PC (Subscriber)
	Role               UserRole                  // 用户角色
	mu                 sync.Mutex                // 保护 UpPC, DownPC, closed
	wsMu               sync.Mutex                // 保护 WS 写入操作
	downNegotiating    atomic.Bool               // 是否正在进行一次完整的 SDP 交换
	downNeedRetry      atomic.Bool               // 协商期间是否有新的 Track 变更需要再次协商
	UpCandidateQueue   []webrtc.ICECandidateInit // 新增：用于缓冲在上行 Offer 之前到达的 ICE Candidate
	candidateMu        sync.Mutex                // 新增锁来保护 UpCandidateQueue，
	lastPLIRequestTime int64                     // 记录上一次请求关键帧的时间戳（纳秒）用于限流
}

// PeerTracks 存储客户端发布的本地轨道
type PeerTracks struct {
	Audio *webrtc.TrackLocalStaticRTP
	Video *webrtc.TrackLocalStaticRTP
}

// 请求发布者立即产生一个关键帧
func (p *Peer) RequestKeyFrame() {
	p.mu.Lock()
	pc := p.UpPC
	p.mu.Unlock()

	if pc == nil || pc.ConnectionState() != webrtc.PeerConnectionStateConnected {
		return
	}

	now := time.Now().UnixNano()
	last := atomic.LoadInt64(&p.lastPLIRequestTime)

	// 如果距离上次请求不足 500ms，则直接跳过
	// 500ms = 500 * 1000 * 1000 纳秒
	if now-last < int64(500*time.Millisecond) {
		// logger.Log.Sugar().Debugf("PLI 请求太频繁，跳过, uid: %s", u.UID)
		return
	}

	// 更新时间戳
	atomic.StoreInt64(&p.lastPLIRequestTime, now)

	for _, receiver := range pc.GetReceivers() {
		track := receiver.Track()
		if track == nil || track.Kind() != webrtc.RTPCodecTypeVideo {
			continue
		}

		_ = pc.WriteRTCP([]rtcp.Packet{
			&rtcp.PictureLossIndication{
				MediaSSRC: uint32(track.SSRC()),
			},
		})
	}
	// logger.Log.Sugar().Debugf("已向发布者 %s 发送 PLI 请求关键帧", p.UID)
}
