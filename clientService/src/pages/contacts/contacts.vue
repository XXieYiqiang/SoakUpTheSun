<script lang="ts" setup>
import { t } from '@/locale/index'
import { volunteerData } from './volunteer-data'

definePage({
  style: {
    navigationBarTitleText: '%contacts.title%',
    navigationStyle: 'custom',
  },
})

interface Volunteer {
  id: string
  name: string
  pinyin: string
  phone: string
  region: string
  volunteerId: string
}

const searchKeyword = ref('')
const currentLetter = ref('')
const scrollViewRef = ref<any>(null)

const filteredData = computed(() => {
  if (!searchKeyword.value) {
    return volunteerData
  }

  const keyword = searchKeyword.value.toLowerCase()
  return volunteerData.filter((volunteer) => {
    return volunteer.name.toLowerCase().includes(keyword)
      || volunteer.pinyin.toLowerCase().includes(keyword)
      || volunteer.phone.includes(keyword)
  })
})

const groupedData = computed(() => {
  const grouped: Record<string, Volunteer[]> = {}

  filteredData.value.forEach((volunteer) => {
    const letter = volunteer.pinyin.charAt(0).toUpperCase()
    if (!grouped[letter]) {
      grouped[letter] = []
    }
    grouped[letter].push(volunteer)
  })

  return grouped
})

const indexList = computed(() => {
  return Object.keys(groupedData.value).sort()
})

watch(indexList, (newList) => {
  if (newList.length > 0 && !currentLetter.value) {
    currentLetter.value = newList[0]
  }
}, { immediate: true })

function handleContactClick(contact: Volunteer) {
  uni.showActionSheet({
    itemList: [
      t('contacts.call'),
      t('contacts.message'),
      t('contacts.viewDetail'),
    ],
    success: (res) => {
      if (res.tapIndex === 0) {
        uni.makePhoneCall({
          phoneNumber: contact.phone,
        })
      }
      else if (res.tapIndex === 1) {
        uni.showToast({
          title: t('contacts.messageDeveloping'),
          icon: 'none',
          duration: 2000,
        })
      }
      else if (res.tapIndex === 2) {
        uni.showToast({
          title: t('contacts.detailDeveloping'),
          icon: 'none',
          duration: 2000,
        })
      }
    },
  })
}

function handleSearchInput(e: any) {
  searchKeyword.value = e.detail.value
}

function handleSearchClear() {
  searchKeyword.value = ''
}

function handleScroll(e: any) {
  const scrollTop = e.detail.scrollTop
  const letters = indexList.value

  for (let i = 0; i < letters.length; i++) {
    const letter = letters[i]
    const query = uni.createSelectorQuery()
    query.select(`#section-${letter}`).boundingClientRect()
    query.exec((res) => {
      if (res[0] && res[0].top <= 100) {
        currentLetter.value = letter
      }
    })
  }
}

function scrollToLetter(letter: string) {
  const query = uni.createSelectorQuery()
  query.select(`#section-${letter}`).boundingClientRect()
  query.select('.contacts-scroll').boundingClientRect()
  query.exec((res) => {
    if (res[0] && res[1] && scrollViewRef.value) {
      const targetTop = res[0].top - res[1].top
      scrollViewRef.value.scrollTo({
        top: targetTop,
        duration: 300,
      })
    }
  })
}
</script>

<template>
  <view class="contacts-page">
    <u-navbar
      :title="t('contacts.title')"
      :safe-area-inset-top="true"
      :placeholder="true"
      :border="true"
      bg-color="#ffffff"
      title-color="#000000"
      :title-size="34"
      :auto-back="true"
    />

    <view class="search-bar">
      <u-search
        v-model="searchKeyword"
        :placeholder="t('contacts.searchPlaceholder')"
        :show-action="!!searchKeyword"
        action-text="搜索"
        bg-color="#f5f5f5"
        border-color="transparent"
        @search="handleSearchClear"
        @custom="handleSearchClear"
      />
    </view>

    <scroll-view
      ref="scrollViewRef"
      class="contacts-scroll"
      scroll-y
      @scroll="handleScroll"
    >
      <view
        v-if="indexList.length === 0"
        class="empty-wrapper"
      >
        <u-empty
          mode="search"
          :text="t('contacts.empty')"
        />
      </view>

      <view
        v-else
        class="contacts-list"
      >
        <view
          v-for="letter in indexList"
          :id="`section-${letter}`"
          :key="letter"
          class="letter-section"
        >
          <view class="letter-header">
            {{ letter }}
          </view>
          <view
            v-for="contact in groupedData[letter]"
            :key="contact.id"
            class="contact-item"
            @click="handleContactClick(contact)"
          >
            <view class="contact-avatar">
              <text class="avatar-text">
                {{ contact.name.charAt(0) }}
              </text>
            </view>
            <view class="contact-info">
              <text class="contact-name">
                {{ contact.name }}
              </text>
              <text class="contact-phone">
                {{ contact.phone }}
              </text>
            </view>
            <view class="contact-icon">
              <u-icon
                name="phone-fill"
                size="40"
                color="#5D9997"
              />
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view
      v-if="indexList.length > 0"
      class="index-bar"
    >
      <view
        v-for="letter in indexList"
        :key="letter"
        class="index-item"
        :class="{ active: currentLetter === letter }"
        @click="scrollToLetter(letter)"
      >
        {{ letter }}
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.contacts-page {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .search-bar {
    background: #ffffff;
    padding: 16rpx 24rpx;
    flex-shrink: 0;
    position: relative;
    z-index: 10;
  }

  .contacts-scroll {
    flex: 1;
    height: 0;
    overflow-y: auto;
  }

  .empty-wrapper {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #ffffff;
  }

  .contacts-list {
    background: #ffffff;
  }

  .letter-section {
    .letter-header {
      padding: 16rpx 24rpx;
      background: #f5f5f5;
      font-size: 24rpx;
      color: #999999;
      font-weight: 500;
    }

    .contact-item {
      display: flex;
      align-items: center;
      padding: 24rpx;
      background: #ffffff;
      border-bottom: 1rpx solid #f0f0f0;
      transition: background-color 0.3s;

      &:active {
        background-color: #f8f8f8;
      }

      .contact-avatar {
        width: 96rpx;
        height: 96rpx;
        border-radius: 50%;
        background: linear-gradient(135deg, #5d9997 0%, #4a7a78 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 24rpx;
        flex-shrink: 0;

        .avatar-text {
          font-size: 40rpx;
          color: #ffffff;
          font-weight: 600;
        }
      }

      .contact-info {
        flex: 1;
        min-width: 0;

        .contact-name {
          display: block;
          font-size: 32rpx;
          color: #333333;
          font-weight: 500;
          margin-bottom: 8rpx;
        }

        .contact-phone {
          display: block;
          font-size: 26rpx;
          color: #999999;
        }
      }

      .contact-icon {
        width: 72rpx;
        height: 72rpx;
        border-radius: 50%;
        background: #f0f8f8;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
    }
  }

  .index-bar {
    position: fixed;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    background: rgba(255, 255, 255, 0.9);
    border-radius: 40rpx 0 0 40rpx;
    padding: 16rpx 8rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    box-shadow: -2rpx 0 8rpx rgba(0, 0, 0, 0.1);
    z-index: 100;

    .index-item {
      width: 40rpx;
      height: 40rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20rpx;
      color: #999999;
      margin: 4rpx 0;
      border-radius: 50%;
      transition: all 0.3s;

      &.active {
        background: #5d9997;
        color: #ffffff;
        font-weight: 600;
      }
    }
  }
}
</style>
