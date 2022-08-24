import router from './index'
import store from "../store";
import { getToken } from '@/utils/cookies'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

/**
 * @description 路由守卫
 * @author fxbsujay@gmail.com
 * @version 0:28 2022/6/5
 */

const whiteList: Array<string> = ['/login']

/**
 * 加载条
 */
NProgress.configure({ showSpinner: false })

router.beforeEach( (to, from, next)=> {
    NProgress.start()
    const token:string = store.getters.token ||  getToken()
    if (!token) {
        if (whiteList.includes(to.path)) {
            next()
            NProgress.done()
        } else {
            next('/login')
            NProgress.done()
        }
    } else {
        if (to.path === '/login') {
            next('/')
            NProgress.done()
        } else {
            next()
            NProgress.done()
        }
    }
})

router.afterEach((to) => {
    NProgress.done()
})