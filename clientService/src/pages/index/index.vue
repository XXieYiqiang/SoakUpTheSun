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
const isGalleryOpen = ref(false)
const h5ResizeHandler = ref<((ev: UIEvent) => void) | null>(null)

function getH5CameraTarget() {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return { width: 1280, height: 720, aspectRatio: 16 / 9 }
  }
  const el = document.querySelector('.camera-container') as HTMLElement | null
  const rect = el?.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  const cssW = rect?.width || window.innerWidth || 375
  const cssH = rect?.height || window.innerHeight || 667
  const width = Math.max(320, Math.round(cssW * dpr))
  const height = Math.max(240, Math.round(cssH * dpr))
  const aspectRatio = cssW > 0 && cssH > 0 ? cssW / cssH : 16 / 9
  return { width, height, aspectRatio }
}

async function applyH5Constraints() {
  // #ifdef H5
  const currentStream = stream.value as MediaStream | null
  if (!currentStream)
    return
  const videoTrack = currentStream.getVideoTracks?.()?.[0]
  if (!videoTrack)
    return
  const { width, height, aspectRatio } = getH5CameraTarget()
  try {
    await videoTrack.applyConstraints({
      width: { ideal: width },
      height: { ideal: height },
      aspectRatio: { ideal: aspectRatio },
    })
  }
  catch {}
  // #endif
}

async function handleShutterClick() {
  if (!isCameraActive.value) {
    await startCamera()
    return
  }
  if (isCapturing.value) {
    stopCapture()
    return
  }
  startCapture()
}

function handleShutterLongpress() {
  if (isCameraActive.value) {
    stopCamera()
  }
}

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
      await nextTick()
      const target = getH5CameraTarget()
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
              width: { ideal: target.width },
              height: { ideal: target.height },
              aspectRatio: { ideal: target.aspectRatio },
              frameRate: { ideal: 30, max: 60 },
            }
          : {
              width: { ideal: target.width },
              height: { ideal: target.height },
              aspectRatio: { ideal: target.aspectRatio },
              frameRate: { ideal: 30, max: 60 },
              facingMode: { ideal: 'environment' },
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
      await applyH5Constraints()
      if (!h5ResizeHandler.value && typeof window !== 'undefined') {
        const handler = () => {
          applyH5Constraints()
        }
        h5ResizeHandler.value = handler
        window.addEventListener('resize', handler)
      }
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
  if (h5ResizeHandler.value && typeof window !== 'undefined') {
    window.removeEventListener('resize', h5ResizeHandler.value)
    h5ResizeHandler.value = null
  }
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

import { getPictureAnalysisResponse, uploadPictureAnalysis } from '@/api/index'

function pushCapturedImage(imageData: string) {
  capturedImages.value.unshift(imageData)
  if (capturedImages.value.length > 10) {
    capturedImages.value.pop()
  }

  // 自动上传分析
  uploadPictureAnalysis(imageData, '自动截取图片分析').then(res => {
    // res.data 假设包含 id 字段，根据实际接口返回调整
    // 假设 res 结构为 { code: 0, data: { id: 123, ... } } 或 { code: 0, data: 123 }
    // 如果直接返回 id，则 pictureId = res.data
    // 如果返回对象，则 pictureId = res.data.id
    
    // 打印一下以便调试
    console.log('Upload response:', res)
    
    // 这里假设 res.data 是包含 id 的对象，或者根据用户之前的描述 "data": { "id": ... }
    // 但用户之前提供的用户信息接口是 data: { id: ... }
    // 对于上传接口，用户没有提供具体返回，但通常会返回 ID
    
    let pictureId = null
    if (res && res.data) {
       if (typeof res.data === 'object' && res.data.id) {
         pictureId = res.data.id
       } else if (typeof res.data === 'string' || typeof res.data === 'number') {
         pictureId = res.data
       }
    }
    
    if (pictureId) {
      console.log('Got pictureId:', pictureId)
      // 获取分析结果
      getPictureAnalysisResponse(pictureId).then(analysisRes => {
         console.log('Analysis result:', analysisRes)
         if (analysisRes.code === 200 || analysisRes.code === 0) {
             uni.showToast({
               title: '分析结果已获取',
               icon: 'success',
               duration: 2000
             })
             // 这里可以处理 analysisRes.data，例如弹窗显示或播报
         }
      })
    }
  }).catch(err => {
    console.error('图片上传分析失败:', err)
  })

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
    const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1
    ;(canvas as any).width = vw * dpr
    ;(canvas as any).height = vh * dpr
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ctx.clearRect(0, 0, vw, vh)
    ctx.drawImage(video, 0, 0, vw / dpr, vh / dpr)

    const imageData = (canvas as any).toDataURL('image/jpeg', 1)
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

    <view class="camera-card">
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

      <view class="camera-overlay">
        <view class="camera-pill camera-pill-left">
          <text class="camera-pill-text">
            {{ isCapturing ? 'AI 识别中...' : 'AI 待机中' }}
          </text>
        </view>

        <view class="corner corner-tl" />
        <view class="corner corner-tr" />
        <view class="corner corner-bl" />
        <view class="corner corner-br" />

        <view class="focus-dot">
          <view class="focus-dot-inner" />
        </view>
      </view>
    </view>


    <view class="hint-text">
      <text class="hint-text-inner">
        {{ isCapturing ? '轻触下方按钮停止识别' : '轻触下方按钮开始识别' }}
      </text>
    </view>
    <view class="bottom-controls">
      <view
        class="side-btn"
        :class="{ 'side-btn--on': isCameraActive, 'side-btn--off': !isCameraActive }"
        @click="isCameraActive ? stopCamera() : startCamera()"
      >
        <text class="side-btn-icon"></text>
      </view>

      <view
        class="shutter"
        :class="{
          'shutter--capturing': isCapturing,
          'shutter--idle': isCameraActive && !isCapturing,
          'shutter--inactive': !isCameraActive,
        }"
        @click="handleShutterClick"
        @longpress="handleShutterLongpress"
      >
        <view class="shutter-ring">
          <view class="shutter-core" />
        </view>
      </view>

      <view
        class="side-btn"
        @click="isGalleryOpen = true"
      >
        <u-icon
          name="list"
          size="44"
          color="rgba(0, 0, 0, 0.7)"
        />
      </view>
    </view>

    <u-popup
      v-model="isGalleryOpen"
      mode="bottom"
      :round="28"
      :close-on-click-overlay="true"
    >
      <view class="gallery-popup">
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
    </u-popup>
  </view>
</template>

<style scoped>
.index-page {
  width: 100%;
  min-height: calc(100vh - 190rpx);
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-bottom: 100rpx;
}

.safe-area-inset-top {
  height: constant(safe-area-inset-top);
  height: env(safe-area-inset-top);
}

.camera-card {
  position: relative;
  width: calc(100% - 80rpx);
  margin: 30rpx 40rpx 0 40rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: #000;
  box-shadow: 0 18rpx 50rpx rgba(0, 0, 0, 0.18);
}

.camera-container {
  width: 100%;
  height: 900rpx;
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

.camera-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.camera-pill {
  position: absolute;
  top: 26rpx;
  padding: 14rpx 22rpx;
  backdrop-filter: blur(10px);
}

.camera-pill-left {
  left: 22rpx;
}

.camera-pill-right {
  right: 22rpx;
}

.camera-pill-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.92);
}

.corner {
  position: absolute;
  width: 56rpx;
  height: 56rpx;
  border-color: rgba(255, 255, 255, 0.75);
}

.corner-tl {
  top: 26rpx;
  left: 26rpx;
  border-top-width: 4rpx;
  border-left-width: 4rpx;
  border-style: solid;
  border-right: 0;
  border-bottom: 0;
  border-top-left-radius: 16rpx;
}

.corner-tr {
  top: 26rpx;
  right: 26rpx;
  border-top-width: 4rpx;
  border-right-width: 4rpx;
  border-style: solid;
  border-left: 0;
  border-bottom: 0;
  border-top-right-radius: 16rpx;
}

.corner-bl {
  bottom: 26rpx;
  left: 26rpx;
  border-bottom-width: 4rpx;
  border-left-width: 4rpx;
  border-style: solid;
  border-right: 0;
  border-top: 0;
  border-bottom-left-radius: 16rpx;
}

.corner-br {
  bottom: 26rpx;
  right: 26rpx;
  border-bottom-width: 4rpx;
  border-right-width: 4rpx;
  border-style: solid;
  border-left: 0;
  border-top: 0;
  border-bottom-right-radius: 16rpx;
}

.focus-dot {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 2rpx dashed rgba(255, 255, 255, 0.35);
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.focus-dot-inner {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: #ff3b30;
}

.hint-text {
  text-align: center;
}

.hint-text-inner {
  font-size: 28rpx;
  color: rgba(0, 0, 0, 0.55);
}

.bottom-controls {
  padding: 0 40rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.side-btn {
  width: 92rpx;
  height: 92rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
}

.side-btn--off {
  background: rgba(82, 196, 26, 0.92);
  box-shadow: 0 14rpx 28rpx rgba(82, 196, 26, 0.28), 0 10rpx 24rpx rgba(0, 0, 0, 0.12);
}

.side-btn--on {
  background: rgba(255, 77, 79, 0.92);
  box-shadow: 0 14rpx 28rpx rgba(255, 77, 79, 0.28), 0 10rpx 24rpx rgba(0, 0, 0, 0.12);
}

.side-btn-icon {
  font-size: 40rpx;
  line-height: 1;
  color: rgba(255, 255, 255, 0.96);
}

.shutter {
  width: 170rpx;
  height: 170rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shutter-ring {
  width: 170rpx;
  height: 170rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14rpx 40rpx rgba(0, 0, 0, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.shutter-core {
  width: 124rpx;
  height: 124rpx;
  border-radius: 50%;
  background: #5d9997;
  box-shadow: inset 0 10rpx 18rpx rgba(255, 255, 255, 0.25), inset 0 -10rpx 18rpx rgba(0, 0, 0, 0.18);
}

.shutter--inactive .shutter-ring {
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 10rpx 28rpx rgba(0, 0, 0, 0.14);
}

.shutter--inactive .shutter-core {
  background: rgba(93, 153, 151, 0.38);
  box-shadow: inset 0 10rpx 18rpx rgba(255, 255, 255, 0.3), inset 0 -10rpx 18rpx rgba(0, 0, 0, 0.08);
}

.shutter--idle .shutter-core {
  background: rgba(93, 153, 151, 0.88);
  box-shadow: inset 0 10rpx 18rpx rgba(255, 255, 255, 0.22), inset 0 -10rpx 18rpx rgba(0, 0, 0, 0.16);
}

.shutter--capturing .shutter-ring {
  box-shadow: 0 18rpx 48rpx rgba(93, 153, 151, 0.38), 0 14rpx 40rpx rgba(0, 0, 0, 0.18);
}

.shutter--capturing .shutter-core {
  background: #5d9997;
}

.gallery-popup {
  height: 80vh;
  background: rgba(255, 255, 255, 0.98);
  border-top-left-radius: 28rpx;
  border-top-right-radius: 28rpx;
  padding: 30rpx;
  padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
  display: flex;
  flex-direction: column;
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
