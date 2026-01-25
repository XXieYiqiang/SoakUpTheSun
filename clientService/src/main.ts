import uViewPro from 'uview-pro'
import { createSSRApp } from 'vue'
import App from './App.vue'
import { requestInterceptor } from './http/interceptor'
import i18n from './locale/index'
import { routeInterceptor } from './router/interceptor'

import store from './store'
import theme from './style/uViewPro'
import '@/style/index.scss'
import 'virtual:uno.css'

export function createApp() {
  const app = createSSRApp(App)
  app.use(uViewPro, { theme: {
    themes: theme,
    defaultTheme: 'pur',
  } })
  app.use(store)
  app.use(i18n)
  app.use(routeInterceptor)
  app.use(requestInterceptor)

  return {
    app,
  }
}
