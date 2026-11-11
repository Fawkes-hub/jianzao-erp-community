<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card>
          <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row">
            <t-form-item label="参数名称" name="configName">
              <t-input v-model="queryParams.configName" placeholder="请输入参数名称" clearable @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="参数键名" name="configKey">
              <t-input v-model="queryParams.configKey" placeholder="请输入参数键名" clearable @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="系统内置" name="configType">
              <t-select v-model="queryParams.configType" placeholder="系统内置" clearable :options="sys_yes_no" />
            </t-form-item>
            <t-form-item label="创建时间" name="createTime">
              <!-- 值类型保持 YYYY-MM-DD HH:mm:ss 字符串，addDateRange 写入的 beginTime/endTime 请求字段格式不变 -->
              <t-date-range-picker
                v-model="dateRange"
                value-type="YYYY-MM-DD HH:mm:ss"
                :placeholder="['开始日期', '结束日期']"
                style="width: 308px"
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
    <t-card>
      <template #header>
        <div class="config-toolbar">
          <t-button v-hasPermi="['system:config:add']" theme="primary" variant="outline" @click="handleAdd">
            <template #icon><PlusIcon /></template>
            新增
          </t-button>
          <t-button v-hasPermi="['system:config:edit']" theme="success" variant="outline" :disabled="single" @click="handleUpdate()">
            <template #icon><Edit1Icon /></template>
            修改
          </t-button>
          <t-button v-hasPermi="['system:config:remove']" theme="danger" variant="outline" :disabled="multiple" @click="handleDelete()">
            <template #icon><DeleteIcon /></template>
            删除
          </t-button>
          <t-button v-hasPermi="['system:config:export']" theme="warning" variant="outline" @click="handleExport">
            <template #icon><DownloadIcon /></template>
            导出
          </t-button>
          <t-button v-hasPermi="['system:config:remove']" theme="danger" variant="outline" @click="handleRefreshCache">
            <template #icon><RefreshIcon /></template>
            刷新缓存
          </t-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>

      <t-table
        class="erp-table"
        row-key="configId"
        :data="configList"
        :columns="configColumns"
        :loading="loading"
        bordered
        size="small"
        cell-empty-content="-"
        :resizable="true"
        :selected-row-keys="ids"
        @select-change="handleSelectionChange"
        @column-resize-change="(ctx: any) => saveColumnWidths('system/config/list', ctx.columnsWidth)"
      >
        <template #configType="{ row }">
          <dict-tag :options="sys_yes_no" :value="row.configType" />
        </template>
        <template #createTime="{ row }">{{ proxy.parseTime(row.createTime) }}</template>
        <template #operation="{ row }">
          <!-- 权限判断挂外层 t-tooltip：v-hasPermi 在 mounted 里 removeChild，与 tooltip 包裹结构打架（DOC/36 P1） -->
          <t-tooltip v-if="checkPermi(['system:config:edit'])" content="修改" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
              <template #icon><Edit1Icon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['system:config:remove'])" content="删除" placement="top">
            <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
              <template #icon><DeleteIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </t-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </t-card>

    <!-- 添加或修改参数配置对话框 -->
    <t-dialog v-model:visible="dialog.visible" :header="dialog.title" width="500px" attach="body">
      <t-form ref="configFormRef" :data="form" :rules="rules" label-width="80px">
        <t-form-item label="参数名称" name="configName">
          <t-input v-model="form.configName" placeholder="请输入参数名称" />
        </t-form-item>
        <t-form-item label="参数键名" name="configKey">
          <t-input v-model="form.configKey" placeholder="请输入参数键名" />
        </t-form-item>
        <t-form-item label="参数键值" name="configValue">
          <t-textarea v-model="form.configValue" placeholder="请输入参数键值" />
        </t-form-item>
        <t-form-item label="系统内置" name="configType">
          <t-radio-group v-model="form.configType">
            <t-radio v-for="dict in sys_yes_no" :key="dict.value" :value="dict.value">{{ dict.label }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="备注" name="remark">
          <t-textarea v-model="form.remark" placeholder="请输入内容" />
        </t-form-item>
      </t-form>
      <template #footer>
        <div class="dialog-footer">
          <t-button theme="primary" @click="submitForm">确 定</t-button>
          <t-button theme="default" variant="outline" @click="cancel">取 消</t-button>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<script setup name="Config" lang="ts">
import { listConfig, getConfig, delConfig, addConfig, updateConfig, refreshCache } from '@/api/system/config';
import { ConfigForm, ConfigQuery, ConfigVO } from '@/api/system/config/types';
import { checkPermi } from '@/utils/permission';
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import { DeleteIcon, DownloadIcon, Edit1Icon, PlusIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_yes_no } = toRefs<any>(proxy?.useDict('sys_yes_no'));

const configList = ref<ConfigVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
/** 创建时间区间：t-date-range-picker 输出 YYYY-MM-DD HH:mm:ss 字符串数组，addDateRange 读取 [0]/[1] 不变 */
const dateRange = ref<[string, string]>(['', '']);

/** t-form 实例提供 reset()/clearValidate()/validate()，与 EP 表单实例契约不同 */
const queryFormRef = ref<any>();
const configFormRef = ref<any>();
const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});
const initFormData: ConfigForm = {
  configId: undefined,
  configName: '',
  configKey: '',
  configValue: '',
  configType: 'Y',
  remark: ''
};
const data = reactive<{ form: ConfigForm; queryParams: ConfigQuery }>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    configName: '',
    configKey: '',
    configType: ''
  }
});

const { queryParams, form } = toRefs<{ form: ConfigForm; queryParams: ConfigQuery }>(data);

/** 表单校验规则：t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，单独用 Record 声明（DOC/35 §5） */
const rules: Record<string, any> = {
  configName: [{ required: true, message: '参数名称不能为空', trigger: 'blur' }],
  configKey: [{ required: true, message: '参数键名不能为空', trigger: 'blur' }],
  configValue: [{ required: true, message: '参数键值不能为空', trigger: 'blur' }]
};

/** 参数列表列配置：t-table 用 columns 数组，cell 字符串与 #插槽同名（DOC/35 §5） */
const configColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 55, align: 'center' },
  { colKey: 'configName', title: '参数名称', align: 'center', ellipsis: true },
  { colKey: 'configKey', title: '参数键名', align: 'center', ellipsis: true },
  { colKey: 'configValue', title: '参数键值', align: 'center', ellipsis: true },
  { colKey: 'configType', title: '系统内置', align: 'center', cell: 'configType' },
  { colKey: 'remark', title: '备注', align: 'center', ellipsis: true },
  { colKey: 'createTime', title: '创建时间', align: 'center', width: 180, cell: 'createTime' },
  { colKey: 'operation', title: '操作', align: 'center', width: 150, cell: 'operation' }
];
mergeSavedWidths('system/config/list', configColumns);

/** 查询参数列表 */
const getList = async () => {
  loading.value = true;
  const res = await listConfig(proxy?.addDateRange(queryParams.value, dateRange.value));
  configList.value = res.rows;
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
  configFormRef.value?.clearValidate();
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
/** 重置按钮操作 */
const resetQuery = () => {
  dateRange.value = ['', ''];
  queryFormRef.value?.reset();
  queryParams.value.configName = '';
  queryParams.value.configKey = '';
  queryParams.value.configType = '';
  handleQuery();
};
/** 多选框选中数据：t-table 的 select-change 只回传选中的 rowKey（rowKey=configId） */
const handleSelectionChange = (keys: Array<string | number>) => {
  ids.value = keys;
  single.value = keys.length != 1;
  multiple.value = !keys.length;
};
/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加参数';
};
/** 修改按钮操作 */
const handleUpdate = async (row?: ConfigVO) => {
  reset();
  const configId = row?.configId || ids.value[0];
  const res = await getConfig(configId);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改参数';
};
/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async () => {
  const result = await configFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  form.value.configId ? await updateConfig(form.value) : await addConfig(form.value);
  proxy?.$modal.msgSuccess('操作成功');
  dialog.visible = false;
  await getList();
};
/** 删除按钮操作 */
const handleDelete = async (row?: ConfigVO) => {
  const configIds = row?.configId || ids.value;
  await proxy?.$modal.confirm('是否确认删除参数编号为"' + configIds + '"的数据项？');
  await delConfig(configIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};
/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'system/config/export',
    {
      ...queryParams.value
    },
    `config_${new Date().getTime()}.xlsx`
  );
};
/** 刷新缓存按钮操作 */
const handleRefreshCache = async () => {
  await refreshCache();
  proxy?.$modal.msgSuccess('刷新缓存成功');
};

onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.config-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
