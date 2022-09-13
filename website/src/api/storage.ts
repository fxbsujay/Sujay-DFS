import https from './http/https'
import {Method, ContentType, RequestParams} from './http'
import { StorageModel } from '@/model/Models'
import {LoginModel} from "@/model/UserModel";

/**
 * <p>查询storage节点信息</p>
 * @author fxbsujay@gmail.com
 * @version 13:24 2022/8/30
 */
export const queryListApi = () => {
    return https(false).request<Array<StorageModel>>('storage/list', Method.GET, {}, ContentType.form)
}

export const uploadApi = (uploadInfo: RequestParams) => {
    return https(false).request<LoginModel>('upload', Method.POST, uploadInfo, ContentType.multipart)
}

export const removeApi = (file: RequestParams) => {
    return https(false).request<boolean>('storage/remove', Method.DELETE, file, ContentType.form)
}