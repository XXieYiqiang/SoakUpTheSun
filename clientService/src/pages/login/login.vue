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
        password: formData.password,
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

  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1500))
    saveCredentials()
    uni.showToast({
      title: t('login.loginSuccess'),
      icon: 'success',
      duration: 2000,
    })
    setTimeout(() => {
      uni.switchTab({
        url: '/pages/index/index',
      })
    }, 1000)
  }
  catch (error) {
    uni.showToast({
      title: t('login.loginFailed'),
      icon: 'none',
      duration: 2000,
    })
  }
  finally {
    loading.value = false
  }
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
  uni.showToast({
    title: t('login.registerDeveloping'),
    icon: 'none',
    duration: 2000,
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

async function handleVoiceLogin() {
  if (loading.value || isVoiceProcessing.value)
    return

  isVoiceProcessing.value = true

  try {
    // #ifdef H5
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition

    if (!SpeechRecognition) {
      console.log('浏览器不支持语音识别 API')
      uni.showModal({
        title: t('login.voiceNotSupported'),
        content: '当前浏览器不支持语音识别功能。建议使用 Chrome、Edge 或 Safari 浏览器。',
        showCancel: false,
      })
      isVoiceProcessing.value = false
      return
    }

    const checkMicrophonePermission = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        stream.getTracks().forEach(track => track.stop())
        return true
      }
      catch (error: any) {
        console.error('麦克风权限检查失败:', error)
        if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
          uni.showModal({
            title: '麦克风权限被拒绝',
            content: '请在浏览器设置中允许麦克风访问，然后重试。',
            showCancel: false,
          })
        }
        else if (error.name === 'NotFoundError') {
          uni.showModal({
            title: '未检测到麦克风',
            content: '请检查您的设备是否连接了麦克风。',
            showCancel: false,
          })
        }
        else {
          uni.showModal({
            title: '麦克风访问失败',
            content: `无法访问麦克风: ${error.message}`,
            showCancel: false,
          })
        }
        return false
      }
    }

    const hasPermission = await checkMicrophonePermission()
    if (!hasPermission) {
      isVoiceProcessing.value = false
      return
    }

    const SpeechRecognitionConstructor = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
    const speechRecognition = new SpeechRecognitionConstructor()
    speechRecognition.lang = 'zh-CN'
    speechRecognition.continuous = false
    speechRecognition.interimResults = true
    speechRecognition.maxAlternatives = 3

    let recognitionTimeout: any = null
    let hasReceivedResult = false

    speechRecognition.onstart = () => {
      console.log('🎤 语音识别开始，请开始说话...')
      isVoiceListening.value = true

      recognitionTimeout = setTimeout(() => {
        if (!hasReceivedResult && isVoiceListening.value) {
          console.log('⏰ 语音识别超时，请重试')
          try {
            speechRecognition.stop()
          }
          catch (error) {
            console.error('停止语音识别失败:', error)
          }
        }
      }, 15000)
    }

    speechRecognition.onresult = (event: any) => {
      hasReceivedResult = true
      if (recognitionTimeout) {
        clearTimeout(recognitionTimeout)
      }

      const results = event.results
      const lastResult = results[results.length - 1]
      const transcript = lastResult[0].transcript
      const confidence = lastResult[0].confidence
      const isFinal = lastResult.isFinal

      console.log('==========================================')
      console.log('🎤 语音识别结果:')
      console.log(`📝 识别文本: ${transcript}`)
      console.log(`📊 置信度: ${(confidence * 100).toFixed(2)}%`)
      console.log(`✅ 是否最终结果: ${isFinal ? '是' : '否（中间结果）'}`)
      console.log(`📋 所有候选结果:`)
      
      for (let i = 0; i < lastResult.length; i++) {
        console.log(`   ${i + 1}. ${lastResult[i].transcript} (置信度: ${(lastResult[i].confidence * 100).toFixed(2)}%)`)
      }
      console.log('==========================================')

      if (isFinal) {
        const { phone, password } = parseVoiceContent(transcript)

        if (phone && password) {
          formData.phone = phone
          formData.password = password
          uni.showToast({
            title: t('login.voiceSuccess'),
            icon: 'success',
            duration: 2000,
          })
        }
        else {
          uni.showToast({
            title: `识别结果: ${transcript}`,
            icon: 'none',
            duration: 3000,
          })
        }
      }
    }

    speechRecognition.onerror = (event: any) => {
      console.log('==========================================')
      console.log('❌ 语音识别错误:')
      console.log(`错误类型: ${event.error}`)
      console.log(`错误信息: ${event.message}`)
      console.log('==========================================')
      
      if (recognitionTimeout) {
        clearTimeout(recognitionTimeout)
      }

      let errorMessage = t('login.voiceFailed')

      switch (event.error) {
        case 'no-speech':
          errorMessage = '未检测到语音，请检查：\n1. 麦克风是否正常工作\n2. 说话声音是否足够大\n3. 麦克风是否被其他应用占用'
          console.log('💡 提示: no-speech 错误通常表示：')
          console.log('   - 麦克风权限未授予')
          console.log('   - 麦克风设备不可用')
          console.log('   - 说话声音太小')
          console.log('   - 麦克风被静音')
          break
        case 'audio-capture':
          errorMessage = '无法访问麦克风，请检查权限设置'
          console.log('💡 提示: 请确保已授予麦克风权限')
          break
        case 'not-allowed':
          errorMessage = '麦克风权限被拒绝，请在浏览器设置中允许访问'
          console.log('💡 提示: 需要在浏览器中手动授予麦克风权限')
          break
        case 'network':
          errorMessage = '网络连接错误，请检查网络'
          console.log('💡 提示: 语音识别需要网络连接')
          break
        case 'aborted':
          errorMessage = '语音识别已取消'
          console.log('💡 提示: 识别被手动停止')
          break
        case 'service-not-allowed':
          errorMessage = '语音识别服务不可用，请检查网络连接'
          console.log('💡 提示: 可能是网络问题或浏览器不支持')
          break
        default:
          errorMessage = `语音识别错误: ${event.error}`
          console.log('💡 提示: 未知错误类型')
      }

      uni.showModal({
        title: '语音识别失败',
        content: errorMessage,
        showCancel: false,
      })
    }

    speechRecognition.onend = () => {
      console.log('==========================================')
      console.log('🏁 语音识别结束')
      console.log(`是否收到结果: ${hasReceivedResult ? '是' : '否'}`)
      console.log(`是否正在监听: ${isVoiceListening.value ? '是' : '否'}`)
      console.log('==========================================')
      
      if (recognitionTimeout) {
        clearTimeout(recognitionTimeout)
      }
      isVoiceListening.value = false
      isVoiceProcessing.value = false
    }

    uni.showModal({
      title: '语音登录提示',
      content: '请清晰地说出您的账号和密码，例如：\n\n"账号123456密码789"\n\n注意：\n• 请确保麦克风已授权\n• 请在安静环境下使用\n• 请清晰大声地说话',
      showCancel: false,
      confirmText: '开始识别',
    })

    setTimeout(() => {
      try {
        speechRecognition.start()
        console.log('✅ 语音识别已启动')
      }
      catch (error) {
        console.error('❌ 启动语音识别失败:', error)
        uni.showToast({
          title: '启动语音识别失败，请重试',
          icon: 'none',
          duration: 2000,
        })
        isVoiceProcessing.value = false
      }
    }, 500)
    // #endif

    // #ifdef APP-PLUS
    const checkAppPermission = async () => {
      try {
        const setting = await uni.getSetting()
        if (!setting.authSetting['scope.record']) {
          const authorizeResult = await uni.authorize({
            scope: 'scope.record',
          })
          if (!authorizeResult[1].authSetting['scope.record']) {
            return false
          }
        }
        return true
      }
      catch (error) {
        return false
      }
    }

    const appHasPermission = await checkAppPermission()
    if (!appHasPermission) {
      uni.showModal({
        title: t('login.voicePermissionRequest'),
        content: t('login.voicePermissionDenied'),
        confirmText: t('login.voiceOpenSettings'),
        cancelText: t('login.voiceAuthorize'),
        success: (res) => {
          if (res.confirm) {
            uni.openSetting()
          }
        },
      })
      return
    }

    const plusSpeech = (plus as any).speech
    console.log('🚀 ~ handleVoiceLogin ~ plusSpeech:', plusSpeech)
    if (!plusSpeech) {
      uni.showToast({
        title: t('login.voiceNotSupported'),
        icon: 'none',
        duration: 2000,
      })
      return
    }

    const speechRecognizer = plusSpeech.createSpeechRecognizer()
    speechRecognizer.start()

    isVoiceListening.value = true

    speechRecognizer.onresult = (event: any) => {
      const transcript = event.result
      const { phone, password } = parseVoiceContent(transcript)

      if (phone && password) {
        formData.phone = phone
        formData.password = password
        uni.showToast({
          title: t('login.voiceSuccess'),
          icon: 'success',
          duration: 2000,
        })
      }
      else {
        uni.showToast({
          title: t('login.voiceParseFailed'),
          icon: 'none',
          duration: 2000,
        })
      }

      speechRecognizer.stop()
      isVoiceListening.value = false
      isVoiceProcessing.value = false
    }

    speechRecognizer.onerror = (event: any) => {
      uni.showToast({
        title: t('login.voiceFailed'),
        icon: 'none',
        duration: 2000,
      })
      speechRecognizer.stop()
      isVoiceListening.value = false
      isVoiceProcessing.value = false
    }

    uni.showToast({
      title: t('login.voiceLoginTip'),
      icon: 'none',
      duration: 2000,
    })
    // #endif
  }
  catch (error) {
    uni.showToast({
      title: t('login.voiceNotSupported'),
      icon: 'none',
      duration: 2000,
    })
    isVoiceProcessing.value = false
    isVoiceListening.value = false
  }
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

        <view class="register-link">
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
