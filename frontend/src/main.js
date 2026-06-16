import { createApp } from 'vue'

// 修复 Element Plus ResizeObserver 报错
const debounce = (fn, ms) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms) } }
const _ResizeObserver = window.ResizeObserver
window.ResizeObserver = class extends _ResizeObserver {
  constructor(cb) { super(debounce(cb, 100)) }
}

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
