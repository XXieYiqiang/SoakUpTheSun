<script lang="ts" setup>
import { t } from '@/locale/index'

definePage({
  style: {
    navigationBarTitleText: '志愿者房间',
    navigationStyle: 'custom',
  },
})

const currentUser = ref({
  avatar: '',
  nickname: '我',
})

const volunteers = ref([
  {
    id: 1,
    nickname: '志愿者1',
    avatar: '',
    status: 'online',
  },
  {
    id: 2,
    nickname: '志愿者2',
    avatar: '',
    status: 'online',
  },
])

onMounted(() => {
  const userInfo = uni.getStorageSync('user_info')
  if (userInfo && userInfo.userAvatar) {
    currentUser.value.avatar = userInfo.userAvatar
  }
})

function handleDisconnect() {
  uni.showModal({
    title: '提示',
    content: '确定要断开连接吗？',
    success: (res) => {
      if (res.confirm) {
        uni.navigateBack()
        uni.showToast({
          title: '已断开连接',
          icon: 'none',
        })
      }
    },
  })
}
</script>

<template>
  <view class="room-container">
    <u-navbar
      title="语音通话"
      :safe-area-inset-top="true"
      :placeholder="true"
      :border="false"
      background="#7ec4c2"
      title-color="#fff"
      :title-size="34"
      :auto-back="true"
      back-icon-color="#fff"
    />

    <view class="room-content">
      <!-- 顶部状态提示 -->
      <view class="status-tip">
        正在与志愿者通话中...
      </view>

      <!-- 志愿者区域 (上方) -->
      <view class="volunteers-section">
        <view
          v-for="(vol, index) in volunteers"
          :key="vol.id"
          class="volunteer-item"
        >
          <view class="avatar-wrapper volunteer-avatar">
            <image
              class="avatar-img"
              :src="vol.avatar || `https://images.unsplash.com/photo-15${index}99-d1d0cf377fde?w=150&h=150&fit=crop`"
              mode="aspectFill"
            />
            <view class="user-label">{{ vol.nickname }}</view>
            <view class="wave-animation">
              <view class="bar bar1" />
              <view class="bar bar2" />
              <view class="bar bar3" />
            </view>
          </view>
        </view>
      </view>

      <!-- 本人区域 (中间) -->
      <view class="user-section">
        <view class="avatar-wrapper my-avatar">
          <image
            class="avatar-img"
            :src="currentUser.avatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop'"
            mode="aspectFill"
          />
          <view class="user-label">我</view>
        </view>
      </view>

      <!-- 底部操作区 -->
      <view class="footer-section">
        <view class="action-btn mute-btn">
          <u-icon name="mic" size="50" color="#fff" />
          <text class="btn-label">静音</text>
        </view>
        
        <view class="disconnect-btn" @click="handleDisconnect">
          <u-icon name="phone-fill" size="60" color="#fff" />
        </view>

        <view class="action-btn speaker-btn">
          <u-icon name="volume-up" size="50" color="#fff" />
          <text class="btn-label">免提</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.room-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(180deg, #7ec4c2 0%, #a0d8d6 100%);
  display: flex;
  flex-direction: column;

  .room-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40rpx;
    box-sizing: border-box;
    position: relative;
  }

  .status-tip {
    color: #ffffff;
    font-size: 28rpx;
    margin-bottom: 60rpx;
    opacity: 0.9;
  }

  .avatar-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;

    .avatar-img {
      border-radius: 50%;
      border: 4rpx solid rgba(255, 255, 255, 0.6);
      background: rgba(255, 255, 255, 0.2);
      object-fit: cover;
    }

    .user-label {
      margin-top: 20rpx;
      font-size: 32rpx;
      color: #ffffff;
      font-weight: 500;
    }
  }

  .volunteers-section {
    width: 100%;
    display: flex;
    justify-content: center;
    gap: 60rpx;
    margin-bottom: 80rpx;

    .volunteer-item {
      .volunteer-avatar {
        .avatar-img {
          width: 180rpx;
          height: 180rpx;
        }

        .wave-animation {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6rpx;
          margin-top: 10rpx;
          height: 20rpx;

          .bar {
            width: 6rpx;
            background-color: #ffffff;
            border-radius: 4rpx;
            animation: wave 1s ease-in-out infinite;
          }
          .bar1 { height: 10rpx; animation-delay: 0s; }
          .bar2 { height: 16rpx; animation-delay: 0.1s; }
          .bar3 { height: 10rpx; animation-delay: 0.2s; }
        }
      }
    }
  }

  .user-section {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: flex-start;

    .my-avatar {
      .avatar-img {
        width: 140rpx;
        height: 140rpx;
        opacity: 0.9;
      }
      
      .user-label {
        font-size: 28rpx;
        color: rgba(255, 255, 255, 0.9);
      }
    }
  }

  .footer-section {
    width: 100%;
    display: flex;
    justify-content: space-around;
    align-items: center;
    padding-bottom: 80rpx;

    .action-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16rpx;
      opacity: 0.9;
      transition: opacity 0.3s;

      &:active {
        opacity: 1;
      }

      .btn-label {
        font-size: 24rpx;
        color: #fff;
      }
    }

    .disconnect-btn {
      width: 140rpx;
      height: 140rpx;
      border-radius: 50%;
      background-color: #ff6b6b;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 8rpx 24rpx rgba(255, 107, 107, 0.3);
      transition: transform 0.2s;

      &:active {
        transform: scale(0.95);
      }
    }
  }

  @keyframes wave {
    0%, 100% { height: 10rpx; }
    50% { height: 20rpx; }
  }
}
</style>
