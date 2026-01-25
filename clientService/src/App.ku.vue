<script setup lang="ts">
import { ref } from 'vue'
import FgTabbar from '@/tabbar/index.vue'
import { isPageTabbar } from './tabbar/store'
import { currRoute } from './utils'

import { useTheme } from 'uview-pro';

const { themes, currentTheme , setTheme} = useTheme();
const color = themes.value[0].color
color.primary = '#5d9997'
color.primaryDark = '#4a7a79'
color.primaryDisabled = '#afccd0'
color.primaryLight = '#e2edec'

setTheme('pur')

const isCurrentPageTabbar = ref(true)
onShow(() => {
  console.log('App.ku.vue onShow', currRoute())
  const { path } = currRoute()
  // “蜡笔小开心”提到本地是 '/pages/index/index'，线上是 '/' 导致线上 tabbar 不见了
  // 所以这里需要判断一下，如果是 '/' 就当做首页，也要显示 tabbar
  if (path === '/') {
    isCurrentPageTabbar.value = true
  }
  else {
    isCurrentPageTabbar.value = isPageTabbar(path)
  }
})

const helloKuRoot = ref('Hello AppKuVue')

const exposeRef = ref('this is form app.Ku.vue')

defineExpose({
  exposeRef,
})
</script>

<template>
  <u-config-provider :themes="themes">
    <view>
      <view class="hidden text-center">
        {{ helloKuRoot }}，这里可以配置全局的东西
      </view>

      <KuRootView />

      <FgTabbar v-if="isCurrentPageTabbar" />
    </view>
  </u-config-provider>
</template>
