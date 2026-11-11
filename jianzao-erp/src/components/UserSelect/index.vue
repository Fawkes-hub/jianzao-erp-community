<template>
  <div>
    <t-dialog v-model:visible="userDialog.visible.value" width="80%" attach="body">
      <template #header>{{ userDialog.title.value }}</template>
      <t-row :gutter="20">
        <!-- 部门树 -->
        <t-col :lg="2" :xs="12">
          <t-card hover-shadow>
            <t-input v-model="deptName" placeholder="请输入部门名称" clearable>
              <template #prefix-icon><SearchIcon /></template>
            </t-input>
            <t-tree
              ref="deptTreeRef"
              class="mt-2"
              :data="deptOptions"
              :keys="{ value: 'id', label: 'label', children: 'children' }"
              :activable="true"
              :actived="deptActived"
              :expand-on-click-node="false"
              :filter="deptFilter"
              :hover="true"
              expand-all
              @click="handleNodeClick"
            />
          </t-card>
        </t-col>
        <t-col :lg="10" :xs="12">
          <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
            <div v-show="showSearch" class="mb-[10px]">
              <t-card hover-shadow>
                <t-form ref="queryFormRef" :data="queryParams" layout="inline" reset-type="initial">
                  <t-form-item label="用户名称" name="userName">
                    <t-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable @enter="handleQuery" />
                  </t-form-item>
                  <t-form-item label="手机号码" name="phonenumber">
                    <t-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable @enter="handleQuery" />
                  </t-form-item>
                  <t-form-item>
                    <t-button theme="primary" @click="handleQuery">
                      <template #icon><SearchIcon /></template>
                      搜索
                    </t-button>
                    <t-button theme="default" variant="outline" @click="() => resetQuery()">
                      <template #icon><RefreshIcon /></template>
                      重置
                    </t-button>
                  </t-form-item>
                </t-form>
              </t-card>
            </div>
          </transition>

          <t-card hover-shadow>
            <template v-if="prop.multiple" #header>
              <t-tag v-for="user in selectUserList" :key="user.userId" closable style="margin: 2px" @close="handleCloseTag(user)">
                {{ user.nickName }}
              </t-tag>
            </template>

            <vxe-table
              ref="tableRef"
              height="400px"
              border
              show-overflow
              :data="userList"
              :loading="loading"
              :row-config="{ keyField: 'userId', isHover: true }"
              :checkbox-config="{ reserve: true, trigger: 'row', highlight: true, showHeader: prop.multiple }"
              @checkbox-all="handleCheckboxAll"
              @checkbox-change="handleCheckboxChange"
            >
              <vxe-column type="checkbox" width="50" align="center" />
              <vxe-column key="userId" title="用户编号" align="center" field="userId" />
              <vxe-column key="userName" title="用户名称" align="center" field="userName" />
              <vxe-column key="nickName" title="用户昵称" align="center" field="nickName" />
              <vxe-column key="deptName" title="部门" align="center" field="deptName" />
              <vxe-column key="phonenumber" title="手机号码" align="center" field="phonenumber" width="120" />
              <vxe-column key="status" title="状态" align="center">
                <template #default="scope">
                  <dict-tag :options="sys_normal_disable" :value="scope.row.status"></dict-tag>
                </template>
              </vxe-column>

              <vxe-column title="创建时间" align="center" width="160">
                <template #default="scope">
                  <span>{{ scope.row.createTime }}</span>
                </template>
              </vxe-column>
            </vxe-table>

            <pagination
              v-show="total > 0"
              v-model:page="queryParams.pageNum"
              v-model:limit="queryParams.pageSize"
              :total="total"
              @pagination="pageList"
            />
          </t-card>
        </t-col>
      </t-row>

      <template #footer>
        <t-button theme="default" variant="outline" @click="close">取消</t-button>
        <t-button theme="primary" @click="confirm">确定</t-button>
      </template>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import api from '@/api/system/user';
import { UserQuery, UserVO } from '@/api/system/user/types';
import { DeptTreeVO, DeptVO } from '@/api/system/dept/types';
import { VxeTableInstance } from 'vxe-table';
import { SearchIcon, RefreshIcon } from 'tdesign-icons-vue-next';
import type { TreeNodeModel } from 'tdesign-vue-next';
import useDialog from '@/hooks/useDialog';

interface PropType {
  modelValue?: UserVO[] | UserVO | undefined;
  multiple?: boolean;
  data?: string | number | (string | number)[] | undefined;
  userIds?: string | number | (string | number)[] | undefined;
}
const prop = withDefaults(defineProps<PropType>(), {
  multiple: true,
  modelValue: undefined,
  data: undefined,
  userIds: undefined
});
const emit = defineEmits(['update:modelValue', 'confirmCallBack']);

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_normal_disable'));

const userList = ref<UserVO[]>();
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const dateRange = ref<[DateModelType, DateModelType]>(['', '']);
const deptName = ref('');
const deptOptions = ref<DeptTreeVO[]>([]);
const selectUserList = ref<UserVO[]>([]);
// t-tree 的激活节点为数组，用单元素数组维持原部门树的单选高亮语义
const deptActived = ref<(string | number)[]>([]);

const deptTreeRef = ref<any>();
const queryFormRef = ref<any>();
const tableRef = ref<VxeTableInstance<UserVO>>();

const userDialog = useDialog({
  title: '用户选择'
});

const queryParams = ref<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  phonenumber: '',
  status: '',
  deptId: '',
  roleId: '',
  userIds: ''
});

const defaultSelectUserIds = computed(() => computedIds(prop.data));

/**
 * 部门树过滤条件：t-tree 的 filter 是响应式 prop，函数引用变化才会触发节点可见性重算
 * 保持原部门树按 label 子串匹配的逻辑；未录入时不传 filter，让树回到正常展开态显示全部节点
 */
const deptFilter = computed<((node: TreeNodeModel) => boolean) | undefined>(() => {
  const keyword = deptName.value;
  if (!keyword) return undefined;
  return (node: TreeNodeModel) => String(node.label ?? '').indexOf(keyword) !== -1;
});

const confirm = () => {
  emit('update:modelValue', selectUserList.value);
  emit('confirmCallBack', selectUserList.value);
  userDialog.closeDialog();
};

const computedIds = (data) => {
  if (data === '' || data === null || data === undefined) {
    return [];
  }
  if (data instanceof Array) {
    return data.map((item) => String(item));
  } else if (typeof data === 'string') {
    return data.split(',');
  } else if (typeof data === 'number') {
    return [String(data)];
  } else {
    console.warn('<UserSelect> The data type of data should be array or string or number, but I received other');
    return [];
  }
};

/** 查询部门下拉树结构 */
const getTreeSelect = async () => {
  const res = await api.deptTreeSelect();
  deptOptions.value = res.data;
};

/** 查询用户列表 */
const getList = async () => {
  loading.value = true;
  queryParams.value.userIds = prop.userIds;
  const res = await api.listUser(proxy?.addDateRange(queryParams.value, dateRange.value));
  loading.value = false;
  userList.value = res.rows;
  total.value = res.total;
};

const pageList = async () => {
  await getList();
  const users = userList.value.filter((item) => {
    return selectUserList.value.some((user) => user.userId === item.userId);
  });
  await tableRef.value.setCheckboxRow(users, true);
};

/** 节点单击事件：t-tree 的 click 上下文是 { node, e }，node.data 为原始数据 */
const handleNodeClick = (context: { node: TreeNodeModel }) => {
  queryParams.value.deptId = context.node.data.id;
  handleQuery();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
/** 重置按钮操作 */
const resetQuery = (refresh = true) => {
  dateRange.value = ['', ''];
  queryFormRef.value?.reset();
  queryParams.value.pageNum = 1;
  queryParams.value.deptId = undefined;
  deptActived.value = [];
  refresh && handleQuery();
};

const handleCheckboxChange = (checked) => {
  if (!prop.multiple && checked.checked) {
    tableRef.value.setCheckboxRow(selectUserList.value, false);
    selectUserList.value = [];
  }
  const row = checked.row;
  if (checked.checked) {
    selectUserList.value.push(row);
  } else {
    selectUserList.value = selectUserList.value.filter((item) => {
      return item.userId !== row.userId;
    });
  }
};
const handleCheckboxAll = (checked) => {
  const rows = userList.value;
  if (checked.checked) {
    rows.forEach((row) => {
      if (!selectUserList.value.some((item) => item.userId === row.userId)) {
        selectUserList.value.push(row);
      }
    });
  } else {
    selectUserList.value = selectUserList.value.filter((item) => {
      return !rows.some((row) => row.userId === item.userId);
    });
  }
};

const handleCloseTag = (user: UserVO) => {
  const userId = user.userId;
  // 使用split删除用户
  const index = selectUserList.value.findIndex((item) => item.userId === userId);
  const rows = selectUserList.value[index];
  tableRef.value?.setCheckboxRow(rows, false);
  selectUserList.value.splice(index, 1);
};

const initSelectUser = async () => {
  if (defaultSelectUserIds.value.length > 0) {
    const { data } = await api.optionSelect(defaultSelectUserIds.value);
    selectUserList.value = data;
    const users = userList.value.filter((item) => {
      return defaultSelectUserIds.value.includes(String(item.userId));
    });
    await nextTick(() => {
      tableRef.value.setCheckboxRow(users, true);
    });
  }
};
const close = () => {
  userDialog.closeDialog();
};

watch(
  () => userDialog.visible.value,
  async (newValue: boolean) => {
    if (newValue) {
      await getTreeSelect(); // 初始化部门数据
      await getList(); // 初始化列表数据
      await initSelectUser();
    } else {
      tableRef.value.clearCheckboxReserve();
      tableRef.value.clearCheckboxRow();
      resetQuery(false);
      selectUserList.value = [];
    }
  }
);

defineExpose({
  open: userDialog.openDialog,
  close: userDialog.closeDialog
});
</script>
