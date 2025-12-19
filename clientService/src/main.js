import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
// 引入 WebSocketService
// import WebSocketService from './plugins/ws';

Vue.config.productionTip = false
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import uploader from 'vue-simple-uploader'

// 引入全局函数
import globalFunction from '@/libs/globalFunction/index.js'

// 引入自定义的全局配置
import config from '@/config/index.js'

import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css' // 也可以用 'atom-one-dark.css' 等其他主题

// 引入 Video.js 和 vue-video-player
import VideoPlayer from 'vue-video-player'
import 'video.js/dist/video-js.css'

Vue.config.productionTip = false

// 注册 vue-video-player 插件
Vue.use(VideoPlayer)

// 配置 marked
marked.setOptions({
  renderer: new marked.Renderer(),
  highlight: function (code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  gfm: true,
  breaks: true,
  smartLists: true
})

// 加到 Vue 原型链
Vue.prototype.$marked = marked


Vue.use(ElementUI);
Vue.prototype.$config = config
Vue.use(uploader);

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
