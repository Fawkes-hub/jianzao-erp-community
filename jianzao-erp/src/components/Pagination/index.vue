<template>
  <div :class="{ hidden: hidden }" class="pagination-container">
    <t-pagination
      :total="total"
      :current="currentPage"
      :page-size="pageSize"
      :page-size-options="pageSizes"
      :show-jumper="true"
      @current-change="handleCurrentChange"
      @page-size-change="handleSizeChange"
    />
  </div>
</template>

<script setup name="Pagination" lang="ts">
import { scrollTo } from '@/utils/scroll-to';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  total: propTypes.number,
  page: propTypes.number.def(1),
  limit: propTypes.number.def(20),
  pageSizes: { type: Array<number>, default: () => [10, 20, 30, 50] },
  autoScroll: propTypes.bool.def(true),
  hidden: propTypes.bool.def(false)
});

const emit = defineEmits(['update:page', 'update:limit', 'pagination']);
const currentPage = computed({
  get() {
    return props.page;
  },
  set(val) {
    emit('update:page', val);
  }
});
const pageSize = computed({
  get() {
    return props.limit;
  },
  set(val) {
    emit('update:limit', val);
  }
});
function handleSizeChange(val: number) {
  // TDesign 分页为单向绑定，需显式回写以维持原 v-model:limit 契约
  pageSize.value = val;
  if (currentPage.value * val > props.total) {
    currentPage.value = 1;
  }
  emit('pagination', { page: currentPage.value, limit: val });
  if (props.autoScroll) {
    scrollTo(0, 800);
  }
}
function handleCurrentChange(val: number) {
  // 同上，维持原 v-model:page 契约
  currentPage.value = val;
  emit('pagination', { page: val, limit: pageSize.value });
  if (props.autoScroll) {
    scrollTo(0, 800);
  }
}
</script>

<style lang="scss" scoped>
.pagination-container.hidden {
  display: none;
}
</style>
