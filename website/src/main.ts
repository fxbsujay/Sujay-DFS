import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from '@/store'
import '@/styles/index.less'
import '@/router/permission'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/antd.css';
import i18n from '@/i18n'
createApp(App)
    .use(router)
    .use(store)
    .use(Antd)
    .use(i18n)
    .mount('#app')
