<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card :bordered="false" class="erp-filter-card">
          <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row" label-align="right" :label-width="'auto'" @submit="handleQuery">
            <t-form-item label="登录地址" name="ipaddr">
              <t-input v-model="queryParams.ipaddr" placeholder="请输入登录地址" clearable class="query-w-md" @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="用户名称" name="userName">
              <t-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable class="query-w-md" @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="状态" name="status">
              <t-select v-model="queryParams.status" placeholder="登录状态" clearable class="query-w-sm" :options="sys_common_status" />
            </t-form-item>
            <t-form-item label="登录时间">
              <t-date-range-picker
                v-model="dateRange"
                mode="date"
                value-type="YYYY-MM-DD HH:mm:ss"
                enable-time-picker
                :placeholder="['开始日期', '结束日期']"
                :default-time="['00:00:00', '23:59:59']"
                class="query-w-lg"
              />
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

    <t-card :bordered="false" class="erp-filter-card">
      <template #header>
        <div class="logininfor-toolbar">
          <t-button v-if="checkPermi(['monitor:logininfor:remove'])" theme="danger" variant="outline" :disabled="multiple" @click="handleDelete()">
            <template #icon><DeleteIcon /></template>
            删除
          </t-button>
          <t-button v-if="checkPermi(['monitor:logininfor:remove'])" theme="danger" variant="outline" @click="handleClean">
            <template #icon><DeleteIcon /></template>
            清空
          </t-button>
          <t-button v-if="checkPermi(['monitor:logininfor:unlock'])" theme="primary" variant="outline" :disabled="single" @click="handleUnlock">
            <template #icon><LockOffIcon /></template>
            解锁
          </t-button>
          <t-button v-if="checkPermi(['monitor:logininfor:export'])" theme="warning" variant="outline" @click="handleExport">
            <template #icon><DownloadIcon /></template>
            导出
          </t-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>

      <t-table
        class="erp-table"
        row-key="infoId"
        :data="loginInfoList"
        :columns="loginInfoColumns"
        :loading="loading"
        :hover="true"
        :resizable="true"
        :sort="sortInfo"
        size="small"
        bordered
        cell-empty-content="-"
        :selected-row-keys="ids"
        @select-change="handleSelectionChange"
        @sort-change="handleSortChange"
        @column-resize-change="(ctx: any) => saveColumnWidths('monitor-logininfor/list', ctx.columnsWidth)"
      >
        <template #deviceType="{ row }">
          <dict-tag :options="sys_device_type" :value="row.deviceType" />
        </template>
        <template #status="{ row }">
          <dict-tag :options="sys_common_status" :value="row.status" />
        </template>
        <template #loginTime="{ row }">
          <span>{{ proxy.parseTime(row.loginTime) }}</span>
        </template>
      </t-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </t-card>
  </div>
</template>

<script setup name="Logininfor" lang="ts">
import { list, delLoginInfo, cleanLoginInfo, unlockLoginInfo } from '@/api/monitor/loginInfo';
import { LoginInfoQuery, LoginInfoVO } from '@/api/monitor/loginInfo/types';
// 列宽记忆：拖拽后按表名存 localStorage，进页面还原（见 hooks/useTableColumnWidth）
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
// 权限判断改外层 v-if：原权限指令在 mounted 里 removeChild，会与卡片头插槽/TD 结构打架（DOC/36 P6）
import { checkPermi } from '@/utils/permission';
// TDesign 图标必须显式 import，不会自动解析（DOC/35 §3）
import { DeleteIcon, DownloadIcon, LockOffIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_device_type } = toRefs<any>(proxy?.useDict('sys_device_type'));
const { sys_common_status } = toRefs<any>(proxy?.useDict('sys_common_status'));

const loginInfoList = ref<LoginInfoVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const selectName = ref<Array<string>>([]);
const total = ref(0);
const dateRange = ref<[string, string]>(['', '']);
const defaultSort = ref<any>({ prop: 'loginTime', order: 'descending' });
// t-table 排序为受控 sort，形状 { sortBy, descending }；由 sort-change 回写并触发后端排序查询
const sortInfo = ref<any>({ sortBy: defaultSort.value.prop, descending: true });

const queryFormRef = ref<any>();
// 查询参数
const queryParams = ref<LoginInfoQuery>({
  pageNum: 1,
  pageSize: 10,
  ipaddr: '',
  userName: '',
  status: '',
  orderByColumn: defaultSort.value.prop,
  isAsc: defaultSort.value.order
});

/** 登录日志列表列配置：t-table 用 columns 数组，插槽名与 cell 字符串同名（DOC/35 §5） */
const loginInfoColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 55, align: 'center' },
  { colKey: 'infoId', title: '访问编号', align: 'center' },
  { colKey: 'userName', title: '用户名称', align: 'center', ellipsis: true, sorter: true },
  { colKey: 'clientKey', title: '客户端', align: 'center', ellipsis: true },
  { colKey: 'deviceType', title: '设备类型', align: 'center', cell: 'deviceType' },
  { colKey: 'ipaddr', title: '地址', align: 'center', ellipsis: true },
  { colKey: 'loginLocation', title: '登录地点', align: 'center', ellipsis: true },
  { colKey: 'os', title: '操作系统', align: 'center', ellipsis: true },
  { colKey: 'browser', title: '浏览器', align: 'center', ellipsis: true },
  { colKey: 'status', title: '登录状态', align: 'center', cell: 'status' },
  { colKey: 'msg', title: '描述', align: 'center', ellipsis: true },
  { colKey: 'loginTime', title: '访问时间', align: 'center', width: 180, sorter: true, cell: 'loginTime' }
];
mergeSavedWidths('monitor-logininfor/list', loginInfoColumns);

/** 查询登录日志列表 */
const getList = async () => {
  loading.value = true;
  const res = await list(proxy?.addDateRange(queryParams.value, dateRange.value));
  loginInfoList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
/** 重置按钮操作：t-form 的 reset() 默认重置字段，排序回到默认降序 */
const resetQuery = () => {
  dateRange.value = ['', ''];
  queryFormRef.value?.reset();
  sortInfo.value = { sortBy: defaultSort.value.prop, descending: true };
  queryParams.value.orderByColumn = defaultSort.value.prop;
  queryParams.value.isAsc = defaultSort.value.order;
  queryParams.value.pageNum = 1;
  getList();
};
/** 多选框选中数据：t-table 的 select-change 只回传选中的 rowKey（rowKey=infoId），选中行从列表映射 */
const handleSelectionChange = (keys: Array<string | number>) => {
  ids.value = keys;
  const selection = loginInfoList.value.filter((item) => keys.includes(item.infoId));
  multiple.value = !selection.length;
  single.value = selection.length != 1;
  selectName.value = selection.map((item) => item.userName);
};
/** 排序触发事件：t-table 回传 { sortBy, descending }，映射回后端的 orderByColumn / isAsc */
const handleSortChange = (sort: any) => {
  sortInfo.value = sort;
  queryParams.value.orderByColumn = sort?.sortBy ?? defaultSort.value.prop;
  queryParams.value.isAsc = sort ? (sort.descending ? 'descending' : 'ascending') : defaultSort.value.order;
  getList();
};
/** 删除按钮操作 */
const handleDelete = async (row?: LoginInfoVO) => {
  const infoIds = row?.infoId || ids.value;
  await proxy?.$modal.confirm('是否确认删除访问编号为"' + infoIds + '"的数据项?');
  await delLoginInfo(infoIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};
/** 清空按钮操作 */
const handleClean = async () => {
  await proxy?.$modal.confirm('是否确认清空所有登录日志数据项?');
  await cleanLoginInfo();
  await getList();
  proxy?.$modal.msgSuccess('清空成功');
};
/** 解锁按钮操作 */
const handleUnlock = async () => {
  const username = selectName.value;
  await proxy?.$modal.confirm('是否确认解锁用户"' + username + '"数据项?');
  await unlockLoginInfo(username);
  proxy?.$modal.msgSuccess('用户' + username + '解锁成功');
};
/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'monitor/logininfor/export',
    {
      ...queryParams.value
    },
    `logininfor_${new Date().getTime()}.xlsx`
  );
};

onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
// 卡片头工具栏：原栅格行的 flex 布局收敛到本页 scoped 类
.logininfor-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
