import https from './http/https'
import { Method, ContentType } from './http'
import { StorageModel, TrackerModel } from '@/model/Models'

/**
 * <p>查询tracker节点信息</p>
 *
 * @author fxbsujay@gmail.com
 * @version 11:19 2022/8/30
 */
export const queryTreeApi = () => {
    return https(false).request<Array<StorageModel>>('tracker/tree', Method.GET, {}, ContentType.form)
}

/**
 * <p>查询tracker节点信息</p>
 *
 * @author fxbsujay@gmail.com
 * @version 16:26 2022/8/30
 */
export const queryInfoApi = () => {
    return https(false).request<TrackerModel>('tracker/info', Method.GET, {}, ContentType.form)
}