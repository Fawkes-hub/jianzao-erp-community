<script setup lang="ts">
/**
 * 单行省略 + 悬浮显示完整文案
 *
 * 专用于 el-select / t-select 的 #label 插槽：选中标签超长时组件只做省略号截断，
 * 不会给出任何悬浮提示，长单号/长商品名无法看全。此组件在截断处挂 tooltip 补上。
 *
 * 用法（置于选择器的 #label 插槽内）：
 * <template #label="{ label }"><EllipsisTip :text="label" /></template>
 */
withDefaults(
  defineProps<{
    /** 完整文案；为空时不展示 tooltip */
    text?: string | number | null;
    /** tooltip 弹出方向 */
    placement?: 'top' | 'bottom' | 'left' | 'right';
  }>(),
  {
    text: '',
    placement: 'top'
  }
);
</script>

<template>
  <t-tooltip :content="String(text ?? '')" :placement="placement" :disabled="!text" :delay="200">
    <span class="block truncate">{{ text }}</span>
  </t-tooltip>
</template>
