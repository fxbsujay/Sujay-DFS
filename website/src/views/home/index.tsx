/**
 * 首页
 *
 * @author fxbsujay@gmail.com
 * @version 13:56 2022/8/24
 */
import { defineComponent, reactive } from 'vue'
import { TableColumnType } from 'ant-design-vue'
import { StorageModel, TrackerModel, FileTreeModel } from '@/model/Models'
import { queryListApi } from '@/api/storage'
import { queryInfoApi, queryTreeApi } from '@/api/tracker'

export default defineComponent({
    name: 'Home',
    setup () {
        const columns: TableColumnType<StorageModel>[] = [
            {
                key: 'host',
                title: 'host',
                dataIndex: 'host',
                align: 'center'
            },
            {
                key: 'status',
                title: 'status',
                dataIndex: 'status',
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
        ]

        const data = reactive({
            loading: false,
            trackerInfo: new TrackerModel(),
            list: new Array<StorageModel>
        })


        const init = () => {
            data.loading = true
            queryListApi().then( res => {
                data.list = res
                data.loading = false
            }).catch( res => {
                data.loading = false
            })

            queryInfoApi().then( res => {
                data.trackerInfo = res
            })

        }

        init()

        return () =>(
           <div>
               <a-row>
                   <a-descriptions title="Tracker Info">
                       <a-descriptions-item label="host">{ data.trackerInfo.host }</a-descriptions-item>
                       <a-descriptions-item label="image file path">{ data.trackerInfo.baseDir }</a-descriptions-item>
                       <a-descriptions-item label="ready log file path">{ data.trackerInfo.logBaseDir }</a-descriptions-item>
                       <a-descriptions-item label="port">{ data.trackerInfo.port }</a-descriptions-item>
                       <a-descriptions-item label="httpPort">{ data.trackerInfo.httpPort }</a-descriptions-item>
                   </a-descriptions>
               </a-row>
               <a-divider orientation="left">storage list</a-divider>
               <a-spin
                   spinning={ data.loading }
               >
                   <a-table data-source={ data.list } columns={ columns } />
               </a-spin>

           </div>
        )
    }
})