<template>
  <div class="top-right-btn" :style="style">
    <div class="flex items-center">
      <t-tooltip v-if="search" class="item" :content="showSearch ? '隐藏搜索' : '显示搜索'" placement="top">
        <t-button shape="square" variant="text" @click="toggleSearch()">
          <template #icon><SearchIcon /></template>
        </t-button>
      </t-tooltip>
      <t-tooltip class="item" content="刷新" placement="top">
        <t-button shape="square" variant="text" @click="refresh()">
          <template #icon><RefreshIcon /></template>
        </t-button>
      </t-tooltip>
      <t-tooltip v-if="columns" class="item" content="显示/隐藏列" placement="top">
        <div class="show-btn">
          <t-popup placement="bottom" trigger="click">
            <t-button shape="square" variant="text">
              <template #icon><MenuIcon /></template>
            </t-button>
            <template #content>
              <div class="tree-header">显示/隐藏列</div>
              <t-tree
                checkable
                :data="columns"
                :keys="{ value: 'key', label: 'label', children: 'children' }"
                :value="checkedKeys"
                @change="columnChange"
              />
            </template>
          </t-popup>
        </div>
      </t-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MenuIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  showSearch: propTypes.bool.def(true),
  columns: propTypes.fieldOption,
  search: propTypes.bool.def(true),
  gutter: propTypes.number.def(10)
});

const emits = defineEmits(['update:showSearch', 'queryTable']);

const style = computed(() => {
  const ret: any = {};
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`;
  }
  return ret;
});

// 勾选态由 columns.visible 受控推导，替代原 EP Tree 实例 setChecked
const checkedKeys = computed<Array<string | number>>(() => props.columns?.filter((item) => item.visible).map((item) => item.key) ?? []);

// 搜索
function toggleSearch() {
  emits('update:showSearch', !props.showSearch);
}

// 刷新
function refresh() {
  emits('queryTable');
}

// 更改数据列的显示和隐藏；TDesign Tree 的第一参数即勾选值数组
function columnChange(value: Array<string | number>) {
  props.columns?.forEach((item) => {
    item.visible = value.includes(item.key);
  });
}
</script>

<style lang="scss" scoped>
.tree-header {
  width: 100%;
  line-height: 24px;
  text-align: center;
}
.show-btn {
  margin-left: 12px;
}
</style>
