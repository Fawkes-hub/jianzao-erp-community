<script setup lang="ts">
import { HelpCircleIcon } from 'tdesign-icons-vue-next';

/**
 * 字段释义提示：列名/表单项旁的问号图标，鼠标移入显示完整说明
 *
 * 用于「交期 / 承诺交期」等易混字段的语义说明，供订单列表与抽屉明细表头复用。
 * 仅做展示，不承载业务逻辑。
 *
 * 用法：<FieldTip content="说明文案" placement="top" />
 */
withDefaults(
  defineProps<{
    /** 提示完整文案 */
    content: string;
    /** tooltip 弹出方向 */
    placement?: 'top' | 'bottom' | 'left' | 'right';
  }>(),
  { placement: 'top' }
);
</script>

<template>
  <t-tooltip :content="content" :placement="placement" :delay="200">
    <HelpCircleIcon class="cursor-help erp-text-secondary align-middle ml-1" />
  </t-tooltip>
</template>
