<script lang="ts" setup>
import { getCurrentInstance, nextTick } from 'vue'

defineOptions({
  name: 'Home',
})
definePage({
  type: 'home',
  style: {
    navigationStyle: 'default',
    navigationBarTitleText: '%tabbar.home%',
  },
})

const videoRef = ref<any>(null)
const canvasRef = ref<any>(null)
const isCameraActive = ref(false)
const isCapturing = ref(false)
const captureCountdown = ref(10)
const capturedImages = ref<string[]>([])
const captureInterval = ref<any>(null)
const stream = ref<any>(null)
const livePusherContext = ref<any>(null)

function unwrapRefEl(val: any) {
  return val && val.$el ? val.$el : val
}

function getNativeVideoEl(): HTMLVideoElement | null {
  const candidate = unwrapRefEl(videoRef.value)
  const direct = unwrapRefEl(candidate)
  if (direct && typeof direct === 'object') {
    const tag = (direct as any).tagName
    if (tag && String(tag).toLowerCase() === 'video') {
      return direct as HTMLVideoElement
    }
    if (typeof (direct as any).querySelector === 'function') {
      const found = (direct as any).querySelector('video')
      if (found) {
        return found as HTMLVideoElement
      }
    }
  }
  const byId = typeof document !== 'undefined' ? document.getElementById('cameraVideo') : null
  if (byId) {
    const tag = (byId as any).tagName
    if (tag && String(tag).toLowerCase() === 'video') {
      return byId as any
    }
    if (typeof (byId as any).querySelector === 'function') {
      const found = (byId as any).querySelector('video')
      if (found) {
        return found as HTMLVideoElement
      }
    }
  }
  return null
}

function getNativeCanvasEl(): HTMLCanvasElement | null {
  const candidate = unwrapRefEl(canvasRef.value)
  const direct = unwrapRefEl(candidate)
  if (direct && typeof direct === 'object') {
    const tag = (direct as any).tagName
    if (tag && String(tag).toLowerCase() === 'canvas') {
      return direct as HTMLCanvasElement
    }
    if (typeof (direct as any).querySelector === 'function') {
      const found = (direct as any).querySelector('canvas')
      if (found) {
        return found as HTMLCanvasElement
      }
    }
  }
  const byId = typeof document !== 'undefined' ? document.getElementById('cameraCanvas') : null
  if (byId) {
    const tag = (byId as any).tagName
    if (tag && String(tag).toLowerCase() === 'canvas') {
      return byId as any
    }
    if (typeof (byId as any).querySelector === 'function') {
      const found = (byId as any).querySelector('canvas')
      if (found) {
        return found as HTMLCanvasElement
      }
    }
  }
  return null
}

async function checkCameraPermission() {
  // #ifdef H5
  try {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      uni.showToast({
        title: '当前浏览器不支持摄像头',
        icon: 'none',
        duration: 3000,
      })
      return false
    }
    return true
  }
  catch (error) {
    console.log('检查摄像头权限失败:', error)
    return true
  }
  // #endif
}

async function startCamera() {
  if (isCameraActive.value) {
    return
  }

  const hasPermission = await checkCameraPermission()
  if (!hasPermission) {
    return
  }

  try {
    // #ifdef H5
    let cameraStream: MediaStream | null = null
    try {
      const devices = await navigator.mediaDevices.enumerateDevices()
      const cameras = devices.filter(d => d.kind === 'videoinput')
      if (cameras.length === 0) {
        uni.showToast({
          title: '未检测到摄像头设备',
          icon: 'none',
          duration: 3000,
        })
        return
      }
      const firstCam = cameras[0]
      const constraints: MediaStreamConstraints = {
        video: firstCam.deviceId
          ? {
              deviceId: { exact: firstCam.deviceId },
              width: { ideal: 1280 },
              height: { ideal: 720 },
            }
          : {
              width: { ideal: 1280 },
              height: { ideal: 720 },
            },
        audio: false,
      }
      cameraStream = await navigator.mediaDevices.getUserMedia(constraints)
    }
    catch (error: any) {
      console.error('第一次获取摄像头失败，尝试兼容约束:', error)
      try {
        cameraStream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: false,
        })
      }
      catch (error2: any) {
        console.error('摄像头最终启动失败:', error2)
        const name = error2?.name || ''
        let message = '摄像头启动失败'
        if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
          message = '摄像头权限被拒绝，请在浏览器地址栏允许摄像头访问'
        }
        else if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
          message = '未找到可用摄像头设备'
        }
        else if (name === 'NotReadableError' || name === 'TrackStartError') {
          message = '摄像头设备被其他应用占用或不可用'
        }

        uni.showToast({
          title: message,
          icon: 'none',
          duration: 4000,
        })
        throw error2
      }
    }

    stream.value = cameraStream
    await nextTick()
    const videoEl = getNativeVideoEl()
    if (videoEl) {
      videoEl.muted = true
      ;(videoEl as any).playsInline = true
      videoEl.autoplay = true
      ;(videoEl as any).srcObject = cameraStream
      if (typeof videoEl.play === 'function') {
        try {
          await videoEl.play()
        }
        catch (playErr: any) {
          const name = playErr?.name || ''
          if (name === 'NotAllowedError') {
            uni.showToast({
              title: '浏览器自动播放限制，请点击页面任意位置后重试',
              icon: 'none',
              duration: 3000,
            })
          }
          console.error('视频播放失败，但已获取到摄像头流:', playErr)
          if (typeof document !== 'undefined') {
            const handler = async () => {
              document.removeEventListener('click', handler as any)
              try {
                await videoEl.play()
              }
              catch (e) {
                console.error('点击后播放仍失败:', e)
              }
            }
            document.addEventListener('click', handler as any, { once: true } as any)
          }
        }
      }
    }
    else {
      uni.showToast({
        title: '视频元素未就绪',
        icon: 'none',
        duration: 3000,
      })
      return
    }

    isCameraActive.value = true
    startCapture()

    uni.showToast({
      title: '摄像头启动成功',
      icon: 'success',
      duration: 1500,
    })
    // #endif

    // #ifdef APP-PLUS
    if (!livePusherContext.value) {
      const instance = getCurrentInstance()
      livePusherContext.value = uni.createLivePusherContext('livePusher', instance)
    }

    if (livePusherContext.value) {
      await livePusherContext.value.startPreview()
      isCameraActive.value = true
      startCapture()
      uni.showToast({
        title: '摄像头启动成功',
        icon: 'success',
        duration: 1500,
      })
    }
    // #endif
  }
  catch (error) {
    console.error('启动摄像头失败:', error)
    const name = (error as any)?.name || ''
    let message = '启动失败'
    if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
      message = '摄像头权限被拒绝，请在浏览器地址栏允许摄像头访问'
    }
    else if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
      message = '未找到可用摄像头设备'
    }
    else if (name === 'NotReadableError' || name === 'TrackStartError') {
      message = '摄像头设备被其他应用占用或不可用'
    }
    uni.showToast({
      title: message,
      icon: 'none',
      duration: 4000,
    })
  }
}

async function stopCamera() {
  if (!isCameraActive.value) {
    return
  }

  stopCapture()

  // #ifdef H5
  if (stream.value) {
    stream.value.getTracks().forEach((track: any) => track.stop())
    stream.value = null
  }
  const videoEl = getNativeVideoEl()
  if (videoEl) {
    ;(videoEl as any).srcObject = null
  }
  // #endif

  // #ifdef APP-PLUS
  if (livePusherContext.value) {
    livePusherContext.value.stopPreview()
  }
  // #endif

  isCameraActive.value = false
}

function pushCapturedImage(imageData: string) {
  capturedImages.value.unshift(imageData)
  if (capturedImages.value.length > 10) {
    capturedImages.value.pop()
  }

  uni.showToast({
    title: '截取成功',
    icon: 'success',
    duration: 1000,
  })
}

function captureImage() {
  try {
    // #ifdef H5
    const canvas = getNativeCanvasEl()
    const video = getNativeVideoEl()
    if (!video || !canvas || typeof (canvas as any).getContext !== 'function') {
      return
    }

    const ctx = (canvas as any).getContext('2d')

    if (!ctx) {
      return
    }

    const vw = (video as any).videoWidth
    const vh = (video as any).videoHeight
    if (!vw || !vh) {
      console.log('视频画面未准备好，暂不截取', { vw, vh })
      return
    }
    const cw = (video as any).clientWidth || (video as any).offsetWidth || vw
    const ch = (video as any).clientHeight || (video as any).offsetHeight || vh
    ;(canvas as any).width = cw
    ;(canvas as any).height = ch
    let fit = ''
    try {
      fit = typeof window !== 'undefined' ? window.getComputedStyle(video).objectFit : ''
    }
    catch {}
    if (fit === 'contain') {
      const scale = Math.min(cw / vw, ch / vh)
      const dw = Math.round(vw * scale)
      const dh = Math.round(vh * scale)
      const dx = Math.round((cw - dw) / 2)
      const dy = Math.round((ch - dh) / 2)
      ctx.clearRect(0, 0, cw, ch)
      ctx.drawImage(video, 0, 0, vw, vh, dx, dy, dw, dh)
    }
    else if (fit === 'cover' || !fit) {
      const scale = Math.max(cw / vw, ch / vh)
      const sw = Math.round(cw / scale)
      const sh = Math.round(ch / scale)
      const sx = Math.round((vw - sw) / 2)
      const sy = Math.round((vh - sh) / 2)
      ctx.clearRect(0, 0, cw, ch)
      ctx.drawImage(video, sx, sy, sw, sh, 0, 0, cw, ch)
    }
    else {
      ctx.clearRect(0, 0, cw, ch)
      ctx.drawImage(video, 0, 0, vw, vh, 0, 0, cw, ch)
    }

    const imageData = (canvas as any).toDataURL('image/jpeg', 0.8)
    pushCapturedImage(imageData)
    // #endif

    // #ifdef APP-PLUS
    if (!livePusherContext.value) {
      return
    }

    livePusherContext.value.snapshot({
      success: (res: any) => {
        const imagePath = res.tempImagePath || (res.message && res.message.tempImagePath)
        if (!imagePath) {
          uni.showToast({
            title: '截取失败',
            icon: 'none',
            duration: 2000,
          })
          return
        }

        pushCapturedImage(imagePath)
      },
      fail: (error: any) => {
        console.error('截取图片失败:', error)
        uni.showToast({
          title: '截取失败',
          icon: 'none',
          duration: 2000,
        })
      },
    })
    // #endif
  }
  catch (error) {
    console.error('截取图片失败:', error)
    uni.showToast({
      title: '截取失败',
      icon: 'none',
      duration: 2000,
    })
  }
}

function startCapture() {
  if (isCapturing.value) {
    return
  }

  isCapturing.value = true
  captureImage()
  captureCountdown.value = 10
  captureInterval.value = setInterval(() => {
    captureCountdown.value -= 1
    if (captureCountdown.value <= 0) {
      captureImage()
      captureCountdown.value = 10
    }
  }, 1000)
}

function stopCapture() {
  if (captureInterval.value) {
    clearInterval(captureInterval.value)
    captureInterval.value = null
  }
  isCapturing.value = false
  captureCountdown.value = 10
}

function saveImage(imageData: string) {
  // #ifdef H5
  const link = document.createElement('a')
  link.href = imageData
  link.download = `capture_${Date.now()}.jpg`
  link.click()
  // #endif

  // #ifdef APP-PLUS
  uni.saveImageToPhotosAlbum({
    filePath: imageData,
    success: () => {
      uni.showToast({
        title: '保存成功',
        icon: 'success',
        duration: 2000,
      })
    },
    fail: () => {
      uni.showToast({
        title: '保存失败',
        icon: 'none',
        duration: 2000,
      })
    },
  })
  // #endif
}

function clearImages() {
  capturedImages.value = []
  uni.showToast({
    title: '已清空',
    icon: 'success',
    duration: 1000,
  })
}

onUnload(() => {
  stopCamera()
})
</script>

<template>
  <view class="index-page">
    <view class="safe-area-inset-top" />

    <view class="camera-container">
      <!-- #ifdef H5 -->
      <video
        id="cameraVideo"
        ref="videoRef"
        class="camera-video"
        autoplay
        playsinline
        muted
      />
      <canvas
        id="cameraCanvas"
        ref="canvasRef"
        class="camera-canvas"
        style="display: none;"
      />
      <!-- #endif -->

      <!-- #ifdef APP-PLUS -->
      <live-pusher
        id="livePusher"
        class="camera-video"
        url=""
        mode="FHD"
        :muted="true"
        :enable-camera="true"
        :auto-focus="true"
        :beauty="0"
      />
      <!-- #endif -->
    </view>

    <view class="control-panel">
      <view class="status-bar">
        <view class="status-item">
          <view
            class="status-dot"
            :class="{ active: isCameraActive }"
          />
          <text class="status-text">
            {{ isCameraActive ? '摄像头运行中' : '摄像头已关闭' }}
          </text>
        </view>
        <view class="status-item">
          <view
            class="status-dot"
            :class="{ active: isCapturing }"
          />
          <text
            v-if="isCapturing"
            class="status-text"
          >
            自动截取中 ({{ captureCountdown }}s)
          </text>
          <text
            v-else
            class="status-text"
          >
            自动截取已停止
          </text>
        </view>
      </view>

      <view class="button-group">
        <u-button
          type="success"
          size="mini"
          :disabled="isCameraActive"
          @click="startCamera"
        >
          启动摄像头
        </u-button>

        <u-button
          type="error"
          size="mini"
          :disabled="!isCameraActive"
          @click="stopCamera"
        >
          停止摄像头
        </u-button>

        <u-button
          type="primary"
          size="mini"
          :disabled="!isCameraActive"
          @click="captureImage"
        >
          立即截取
        </u-button>
      </view>
    </view>

    <view class="images-container">
      <view class="images-header">
        <text class="images-title">截取的图片 ({{ capturedImages.length }}/10)</text>
        <view
          class="clear-btn"
          @click="clearImages"
        >
          <text>清空</text>
        </view>
      </view>

      <scroll-view
        class="images-scroll"
        scroll-y
      >
        <view
          v-for="(image, index) in capturedImages"
          :key="index"
          class="image-item"
        >
          <image
            class="image-preview"
            :src="image"
            mode="widthFix"
          />
          <view
            class="image-save-btn"
            @click="saveImage(image)"
          >
            <text class="image-save-text">保存</text>
          </view>
        </view>

        <view
          v-if="capturedImages.length === 0"
          class="empty-state"
        >
          <text class="empty-text">暂无截取的图片</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<style scoped>
.index-page {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f5f5 0%, #e8e8e8 100%);
  display: flex;
  flex-direction: column;
}

.safe-area-inset-top {
  height: constant(safe-area-inset-top);
  height: env(safe-area-inset-top);
}

.camera-container {
  width: 100%;
  height: 400rpx;
  background: #000;
  position: relative;
}

.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-canvas {
  position: absolute;
  top: 0;
  left: 0;
  opacity: 0;
  pointer-events: none;
}

.control-panel {
  background: #ffffff;
  padding: 30rpx;
  margin: 20rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.status-bar {
  display: flex;
  justify-content: space-around;
  margin-bottom: 30rpx;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.status-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #ccc;
  transition: all 0.3s ease;
}

.status-dot.active {
  background: #52c41a;
  box-shadow: 0 0 10rpx rgba(82, 196, 26, 0.5);
}

.status-text {
  font-size: 26rpx;
  color: #666;
}

.button-group {
  display: flex;
  justify-content: space-around;
  gap: 20rpx;
}
.images-container {
  flex: 1;
  background: #ffffff;
  margin: 0 20rpx 20rpx 20rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  height: 0;
}

.images-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.images-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.clear-btn {
  padding: 10rpx;
}

.images-scroll {
  flex: 1;
  overflow: hidden;
}

.image-item {
  position: relative;
  margin-bottom: 20rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.image-preview {
  width: 100%;
  height: auto;
  background: #f5f5f5;
}

.image-save-btn {
  position: absolute;
  bottom: 20rpx;
  right: 20rpx;
  padding: 12rpx 24rpx;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 40rpx;
}

.image-save-text {
  font-size: 24rpx;
  color: #ffffff;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
  gap: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>
