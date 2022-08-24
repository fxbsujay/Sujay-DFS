import {defineComponent, ref, watch} from 'vue'
import {RouteLocationMatched, useRoute} from 'vue-router'

/**
 * @description 视图组件
 * @author fxbsujay@gmail.com
 * @version 22:06 2022/6/5
 */
export default defineComponent({
    name: 'LayoutContent',
    render() {
        const route = useRoute()
        const breadcrumbList = ref<RouteLocationMatched[]>([])

        /**
         * 监听路由的变化
         */
        watch(route, () => {
            breadcrumbList.value = route.matched
        },{ deep: true, immediate: true})

        /**
         * 获取面包屑
         * @param route {RouteLocationMatched}
         */
        const getBreadcrumbItems = (route: RouteLocationMatched[]): any => {
            return route
                .filter( item => {
                    return item.name != undefined && item.name != ''
                })
                .map( item => {
                    if (item.name) {
                        return (
                            <a-breadcrumb-item>{ item.meta.title }</a-breadcrumb-item>
                        )
                    }
                return
                })
        }

        return  (
            <a-layout-content>
                <a-breadcrumb style={"margin: 16px 0"}>
                { getBreadcrumbItems(breadcrumbList.value)}

                </a-breadcrumb>
                <div class="app-main">
                    <router-view/>
                </div>
            </a-layout-content>
        )
    }
})