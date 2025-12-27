<template>
  <div class="sfu-room">
    <header class="glass-card header-bar">
      <div class="brand">
        <div class="live-dot" :class="{ active: joined }"></div>
        <h2 class="title">SFU Pro <span class="subtitle">Video Meeting</span></h2>
      </div>
      <div v-if="joined" class="room-stats">
        <span class="badge">房间: {{ roomID }}</span>
        <span class="badge">用户: {{ uid }}</span>
        <button class="btn-danger" @click="leaveRoom">离开会议</button>
      </div>
    </header>

    <div v-if="!joined" class="login-container">
      <div class="glass-card login-box">
        <h3>加入会议</h3>
        <div class="input-group">
          <input v-model="roomID" placeholder="房间号" />
          <input v-model="uid" placeholder="您的昵称" />
          <input v-model="tokenInfo" placeholder="TokenInfo" />
        </div>
        <button class="btn-primary" @click="joinRoom" :disabled="loading">
          {{ loading ? '正在进入...' : '立即加入' }}
        </button>
      </div>
    </div>

    <main v-else class="meeting-area">
      <div class="layout-grid">

        <section class="main-stage glass-card">
          <div class="stage-content">
            <div class="video-wrapper">
              <video v-show="focusId === 'local'" ref="focusLocalVideo" autoplay muted playsinline class="mirror">
              </video>
              <video v-for="(stream, remoteUid) in remoteStreams" :key="'focus-' + remoteUid"
                v-show="focusId === remoteUid" :src-object.prop="stream" autoplay playsinline>
              </video>
              <div class="stage-info">
                <span class="talking-name">正在观看: {{ focusId === 'local' ? '我' : focusId }}</span>
              </div>
            </div>
          </div>
        </section>

        <aside class="side-gallery">
          <div class="gallery-grid">
            <div class="mini-card glass-card" :class="{ 'is-focus': focusId === 'local' }" @click="focusId = 'local'">
              <video ref="miniLocalVideo" autoplay muted playsinline class="mirror"></video>
              <div class="mini-label">我</div>
            </div>

            <div v-for="(stream, remoteUid) in remoteStreams" :key="'mini-' + remoteUid" class="mini-card glass-card"
              :class="{ 'is-focus': focusId === remoteUid }" @click="focusId = remoteUid">
              <video :src-object.prop="stream" autoplay playsinline></video>
              <div class="mini-label">{{ remoteUid }}</div>
            </div>
          </div>

          <div class="mini-logs glass-card">
            <div class="log-header">系统日志</div>
            <div class="log-content">
              <div v-for="(l, i) in logs" :key="i" class="log-item">{{ l }}</div>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script>
export default {
  name: "SFURoom",
  data() {
    return {
      // wsUrl: "ws://192.168.43.143:8080/room/join",
      // wsUrl: "ws://127.0.0.1:10014/room/join",
      wsUrl: "ws://192.168.43.95:10014/room/join",
      roomID: "2jcfZY1wsrddK5D2g3Np1z",
      uid: "user_" + Math.floor(Math.random() * 10000),
      tokenInfo: 'tokenInfo',

      joined: false,
      loading: false,
      ws: null,
      logs: [],
      focusId: 'local', // 当前大屏显示的ID

      // 推流
      localStream: null,
      upPC: null,

      // 拉流核心逻辑 (保持不变)
      downPC: null,
      negotiating: false,
      offerQueue: [],
      pendingCandidates: [],

      // 远端流
      remoteStreams: {}
    };
  },

  watch: {
    // 确保本地流在 DOM 更新后挂载到所有本地 video 标签
    joined(val) {
      if (val) {
        this.$nextTick(() => {
          this.updateLocalVideos();
        });
      }
    }
  },
  mounted() {
    // 获取跳转时传入的 roomID 和 token
    const roomID = this.$route.params.roomId;
    const tokenInfo = this.$route.params.token;

    console.log('当前房间ID:', roomID);
    console.log('当前Token:', tokenInfo);

    if (roomID && tokenInfo) {
      this.roomID = roomID;
      this.tokenInfo = tokenInfo;

      this.joinRoom()
    } else {
      console.error('参数丢失，无法进入聊天室');
    }
  },
  methods: {
    log(msg) {
      const t = new Date().toLocaleTimeString();
      this.logs.unshift(`[${t}] ${msg}`);
      if (this.logs.length > 20) this.logs.pop();
    },

    updateLocalVideos() {
      if (this.localStream) {
        if (this.$refs.focusLocalVideo) this.$refs.focusLocalVideo.srcObject = this.localStream;
        if (this.$refs.miniLocalVideo) this.$refs.miniLocalVideo.srcObject = this.localStream;
      }
    },

    async joinRoom() {
      this.loading = true;
      try {
        this.localStream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: true
        });
        this.connectWS();
      } catch (e) {
        alert("获取媒体失败: " + e.message);
        this.loading = false;
      }
    },

    connectWS() {
      // const url = `${this.wsUrl}?roomID=${this.roomID}&uid=${this.uid}`;
      const url = `${this.wsUrl}?roomID=${this.roomID}&uid=${this.uid}&roomToken=${encodeURIComponent(this.tokenInfo)}`;
      this.ws = new WebSocket(url);
      this.ws.onopen = () => {
        this.joined = true;
        this.loading = false;
        this.log("信令连接成功");
        this.createUpPC();
      };
      this.ws.onmessage = e => this.handleSignal(JSON.parse(e.data));
      this.ws.onclose = () => this.leaveRoom(false);
    },

    sendSignal(event, data) {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ event, data }));
      }
    },

    async handleSignal({ event, data }) {
      switch (event) {
        case "up_answer":
          await this.upPC.setRemoteDescription({ type: "answer", sdp: data.sdp });
          this.log("推流成功");
          break;
        case "up_candidate":
          this.upPC && this.upPC.addIceCandidate(data.candidate);
          break;
        case "down_offer":
          this.offerQueue.push(data);
          this.tryProcessOffer();
          break;
        case "down_candidate":
          this.handleDownCandidate(data.candidate);
          break;
        case "unpublish_stream":
        case "user_leave":
          this.removeRemote(data.publisherId || data.uid);
          break;
      }
    },

    async createUpPC() {
      this.upPC = new RTCPeerConnection({
        iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
      });
      this.localStream.getTracks().forEach(t => this.upPC.addTrack(t, this.localStream));
      this.upPC.onicecandidate = e => {
        e.candidate && this.sendSignal("up_candidate", { candidate: e.candidate });
      };
      const offer = await this.upPC.createOffer();
      await this.upPC.setLocalDescription(offer);
      this.sendSignal("up_offer", { uid: this.uid, sdp: offer.sdp });
    },

    initDownPC() {
      if (this.downPC) return;
      this.downPC = new RTCPeerConnection({
        iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
      });
      this.downPC.onicecandidate = e => {
        e.candidate && this.sendSignal("down_candidate", { candidate: e.candidate });
      };
      this.downPC.ontrack = e => {
        const stream = e.streams[0];
        if (!stream) return;
        const uid = stream.id;
        if (!this.remoteStreams[uid]) {
          this.$set(this.remoteStreams, uid, stream);
          this.log(`订阅到 ${uid}`);
        }
      };
    },

    async tryProcessOffer() {
      if (this.negotiating || this.offerQueue.length === 0) return;
      this.negotiating = true;
      const offer = this.offerQueue.shift();
      try {
        this.initDownPC();
        await this.downPC.setRemoteDescription({ type: "offer", sdp: offer.sdp });
        while (this.pendingCandidates.length) {
          await this.downPC.addIceCandidate(new RTCIceCandidate(this.pendingCandidates.shift()));
        }
        const answer = await this.downPC.createAnswer();
        await this.downPC.setLocalDescription(answer);
        this.sendSignal("down_answer", { sdp: answer.sdp });
      } catch (e) {
        console.error(e);
      } finally {
        this.negotiating = false;
        setTimeout(() => this.tryProcessOffer(), 0);
      }
    },

    async handleDownCandidate(cand) {
      if (!this.downPC || !this.downPC.remoteDescription) {
        this.pendingCandidates.push(cand);
        return;
      }
      try { await this.downPC.addIceCandidate(new RTCIceCandidate(cand)); } catch { }
    },

    removeRemote(uid) {
      if (this.remoteStreams[uid]) {
        this.$delete(this.remoteStreams, uid);
        if (this.focusId === uid) this.focusId = 'local';
        this.log(`用户 ${uid} 离开`);
      }
    },

    leaveRoom(closeWs = true) {
      this.joined = false;
      this.focusId = 'local';
      this.offerQueue = [];
      this.pendingCandidates = [];
      this.upPC && this.upPC.close();
      this.downPC && this.downPC.close();
      if (this.localStream) {
        this.localStream.getTracks().forEach(t => t.stop());
      }
      if (closeWs && this.ws) this.ws.close();
      this.upPC = this.downPC = this.ws = this.localStream = null;
      this.remoteStreams = {};
      this.log("已退出");
    }
  },
  beforeDestroy() {
    this.leaveRoom();
  }
};
</script>

<style scoped>
/* 修改和新增的样式 */

.main-stage {
  position: relative;
  background: #0a0a0a;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  /* 增加内边距，使中间视频不顶格 */
  background: radial-gradient(circle, #2a2a2a 0%, #000000 100%);
}

.stage-content {
  width: 100%;
  height: 100%;
  max-width: 90%;
  /* 宽度最大占用 90%，留出呼吸感 */
  max-height: 90%;
  /* 高度最大占用 90% */
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  box-shadow: 0 20px 50px fff(0, 0, 0, 0.5);
  /* 给大屏增加阴影层级 */
  border-radius: 30px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-stage video {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  /* 确保画面比例不失真且居中 */
}

/* 优化右侧缩略图间距 */
.gallery-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  /* 稍微加大间距 */
  overflow-y: auto;
  padding: 4px;
}

.mini-card {
  position: relative;
  aspect-ratio: 16/9;
  cursor: pointer;
  overflow: hidden;
  border-radius: 20px;
  /* 圆角配合大屏 */
  border: 2px solid transparent;
  transition: transform 0.2s;
}

.mini-card:hover {
  transform: scale(1.02);
}

.mini-card.is-focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 10px rgba(79, 70, 229, 0.3);
}

/* 基础容器 */
.sfu-room {
  --primary-color: #4f46e5;
  --bg-color: #0f172a;
  --glass-bg: rgba(255, 255, 255, 0.05);
  --glass-border: rgba(255, 255, 255, 0.1);

  min-height: 100vh;
  background: var(--bg-color);
  color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  display: flex;
  flex-direction: column;
  padding: 16px;
  box-sizing: border-box;
}

/* 玻璃拟态卡片 */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  margin-bottom: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.live-dot {
  width: 10px;
  height: 10px;
  background: #64748b;
  border-radius: 50%;
}

.live-dot.active {
  background: #10b981;
  box-shadow: 0 0 10px #10b981;
  animation: pulse 2s infinite;
}

.title {
  font-size: 1.1rem;
  margin: 0;
}

.subtitle {
  color: #94a3b8;
  font-weight: normal;
  font-size: 0.9rem;
}

.badge {
  background: rgba(0, 0, 0, 0.3);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.8rem;
  margin-right: 10px;
}

/* 登录框 */
.login-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-box {
  width: 320px;
  padding: 30px;
  text-align: center;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 20px 0;
}

input {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid var(--glass-border);
  padding: 10px;
  border-radius: 6px;
  color: white;
  outline: none;
}

button {
  cursor: pointer;
  border: none;
  transition: 0.2s;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
  padding: 10px;
  border-radius: 6px;
  width: 100%;
}

.btn-danger {
  background: #ef4444;
  color: white;
  padding: 6px 14px;
  border-radius: 6px;
}

/* 布局网格 */
.meeting-area {
  flex: 1;
  min-height: 0;
}

.layout-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  /* 大屏 + 侧边栏宽度 */
  gap: 16px;
  height: 100%;
}

/* 主舞台 */
.main-stage {
  position: relative;
  /* background: #fff; */
  /* background: radial-gradient(circle, #2a2a2a 0%, #fff 100%); */
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}

.main-stage video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.stage-info {
  position: absolute;
  bottom: 16px;
  left: 16px;
  background: rgba(0, 0, 0, 0.6);
  padding: 4px 12px;
  border-radius: 4px;
}

/* 侧边缩略图网格 */
.side-gallery {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.gallery-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  /* 关键：一行两个 */
  gap: 8px;
  overflow-y: auto;
  padding-right: 4px;
}

.mini-card {
  position: relative;
  aspect-ratio: 16/9;
  cursor: pointer;
  overflow: hidden;
  border: 2px solid transparent;
}

.mini-card.is-focus {
  border-color: var(--primary-color);
}

.mini-card video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mini-label {
  position: absolute;
  bottom: 4px;
  left: 4px;
  font-size: 10px;
  background: rgba(0, 0, 0, 0.5);
  padding: 1px 4px;
  border-radius: 2px;
}

/* 日志区 */
.mini-logs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 120px;
  max-height: 200px;
  font-size: 0.75rem;
}

.log-header {
  padding: 8px;
  border-bottom: 1px solid var(--glass-border);
  color: #94a3b8;
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  font-family: monospace;
}

.log-item {
  margin-bottom: 4px;
  color: #cbd5e1;
}

/* 辅助类 */
.mirror {
  transform: scaleX(-1);
  border-radius: 10px;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }

  50% {
    opacity: 0.5;
  }

  100% {
    opacity: 1;
  }
}

/* 响应式：屏幕太小时侧边栏移到下面 */
@media (max-width: 1000px) {
  .layout-grid {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr 200px;
  }

  .side-gallery {
    flex-direction: row;
  }

  .gallery-grid {
    grid-template-columns: repeat(10, 160px);
    display: flex;
  }

  .mini-logs {
    display: none;
  }
}
</style>