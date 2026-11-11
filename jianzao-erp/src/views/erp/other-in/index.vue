<template>
  <div class="p-2">
    <t-row :gutter="20">
      <t-col :span="12">
        <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
          <div v-show="showSearch" class="erp-row-container">
            <t-card :bordered="false" class="erp-filter-card" title="筛选条件">
              <template #actions>
                <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
              </template>
              <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row" label-align="right" :label-width="'auto'" @submit="handleQuery">
                <t-form-item label="单号" name="keyword">
                  <t-input v-model="queryParams.keyword" placeholder="请输入其他入库单号" clearable class="query-w-md" @enter="handleQuery" />
                </t-form-item>
                <t-form-item label="入库原因" name="reason">
                  <t-select v-model="queryParams.reason" placeholder="全部原因" clearable class="query-w-sm" :options="reasonOptions" />
                </t-form-item>
                <t-form-item label="状态" name="status">
                  <t-select v-model="queryParams.status" placeholder="单据状态" clearable class="query-w-sm" :options="statusOptions" />
                </t-form-item>
                <div class="erp-filter-actions">
                  <t-button theme="primary" @click="handleQuery">搜索</t-button>
                  <t-button theme="default" variant="outline" @click="resetQuery">重置</t-button>
                </div>
              </t-form>
            </t-card>
          </div>
        </transition>

        <div v-if="!showSearch" class="mb-[10px] flex justify-end">
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>

        <div class="erp-surface erp-panel">
          <div class="erp-panel__head">
            <div class="flex items-center justify-between w-full">
              <t-button v-hasPermi="['erp:other-in:add']" theme="primary" variant="outline" @click="handleAdd">新增其他入库单</t-button>
            </div>
          </div>

          <t-table
            class="erp-table"
            row-key="id"
            :data="otherInList"
            :columns="listColumns"
            :loading="loading"
            :hover="true"
            :resizable="true"
            size="small"
            cell-empty-content="-"
            @column-resize-change="(ctx: any) => saveColumnWidths('other-in/list', ctx.columnsWidth)"
          >
            <template #bizDate="{ row }">{{ toDate(row.bizDate) }}</template>
            <template #reason="{ row }">{{ reasonLabel(row.reason) }}</template>
            <template #amount="{ row }">{{ amountText(row.amount) }}</template>
            <template #status="{ row }">
              <StatusTag kind="bill" :value="row.status" :status-text="row.statusText" />
            </template>
            <template #operation="{ row }">
              <div class="flex items-center justify-center gap-1">
                <t-tooltip v-if="checkPermi(['erp:other-in:query'])" content="查看" placement="top">
                  <t-button theme="primary" variant="text" shape="square" size="small" @click="handleView(row)">
                    <template #icon><BrowseIcon /></template>
                  </t-button>
                </t-tooltip>
                <t-tooltip v-if="row.status === 'DRAFT' && checkPermi(['erp:other-in:edit'])" content="编辑" placement="top">
                  <t-button theme="primary" variant="text" shape="square" size="small" @click="handleEdit(row)">
                    <template #icon><Edit1Icon /></template>
                  </t-button>
                </t-tooltip>
                <t-tooltip v-if="row.status === 'DRAFT' && checkPermi(['erp:other-in:audit'])" content="确认" placement="top">
                  <t-button theme="success" variant="text" shape="square" size="small" @click="handleConfirm(row)">
                    <template #icon><CheckCircleIcon /></template>
                  </t-button>
                </t-tooltip>
                <t-tooltip v-if="row.status === 'CONFIRMED' && checkPermi(['erp:other-in:withdraw'])" content="撤回" placement="top">
                  <t-button theme="warning" variant="text" shape="square" size="small" @click="handleWithdraw(row)">
                    <template #icon><RollbackIcon /></template>
                  </t-button>
                </t-tooltip>
                <t-tooltip v-if="row.status === 'CONFIRMED' && checkPermi(['erp:other-in:void'])" content="作废" placement="top">
                  <t-button theme="danger" variant="text" shape="square" size="small" @click="handleVoid(row)">
                    <template #icon><CloseCircleIcon /></template>
                  </t-button>
                </t-tooltip>
                <t-tooltip v-if="row.status === 'DRAFT' && checkPermi(['erp:other-in:remove'])" content="删除" placement="top">
                  <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
                    <template #icon><DeleteIcon /></template>
                  </t-button>
                </t-tooltip>
              </div>
            </template>
          </t-table>

          <pagination
            v-show="total > 0"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            :total="total"
            @pagination="getList"
          />
        </div>
      </t-col>
    </t-row>

    <!-- 新增 / 编辑 / 查看 其他入库单 -->
    <t-drawer v-model:visible="dialog.visible" :header="dialog.title" size="92%" :destroy-on-close="true" attach="body">
      <!-- 抽屉 body 自带纵向滚动，footer 固定；不再依赖无效的 body-style（普通 div 不识别） -->
      <div class="erp-drawer-body">
        <t-form ref="otherInFormRef" :data="form" :rules="rules" label-align="top" :disabled="readonly">
          <t-row :gutter="16">
            <t-col :span="3">
              <t-form-item label="业务日期" name="bizDate">
                <t-date-picker v-model="form.bizDate" value-type="YYYY-MM-DD" placeholder="请选择日期" style="width: 100%" />
              </t-form-item>
            </t-col>
            <t-col :span="3">
              <t-form-item label="入库原因" name="reason">
                <t-select v-model="form.reason" placeholder="请选择入库原因" style="width: 100%" :options="reasonOptions" />
              </t-form-item>
            </t-col>
            <t-col :span="6">
              <t-form-item label="备注">
                <t-input v-model="form.remark" placeholder="备注" />
              </t-form-item>
            </t-col>
          </t-row>
        </t-form>

        <div class="erp-detail-bar">
          <div class="flex items-center gap-2">
            <t-button v-if="!readonly" theme="primary" variant="outline" size="small" @click="addRow">
              <template #icon><PlusIcon /></template>
              添加明细
            </t-button>
            <t-tag v-if="readonly" theme="default" size="small">已确认/已作废单据不可修改</t-tag>
          </div>
        </div>

        <t-table
          class="erp-table"
          row-key="__key"
          :data="rows"
          :columns="rowColumns"
          :loading="detailLoading"
          :hover="true"
          :resizable="true"
          size="small"
          cell-empty-content="-"
          @column-resize-change="(ctx: any) => saveColumnWidths('other-in/detail', ctx.columnsWidth)"
        >
          <template #rowIndex="{ rowIndex }">{{ rowIndex + 1 }}</template>
          <template #product="{ row, rowIndex }">
            <ProductSelect
              v-model="row.productId"
              :disabled="readonly"
              show-category-filter
              placeholder="请选择产品（不限类型）"
              @change="(product) => onProductChange(rowIndex, product)"
            />
          </template>
          <template #qty="{ row, rowIndex }">
            <t-input-number
              :key="`qty-${rowIndex}-${qtyPrecision(row.qtyScale)}`"
              v-model="row.qty"
              :decimal-places="qtyPrecision(row.qtyScale)"
              :step="qtyStep(row.qtyScale)"
              :min="0"
              theme="normal"
              align="right"
              :disabled="readonly"
              style="width: 100%"
            />
          </template>
          <template #unitCost="{ row }">
            <t-input-number
              v-model="row.unitCost"
              :decimal-places="2"
              :min="0"
              theme="normal"
              align="right"
              :disabled="readonly"
              style="width: 100%"
            />
          </template>
          <template #amount="{ row }">{{ rowAmountText(row) }}</template>
          <template #remark="{ row }">
            <t-input v-model="row.remark" :title="row.remark" :disabled="readonly" placeholder="备注" />
          </template>
          <template #rowOperation="{ rowIndex }">
            <t-tooltip content="删除该行" placement="top">
              <t-button theme="danger" variant="text" shape="square" size="small" @click="removeRow(rowIndex)">
                <template #icon><DeleteIcon /></template>
              </t-button>
            </t-tooltip>
          </template>
        </t-table>
      </div>

      <template #footer>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4 text-sm">
            <span
              >明细行数：<b>{{ rows.length }}</b></span
            >
            <span
              >合计数量：<b>{{ formatQty(totalQty) }}</b></span
            >
            <span
              >金额合计：<b class="text-primary">{{ amountTotalText }}</b></span
            >
          </div>
          <div class="flex items-center gap-2">
            <t-button theme="default" variant="outline" @click="dialog.visible = false">{{ readonly ? '关 闭' : '取 消' }}</t-button>
            <t-button v-if="!readonly" theme="primary" variant="outline" @click="submitForm(false)">保存草稿</t-button>
            <t-button v-if="!readonly && checkPermi(['erp:other-in:audit'])" theme="success" @click="submitForm(true)">保存并确认</t-button>
          </div>
        </div>
      </template>
    </t-drawer>
  </div>
</template>

<script setup name="ErpOtherIn" lang="ts">
import { addOtherIn, confirmOtherIn, delOtherIn, getOtherIn, listOtherIn, updateOtherIn, voidOtherIn, withdrawOtherIn } from '@/api/erp/trade';
import { getStockBalance } from '@/api/erp/stock';
import { ERP_BILL_STATUS, ERP_OTHER_IN_REASON } from '@/api/erp/enums';
import type { ErpBillStatus, ErpOtherInReason } from '@/api/erp/enums';
import type { ErpAmount, ErpId, ErpOtherInVO, ErpProductVO } from '@/api/erp/types';
import { formatMoney, formatQty, normalizeQty, qtyPrecision, qtyStep, roundMoney } from '@/utils/erp/calc';
import { checkPermi } from '@/utils/permission';
// 列宽记忆：拖拽后按表名存 localStorage，进页面还原（见 hooks/useTableColumnWidth）
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import ProductSelect from '@/components/erp/ProductSelect.vue';
import StatusTag from '@/components/erp/StatusTag.vue';
import { BrowseIcon, CheckCircleIcon, CloseCircleIcon, DeleteIcon, Edit1Icon, PlusIcon, RollbackIcon } from 'tdesign-icons-vue-next';

interface OtherInRow {
  /** t-table 行 key（前端生成，不提交后端） */
  __key?: string;
  productId?: ErpId;
  productCode?: string;
  productName?: string;
  qty: number;
  /** 数量精度 0~3（商品级，DOC/26 §10） */
  qtyScale?: number;
  unitCost?: number;
  remark?: string;
}

interface OtherInFormState {
  id?: ErpId;
  bizDate: string;
  reason?: ErpOtherInReason;
  remark: string;
}

interface OtherInQueryState extends PageQuery {
  keyword: string;
  reason: string;
  status: string;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const statusOptions = Object.entries(ERP_BILL_STATUS).map(([value, item]) => ({ value: value as ErpBillStatus, label: item.label }));
const reasonOptions = Object.entries(ERP_OTHER_IN_REASON).map(([value, item]) => ({ value: value as ErpOtherInReason, label: item.label }));

const otherInList = ref<ErpOtherInVO[]>([]);
const loading = ref(true);
const detailLoading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const otherInFormRef = ref<any>();
const queryFormRef = ref<any>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

/** 只读（查看已确认/已作废单据） */
const readonly = ref(false);

const rows = ref<OtherInRow[]>([]);

/** 主列表列配置：t-table 用 columns 数组，插槽名与 cell 字符串同名（DOC/35 §5） */
const listColumns: any[] = [
  { colKey: 'no', title: '单号', align: 'center', minWidth: 130, ellipsis: true },
  { colKey: 'bizDate', title: '业务日期', align: 'center', width: 110, cell: 'bizDate' },
  { colKey: 'reason', title: '入库原因', align: 'center', width: 110, cell: 'reason' },
  { colKey: 'amount', title: '入库金额', align: 'right', width: 130, cell: 'amount' },
  { colKey: 'status', title: '状态', align: 'center', width: 90, cell: 'status' },
  { colKey: 'remark', title: '备注', align: 'center', minWidth: 140, ellipsis: true },
  { colKey: 'operation', title: '操作', align: 'center', width: 160, cell: 'operation' }
];
mergeSavedWidths('other-in/list', listColumns);

/** 明细行列表配置：数量/单价/备注是行内编辑控件；只读时不出现操作列 */
const rowColumns = computed<any[]>(() => {
  const cols: any[] = [
    { colKey: 'rowIndex', title: '#', width: 50, align: 'center', cell: 'rowIndex' },
    { colKey: 'productId', title: '产品', minWidth: 260, cell: 'product' },
    { colKey: 'productName', title: '产品名称', align: 'center', minWidth: 150, ellipsis: true },
    { colKey: 'qty', title: '数量', width: 140, align: 'right', cell: 'qty' },
    { colKey: 'unitCost', title: '入库单价', width: 150, align: 'right', cell: 'unitCost' },
    { colKey: 'amount', title: '金额', width: 120, align: 'right', cell: 'amount' },
    { colKey: 'remark', title: '备注', minWidth: 150, cell: 'remark' }
  ];
  if (!readonly.value) {
    cols.push({ colKey: 'operation', title: '操作', width: 70, align: 'center', cell: 'rowOperation' });
  }
  return mergeSavedWidths('other-in/detail', cols);
});

const createForm = (): OtherInFormState => ({
  id: undefined,
  bizDate: new Date().toISOString().slice(0, 10),
  reason: undefined,
  remark: ''
});

const form = reactive<OtherInFormState>(createForm());

const queryParams = reactive<OtherInQueryState>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  reason: '',
  status: ''
});

// t-form 的 rules 形状与 EP 的 ElFormRules 不兼容（本页仅用于必填星号，提交前另有 validateRows 手工校验）
const rules: Record<string, any> = {
  bizDate: [{ required: true, message: '业务日期不能为空', trigger: 'change' }],
  reason: [{ required: true, message: '入库原因不能为空', trigger: 'change' }]
};

const num = (value: ErpAmount | undefined | null): number => {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
};

/** 日期只取 yyyy-MM-dd */
const toDate = (value: string | undefined): string => (value ? String(value).slice(0, 10) : '');

const reasonLabel = (value: string | undefined): string => {
  if (!value) return '-';
  return ERP_OTHER_IN_REASON[value as ErpOtherInReason]?.label ?? value;
};

const totalQty = computed(() => roundMoney(rows.value.reduce((sum, row) => sum + (row.qty || 0), 0)));

const rowAmount = (row: OtherInRow): number => roundMoney((row.qty || 0) * (row.unitCost || 0));

const rowAmountText = (row: OtherInRow): string => {
  if (row.unitCost === undefined || row.unitCost === null) return '-';
  return formatMoney(rowAmount(row));
};

const amountTotal = computed(() => roundMoney(rows.value.reduce((sum, row) => sum + (row.unitCost === undefined ? 0 : rowAmount(row)), 0)));

const amountText = (value: number | undefined | null): string => (value === undefined || value === null ? '-' : formatMoney(value));

const amountTotalText = computed(() => (amountTotal.value > 0 ? formatMoney(amountTotal.value) : '-'));

/** 查询其他入库单列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res = await listOtherIn(queryParams);
    otherInList.value = res.rows ?? [];
    total.value = res.total ?? 0;
  } finally {
    loading.value = false;
  }
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  queryParams.pageNum = 1;
  queryParams.reason = '';
  queryParams.status = '';
  handleQuery();
};

/** 明细行 t-table 行 key：新增行没有业务主键，用前端自增序号（提交前 buildPayload 只取业务字段） */
let rowSeq = 0;
const nextRowKey = (): string => `row-${++rowSeq}`;

const createRow = (): OtherInRow => ({
  __key: nextRowKey(),
  productId: undefined,
  qty: 1,
  unitCost: undefined,
  remark: ''
});

/** 表单重置 */
const resetForm = async () => {
  Object.assign(form, createForm());
  rows.value = [createRow()];
  readonly.value = false;
  await nextTick();
  otherInFormRef.value?.clearValidate();
};

/** 新增按钮操作 */
const handleAdd = async () => {
  await resetForm();
  dialog.visible = true;
  dialog.title = '新增其他入库单';
};

const loadDetail = async (id: ErpId, isReadonly: boolean) => {
  detailLoading.value = true;
  try {
    const res = await getOtherIn(id);
    const detail = res.data;
    Object.assign(form, {
      id: detail.id,
      bizDate: toDate(detail.bizDate),
      reason: detail.reason,
      remark: detail.remark || ''
    });
    rows.value = (detail.items ?? []).map((item) => ({
      __key: nextRowKey(),
      productId: item.productId,
      productCode: item.productCode,
      productName: item.productName,
      qty: num(item.qty),
      qtyScale: item.qtyScale,
      unitCost: item.unitCost === undefined || item.unitCost === null ? undefined : num(item.unitCost),
      remark: item.remark
    }));
    readonly.value = isReadonly;
    await nextTick();
    otherInFormRef.value?.clearValidate();
  } finally {
    detailLoading.value = false;
  }
};

/** 编辑按钮操作 */
const handleEdit = async (row: ErpOtherInVO) => {
  await resetForm();
  dialog.visible = true;
  dialog.title = `编辑其他入库单 ${row.no ?? ''}`;
  await loadDetail(row.id, false);
};

/** 查看按钮操作 */
const handleView = async (row: ErpOtherInVO) => {
  await resetForm();
  dialog.visible = true;
  dialog.title = `查看其他入库单 ${row.no ?? ''}`;
  await loadDetail(row.id, true);
};

/** 添加明细行 */
const addRow = () => {
  rows.value.push(createRow());
};

/** 删除明细行 */
const removeRow = (index: number) => {
  rows.value.splice(index, 1);
  if (rows.value.length === 0) {
    rows.value.push(createRow());
  }
};

/** 选择产品后带出名称与当前加权成本作为默认入库单价 */
const onProductChange = async (index: number, product?: ErpProductVO) => {
  const row = rows.value[index];
  if (!row) return;
  if (!product) {
    row.productCode = undefined;
    row.productName = undefined;
    row.qtyScale = undefined;
    return;
  }
  row.productCode = product.code;
  row.productName = product.name;
  row.qtyScale = product.qtyScale;
  // 精度随商品切换时把已录数量归一化，避免小数商品的数量挂到整数商品行上（数值控件只改显示不回写 model）
  row.qty = normalizeQty(row.qty, product.qtyScale);
  try {
    const res = await getStockBalance(product.id);
    const avg = res.data?.avgCost;
    row.unitCost = avg === undefined || avg === null || num(avg) <= 0 ? 0 : num(avg);
  } catch {
    row.unitCost = 0;
  }
};

/** 组装提交数据 */
const buildPayload = (confirm: boolean) => ({
  id: form.id,
  bizDate: form.bizDate,
  reason: form.reason,
  remark: form.remark,
  confirm,
  items: rows.value.map((row) => ({
    productId: row.productId,
    qty: row.qty,
    unitCost: row.unitCost ?? 0,
    remark: row.remark
  }))
});

/** 提交前校验 */
const validateRows = (): boolean => {
  if (!form.bizDate) {
    proxy?.$modal.msgWarning('请选择业务日期');
    return false;
  }
  if (!form.reason) {
    proxy?.$modal.msgWarning('请选择入库原因');
    return false;
  }
  if (rows.value.length === 0) {
    proxy?.$modal.msgWarning('请至少添加一行明细');
    return false;
  }
  const seen = new Set<ErpId>();
  for (const row of rows.value) {
    if (!row.productId) {
      proxy?.$modal.msgWarning('明细中存在未选择产品的行');
      return false;
    }
    if (seen.has(row.productId)) {
      proxy?.$modal.msgWarning('明细中产品重复，请合并为一行');
      return false;
    }
    seen.add(row.productId);
    if (!(row.qty > 0)) {
      proxy?.$modal.msgWarning('数量必须大于 0');
      return false;
    }
    if (!(row.unitCost !== undefined && row.unitCost > 0)) {
      proxy?.$modal.msgWarning('入库单价必须大于 0（选择产品后自动带出，可修改）');
      return false;
    }
  }
  return true;
};

/** 提交按钮 */
const submitForm = async (confirm: boolean) => {
  if (!validateRows()) return;
  const payload = buildPayload(confirm);
  if (form.id) {
    await updateOtherIn(payload);
  } else {
    await addOtherIn(payload);
  }
  proxy?.$modal.msgSuccess(confirm ? '已保存并确认' : '已保存草稿');
  dialog.visible = false;
  await getList();
};

/** 确认按钮操作 */
const handleConfirm = async (row: ErpOtherInVO) => {
  try {
    await proxy?.$modal.confirm(`是否确认其他入库单「${row.no ?? ''}」？确认后按录入单价增加库存并更新加权成本。`);
  } catch {
    return;
  }
  await confirmOtherIn(row.id);
  proxy?.$modal.msgSuccess('已确认');
  await getList();
};

/** 撤回按钮操作 */
const handleWithdraw = async (row: ErpOtherInVO) => {
  try {
    await proxy?.$modal.confirm(`是否撤回其他入库单「${row.no ?? ''}」？撤回后回到草稿可继续修改。`);
  } catch {
    return;
  }
  await withdrawOtherIn(row.id);
  proxy?.$modal.msgSuccess('已撤回');
  await getList();
};

/** 作废按钮操作 */
const handleVoid = async (row: ErpOtherInVO) => {
  try {
    await proxy?.$modal.confirm(`是否作废其他入库单「${row.no ?? ''}」？作废为终态，不可再修改。`);
  } catch {
    return;
  }
  await voidOtherIn(row.id);
  proxy?.$modal.msgSuccess('已作废');
  await getList();
};

/** 删除按钮操作：仅草稿单据可删除，后端逻辑删除，单号不回收 */
const handleDelete = async (row: ErpOtherInVO) => {
  try {
    await proxy?.$modal.confirm(`是否删除其他入库单「${row.no ?? ''}」？删除后不可恢复，单号不会回收重用。`);
  } catch {
    return;
  }
  await delOtherIn(row.id);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

onMounted(() => {
  getList();
});
</script>
