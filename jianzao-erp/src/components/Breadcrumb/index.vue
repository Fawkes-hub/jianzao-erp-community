<template>
  <t-breadcrumb class="app-breadcrumb" separator="/" :options="breadcrumbOptions" />
</template>

<script setup lang="ts">
import { RouteLocationMatched } from 'vue-router';
import { usePermissionStore } from '@/store/modules/permission';

const route = useRoute();
const permissionStore = usePermissionStore();
const levelList = ref<RouteLocationMatched[]>([]);

// t-breadcrumb 用 options 渲染：末项与 noRedirect 项置灰不可点，其余按 redirect/path 跳转
const breadcrumbOptions = computed<any[]>(() =>
  levelList.value.map((item, index) => {
    const disabled = item.redirect === 'noRedirect' || index === levelList.value.length - 1;
    return {
      content: item.meta?.title,
      disabled,
      to: disabled ? undefined : item.redirect || item.path
    };
  })
);

const getBreadcrumb = () => {
  // only show routes with meta.title
  let matched = [];
  const pathNum = findPathNum(route.path);
  // multi-level menu
  if (pathNum > 2) {
    const reg = /\/\w+/gi;
    const pathList = route.path.match(reg).map((item, index) => {
      if (index !== 0) item = item.slice(1);
      return item;
    });
    getMatched(pathList, permissionStore.defaultRoutes, matched);
  } else {
    matched = route.matched.filter((item) => item.meta && item.meta.title);
  }
  // 判断是否为首页
  if (!isDashboard(matched[0])) {
    matched = [{ path: '/', meta: { title: '首页' } }].concat(matched);
  }
  levelList.value = matched.filter((item) => item.meta && item.meta.title && item.meta.breadcrumb !== false);
};
const findPathNum = (str, char = '/') => {
  if (typeof str !== 'string' || str.length === 0) return 0;
  return str.split(char).length - 1;
};
const getMatched = (pathList, routeList, matched) => {
  const data = routeList.find((item) => item.path == pathList[0] || (item.name += '').toLowerCase() == pathList[0]);
  if (data) {
    matched.push(data);
    if (data.children && pathList.length) {
      pathList.shift();
      getMatched(pathList, data.children, matched);
    }
  }
};
const isDashboard = (route: RouteLocationMatched) => {
  const name = route && (route.name as string);
  if (!name) {
    return false;
  }
  // 工作台动态路由 name 形如 "dashboard3010"（后端 routeName + menuId）
  return name.trim() === 'Index' || /^dashboard/i.test(name.trim());
};

watchEffect(() => {
  // if you go to the redirect page, do not update the breadcrumbs
  if (route.path.startsWith('/redirect/')) return;
  getBreadcrumb();
});
onMounted(() => {
  getBreadcrumb();
});
</script>

<style lang="scss" scoped>
.app-breadcrumb {
  display: flex;
  align-items: center;
  height: 100%;
  font-size: 14px;
  margin-left: 0;

  :deep(.t-breadcrumb__item) {
    display: inline-flex;
    align-items: center;
  }

  // 隐藏末项分隔符，保持与 EP 面包屑一致的收尾
  :deep(.t-breadcrumb__item:last-child .t-breadcrumb__separator) {
    display: none;
  }

  :deep(.t-is-disabled) {
    cursor: text;
  }
}
</style>
