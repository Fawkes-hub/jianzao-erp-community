<script setup lang="ts">
/**
 * 商品选择器（弹窗式）
 *
 * 触发器是一个只读输入框，点击弹出商品选择窗：关键词搜索 + 分类筛选 + 明细表格。
 * 相比原先的 el-select 下拉，能同时呈现编码/名称/规格/单位/分类/数量精度/库存/参考价，
 * 避免「只能看到一行标签、选错商品」。
 *
 * 对外契约与旧的下拉版保持一致（v-model + change(product)），因此开单页无需改动：
 * - modelValue：商品 ID
 * - change：选中/清空后回传完整 ErpProductVO（qtyScale、purchasePrice、costPrice 等供开单页使用）
 */
import { computed, ref, watch } from 'vue';
import { getProduct, productCategoryOptions, productOptions } from '@/api/erp/master';
import { listStockBalances } from '@/api/erp/stock';
import { ERP_PRODUCT_TYPE, ERP_QTY_SCALE_OPTIONS } from '@/api/erp/enums';
import type { ErpId, ErpProductCategoryVO, ErpProductType, ErpProductVO } from '@/api/erp/types';
import { formatMoney, formatQtyTrim } from '@/utils/erp/calc';
import { checkPermi } from '@/utils/permission';
import useDialog from '@/hooks/useDialog';
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import StatusTag from '@/components/erp/StatusTag.vue';
import { CloseCircleFilledIcon, SearchIcon } from 'tdesign-icons-vue-next';

const props = withDefaults(
  defineProps<{
    /** 单选为商品 ID；多选（multiple）为 ID 数组 */
    modelValue?: ErpId | ErpId[] | null;
    /** 限定产品类型，PRODUCT=成品，MATERIAL=原料；不传=不限（弹窗内提供类型切换） */
    type?: ErpProductType;
    placeholder?: string;
    disabled?: boolean;
    /** 允许清空 */
    clearable?: boolean;
    /** 多选：弹窗显示勾选列，确认后 modelValue 为 ID 数组；多选模式不分页，表格内滚动 */
    multiple?: boolean;
    /** 弹窗展示库存列（无 erp:stock:list 权限时自动隐藏） */
    showStock?: boolean;
    /** 弹窗展示分类筛选 */
    showCategoryFilter?: boolean;
    /** 弹窗标题 */
    dialogTitle?: string;
  }>(),
  {
    modelValue: undefined,
    type: undefined,
    placeholder: '请选择产品',
    disabled: false,
    clearable: true,
    multiple: false,
    showStock: true,
    showCategoryFilter: false,
    dialogTitle: '选择商品'
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: ErpId | ErpId[] | undefined): void;
  /** 单选：选中/清空时回传商品对象（multiple 模式下不触发） */
  (e: 'change', product: ErpProductVO | undefined): void;
  /** 多选：确认/清空时回传商品数组，仅 multiple 模式触发 */
  (e: 'change-multiple', products: ErpProductVO[]): void;
}>();

const productDialog = useDialog({ title: props.dialogTitle });

const loading = ref(false);
/** 当前检索结果（关键词/分类由服务端过滤） */
const list = ref<ErpProductVO[]>([]);
/** 已选商品，用于触发器回显 */
const selected = ref<ErpProductVO>();
/** 弹窗内高亮行，点「确定」后落为选中 */
const currentRow = ref<ErpProductVO>();

const keyword = ref('');
const categoryId = ref<ErpId>();
/** 类型筛选：仅 props.type 未限定时生效 */
const typeFilter = ref<ErpProductType>();
/** 类型下拉选项：t-select 只认 {label,value} 数组，直接传 Record 会在其内部 .map 抛 TypeError */
const typeOptions = computed(() => Object.entries(ERP_PRODUCT_TYPE).map(([value, item]) => ({ value, label: item.label })));

const categories = ref<ErpProductCategoryVO[]>([]);
const categoriesLoaded = ref(false);

/** 库存：productId -> qty；无权限或加载失败则不展示库存列 */
const stockMap = ref<Record<string, number>>({});
const stockLoaded = ref(false);
const stockAvailable = ref(false);

const pageNum = ref(1);
const pageSize = ref(10);
/** 多选已勾选 ID（受控）：t-table 的 selected-row-keys 只认这个数组，不再从表格实例读 */
const checkedIds = ref<ErpId[]>([]);

/** 多选：弹窗内实时勾选数（modelValue 要等「确定」才更新，不能拿来判断按钮可用性） */
const pickingCount = computed(() => checkedIds.value.length);

/** 多选已选 ID 集合，触发器文案与「确定」按钮可用性都基于它 */
const pickedIds = computed<ErpId[]>(() => (props.multiple && Array.isArray(props.modelValue) ? props.modelValue : []));

/** 触发器展示文案：多选显示数量，单选显示「编码 名称 / 规格」 */
const label = computed(() => {
  if (props.multiple) return pickedIds.value.length ? `已选 ${pickedIds.value.length} 项` : '';
  const p = selected.value;
  if (!p) return '';
  return `${p.code} ${p.name}${p.spec ? ' / ' + p.spec : ''}`;
});

/** 类型在服务端已过滤时不再二次过滤 */
const filtered = computed(() => {
  if (props.type || !typeFilter.value) return list.value;
  return list.value.filter((item) => item.type === typeFilter.value);
});

const total = computed(() => filtered.value.length);

/**
 * 表格数据
 *
 * 单选按 10 条客户端分页；多选不分页（表格内滚动），
 * 因为跨页勾选需要 reserve-selection，确认时容易丢页外已选行，得不偿失。
 */
const pagedList = computed(() => {
  if (props.multiple) return filtered.value;
  const start = (pageNum.value - 1) * pageSize.value;
  return filtered.value.slice(start, start + pageSize.value);
});

/** 多选模式「确定」是否可用：按弹窗内实时勾选数判断 */
const canConfirm = computed(() => (props.multiple ? pickingCount.value > 0 : !!currentRow.value));

const showStockColumn = computed(() => props.showStock && stockAvailable.value);

/** 单选当前高亮行：t-table 用 active-row-keys 受控（多选态不高亮） */
const activeRowKeys = computed<Array<string | number>>(() => (props.multiple || !currentRow.value ? [] : [currentRow.value.id]));

/**
 * 表格列配置
 *
 * t-table 用 columns 数组：勾选列必须写成 { type: 'multiple' } 的列项才会渲染，
 * 只传 selectedRowKeys 不建列（useRowSelect 只从 columns 里找 type）。
 * 弹窗表同样开列宽拖拽，宽度按 'product-select/list' 记忆。
 */
const columns = computed<any[]>(() => {
  const cols: any[] = [];
  if (props.multiple) cols.push({ colKey: 'row-select', type: 'multiple', width: 46, align: 'center' });
  cols.push(
    { colKey: 'code', title: '编码', align: 'center', width: 120, ellipsis: true },
    { colKey: 'name', title: '名称', align: 'left', minWidth: 160, ellipsis: true },
    { colKey: 'spec', title: '规格', align: 'center', width: 120, ellipsis: true },
    { colKey: 'unit', title: '单位', align: 'center', width: 70 },
    { colKey: 'categoryName', title: '分类', align: 'center', width: 110, ellipsis: true }
  );
  if (!props.type) cols.push({ colKey: 'type', title: '类型', align: 'center', width: 80 });
  cols.push(
    { colKey: 'supplierName', title: '供应商', align: 'center', width: 130, ellipsis: true },
    { colKey: 'qtyScale', title: '数量精度', align: 'center', width: 90 },
    { colKey: 'taxType', title: '含税', align: 'center', width: 100 }
  );
  if (showStockColumn.value) cols.push({ colKey: 'stock', title: '库存', align: 'right', width: 100 });
  cols.push(
    { colKey: 'purchasePrice', title: '采购价', align: 'right', width: 110 },
    { colKey: 'costPrice', title: '最近成本', align: 'right', width: 110 }
  );
  return mergeSavedWidths('product-select/list', cols);
});

/**
 * 数量精度文案
 *
 * @param scale 精度位数 0~3
 * @returns 中文文案，如「整数」「2位小数」
 */
const qtyScaleText = (scale?: number | null): string => ERP_QTY_SCALE_OPTIONS.find((item) => item.value === Number(scale ?? 0))?.label ?? '整数';

/** 分类筛选条：仅首次打开时加载 */
const loadCategories = async () => {
  if (!props.showCategoryFilter || categoriesLoaded.value) return;
  categoriesLoaded.value = true;
  try {
    const res = await productCategoryOptions();
    categories.value = res.data ?? [];
  } catch {
    categories.value = [];
  }
};

/**
 * 加载库存用于展示
 *
 * 无 erp:stock:list 权限时直接跳过，避免每次打开弹窗都弹一次 403 提示。
 */
const loadStock = async () => {
  if (!props.showStock || stockLoaded.value) return;
  if (!checkPermi(['erp:stock:list'])) {
    stockLoaded.value = true;
    return;
  }
  try {
    const res = await listStockBalances({});
    const map: Record<string, number> = {};
    // 库存接口按「产品+仓库」逐行返回，选择器展示全仓合计，故累加而非覆盖
    (res.data ?? []).forEach((row) => {
      const key = String(row.productId);
      map[key] = (map[key] ?? 0) + Number(row.qty ?? 0);
    });
    stockMap.value = map;
    stockAvailable.value = true;
  } catch {
    stockAvailable.value = false;
  } finally {
    stockLoaded.value = true;
  }
};

/** 按当前关键词/分类/类型检索商品；请求失败按空结果处理（错误提示由请求拦截器统一弹出） */
const load = async () => {
  loading.value = true;
  try {
    const res = await productOptions(keyword.value.trim() || undefined, categoryId.value, props.type);
    list.value = res.data ?? [];
    pageNum.value = 1;
  } catch {
    list.value = [];
  } finally {
    loading.value = false;
  }
};

/**
 * 多选回显：把已选 ID 收敛到当前列表内的行（商品被删或类型不符时丢弃）
 *
 * t-table 勾选为受控模式，直接给 checkedIds 赋值即可，不再操作表格实例。
 */
const restoreSelection = () => {
  if (!props.multiple) return;
  const idSet = new Set(pickedIds.value.map((id) => String(id)));
  // 回填行自身的 id 作为勾选 key：父级传字符串、列表 id 为数字时，t-table 内部的严格 includes 才不会落空
  checkedIds.value = list.value.filter((row) => idSet.has(String(row.id))).map((row) => row.id);
};

/** 打开弹窗：重置检索条件并拉取数据 */
const open = async () => {
  if (props.disabled) return;
  keyword.value = '';
  categoryId.value = undefined;
  typeFilter.value = undefined;
  currentRow.value = selected.value;
  checkedIds.value = [];
  productDialog.openDialog();
  await Promise.all([loadCategories(), loadStock(), load()]);
  restoreSelection();
};

/** 勾选变化：TDesign 回传当前全部已选行 key（受控），直接同步给 checkedIds */
const handleSelectChange = (keys: Array<string | number>) => {
  checkedIds.value = keys;
};

/** 切换分类 */
const selectCategory = async (id?: ErpId) => {
  if (String(categoryId.value ?? '') === String(id ?? '')) return;
  categoryId.value = id;
  await load();
};

/** 类型切换（仅 props.type 未限定时可用） */
const selectType = (value?: ErpProductType) => {
  typeFilter.value = value || undefined;
  pageNum.value = 1;
};

/** 确认选择：单选回传商品对象，多选回传商品数组，开单页据此带出单位/精度/默认价 */
const confirmPick = () => {
  if (props.multiple) {
    // 行 key 是原始 id，逐行 String() 比对：不能先整体 join 再 includes，否则字符串/数字类型不一致会筛空
    const rows = list.value.filter((r) => checkedIds.value.some((k) => String(k) === String(r.id)));
    if (!rows.length) return;
    emit(
      'update:modelValue',
      rows.map((row) => row.id)
    );
    emit('change-multiple', rows);
    productDialog.closeDialog();
    return;
  }
  const product = currentRow.value;
  if (!product) return;
  selected.value = product;
  emit('update:modelValue', product.id);
  emit('change', product);
  productDialog.closeDialog();
};

/** 单击行：多选切换勾选；单选只高亮，避免误触直接改单（改单会触发开单页重算默认价） */
const handleRowClick = ({ row }: { row: ErpProductVO }) => {
  if (props.multiple) {
    // 行点击不触发 select-change，受控勾选在这里按 id 做数组 toggle
    const exists = checkedIds.value.some((k) => String(k) === String(row.id));
    checkedIds.value = exists ? checkedIds.value.filter((k) => String(k) !== String(row.id)) : [...checkedIds.value, row.id];
    return;
  }
  currentRow.value = row;
};

/** 双击行直接确认 */
const handleRowDblclick = ({ row }: { row: ErpProductVO }) => {
  if (props.multiple) return;
  currentRow.value = row;
  confirmPick();
};

/** 清空选择 */
const clear = () => {
  if (props.multiple) {
    checkedIds.value = [];
    emit('update:modelValue', []);
    emit('change-multiple', []);
    return;
  }
  selected.value = undefined;
  emit('update:modelValue', undefined);
  emit('change', undefined);
};

// 外部改值（编辑回显、程序赋值）时同步选中项：先查已加载列表，再回源单商品接口。
// 多选模式的触发器只显示数量，不需要解析商品对象，跳过。
watch(
  () => props.modelValue,
  async (value) => {
    if (props.multiple) return;
    if (value === null || value === undefined || value === '' || Array.isArray(value)) {
      selected.value = undefined;
      return;
    }
    if (selected.value && String(selected.value.id) === String(value)) return;
    const hit = list.value.find((item) => String(item.id) === String(value));
    if (hit) {
      selected.value = hit;
      return;
    }
    try {
      const res = await getProduct(value);
      selected.value = res.data;
    } catch {
      selected.value = undefined;
    }
  },
  { immediate: true }
);

defineExpose({ open });
</script>

<template>
  <div class="product-picker">
    <!-- 触发器：只读输入框，点击弹出商品选择窗；DOC/35 T2 起改用 TDesign t-input，与开单页其余控件同款 -->
    <div class="product-picker__trigger" :title="label || ''" @click="open">
      <t-input :value="label" :placeholder="placeholder" :disabled="disabled" readonly>
        <template #suffix>
          <CloseCircleFilledIcon v-if="clearable && label && !disabled" class="product-picker__clear" @click.stop="clear" />
          <SearchIcon v-else class="product-picker__icon" />
        </template>
      </t-input>
    </div>

    <t-dialog
      v-model:visible="productDialog.visible.value"
      :header="productDialog.title.value"
      width="88%"
      top="6vh"
      attach="body"
      :destroy-on-close="true"
    >
      <div class="flex flex-wrap items-center gap-3 mb-3">
        <t-input v-model="keyword" placeholder="编码 / 名称 / 规格" clearable style="width: 260px" @enter="load" @clear="load">
          <template #prefix-icon><SearchIcon /></template>
        </t-input>
        <t-select
          v-if="!type"
          :model-value="typeFilter"
          placeholder="全部类型"
          clearable
          style="width: 120px"
          :options="typeOptions"
          @change="selectType"
        />
        <t-button theme="primary" @click="load">
          <template #icon><SearchIcon /></template>
          搜索
        </t-button>
        <span class="erp-text-secondary"> 共 {{ total }} 个商品{{ multiple ? '，点击行勾选，再点「确定」' : '，点击行选中、双击直接确定' }} </span>
      </div>

      <!-- 分类筛选：与商品资料同一套分类，快速缩小范围 -->
      <div v-if="showCategoryFilter && categories.length > 0" class="flex flex-wrap gap-1 mb-2">
        <t-check-tag :checked="categoryId == null" @change="selectCategory(undefined)">全部</t-check-tag>
        <t-check-tag v-for="item in categories" :key="item.id" :checked="String(categoryId) === String(item.id)" @change="selectCategory(item.id)">
          {{ item.name }}{{ item.status === 1 ? '' : '（停用）' }}
        </t-check-tag>
      </div>

      <t-table
        class="erp-table"
        row-key="id"
        :data="pagedList"
        :columns="columns"
        :loading="loading"
        bordered
        :hover="true"
        :resizable="true"
        size="small"
        max-height="52vh"
        active-row-type="single"
        :active-row-keys="activeRowKeys"
        :selected-row-keys="checkedIds"
        @row-click="(ctx: any) => handleRowClick(ctx)"
        @row-dblclick="(ctx: any) => handleRowDblclick(ctx)"
        @select-change="handleSelectChange"
        @column-resize-change="(ctx: any) => saveColumnWidths('product-select/list', ctx.columnsWidth)"
      >
        <template #spec="{ row }">{{ row.spec || '-' }}</template>
        <template #unit="{ row }">{{ row.unit || '-' }}</template>
        <template #categoryName="{ row }">{{ row.categoryName || '未分类' }}</template>
        <template #type="{ row }">{{ ERP_PRODUCT_TYPE[row.type]?.label || '-' }}</template>
        <template #supplierName="{ row }">{{ row.supplierName || '-' }}</template>
        <template #qtyScale="{ row }">{{ qtyScaleText(row.qtyScale) }}</template>
        <template #taxType="{ row }">
          <!-- 复用商品资料的发票类型映射：不含税即不含税价，专票/普票为含税价 -->
          <StatusTag kind="tax" :value="row.taxType" />
        </template>
        <template #stock="{ row }">{{ formatQtyTrim(stockMap[String(row.id)]) }}</template>
        <template #purchasePrice="{ row }">{{ formatMoney(row.purchasePrice) }}</template>
        <template #costPrice="{ row }">{{ formatMoney(row.costPrice) }}</template>
        <template #empty>
          <span class="erp-text-secondary">没有匹配的商品</span>
        </template>
      </t-table>

      <pagination v-if="!multiple" v-show="total > 0" v-model:page="pageNum" v-model:limit="pageSize" :total="total" />

      <template #footer>
        <div class="flex items-center justify-between">
          <div v-if="multiple" class="erp-text-secondary">
            已选 <b>{{ pickingCount }}</b> 项
          </div>
          <div v-else class="erp-text-secondary">
            当前选中：<b>{{ currentRow ? `${currentRow.code} ${currentRow.name}` : '未选择' }}</b>
          </div>
          <div>
            <t-button theme="default" variant="outline" @click="productDialog.closeDialog()">取 消</t-button>
            <t-button theme="primary" :disabled="!canConfirm" @click="confirmPick">确 定</t-button>
          </div>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<style scoped lang="scss">
.product-picker__trigger {
  cursor: pointer;

  :deep(.t-input__inner) {
    cursor: pointer;
  }
}

.product-picker__icon,
.product-picker__clear {
  color: var(--td-text-color-placeholder, #999);
}

.product-picker__clear {
  cursor: pointer;

  &:hover {
    color: var(--td-brand-color, #0052d9);
  }
}
</style>
