import https from './http/https'
import { Method, ContentType } from './http'
import { StorageModel } from '@/model/Models'

/**
 * <p>查询storage节点信息</p>
 * @author fxbsujay@gmail.com
 * @version 13:24 2022/8/30
 */
export const queryListApi = () => {
    return https(false).request<Array<StorageModel>>('storage/list', Method.GET, {}, ContentType.form)
}