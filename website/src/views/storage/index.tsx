import {defineComponent, reactive, ref} from 'vue'
import { queryTreeApi, queryInfoApi } from '@/api/tracker'
import { FileTreeModel } from '@/model/Models'
import FileTree from './fileTree.vue'
import { DeleteOutlined, EyeOutlined, CloudUploadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import './index.less'
import { UploadProps } from "ant-design-vue";
import { uploadApi } from '@/api/storage'
export default defineComponent({
    name: 'Storage',
    components: {
        DeleteOutlined,
        EyeOutlined,
        CloudUploadOutlined,
        SearchOutlined,
        FileTree
    },
    setup() {

        const data = reactive({
            treeLoading: false,
            fileTree: new FileTreeModel(),
            requestHeader: '',
            uploadVisible: false,
            path: '',
            queryData: {
                path: ''
            },
            formData: {
                filepath: ''
            },
            fieldNames: {
                children: 'children',
                title: 'path',
                key: 'path'
            }
        })

        const init = () => {
            data.treeLoading = true
            queryTreeApi(data.queryData).then( res => {
                data.fileTree = res
                data.treeLoading = false
            }).catch( res => {
                data.treeLoading = false
            })

            queryInfoApi().then( res => {
                data.requestHeader = 'http://' + res.host + ':' + res.httpPort
            })
        }

        const fileList = ref<UploadProps['fileList']>([]);

        /**
         * 上传文件
         */
        const uploadHandle = () => {
            const file = new File(fileList.value,data.formData.filepath + "/" + fileList.value[0].name)
            uploadApi({ "file": file }).then( res => {
                data.uploadVisible = false
            })
        }

        const openUpload = ():boolean =>  data.uploadVisible = true

        /**
         * 手动上传
         */
        const beforeUpload: UploadProps['beforeUpload'] = file => {
            fileList.value = [...fileList.value, file];
            return false;
        }

        init()
        return () => (
            <>
                <a-input-search
                    v-model:value={ data.queryData.path }
                    style={{ width: '300px', margin: '10px'}}
                    placeholder={ "input search text" }
                    enter-button
                    onSearch={ init }
                />
                <a-button
                    style={{ width: '50px', margin: '10px'}}
                    type="primary"
                    size="middle"
                    onClick={ openUpload }
                >
                    <CloudUploadOutlined />
                </a-button>
                <FileTree
                    treeList={ [data.fileTree] }
                    requestHeader={ data.requestHeader }
                />
                <a-modal
                    style={{ padding: '25px' }}
                    v-model:visible={ data.uploadVisible }
                    title={ "Upload" }
                    onOk={ uploadHandle }

                >
                    <div style={{ padding: '20px' }}>
                        <a-form
                            model={data.formData}
                            name="basic"
                        >
                            <a-form-item
                                label={ "file path" }
                                name="file path"
                            >
                                <a-input v-model:value={ data.formData.filepath } placeholder={ "for example:  /aaa/bbb" }/>
                            </a-form-item>
                        </a-form>

                        <a-upload-dragger
                            vfileList={ fileList }
                            name="file"
                            multiple={ true }
                            action="http://localhost:9080/api/storage/upload"
                            before-upload={ beforeUpload }
                        >
                            <p class="ant-upload-drag-icon">
                                <inbox-outlined></inbox-outlined>
                            </p>
                            <p class="ant-upload-text">Click or drag file to this area to upload</p>
                            <p class="ant-upload-hint">
                                Support for a single or bulk upload. Strictly prohibit from uploading company data or
                                other
                                band files
                            </p>
                        </a-upload-dragger>
                    </div>
                </a-modal>
            </>

        )
    }
})
