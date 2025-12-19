<template>
  <div id="app">
    <div class="tech-bg"></div>

    <header class="app-header" v-if="showLayout">
      <div class="header-left">
        <div class="logo-container">
          <span class="logo-text">Gim-Chat <span class="logo-suffix">PRO</span></span>
        </div>
      </div>

      <div class="header-right">
        <div class="user-info-panel">
          <template v-if="isLogin">
            <div class="avatar-wrapper">
              <img :src="userInfo.avatarUrl" alt="头像" class="user-avatar" />
              <div class="status-dot"></div>
            </div>
            <span class="username">{{ userInfo.userName }}</span>
            <el-button type="text" class="logout-btn" @click="doLogout">
              <i class="el-icon-switch-button"></i> 退出
            </el-button>
          </template>
          <template v-else>
            <el-button class="tech-btn primary" @click="doLogin" size="small">立即登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <div class="app-body">
      <aside class="sidebar" :class="{ 'collapsed': isSidebarCollapsed }" v-if="showLayout">
        <div class="nav-scroll">
          <ul class="nav-list">
            <li><router-link to="#" class="nav-item">
                <div class="toggle-icon-btn" @click="toggleSidebar">
                  <i :class="isSidebarCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
                </div>
              </router-link></li>
            <li><router-link to="/index" class="nav-item">
                <span class="icon">🏠</span> <span class="text">首页</span>
              </router-link></li>
               <li><router-link to="/chatRoom" class="nav-item">
                <span class="icon">📚</span> <span class="text">聊天室</span>
              </router-link></li>
          </ul>
        </div>
      </aside>
      <main class="main-view-container">
        <div class="main-view-content">
          <transition name="fade-transform" mode="out-in">
            <router-view />
          </transition>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import { handleLogin, handleLogout } from '@/api/user'
import WebSocketService from '@/plugins/ws';
import Vue from 'vue'

export default {
  components: {  },
  data() {
    return {
      defaultAvatar: 'https://iknow-pic.cdn.bcebos.com/962bd40735fae6cd17bafbff1db30f2442a70f25',

      isSidebarCollapsed: false,
    };
  },
  mounted() {
    window.addEventListener('beforeunload', this.clearLoginState);
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.clearLoginState);
  },
  computed: {
    showLayout() {
      // 假设你的登录页面的路由名称是 'login'
      // 只有当前路由名称不是 'login' 时，才显示头部和侧边栏
      return this.$route.name !== 'login';
    },
    isLogin() {
      return this.$store.getters['user/isLogin'];
    },
    userInfo() {
      return this.$store.state.user.userInfo || {}
    }
  },
  methods: {
    /**
     * 切换侧边栏状态的方法
     */
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed;
    },
    clearLoginState() {
      if (performance.getEntriesByType('navigation')[0].type !== 'reload') {
        this.$store.dispatch('user/logout');
      }
    },
    async doLogin() {
      this.$router.push({ name: 'login' });
    },
    async doLogout() {

      const param = {
        userId: this.userInfo.userId
      }
      try {
        const res = await handleLogout(param)
        if (res && res.data && res.data.code === 200) {
          this.$store.dispatch('user/logout')
        } else {
          this.$message.error(res.data.message);
          this.$router.push({
            name: 'login'
          });
        }
      } finally {
        this.$message.success('已安全断开连接');
        this.$router.push({
          name: 'login'
        });
      }
    },
    initWebSocket(userId) {
      if (this.wsService) {
        this.wsService.close();
      }
      this.wsService = new WebSocketService(this.$store);
      this.wsService.connect(userId);
      this.$ws = this.wsService;
      Vue.prototype.$ws = this.wsService;
      this.$ws.send({})
    }
  },
};
</script>

<style lang="less" scoped>
/* ================= 变量定义 ================= */
@bg-dark: #0f1219;
@bg-panel: #1a1f2c;
@primary-color: #00f2ff;
/* 赛博青色 */
@accent-color: #7d2ae8;
/* 霓虹紫 */
@text-main: #e2e8f0;
@text-sub: #94a3b8;
@glass-bg: rgba(20, 25, 40, 0.75);
@border-color: rgba(255, 255, 255, 0.08);

/* 动画颜色定义 */
@glow-color-A: #00f2ff;
/* 青色 */
@glow-color-B: #7d2ae8;
/* 紫色 */

/* 🚀 关键帧动画 1：主内容区域四周光圈流动/呼吸效果 (优化为内发光) */
@keyframes tech-glow-flow {
  0% {
    /* 青色微弱光晕 (内发光 + 外发光) */
    box-shadow:
      inset 0 0 5px fade(@glow-color-A, 40%),
      /* 新增内发光，提供边界感 */
      0 0 15px fade(@glow-color-A, 60%);
    filter: drop-shadow(0 0 6px fade(@glow-color-A, 80%));
  }

  50% {
    /* 紫色增强光晕 */
    box-shadow:
      inset 0 0 8px fade(@glow-color-B, 50%),
      0 0 25px fade(@glow-color-B, 80%);
    filter: drop-shadow(0 0 10px fade(@glow-color-B, 90%));
  }

  100% {
    /* 循环回到青色 */
    box-shadow:
      inset 0 0 5px fade(@glow-color-A, 40%),
      0 0 15px fade(@glow-color-A, 60%);
    filter: drop-shadow(0 0 6px fade(@glow-color-A, 80%));
  }
}

/* 🚀 关键帧动画 2：顶部导航栏底部光圈流动 */
@keyframes tech-glow-flow-bottom {
  0% {
    /* 青色微弱光晕，向下偏移 */
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.4),
      /* 保持基础阴影深度 */
      0 2px 10px 1px fade(@glow-color-A, 60%);
    /* 底部青色发光 */
  }

  50% {
    /* 紫色增强光晕 */
    box-shadow: 0 6px 15px rgba(0, 0, 0, 0.6),
      /* 增强基础阴影深度 */
      0 3px 15px 2px fade(@glow-color-B, 80%);
    /* 底部紫色发光 */
  }

  100% {
    /* 循环回到青色 */
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.4),
      0 2px 10px 1px fade(@glow-color-A, 60%);
  }
}

/* 🚀 关键帧动画 3：侧边栏柔和流动光圈 */
@keyframes sidebar-glow {
  0% {
    /* 青色柔和光晕 (主要在右侧，作为分隔线) */
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.5),
      /* 右侧基础阴影 */
      0 0 5px 1px fade(@glow-color-A, 40%);
    /* 整体柔和青色光晕 */
  }

  50% {
    /* 紫色增强光晕 */
    box-shadow: 4px 0 15px rgba(0, 0, 0, 0.7),
      /* 右侧增强阴影 */
      0 0 8px 1.5px fade(@glow-color-B, 50%);
    /* 整体柔和紫色光晕 */
  }

  100% {
    /* 循环回到青色 */
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.5),
      0 0 5px 1px fade(@glow-color-A, 40%);
  }
}


/* ================= 全局布局 ================= */
#app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
  background-color: @bg-dark;
  color: @text-main;
  overflow: hidden;
  position: relative;
}

/* 背景纹理 */
.tech-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  background:
    radial-gradient(circle at 15% 50%, rgba(125, 42, 232, 0.08) 0%, transparent 25%),
    radial-gradient(circle at 85% 30%, rgba(0, 242, 255, 0.05) 0%, transparent 25%);
  pointer-events: none;
}

/* ================= 顶部导航栏 ================= */
.app-header {
  height: 64px;
  background: @glass-bg;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid @border-color;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;

  .header-left {
    display: flex;
    align-items: center; // 确保按钮垂直居中
  }

  .logo-container {
    display: flex;
    align-items: center;
    cursor: pointer;

    .logo-icon {
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, @primary-color, @accent-color);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 900;
      color: #fff;
      margin-right: 12px;
      box-shadow: 0 0 10px rgba(0, 242, 255, 0.3);
    }

    .logo-text {
      font-size: 20px;
      font-weight: 700;
      letter-spacing: 0.5px;
      color: #fff;

      .logo-suffix {
        font-size: 10px;
        background: rgba(255, 255, 255, 0.1);
        padding: 2px 6px;
        border-radius: 4px;
        margin-left: 4px;
        color: @primary-color;
      }
    }
  }

  .user-info-panel {
    display: flex;
    align-items: center;
    gap: 16px;

    .avatar-wrapper {
      position: relative;

      .user-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        border: 2px solid rgba(255, 255, 255, 0.1);
        transition: all 0.3s;

        &:hover {
          border-color: @primary-color;
        }
      }

      .status-dot {
        position: absolute;
        bottom: 0;
        right: 0;
        width: 10px;
        height: 10px;
        background: #10b981;
        border: 2px solid @bg-panel;
        border-radius: 50%;
      }
    }

    .username {
      font-size: 14px;
      font-weight: 500;
    }

    .logout-btn {
      color: @text-sub;

      &:hover {
        color: #ff4d4f;
      }
    }
  }
}


/* ================= 主体布局 ================= */
.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  z-index: 10;

  border: 1px solid rgba(56, 189, 248, 0.3);
  /* 移除静态边框 */

  /* 🚀 默认样式（基础光圈和深度阴影） */
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.8);
  /* 底部深度阴影 */

  width: 100%;
  /* 确保它占据全部宽度 */
}

/* ================= 左侧侧边栏 ================= */
/* 1. 定义关键帧动画 */
@keyframes neon-pulse {
  0% {
    /* 青色光晕 */
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.6),
      0 0 20px rgba(0, 242, 255, 0.4);
  }

  50% {
    /* 紫色增强光晕 */
    box-shadow: 0 0 15px rgba(125, 42, 232, 0.8),
      0 0 30px rgba(125, 42, 232, 0.6);
  }

  100% {
    /* 循环回到青色 */
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.6),
      0 0 20px rgba(0, 242, 255, 0.4);
  }
}

.sidebar {
  width: 220px;
  background: rgba(18, 22, 33, 0.6);
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  padding: 20px 12px;
  // transition: all 0.3s ease;
  transition: transform 0.3s ease;
  z-index: 20;
  /* 确保在 app-body 的光晕之上 */

  position: relative;

  /* 🚀 应用侧边栏流动光圈动画 */
  animation: sidebar-glow 4s ease-in-out infinite alternate;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
  /* 基础深度阴影 */

  .nav-scroll {
    flex: 1;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 0;
    }
  }

  .nav-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      margin-bottom: 8px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      color: @text-sub;
      text-decoration: none;
      border-radius: 12px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      font-size: 15px;
      position: relative;
      overflow: hidden;

      .icon {
        margin-right: 12px;
        font-size: 18px;
        filter: grayscale(100%);
        transition: 0.3s;
      }

      .nav-img-icon {
        width: 20px;
        height: 20px;
        margin-right: 12px;
        filter: grayscale(100%);
        transition: 0.3s;
      }

      /* 悬停效果 */
      &:hover {
        background: rgba(255, 255, 255, 0.03);
        color: @text-main;

        .icon,
        .nav-img-icon {
          filter: grayscale(0);
          transform: scale(1.1);
        }
      }

      /* 激活状态 */
      &.router-link-active {
        background: linear-gradient(90deg, rgba(0, 242, 255, 0.1) 0%, transparent 100%);
        color: @primary-color;
        font-weight: 600;

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 15%;
          bottom: 15%;
          width: 3px;
          background: @primary-color;
          border-radius: 0 4px 4px 0;
          box-shadow: 0 0 8px @primary-color;
        }

        .icon,
        .nav-img-icon {
          filter: grayscale(0);
        }
      }

      /* AI 特殊菜单 */
      &.ai-special {
        &.router-link-active {
          color: @accent-color;

          &::before {
            background: @accent-color;
            box-shadow: 0 0 8px @accent-color;
          }

          background: linear-gradient(90deg, rgba(125, 42, 232, 0.1) 0%, transparent 100%);
        }
      }
    }
  }

  .sidebar-footer {
    padding-top: 20px;
    border-top: 1px solid @border-color;

    .setting-link {
      display: flex;
      align-items: center;
      justify-content: center;
      color: @text-sub;
      text-decoration: none;
      font-size: 14px;
      padding: 10px;
      border-radius: 8px;
      transition: 0.3s;

      &:hover {
        background: rgba(255, 255, 255, 0.05);
        color: @text-main;
      }

      i {
        margin-right: 6px;
      }
    }
  }
}

/* 将按钮定位到右上角 */
.toggle-icon-btn {
  // position: absolute;
  top: 10px;
  /* 距离顶部 10px */
  right: 10px;
  /* 距离右侧 10px */
  cursor: pointer;
  font-size: 24px;
  color: @text-sub;
  transition: all 0.3s;
}

.toggle-icon-btn:hover {
  color: @primary-color;
  /* 鼠标悬停时颜色变化 */
  transform: scale(1.1);
  /* 增加鼠标悬停时的缩放效果 */
}

.sidebar.collapsed {
  width: 89px;
  /* 收缩时宽度 */

  // transform: translateX(-140px); /* 收缩时宽度 */

  transform: scaleX(1);
  /* 通过缩放来实现收缩效果 */
  transform-origin: left center;
  /* 确保从左侧收缩 */

}

.nav-item {
  /* 收缩后，隐藏文本 */
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: @text-sub;
  text-decoration: none;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 15px;
  position: relative;
  overflow: hidden;

  .text {
    display: block;
    transition: opacity 0.3s ease;
  }

  .collapsed & .text {
    display: none;
  }
}

/* ================= 主内容容器 ================= */
.main-view-container {
  flex: 1;
  padding: 20px;
  overflow: hidden;
  position: relative;

  /* 通过 flex-grow 来填充剩余空间 */
  flex-grow: 1;
  transition: all 0.3s ease;
  /* 平滑过渡 */

  /* 🚀 新增全屏样式 */
  &.is-fullscreen {
    padding: 0; // 移除 app-body 的 padding

    .main-view-content {
      border-radius: 0; // 移除圆角，实现全屏
      background: @bg-dark; // 使用暗色背景，与 App.vue 的 body 背景保持一致
      border: none; // 移除边框
      box-shadow: none; // 移除阴影
      width: 100%;
      height: 100%;
    }
  }
}

.main-view-content {
  width: 100%;
  height: 100%;
  background: rgba(30, 34, 45, 0.5);
  /* 半透明内容背景 */
  border-radius: 16px;
  border: 1px solid @border-color;
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.2);
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;

  /* 自定义滚动条 */
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

/* ================= 动画 ================= */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-transform-enter {
  opacity: 0;
  transform: translateY(10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ================= Element UI 深度覆盖 (Dark Mode) ================= */
/* 按钮样式 */
.tech-btn {
  &.primary {
    background: linear-gradient(135deg, #006eff, #00f2ff);
    border: none;
    box-shadow: 0 4px 15px rgba(0, 242, 255, 0.3);
    font-weight: 600;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(0, 242, 255, 0.5);
    }
  }

  &.block-btn {
    width: 100%;
    height: 42px;
    font-size: 16px;
    letter-spacing: 2px;
  }
}

/* 弹窗样式覆盖 */
::v-deep .tech-dialog {
  background: #1a1d26;
  border: 1px solid rgba(0, 242, 255, 0.2);
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.6);
  border-radius: 12px;

  .el-dialog__header {
    padding: 20px 20px 10px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);

    .el-dialog__title {
      color: @primary-color;
      font-size: 14px;
      letter-spacing: 1px;
      font-family: monospace;
    }

    .el-dialog__close {
      color: @text-sub;

      &:hover {
        color: #fff;
      }
    }
  }

  .el-dialog__body {
    padding: 30px;
    color: @text-main;
  }
}

/* 登录表单内部 */
.dialog-content-wrapper {
  text-align: center;

  .login-title {
    font-size: 24px;
    color: #fff;
    margin-bottom: 8px;
    font-weight: 600;
  }

  .login-subtitle {
    color: @text-sub;
    font-size: 13px;
    margin-bottom: 30px;
  }
}

::v-deep .tech-form {
  .el-form-item__label {
    color: @text-sub;
  }

  /* 输入框样式覆盖 */
  .el-input__inner {
    background: rgba(0, 0, 0, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #fff;
    height: 44px;
    border-radius: 8px;

    &:focus {
      border-color: @primary-color;
      box-shadow: 0 0 0 2px rgba(0, 242, 255, 0.1);
    }
  }

  .el-input__icon {
    color: @text-sub;
  }
}
</style>