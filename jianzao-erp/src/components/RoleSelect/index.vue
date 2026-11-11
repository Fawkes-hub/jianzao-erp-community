<template>
  <div>
    <t-dialog v-model:visible="roleDialog.visible.value" width="80%" attach="body" :close-on-overlay-click="false">
      <template #header>{{ roleDialog.title.value }}</template>
      <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
        <div v-show="showSearch" class="mb-[10px]">
          <t-card hover-shadow>
            <t-form ref="queryFormRef" :data="queryParams" layout="inline" reset-type="initial">
              <t-form-item label="角色名称" name="roleName">
                <t-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable @enter="handleQuery" />
              </t-form-item>
              <t-form-item label="权限字符" name="roleKey">
                <t-input v-model="queryParams.roleKey" placeholder="请输入权限字符" clearable @enter="handleQuery" />
              </t-form-item>

              <t-form-item>
                <t-button theme="primary" @click="handleQuery">
                  <template #icon><SearchIcon /></template>
                  搜索
                </t-button>
                <t-button theme="default" variant="outline" @click="resetQuery">
                  <template #icon><RefreshIcon /></template>
                  重置
                </t-button>
              </t-form-item>
            </t-form>
          </t-card>
        </div>
      </transition>

      <t-card hover-shadow>
        <template #header>
          <t-tag v-for="role in selectRoleList" :key="role.roleId" closable style="margin: 2px" @close="handleCloseTag(role)">
            {{ role.roleName }}
          </t-tag>
        </template>

        <vxe-table
          ref="tableRef"
          height="400px"
          border
          show-overflow
          :data="roleList"
          :loading="loading"
          :row-config="{ keyField: 'roleId' }"
          :checkbox-config="{ reserve: true, checkRowKeys: defaultSelectRoleIds }"
          highlight-current-row
          @checkbox-all="handleCheckboxAll"
          @checkbox-change="handleCheckboxChange"
        >
          <vxe-column type="checkbox" width="50" align="center" />
          <vxe-column v-if="false" key="roleId" label="角色编号" />
          <vxe-column field="roleName" title="角色名称" />
          <vxe-column field="roleKey" title="权限字符" />
          <vxe-column field="roleSort" title="显示顺序" width="100" />
          <vxe-column title="状态" align="center" width="100">
            <template #default="scope">
              <dict-tag :options="sys_normal_disable" :value="scope.row.status"></dict-tag>
            </template>
          </vxe-column>
          <vxe-column field="createTime" title="创建时间" align="center">
            <template #default="scope">
              <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
            </template>
          </vxe-column>
        </vxe-table>

        <pagination
          v-if="total > 0"
          v-model:total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="pageList"
        />
      </t-card>
      <template #footer>
        <t-button theme="default" variant="outline" @click="close">取消</t-button>
        <t-button theme="primary" @click="confirm">确定</t-button>
      </template>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { RoleVO, RoleQuery } from '@/api/system/role/types';
import { VxeTableInstance } from 'vxe-table';
import { SearchIcon, RefreshIcon } from 'tdesign-icons-vue-next';
import useDialog from '@/hooks/useDialog';
import api from '@/api/system/role';
interface PropType {
  modelValue?: RoleVO[] | RoleVO | undefined;
  multiple?: boolean;
  data?: string | number | (string | number)[];
}
const prop = withDefaults(defineProps<PropType>(), {
  multiple: true,
  modelValue: undefined,
  data: undefined
});
const emit = defineEmits(['update:modelValue', 'confirmCallBack']);

const router = useRouter();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_normal_disable'));

const roleList = ref<RoleVO[]>();
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const dateRange = ref<[DateModelType, DateModelType]>(['', '']);
const selectRoleList = ref<RoleVO[]>([]);

const roleDialog = useDialog({
  title: '角色选择'
});

/** 查询表单已换 t-form，reset() 为其实例方法；reset-type="initial" 保持恢复初始值的重置语义 */
const queryFormRef = ref<any>();
const tableRef = ref<VxeTableInstance<RoleVO>>();

const queryParams = ref<RoleQuery>({
  pageNum: 1,
  pageSize: 10,
  roleName: '',
  roleKey: '',
  status: ''
});

const defaultSelectRoleIds = computed(() => computedIds(prop.data));

const confirm = () => {
  emit('update:modelValue', selectRoleList.value);
  emit('confirmCallBack', selectRoleList.value);
  roleDialog.closeDialog();
};

const computedIds = (data) => {
  if (data instanceof Array) {
    return [...data];
  } else if (typeof data === 'string') {
    return data.split(',');
  } else if (typeof data === 'number') {
    return [data];
  } else {
    console.warn('<RoleSelect> The data type of data should be array or string or number, but I received other');
    return [];
  }
};

/**
 * 查询角色列表
 */
const getList = () => {
  loading.value = true;
  api.listRole(proxy?.addDateRange(queryParams.value, dateRange.value)).then((res) => {
    roleList.value = res.rows;
    total.value = res.total;
    loading.value = false;
  });
};
const pageList = async () => {
  await getList();
  const roles = roleList.value.filter((item) => {
    return selectRoleList.value.some((role) => role.roleId === item.roleId);
  });
  await tableRef.value.setCheckboxRow(roles, true);
};
/**
 * 搜索按钮操作
 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置 */
const resetQuery = () => {
  dateRange.value = ['', ''];
  queryFormRef.value?.reset();
  handleQuery();
};

const handleCheckboxChange = (checked) => {
  if (!prop.multiple && checked.checked) {
    tableRef.value.setCheckboxRow(selectRoleList.value, false);
    selectRoleList.value = [];
  }
  const row = checked.row;
  if (checked.checked) {
    selectRoleList.value.push(row);
  } else {
    selectRoleList.value = selectRoleList.value.filter((item) => {
      return item.roleId !== row.roleId;
    });
  }
};
const handleCheckboxAll = (checked) => {
  const rows = roleList.value;
  if (checked.checked) {
    rows.forEach((row) => {
      if (!selectRoleList.value.some((item) => item.roleId === row.roleId)) {
        selectRoleList.value.push(row);
      }
    });
  } else {
    selectRoleList.value = selectRoleList.value.filter((item) => {
      return !rows.some((row) => row.roleId === item.roleId);
    });
  }
};

const handleCloseTag = (user: RoleVO) => {
  const roleId = user.roleId;
  // 使用split删除用户
  const index = selectRoleList.value.findIndex((item) => item.roleId === roleId);
  const rows = selectRoleList.value[index];
  tableRef.value?.setCheckboxRow(rows, false);
  selectRoleList.value.splice(index, 1);
};
/**
 * 初始化选中数据
 */
const initSelectRole = async () => {
  if (defaultSelectRoleIds.value.length > 0) {
    const { data } = await api.optionSelect(defaultSelectRoleIds.value);
    selectRoleList.value = data;
    const users = roleList.value.filter((item) => {
      return defaultSelectRoleIds.value.includes(String(item.roleId));
    });
    await nextTick(() => {
      tableRef.value.setCheckboxRow(users, true);
    });
  }
};
const close = () => {
  roleDialog.closeDialog();
};
watch(
  () => roleDialog.visible.value,
  (newValue: boolean) => {
    if (newValue) {
      initSelectRole();
    } else {
      tableRef.value.clearCheckboxReserve();
      tableRef.value.clearCheckboxRow();
      resetQuery();
      selectRoleList.value = [];
    }
  }
);
onMounted(() => {
  getList(); // 初始化列表数据
});

defineExpose({
  open: roleDialog.openDialog,
  close: roleDialog.closeDialog
});
</script>
