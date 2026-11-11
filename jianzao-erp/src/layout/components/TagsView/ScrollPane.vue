<template>
  <!-- TDesign 无 scrollbar 组件，用原生 div + CSS overflow-x 承接横向滚动 -->
  <div ref="scrollContainerRef" class="scroll-container" @wheel.prevent="handleScroll">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { RouteLocationNormalized } from 'vue-router';
import { useTagsViewStore } from '@/store/modules/tagsView';

const tagAndTagSpacing = ref(4);

const scrollContainerRef = ref<HTMLDivElement>();
const scrollWrapper = computed<HTMLDivElement | undefined>(() => scrollContainerRef.value);

onMounted(() => {
  scrollWrapper.value?.addEventListener('scroll', emitScroll, true);
});
onBeforeUnmount(() => {
  scrollWrapper.value?.removeEventListener('scroll', emitScroll, true);
});

const handleScroll = (e: WheelEvent) => {
  const eventDelta = (e as any).wheelDelta || -e.deltaY * 40;
  const $scrollWrapper = scrollWrapper.value;
  if (!$scrollWrapper) return;

  $scrollWrapper.scrollLeft = $scrollWrapper.scrollLeft + eventDelta / 4;
};
const emits = defineEmits(['scroll']);
const emitScroll = () => {
  emits('scroll');
};

const tagsViewStore = useTagsViewStore();
const visitedViews = computed(() => tagsViewStore.visitedViews);

const moveToTarget = (currentTag: RouteLocationNormalized) => {
  const $container = scrollContainerRef.value;
  if (!$container) return;
  const $containerWidth = $container.offsetWidth;
  const $scrollWrapper = scrollWrapper.value;
  if (!$scrollWrapper) return;

  let firstTag = null;
  let lastTag = null;

  // find first tag and last tag
  if (visitedViews.value.length > 0) {
    firstTag = visitedViews.value[0];
    lastTag = visitedViews.value[visitedViews.value.length - 1];
  }

  if (firstTag === currentTag) {
    $scrollWrapper.scrollLeft = 0;
  } else if (lastTag === currentTag) {
    $scrollWrapper.scrollLeft = $scrollWrapper.scrollWidth - $containerWidth;
  } else {
    const tagListDom: any = document.getElementsByClassName('tags-view-item');
    const currentIndex = visitedViews.value.findIndex((item) => item === currentTag);
    let prevTag = null;
    let nextTag = null;

    for (const k in tagListDom) {
      if (k !== 'length' && Object.hasOwnProperty.call(tagListDom, k)) {
        if (tagListDom[k].dataset.path === visitedViews.value[currentIndex - 1].path) {
          prevTag = tagListDom[k];
        }
        if (tagListDom[k].dataset.path === visitedViews.value[currentIndex + 1].path) {
          nextTag = tagListDom[k];
        }
      }
    }

    // the tag's offsetLeft after of nextTag
    const afterNextTagOffsetLeft = nextTag.offsetLeft + nextTag.offsetWidth + tagAndTagSpacing.value;

    // the tag's offsetLeft before of prevTag
    const beforePrevTagOffsetLeft = prevTag.offsetLeft - tagAndTagSpacing.value;
    if (afterNextTagOffsetLeft > $scrollWrapper.scrollLeft + $containerWidth) {
      $scrollWrapper.scrollLeft = afterNextTagOffsetLeft - $containerWidth;
    } else if (beforePrevTagOffsetLeft < $scrollWrapper.scrollLeft) {
      $scrollWrapper.scrollLeft = beforePrevTagOffsetLeft;
    }
  }
};

defineExpose({
  moveToTarget
});
</script>

<style lang="scss" scoped>
.scroll-container {
  white-space: nowrap;
  position: relative;
  width: 100%;
  height: 49px;
  overflow-x: auto;
  overflow-y: hidden;
}
</style>
