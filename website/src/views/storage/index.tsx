import { defineComponent, reactive } from 'vue'
import { queryTreeApi, queryInfoApi } from '@/api/tracker'
import { FileTreeModel } from '@/model/Models'
import FileTree from './fileTree.vue'
import './index.less'
export default defineComponent({
    name: 'Storage',
    components: {
        FileTree
    },
    setup() {

        const data = reactive({
            treeLoading: false,
            fileTree: new FileTreeModel(),
            requestHeader: '',
            fieldNames: {
                children: 'children',
                title: 'path',
                key: 'path'
            }
        })

        const init = () => {
            data.treeLoading = true
            queryTreeApi().then( res => {
                data.fileTree = res
                data.treeLoading = false
            }).catch( res => {
                data.treeLoading = false
            })

            queryInfoApi().then( res => {
                data.requestHeader = 'http://' + res.host + ':' + res.httpPort
            })
        }

        init()
        return () => (
            <>
                <FileTree treeList={ [data.fileTree] } requestHeader={ data.requestHeader } />
            </>
        )
    }
})
