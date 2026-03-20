<script lang="ts" setup>
import { computed, ref } from 'vue'
import { t } from '@/locale/index'
import { historyData } from './history-data'

definePage({
  style: {
    navigationBarTitleText: '%history.title%',
    navigationStyle: 'custom',
  },
})

const selectedType = ref('all')

const filteredHistory = computed(() => {
  if (selectedType.value === 'all') {
    return historyData
  }
  return historyData.filter(item => item.type === selectedType.value)
})

const typeOptions = computed(() => [
  { label: t('history.all'), value: 'all' },
  { label: t('history.fingerprint'), value: 'fingerprint' },
  { label: t('history.face'), value: 'face' },
  { label: t('history.voice'), value: 'voice' },
  { label: t('history.other'), value: 'other' },
])

function getStatusColor(status: string) {
  switch (status) {
    case 'success':
      return '#19be6b'
    case 'failed':
      return '#fa3534'
    case 'pending':
      return '#ff9900'
    default:
      return '#c0c0c0'
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'success':
      return t('history.success')
    case 'failed':
      return t('history.failed')
    case 'pending':
      return t('history.pending')
    default:
      return ''
  }
}

function getTypeIcon(type: string) {
  switch (type) {
    case 'fingerprint':
      return 'fingerprint'
    case 'face':
      return 'account'
    case 'voice':
      return 'mic'
    case 'other':
      return 'list'
    default:
      return 'list'
  }
}

function handleTypeChange(value: string) {
  selectedType.value = value
}

function handleItemClick(item: any) {
  uni.showActionSheet({
    itemList: [
      t('history.viewDetail'),
      t('history.delete'),
    ],
    success: (res) => {
      if (res.tapIndex === 0) {
        uni.showToast({
          title: t('history.detailDeveloping'),
          icon: 'none',
          duration: 2000,
        })
      }
      else if (res.tapIndex === 1) {
        uni.showToast({
          title: t('history.deleteDeveloping'),
          icon: 'none',
          duration: 2000,
        })
      }
    },
  })
}
</script>

<template>
  <view class="history-container">
    <u-navbar
      :title="t('history.title')"
      :safe-area-inset-top="true"
      :placeholder="true"
      :border="true"
      bg-color="#ffffff"
      title-color="#000000"
      :title-size="34"
      :auto-back="true"
    />

    <view class="filter-section">
      <scroll-view
        class="filter-scroll"
        scroll-x
        :show-scrollbar="false"
      >
        <view class="filter-list">
          <view
            v-for="option in typeOptions"
            :key="option.value"
            class="filter-item"
            :class="{ active: selectedType === option.value }"
            @click="handleTypeChange(option.value)"
          >
            {{ option.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <scroll-view
      class="history-content"
      scroll-y
      :show-scrollbar="false"
      enhanced
      :bounces="false"
    >
      <view
        v-if="filteredHistory.length === 0"
        class="empty-wrapper"
      >
        <u-empty
          mode="list"
          :text="t('history.empty')"
        />
      </view>

      <view
        v-else
        class="history-list"
      >
        <view
          v-for="item in filteredHistory"
          :key="item.id"
          class="history-item"
          @click="handleItemClick(item)"
        >
          <view class="item-left">
            <view class="item-icon">
              <u-icon
                :name="getTypeIcon(item.type)"
                size="40"
                color="#5D9997"
              />
            </view>
            <view class="item-info">
              <view class="item-title">
                {{ item.title }}
              </view>
              <view class="item-description">
                {{ item.description }}
              </view>
              <view class="item-meta">
                <view class="meta-item">
                  <u-icon
                    name="clock"
                    size="24"
                    color="#999"
                  />
                  <text class="meta-text">
                    {{ item.time }}
                  </text>
                </view>
                <view
                  v-if="item.duration"
                  class="meta-item"
                >
                  <u-icon
                    name="timer"
                    size="24"
                    color="#999"
                  />
                  <text class="meta-text">
                    {{ item.duration }}
                  </text>
                </view>
                <view
                  v-if="item.fileSize"
                  class="meta-item"
                >
                  <u-icon
                    name="file-text"
                    size="24"
                    color="#999"
                  />
                  <text class="meta-text">
                    {{ item.fileSize }}
                  </text>
                </view>
              </view>
            </view>
          </view>
          <view class="item-right">
            <view
              class="item-status"
              :style="{ color: getStatusColor(item.status) }"
            >
              {{ getStatusText(item.status) }}
            </view>
            <view class="item-arrow">
              <u-icon
                name="arrow-right"
                size="32"
                color="#c0c0c0"
              />
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.history-container {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .filter-section {
    background: #ffffff;
    padding: 24rpx 0;
    flex-shrink: 0;
    border-bottom: 1rpx solid #e5e5e5;

    .filter-scroll {
      width: 100%;
      white-space: nowrap;

      .filter-list {
        display: inline-flex;
        padding: 0 24rpx;

        .filter-item {
          padding: 12rpx 28rpx;
          margin-right: 16rpx;
          border-radius: 40rpx;
          font-size: 28rpx;
          color: #666;
          background: #f5f5f5;
          transition: all 0.3s ease;
          white-space: nowrap;

          &:last-child {
            margin-right: 0;
          }

          &.active {
            background: #5d9997;
            color: #ffffff;
            font-weight: 500;
          }

          &:active {
            opacity: 0.8;
          }
        }
      }
    }
  }

  .history-content {
    flex: 1;
    height: 0;
    padding: 24rpx;
    box-sizing: border-box;
    max-width: 100%;
    width: 100%;
  }

  .empty-wrapper {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #ffffff;
    border-radius: 16rpx;
  }

  .history-list {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
  }

  .history-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #ffffff;
    border-radius: 16rpx;
    padding: 24rpx;
    transition: all 0.3s ease;

    &:active {
      background: #f8f8f8;
    }

    .item-left {
      display: flex;
      align-items: flex-start;
      flex: 1;
      min-width: 0;

      .item-icon {
        width: 80rpx;
        height: 80rpx;
        border-radius: 12rpx;
        background: #f0f8f8;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 20rpx;
        flex-shrink: 0;
      }

      .item-info {
        flex: 1;
        min-width: 0;

        .item-title {
          font-size: 30rpx;
          color: #333;
          font-weight: 500;
          margin-bottom: 8rpx;
        }

        .item-description {
          font-size: 26rpx;
          color: #666;
          margin-bottom: 12rpx;
          line-height: 1.4;
        }

        .item-meta {
          display: flex;
          align-items: center;
          flex-wrap: wrap;

          .meta-item {
            display: flex;
            align-items: center;
            margin-right: 24rpx;
            margin-bottom: 4rpx;

            &:last-child {
              margin-right: 0;
            }

            .meta-text {
              font-size: 22rpx;
              color: #999;
              margin-left: 6rpx;
            }
          }
        }
      }
    }

    .item-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      margin-left: 16rpx;
      flex-shrink: 0;

      .item-status {
        font-size: 24rpx;
        font-weight: 500;
        padding: 6rpx 12rpx;
        border-radius: 8rpx;
        background: rgba(0, 0, 0, 0.05);
        margin-bottom: 8rpx;
      }

      .item-arrow {
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}
</style>
