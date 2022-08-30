/**
 * 首页
 *
 * @author fxbsujay@gmail.com
 * @version 13:56 2022/8/24
 */
import { defineComponent, reactive } from 'vue'
import { TableColumnType } from 'ant-design-vue'
import { StorageModel } from '@/model/Models'
import { queryListApi } from '@/api/storage'
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
            list: new Array<StorageModel>
        })

        const queryList = () => {
            data.loading = true
            queryListApi().then( res => {
                data.list = res
                data.loading = false
            }).catch( res => {
                data.loading = false
            })
        }

        queryList()

        return () =>(
           <div>
               <a-row>
                   <a-descriptions title="Tracker Info">
                       <a-descriptions-item label="host">localhost</a-descriptions-item>
                       <a-descriptions-item label="base file path">/susu/</a-descriptions-item>
                       <a-descriptions-item label="user">admin</a-descriptions-item>
                       <a-descriptions-item label="port">8080</a-descriptions-item>
                       <a-descriptions-item label="client heartbeat interval">
                          80000 s
                       </a-descriptions-item>
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