<template>
  <div
    :class="{ 'has-logo': showLogo }"
    :style="{
      backgroundColor: bgColor,
      '--sidebar-menu-text': textColor,
      '--sidebar-menu-active-bg': darkMenu ? theme : 'rgba(0, 82, 217, 0.1)',
      '--sidebar-menu-active-text': darkMenu ? '#ffffff' : '#0052d9',
      '--sidebar-menu-hover': darkMenu ? 'var(--menuHover)' : 'rgba(0, 82, 217, 0.06)'
    }"
  >
    <logo v-if="showLogo" :collapse="isCollapse" :dark="darkMenu" />
    <!-- TDesign 无 scrollbar 组件，用原生 div + CSS overflow 承接侧栏滚动 -->
    <div class="sidebar-scroll scrollbar-wrapper" :class="sideTheme">
      <transition :enter-active-class="proxy?.animate.menuSearchAnimate.enter" mode="out-in">
        <t-menu :value="activeMenu" :collapsed="isCollapse" :expand-mutex="true" :theme="darkMenu ? 'dark' : 'light'" :width="['200px', '54px']">
          <sidebar-item v-for="(r, index) in sidebarRouters" :key="r.path + index" :item="r" :base-path="r.path" />
        </t-menu>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import Logo from './Logo.vue';
import SidebarItem from './SidebarItem.vue';
import variables from '@/assets/styles/variables.module.scss';
import { useAppStore } from '@/store/modules/app';
import { useSettingsStore } from '@/store/modules/settings';
import { usePermissionStore } from '@/store/modules/permission';
import { RouteRecordRaw } from 'vue-router';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const route = useRoute();
const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const sidebarRouters = computed<RouteRecordRaw[]>(() => permissionStore.getSidebarRoutes());
const showLogo = computed(() => settingsStore.sidebarLogo);
const sideTheme = computed(() => settingsStore.sideTheme);
// 全局暗色由 <html class="dark"> 或 theme-mode="dark" 驱动，监听变化跟随
const checkDark = () => document.documentElement.classList.contains('dark') || document.documentElement.getAttribute('theme-mode') === 'dark';
const isDarkHtml = ref(checkDark());
const darkObserver = new MutationObserver(() => {
  isDarkHtml.value = checkDark();
});
// 全局暗色时侧栏必须跟着暗，否则暗色应用里会留一条白栏
const darkMenu = computed(() => sideTheme.value === 'theme-dark' || isDarkHtml.value);
const theme = computed(() => settingsStore.theme);
const isCollapse = computed(() => !appStore.sidebar.opened);

const activeMenu = computed(() => {
  const { meta, path } = route;
  // if set path, the sidebar will highlight the path you set
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

onMounted(() => darkObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'theme-mode'] }));
onBeforeUnmount(() => darkObserver.disconnect());

const bgColor = computed(() => (darkMenu.value ? variables.menuBackground : variables.menuLightBackground));
const textColor = computed(() => (darkMenu.value ? variables.menuColor : variables.menuLightColor));
</script>

<style lang="scss" scoped>
/* 深色侧栏与 TDesign 内置 t-menu 配色不一致，仅在 scoped 内用 :deep 微调，不改全局 .scss */
.sidebar-scroll {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;

  .has-logo & {
    height: calc(100% - 50px);
  }

  :deep(.t-default-menu) {
    width: 100% !important;
    height: auto;
    min-height: 100%;
    background-color: transparent;
    border: none;
  }

  :deep(.t-default-menu .t-menu__item) {
    color: var(--sidebar-menu-text) !important;
    border-radius: var(--app-radius-md);
    height: 45px;
    margin: 1px 5px;
  }

  :deep(.t-default-menu .t-menu__item:hover:not(.t-is-active):not(.t-is-disabled)) {
    background-color: var(--sidebar-menu-hover) !important;
  }

  :deep(.t-default-menu .t-menu__item.t-is-active:not(.t-is-opened)) {
    background-color: var(--sidebar-menu-active-bg) !important;
    color: var(--sidebar-menu-active-text) !important;
    font-weight: 500;
  }

  :deep(.t-default-menu .svg-icon) {
    margin-right: 10px;
  }

  /* 侧边栏折叠时消除 svg-icon 的 margin-right 和 item padding，保证图标在胶囊内严格居中 */
  :deep(.t-default-menu.t-is-collapsed .t-menu__item) {
    padding: 0 !important;
    display: flex !important;
    justify-content: center !important;
    align-items: center !important;
  }

  :deep(.t-default-menu.t-is-collapsed .svg-icon) {
    margin: 0 !important;
  }

  :deep(.t-default-menu .menu-title) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
