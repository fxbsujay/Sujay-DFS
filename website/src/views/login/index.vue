<template>
  <div class="login-container">
    <div class="sea">
      <div class="wave"></div>
      <div class="wave"></div>
    </div>
    <div class="container">
      <div class="left">
        <h1>SUSU</h1>
        <p class="left-top">SUSU - DFS</p>
        <p>欢迎光临, 请输入您的账号和密码 !</p>
      </div>
      <div class="right">
        <div class="form">
          <label></label>
          <a-input v-model:value="state.username" placeholder="账号">
            <template #suffix>
              <a-tooltip>
                <user-outlined type="user" />
              </a-tooltip>
            </template>
          </a-input>
          <label></label>
          <a-input-password v-model:value="state.password" placeholder="密码" />
          <a-button type="primary" @click="login" :loading="loading">{{ $t('login.title')}}</a-button>
        </div>
      </div>
    </div>
  </div>

</template>

<script lang="ts">
import { UserOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { defineComponent, ref,toRefs,toRef,reactive } from 'vue'

import './index.less'
import { LoginModel } from '@/model/UserModel'
import { useStore } from 'vuex'
import {UserActionTypes} from '@/store/modules/app'

export default defineComponent({
  name: 'Login',
  components: {
    UserOutlined,
    InfoCircleOutlined
  },

  setup: function () {

    const store = useStore()

    const loading = ref<boolean>(false)
    const state = ref<LoginModel>({
      username: '',
      password: ''
    })

    const methods = reactive({
      login: async () => {
        loading.value = true
        await store.dispatch(UserActionTypes.LOGIN, { username: state.value.username, password: state.value.password }).catch( res => {
          loading.value = false
        })

      }
    })

    return {
      loading,
      state,
      ...toRefs(methods)
    }
  }
})
//  .ant-input-affix-wrapper > input.ant-input
</script>

<style lang="less">


</style>