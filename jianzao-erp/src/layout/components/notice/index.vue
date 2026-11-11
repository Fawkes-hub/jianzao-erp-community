<template>
  <div v-loading="state.loading" class="layout-navbars-breadcrumb-user-news">
    <div class="head-box">
      <div class="head-box-title">通知公告</div>
      <div class="head-box-btn" @click="readAll">全部已读</div>
    </div>
    <div v-loading="state.loading" class="content-box">
      <template v-if="newsList.length > 0">
        <div v-for="(v, k) in newsList" :key="k" class="content-box-item" @click="onNewsClick(k)">
          <div class="item-conten">
            <div>{{ v.message }}</div>
            <div class="content-box-msg"></div>
            <div class="content-box-time">{{ v.time }}</div>
          </div>
          <!-- 已读/未读 -->
          <t-tag v-if="v.read" class="read" theme="success" size="small">已读</t-tag>
          <t-tag v-else class="read" theme="danger" size="small">未读</t-tag>
        </div>
      </template>
      <t-empty v-else description="消息为空"></t-empty>
    </div>
  </div>
</template>

<script setup lang="ts" name="layoutBreadcrumbUserNews">
import { useNoticeStore } from '@/store/modules/notice';

const noticeStore = useNoticeStore();
const { readAll } = useNoticeStore();

// 定义变量内容
const state = reactive({
  loading: false
});
const newsList = ref([]) as any;

/**
 * 初始化数据
 * @returns
 */
const getTableData = async () => {
  state.loading = true;
  newsList.value = noticeStore.state.notices;
  state.loading = false;
};

//点击消息，写入已读
const onNewsClick = (item: any) => {
  newsList.value[item].read = true;
  //并且写入pinia
  noticeStore.state.notices = newsList.value;
};

// 前往通知中心点击

onMounted(() => {
  nextTick(() => {
    getTableData();
  });
});
</script>

<style lang="scss" scoped>
.layout-navbars-breadcrumb-user-news {
  .head-box {
    display: flex;
    border-bottom: 1px solid var(--td-component-stroke);
    box-sizing: border-box;
    color: var(--td-text-color-primary);
    justify-content: space-between;
    height: 35px;
    align-items: center;
    .head-box-btn {
      color: var(--td-brand-color);
      font-size: 13px;
      cursor: pointer;
      opacity: 0.8;
      &:hover {
        opacity: 1;
      }
    }
  }
  .content-box {
    height: 300px;
    overflow: auto;
    font-size: 13px;
    .content-box-item {
      padding-top: 12px;
      display: flex;
      &:last-of-type {
        padding-bottom: 12px;
      }
      .content-box-msg {
        color: var(--td-text-color-secondary);
        margin-top: 5px;
        margin-bottom: 5px;
      }
      .content-box-time {
        color: var(--td-text-color-secondary);
      }
      .item-conten {
        width: 100%;
        display: flex;
        flex-direction: column;
      }
    }
  }
  .foot-box {
    height: 35px;
    color: var(--td-brand-color);
    font-size: 13px;
    cursor: pointer;
    opacity: 0.8;
    display: flex;
    align-items: center;
    justify-content: center;
    border-top: 1px solid var(--td-component-stroke);
    &:hover {
      opacity: 1;
    }
  }
  :deep(.t-empty__description) {
    font-size: 13px;
  }
}
</style>
