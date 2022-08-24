import {ContentType, Method, RequestParams} from './type'
import axios, {AxiosError, AxiosInstance, AxiosRequestConfig} from 'axios'
import {Convert} from './json2Model'
import DuplicateRequest from './duplicate'
import {message} from 'ant-design-vue'
import 'ant-design-vue/es/message/style/css'
import { RootObject } from '@/model/BaseModel'

/**
 * <p>Axios封装</p>
 * @author fxbsujay@gmail.com
 * @version 13:24 2022/6/3
 */
export * from './type'

export interface HttpClientConfig extends AxiosRequestConfig {
  /**
   * 参数体
   */
  defaultParams?: RequestParams;
  /**
   * 请求间隔
   */
  clickInterval?: number;
}

export default class HttpClient {
  private _defaultConfig: HttpClientConfig;
  public httpClient: AxiosInstance;

  constructor(options: HttpClientConfig = {}) {
    this.httpClient = axios.create(options);
    this._defaultConfig = options;
  }

  /**
   * @description: 封装请求类
   * @param {Method} method 请求方式
   * @param {string} path 请求路径
   * @param {RequestParams} params 参数
   * @param {ContentType} contentType http配置
   * @param {RequestOptions} optionsSource
   * @return {*}
   */
  async request<T>(
    path: string = '',
    method: Method = Method.GET,
    params?: RequestParams,
    contentType: ContentType = ContentType.json,
    optionsSource?: HttpClientConfig
  ) {
    const options: HttpClientConfig = Object.assign(
      {},
      this._defaultConfig,
      optionsSource
    );
    const { headers, clickInterval } = options;
    if (headers) {
      headers['content-type'] = contentType;
    }
    let allParams = Object.assign(
      {},
      this._defaultConfig.defaultParams,
      params
    );

    const requestConfig: HttpClientConfig = {
      url: `${path}`,
      method,
      headers,
    };

    if (DuplicateRequest.hashUrlAndParams(requestConfig.url ?? '', method, allParams, clickInterval)) {
      return Promise.reject();
    }

    /**
     * 防止请求缓存错乱
     */
    if (method === Method.GET) {
      allParams = {
        ...allParams,
        ...{ _t: new Date().getTime() }
      }
    }

    if (contentType === ContentType.form) {
      requestConfig.params = allParams;
    } else {
      requestConfig.data = JSON.stringify(allParams);
    }
    return this.httpClient
      .request(requestConfig)
      .then(res => {
        const data: string = JSON.stringify(res.data);
        if (res.status >= 200 && res.status < 300) {
          let {code, data: result, msg } = Convert.jsonToModel(data) as RootObject<T>
          if (code !== 200) {
            return Promise.reject(new AxiosError(msg));
          }
          return result as T;
        }
        return Promise.reject(new AxiosError('网络异常 ！'));
      })
      .catch(async error => {
        message.error(error.message)
        return Promise.reject();
      });
  }
}
