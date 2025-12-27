<template>
  <div class="login-view-container">
    <div class="tech-bg-overlay"></div>

    <div class="login-card-wrapper tech-dialog">
      <div class="dialog-content-wrapper">
        <h2 class="login-title">用户登录</h2>
        <p class="login-subtitle">欢迎连接 Gim-Chat 智能终端</p>

        <div class="voice-panel">
          <div class="voice-btn-group">
            <el-button 
              :type="voice.active ? 'danger' : 'primary'" 
              circle 
              @click="toggleVoiceAssistant"
              :class="{'pulse-red': voice.active}"
              icon="el-icon-microphone"
            ></el-button>
            <div class="voice-status-text">
              <span class="status-tag">{{ voice.isRunning ? 'LISTENING' : 'IDLE' }}</span>
              <p class="tips">{{ voice.tips }}</p>
            </div>
          </div>
          <div v-if="voice.interimText" class="interim-display">
            " {{ voice.interimText }} "
          </div>
        </div>

        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="tech-form">
          <el-form-item prop="userAccount">
            <el-input v-model="loginForm.userAccount" placeholder="请输入用户账号" prefix-icon="el-icon-mobile-phone" class="tech-input"></el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" prefix-icon="el-icon-lock" class="tech-input"></el-input>
          </el-form-item>

          <el-form-item class="login-action-item">
            <el-button type="primary" @click="submitLogin" class="tech-btn block-btn primary" :loading="isLoading">
              {{ isLoading ? '连接中...' : '登 录' }} <i class="el-icon-right"></i>
            </el-button>
          </el-form-item>

          <div class="extra-links">
            <el-button type="text" class="register-btn" @click="goToRegister">
              还没有账号？去注册 <i class="el-icon-arrow-right"></i>
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { handleLogin } from '@/api/user'

export default {
  name: 'LoginView',
  data() {
    return {
      isLoading: false,
      loginForm: { userAccount: "", password: "" },
      loginRules: {
        userAccount: [{ required: true, message: "请输入用户名", trigger: "blur" }],
        password: [{ required: true, message: "请输入密码", trigger: "blur" }],
      },
      // 🚀 语音助手状态对象
      voice: {
        active: false,      // 用户是否开启了助手
        isRunning: false,   // 引擎是否物理运行中（锁）
        stage: 'idle',      // 阶段：idle, account, password
        tips: '点击图标开启语音登录',
        interimText: '',
        recognition: null
      }
    };
  },
  mounted() {
    this.initVoiceEngine();
  },
  methods: {
    initVoiceEngine() {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SpeechRecognition) {
        this.voice.tips = "浏览器不支持语音识别";
        return;
      }
      this.voice.recognition = new SpeechRecognition();
      this.voice.recognition.lang = 'zh-CN';
      this.voice.recognition.interimResults = true;
      this.voice.recognition.continuous = false; // 采用手动维护重连，比原生 continuous 更稳

      // 启动成功回调
      this.voice.recognition.onstart = () => {
        this.voice.isRunning = true;
        console.log("Speech Engine: Started");
      };

      // 识别结果回调
      this.voice.recognition.onresult = (event) => {
        let result = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
          result += event.results[i][0].transcript;
        }
        this.voice.interimText = result;

        // 只有当识别结束（停顿）时才处理结果
        if (event.results[event.results.length - 1].isFinal) {
          const finalResult = result.replace(/[。\s]/g, '');
          this.processVoiceCommand(finalResult);
        }
      };

      // 错误处理回调
      this.voice.recognition.onerror = (event) => {
        console.error("Speech Engine Error:", event.error);
        if (event.error === 'no-speech') {
          this.voice.tips = "没听清，请再说一遍...";
        }
        // 注意：报错后浏览器会自动触发 onend
      };

      // 停止回调（核心锁控制）
      this.voice.recognition.onend = () => {
        this.voice.isRunning = false;
        console.log("Speech Engine: Stopped");
        
        // 如果用户没关助手且没录完，自动重启引擎
        if (this.voice.active && this.voice.stage !== 'done') {
          this.safeStart();
        }
      };
    },

    // 🚀 安全启动：解决 InvalidStateError
    safeStart() {
      if (this.voice.isRunning) return; 
      try {
        this.voice.recognition.start();
      } catch (e) {
        console.warn("Speech start conflict prevented.");
      }
    },

    speak(text) {
      const msg = new SpeechSynthesisUtterance(text);
      msg.lang = 'zh-CN';
      window.speechSynthesis.speak(msg);
    },

    toggleVoiceAssistant() {
      if (this.voice.active) {
        this.voice.active = false;
        this.voice.stage = 'idle';
        this.voice.tips = '语音助手已关闭';
        this.voice.recognition.stop();
      } else {
        this.voice.active = true;
        this.voice.stage = 'account';
        this.voice.tips = '请说出您的账号';
        this.speak("语音助手已就绪，请告诉我您的账号");
        this.safeStart();
      }
    },

    processVoiceCommand(text) {
      if (this.voice.stage === 'account') {
        this.loginForm.userAccount = text;
        this.voice.stage = 'password';
        this.voice.tips = '账号录入成功，请说密码';
        this.speak("收到，请输入密码");
      } else if (this.voice.stage === 'password') {
        this.loginForm.password = text;
        this.voice.stage = 'done';
        this.voice.tips = '识别完成，连接终端...';
        this.speak("正在连接，请稍后");
        this.submitLogin();
      }
    },

    async submitLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (!valid) return;
        this.isLoading = true;
        try {
          const res = await handleLogin(this.loginForm);
          if (res.data.success && res.data.code === '0') {
            this.speak("连接成功");
            this.$store.dispatch('user/login', res.data.data);
            this.$router.push({ name: 'index' });
          } else {
            this.speak("登录失败，" + res.data.message);
            this.voice.stage = 'account'; // 失败则重回到账号录入
            this.voice.active = true;
            this.safeStart();
          }
        } catch (e) {
          this.$message.error('网络超时，服务器内存压力过大');
        } finally {
          this.isLoading = false;
          if (this.voice.stage === 'done') this.voice.active = false;
        }
      });
    },
    goToRegister() { this.$router.push({ name: 'register' }); }
  }
};
</script>

<style lang="less" scoped>
.voice-panel {
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 25px;
    text-align: left;

    .voice-btn-group {
        display: flex;
        align-items: center;
        gap: 15px;
    }

    .voice-status-text {
        .status-tag {
            font-size: 10px;
            background: #00f2ff;
            color: #000;
            padding: 1px 4px;
            font-weight: bold;
            border-radius: 2px;
        }

        .tips {
            margin: 5px 0 0;
            color: #00f2ff;
            font-size: 13px;
        }
    }

    .interim-display {
        margin-top: 10px;
        font-style: italic;
        color: #94a3b8;
        font-size: 12px;
        border-top: 1px solid rgba(255, 255, 255, 0.05);
        padding-top: 5px;
    }
}

.pulse-red {
    animation: pulse-red-animation 1.5s infinite;
    background: #ff4949 !important;
    border-color: #ff4949 !important;
}

@keyframes pulse-red-animation {
    0% {
        box-shadow: 0 0 0 0px rgba(255, 73, 73, 0.7);
    }

    70% {
        box-shadow: 0 0 0 15px rgba(255, 73, 73, 0);
    }

    100% {
        box-shadow: 0 0 0 0px rgba(255, 73, 73, 0);
    }
}

/* ================= 变量定义 (从 App.vue 复制) ================= */
@bg-dark: #0f1219;
@primary-color: #00f2ff;
/* 赛博青色 */
@accent-color: #7d2ae8;
/* 霓虹紫 */
@text-sub: #94a3b8;
@text-main: #e2e8f0;

/* 动画颜色定义 */
@glow-color-A: #00f2ff;
@glow-color-B: #7d2ae8;

/* 🚀 关键帧动画：光晕呼吸 */
@keyframes tech-glow-pulse {
    0% {
        box-shadow: 0 0 15px fade(@glow-color-A, 50%), 0 0 30px fade(@glow-color-A, 30%);
    }

    50% {
        box-shadow: 0 0 25px fade(@glow-color-B, 70%), 0 0 45px fade(@glow-color-B, 50%);
    }

    100% {
        box-shadow: 0 0 15px fade(@glow-color-A, 50%), 0 0 30px fade(@glow-color-A, 30%);
    }
}


/* ================= 页面布局 (居中) ================= */
.login-view-container {
    display: flex;
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    /* 垂直居中 */
    min-height: 100vh;
    background-color: @bg-dark;
    position: relative;
    overflow: hidden;
    z-index: 1;
}

/* 背景纹理 */
.tech-bg-overlay {
    position: absolute;
    width: 100%;
    height: 100%;
    background:
        radial-gradient(circle at 10% 80%, rgba(125, 42, 232, 0.1) 0%, transparent 30%),
        radial-gradient(circle at 90% 20%, rgba(0, 242, 255, 0.08) 0%, transparent 30%);
    z-index: 0;
}

/* 登录卡片样式 */
.login-card-wrapper {
    width: 520px;
    padding-bottom: 20px;
    background: rgba(26, 29, 38, 0.95);
    backdrop-filter: blur(5px);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 12px;
    z-index: 10;

    /* 应用光晕动画 */
    animation: tech-glow-pulse 4s ease-in-out infinite alternate;
    box-shadow: 0 0 40px rgba(0, 0, 0, 0.8);
}

.dialog-header {
    padding: 20px 30px 15px;
    position: relative;

    .dialog-title-label {
        color: @primary-color;
        font-size: 14px;
        letter-spacing: 2px;
        font-family: 'Consolas', monospace;
        text-align: center;
        margin-bottom: 10px;
    }

    .header-deco-line {
        height: 1px;
        width: 80%;
        margin: 0 auto;
        background: linear-gradient(90deg, transparent, @primary-color, transparent);
        box-shadow: 0 0 5px @primary-color;
    }
}

.dialog-content-wrapper {
    padding: 30px;
    padding-top: 10px;
    text-align: center;

    .login-title {
        font-size: 28px;
        color: #fff;
        margin-bottom: 8px;
        font-weight: 700;
        text-shadow: 0 0 5px rgba(0, 242, 255, 0.4);
    }

    .login-subtitle {
        color: @text-sub;
        font-size: 14px;
        margin-bottom: 40px;
    }
}

/* ================= Element UI 样式覆盖 (美化) ================= */
.tech-btn {
    &.primary {
        background: linear-gradient(135deg, @primary-color, #006eff);
        box-shadow: 0 4px 15px rgba(0, 242, 255, 0.5);
        font-weight: 700;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);

        &:hover {
            transform: scale(1.02);
            box-shadow: 0 0 25px rgba(0, 242, 255, 0.8), 0 0 10px rgba(125, 42, 232, 0.5);
            background: linear-gradient(135deg, #00f2ff, #0099ff);
        }

        &:active {
            transform: scale(0.98);
            box-shadow: 0 0 5px rgba(0, 242, 255, 0.9);
        }
    }

    &.block-btn {
        width: 100%;
        height: 48px;
        font-size: 18px;
        letter-spacing: 3px;
    }
}

.tech-form {
    .el-form-item {
        margin-bottom: 25px;
    }

    .el-input__inner {
        background: rgba(0, 0, 0, 0.4);
        border: 1px solid rgba(0, 242, 255, 0.2);
        color: @text-main;
        height: 48px;
        border-radius: 4px;
        font-size: 16px;

        &:focus {
            border-color: @primary-color;
            box-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
        }
    }

    .el-input__icon {
        color: @primary-color;
        font-size: 18px;
    }

    /* 验证码容器样式 */
    .captcha-item {
        display: flex;
        align-items: flex-start;

        .captcha-input {
            flex: 1;
            margin-right: 15px;
        }

        .captcha-img-box {
            height: 48px;
            width: 100px;
            border-radius: 4px;
            overflow: hidden;
            cursor: pointer;
            border: 1px solid rgba(0, 242, 255, 0.3);
            transition: all 0.3s;
            background: rgba(0, 0, 0, 0.6);
            display: flex;
            justify-content: center;
            align-items: center;

            &:hover {
                box-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
            }

            .captcha-img {
                width: 100%;
                height: 100%;
                display: block;
                object-fit: contain;
            }

            .captcha-placeholder {
                color: @text-sub;
                font-size: 12px;
            }
        }
    }
}

/* 底部额外链接样式 (去注册) */
.extra-links {
    text-align: right;
    margin-top: 15px;

    .register-btn {
        color: @text-sub;
        font-size: 14px;
        padding: 5px 10px;
        border-radius: 4px;
        transition: color 0.3s, background 0.3s;

        &:hover {
            color: @primary-color;
            background: rgba(0, 242, 255, 0.05);
        }
    }
}
</style>