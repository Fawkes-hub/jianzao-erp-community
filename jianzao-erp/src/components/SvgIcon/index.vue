<template>
  <!-- TDesign 图标优先；旧菜单数据里的 RuoYi 图标名走下方雪碧图兜底 -->
  <t-icon v-if="tdIconName" :name="tdIconName" :class="svgClass" :style="iconStyle" aria-hidden="true" />
  <svg v-else :class="svgClass" aria-hidden="true">
    <use :xlink:href="iconName" :fill="color" />
  </svg>
</template>

<script setup lang="ts">
import { manifest } from 'tdesign-icons-vue-next';
import { propTypes } from '@/utils/propTypes';

/**
 * RuoYi 历史菜单图标 → TDesign 图标名。
 * 存量 sys_menu.icon 不改库也能直接显示为 TDesign 图标；新选图标直接存 TDesign 名。
 */
const LEGACY_ICON_MAP: Record<string, string> = {
  system: 'setting',
  monitor: 'server',
  tool: 'tools',
  guide: 'help-circle',
  peoples: 'usergroup',
  post: 'usergroup',
  dict: 'book-open',
  tree: 'tree-round-dot',
  'tree-table': 'tree-square-dot',
  job: 'time',
  list: 'task',
  log: 'history',
  logininfor: 'login',
  message: 'notification',
  online: 'user',
  redis: 'data-base',
  international: 'translate',
  // 顶栏控件图标：避免非 TDesign 名回落到旧实心雪碧图，与其余线性图标混排
  company: 'building',
  'exit-fullscreen': 'fullscreen-exit'
};

const tdIconNames = new Set(manifest.map((item) => item.stem));

const props = defineProps({
  iconClass: propTypes.string.isRequired,
  className: propTypes.string.def(''),
  color: propTypes.string.def('')
});
const iconName = computed(() => `#icon-${props.iconClass}`);
const tdIconName = computed(() => {
  const name = LEGACY_ICON_MAP[props.iconClass] || props.iconClass;
  return tdIconNames.has(name) ? name : '';
});
const iconStyle = computed(() => (props.color ? { color: props.color } : undefined));
const svgClass = computed(() => {
  if (props.className) {
    return `svg-icon ${props.className}`;
  }
  return 'svg-icon';
});
</script>

<style lang="scss" scoped>
.sub-el-icon,
.nav-icon {
  display: inline-block;
  font-size: 15px;
  margin-right: 12px;
  position: relative;
}

.svg-icon {
  width: 1em;
  height: 1em;
  position: relative;
  fill: currentColor;
  vertical-align: -2px;
}
</style>
