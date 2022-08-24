import { createStore } from 'vuex'
import app from './modules/app'
import getters from './getters'
/**
 * <p>VUEX 公用变量配置</p>
 * @author fxbsujay@gmail.com
 * @version 13:24 2022/6/3
 */
const store = createStore({
    modules: {
        app
    },
    getters
})

export default store;