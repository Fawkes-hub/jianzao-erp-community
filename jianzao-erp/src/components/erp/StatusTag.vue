<script setup lang="ts">
import { computed } from 'vue';
import { ERP_BILL_STATUS, ERP_AR_AP_STATUS, ERP_ORDER_EXEC_STATUS, ERP_PRODUCT_TYPE, ERP_TAX_TYPE } from '@/api/erp/enums';
import type { ErpArApStatus, ErpBillStatus, ErpProductType, ErpTaxType } from '@/api/erp/enums';

type TagKind = 'bill' | 'arAp' | 'productType' | 'tax' | 'exec';
type TagTheme = 'default' | 'primary' | 'warning' | 'danger' | 'success';

/**
 * 前端映射表的 type（数据，来自 enums）→ t-tag 的 theme
 *
 * enums 里的 type 沿用 Element Plus 的四色命名，TDesign 无 info 主题，统一落到 default（灰色）；
 * 未登记的状态码同样兜底 default，避免出现 TDesign 不认识的 theme。
 */
const THEME: Record<string, TagTheme> = {
  primary: 'primary',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  info: 'default'
};

const props = defineProps<{
  kind: TagKind;
  value?: string | number | null;
  /** 覆盖 DRAFT 的显示文案；不传时沿用 ERP_BILL_STATUS 的「草稿」 */
  draftLabel?: string;
  /** 后端回填的状态中文文案（VO 的 statusText）；传了优先于前端映射表 */
  statusText?: string | null;
}>();

const maps: Record<TagKind, Record<string, { label: string; type: string }>> = {
  bill: {
    ...ERP_BILL_STATUS,
    DISPATCHED: { label: '已发料', type: 'warning' },
    COMPLETED: { label: '已完成', type: 'success' }
  },
  arAp: ERP_AR_AP_STATUS,
  productType: ERP_PRODUCT_TYPE,
  exec: ERP_ORDER_EXEC_STATUS,
  tax: {
    NO_TAX: { label: ERP_TAX_TYPE.NO_TAX.label, type: 'info' },
    VAT_INVOICE: { label: ERP_TAX_TYPE.VAT_INVOICE.label, type: 'warning' },
    GENERAL_INVOICE: { label: ERP_TAX_TYPE.GENERAL_INVOICE.label, type: 'success' }
  }
};

const meta = computed(() => {
  const key = props.value == null ? '' : String(props.value);
  const found = maps[props.kind][key];
  // 标签颜色仍由前端映射表决定；未登记的状态码统一走 default（t-tag 无 info 主题）
  const type = found === undefined ? 'default' : found.type;
  // 后端已回填状态中文文案时优先采用（VO 的 statusText）
  const backendText = props.statusText;
  const hasBackendText = backendText !== undefined && backendText !== null && backendText !== '';
  if (found === undefined) {
    return { label: hasBackendText ? backendText : key || '-', type };
  }
  // DRAFT 在采购/销售单里业务上叫「待入库/待出库」，由调用方传入；优先级高于后端文案
  if (key === 'DRAFT' && props.draftLabel !== undefined && props.draftLabel !== '') {
    return { label: props.draftLabel, type };
  }
  if (hasBackendText) {
    return { label: backendText, type };
  }
  return found;
});
</script>

<template>
  <t-tag :theme="THEME[meta.type] ?? 'default'" size="small">{{ meta.label }}</t-tag>
</template>
