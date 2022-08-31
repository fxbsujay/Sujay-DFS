import { defineComponent, reactive } from 'vue'
import { queryTreeApi } from '@/api/tracker'
import { FileTreeModel } from '@/model/Models'
export default defineComponent({
    name: 'Storage',
    setup() {

        const data = reactive({
            treeLoading: false,
            fileTree: new FileTreeModel(),
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
        }

        init()

        return () => (
            <>
                <a-directory-tree
                    tree-data={ [data.fileTree] }
                    field-names={ data.fieldNames }
                >
                </a-directory-tree>
            </>
        )
    }
})