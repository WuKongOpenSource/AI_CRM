import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { Capacitor } from '@capacitor/core'
// 本地字体
import '@fontsource/inter/300.css'
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
import '@fontsource/inter/800.css'
import '@fontsource/inter/900.css'
import '@fontsource-variable/noto-sans-sc'
// 本地图标
import 'material-symbols/outlined.css'
import WkIcon from '@/components/common/WkIcon.vue'
import { useTheme } from '@/composables/useTheme'
import { scheduleCapacitorUpdateCheck, scheduleLiveUpdateHealthReport } from '@/utils/capacitorUpdate'

import App from './App.vue'
import router from './router'
import './styles/iconfont.css'
import './styles/main.css'
import './styles/wk-crm-el-field-skin.css'

useTheme()

function isNativeMobileRuntime(): boolean {
  if (typeof window === 'undefined') return false

  const isMobile =
    window.innerWidth < 768 ||
    (typeof window.matchMedia === 'function' && window.matchMedia('(pointer: coarse)').matches)

  return isMobile && Capacitor.isNativePlatform()
}

// Capacitor iOS safe-area: only enable for native mobile shell
if (isNativeMobileRuntime()) {
  document.documentElement.classList.add('wk-native-mobile')
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.component('WkIcon', WkIcon)

// Wait for router to be ready before mounting to prevent duplicate initial navigation
router.isReady().then(() => {
  app.mount('#app')

  if (isNativeMobileRuntime()) {
    scheduleLiveUpdateHealthReport()
    scheduleCapacitorUpdateCheck()
  }
})
