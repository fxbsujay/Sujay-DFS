import { defineComponent } from 'vue'
import { AsyncRoutes } from '@/router'
import { RouteRecordRaw } from 'vue-router'

/**
 * @description 菜单组件
 * @author fxbsujay@gmail.com
 * @version 17:50 2022/6/5
 */
export default defineComponent({
    name: 'LayoutMenu',
    methods: {
        /**
         * 单菜单
         * @param routes 路由表
         */
        getNavMenuItems(routes: Array<RouteRecordRaw> = []) {
            return routes.map( (item,index) => {
                if (item.children && item.children.length > 1) {
                    return this.getSubMenuOrItem(item)
                }
                return  (
                    <a-menu-item key={item.name}>
                        <router-link class="nav-link"  to={item.path}>{item.meta ? item.meta.title : item.name}</router-link>
                    </a-menu-item>
                )

            })
        },
        /**
         * 多级菜单
         * @param route 路由
         */
        getSubMenuOrItem( route: RouteRecordRaw ) {
            if (!route.children) {
                return
            }
            return (
                <a-sub-menu title={route.meta.title} key={route.name}>
                    { this.getNavMenuItems(route.children)}
                </a-sub-menu>
            )
        }
    },
    render() {
        return (
            <> { this.getNavMenuItems(AsyncRoutes)} </>
        )
    }
})