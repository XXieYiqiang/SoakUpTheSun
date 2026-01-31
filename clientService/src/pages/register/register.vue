<script lang="ts" setup>
import { t } from '@/locale/index'

defineOptions({
  name: 'Register',
})
definePage({
  style: {
    navigationBarTitleText: '注册',
    navigationStyle: 'custom',
  },
})

const formData = reactive({
  account: '', // 账号
  password: '', // 密码
  username: '', // 用户名
  bio: '', // 简介
  avatar: '', // 头像 base64
})

const loading = ref(false)
const showPassword = ref(false)

// 压缩图片并转Base64
async function compressAndToBase64(tempFilePath: string): Promise<string> {
  // #ifdef H5
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.src = tempFilePath
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('Canvas context not available'))
        return
      }
      canvas.width = img.width
      canvas.height = img.height
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      const base64 = canvas.toDataURL('image/jpeg', 0.8) // 0.8 quality
      resolve(base64)
    }
    img.onerror = reject
  })
  // #endif
}

function handleChooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      try {
        uni.showLoading({ title: '处理中...' })
        const tempFilePath = res.tempFilePaths[0]
        const base64 = await compressAndToBase64(tempFilePath)
        formData.avatar = base64
        uni.hideLoading()
      } catch (error) {
        uni.hideLoading()
        uni.showToast({
          title: '图片处理失败',
          icon: 'none'
        })
        console.error(error)
      }
    }
  })
}

function validateForm() {
  if (!formData.avatar) {
    uni.showToast({ title: '请上传头像', icon: 'none' })
    return false
  }
  if (!formData.account) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return false
  }
  if (!formData.username) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return false
  }
  if (!formData.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }
  return true
}

import { register } from '@/api/login'

async function handleRegister() {
  if (!validateForm()) return

  loading.value = true
  try {
    const res:any = await register({
      userAccount: formData.account,
      userPassword: formData.password,
      userName: formData.username,
      userAvatar: formData.avatar,
      userProfile: formData.bio,
    })

    if (res.code === 200 || res.code === 0) { // 根据实际后端返回code判断
      uni.showToast({
        title: '注册成功',
        icon: 'success'
      })
      
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    } else {
      uni.showToast({
        title: res.msg || '注册失败',
        icon: 'none'
      })
    }
  } catch (error: any) {
    uni.showToast({
      title: error.message || '注册失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function handleLoginLink() {
  uni.navigateBack()
}

function togglePassword() {
  showPassword.value = !showPassword.value
}
</script>

<template>
  <view class="register-container">
    <view class="safe-area-inset-top" />

    <view class="register-content">
      <view class="logo-section">
        <view class="app-name">账号注册</view>
        <view class="app-desc">创建一个新账号以开始使用</view>
      </view>

      <view class="form-section">
        <!-- 头像上传 -->
        <view class="avatar-upload" @click="handleChooseAvatar">
          <view v-if="formData.avatar" class="avatar-preview">
            <image :src="formData.avatar" mode="aspectFill" class="avatar-image" />
            <view class="avatar-edit-mask">
              <u-icon name="camera" size="24" color="#fff" />
            </view>
          </view>
          <view v-else class="avatar-placeholder">
            <u-icon name="camera" size="40" color="#999" />
            <text class="upload-text">上传头像</text>
          </view>
        </view>

        <!-- 表单输入 -->
        <view class="input-group">
          <view class="input-wrapper">
            <view class="input-icon">
              <u-icon name="account" size="36" color="#999" />
            </view>
            <input
              v-model="formData.account"
              class="input-field"
              placeholder="请输入账号"
              :disabled="loading"
            >
          </view>
        </view>

        <view class="input-group">
          <view class="input-wrapper">
            <view class="input-icon">
              <u-icon name="edit-pen" size="36" color="#999" />
            </view>
            <input
              v-model="formData.username"
              class="input-field"
              placeholder="请输入用户名"
              :disabled="loading"
            >
          </view>
        </view>

        <view class="input-group">
          <view class="input-wrapper">
            <view class="input-icon">
              <u-icon name="lock" size="36" color="#999" />
            </view>
            <input
              v-model="formData.password"
              :password="!showPassword"
              class="input-field"
              placeholder="请输入密码"
              :disabled="loading"
            >
            <view class="password-toggle" @click="togglePassword">
              <u-icon
                :name="showPassword ? 'eye-off' : 'eye'"
                size="36"
                color="#999"
              />
            </view>
          </view>
        </view>

        <view class="input-group">
          <view class="input-wrapper textarea-wrapper">
            <view class="input-icon top-align">
              <u-icon name="file-text" size="36" color="#999" />
            </view>
            <textarea
              v-model="formData.bio"
              class="input-field textarea-field"
              placeholder="请输入个人简介"
              :disabled="loading"
              :maxlength="200"
            />
          </view>
        </view>

        <view
          class="register-btn"
          :class="{ loading }"
          @click="handleRegister"
        >
          <text v-if="loading">注册中...</text>
          <text v-else>立即注册</text>
        </view>

        <view class="login-link">
          <text class="login-text" @click="handleLoginLink">
            已有账号？立即登录
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.register-container {
  width: 100%;
  height: 100vh;
  background:
    linear-gradient(180deg, rgba(126, 196, 194, 0.85) 0%, rgba(93, 153, 151, 0.85) 100%),
    url('@/static/images/blindway.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -50%;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
    pointer-events: none;
  }

  .safe-area-inset-top {
    height: constant(safe-area-inset-top);
    height: env(safe-area-inset-top);
  }

  .register-content {
    width: 100%;
    height: 100%;
    padding: 40rpx 48rpx;
    box-sizing: border-box;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .logo-section {
    text-align: center;
    margin-bottom: 60rpx;
    margin-top: -40rpx;

    .app-name {
      font-size: 48rpx;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 20rpx;
      letter-spacing: 4rpx;
      text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
    }

    .app-desc {
      font-size: 28rpx;
      color: rgba(255, 255, 255, 0.9);
      letter-spacing: 2rpx;
      font-weight: 400;
    }
  }

  .form-section {
    position: relative;
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(40rpx);
    -webkit-backdrop-filter: blur(40rpx);
    border-radius: 32rpx;
    padding: 60rpx 48rpx;
    box-shadow:
      0 24rpx 80rpx rgba(0, 0, 0, 0.12),
      0 8rpx 24rpx rgba(0, 0, 0, 0.08),
      inset 0 1rpx 0 rgba(255, 255, 255, 0.8);
    border: 1rpx solid rgba(255, 255, 255, 0.6);

    .avatar-upload {
      width: 160rpx;
      height: 160rpx;
      margin: 0 auto 60rpx;
      position: relative;
      border-radius: 50%;
      background: #f8f9fb;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
      cursor: pointer;
      border: 4rpx solid #fff;

      .avatar-preview {
        width: 100%;
        height: 100%;
        border-radius: 50%;
        overflow: hidden;
        position: relative;

        .avatar-image {
          width: 100%;
          height: 100%;
        }

        .avatar-edit-mask {
          position: absolute;
          bottom: 0;
          left: 0;
          width: 100%;
          height: 40rpx;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }

      .avatar-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        border-radius: 50%;

        .upload-text {
          font-size: 20rpx;
          color: #999;
          margin-top: 8rpx;
        }
      }
    }

    .input-group {
      margin-bottom: 32rpx;

      .input-wrapper {
        display: flex;
        align-items: center;
        background: #f8f9fb;
        border-radius: 20rpx;
        padding: 0 28rpx;
        height: 96rpx;
        position: relative;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        border: 2rpx solid transparent;

        &.textarea-wrapper {
          height: auto;
          min-height: 96rpx;
          padding-top: 24rpx;
          padding-bottom: 24rpx;
          align-items: flex-start;
        }

        &:focus-within {
          background: #ffffff;
          border-color: #5d9997;
          box-shadow:
            0 0 0 6rpx rgba(93, 153, 151, 0.08),
            0 4rpx 12rpx rgba(93, 153, 151, 0.12);
          transform: translateY(-2rpx);
        }

        .input-icon {
          font-size: 32rpx;
          margin-right: 20rpx;
          opacity: 0.5;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: opacity 0.3s ease;

          &.top-align {
            margin-top: 6rpx;
          }
        }

        .input-field {
          flex: 1;
          height: 100%;
          font-size: 30rpx;
          color: #2c3e50;
          background: transparent;
          outline: none;
          font-weight: 400;

          &.textarea-field {
            width: 100%;
            height: 160rpx;
            line-height: 1.5;
            padding-top: 0;
          }

          &::placeholder {
            color: #a0aec0;
            font-weight: 400;
          }
        }

        .password-toggle {
          padding: 16rpx;
          margin: -16rpx;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          opacity: 0.5;
          transition: opacity 0.3s ease;

          &:active {
            opacity: 0.8;
          }
        }
      }
    }

    .register-btn {
      width: 100%;
      height: 96rpx;
      background: linear-gradient(135deg, #7ec4c2 0%, #5d9997 100%);
      border-radius: 20rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 34rpx;
      font-weight: 600;
      color: #ffffff;
      box-shadow:
        0 8rpx 24rpx rgba(93, 153, 151, 0.25),
        0 4rpx 8rpx rgba(93, 153, 151, 0.15);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      cursor: pointer;
      letter-spacing: 4rpx;
      margin-top: 60rpx;

      &:active {
        transform: scale(0.97);
        box-shadow: 0 4rpx 12rpx rgba(93, 153, 151, 0.2);
      }

      &.loading {
        opacity: 0.7;
        cursor: not-allowed;
      }
    }

    .login-link {
      text-align: center;
      margin-top: 32rpx;

      .login-text {
        font-size: 26rpx;
        color: #5d9997;
        cursor: pointer;
        font-weight: 500;
        transition: opacity 0.3s ease;

        &:active {
          opacity: 0.7;
        }
      }
    }
  }
}
</style>
