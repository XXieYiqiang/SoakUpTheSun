<script lang="ts" setup>
import { computed, ref } from 'vue'
import { t } from '@/locale/index'

definePage({
  style: {
    navigationBarTitleText: '%me.settings%',
    navigationStyle: 'custom',
  },
})

const currentLocale = ref(uni.getLocale())
const fingerprintEnabled = ref(true)
const isH5 = uni.getSystemInfoSync().uniPlatform === 'web'

const settingsList = computed(() => {
  const list = [
    {
      title: t('settings.fingerprint'),
      icon: 'lock',
      type: 'switch',
      value: fingerprintEnabled.value,
    },
    {
      title: t('settings.setupFingerprint'),
      icon: 'fingerprint',
      type: 'link',
    },
    {
      title: t('settings.language'),
      icon: 'edit-pen',
      type: 'select',
      value: currentLocale.value,
    },
    {
      title: t('settings.about'),
      icon: 'info',
      type: 'link',
    },
    {
      title: t('settings.privacy'),
      icon: 'lock',
      type: 'link',
    },
    {
      title: t('settings.terms'),
      icon: 'file-text',
      type: 'link',
    },
  ]

  return list
})

const displaySettingsList = computed(() => {
  if (isH5) {
    return settingsList.value.filter((item) => {
      return item.title !== t('settings.fingerprint') && item.title !== t('settings.setupFingerprint')
    })
  }
  return settingsList.value
})

function handleSwitchChange(item: any, value: boolean) {
  if (item.type === 'switch') {
    fingerprintEnabled.value = value
    uni.showToast({
      title: value ? t('settings.fingerprintEnabled') : t('settings.fingerprintDisabled'),
      icon: 'none',
      duration: 2000,
    })
  }
}

function handleLanguageChange() {
  uni.showActionSheet({
    itemList: ['简体中文', 'English'],
    success: (res) => {
      const newLocale = res.tapIndex === 0 ? 'zh-Hans' : 'en'
      uni.setLocale(newLocale)
      uni.setStorageSync('locale', newLocale)
      currentLocale.value = newLocale
      uni.showToast({
        title: t('settings.languageChanged'),
        icon: 'none',
        duration: 2000,
      })
    },
  })
}

function handleItemClick(item: any) {
  if (item.type === 'link') {
    if (item.title === t('settings.setupFingerprint')) {
      handleSetupFingerprint()
    }
    else {
      uni.showToast({
        title: `${item.title} 功能开发中`,
        icon: 'none',
        duration: 2000,
      })
    }
  }
  else if (item.type === 'select') {
    handleLanguageChange()
  }
}

function handleSetupFingerprint() {
  uni.checkIsSupportSoterAuthentication({
    success: (res) => {
      const supportModes = res.supportMode || []

      if (!supportModes.includes('fingerPrint')) {
        uni.showToast({
          title: t('settings.fingerprintNotSupported'),
          icon: 'none',
          duration: 2000,
        })
        return
      }

      uni.checkIsSoterEnrolledInDevice({
        checkAuthMode: 'fingerPrint',
        success: (enrollRes) => {
          if (!enrollRes.isEnrolled) {
            uni.showToast({
              title: t('settings.fingerprintNotEnrolled'),
              icon: 'none',
              duration: 2000,
            })
            return
          }

          uni.startSoterAuthentication({
            requestAuthModes: ['fingerPrint'],
            challenge: '123456',
            authContent: t('settings.authContent'),
            success: (authRes) => {
              fingerprintEnabled.value = true
              uni.showToast({
                title: t('settings.fingerprintSetupSuccess'),
                icon: 'success',
                duration: 2000,
              })
            },
            fail: () => {
              uni.showToast({
                title: t('settings.fingerprintSetupFailed'),
                icon: 'none',
                duration: 2000,
              })
            },
          })
        },
        fail: () => {
          uni.showToast({
            title: t('settings.fingerprintCheckFailed'),
            icon: 'none',
            duration: 2000,
          })
        },
      })
    },
    fail: () => {
      uni.showToast({
        title: t('settings.fingerprintCheckFailed'),
        icon: 'none',
        duration: 2000,
      })
    },
  })
}

import { logout } from '@/api/login'

function handleLogout() {
  uni.showModal({
    title: t('settings.logoutTitle'),
    content: t('settings.logoutConfirm'),
    confirmText: t('settings.confirm'),
    cancelText: t('settings.cancel'),
    success: async (res) => {
      if (res.confirm) {
        try {
          await logout()
        } catch (error) {
          console.error('Logout failed:', error)
        }
        
        uni.showToast({
          title: t('settings.logoutSuccess'),
          icon: 'success',
          duration: 2000,
        })
        setTimeout(() => {
          uni.reLaunch({
            url: '/pages/login/login',
          })
        }, 2000)
      }
    },
  })
}
</script>

<template>
  <view class="settings-container">
    <u-navbar
      :title="t('me.settings')"
      :safe-area-inset-top="true"
      :placeholder="true"
      :border="true"
      bg-color="#ffffff"
      title-color="#000000"
      :title-size="34"
      :auto-back="true"
    />

    <scroll-view
      class="settings-content"
      scroll-y
      :show-scrollbar="false"
      enhanced
      :bounces="false"
    >
      <view class="settings-section">
        <view
          v-for="(item, index) in displaySettingsList"
          :key="index"
          class="settings-item"
          @click="handleItemClick(item)"
        >
          <view class="item-left">
            <view class="item-icon">
              <u-icon
                :name="item.icon"
                size="44"
                color="#5D9997"
              />
            </view>
            <text class="item-title">
              {{ item.title }}
            </text>
          </view>
          <view class="item-right">
            <u-switch
              v-if="item.type === 'switch'"
              v-model="fingerprintEnabled"
              active-color="#5D9997"
              @change="(value: boolean) => handleSwitchChange(item, value)"
            />
            <text
              v-else-if="item.type === 'select'"
              class="item-value"
            >
              {{ item.value === 'zh-Hans' ? '简体中文' : 'English' }}
            </text>
            <u-icon
              v-else
              name="arrow-right"
              size="32"
              color="#c0c0c0"
            />
          </view>
        </view>
      </view>

      <view class="logout-section">
        <view
          class="logout-btn"
          @click="handleLogout"
        >
          <text class="logout-text">
            {{ t('settings.logout') }}
          </text>
        </view>
      </view>

      <view class="version-info">
        <text class="version-text">
          {{ t('settings.version') }} 1.0.0
        </text>
      </view>
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.settings-container {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .settings-content {
    flex: 1;
    height: 0;
    padding: 24rpx;
    box-sizing: border-box;
  }

  .settings-section {
    background: #ffffff;
    border-radius: 16rpx;
    overflow: hidden;

    .settings-item {
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

      .item-left {
        display: flex;
        align-items: center;
        flex: 1;

        .item-icon {
          width: 64rpx;
          height: 64rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f0f8f8;
          border-radius: 12rpx;
          margin-right: 20rpx;
        }

        .item-title {
          font-size: 30rpx;
          color: #333;
          font-weight: 400;
        }
      }

      .item-right {
        display: flex;
        align-items: center;

        .item-value {
          font-size: 28rpx;
          color: #999;
          margin-right: 12rpx;
        }
      }
    }
  }

  .logout-section {
    margin-top: 40rpx;

    .logout-btn {
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

      .logout-text {
        font-size: 30rpx;
        color: #fa3534;
        font-weight: 500;
      }
    }
  }

  .version-info {
    margin-top: 40rpx;
    text-align: center;

    .version-text {
      font-size: 24rpx;
      color: #999;
    }
  }
}
</style>
