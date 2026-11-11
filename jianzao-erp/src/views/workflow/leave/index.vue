<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card>
          <t-form ref="queryFormRef" :data="queryParams" layout="inline" class="erp-filter-row">
            <t-form-item label="请假天数" name="startLeaveDays">
              <t-input v-model="queryParams.startLeaveDays" placeholder="请输入请假天数" clearable @enter="handleQuery" />
            </t-form-item>
            <t-form-item name="endLeaveDays"> 至 </t-form-item>
            <t-form-item name="endLeaveDays">
              <t-input v-model="queryParams.endLeaveDays" placeholder="请输入请假天数" clearable @enter="handleQuery" />
            </t-form-item>
            <div class="erp-filter-actions">
              <t-button theme="primary" @click="handleQuery">
                <template #icon><SearchIcon /></template>
                搜索
              </t-button>
              <t-button theme="default" variant="outline" @click="resetQuery">
                <template #icon><RefreshIcon /></template>
                重置
              </t-button>
            </div>
          </t-form>
        </t-card>
      </div>
    </transition>

    <t-card>
      <template #header>
        <div class="leave-toolbar">
          <t-button v-hasPermi="['workflow:leave:add']" theme="primary" variant="outline" @click="handleAdd">
            <template #icon><PlusIcon /></template>
            新增
          </t-button>
          <t-button v-hasPermi="['workflow:leave:export']" theme="warning" variant="outline" @click="handleExport">
            <template #icon><DownloadIcon /></template>
            导出
          </t-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>

      <t-table
        class="erp-table"
        row-key="id"
        :data="leaveList"
        :columns="leaveColumns"
        :loading="loading"
        :hover="true"
        :resizable="true"
        size="small"
        bordered
        cell-empty-content="-"
        :selected-row-keys="ids"
        @select-change="handleSelectionChange"
      >
        <template #leaveType="{ row }">
          <t-tag theme="default">{{ options.find((e) => e.value === row.leaveType)?.label }}</t-tag>
        </template>
        <template #startDate="{ row }">
          <span>{{ proxy.parseTime(row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
        <template #endDate="{ row }">
          <span>{{ proxy.parseTime(row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
        <template #status="{ row }">
          <dict-tag :options="wf_business_status" :value="row.status"></dict-tag>
        </template>
        <template #operation="{ row }">
          <div class="leave-operations">
            <t-button
              v-if="(row.status === 'draft' || row.status === 'cancel' || row.status === 'back') && checkPermi(['workflow:leave:edit'])"
              theme="primary"
              variant="text"
              size="small"
              @click="handleUpdate(row)"
            >
              <template #icon><Edit1Icon /></template>
              修改
            </t-button>
            <t-button
              v-if="(row.status === 'draft' || row.status === 'cancel' || row.status === 'back') && checkPermi(['workflow:leave:remove'])"
              theme="primary"
              variant="text"
              size="small"
              @click="handleDelete(row)"
            >
              <template #icon><DeleteIcon /></template>
              删除
            </t-button>
            <t-button theme="primary" variant="text" size="small" @click="handleView(row)">
              <template #icon><BrowseIcon /></template>
              查看
            </t-button>
            <t-button v-if="row.status === 'waiting'" theme="primary" variant="text" size="small" @click="handleCancelProcessApply(row.id)">
              <template #icon><NotificationIcon /></template>
              撤销
            </t-button>
          </div>
        </template>
      </t-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </t-card>
  </div>
</template>

<script setup name="Leave" lang="ts">
import { delLeave, listLeave } from '@/api/workflow/leave';
import { cancelProcessApply } from '@/api/workflow/instance';
import { LeaveForm, LeaveQuery, LeaveVO } from '@/api/workflow/leave/types';
import { checkPermi } from '@/utils/permission';
// TDesign 图标必须显式 import，不会自动解析（DOC/35 §3）
import { BrowseIcon, DeleteIcon, DownloadIcon, Edit1Icon, NotificationIcon, PlusIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { wf_business_status } = toRefs<any>(proxy?.useDict('wf_business_status'));
const leaveList = ref<LeaveVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const options = [
  {
    value: '1',
    label: '事假'
  },
  {
    value: '2',
    label: '调休'
  },
  {
    value: '3',
    label: '病假'
  },
  {
    value: '4',
    label: '婚假'
  }
];

const queryFormRef = ref<any>();

const data = reactive<{ form: LeaveForm; queryParams: LeaveQuery }>({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    startLeaveDays: undefined,
    endLeaveDays: undefined
  }
});

const { queryParams } = toRefs(data);

/** 请假列表列配置：t-table 用 columns 数组，cell 字符串与 #插槽同名（DOC/35 §5） */
const leaveColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 55, align: 'center' },
  { colKey: 'leaveType', title: '请假类型', align: 'center', cell: 'leaveType' },
  { colKey: 'startDate', title: '开始时间', align: 'center', cell: 'startDate' },
  { colKey: 'endDate', title: '结束时间', align: 'center', cell: 'endDate' },
  { colKey: 'leaveDays', title: '请假天数', align: 'center' },
  { colKey: 'remark', title: '请假原因', align: 'center' },
  { colKey: 'status', title: '流程状态', align: 'center', minWidth: 70, cell: 'status' },
  { colKey: 'operation', title: '操作', align: 'center', width: 162, cell: 'operation' }
];

/** 查询请假列表 */
const getList = async () => {
  loading.value = true;
  const res = await listLeave(queryParams.value);
  leaveList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  handleQuery();
};

/** 多选框选中数据：t-table 的 select-change 只回传选中的 rowKey（rowKey=id） */
const handleSelectionChange = (selection: Array<string | number>) => {
  ids.value = selection;
  single.value = selection.length != 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  proxy.$tab.closePage(proxy.$route);
  proxy.$router.push({
    path: `/workflow/leaveEdit/index`,
    query: {
      type: 'add'
    }
  });
};

/** 修改按钮操作 */
const handleUpdate = (row?: LeaveVO) => {
  proxy.$tab.closePage(proxy.$route);
  proxy.$router.push({
    path: `/workflow/leaveEdit/index`,
    query: {
      id: row.id,
      type: 'update'
    }
  });
};

/** 查看按钮操作 */
const handleView = (row?: LeaveVO) => {
  proxy.$tab.closePage(proxy.$route);
  proxy.$router.push({
    path: `/workflow/leaveEdit/index`,
    query: {
      id: row.id,
      type: 'view'
    }
  });
};

/** 删除按钮操作 */
const handleDelete = async (row?: LeaveVO) => {
  const _ids = row?.id || ids.value;
  await proxy?.$modal.confirm('是否确认删除请假编号为"' + _ids + '"的数据项？').finally(() => (loading.value = false));
  await delLeave(_ids);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'workflow/leave/export',
    {
      ...queryParams.value
    },
    `leave_${new Date().getTime()}.xlsx`
  );
};

/** 撤销按钮操作 */
const handleCancelProcessApply = async (id: string) => {
  await proxy?.$modal.confirm('是否确认撤销当前单据？');
  loading.value = true;
  const data = {
    businessId: id,
    message: '申请人撤销流程！'
  };
  await cancelProcessApply(data).finally(() => (loading.value = false));
  await getList();
  proxy?.$modal.msgSuccess('撤销成功');
};
onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.leave-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.leave-operations {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
