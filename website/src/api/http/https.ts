/**
 * <p>请求客户端配置</p>
 * @author fxbsujay@gmail.com
 * @version 13:24 2022/6/3
 */
import HttpClient, { HttpClientConfig } from './index'
import Keys from '@/constant/key'
import { getToken } from '@/utils/cookies'

const https = (hasToken: Boolean = true) => {
  const config: HttpClientConfig = {
    baseURL: Keys.httpPath,
    clickInterval: 1000,
    timeout: 5000,
    timeoutErrorMessage: '请求超时',
    headers: {
      [Keys.tokenKey]: hasToken ? getToken() as string : ''
    }
  }
  return new HttpClient(config)
}

export default https
