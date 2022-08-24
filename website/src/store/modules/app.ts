import { loginRequest } from '@/api/user'
import router from '../../router'
import {setToken, getToken, removeToken, getLang} from '@/utils/cookies'

export enum UserActionTypes {
    LOGIN = 'LOGIN',
    LOGOUT = 'LOGOUT'
}

export default {
    namespace: true,
    state: () =>({
        token: getToken() || '',
        lang: getLang() || ''
    }),
    mutations: {
        setToken(state: any, token: string) {
            setToken(token)
        },
        removeToken(state: any) {
            state.token = ''
            removeToken()
        }
    },
    actions: {
        /**
         * 用户登录
         * @author fxbsujay@gmail.com
         * @param commit 方法体
         * @param loginModel 登录请求体对象
         */
        async [UserActionTypes.LOGIN]({commit}: any, loginModel: { username: string, password: string }) {
            let {username, password} = loginModel
            username = username.trim()
            await loginRequest({username, password}).then( async (res) => {
                await commit('setToken', res?.token)
                await router.replace('/')
            }).catch( async err => {
                return Promise.reject();
            })
        },

        /**
         * 用户退出
         * @author fxbsujay@gmail.com
         * @param commit 方法体
         * @param loginModel 登录请求体对象
         */
        async [UserActionTypes.LOGOUT]({commit}: any,{}) {
            commit('removeToken')
            router.replace('/login').then(r => {})
        }
    }
}