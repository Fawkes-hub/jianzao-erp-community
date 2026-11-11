<template>
  <div class="layout-search-dialog">
    <t-dialog
      v-model:visible="state.isShowSearch"
      destroy-on-close
      dialog-class-name="layout-search-panel"
      :header="false"
      :footer="false"
      :close-btn="false"
      width="600px"
      top="12vh"
      attach="body"
    >
      <div class="layout-search-dialog__content">
        <t-auto-complete
          v-model="state.menuQuery"
          class="layout-search-dialog__autocomplete"
          :options="state.menuList"
          :filter="menuFilter"
          :highlight-keyword="false"
          placeholder="搜索"
          autofocus
          @select="onHandleSelect"
        >
          <template #prefixIcon>
            <svg-icon class-name="search-icon" icon-class="search" />
          </template>
          <template #option="{ option }">
            <div>
              <svg-icon :icon-class="option.icon" class="mr5" />
              {{ option.title }}
            </div>
          </template>
        </t-auto-complete>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts" name="layoutBreadcrumbSearch">
import { getNormalPath } from '@/utils/ruoyi';
import { isHttp } from '@/utils/validate';
import { usePermissionStore } from '@/store/modules/permission';
import { RouteRecordRaw } from 'vue-router';
type Router = Array<{
  text: string;
  path: string;
  icon: string;
  title: string[];
}>;
type SearchState<T = any> = {
  isShowSearch: boolean;
  menuQuery: string;
  menuList: T[];
};
// 定义变量内容
const router = useRouter();
const routes = computed(() => usePermissionStore().routes);
const state = reactive<SearchState>({
  isShowSearch: false,
  menuQuery: '',
  menuList: []
});

// 搜索弹窗打开
const openSearch = () => {
  state.menuQuery = '';
  state.isShowSearch = true;
  state.menuList = generateRoutes(routes.value as any);
};
// 搜索弹窗关闭
const closeSearch = () => {
  state.isShowSearch = false;
};
// 菜单搜索数据过滤（TDesign auto-complete 的 filter：按标题匹配 text）
const menuFilter = (filterWords: string, option: any) => {
  return option.title.indexOf(filterWords) > -1;
};

// Filter out the routes that can be displayed in the sidebar
// And generate the internationalized title
const generateRoutes = (routes: RouteRecordRaw[], basePath = '', prefixTitle: string[] = []) => {
  let res: Router = [];
  routes.forEach((r) => {
    // skip hidden router
    if (!r.hidden) {
      const p = r.path.length > 0 && r.path[0] === '/' ? r.path : '/' + r.path;
      const data: any = {
        // TDesign auto-complete 的 select 回传 option.text，用 path 作为回传值供跳转
        text: !isHttp(r.path) ? getNormalPath(basePath + p) : r.path,
        path: !isHttp(r.path) ? getNormalPath(basePath + p) : r.path,
        icon: r.meta?.icon,
        title: [...prefixTitle]
      };
      if (r.meta && r.meta.title) {
        data.title = [...data.title, r.meta.title];
        if (r.redirect !== 'noRedirect') {
          // only push the routes with title
          // special case: need to exclude parent router without redirect
          res.push(data);
        }
      }
      // recursive child routes
      if (r.children) {
        const tempRoutes = generateRoutes(r.children, data.path, data.title);
        if (tempRoutes.length >= 1) {
          res = [...res, ...tempRoutes];
        }
      }
    }
  });
  res.forEach((item: any) => {
    if (item.title instanceof Array) {
      item.title = item.title.join('/');
    }
  });
  return res;
};
// 当前菜单选中时（TDesign auto-complete 的 select 入参为 option.text，即上面的 path）
const onHandleSelect = (val: string) => {
  const paths = val;
  if (isHttp(paths)) {
    // http(s):// 路径新窗口打开
    const pindex = paths.indexOf('http');
    window.open(paths.substring(pindex, paths.length), '_blank');
  } else {
    router.push(paths);
  }
  state.menuQuery = '';
  closeSearch();
};

// 暴露变量
defineExpose({
  openSearch
});
</script>

<style lang="scss" scoped>
/* attach="body" 的弹层渲染在 body 下，scoped 前缀命中不到，用 :global 精确限定本弹窗类名 */
:global(.layout-search-panel.t-dialog) {
  border-radius: var(--app-radius-base);
  overflow: visible;

  :global(.t-dialog__body) {
    padding: 20px;
  }
}

.layout-search-dialog {
  &__content {
    width: 100%;
  }

  &__autocomplete {
    width: 100%;

    :deep(.t-input) {
      min-height: 44px;
      border-radius: var(--app-radius-md);
    }
  }
}
</style>
