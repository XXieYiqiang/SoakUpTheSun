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
  gender: string
  age: number
  skills: string[]
}

const searchKeyword = ref('')
const currentLetter = ref('')
const scrollViewRef = ref<any>(null)
const showDetailPopup = ref(false)
const selectedVolunteer = ref<Volunteer | null>(null)

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

function handleContactClick(volunteer: Volunteer) {
  selectedVolunteer.value = volunteer
  showDetailPopup.value = true
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
          <u-cell-group>
            <u-cell-item
              v-for="volunteer in groupedData[letter]"
              :key="volunteer.id"
              :title="volunteer.name"
              :label="volunteer.phone"
              :arrow="true"
              @click="handleContactClick(volunteer)"
            >
              <template #icon>
                <view class="contact-avatar">
                  <text class="avatar-text">
                    {{ volunteer.name.charAt(0) }}
                  </text>
                </view>
              </template>
            </u-cell-item>
          </u-cell-group>
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

    <u-popup
      v-model="showDetailPopup"
      mode="center"
      :round="16"
      :closeable="false"
      :safe-area-inset-bottom="true"
    >
      <view
        v-if="selectedVolunteer"
        class="volunteer-detail"
      >
        <view class="detail-header">
          <view class="detail-avatar">
            <text class="detail-avatar-text">
              {{ selectedVolunteer.name.charAt(0) }}
            </text>
          </view>
          <view class="detail-name">
            {{ selectedVolunteer.name }}
          </view>
          <view class="detail-id">
            {{ selectedVolunteer.volunteerId }}
          </view>
        </view>

        <view class="detail-content">
          <view class="detail-item">
            <view class="detail-label">
              <u-icon name="phone" size="32" color="#5D9997" />
              <text class="label-text">电话</text>
            </view>
            <view class="detail-value">
              {{ selectedVolunteer.phone }}
            </view>
          </view>

          <view class="detail-item">
            <view class="detail-label">
              <u-icon name="account" size="32" color="#5D9997" />
              <text class="label-text">性别</text>
            </view>
            <view class="detail-value">
              {{ selectedVolunteer.gender }}
            </view>
          </view>

          <view class="detail-item">
            <view class="detail-label">
              <u-icon name="calendar" size="32" color="#5D9997" />
              <text class="label-text">年龄</text>
            </view>
            <view class="detail-value">
              {{ selectedVolunteer.age }}岁
            </view>
          </view>

          <view class="detail-item">
            <view class="detail-label">
              <u-icon name="map" size="32" color="#5D9997" />
              <text class="label-text">地区</text>
            </view>
            <view class="detail-value">
              {{ selectedVolunteer.region }}
            </view>
          </view>

          <view class="detail-item full-width">
            <view class="detail-label">
              <u-icon name="star" size="32" color="#5D9997" />
              <text class="label-text">技能</text>
            </view>
            <view class="detail-value skills">
              <view
                v-for="(skill, index) in selectedVolunteer.skills"
                :key="index"
                class="skill-tag"
              >
                {{ skill }}
              </view>
            </view>
          </view>
        </view>

        <view class="detail-footer">
          <u-button
            type="primary"
            @click="showDetailPopup = false"
          >
            关闭
          </u-button>
        </view>
      </view>
    </u-popup>
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
  }

  .contact-avatar {
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #5d9997 0%, #4a7a78 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-right: 20px;

    .avatar-text {
      font-size: 40rpx;
      color: #ffffff;
      font-weight: 600;
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

  .volunteer-detail {
    width: 600rpx;
    max-height: 80vh;
    overflow-y: auto;
    background: #ffffff;
    padding: 40rpx 32rpx;

    .detail-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 40rpx;

      .detail-avatar {
        width: 120rpx;
        height: 120rpx;
        border-radius: 50%;
        background: linear-gradient(135deg, #5d9997 0%, #4a7a78 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 20rpx;

        .detail-avatar-text {
          font-size: 48rpx;
          color: #ffffff;
          font-weight: 600;
        }
      }

      .detail-name {
        font-size: 36rpx;
        color: #333333;
        font-weight: 600;
        margin-bottom: 8rpx;
      }

      .detail-id {
        font-size: 24rpx;
        color: #999999;
      }
    }

    .detail-content {
      .detail-item {
        display: flex;
        align-items: center;
        padding: 24rpx 0;
        border-bottom: 1rpx solid #f0f0f0;

        &.full-width {
          flex-direction: column;
          align-items: flex-start;
        }

        &:last-child {
          border-bottom: none;
        }

        .detail-label {
          display: flex;
          align-items: center;
          min-width: 120rpx;

          .label-text {
            font-size: 28rpx;
            color: #666666;
            margin-left: 8rpx;
          }
        }

        .detail-value {
          flex: 1;
          font-size: 28rpx;
          color: #333333;
          margin-left: 20rpx;

          &.skills {
            display: flex;
            flex-wrap: wrap;
            gap: 12rpx;
            margin-top: 16rpx;
            margin-left: 0;

            .skill-tag {
              padding: 8rpx 20rpx;
              background: #f0f8f8;
              color: #5d9997;
              font-size: 24rpx;
              border-radius: 8rpx;
            }
          }
        }
      }
    }

    .detail-footer {
      margin-top: 40rpx;
      padding-top: 32rpx;
      border-top: 1rpx solid #f0f0f0;
    }
  }
}
</style>
