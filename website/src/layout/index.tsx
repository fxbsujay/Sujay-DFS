import { ref, defineComponent } from 'vue'
import LayoutMenu from '../components/LayoutMenu'
import LayoutContent from '../components/LayoutContent'
import LayoutAvatar from '../components/LayoutAvatar.vue'
import LayoutI18n from '@/components/LayoutI18n.vue'
import 'ant-design-vue/es/breadcrumb/style/css'
import { useRoute } from 'vue-router'
import { SysConstant } from "@/constant/key"
export default defineComponent({
  name: "Layout",
  components: {
    LayoutMenu,
    LayoutContent,
    LayoutAvatar,
    LayoutI18n
  },
  setup() {
    const selectedKeyList = ref<string[]>([useRoute().name as string]);
    return () => (
      <a-layout class="layout">
        <a-layout-header>
          <div class="layout-logo">
            <h1>{ SysConstant.projectName }</h1>
          </div>
          <div class="layout-menu">
            <a-menu
                v-model:selectedKeys={ selectedKeyList.value }
                theme="light"
                mode="horizontal"
            >
              <layout-menu ></layout-menu>
            </a-menu>
          </div>
          <div class="layout-icon">
            <layout-i18n></layout-i18n>
          </div>
          <div class="layout-icon">
            <layout-avatar></layout-avatar>
          </div>
        </a-layout-header>
        <layout-content></layout-content>
        <a-layout-footer >
          Ant Design ©2018 Created by Ant UED
        </a-layout-footer>
      </a-layout>
    )
  }
})
