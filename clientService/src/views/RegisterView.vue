<template>
  <div class="register-view-container">
    <div class="tech-bg-overlay"></div>

    <div class="register-card-wrapper tech-dialog">
      <div class="dialog-content-wrapper">
        <h2 class="register-title">用户注册</h2>
        <p class="register-subtitle">欢迎加入 Gim-Chat 智能终端</p>

        <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="tech-form">
          <!-- 用户名 -->
          <el-form-item prop="userName">
            <el-input v-model="registerForm.userName" placeholder="请输入用户名" prefix-icon="el-icon-user"
              class="tech-input"></el-input>
          </el-form-item>

          <!-- 账号 -->
          <el-form-item prop="userAccount">
            <el-input v-model="registerForm.userAccount" placeholder="请输入账号" prefix-icon="el-icon-user"
              class="tech-input"></el-input>
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="userPassword">
            <el-input v-model="registerForm.userPassword" type="password" show-password placeholder="请输入密码"
              prefix-icon="el-icon-lock" class="tech-input"></el-input>
          </el-form-item>

          <!-- 确认密码 -->
          <el-form-item prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="确认密码"
              prefix-icon="el-icon-lock" class="tech-input"></el-input>
          </el-form-item>

          <!-- 提交按钮 -->
          <el-form-item class="register-action-item">
            <el-button type="primary" @click="submitRegister" class="tech-btn block-btn primary" :loading="isLoading">
              {{ isLoading ? '注册中...' : '注 册' }} <i class="el-icon-right"></i>
            </el-button>

            <div class="extra-links">
              <span class="tip-text">已有账号？</span>
              <el-button type="text" @click="goToLogin" class="login-link-btn">
                立即登录
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { handleRgister } from '@/api/user'; // 注册逻辑接口

export default {
  name: 'RegisterView',
  data() {
    return {
      isLoading: false, // 注册加载状态
      registerForm: {
        userAccount: "",
        userPassword: "",
        confirmPassword: "", // 确认密码字段
        userName: "",
      },
      registerRules: {
        userAccount: [
          { required: true, message: "请输入账号", trigger: "blur" },
        ],
        userPassword: [
          { required: true, message: "请输入密码", trigger: "blur" },
          { min: 6, message: "密码长度不能少于 6 位", trigger: "blur" }
        ],
        confirmPassword: [
          { required: true, message: "请确认密码", trigger: "blur" },
          { validator: this.checkPassword, trigger: "blur" }
        ],
        userName: [
          { required: true, message: "请输入用户名", trigger: "blur" },
        ],
      },
    };
  },
  methods: {
    goToLogin() {
      this.$router.push({ name: "login" }); // 跳转到登录页面
    },
    // 自定义密码验证：确认密码和密码一致性
    checkPassword(rule, value, callback) {
      if (value !== this.registerForm.userPassword) {
        callback(new Error("两次输入的密码不一致"));
      } else {
        callback();
      }
    },

    // 提交注册
    async submitRegister() {
      this.$refs.registerForm.validate(async (valid) => {
        if (valid) {
          this.isLoading = true;
          const param = { ...this.registerForm };

          try {

            const res = await handleRgister(param);
            console.log('resData=', res)
            if (res.data.success && res.data.code === '0') {
              this.$message.success("注册成功，欢迎加入！");
              this.$router.push({ name: "login" }); // 跳转到登录页面
            } else {
              this.$message.error(res.data.message || "注册失败");
            }
          } catch (e) {
            console.error(e);
            this.$message.error("网络错误或服务异常");
          } finally {
            this.isLoading = false;
          }
        }
      });
    }
  }
};
</script>

<style lang="less" scoped>
@bg-dark: #0f1219;
/* 定义你想要的背景色 */
@primary-color: #00f2ff;
@accent-color: #7d2ae8;
@text-sub: #94a3b8;
@text-main: #e2e8f0;
/* ================= 样式 (与登录页面一致) ================= */

/* 背景 */
.register-view-container {
  display: flex;
  justify-content: center;
  align-items: center;
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
  background: radial-gradient(circle at 10% 80%, rgba(125, 42, 232, 0.1) 0%, transparent 30%),
    radial-gradient(circle at 90% 20%, rgba(0, 242, 255, 0.08) 0%, transparent 30%);
  z-index: 0;
}

/* 注册卡片样式 */
.register-card-wrapper {
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

/* 注册标题和副标题 */
.dialog-content-wrapper {
  padding: 30px;
  padding-top: 10px;
  text-align: center;

  .register-title {
    font-size: 28px;
    color: #fff;
    margin-bottom: 8px;
    font-weight: 700;
    text-shadow: 0 0 5px rgba(0, 242, 255, 0.4);
  }

  .register-subtitle {
    color: @text-sub;
    font-size: 14px;
    margin-bottom: 40px;
  }
}

/* ================= Element UI 样式覆盖 ================= */
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
}

/* 底部链接样式 (去登录) */
.extra-links {
  text-align: center;
  /* 居中显示 */
  margin-top: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;

  .tip-text {
    color: @text-sub;
    font-size: 14px;
  }

  .login-link-btn {
    color: @primary-color;
    font-size: 14px;
    font-weight: 700;
    padding: 0;
    text-decoration: underline;
    /* 增加下划线强化链接感 */
    transition: all 0.3s;

    &:hover {
      color: lighten(@primary-color, 15%);
      text-shadow: 0 0 8px rgba(0, 242, 255, 0.6);
    }
  }
}
</style>
