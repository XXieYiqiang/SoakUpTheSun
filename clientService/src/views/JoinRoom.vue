<template>
  <div class="join-room">
    <div v-if="isProcessing" class="status-box">
      <div class="spinner"></div>
      <p>{{ processText }}</p>
    </div>
    
    <div v-else-if="hasError" class="error-box">
      <h3>访问受限</h3>
      <p>{{ errorTip }}</p>
      <button @click="closeWindow">返回并重试</button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      // 状态控制
      isProcessing: true,
      processText: '正在解析用户信息...',
      hasError: false,
      errorTip: '',
      
      // 用户数据
      roomId: '',
      tokenInfo: ''
    };
  },
  mounted() {
    // 页面加载后延迟执行，确保路由已完全准备好
    this.$nextTick(() => {
      this.initJoinLogic();
    });
  },
  methods: {
    initJoinLogic() {
      const query = this.$route.query;
      
      // 1. 获取并清理参数
      this.roomId = this.cleanParam(query.room);
      this.token = this.cleanParam(query.token);

      console.log('解析到的数据:', { name: this.roomId, age: this.token });

      // 2. 校验逻辑
      // if (!this.roomId || this.userName === 'undefined') {
      //   this.showError('未检测到有效的用户名信息');
      //   return;
      // }

      // 3. 模拟请求后端接口进行验证
      this.handleVerify();
    },

    /**
     * 清理参数：去除单引号、双引号、多余空格
     */
    cleanParam(val) {
      if (val === undefined || val === null) return '';
      // 处理可能被 URL 转义的引号，并去除前后的单/双引号
      return decodeURIComponent(val)
        .replace(/^['"]|['"]$/g, '') 
        .trim();
    },

    async handleVerify() {
      this.processText = '正在为您匹配通讯房间...';
      
      try {
        // 此处应为实际的接口调用，例如：
        // const res = await this.$api.getToken({ name: this.userName, age: this.userAge });
        
        // 模拟接口成功返回
        setTimeout(() => {
          this.$router.replace({
            name: 'chatRoom',
            params: {
              roomId: this.roomId, 
              token: this.token 
            }
          });
        }, 5000);
      } catch (err) {
        this.showError('通讯服务器连接失败，请检查网络');
      }
    },

    showError(msg) {
      this.isProcessing = false;
      this.hasError = true;
      this.errorTip = msg;
    },

    closeWindow() {
      // 返回上一页或关闭
      if (window.history.length > 1) {
        this.$router.back();
      } else {
        window.close();
      }
    }
  }
}
</script>

<style scoped>
.join-room { 
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 20px; 
  text-align: center; 
  background-color: #f9f9f9;
}

.status-box .spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-box h3 { color: #333; margin-top: 20px; }
.error-box p { color: #999; font-size: 14px; margin: 10px 0 30px; }

button {
  padding: 10px 30px; 
  background: #3498db; 
  color: #fff;
  border: none; 
  border-radius: 20px; 
  cursor: pointer;
  transition: background 0.3s;
}

button:hover {
  background: #2980b9;
}
</style>