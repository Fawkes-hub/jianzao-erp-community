<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item, item.children) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <!-- t-menu-item 自带 to/routerLink 走 vue-router.push，包 app-link 会形成 a>li>a 嵌套导致整页跳转、TagsView 收不到路由变化 -->
      <!-- 外链必须关掉 routerLink：否则组件内部会把 https:// 当路由 push，且 <a> 的 preventDefault 会拦掉跳转 -->
      <t-menu-item
        v-if="onlyOneChild.meta"
        :value="menuValue"
        :class="{ 'submenu-title-noDropdown': !isNest }"
        :to="menuLink.isExternal ? undefined : menuLink.target"
        :href="menuLink.isExternal ? (menuLink.target as string) : undefined"
        :target="menuLink.isExternal ? '_blank' : undefined"
        :router-link="!menuLink.isExternal"
      >
        <template #icon>
          <svg-icon :icon-class="onlyOneChild.meta.icon || (item.meta && item.meta.icon)" />
        </template>
        <span class="menu-title" :title="hasTitle(onlyOneChild.meta.title)">{{ onlyOneChild.meta.title }}</span>
      </t-menu-item>
    </template>

    <t-submenu v-else ref="subMenu" :value="resolvePath(item.path)">
      <template v-if="item.meta" #icon>
        <svg-icon :icon-class="item.meta ? item.meta.icon : ''" />
      </template>
      <template v-if="item.meta" #title>
        <span class="menu-title" :title="hasTitle(item.meta?.title)">{{ item.meta?.title }}</span>
      </template>

      <sidebar-item
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </t-submenu>
  </div>
</template>

<script setup lang="ts">
import { isExternal } from '@/utils/validate';
import { getNormalPath } from '@/utils/ruoyi';
import { RouteRecordRaw } from 'vue-router';

const props = defineProps({
  item: {
    type: Object as PropType<RouteRecordRaw>,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
});

const onlyOneChild = ref<any>({});

const hasOneShowingChild = (parent: RouteRecordRaw, children?: RouteRecordRaw[]) => {
  if (!children) {
    children = [];
  }
  const showingChildren = children.filter((item) => {
    if (item.hidden) {
      return false;
    }
    onlyOneChild.value = item;
    return true;
  });

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true;
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true };
    return true;
  }

  return false;
};

const resolvePath = (routePath: string, routeQuery?: string): any => {
  if (isExternal(routePath)) {
    return routePath;
  }
  if (isExternal(props.basePath as string)) {
    return props.basePath;
  }
  if (routeQuery) {
    const query = JSON.parse(routeQuery);
    return { path: getNormalPath(props.basePath + '/' + routePath), query: query };
  }
  return getNormalPath(props.basePath + '/' + routePath);
};

/** 菜单高亮值：始终用内部路径字符串，不带 query */
const menuValue = computed(() => resolvePath(onlyOneChild.value.path) as string);

/**
 * 菜单跳转目标：外链返回原始 URL（routerLink 关闭，走 href 新窗口）；
 * 内部路由返回 path 或 { path, query }，交给 t-menu-item 的 to 走 vue-router.push。
 */
const menuLink = computed(() => {
  const target = resolvePath(onlyOneChild.value.path, onlyOneChild.value.query);
  return { target, isExternal: typeof target === 'string' && isExternal(target) };
});

const hasTitle = (title: string | undefined): string => {
  if (!title || title.length <= 5) {
    return '';
  }
  return title;
};
</script>
