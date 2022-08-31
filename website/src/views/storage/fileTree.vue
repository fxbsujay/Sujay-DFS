<template>
  <div class="file-tree">
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
  </div>

</template>

<script lang="ts">
import { FileTreeModel } from '@/model/Models'
import { TreeProps } from 'ant-design-vue'
import { DeleteOutlined, EyeOutlined } from '@ant-design/icons-vue'
export default {
  name: "FileTree",
  components: {
    DeleteOutlined,
    EyeOutlined
  },
  props: {
    treeList: {
      type: Array<FileTreeModel>(),
      default: []
    },
    deleteHandle: {
      type: Function,
      default: () => {}
    }
  },

  setup() {

    const fieldNames: TreeProps['fieldNames'] = {
      children: 'children',
      title: 'path',
      key: 'path'
    }

    const viewHandle = (file: FileTreeModel) => {
      console.log(file)
    }

    return {
      fieldNames,
      viewHandle
    }

  }
}
</script>
