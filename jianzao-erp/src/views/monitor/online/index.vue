<template>
  <div class="p-2">
    <div class="mb-[10px]">
      <t-card :bordered="false" class="erp-filter-card" title="筛选条件">
        <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row" label-align="right" :label-width="'auto'" @submit="handleQuery">
          <t-form-item label="登录地址" name="ipaddr">
            <t-input v-model="queryParams.ipaddr" placeholder="请输入登录地址" clearable class="query-w-md" @enter="handleQuery" />
          </t-form-item>
          <t-form-item label="用户名称" name="userName">
            <t-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable class="query-w-md" @enter="handleQuery" />
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
    <div class="erp-surface erp-panel">
      <t-table
        class="erp-table"
        row-key="tokenId"
        :data="onlineList.slice((queryParams.pageNum - 1) * queryParams.pageSize, queryParams.pageNum * queryParams.pageSize)"
        :columns="listColumns"
        :loading="loading"
        :hover="true"
        :resizable="true"
        size="small"
        cell-empty-content="-"
      >
        <template #rowIndex="{ rowIndex }">{{ (queryParams.pageNum - 1) * queryParams.pageSize + rowIndex + 1 }}</template>
        <template #deviceType="{ row }">
          <dict-tag :options="sys_device_type" :value="row.deviceType" />
        </template>
        <template #loginTime="{ row }">
          <span>{{ proxy.parseTime(row.loginTime) }}</span>
        </template>
        <template #operation="{ row }">
          <!-- 权限判断改挂外层：v-hasPermi 在 mounted 删节点会与 t-tooltip 的包裹结构打架 -->
          <t-tooltip v-if="checkPermi(['monitor:online:forceLogout'])" content="强退" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleForceLogout(row)">
              <template #icon><DeleteIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </t-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" />
    </div>
  </div>
</template>

<script setup name="Online" lang="ts">
import { forceLogout, list as initData } from '@/api/monitor/online';
import { OnlineQuery, OnlineVO } from '@/api/monitor/online/types';
import api from '@/api/system/user';
import { to } from 'await-to-js';
import { checkPermi } from '@/utils/permission';
import { DeleteIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_device_type } = toRefs<any>(proxy?.useDict('sys_device_type'));

const onlineList = ref<OnlineVO[]>([]);
const loading = ref(true);
const total = ref(0);

const queryFormRef = ref<any>();

const queryParams = ref<OnlineQuery>({
  pageNum: 1,
  pageSize: 10,
  ipaddr: '',
  userName: ''
});

/** 在线用户列表列配置：t-table 用 columns 数组，模板列走同名 cell 插槽 */
const listColumns: any[] = [
  { colKey: 'rowIndex', title: '序号', width: 50, align: 'center', cell: 'rowIndex' },
  { colKey: 'tokenId', title: '会话编号', align: 'center', ellipsis: true },
  { colKey: 'userName', title: '登录名称', align: 'center', ellipsis: true },
  { colKey: 'clientKey', title: '客户端', align: 'center', ellipsis: true },
  { colKey: 'deviceType', title: '设备类型', align: 'center', cell: 'deviceType' },
  { colKey: 'deptName', title: '所属部门', align: 'center', ellipsis: true },
  { colKey: 'ipaddr', title: '主机', align: 'center', ellipsis: true },
  { colKey: 'loginLocation', title: '登录地点', align: 'center', ellipsis: true },
  { colKey: 'os', title: '操作系统', align: 'center', ellipsis: true },
  { colKey: 'browser', title: '浏览器', align: 'center', ellipsis: true },
  { colKey: 'loginTime', title: '登录时间', align: 'center', width: 180, cell: 'loginTime' },
  { colKey: 'operation', title: '操作', align: 'center', width: 80, className: 'small-padding fixed-width', cell: 'operation' }
];

/** 查询登录日志列表 */
const getList = async () => {
  loading.value = true;
  const res = await initData(queryParams.value);
  onlineList.value = res.rows;
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
/** 强退按钮操作 */
const handleForceLogout = async (row: OnlineVO) => {
  const [err] = await to(proxy?.$modal.confirm('是否确认强退名称为"' + row.userName + '"的用户?') as any);
  if (!err) {
    await forceLogout(row.tokenId);
    await getList();
    proxy?.$modal.msgSuccess('删除成功');
  }
};

onMounted(() => {
  getList();
});
</script>
