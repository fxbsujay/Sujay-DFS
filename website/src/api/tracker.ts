import https from './http/https'
import {Method, ContentType, RequestParams} from './http'
import {FileTreeModel, TrackerModel} from '@/model/Models'

/**
 * <p>查询tracker节点信息</p>
 *
 * @author fxbsujay@gmail.com
 * @version 11:19 2022/8/30
 */
export const queryTreeApi = (queryData: RequestParams) => {
    return https(false).request<FileTreeModel>('tracker/tree', Method.GET, queryData, ContentType.form)
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

export const queryListApi = () => {
    return https(false).request<Array<TrackerModel>>('tracker/list', Method.GET, {}, ContentType.form)
}