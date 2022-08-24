import { RequestParams } from './http'
import https from './http/https'
import { LoginModel } from '@/model/UserModel'
import { Method, ContentType } from './http'

/**
 * <p>用户API</p>
 * @author fxbsujay@gmail.com
 * @version 13:24 2022/6/3
 */
export const loginRequest = (userInfo: RequestParams) => {

    return https(false).request<LoginModel>('auth/login', Method.POST, userInfo, ContentType.json)
}