<script lang="ts" setup>
import { t } from '@/locale/index'

defineOptions({
  name: 'Login',
})
definePage({
  style: {
    navigationBarTitleText: '%tabbar.home%',
    navigationStyle: 'custom',
  },
})

const formData = reactive({
  phone: '',
  password: '',
  rememberMe: true,
})

const loading = ref(false)
const showPassword = ref(false)
const isVoiceListening = ref(false)
const isVoiceProcessing = ref(false)

const STORAGE_KEY = 'login_credentials'

function loadSavedCredentials() {
  try {
    const saved = uni.getStorageSync(STORAGE_KEY)
    if (saved && saved.phone && saved.password) {
      formData.phone = saved.phone
      formData.password = saved.password
      formData.rememberMe = true
    }
  }
  catch (error) {
    console.log('读取保存的登录信息失败:', error)
  }
}

function saveCredentials() {
  try {
    if (formData.rememberMe) {
      uni.setStorageSync(STORAGE_KEY, {
        phone: formData.phone,
      })
    }
    else {
      uni.removeStorageSync(STORAGE_KEY)
    }
  }
  catch (error) {
    console.log('保存登录信息失败:', error)
  }
}

onLoad(() => {
  loadSavedCredentials()
})

const rules = {
  phone: {
    required: true,
    message: '请输入手机号',
    trigger: 'blur',
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: 'blur',
  },
}

function validateForm() {
  if (!formData.phone) {
    uni.showToast({
      title: t('login.phoneRequired'),
      icon: 'none',
      duration: 2000,
    })
    return false
  }
  if (!formData.password) {
    uni.showToast({
      title: t('login.passwordRequired'),
      icon: 'none',
      duration: 2000,
    })
    return false
  }
  return true
}

async function handleLogin() {
  if (!validateForm())
    return

    setTimeout(() => {
      uni.switchTab({
        url: '/pages/index/index',
      })
    }, 1000)

  // loading.value = true
  // try {
  //   const res:any = await userLogin({
  //     userAccount: formData.phone,
  //     password: formData.password,
  //   })

  //   if (res.code === 200 || res.code === 0) { // 根据后端实际返回调整
  //     saveCredentials()
  //     // 假设 res.data 包含 token 等信息，这里可以保存到 store 或 storage
  //     // if (res.data?.token) uni.setStorageSync('token', res.data.token)

  //     uni.showToast({
  //       title: t('login.loginSuccess'),
  //       icon: 'success',
  //       duration: 1000,
  //     })
  //     setTimeout(() => {
  //       uni.switchTab({
  //         url: '/pages/index/index',
  //       })
  //     }, 300)
  //   } else {
  //     uni.showToast({
  //       title: res.msg || t('login.loginFailed'),
  //       icon: 'none',
  //       duration: 2000,
  //     })
  //   }
  // }
  // catch (error: any) {
  //   uni.showToast({
  //     title: error.message || t('login.loginFailed'),
  //     icon: 'none',
  //     duration: 2000,
  //   })
  // }
  // finally {
  //   loading.value = false
  // }
}

function togglePassword() {
  showPassword.value = !showPassword.value
}

function handleForgotPassword() {
  uni.showToast({
    title: t('login.forgotPasswordDeveloping'),
    icon: 'none',
    duration: 2000,
  })
}

function handleRegister() {
  uni.navigateTo({
    url: '/pages/register/register'
  })
}

function toggleRememberMe() {
  if (!loading.value) {
    formData.rememberMe = !formData.rememberMe
    if (!formData.rememberMe) {
      try {
        uni.removeStorageSync(STORAGE_KEY)
      }
      catch (error) {
        console.log('清除登录信息失败:', error)
      }
    }
  }
}

function parseVoiceContent(text: string) {
  const normalizedText = text.toLowerCase().replace(/\s+/g, '')

  let phone = ''
  let password = ''

  const phoneMatch = normalizedText.match(/账号(\d+)/) || normalizedText.match(/account(\d+)/)
  const passwordMatch = normalizedText.match(/密码(\d+)/) || normalizedText.match(/password(\d+)/)

  if (phoneMatch) {
    phone = phoneMatch[1]
  }

  if (passwordMatch) {
    password = passwordMatch[1]
  }

  return { phone, password }
}

import { getBaiduToken, recognizeSpeech, userLogin } from '@/api/login'
import { H5Recorder } from '@/utils/recorder'

const APIKEY = import.meta.env.VITE_APP_BAIDU_SPEECH_APIKEY
const SECRETKEY = import.meta.env.VITE_APP_BAIDU_SPEECH_SECRETKEY
const BAIDU_TOKEN_KEY = 'baidu_access_token'

let voiceTimer: ReturnType<typeof setTimeout> | null = null

// #ifdef H5
let recorder: H5Recorder | null = null
// #endif

// #ifdef APP-PLUS
const recorderManager = uni.getRecorderManager()
// #endif

async function getBaiduAccessToken() {
  const savedToken = uni.getStorageSync(BAIDU_TOKEN_KEY)
  if (savedToken) {
    return savedToken
  }

  try {
    const res: any = await getBaiduToken({
      grant_type: 'client_credentials',
      client_id: APIKEY,
      client_secret: SECRETKEY,
    })

    if (res.data && res.data.access_token) {
      uni.setStorageSync(BAIDU_TOKEN_KEY, res.data.access_token)
      return res.data.access_token
    }
    throw new Error('获取百度Token失败')
  }
  catch (error) {
    console.error('getBaiduAccessToken error:', error)
    throw error
  }
}

// #ifdef H5
async function startH5Recording() {
  try {
    recorder = new H5Recorder()
    await recorder.start()

    isVoiceListening.value = true
    uni.showToast({
      title: '正在录音，再次点击停止（30秒自动停止）',
      icon: 'none',
    })

    if (voiceTimer)
      clearTimeout(voiceTimer)
    voiceTimer = setTimeout(() => {
      if (isVoiceListening.value) {
        stopH5Recording()
        uni.showToast({
          title: '录音已自动停止',
          icon: 'none',
        })
      }
    }, 30000)
  }
  catch (error: any) {
    console.error('H5录音失败:', error)
    uni.showToast({
      title: error.message || '无法访问麦克风',
      icon: 'none',
    })
    isVoiceListening.value = false
  }
}

async function stopH5Recording() {
  if (voiceTimer) {
    clearTimeout(voiceTimer)
    voiceTimer = null
  }

  if (!recorder) return

  isVoiceListening.value = false
  isVoiceProcessing.value = true

  try {
    const { base64, len } = await recorder.stop()
    recorder = null
    await processVoice(base64, len)
  }
  catch (error: any) {
    console.error('语音识别错误:', error)
    uni.showToast({
      title: error.message || t('login.voiceFailed'),
      icon: 'none',
    })
    isVoiceProcessing.value = false
  }
}
// #endif

// #ifdef APP-PLUS
function startAppRecording() {
  isVoiceListening.value = true
  uni.showToast({
    title: '正在录音，再次点击停止（30秒自动停止）',
    icon: 'none',
  })

  // 监听录音停止
  recorderManager.onStop(async (res) => {
    isVoiceListening.value = false
    if (voiceTimer) {
      clearTimeout(voiceTimer)
      voiceTimer = null
    }
    
    isVoiceProcessing.value = true
    try {
      const fs = uni.getFileSystemManager()
      const base64 = fs.readFileSync(res.tempFilePath, 'base64')
      const fileInfo = await new Promise<UniApp.GetFileInfoSuccess>((resolve, reject) => {
         uni.getFileInfo({
            filePath: res.tempFilePath,
            success: resolve,
            fail: reject
         })
      })
      await processVoice(base64 as string, fileInfo.size)
    } catch (error: any) {
       console.error('APP录音处理失败:', error)
       uni.showToast({
         title: error.message || '录音处理失败',
         icon: 'none',
       })
       isVoiceProcessing.value = false
    }
  })

  // 监听错误
  recorderManager.onError((err) => {
    console.error('录音错误:', err)
    isVoiceListening.value = false
    if (voiceTimer) {
      clearTimeout(voiceTimer)
      voiceTimer = null
    }
    uni.showToast({
      title: '录音失败',
      icon: 'none',
    })
  })

  recorderManager.start({
    format: 'pcm', // 百度语音识别支持 pcm
    sampleRate: 16000, // 采样率 16k
    numberOfChannels: 1, // 单声道
  })

  if (voiceTimer)
    clearTimeout(voiceTimer)
  voiceTimer = setTimeout(() => {
    if (isVoiceListening.value) {
      stopAppRecording()
      uni.showToast({
        title: '录音已自动停止',
        icon: 'none',
      })
    }
  }, 30000)
}

function stopAppRecording() {
  recorderManager.stop()
}
// #endif

async function processVoice(base64: string, len: number) {
  try {
    const token = await getBaiduAccessToken()
    const cuid = uni.getSystemInfoSync().deviceId || 'soak-user'

    const res: any = await recognizeSpeech({
      format: 'pcm',
      rate: 16000,
      channel: 1,
      cuid,
      token,
      speech: base64,
      len,
    })

    console.log('Baidu ASR Result:', res.data)

    if (res.data && res.data.err_no === 0 && res.data.result) {
      const text = res.data.result.join('')
      const { phone, password } = parseVoiceContent(text)
      if (phone || password) {
        if (phone)
          formData.phone = phone
        if (password)
          formData.password = password
        uni.showToast({
          title: t('login.voiceSuccess'),
          icon: 'success',
        })
      }
      else {
        uni.showToast({
          title: `识别结果: ${text} (未匹配到账号密码)`,
          icon: 'none',
          duration: 3000,
        })
      }
    }
    else {
      throw new Error(res.data?.err_msg || '识别失败')
    }
  } catch (error: any) {
    throw error
  } finally {
    isVoiceProcessing.value = false
  }
}

async function handleVoiceLogin() {
  if (loading.value || isVoiceProcessing.value)
    return

  // #ifdef H5
  if (isVoiceListening.value) {
    stopH5Recording()
    return
  }
  await startH5Recording()
  return
  // #endif
}
</script>

<template>
  <view class="login-container">
    <view class="safe-area-inset-top" />

    <view class="login-content">
      <view class="logo-section">
        <view class="app-name">
          {{ t('login.welcome') }}
        </view>
        <view class="app-desc">
          {{ t('login.description') }}
        </view>
      </view>

      <view class="form-section">
        <view class="input-group">
          <view class="input-wrapper">
            <view class="input-icon">
              <u-icon name="phone" size="36" color="#999" />
            </view>
            <input
              v-model="formData.phone"
              class="input-field"
              :placeholder="t('login.phonePlaceholder')"
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
              :placeholder="t('login.passwordPlaceholder')"
              :disabled="loading"
            >
            <view
              class="password-toggle"
              @click="togglePassword"
            >
              <u-icon
                :name="showPassword ? 'eye-off' : 'eye'"
                size="36"
                color="#999"
              />
            </view>
          </view>
        </view>

        <view class="options-row">
          <view
            class="remember-me"
            @click="toggleRememberMe"
          >
            <checkbox
              :checked="formData.rememberMe"
              :disabled="loading"
              color="#5D9997"
              class="checkbox-custom"
              @change.stop="formData.rememberMe = $event.detail.value"
            />
            <text class="checkbox-label">
              {{ t('login.rememberPassword') }}
            </text>
          </view>
          <view
            class="forgot-link"
            @click="handleForgotPassword"
          >
            {{ t('login.forgotPassword') }}
          </view>
        </view>

        <view
          class="login-btn"
          :class="{ loading }"
          @click="handleLogin"
        >
          <text v-if="loading">
            {{ t('login.logging') }}
          </text>
          <text v-else>
            {{ t('login.loginButton') }}
          </text>
        </view>

        <view
          class="voice-login-btn"
          :class="{ listening: isVoiceListening, processing: isVoiceProcessing }"
          @click="handleVoiceLogin"
        >
          <u-icon
            :name="isVoiceListening ? 'mic' : 'mic'"
            size="36"
            :color="isVoiceListening ? '#5d9997' : '#ffffff'"
          />
          <text v-if="isVoiceListening">
            {{ t('login.voiceListening') }}
          </text>
          <text v-else-if="isVoiceProcessing">
            {{ t('login.voiceProcessing') }}
          </text>
          <text v-else>
            {{ t('login.voiceLogin') }}
          </text>
        </view>

        <view class="register-link" @click="handleRegister">
          <text class="register-text">
            {{ t('login.register') }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.login-container {
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

  .login-content {
    width: 100%;
    height: 100%;
    padding: 60rpx 48rpx;
    box-sizing: border-box;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .logo-section {
    text-align: center;
    margin-bottom: 100rpx;
    margin-top: -60rpx;

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
    padding: 80rpx 48rpx;
    box-shadow:
      0 24rpx 80rpx rgba(0, 0, 0, 0.12),
      0 8rpx 24rpx rgba(0, 0, 0, 0.08),
      inset 0 1rpx 0 rgba(255, 255, 255, 0.8);
    margin-bottom: -40rpx;
    border: 1rpx solid rgba(255, 255, 255, 0.6);

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.4) 0%, rgba(255, 255, 255, 0.1) 100%);
      border-radius: 32rpx;
      pointer-events: none;
      z-index: -1;
    }

    .input-group {
      margin-bottom: 40rpx;

      &:last-child {
        margin-bottom: 0;
      }

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
        }

        .input-field {
          flex: 1;
          height: 100%;
          font-size: 30rpx;
          color: #2c3e50;
          background: transparent;
          outline: none;
          font-weight: 400;

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

    .options-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 40rpx;

      .remember-me {
        display: flex;
        align-items: center;
        cursor: pointer;
        transition: opacity 0.3s ease;

        &:active {
          opacity: 0.7;
        }

        .checkbox-custom {
          transform: scale(0.85);
        }

        .checkbox-label {
          font-size: 26rpx;
          color: #718096;
          margin-left: 12rpx;
          font-weight: 400;
        }
      }

      .forgot-link {
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

    .login-btn {
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

      &:active {
        transform: scale(0.97);
        box-shadow: 0 4rpx 12rpx rgba(93, 153, 151, 0.2);
      }

      &.loading {
        opacity: 0.7;
        cursor: not-allowed;
      }
    }

    .register-link {
      text-align: center;
      margin-top: 32rpx;

      .register-text {
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

    .voice-login-btn {
      width: 100%;
      height: 96rpx;
      background: rgba(93, 153, 151, 0.15);
      border: 2rpx solid #5d9997;
      border-radius: 20rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 30rpx;
      font-weight: 500;
      color: #5d9997;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      cursor: pointer;
      letter-spacing: 2rpx;
      margin-top: 24rpx;
      gap: 16rpx;

      &:active {
        transform: scale(0.97);
        opacity: 0.8;
      }

      &.listening {
        background: rgba(93, 153, 151, 0.25);
        border-color: #5d9997;
        animation: pulse 1.5s ease-in-out infinite;
      }

      &.processing {
        opacity: 0.7;
        cursor: not-allowed;
      }

      @keyframes pulse {
        0%,
        100% {
          box-shadow: 0 0 0 0 rgba(93, 153, 151, 0.4);
        }
        50% {
          box-shadow: 0 0 0 20rpx rgba(93, 153, 151, 0);
        }
      }
    }
  }
}
</style>
