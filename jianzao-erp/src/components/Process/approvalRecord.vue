<template>
  <div class="container">
    <t-dialog
      v-model:visible="visible"
      :width="props.width"
      draggable
      attach="body"
      :footer="false"
      :close-on-overlay-click="false"
      dialog-class-name="approval-record-dialog"
    >
      <template #header>审批记录</template>
      <t-tabs v-model="tabActiveName">
        <t-tab-panel label="流程图" value="image">
          <div class="flow-chart-pane" v-loading="loading" style="height: 68vh">
            <flowChart :ins-id="insId" v-if="insId" />
          </div>
        </t-tab-panel>
        <t-tab-panel label="审批信息" value="info">
          <div v-loading="loading">
            <t-table
              class="erp-table"
              :data="historyList"
              :columns="historyColumns"
              :resizable="true"
              row-key="__key"
              bordered
              size="small"
              cell-empty-content="-"
              style="width: 100%"
            >
              <template #approveName="{ row }">
                <template v-if="row.approveName">
                  <t-tag v-for="(item, index) in row.approveName.split(',')" :key="index" theme="success" variant="light" size="small">
                    {{ item }}
                  </t-tag>
                </template>
                <t-tag v-else theme="success" variant="light" size="small">无</t-tag>
              </template>
              <template #flowStatus="{ row }">
                <dict-tag :options="wf_task_status" :value="row.flowStatus"></dict-tag>
              </template>
              <template #attachmentList="{ row }">
                <t-popup
                  v-if="row.attachmentList && row.attachmentList.length > 0"
                  placement="right"
                  trigger="click"
                  :overlay-inner-style="{ width: '310px' }"
                >
                  <t-button theme="primary" size="small" style="margin-right: 16px">附件</t-button>
                  <template #content>
                    <t-table
                      class="erp-table"
                      :data="row.attachmentList"
                      :columns="attachmentColumns"
                      :resizable="true"
                      row-key="ossId"
                      bordered
                      size="small"
                      cell-empty-content="-"
                    >
                      <template #name="{ row: file }">
                        <t-button theme="primary" variant="text" size="small" @click="handleDownload(file.ossId)">下载</t-button>
                      </template>
                    </t-table>
                  </template>
                </t-popup>
              </template>
            </t-table>
          </div>
        </t-tab-panel>
      </t-tabs>
    </t-dialog>
  </div>
</template>
<script setup lang="ts">
import { flowHisTaskList } from '@/api/workflow/instance';
import { propTypes } from '@/utils/propTypes';
import { listByIds } from '@/api/system/oss';
import FlowChart from '@/components/Process/flowChart.vue';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { wf_task_status } = toRefs<any>(proxy?.useDict('wf_task_status'));
const props = defineProps({
  width: propTypes.string.def('80%'),
  height: propTypes.string.def('100%')
});

// TDesign 的 sorter 传 true 只发排序事件、不本地排序；补一个与 el-table-column sortable 等价的本地比较器
const localSorter = (colKey: string) => (a: any, b: any) => {
  const x = a?.[colKey];
  const y = b?.[colKey];
  if (x == null && y == null) return 0;
  if (x == null) return -1;
  if (y == null) return 1;
  return x < y ? -1 : x > y ? 1 : 0;
};

// 审批信息列：prop→colKey、label→title、show-overflow-tooltip→ellipsis、sortable→sorter
const historyColumns = computed<any[]>(() => [
  { colKey: 'serial-number', title: '序号', align: 'center', width: 60 },
  { colKey: 'nodeName', title: '任务名称', sorter: localSorter('nodeName'), align: 'center' },
  { colKey: 'approveName', title: '办理人', sorter: localSorter('approveName'), align: 'center', ellipsis: true, cell: 'approveName' },
  { colKey: 'flowStatus', title: '状态', width: 80, sorter: localSorter('flowStatus'), align: 'center', cell: 'flowStatus' },
  { colKey: 'message', title: '审批意见', ellipsis: true, sorter: localSorter('message'), align: 'center' },
  { colKey: 'createTime', title: '开始时间', width: 160, ellipsis: true, sorter: localSorter('createTime'), align: 'center' },
  { colKey: 'updateTime', title: '结束时间', width: 160, ellipsis: true, sorter: localSorter('updateTime'), align: 'center' },
  { colKey: 'runDuration', title: '运行时长', width: 140, ellipsis: true, sorter: localSorter('runDuration'), align: 'center' },
  { colKey: 'attachmentList', width: 120, title: '附件', align: 'center', cell: 'attachmentList' }
]);

// 附件弹层内表格列：操作列走 cell 指名插槽（原 el-table-column 的 #default 覆盖）
const attachmentColumns = computed<any[]>(() => [
  { colKey: 'originalName', width: 202, ellipsis: true, title: '附件名称' },
  { colKey: 'name', width: 80, align: 'center', title: '操作', cell: 'name' }
]);

const loading = ref(false);
const visible = ref(false);
const historyList = ref<Array<any>>([]);
const tabActiveName = ref('image');
const insId = ref(null);

//初始化查询审批记录
const init = async (businessId: string | number) => {
  visible.value = true;
  loading.value = true;
  tabActiveName.value = 'image';
  historyList.value = [];
  flowHisTaskList(businessId).then((resp) => {
    if (resp.data) {
      // 历史任务没有稳定业务主键，按 SKILL §4 生成前端 __key 供 t-table 行标识（纯展示、不提交）
      historyList.value = (resp.data.list ?? []).map((row: any, index: number) => ({ ...row, __key: `his-${index}` }));
      insId.value = resp.data.instanceId;
      if (historyList.value.length > 0) {
        historyList.value.forEach((item) => {
          if (item.ext) {
            getIds(item.ext).then((res) => {
              item.attachmentList = res.data;
            });
          } else {
            item.attachmentList = [];
          }
        });
      }
      loading.value = false;
    }
  });
};
const getIds = async (ids: string | number) => {
  const res = await listByIds(ids);
  return res;
};

/** 下载按钮操作 */
const handleDownload = (ossId: string) => {
  proxy?.$download.oss(ossId);
};

/**
 * 对外暴露子组件方法
 */
defineExpose({
  init
});
</script>
<style lang="scss" scoped>
/* attach="body" 的弹层渲染在 body 下，scoped 前缀命中不到，用 :global 精确限定本弹窗类名 */
:global(.approval-record-dialog .t-dialog__body) {
  max-height: calc(100vh - 170px) !important;
  min-height: calc(100vh - 170px) !important;
}
</style>
