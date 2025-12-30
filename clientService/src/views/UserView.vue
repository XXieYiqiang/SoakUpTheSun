<template>
  <div class="user-dashboard-wrapper">
    <div class="camera-container">
      <h3>用户实时监控端</h3>
      <video ref="videoPlayer" autoplay playsinline muted width="640" height="480"></video>
      <canvas ref="captureCanvas" style="display: none;" width="640" height="480"></canvas>

      <div class="controls">
        <button @click="startStreaming" :disabled="isStreaming">开始推流</button>
        <button @click="stopStreaming" :disabled="!isStreaming">停止推流</button>
        <p>状态: {{ connectionStatus }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
export default {
  name: 'UserDashboard',

  data() {
    return {
      stream: null,
      socket: null,
      isStreaming: false,
      connectionStatus: '未连接',
      timer: null,
      wsUrl: process.env.VUE_APP_WEBSOCKET_USER_IMAGE,
    };
  },

  mounted() {
    this.initCamera();
  },

  beforeDestroy() {
    this.stopStreaming();
    this.releaseCamera();
  },

  methods: {
    // 1. 初始化摄像头
    async initCamera() {
      try {
        this.stream = await navigator.mediaDevices.getUserMedia({
          video: { width: 640, height: 480 },
          audio: false // 如果只需要图片帧，通常关闭音频
        });
        this.$refs.videoPlayer.srcObject = this.stream;
      } catch (err) {
        console.error("无法访问摄像头:", err);
        this.connectionStatus = '摄像头开启失败';
      }
    },

    // 2. 开始推流
    async startStreaming() {
      if (!this.stream) return;

      const token = getToken();
      if (!token) {
        this.connectionStatus = '未登录或 Token 已失效';
        return;
      }

      const finalWsUrl = `${this.wsUrl}${this.wsUrl.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`;

      try {
        this.connectionStatus = '正在建立连接...';
        // 1. 调用带超时的连接方法
        await this.connectWithRetry(finalWsUrl);

        // 2. 连接成功后的逻辑
        this.isStreaming = true;
        this.connectionStatus = '连接成功，身份验证通过';
        this.timer = setInterval(this.captureAndSendFrame, 200);

      } catch (error) {
        // 3. 捕获这里抛出的所有异常
        console.error("推流启动失败:", error);
        this.connectionStatus = `启动失败: ${error.message}`;
        this.stopStreaming(); // 确保清理掉半成品 socket
      }
    },

    /**
     * 核心：使用 Promise 包装 WebSocket 握手过程
     */
    connectWithRetry(url) {
      return new Promise((resolve, reject) => {
        try {
          const ws = new WebSocket(url);

          // 设置一个安全超时（例如 5 秒连不上就认为失败）
          const timeout = setTimeout(() => {
            ws.close();
            reject(new Error("连接服务器超时，请检查网络或地址是否正确"));
             this.$message('连接服务器超时，请检查网络或地址是否正确')
          }, 5000);

          ws.onopen = () => {
            clearTimeout(timeout);
            this.socket = ws; // 成功后挂载到全局
            resolve();
          };

          ws.onerror = (err) => {
            clearTimeout(timeout);
            reject(new Error("WebSocket 握手失败，可能跨域或服务器未启动"));
            this.$message('WebSocket 握手失败，可能跨域或服务器未启动')
          };

          ws.onclose = (event) => {
            // 如果是在 resolve 之前触发了 close，说明是握手阶段被后端拦截器拒绝了
            if (!this.isStreaming) {
              clearTimeout(timeout);
              reject(new Error(`服务器拒绝连接 (状态码: ${event.code})`));
              this.$message(`服务器拒绝连接 (状态码: ${event.code})`)
            }
          };

        } catch (e) {
          // 捕获 URL 非法等同步异常
          reject(e);
        }
      });
    },

    // 3. 核心：截取帧并通过 WebSocket 发送
    captureAndSendFrame() {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

      const video = this.$refs.videoPlayer;
      const canvas = this.$refs.captureCanvas;
      const context = canvas.getContext('2d');

      context.drawImage(video, 0, 0, canvas.width, canvas.height);

      // 1. 获取当前毫秒时间戳
      const timestamp = Date.now();

      canvas.toBlob(async (blob) => {
        if (blob) {
          // 2. 将 Blob 转为 ArrayBuffer 以便操作二进制
          const arrayBuffer = await blob.arrayBuffer();

          // 3. 创建一个新的 Buffer：8字节(时间戳) + 图片数据长度
          const combinedBuffer = new Uint8Array(8 + arrayBuffer.byteLength);

          // 4. 使用 DataView 写入 64 位 BigInt 时间戳 (防止溢出)
          const view = new DataView(combinedBuffer.buffer);
          view.setBigInt64(0, BigInt(timestamp));

          // 5. 拼接图片数据
          combinedBuffer.set(new Uint8Array(arrayBuffer), 8);

          // 6. 发送合成后的二进制包
          this.socket.send(combinedBuffer);
        }
      }, 'image/jpeg', 0.7);
    },

    // 4. 停止推流
    stopStreaming() {
      this.isStreaming = false;
      if (this.timer) clearInterval(this.timer);
      if (this.socket) this.socket.close();
      this.connectionStatus = '已停止';
    },

    // 5. 释放资源
    releaseCamera() {
      if (this.stream) {
        this.stream.getTracks().forEach(track => track.stop());
      }
    }
  },
};
</script>

<style scoped>
.user-dashboard-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.camera-container {
  border: 2px solid #333;
  padding: 10px;
  border-radius: 8px;
}

video {
  background: #000;
  border-radius: 4px;
}

.controls {
  margin-top: 15px;
  text-align: center;
}

button {
  margin: 0 10px;
  padding: 8px 16px;
  cursor: pointer;
}
</style>