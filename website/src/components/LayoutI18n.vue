<template>

  <a-dropdown >
    <a class="ant-dropdown-link">
     {{ selectedKeys[0].toLocaleUpperCase() }}
    </a>
    <template #overlay>
      <a-menu v-model:selectedKeys="selectedKeys" @click="setLanguage">
        <a-menu-item key="zh">
          ZH 简体中文
        </a-menu-item>
        <a-menu-item key="en">
          EN English
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script lang="ts">

import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLang } from "@/utils/cookies";
import { MenuProps } from "ant-design-vue";
import '@/assets/fonts/en'
export default {
  name: "LayoutI18n",
  setup() {
    const { locale } = useI18n()
    const selectedKeys = ref<string[]>([locale.value])
    const setLanguage: MenuProps['onClick'] = ({ key: value }) => {
      value = value as  string
      setLang(value)
      locale.value = value
      selectedKeys.value = [value]
    };
    return {
      selectedKeys,
      setLanguage,
    }
  }
}
</script>