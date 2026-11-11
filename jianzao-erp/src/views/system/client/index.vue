<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="search">
        <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row" label-width="85px">
          <t-form-item label="客户端key" name="clientKey">
            <t-input v-model="queryParams.clientKey" placeholder="请输入客户端key" clearable @enter="handleQuery" />
          </t-form-item>
          <t-form-item label="客户端秘钥" name="clientSecret">
            <t-input v-model="queryParams.clientSecret" placeholder="请输入客户端秘钥" clearable @enter="handleQuery" />
          </t-form-item>
          <t-form-item label="状态" name="status">
            <t-select v-model="queryParams.status" placeholder="状态" clearable :options="sys_normal_disable" />
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
      </div>
    </transition>

    <t-card>
      <template #header>
        <div class="client-toolbar">
          <t-button v-hasPermi="['system:client:add']" theme="primary" variant="outline" @click="handleAdd">
            <template #icon><PlusIcon /></template>
            新增
          </t-button>
          <t-button v-hasPermi="['system:client:edit']" theme="success" variant="outline" :disabled="single" @click="handleUpdate()">
            <template #icon><Edit1Icon /></template>
            修改
          </t-button>
          <t-button v-hasPermi="['system:client:remove']" theme="danger" variant="outline" :disabled="multiple" @click="handleDelete()">
            <template #icon><DeleteIcon /></template>
            删除
          </t-button>
          <t-button v-hasPermi="['system:client:export']" theme="warning" variant="outline" @click="handleExport">
            <template #icon><DownloadIcon /></template>
            导出
          </t-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>

      <t-table
        class="erp-table"
        row-key="id"
        :data="clientList"
        :columns="clientColumns"
        :loading="loading"
        bordered
        size="small"
        cell-empty-content="-"
        :resizable="true"
        :selected-row-keys="ids"
        @select-change="handleSelectionChange"
        @column-resize-change="(ctx: any) => saveColumnWidths('system/client/list', ctx.columnsWidth)"
      >
        <template #grantTypeList="{ row }">
          <dict-tag :options="sys_grant_type" :value="row.grantTypeList" />
        </template>
        <template #deviceType="{ row }">
          <dict-tag :options="sys_device_type" :value="row.deviceType" />
        </template>
        <template #status="{ row }">
          <t-switch v-model="row.status" :custom-value="['0', '1']" @change="handleStatusChange(row)"></t-switch>
        </template>
        <template #operation="{ row }">
          <!-- 权限判断挂外层 t-tooltip：v-hasPermi 在 mounted 里 removeChild，与 tooltip 包裹结构打架（DOC/36 P1） -->
          <t-tooltip v-if="checkPermi(['system:client:edit'])" content="修改" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
              <template #icon><Edit1Icon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['system:client:remove'])" content="删除" placement="top">
            <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
              <template #icon><DeleteIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </t-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </t-card>
    <!-- 添加或修改客户端管理对话框 -->
    <t-dialog v-model:visible="dialog.visible" :header="dialog.title" width="500px" attach="body">
      <t-form ref="clientFormRef" :data="form" :rules="rules" label-width="100px">
        <t-form-item label="客户端key" name="clientKey">
          <t-input v-model="form.clientKey" :disabled="form.id != null" placeholder="请输入客户端key" />
        </t-form-item>
        <t-form-item label="客户端秘钥" name="clientSecret">
          <t-input v-model="form.clientSecret" :disabled="form.id != null" placeholder="请输入客户端秘钥" />
        </t-form-item>
        <t-form-item label="授权类型" name="grantTypeList">
          <t-select v-model="form.grantTypeList" multiple placeholder="请输入授权类型" :options="sys_grant_type" />
        </t-form-item>
        <t-form-item label="设备类型" name="deviceType">
          <t-select v-model="form.deviceType" placeholder="请输入设备类型" :options="sys_device_type" />
        </t-form-item>
        <t-form-item name="activeTimeout" label-width="auto">
          <template #label>
            <span>
              <t-tooltip content="指定时间无操作则过期（单位：秒），默认30分钟（1800秒）" placement="top">
                <HelpCircleIcon />
              </t-tooltip>
              Token活跃超时时间
            </span>
          </template>
          <t-input v-model="form.activeTimeout" placeholder="请输入Token活跃超时时间" />
        </t-form-item>
        <t-form-item name="timeout" label-width="auto">
          <template #label>
            <span>
              <t-tooltip content="指定时间必定过期（单位：秒），默认七天（604800秒）" placement="top">
                <HelpCircleIcon />
              </t-tooltip>
              Token固定超时时间
            </span>
          </template>
          <t-input v-model="form.timeout" placeholder="请输入Token固定超时时间" />
        </t-form-item>
        <t-form-item label="状态" name="status">
          <t-radio-group v-model="form.status">
            <t-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
              {{ dict.label }}
            </t-radio>
          </t-radio-group>
        </t-form-item>
      </t-form>
      <template #footer>
        <div class="dialog-footer">
          <t-button theme="primary" :loading="buttonLoading" @click="submitForm">确 定</t-button>
          <t-button theme="default" variant="outline" @click="cancel">取 消</t-button>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<script setup name="Client" lang="ts">
import { listClient, getClient, delClient, addClient, updateClient, changeStatus } from '@/api/system/client';
import { ClientVO, ClientQuery, ClientForm } from '@/api/system/client/types';
import { checkPermi } from '@/utils/permission';
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import { DeleteIcon, DownloadIcon, Edit1Icon, HelpCircleIcon, PlusIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_normal_disable'));
const { sys_grant_type } = toRefs<any>(proxy?.useDict('sys_grant_type'));
const { sys_device_type } = toRefs<any>(proxy?.useDict('sys_device_type'));

const clientList = ref<ClientVO[]>([]);
const buttonLoading = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

/** t-form 实例提供 reset()/clearValidate()/validate()，与 EP 表单实例契约不同 */
const queryFormRef = ref<any>();
const clientFormRef = ref<any>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: ClientForm = {
  id: undefined,
  clientId: undefined,
  clientKey: undefined,
  clientSecret: undefined,
  grantTypeList: undefined,
  deviceType: undefined,
  activeTimeout: undefined,
  timeout: undefined,
  status: undefined
};
const data = reactive<{ form: ClientForm; queryParams: ClientQuery }>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    clientId: undefined,
    clientKey: undefined,
    clientSecret: undefined,
    grantType: undefined,
    deviceType: undefined,
    activeTimeout: undefined,
    timeout: undefined,
    status: undefined
  }
});

const { queryParams, form } = toRefs<{ form: ClientForm; queryParams: ClientQuery }>(data);

/** 表单校验规则：t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，单独用 Record 声明（DOC/35 §5） */
const rules: Record<string, any> = {
  id: [{ required: true, message: 'id不能为空', trigger: 'blur' }],
  clientId: [{ required: true, message: '客户端id不能为空', trigger: 'blur' }],
  clientKey: [{ required: true, message: '客户端key不能为空', trigger: 'blur' }],
  clientSecret: [{ required: true, message: '客户端秘钥不能为空', trigger: 'blur' }],
  grantTypeList: [{ required: true, message: '授权类型不能为空', trigger: 'change' }],
  deviceType: [{ required: true, message: '设备类型不能为空', trigger: 'change' }]
};

/** 客户端列表列配置：t-table 用 columns 数组，cell 字符串与 #插槽同名（DOC/35 §5） */
const clientColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 55, align: 'center' },
  { colKey: 'id', title: 'id', align: 'center' },
  { colKey: 'clientId', title: '客户端id', align: 'center' },
  { colKey: 'clientKey', title: '客户端key', align: 'center', ellipsis: true },
  { colKey: 'clientSecret', title: '客户端秘钥', align: 'center', ellipsis: true },
  { colKey: 'grantTypeList', title: '授权类型', align: 'center', cell: 'grantTypeList' },
  { colKey: 'deviceType', title: '设备类型', align: 'center', cell: 'deviceType' },
  { colKey: 'activeTimeout', title: 'Token活跃超时时间', align: 'center' },
  { colKey: 'timeout', title: 'Token固定超时时间', align: 'center' },
  { colKey: 'status', title: '状态', align: 'center', cell: 'status' },
  { colKey: 'operation', title: '操作', align: 'center', width: 120, cell: 'operation' }
];
mergeSavedWidths('system/client/list', clientColumns);

/** 查询客户端管理列表 */
const getList = async () => {
  loading.value = true;
  const res = await listClient(queryParams.value);
  clientList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

/** 取消按钮 */
const cancel = () => {
  reset();
  dialog.visible = false;
};

/** 表单重置：t-form 只清校验态，表单值以 initFormData 为准 */
const reset = () => {
  form.value = { ...initFormData };
  clientFormRef.value?.clearValidate();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  queryParams.value.clientKey = undefined;
  queryParams.value.clientSecret = undefined;
  queryParams.value.status = undefined;
  handleQuery();
};

/** 多选框选中数据：t-table 的 select-change 只回传选中的 rowKey（rowKey=id） */
const handleSelectionChange = (keys: Array<string | number>) => {
  ids.value = keys;
  single.value = keys.length != 1;
  multiple.value = !keys.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加客户端管理';
};

/** 修改按钮操作 */
const handleUpdate = async (row?: ClientVO) => {
  reset();
  const _id = row?.id || ids.value[0];
  const res = await getClient(_id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改客户端管理';
};

/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async () => {
  const result = await clientFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  buttonLoading.value = true;
  if (form.value.id) {
    await updateClient(form.value).finally(() => (buttonLoading.value = false));
  } else {
    await addClient(form.value).finally(() => (buttonLoading.value = false));
  }
  proxy?.$modal.msgSuccess('修改成功');
  dialog.visible = false;
  await getList();
};

/** 删除按钮操作 */
const handleDelete = async (row?: ClientVO) => {
  const _ids = row?.id || ids.value;
  await proxy?.$modal.confirm('是否确认删除客户端管理编号为"' + _ids + '"的数据项？').finally(() => (loading.value = false));
  await delClient(_ids);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'system/client/export',
    {
      ...queryParams.value
    },
    `client_${new Date().getTime()}.xlsx`
  );
};

/** 状态修改  */
const handleStatusChange = async (row: ClientVO) => {
  const text = row.status === '0' ? '启用' : '停用';
  try {
    await proxy?.$modal.confirm('确认要"' + text + '"吗?');
    await changeStatus(row.clientId, row.status);
    proxy?.$modal.msgSuccess(text + '成功');
  } catch (err) {
    row.status = row.status === '0' ? '1' : '0';
  }
};

onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.client-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
