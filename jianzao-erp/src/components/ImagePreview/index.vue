<template>
  <div class="image-preview-root" :style="`width:${realWidth};height:${realHeight};`">
    <t-image-viewer :images="realSrcList" attach="body" :close-on-overlay="true">
      <template #trigger="{ open }">
        <div class="image-preview-trigger" @click="realSrcList.length > 0 && open(0)">
          <t-image :src="realSrc" fit="cover" class="image-preview__img">
            <template #error>
              <div class="image-preview__slot">
                <ImageErrorIcon />
              </div>
            </template>
          </t-image>
        </div>
      </template>
    </t-image-viewer>
  </div>
</template>

<script setup lang="ts">
import { ImageErrorIcon } from 'tdesign-icons-vue-next';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  src: propTypes.string.def(''),
  width: {
    type: [Number, String],
    default: ''
  },
  height: {
    type: [Number, String],
    default: ''
  }
});

const realSrc = computed(() => {
  if (!props.src) {
    return;
  }
  const real_src = props.src.split(',')[0];
  return real_src;
});

const realSrcList = computed(() => {
  if (!props.src) {
    return [];
  }
  const real_src_list = props.src.split(',');
  const srcList: string[] = [];
  real_src_list.forEach((item: string) => {
    if (item.trim() === '') {
      return;
    }
    return srcList.push(item);
  });
  return srcList;
});

const realWidth = computed(() => (typeof props.width == 'string' ? props.width : `${props.width}px`));

const realHeight = computed(() => (typeof props.height == 'string' ? props.height : `${props.height}px`));
</script>

<style lang="scss" scoped>
.image-preview-root {
  border-radius: 5px;
  background-color: #ebeef5;
  box-shadow: 0 0 5px 1px #ccc;

  .image-preview-trigger {
    width: 100%;
    height: 100%;
  }

  :deep(.t-image__wrapper) {
    width: 100%;
    height: 100%;
  }

  :deep(.t-image) {
    width: 100%;
    height: 100%;
    transition: all 0.3s;
    cursor: pointer;

    &:hover {
      transform: scale(1.2);
    }
  }

  :deep(.image-preview__slot) {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    color: #909399;
    font-size: 30px;
  }
}
</style>
