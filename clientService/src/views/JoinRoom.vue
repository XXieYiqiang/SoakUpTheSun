<template>
  <div class="join-room-container">
    <transition name="fade" mode="out-in">
      <div v-if="isProcessing" key="loading" class="dark-glass-card">
        <div class="loader-content">
          <div class="tech-loader">
            <div class="circle-outer"></div>
            <div class="circle-inner"></div>
            <div class="scan-line"></div>
          </div>
          <div class="status-info">
            <h2 class="brand-text">SFU Pro</h2>
            <p class="loading-msg">{{ processText }}</p>
            <div class="progress-track">
              <div class="progress-bar"></div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="hasError" key="error" class="dark-glass-card error-card">
        <div class="error-icon-box">
          <span class="exclamation">!</span>
        </div>
        <h3 class="error-title">进入房间失败</h3>
        <p class="error-desc">{{ errorTip }}</p>
        <button class="action-btn" @click="closeWindow">返回大厅</button>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  data() {
    return {
      isProcessing: true,
      processText: '正在初始化系统...',
      hasError: false,
      errorTip: '',
      roomId: '',
      token: ''
    };
  },
  mounted() {
    this.initJoinLogic();
  },
  methods: {
    async initJoinLogic() {
      const query = this.$route.query;
      this.roomId = this.cleanParam(query.room);
      this.token = this.cleanParam(query.token);

      try {
        await this.delay(800);
        this.processText = '正在校验访问权限...';
        
        await this.delay(1200);
        // if (!this.roomId) throw new Error('无效的房间参数');
        this.processText = '正在分配媒体中继服务...';

        await this.delay(1000);
        this.processText = '连接成功，即将跳转...';
        
        await this.delay(500);
        this.$router.replace({
          name: 'chatRoom',
          params: { roomId: this.roomId, token: this.token }
        });
      } catch (err) {
        this.showError(err.message || '网络连接超时');
      }
    },
    cleanParam(val) {
      if (!val) return '';
      return decodeURIComponent(val).replace(/^['"]|['"]$/g, '').trim();
    },
    delay(ms) { return new Promise(res => setTimeout(res, ms)); },
    showError(msg) {
      this.isProcessing = false;
      this.hasError = true;
      this.errorTip = msg;
    },
    closeWindow() {
      window.history.length > 1 ? this.$router.back() : window.close();
    }
  }
}
</script>

<style scoped>
/* 1. 基础容器：深色背景 */
.join-room-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #060c14; /* 匹配你图片中的深色背景 */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  overflow: hidden;
}

/* 2. 深色毛玻璃卡片 */
.dark-glass-card {
  position: relative;
  background: rgba(16, 24, 39, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 60px 40px;
  width: 360px;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

/* 3. 科技感加载动画 */
.tech-loader {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 30px;
}

.circle-outer {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 2px solid rgba(0, 242, 255, 0.1);
  border-top: 2px solid #00f2ff; /* 核心青色 */
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.circle-inner {
  position: absolute;
  width: 60%;
  height: 60%;
  top: 20%;
  left: 20%;
  border: 2px solid rgba(0, 242, 255, 0.05);
  border-bottom: 2px solid #00f2ff;
  border-radius: 50%;
  animation: spin 1.5s linear infinite reverse;
  opacity: 0.7;
}

/* 4. 品牌与文字 */
.brand-text {
  color: #fff;
  font-size: 20px;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.loading-msg {
  color: #8a99af; /* 灰蓝色文字 */
  font-size: 14px;
  margin-bottom: 25px;
}

/* 5. 进度条 */
.progress-track {
  width: 180px;
  height: 2px;
  background: rgba(255, 255, 255, 0.05);
  margin: 0 auto;
  border-radius: 1px;
  overflow: hidden;
}

.progress-bar {
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, #00f2ff, transparent);
  animation: scan 2s infinite ease-in-out;
}

/* 6. 错误状态样式 */
.error-icon-box {
  width: 60px;
  height: 60px;
  border: 2px solid rgba(255, 71, 87, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.exclamation {
  color: #ff4757;
  font-size: 30px;
  font-weight: bold;
}

.error-title { color: #fff; margin-bottom: 10px; }
.error-desc { color: #8a99af; font-size: 13px; margin-bottom: 30px; }

.action-btn {
  background: #00f2ff;
  color: #000;
  border: none;
  padding: 12px 40px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #00d1db;
  box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
}

/* 动画定义 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes scan {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.4s; }
.fade-enter, .fade-leave-to { opacity: 0; }
</style>