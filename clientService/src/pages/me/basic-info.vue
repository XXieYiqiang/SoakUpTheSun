<script lang="ts" setup>
import type { RegionData } from './region-data'
import type { IUserInfoRes } from '@/api/types/login'
import { t } from '@/locale/index'
import { regionData } from './region-data'

definePage({
  style: {
    navigationBarTitleText: '%basicInfo.title%',
    navigationStyle: 'custom',
  },
})

const userInfo = reactive({
  avatar: '',
  nickname: '用户昵称',
  gender: '男',
  region: '北京市',
  phone: '',
  fingerprintCode: 'FP-2024-001234',
  profile: '这个人很懒，什么都没有留下这个人很懒，什么都没有留下这个人很懒，什么都没有留下这个人很懒，什么都没有留下这个人很懒，什么都没有留下这个人很懒，什么都没有留下',
  createTime: '2024-01-01',
})

import { getUserInfo, updateUser } from '@/api/login'
import { useUserStore } from '@/store'
const STORAGE_KEY = 'login_credentials'
const userStore = useUserStore()
const serverUserInfo = ref<IUserInfoRes | null>(null)
const saving = ref(false)

onShow(() => {
  fetchUserInfo()
})

async function fetchUserInfo() {
  try {
    const saved = uni.getStorageSync(STORAGE_KEY)
    if (!saved.phone) {
      console.warn('未找到保存的登录信息')
      uni.switchTab({
        url: '/pages/login/login',
      })
      return
    }
    const res = await getUserInfo(saved.phone)
    const data = res
    if (data) {
      serverUserInfo.value = data
      userInfo.nickname = data.userName || userInfo.nickname
      userInfo.phone = data.userAccount || userInfo.phone
      userInfo.avatar = data.userAvatar || userInfo.avatar
      userInfo.gender = data.sex === 1 ? '男' : (data.sex === 0 ? '女' : '未知')
      userInfo.region = data.location || userInfo.region
      userInfo.profile = data.userProfile || userInfo.profile
      if (data.createTime) {
         userInfo.createTime = data.createTime.split(' ')[0]
      }
      
      // 更新storage
      uni.setStorageSync('user_info', data)
      userStore.setUserInfo(data)
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const showNicknamePopup = ref(false)
const showAvatarPopup = ref(false)
const showProfilePopup = ref(false)
const showGenderPopup = ref(false)
const showRegionPopup = ref(false)
const tempNickname = ref('')
const tempAvatar = ref('')
const tempProfile = ref('')
const tempGender = ref('')
const tempRegion = ref('')
const selectedProvince = ref<RegionData | null>(null)
const selectedCity = ref<RegionData | null>(null)
const selectedDistrict = ref<RegionData | null>(null)
const currentTab = ref(0)
const locating = ref(false)

function applyViewFromServer(data: IUserInfoRes) {
  userInfo.nickname = data.userName || userInfo.nickname
  userInfo.phone = data.userAccount || userInfo.phone
  userInfo.avatar = data.userAvatar || userInfo.avatar
  userInfo.profile = data.userProfile || userInfo.profile
  userInfo.gender = data.sex === 1 ? '男' : (data.sex === 0 ? '女' : '未知')
  userInfo.region = data.location || userInfo.region
  if (data.createTime) {
    userInfo.createTime = data.createTime.split(' ')[0]
  }
}

async function saveUserPatch(patch: Partial<IUserInfoRes>) {
  if (saving.value)
    return
  const base = serverUserInfo.value || (uni.getStorageSync('user_info') as IUserInfoRes | null) || null
  if (!base) {
    await fetchUserInfo()
  }
  const merged: Partial<IUserInfoRes> = {
    ...(serverUserInfo.value || {}),
    ...patch,
  }
  const userAccount = merged.userAccount || base?.userAccount || userInfo.phone
  if (!userAccount) {
    uni.showToast({
      title: '缺少账号信息',
      icon: 'none',
      duration: 2000,
    })
    return
  }
  const payload: Partial<IUserInfoRes> = {
    userAccount,
    userName: merged.userName,
    userAvatar: merged.userAvatar,
    userProfile: merged.userProfile,
  }

  Object.keys(payload).forEach((k) => {
    if ((payload as any)[k] === undefined) {
      delete (payload as any)[k]
    }
  })

  try {
    saving.value = true
    uni.showLoading({
      title: t('basicInfo.saving') || '保存中...',
      mask: true,
    })
    const updated = await updateUser(payload)
    serverUserInfo.value = { ...(serverUserInfo.value || ({} as any)), ...(updated as any) }
    uni.setStorageSync('user_info', serverUserInfo.value)
    userStore.setUserInfo(serverUserInfo.value as IUserInfoRes)
    applyViewFromServer(serverUserInfo.value as IUserInfoRes)
    uni.hideLoading()
    uni.showToast({
      title: t('basicInfo.saveSuccess'),
      icon: 'success',
      duration: 2000,
    })
    return updated
  }
  catch (e) {
    uni.hideLoading()
    uni.showToast({
      title: t('basicInfo.saveFailed') || '保存失败',
      icon: 'none',
      duration: 2500,
    })
    throw e
  }
  finally {
    saving.value = false
  }
}

const genderOptions = computed(() => [
  {
    label: t('basicInfo.male'),
    value: '男',
  },
  {
    label: t('basicInfo.female'),
    value: '女',
  },
])

const provinceList = computed(() => regionData)

const cityList = computed(() => {
  if (!selectedProvince.value)
    return []
  return selectedProvince.value.children || []
})

const districtList = computed(() => {
  if (!selectedCity.value)
    return []
  return selectedCity.value.children || []
})

const infoList = computed(() => [
  {
    key: 'nickname',
    label: t('basicInfo.nickname'),
    value: userInfo.nickname,
    icon: 'account',
  },
  // {
  //   key: 'gender',
  //   label: t('basicInfo.gender'),
  //   value: userInfo.gender,
  //   icon: 'man',
  // },
  // {
  //   key: 'region',
  //   label: t('basicInfo.region'),
  //   value: userInfo.region,
  //   icon: 'map',
  // },
  {
    key: 'phone',
    label: t('basicInfo.userAccount'),
    value: userInfo.phone,
    icon: 'phone',
    disabled: true,
  },
  {
    key: 'profile',
    label: t('basicInfo.profile'),
    value: userInfo.profile,
    icon: 'file-text',
  },
  {
    key: 'fingerprintCode',
    label: t('basicInfo.fingerprintCode'),
    value: userInfo.fingerprintCode,
    icon: 'fingerprint',
    disabled: true,
  },
  // {
  //   key: 'createTime',
  //   label: t('basicInfo.createTime'),
  //   value: userInfo.createTime,
  //   icon: 'clock',
  //   disabled: true,
  // },
])

function handleEditAvatar() {
  tempAvatar.value = userInfo.avatar
  showAvatarPopup.value = true
}

function handleEditInfo(item: any) {
  console.log('🚀 ~ handleEditInfo ~ item:', item)
  if (item.key === 'nickname') {
    tempNickname.value = userInfo.nickname
    showNicknamePopup.value = true
  }
  else if (item.key === 'profile') {
    tempProfile.value = userInfo.profile
    showProfilePopup.value = true
  }
  else if (item.key === 'gender') {
    tempGender.value = userInfo.gender
    showGenderPopup.value = true
  }
  else if (item.key === 'region') {
    initRegionSelection()
    showRegionPopup.value = true
  }
  else {
    uni.showToast({
      title: `${item.label} ${t('basicInfo.editDeveloping')}`,
      icon: 'none',
      duration: 2000,
    })
  }
}

function initRegionSelection() {
  selectedProvince.value = null
  selectedCity.value = null
  selectedDistrict.value = null
  currentTab.value = 0
  tempRegion.value = userInfo.region
}

function handleProvinceSelect(province: RegionData) {
  selectedProvince.value = province
  currentTab.value = 1
}

function handleCitySelect(city: RegionData) {
  selectedCity.value = city
  if (city.children && city.children.length > 0) {
    currentTab.value = 2
  }
  else {
    saveRegion()
  }
}

function handleDistrictSelect(district: RegionData) {
  selectedDistrict.value = district
  saveRegion()
}

function saveRegion() {
  let regionStr = selectedProvince.value?.label || ''
  if (selectedCity.value) {
    regionStr += selectedCity.value.label
  }
  if (selectedDistrict.value) {
    regionStr += selectedDistrict.value.label
  }
  userInfo.region = regionStr
  showRegionPopup.value = false
  uni.showToast({
    title: t('basicInfo.saveSuccess'),
    icon: 'success',
    duration: 2000,
  })
}

function handleTabChange(index: number) {
  currentTab.value = index
}

function handleLocation() {
  locating.value = true

  uni.showLoading({
    title: t('basicInfo.locating'),
    mask: true,
  })

  uni.getLocation({
    type: 'wgs84',
    altitude: true,
    success: (res) => {
      console.log('定位成功', res)
      console.log(`经度：${res.longitude}`)
      console.log(`纬度：${res.latitude}`)

      uni.hideLoading()

      uni.showToast({
        title: t('basicInfo.locationSuccess'),
        icon: 'success',
        duration: 2000,
      })

      locating.value = false
    },
    fail: (err) => {
      console.error('定位失败：', err)
      uni.hideLoading()

      let errorMsg = t('basicInfo.locationFailed')

      if (err.errMsg.includes('auth deny')) {
        errorMsg = '定位权限被拒绝，请在设置中开启定位权限'
      }
      else if (err.errMsg.includes('no location data')) {
        errorMsg = '无法获取位置信息，请检查GPS是否开启'
      }
      else if (err.errMsg.includes('request:fail')) {
        errorMsg = '定位请求失败，请检查网络连接'
      }

      uni.showModal({
        title: '定位失败',
        content: `${errorMsg}\n\n错误信息：${err.errMsg}`,
        showCancel: false,
        confirmText: '确定',
      })

      locating.value = false
    },
    complete: () => {
      locating.value = false
    },
  })
}

function handleNicknameConfirm() {
  if (tempNickname.value.trim()) {
    const nextName = tempNickname.value.trim()
    saveUserPatch({ userName: nextName }).then(() => {
      userInfo.nickname = nextName
      showNicknamePopup.value = false
    }).catch(() => {})
  }
  else {
    uni.showToast({
      title: t('basicInfo.nicknameRequired'),
      icon: 'none',
      duration: 2000,
    })
  }
}

function handleNicknameCancel() {
  showNicknamePopup.value = false
}

function handleAvatarConfirm() {
  const nextAvatar = tempAvatar.value.trim()
  if (!nextAvatar) {
    uni.showToast({
      title: t('basicInfo.avatarRequired') || '请输入头像链接',
      icon: 'none',
      duration: 2000,
    })
    return
  }
  saveUserPatch({ userAvatar: nextAvatar }).then(() => {
    userInfo.avatar = nextAvatar
    showAvatarPopup.value = false
  }).catch(() => {})
}

function handleAvatarCancel() {
  showAvatarPopup.value = false
}

function handleProfileConfirm() {
  const nextProfile = tempProfile.value.trim()
  if (!nextProfile) {
    uni.showToast({
      title: t('basicInfo.profileRequired') || '请输入简介',
      icon: 'none',
      duration: 2000,
    })
    return
  }
  saveUserPatch({ userProfile: nextProfile }).then(() => {
    userInfo.profile = nextProfile
    showProfilePopup.value = false
  }).catch(() => {})
}

function handleProfileCancel() {
  showProfilePopup.value = false
}

function handleGenderSelect(gender: string) {
  userInfo.gender = gender
  showGenderPopup.value = false
  uni.showToast({
    title: t('basicInfo.saveSuccess'),
    icon: 'success',
    duration: 2000,
  })
}

function handleGenderCancel() {
  showGenderPopup.value = false
}

function handleRegionSelect(region: string) {
  userInfo.region = region
  showRegionPopup.value = false
  uni.showToast({
    title: t('basicInfo.saveSuccess'),
    icon: 'success',
    duration: 2000,
  })
}

function handleRegionCancel() {
  showRegionPopup.value = false
}
</script>

<template>
  <view class="basic-info-container">
    <u-navbar
      :title="t('basicInfo.title')"
      :safe-area-inset-top="true"
      :placeholder="true"
      :border="true"
      bg-color="#ffffff"
      title-color="#000000"
      :title-size="34"
      :auto-back="true"
    />

    <scroll-view
      class="basic-info-content"
      scroll-y
      :show-scrollbar="false"
      enhanced
      :bounces="false"
    >
      <view class="avatar-section">
        <view class="avatar-wrapper">
          <image
            class="avatar"
            :src="userInfo.avatar"
            mode="aspectFill"
          />
          <view
            class="edit-avatar-btn"
            @click="handleEditAvatar"
          >
            <u-icon
              name="camera"
              size="32"
              color="#fff"
            />
          </view>
        </view>
      </view>

      <view class="info-section">
        <view
          v-for="(item, index) in infoList"
          :key="index"
          class="info-item"
          :class="{ 'info-item-disabled': item.disabled }"
          @click="!item.disabled && handleEditInfo(item)"
        >
          <view class="info-left">
            <view class="info-icon">
              <u-icon
                :name="item.icon"
                size="36"
                color="#5D9997"
              />
            </view>
            <text class="info-label">
              {{ item.label }}
            </text>
          </view>
          <view class="info-right">
            <text class="info-value">
              {{ item.value }}
            </text>
            <u-icon
              v-if="!item.disabled"
              name="arrow-right"
              size="28"
              color="#c0c0c0"
            />
          </view>
        </view>
      </view>
    </scroll-view>

    <u-modal
      v-model="showNicknamePopup"
      :title="t('basicInfo.editNickname')"
      :show-confirm-button="true"
      :show-cancel-button="true"
      :cancel-text="t('basicInfo.cancel')"
      :confirm-text="t('basicInfo.confirm')"
      @confirm="handleNicknameConfirm"
      @cancel="handleNicknameCancel"
    >
      <view class="nickname-modal-content">
        <u-input
          v-model="tempNickname"
          :placeholder="t('basicInfo.nicknamePlaceholder')"
          border="none"
          :custom-style="{
            borderRadius: '8rpx',
            padding: '20rpx',
          }"
        />
      </view>
    </u-modal>

    <u-modal
      v-model="showAvatarPopup"
      :title="t('basicInfo.editUserAvatar')"
      :show-confirm-button="true"
      :show-cancel-button="true"
      :cancel-text="t('basicInfo.cancel')"
      :confirm-text="t('basicInfo.confirm')"
      @confirm="handleAvatarConfirm"
      @cancel="handleAvatarCancel"
    >
      <view class="nickname-modal-content">
        <u-input
          v-model="tempAvatar"
          :placeholder="t('basicInfo.avatarPlaceholder')"
          border="none"
          :custom-style="{
            borderRadius: '8rpx',
            padding: '20rpx',
          }"
        />
      </view>
    </u-modal>

    <u-modal
      v-model="showProfilePopup"
      :title="t('basicInfo.editProfile')"
      :show-confirm-button="true"
      :show-cancel-button="true"
      :cancel-text="t('basicInfo.cancel')"
      :confirm-text="t('basicInfo.confirm')"
      @confirm="handleProfileConfirm"
      @cancel="handleProfileCancel"
    >
      <view class="nickname-modal-content">
        <textarea
          v-model="tempProfile"
          class="profile-textarea"
          :placeholder="t('basicInfo.profilePlaceholder')"
        />
      </view>
    </u-modal>

    <u-modal
      v-model="showGenderPopup"
      :title="t('basicInfo.editGender')"
      :show-confirm-button="false"
      :show-cancel-button="true"
      :cancel-text="t('basicInfo.cancel')"
      @cancel="handleGenderCancel"
    >
      <view class="gender-modal-content">
        <view
          v-for="option in genderOptions"
          :key="option.value"
          class="gender-option"
          :class="{ 'gender-option-active': tempGender === option.value }"
          @click="handleGenderSelect(option.value)"
        >
          <text class="gender-option-text">
            {{ option.label }}
          </text>
          <u-icon
            v-if="tempGender === option.value"
            name="checkmark"
            size="40"
            color="#5D9997"
          />
        </view>
      </view>
    </u-modal>

    <u-popup
      v-model="showRegionPopup"
      mode="bottom"
      :round="16"
      :close-on-click-overlay="true"
      @close="handleRegionCancel"
    >
      <view class="region-popup">
        <view class="region-popup-header">
          <text class="region-popup-title">
            {{ t('basicInfo.editRegion') }}
          </text>
          <view
            class="region-popup-close"
            @click="handleRegionCancel"
          >
            <u-icon
              name="close"
              size="36"
              color="#999"
            />
          </view>
        </view>
        <view class="region-location-bar">
          <view class="region-location-info">
            <u-icon
              name="map"
              size="32"
              color="#5D9997"
            />
            <text class="region-location-text">
              {{ userInfo.region }}
            </text>
          </view>
          <view
            class="region-location-btn"
            :class="{ 'region-location-btn-loading': locating }"
            @click="handleLocation"
          >
            <u-icon
              name="reload"
              size="28"
              :color="locating ? '#ccc' : '#5D9997'"
            />
          </view>
        </view>
        <view class="region-popup-tabs">
          <view
            class="region-tab"
            :class="{ 'region-tab-active': currentTab === 0 }"
            @click="handleTabChange(0)"
          >
            <text class="region-tab-text">
              {{ selectedProvince?.label || t('basicInfo.selectProvince') }}
            </text>
          </view>
          <view
            v-if="selectedProvince"
            class="region-tab"
            :class="{ 'region-tab-active': currentTab === 1 }"
            @click="handleTabChange(1)"
          >
            <text class="region-tab-text">
              {{ selectedCity?.label || t('basicInfo.selectCity') }}
            </text>
          </view>
          <view
            v-if="selectedCity && selectedCity.children && selectedCity.children.length > 0"
            class="region-tab"
            :class="{ 'region-tab-active': currentTab === 2 }"
            @click="handleTabChange(2)"
          >
            <text class="region-tab-text">
              {{ selectedDistrict?.label || t('basicInfo.selectDistrict') }}
            </text>
          </view>
        </view>
        <view class="region-popup-content">
          <view
            v-if="currentTab === 0"
            class="region-list"
          >
            <view
              v-for="province in provinceList"
              :key="province.value"
              class="region-item"
              :class="{ 'region-item-active': selectedProvince?.value === province.value }"
              @click="handleProvinceSelect(province)"
            >
              <text class="region-item-text">
                {{ province.label }}
              </text>
              <u-icon
                v-if="selectedProvince?.value === province.value"
                name="checkmark"
                size="40"
                color="#5D9997"
              />
            </view>
          </view>
          <view
            v-if="currentTab === 1"
            class="region-list"
          >
            <view
              v-for="city in cityList"
              :key="city.value"
              class="region-item"
              :class="{ 'region-item-active': selectedCity?.value === city.value }"
              @click="handleCitySelect(city)"
            >
              <text class="region-item-text">
                {{ city.label }}
              </text>
              <u-icon
                v-if="selectedCity?.value === city.value"
                name="checkmark"
                size="40"
                color="#5D9997"
              />
            </view>
          </view>
          <view
            v-if="currentTab === 2"
            class="region-list"
          >
            <view
              v-for="district in districtList"
              :key="district.value"
              class="region-item"
              :class="{ 'region-item-active': selectedDistrict?.value === district.value }"
              @click="handleDistrictSelect(district)"
            >
              <text class="region-item-text">
                {{ district.label }}
              </text>
              <u-icon
                v-if="selectedDistrict?.value === district.value"
                name="checkmark"
                size="40"
                color="#5D9997"
              />
            </view>
          </view>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<style lang="scss" scoped>
.basic-info-container {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .basic-info-content {
    flex: 1;
    height: 0;
    padding-bottom: calc(20rpx + constant(safe-area-inset-bottom));
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  }

  .avatar-section {
    background: #ffffff;
    padding: 60rpx 0;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-bottom: 20rpx;

    .avatar-wrapper {
      position: relative;
      width: 160rpx;
      height: 160rpx;

      .avatar {
        width: 160rpx;
        height: 160rpx;
        border-radius: 50%;
        background: #f0f0f0;
      }

      .edit-avatar-btn {
        position: absolute;
        bottom: 0;
        right: 0;
        width: 56rpx;
        height: 56rpx;
        background: #5d9997;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 4rpx solid #ffffff;
      }
    }
  }

  .info-section {
    background: #ffffff;
    overflow: hidden;

    .info-item {
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

      &.info-item-disabled {
        &:active {
          background: transparent;
        }

        .info-left {
          .info-icon {
            background: #f5f5f5;
          }

          .info-label {
            color: #999;
          }
        }

        .info-right {
          .info-value {
            color: #999;
          }
        }
      }

      .info-left {
        display: flex;
        align-items: center;
        flex: 1;

        .info-icon {
          width: 56rpx;
          height: 56rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f0f8f8;
          border-radius: 12rpx;
          margin-right: 20rpx;
        }

        .info-label {
          font-size: 30rpx;
          color: #333;
          font-weight: 400;
        }
      }

      .info-right {
        display: flex;
        align-items: center;

        .info-value {
          font-size: 30rpx;
          color: #666;
          margin-right: 12rpx;
          text-align: right;
          padding-left: 50rpx;
        }
      }
    }
  }

  .nickname-modal-content {
    padding: 20rpx 32rpx 40rpx;
  }

  .profile-textarea {
    width: 100%;
    min-height: 220rpx;
    padding: 20rpx;
    border-radius: 16rpx;
    background: #f8f8f8;
    font-size: 28rpx;
    color: #333;
    box-sizing: border-box;
  }

  .gender-modal-content {
    padding: 20rpx 0;

    .gender-option {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 32rpx 32rpx;
      transition: background 0.3s ease;

      &:active {
        background: #f8f8f8;
      }

      .gender-option-text {
        font-size: 32rpx;
        color: #333;
      }

      &.gender-option-active {
        .gender-option-text {
          color: #5d9997;
          font-weight: 500;
        }
      }
    }
  }

  .region-popup {
    background: #ffffff;
    height: 90vh;

    .region-popup-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 32rpx;
      border-bottom: 1rpx solid #f0f0f0;

      .region-popup-title {
        font-size: 32rpx;
        color: #333;
        font-weight: 500;
      }

      .region-popup-close {
        width: 48rpx;
        height: 48rpx;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .region-location-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 24rpx 32rpx;
      background: #f8f8f8;

      .region-location-info {
        display: flex;
        align-items: center;
        flex: 1;

        .region-location-text {
          font-size: 28rpx;
          color: #333;
          margin-left: 12rpx;
        }
      }

      .region-location-btn {
        width: 56rpx;
        height: 56rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #ffffff;
        border-radius: 50%;
        transition: all 0.3s ease;

        &:active {
          background: #f0f0f0;
        }

        &.region-location-btn-loading {
          opacity: 0.6;
        }
      }
    }

    .region-popup-tabs {
      display: flex;
      border-bottom: 1rpx solid #f0f0f0;
      background: #fff;
      overflow-x: auto;

      .region-tab {
        flex-shrink: 0;
        padding: 24rpx 32rpx;
        position: relative;
        transition: all 0.3s ease;

        .region-tab-text {
          font-size: 28rpx;
          color: #666;
        }

        &.region-tab-active {
          .region-tab-text {
            color: #5d9997;
            font-weight: 500;
          }

          &::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 50%;
            transform: translateX(-50%);
            width: 40rpx;
            height: 4rpx;
            background: #5d9997;
            border-radius: 2rpx;
          }
        }
      }
    }

    .region-popup-content {
      flex: 1;
      overflow: hidden;
      display: flex;
      flex-direction: column;

      .region-list {
        flex: 1;
        overflow-y: auto;

        .region-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 32rpx 32rpx;
          transition: background 0.3s ease;
          border-bottom: 1rpx solid #f5f5f5;

          &:active {
            background: #f8f8f8;
          }

          .region-item-text {
            font-size: 30rpx;
            color: #333;
          }

          &.region-item-active {
            .region-item-text {
              color: #5d9997;
              font-weight: 500;
            }
          }
        }
      }
    }
  }
}
</style>
