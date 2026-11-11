<template>
  <div class="p-2">
    <t-card>
      <!-- mode用于直接后端发起流程 不同接口实现方式可查看具体后端代码 -->
      <!-- 默认前端发起 前端发起更多样性 比如可以选审批人 选抄送人 上传附件等等 后端发起需要用户自行编写代码传这些参数 -->
      <approvalButton
        @submitForm="submitForm"
        @approvalVerifyOpen="approvalVerifyOpen"
        @handleApprovalRecord="handleApprovalRecord"
        :buttonLoading="buttonLoading"
        :id="form.id"
        :status="form.status"
        :pageType="routeParams.type"
        :mode="false"
      />
    </t-card>
    <t-card class="leave-form-card">
      <!-- 原 el-form 的 v-loading 改由 t-loading 包裹，加载态语义不变 -->
      <t-loading :loading="loading">
        <t-form ref="leaveFormRef" :disabled="routeParams.type === 'view'" :data="form" :rules="rules" label-width="80px">
          <t-form-item v-if="routeParams.type === 'add'" label="流程定义">
            <t-select v-model="flowCode" placeholder="选择流程定义" :options="flowCodeOptions" style="width: 100%" />
          </t-form-item>
          <t-form-item label="请假类型" name="leaveType">
            <t-select v-model="form.leaveType" placeholder="请选择请假类型" :options="options" style="width: 100%" />
          </t-form-item>
          <t-form-item label="请假时间" :required-mark="true">
            <!-- el-date-picker type=daterange → t-date-range-picker：start/end-placeholder 合并为数组（DOC/35 §3） -->
            <t-date-range-picker
              v-model="leaveTime"
              value-type="YYYY-MM-DD HH:mm:ss"
              :placeholder="['开始时间', '结束时间']"
              :default-time="['00:00:00', '23:59:59']"
              @change="changeLeaveTime()"
            />
          </t-form-item>
          <t-form-item label="请假天数" name="leaveDays">
            <t-input v-model="form.leaveDays" disabled type="number" placeholder="请输入请假天数" />
          </t-form-item>
          <t-form-item label="请假原因" name="remark">
            <t-textarea v-model="form.remark" :autosize="{ minRows: 3 }" placeholder="请输入请假原因" />
          </t-form-item>
        </t-form>
      </t-loading>
    </t-card>
    <!-- 提交组件 -->
    <submitVerify ref="submitVerifyRef" :task-variables="taskVariables" @submit-callback="submitCallback" />
    <!-- 审批记录 -->
    <approvalRecord ref="approvalRecordRef" />
  </div>
</template>

<script setup name="Leave" lang="ts">
import { addLeave, getLeave, submitAndFlowStart, updateLeave } from '@/api/workflow/leave';
import { LeaveForm, LeaveQuery, LeaveVO } from '@/api/workflow/leave/types';
import { startWorkFlow } from '@/api/workflow/task';
import SubmitVerify from '@/components/Process/submitVerify.vue';
import ApprovalRecord from '@/components/Process/approvalRecord.vue';
import ApprovalButton from '@/components/Process/approvalButton.vue';
import { AxiosResponse } from 'axios';
import { StartProcessBo } from '@/api/workflow/workflowCommon/types';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const buttonLoading = ref(false);
const loading = ref(true);
const leaveTime = ref<Array<string>>([]);
//路由参数
const routeParams = ref<Record<string, any>>({});
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
const flowCodeOptions = [
  {
    value: 'leave1',
    label: '请假申请-普通'
  },
  {
    value: 'leave2',
    label: '请假申请-排他网关'
  },
  {
    value: 'leave3',
    label: '请假申请-并行网关'
  },
  {
    value: 'leave4',
    label: '请假申请-会签'
  },
  {
    value: 'leave5',
    label: '请假申请-并行会签网关'
  },
  {
    value: 'leave6',
    label: '请假申请-排他并行会签'
  }
];
// 自定义流程可不选择 直接填写flowCode 例如 'leave1'
const flowCode = ref<string>('leave1');

//提交组件
const submitVerifyRef = ref<InstanceType<typeof SubmitVerify>>();
//审批记录组件
const approvalRecordRef = ref<InstanceType<typeof ApprovalRecord>>();

const leaveFormRef = ref<any>();

const submitFormData = ref<StartProcessBo>({
  businessId: '',
  flowCode: '',
  variables: {},
  bizExt: {}
});
const taskVariables = ref<Record<string, any>>({});
const bizExt = ref<Record<string, any>>({});

const initFormData: LeaveForm = {
  id: undefined,
  leaveType: undefined,
  startDate: undefined,
  endDate: undefined,
  leaveDays: undefined,
  remark: undefined,
  status: undefined
};
const data = reactive<{ form: LeaveForm; queryParams: LeaveQuery }>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    startLeaveDays: undefined,
    endLeaveDays: undefined
  }
});

const { form } = toRefs(data);

/** 表单校验规则：t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，单独用 Record 声明（DOC/35 §5） */
const rules: Record<string, any> = {
  id: [{ required: true, message: '主键不能为空', trigger: 'blur' }],
  leaveType: [{ required: true, message: '请假类型不能为空', trigger: 'blur' }],
  leaveTime: [{ required: true, message: '请假时间不能为空', trigger: 'blur' }],
  leaveDays: [{ required: true, message: '请假天数不能为空', trigger: 'blur' }]
};

/** 表单重置：t-form 的 reset() 按 emptyType 清值，这里以 initFormData 为准，只清校验态 */
const reset = () => {
  form.value = { ...initFormData };
  leaveTime.value = [];
  leaveFormRef.value?.clearValidate();
};

const changeLeaveTime = () => {
  const startDate = new Date(leaveTime.value[0]).getTime();
  const endDate = new Date(leaveTime.value[1]).getTime();
  const diffInMilliseconds = endDate - startDate;
  form.value.leaveDays = Math.floor(diffInMilliseconds / (1000 * 60 * 60 * 24)) + 1;
};
/** 获取详情 */
const getInfo = () => {
  loading.value = true;
  buttonLoading.value = false;
  nextTick(async () => {
    const res = await getLeave(routeParams.value.id);
    Object.assign(form.value, res.data);
    leaveTime.value = [];
    leaveTime.value.push(form.value.startDate);
    leaveTime.value.push(form.value.endDate);
    loading.value = false;
    buttonLoading.value = false;
  });
};

/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async (status: string, mode: boolean) => {
  if (leaveTime.value.length === 0) {
    proxy?.$modal.msgError('请假时间不能为空');
    return;
  }
  try {
    const result = await leaveFormRef.value?.validate().catch(() => false);
    const valid = result === true || result === undefined;
    form.value.startDate = leaveTime.value[0];
    form.value.endDate = leaveTime.value[1];
    if (valid) {
      buttonLoading.value = true;
      // 设置后端发起和不等于草稿状态 直接走流程发起
      if (mode && status != 'draft') {
        const res = await submitAndFlowStart(form.value).finally(() => (buttonLoading.value = false));
        form.value = res.data;
        buttonLoading.value = false;
        proxy?.$modal.msgSuccess('操作成功');
        proxy.$tab.closePage(proxy.$route);
        proxy.$router.go(-1);
      } else {
        let res;
        if (form.value.id) {
          res = await updateLeave(form.value).finally(() => (buttonLoading.value = false));
        } else {
          res = await addLeave(form.value).finally(() => (buttonLoading.value = false));
        }
        form.value = res.data;
        if (status === 'draft') {
          buttonLoading.value = false;
          proxy?.$modal.msgSuccess('暂存成功');
          proxy.$tab.closePage(proxy.$route);
          proxy.$router.go(-1);
        } else {
          await handleStartWorkFlow(res.data);
        }
      }
    }
  } finally {
    buttonLoading.value = false;
  }
};

//提交申请
const handleStartWorkFlow = async (data: LeaveForm) => {
  try {
    submitFormData.value.flowCode = flowCode.value;
    submitFormData.value.businessId = data.id;
    //流程变量
    taskVariables.value = {
      // leave2/6 使用的流程变量
      leaveDays: data.leaveDays,
      // leave4/5 使用的流程变量
      userList: ['1', '3', '4']
    };
    //流程实例业务扩展字段
    bizExt.value = {
      businessTitle: '请假申请',
      businessCode: data.applyCode
    };
    submitFormData.value.variables = taskVariables.value;
    submitFormData.value.bizExt = bizExt.value;
    const resp = await startWorkFlow(submitFormData.value);
    if (submitVerifyRef.value) {
      buttonLoading.value = false;
      submitVerifyRef.value.openDialog(resp.data.taskId);
    }
  } finally {
    buttonLoading.value = false;
  }
};
//审批记录
const handleApprovalRecord = () => {
  approvalRecordRef.value.init(form.value.id);
};
//提交回调
const submitCallback = async () => {
  await proxy.$tab.closePage(proxy.$route);
  proxy.$router.go(-1);
};
//审批
const approvalVerifyOpen = async () => {
  submitVerifyRef.value.openDialog(routeParams.value.taskId);
};

onMounted(() => {
  nextTick(async () => {
    routeParams.value = proxy.$route.query;
    reset();
    loading.value = false;
    if (routeParams.value.type === 'update' || routeParams.value.type === 'view' || routeParams.value.type === 'approval') {
      getInfo();
    }
  });
});
</script>

<style lang="scss" scoped>
.leave-form-card {
  height: 78vh;
  overflow-y: auto;
}
</style>
