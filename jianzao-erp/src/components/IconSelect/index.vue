<template>
  <div class="relative" :style="{ 'width': width }">
    <t-input :model-value="modelValue" readonly placeholder="点击选择图标" @click="visible = true">
      <template #label>
        <svg-icon :icon-class="modelValue" />
      </template>
    </t-input>

    <!-- t-popup 插槽方向与 EP 相反：默认插槽是触发器，#content 才是面板；可见性由 popup 的 click 触发器统一管理 -->
    <t-popup :visible="visible" placement="bottom-right" trigger="click" :show-arrow="false" @visible-change="onVisibleChange">
      <div class="cursor-pointer text-[#999] absolute right-[10px] top-0 height-[32px] leading-[32px]">
        <CaretUpIcon v-show="visible" />
        <CaretDownIcon v-show="!visible" />
      </div>
      <template #content>
        <div class="icon-select-panel">
          <t-input v-model="keyword" class="icon-search" placeholder="搜索 TDesign 图标（英文名）" clearable />
          <div class="icon-select-scroll">
            <ul class="icon-list">
              <t-tooltip v-for="(iconName, index) in iconNames" :key="index" :content="iconName" placement="bottom" theme="light">
                <li :class="['icon-item', { active: modelValue == iconName }]" @click="selectedIcon(iconName)">
                  <t-icon :name="iconName" size="20px" />
                </li>
              </t-tooltip>
            </ul>
            <div v-if="iconNames.length < filteredIcons.length" class="icon-more" @click="loadMore">
              已显示 {{ iconNames.length }} / {{ filteredIcons.length }} 个，点击加载更多
            </div>
            <div v-else-if="filteredIcons.length" class="icon-count">共 {{ filteredIcons.length }} 个图标</div>
          </div>
        </div>
      </template>
    </t-popup>
  </div>
</template>

<script setup lang="ts">
import { CaretUpIcon, CaretDownIcon } from 'tdesign-icons-vue-next';
import icons from '@/components/IconSelect/requireIcons';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  modelValue: propTypes.string.isRequired,
  width: propTypes.string.def('400px')
});

const emit = defineEmits(['update:modelValue']);
const visible = ref(false);
const { modelValue, width } = toRefs(props);
const keyword = ref('');
// 全量 2300+ 个图标一次渲染会明显拖慢弹层，默认只渲染首屏，搜索或“加载更多”再扩量
const PAGE_SIZE = 240;
const displayLimit = ref(PAGE_SIZE);
const filteredIcons = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return kw ? icons.filter((iconName) => iconName.includes(kw)) : icons;
});
const iconNames = computed(() => filteredIcons.value.slice(0, displayLimit.value));

// t-popup 用 visible 受控，点击外部关闭时同步回本地状态
const onVisibleChange = (v: boolean) => {
  visible.value = v;
};

// 搜索条件变化后回到首屏，避免在旧分页位置显示空白
watch(keyword, () => {
  displayLimit.value = PAGE_SIZE;
});

/**
 * 追加渲染下一批图标
 */
const loadMore = () => {
  displayLimit.value += PAGE_SIZE;
};

/**
 * 选择图标
 * @param iconName 选择的图标名称
 */
const selectedIcon = (iconName: string) => {
  emit('update:modelValue', iconName);
  visible.value = false;
};
</script>

<style lang="scss" scoped>
.icon-select-panel {
  width: 520px;
}
.icon-search {
  padding: 8px;
}
.icon-select-scroll {
  max-height: calc(50vh - 100px) !important;
  overflow-y: auto;
}
.icon-list {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
  padding: 0 10px 10px;
  margin: 0;

  .icon-item {
    cursor: pointer;
    padding: 8px 0;
    display: flex;
    justify-content: center;
    align-items: center;
    color: var(--td-text-color-secondary);
    border: 1px solid var(--td-component-stroke);
    border-radius: 4px;
    &:hover {
      border-color: var(--td-brand-color);
      color: var(--td-brand-color);
      transition: all 0.2s;
    }
  }
  .active {
    border-color: var(--td-brand-color);
    color: var(--td-brand-color);
    background: var(--td-brand-color-light);
  }
}
.icon-more,
.icon-count {
  padding: 0 10px 10px;
  color: var(--td-text-color-placeholder);
  font-size: 12px;
  text-align: center;
}
.icon-more {
  cursor: pointer;
  color: var(--td-brand-color);
}
</style>
