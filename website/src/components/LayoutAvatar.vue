<template>
    <a-dropdown>
      <a class="ant-dropdown-link">
        <svg class="icon svg-icon" aria-hidden="true">
          <use xlink:href="#icon-wogerenziliao"></use>
        </svg>
      </a>
      <template #overlay>
        <a-menu>
          <a-menu-item  @click="logout" key="1">
            退出
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
</template>

<script lang="ts">
import { defineComponent, reactive, Ref, toRefs } from 'vue'
import { UserOutlined,DribbbleOutlined } from '@ant-design/icons-vue'
import { useStore } from 'vuex'
import { UserActionTypes } from '@/store/modules/app'
import '@/assets/fonts/avatar'
/**
 * @description 头像组件
 * @author fxbsujay@gmail.com
 * @version 21:39 2022/6/10
 */
export default defineComponent({
  name: "LayoutAvatar",
  components: {
    UserOutlined,
    DribbbleOutlined
  },
  setup() {
    const store = useStore()
    const methods = reactive({
      logout: async () => {
        await store.dispatch(UserActionTypes.LOGOUT, { }).catch( res => {})
      }
    })
    return {
      ...toRefs(methods)
    }
  }
})

</script>