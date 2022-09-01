<template>
  <div class="file-tree">
    <a-input-search
        style="width: 300px;margin: 10px 10px"
        placeholder="input search text"
        enter-button
    />
    <a-button style="width:50px;margin: 10px 10px" type="primary" size="middle" @click="data.uploadVisible = true">
      <template #icon>
        <CloudUploadOutlined />
      </template>
    </a-button>

    <a-directory-tree
        :tree-data="treeList"
        :field-names="fieldNames"
    >
      <template #title="item">
        <span>{{ item.path }}</span>
        <a-button v-if="item.type === 1" danger class="tree-node-but" size="small" type="primary">
          <template #icon>
            <DeleteOutlined />
          </template>
        </a-button>
        <a-button v-if="item.type === 1" class="tree-node-but" size="small" type="primary" @click="() => viewHandle(item)">
          <template #icon>
            <EyeOutlined />
          </template>
        </a-button>
      </template>
    </a-directory-tree>

    <a-modal
        style="padding: 25px;width: 350px;height: 350px"
        v-model:visible="data.viewVisible"
        footer=""
    >
      <div style="margin: 20px">
        <a-image
            :width="200"
            :height="200"
            :src="data.path"
            fallback="data:image/png;base64,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"
        />
      </div>
    </a-modal>

    <a-modal
        style="padding: 25px;"
        v-model:visible="data.uploadVisible"
        title="Upload"
        @ok="uploadHandle"
    >
      <div style="margin: 20px">
        <a-form
            :model="data.formData"
            name="basic"
        >
          <a-form-item
              label="file path"
              name="file path"
              :rules="[{ required: true, message: 'Please input your username!' }]"
          >
            <a-input v-model:value="data.formData.filepath" placeholder="for example:  /aaa/bbb"/>
          </a-form-item>
        </a-form>

        <a-upload-dragger
            v-model:fileList="fileList"
            name="file"
            :multiple="true"
            action="http://localhost:9080/api/storage/upload"
            :before-upload="beforeUpload"
        >
          <p class="ant-upload-drag-icon">
            <inbox-outlined></inbox-outlined>
          </p>
          <p class="ant-upload-text">Click or drag file to this area to upload</p>
          <p class="ant-upload-hint">
            Support for a single or bulk upload. Strictly prohibit from uploading company data or other
            band files
          </p>
        </a-upload-dragger>
      </div>
    </a-modal>
  </div>

</template>

<script lang="ts">
import { FileTreeModel } from '@/model/Models'
import { TreeProps } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { DeleteOutlined, EyeOutlined, CloudUploadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { reactive, ref } from 'vue'
import { uploadApi } from '@/api/storage'
export default {
  name: "FileTree",
  components: {
    DeleteOutlined,
    EyeOutlined,
    CloudUploadOutlined,
    SearchOutlined
  },
  props: {
    treeList: {
      type: Array<FileTreeModel>(),
      default: []
    },
    deleteHandle: {
      type: Function,
      default: () => {}
    },
    requestHeader: {
      type: String,
      default: ''
    }
  },

  setup(props: any) {

    const fieldNames: TreeProps['fieldNames'] = {
      children: 'children',
      title: 'path',
      key: 'path'
    }

    const fileList = ref<UploadProps['fileList']>([]);

    const data = reactive({
      viewVisible: false,
      uploadVisible: false,
      path: '',
      formData: {
        filepath: ''
      }
    })

    /**
     *  查看图片请求路径
     */
    const getFilePath = (file: FileTreeModel,index: number = 0, path: string = ''): string => {

      if (file.index === index) {
        return path + '/' + file.path
      }

      if (file.children.length > 0) {
        const filePath = path + '/' + file.path
        file.children.forEach( item => {
          path = getFilePath(item,index,filePath)
        })
      }

      return path
    }

    /**
     *  查看图片
     */
    const viewHandle = (file: FileTreeModel) => {
      data.path = getFilePath(props.treeList[0].children[0],5,props.requestHeader)
      data.viewVisible = true
    }

    /**
     * 上传文件
     */
    const uploadHandle = () => {
      const formData = new FormData();
      formData.append("file",fileList.value[0] as any)
      formData.append("filepath","/aaaaaaaaaaaa")
      uploadApi({ "filepath": "/aaaaaaaa", "file": fileList.value[0]}).then( res => {
        console.log(res)
      })
    }

    /**
     *  手动上传
     */
    const beforeUpload: UploadProps['beforeUpload'] = file => {
      fileList.value = [...fileList.value, file];
      return false;
    };


    return {
      data,
      fieldNames,
      fileList,
      viewHandle,
      beforeUpload,
      uploadHandle
    }

  }
}
</script>
