<script lang="ts" setup>
import { t } from '@/locale/index'

definePage({
  style: {
    navigationBarTitleText: '%tabbar.me%',
    navigationStyle: 'custom',
  },
})

const userInfo = reactive({
  avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop',
  nickname: '用户昵称',
  phone: '138****8888',
})

const menuList = computed(() => [
  {
    title: t('me.basicInfo'),
    icon: 'account',
    path: '/pages/me/basic-info',
  },
  {
    title: t('me.contacts'),
    icon: 'chat',
    path: '/pages/contacts/contacts',
  },
  {
    title: t('me.generateHistory'),
    icon: 'clock',
    path: '/pages/me/history',
  },
  {
    title: t('me.settings'),
    icon: 'setting',
    path: '/pages/me/settings',
  },
])

function handleMenuClick(item: any) {
  if (item.path) {
    uni.navigateTo({
      url: item.path,
    })
  }
  else {
    uni.showToast({
      title: `${item.title} 功能开发中`,
      icon: 'none',
      duration: 2000,
    })
  }
}

function handleQRCode() {
  uni.showToast({
    title: '二维码功能开发中',
    icon: 'none',
    duration: 2000,
  })
}

function handleEditProfile() {
  uni.showToast({
    title: t('me.editProfile'),
    icon: 'none',
    duration: 2000,
  })
}
</script>

<template>
  <view class="me-container">
    <view class="safe-area-inset-top" />

    <scroll-view
      class="me-content"
      scroll-y
      :show-scrollbar="false"
      enhanced
      :bounces="false"
    >
      <view class="header-section">
        <view class="user-info">
          <image
            class="avatar"
            :src="userInfo.avatar"
            mode="aspectFill"
          />
          <view class="user-details">
            <view class="nickname">
              {{ userInfo.nickname }}
            </view>
            <view class="phone">
              {{ userInfo.phone }}
            </view>
          </view>
        </view>
        <view class="header-actions">
          <view
            class="qr-code-btn"
            @click="handleQRCode"
          >
            <u-icon
              name="scan"
              size="48"
              color="#333"
            />
          </view>
        </view>
      </view>

      <view class="menu-section">
        <view
          v-for="(item, index) in menuList"
          :key="index"
          class="menu-item"
          @click="handleMenuClick(item)"
        >
          <view class="menu-left">
            <view class="menu-icon">
              <u-icon
                :name="item.icon"
                size="44"
                color="#5D9997"
              />
            </view>
            <text class="menu-title">
              {{ item.title }}
            </text>
          </view>
          <view class="menu-right">
            <u-icon
              name="arrow-right"
              size="32"
              color="#c0c0c0"
            />
          </view>
        </view>
      </view>

      <view class="edit-section">
        <view
          class="edit-btn"
          @click="handleEditProfile"
        >
          <text class="edit-text">
            {{ t('me.editProfile') }}
          </text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.me-container {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .safe-area-inset-top {
    height: constant(safe-area-inset-top);
    height: env(safe-area-inset-top);
  }

  .me-content {
    width: 100%;
    flex: 1;
    padding: 0;
    padding-bottom: calc(20rpx + constant(safe-area-inset-bottom) + 100rpx);
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom) + 100rpx);
  }

  .header-section {
    background: #ffffff;
    padding: 40rpx 32rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20rpx;

    .user-info {
      display: flex;
      align-items: center;
      flex: 1;

      .avatar {
        width: 120rpx;
        height: 120rpx;
        border-radius: 16rpx;
        margin-right: 24rpx;
        background: #f0f0f0;
      }

      .user-details {
        flex: 1;

        .nickname {
          font-size: 36rpx;
          font-weight: 600;
          color: #333;
          margin-bottom: 12rpx;
        }

        .phone {
          font-size: 28rpx;
          color: #999;
        }
      }
    }

    .header-actions {
      display: flex;
      align-items: center;

      .qr-code-btn {
        width: 72rpx;
        height: 72rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: #f5f5f5;
        transition: background 0.3s ease;

        &:active {
          background: #e0e0e0;
        }
      }
    }
  }

  .menu-section {
    background: #ffffff;
    overflow: hidden;

    .menu-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 32rpx 24rpx;
      border-bottom: 1rpx solid #f0f0f0;
      transition: background 0.3s ease;

      &:last-child {
        border-bottom: none;
      }

      &:active {
        background: #f8f8f8;
      }

      .menu-left {
        display: flex;
        align-items: center;
        flex: 1;

        .menu-icon {
          width: 64rpx;
          height: 64rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f0f8f8;
          border-radius: 12rpx;
          margin-right: 20rpx;
        }

        .menu-title {
          font-size: 30rpx;
          color: #333;
          font-weight: 400;
        }
      }

      .menu-right {
        display: flex;
        align-items: center;
      }
    }
  }

  .edit-section {
    padding: 40rpx 20rpx;

    .edit-btn {
      width: 100%;
      height: 88rpx;
      background: #ffffff;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.3s ease;

      &:active {
        background: #f0f0f0;
      }

      .edit-text {
        font-size: 30rpx;
        color: #5d9997;
        font-weight: 500;
      }
    }
  }
}
</style>
