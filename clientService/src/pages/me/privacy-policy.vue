<script lang="ts" setup>
import { t } from '@/locale/index'

definePage({
  style: {
    navigationBarTitleText: '隐私政策',
    navigationStyle: 'custom',
  },
})

const version = 'v1.0.0'

const licenseText = `MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.`
</script>

<template>
  <view class="page">
    <u-navbar
      title="隐私政策"
      :safe-area-inset-top="true"
      :placeholder="true"
      :border="true"
      bg-color="#ffffff"
      title-color="#000000"
      :title-size="34"
      :auto-back="true"
    />

    <scroll-view
      class="content"
      scroll-y
      :show-scrollbar="false"
      enhanced
      :bounces="false"
    >
      <view class="card">
        <text class="title">隐私政策</text>
        <text class="subtitle">{{ t('settings.version') }} {{ version }}</text>
        <text class="desc">
          本页面采用 MIT 开源许可证文本作为示例内容展示。
        </text>
        <text class="license">{{ licenseText }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  height: 0;
  padding: 24rpx;
  box-sizing: border-box;
}

.card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 32rpx 28rpx;
}

.title {
  display: block;
  font-size: 34rpx;
  color: #333;
  font-weight: 600;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.desc {
  display: block;
  margin-top: 20rpx;
  font-size: 28rpx;
  color: #666;
  line-height: 44rpx;
}

.license {
  display: block;
  margin-top: 24rpx;
  font-size: 24rpx;
  color: #333;
  line-height: 38rpx;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
