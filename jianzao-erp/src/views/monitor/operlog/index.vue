<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card :bordered="false" class="erp-filter-card" title="筛选条件">
          <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row" label-align="right" :label-width="'auto'" @submit="handleQuery">
            <t-form-item label="操作地址" name="operIp">
              <t-input v-model="queryParams.operIp" placeholder="请输入操作地址" clearable class="query-w-md" @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="系统模块" name="title">
              <t-input v-model="queryParams.title" placeholder="请输入系统模块" clearable class="query-w-md" @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="操作人员" name="operName">
              <t-input v-model="queryParams.operName" placeholder="请输入操作人员" clearable class="query-w-md" @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="类型" name="businessType">
              <t-select v-model="queryParams.businessType" placeholder="操作类型" clearable class="query-w-sm" :options="sys_oper_type" />
            </t-form-item>
            <t-form-item label="状态" name="status">
              <t-select v-model="queryParams.status" placeholder="操作状态" clearable class="query-w-sm" :options="sys_common_status" />
            </t-form-item>
            <t-form-item label="操作时间" class="query-w-lg">
              <t-date-range-picker
                v-model="dateRange"
                value-type="YYYY-MM-DD HH:mm:ss"
                :placeholder="['开始日期', '结束日期']"
                :default-time="['00:00:00', '23:59:59']"
              />
            </t-form-item>
            <div class="erp-filter-actions">
              <t-button theme="primary" @click="handleQuery">
                <template #icon><SearchIcon /></template>搜索</t-button
              >
              <t-button theme="default" variant="outline" @click="resetQuery">
                <template #icon><RefreshIcon /></template>重置</t-button
              >
            </div>
          </t-form>
        </t-card>
      </div>
    </transition>

    <div class="erp-surface erp-panel">
      <div class="erp-panel__head">
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-2">
            <t-button v-hasPermi="['monitor:operlog:remove']" theme="danger" variant="outline" :disabled="multiple" @click="handleDelete()">
              <template #icon><DeleteIcon /></template>删除
            </t-button>
            <t-button v-hasPermi="['monitor:operlog:remove']" theme="danger" variant="outline" @click="handleClean">
              <template #icon><ErrorTriangleFilledIcon /></template>清空
            </t-button>
            <t-button v-hasPermi="['monitor:operlog:export']" theme="warning" variant="outline" @click="handleExport">
              <template #icon><DownloadIcon /></template>导出
            </t-button>
          </div>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </div>

      <t-table
        ref="operLogTableRef"
        class="erp-table"
        row-key="operId"
        :data="operlogList"
        :columns="listColumns"
        :loading="loading"
        :hover="true"
        :resizable="true"
        size="small"
        cell-empty-content="-"
        :sort="sortInfo"
        :selected-row-keys="ids"
        @update:sort="sortInfo = $event"
        @select-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <template #businessType="{ row }">
          <dict-tag :options="sys_oper_type" :value="row.businessType" />
        </template>
        <template #status="{ row }">
          <dict-tag :options="sys_common_status" :value="row.status" />
        </template>
        <template #operTime="{ row }">
          <span>{{ proxy.parseTime(row.operTime) }}</span>
        </template>
        <template #costTime="{ row }">
          <span>{{ row.costTime }}毫秒</span>
        </template>
        <template #operation="{ row }">
          <!-- 权限判断改挂外层：v-hasPermi 在 mounted 删节点会与 t-tooltip 的包裹结构打架 -->
          <t-tooltip v-if="checkPermi(['monitor:operlog:query'])" content="详细" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleView(row)">
              <template #icon><BrowseIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </t-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </div>
    <!-- 操作日志详细 -->
    <OperInfoDialog ref="operInfoDialogRef" />
  </div>
</template>

<script setup name="Operlog" lang="ts">
import { list, delOperlog, cleanOperlog } from '@/api/monitor/operlog';
import { OperLogForm, OperLogQuery, OperLogVO } from '@/api/monitor/operlog/types';
import OperInfoDialog from './oper-info-dialog.vue';
import { checkPermi } from '@/utils/permission';
import { BrowseIcon, DeleteIcon, DownloadIcon, ErrorTriangleFilledIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_oper_type, sys_common_status } = toRefs<any>(proxy?.useDict('sys_oper_type', 'sys_common_status'));

const operlogList = ref<OperLogVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const multiple = ref(true);
const total = ref(0);
const dateRange = ref<[DateModelType, DateModelType]>(['', '']);
// t-table 的排序用 { sortBy, descending } 描述，字段与升降序和原 defaultSort 一一对应
const sortInfo = ref<any>({ sortBy: 'operTime', descending: true });

const operLogTableRef = ref<any>();
const queryFormRef = ref<any>();

const data = reactive<PageData<OperLogForm, OperLogQuery>>({
  form: {
    operId: undefined,
    tenantId: undefined,
    title: '',
    businessType: 0,
    businessTypes: undefined,
    method: '',
    requestMethod: '',
    operatorType: 0,
    operName: '',
    deptName: '',
    operUrl: '',
    operIp: '',
    operLocation: '',
    operParam: '',
    jsonResult: '',
    status: 0,
    errorMsg: '',
    operTime: '',
    costTime: 0
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    operIp: '',
    title: '',
    operName: '',
    businessType: '',
    status: '',
    orderByColumn: 'operTime',
    isAsc: 'descending'
  },
  rules: {}
});

const { queryParams, form } = toRefs(data);

/** 操作日志列表列配置：prop→colKey、show-overflow-tooltip→ellipsis、模板列走同名 cell 插槽 */
const listColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 50, align: 'center' },
  { colKey: 'operId', title: '日志编号', align: 'center' },
  { colKey: 'title', title: '系统模块', align: 'center', ellipsis: true },
  { colKey: 'businessType', title: '操作类型', align: 'center', cell: 'businessType' },
  { colKey: 'operName', title: '操作人员', align: 'center', width: 110, ellipsis: true, sorter: true },
  { colKey: 'deptName', title: '部门', align: 'center', width: 130, ellipsis: true },
  { colKey: 'operIp', title: '操作地址', align: 'center', width: 130, ellipsis: true },
  { colKey: 'status', title: '操作状态', align: 'center', cell: 'status' },
  { colKey: 'operTime', title: '操作日期', align: 'center', width: 180, sorter: true, cell: 'operTime' },
  { colKey: 'costTime', title: '消耗时间', align: 'center', width: 110, ellipsis: true, sorter: true, cell: 'costTime' },
  { colKey: 'operation', title: '操作', fixed: 'right', align: 'center', width: 80, className: 'small-padding fixed-width', cell: 'operation' }
];

/** 查询登录日志 */
const getList = async () => {
  loading.value = true;
  const res = await list(proxy?.addDateRange(queryParams.value, dateRange.value));
  operlogList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};
/** 操作日志类型字典翻译 */
const typeFormat = (row: OperLogForm) => {
  return proxy?.selectDictLabel(sys_oper_type.value, row.businessType);
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
/** 重置按钮操作：dateRange 与排序态回默认（t-form.reset 只清它自己管理的字段） */
const resetQuery = () => {
  dateRange.value = ['', ''];
  queryFormRef.value?.reset();
  queryParams.value.pageNum = 1;
  sortInfo.value = { sortBy: 'operTime', descending: true };
  handleQuery();
};
/** 多选框选中数据：t-table 的 select-change 只回传选中的 rowKey（rowKey=operId） */
const handleSelectionChange = (keys: Array<string | number>) => {
  ids.value = keys;
  multiple.value = !keys.length;
};
/** 排序触发事件：t-table 回传 { sortBy, descending }，后端仍用原字段名与升降序字符串 */
const handleSortChange = (sort: any) => {
  queryParams.value.orderByColumn = sort?.sortBy ?? '';
  queryParams.value.isAsc = sort ? (sort.descending ? 'descending' : 'ascending') : '';
  getList();
};

const operInfoDialogRef = ref<InstanceType<typeof OperInfoDialog>>();
/** 详细按钮操作 */
const handleView = (row: OperLogVO) => {
  operInfoDialogRef.value.openDialog(row);
};

/** 删除按钮操作 */
const handleDelete = async (row?: OperLogVO) => {
  const operIds = row?.operId || ids.value;
  await proxy?.$modal.confirm('是否确认删除日志编号为"' + operIds + '"的数据项?');
  await delOperlog(operIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

/** 清空按钮操作 */
const handleClean = async () => {
  await proxy?.$modal.confirm('是否确认清空所有操作日志数据项?');
  await cleanOperlog();
  await getList();
  proxy?.$modal.msgSuccess('清空成功');
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'monitor/operlog/export',
    {
      ...queryParams.value
    },
    `config_${new Date().getTime()}.xlsx`
  );
};
onMounted(() => {
  getList();
});
</script>
