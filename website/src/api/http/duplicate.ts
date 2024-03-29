import qs from 'qs'
import { RequestParams, Method } from './type'

export default class DuplicateRequest {
  private static _instance: DuplicateRequest;
  private constructor() {}
  static getInstance() {
    if (!this._instance) {
      this._instance = new DuplicateRequest();
    }
    return this._instance;
  }

  static lastRequestTimeStamp = 0;
  static lastIdentityCode = '';

  /**
   * @description: 验证是否是相同的url点击过快
   *
   * @param {string} url
   * @param {Method} method
   * @param {RequestParams} params
   * @param interval
   * @return {*}
   */
  static hashUrlAndParams(
    url: string,
    method: Method,
    params: RequestParams,
    interval: number = 1000
  ) {
    const identityCode = [url, method, qs.stringify(params)].join('&');
    const currentTime = Date.parse(new Date().toString());
    if (
      currentTime - DuplicateRequest.lastRequestTimeStamp < interval &&
      DuplicateRequest.lastIdentityCode === identityCode
    ) {
      return true;
    } else {
      DuplicateRequest.lastRequestTimeStamp = currentTime;
      DuplicateRequest.lastIdentityCode = identityCode;
      return false;
    }
  }
}
