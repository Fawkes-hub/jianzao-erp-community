<script setup lang="ts">
/**
 * 往来单位选择器（客户 / 供应商）
 *
 * DOC/35 T2：由 el-select 改为 TDesign t-select，与页面其余筛选控件同尺寸、同圆角、同边框色，
 * 避免「供应商框比旁边输入框宽一圈、样式也不一样」。对外的 v-model / change 契约保持不变。
 */
import { computed, h, ref, watch } from 'vue';
import { customerOptions, supplierOptions } from '@/api/erp/master';
import type { ErpCustomerVO, ErpSupplierVO } from '@/api/erp/types';

type PartyKind = 'customer' | 'supplier';

const props = withDefaults(
  defineProps<{
    modelValue?: string | number | null;
    /** CUSTOMER=客户（往来单位），SUPPLIER=供应商 */
    kind?: PartyKind;
    placeholder?: string;
    disabled?: boolean;
    clearable?: boolean;
  }>(),
  {
    modelValue: undefined,
    kind: 'customer',
    placeholder: '请选择',
    disabled: false,
    clearable: true
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | undefined): void;
  (e: 'change', party: ErpCustomerVO | ErpSupplierVO | undefined): void;
}>();

const loading = ref(false);
const options = ref<(ErpCustomerVO & ErpSupplierVO)[]>([]);

/** 远程查询：keyword 为空时取默认列表 */
const load = async (keyword?: string) => {
  loading.value = true;
  try {
    const res = props.kind === 'customer' ? await customerOptions(keyword) : await supplierOptions(keyword);
    options.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
};

let searchTimer: ReturnType<typeof setTimeout> | undefined;
/** 输入停顿 250ms 再发请求，避免逐字触发远程查询 */
const search = (keyword: string) => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => load(keyword), 250);
};

/** t-select 需要 { label, value }；行内保留「编码 名称 | 电话」两栏，slots.default 覆盖行渲染 */
const selectOptions = computed(() =>
  options.value.map((item) => ({
    value: item.id,
    label: `${item.code} ${item.name}`,
    slots: {
      default: () =>
        h('div', { class: 'flex items-center justify-between gap-4' }, [
          h('span', { class: 'truncate' }, `${item.code} ${item.name}`),
          item.phone ? h('span', { class: 'shrink-0 text-xs erp-text-secondary' }, item.phone) : null
        ])
    }
  }))
);

/** 回显文案：窄控件会截断「编码 名称」，用原生 title 给完整文案 */
const displayLabel = computed(() => selectOptions.value.find((item) => String(item.value) === String(props.modelValue))?.label ?? '');

const handleChange = (value: any) => {
  const found = options.value.find((item) => String(item.id) === String(value));
  emit('update:modelValue', value);
  emit('change', found);
};

watch(
  () => props.kind,
  () => {
    options.value = [];
    load();
  },
  { immediate: true }
);
</script>

<template>
  <!-- 触发器宽度必须跟容器走：auto-width 只按内容量宽度，抽屉里比旁边的日期/输入框短一截（实测 422 列的容器里只有 126px） -->
  <t-select
    :model-value="modelValue"
    :title="displayLabel"
    :placeholder="placeholder"
    :disabled="disabled"
    :clearable="clearable"
    :loading="loading"
    :options="selectOptions"
    filterable
    @change="handleChange"
    @search="search"
    @popup-visible-change="(visible: boolean) => visible && load()"
  />
</template>
