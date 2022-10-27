/**
 * 首页
 *
 * @author fxbsujay@gmail.com
 * @version 13:56 2022/8/24
 */
import { defineComponent, reactive } from 'vue'
import { TableColumnType } from 'ant-design-vue'
import { StorageModel, TrackerModel } from '@/model/Models'
import { queryListApi as storageQueryList } from '@/api/storage'
import { queryListApi as trackerQueryList, queryInfoApi } from '@/api/tracker'

const storageColumns: TableColumnType<StorageModel>[] = [
    {
        key: 'host',
        title: 'host',
        dataIndex: 'host',
        align: 'center'
    },
    {
        key: 'port',
        title: 'port',
        dataIndex: 'port',
        align: 'center'
    },
    {
        key: 'httpPort',
        title: 'http port',
        dataIndex: 'httpPort',
        align: 'center'
    },
    {
        key: 'storedSize',
        title: 'stored size',
        dataIndex: 'storedSize',
        align: 'center'
    },
    {
        key: 'filePath',
        title: 'file path',
        dataIndex: 'filePath',
        align: 'center'
    },
    {
        key: 'status',
        title: 'status',
        dataIndex: 'status',
        align: 'center'
    }
]

const trackerColumns: TableColumnType<TrackerModel>[] = [
    {
        key: 'host',
        title: 'host',
        dataIndex: 'host',
        align: 'center'
    },
    {
        key: 'port',
        title: 'port',
        dataIndex: 'port',
        align: 'center'
    },
    {
        key: 'httpPort',
        title: 'http port',
        dataIndex: 'httpPort',
        align: 'center'
    },
    {
        key: 'status',
        title: 'status',
        dataIndex: 'status',
        align: 'center'
    }
]

export default defineComponent({
    name: 'Home',
    setup () {

        const data = reactive({
            storageListLoading: false,
            trackerListLoading: false,
            trackerInfo: new TrackerModel(),
            storageList: new Array<StorageModel>(),
            trackerList: new Array<TrackerModel>(),
        })

        const init = () => {
            data.storageListLoading = true
            data.trackerListLoading = true
            storageQueryList().then( res => {
                data.storageList = res
                data.storageListLoading = false
            }).catch( res => {
                data.storageListLoading = false
            })

            trackerQueryList().then( res => {
                data.trackerListLoading = false
                data.trackerList = res
            }).catch( res => {
                data.trackerListLoading = false
            })

            queryInfoApi().then( res => {
                data.trackerInfo = res
            })
        }

        init()

        return () =>(
           <>
               <a-row>
                   <a-descriptions labelStyle={{ width: '150px' }} title="Tracker Info">
                       <a-descriptions-item label="host">{ data.trackerInfo.host }</a-descriptions-item>
                       <a-descriptions-item label="base file path">{ data.trackerInfo.baseDir }  , { data.trackerInfo.logBaseDir }</a-descriptions-item>
                       <a-descriptions-item label="port">{ data.trackerInfo.port }</a-descriptions-item>
                       <a-descriptions-item label="httpPort">{ data.trackerInfo.httpPort }</a-descriptions-item>
                       <a-descriptions-item label="total stored size">{ data.trackerInfo.totalStoredSize }</a-descriptions-item>
                       <a-descriptions-item label="count of files">{ data.trackerInfo.fileCount }</a-descriptions-item>
                   </a-descriptions>
               </a-row>

               <a-divider orientation="left">storage list</a-divider>
               <a-spin
                   spinning={ data.storageListLoading }
               >
                   <a-table data-source={ data.storageList } columns={ storageColumns } pagination={ false }/>
               </a-spin>

               <a-divider style={{ marginTop: '40px'}} orientation="left">tracker list</a-divider>
               <a-spin
                   spinning={ data.trackerListLoading }
               >
                   <a-table data-source={ data.trackerList } columns={ trackerColumns } pagination={ false }/>
               </a-spin>

           </>
        )
    }
})