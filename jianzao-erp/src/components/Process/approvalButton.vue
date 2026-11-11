<template>
  <div style="display: flex; justify-content: space-between">
    <div>
      <t-button v-if="submitButtonShow" :loading="props.buttonLoading" theme="default" variant="outline" @click="submitForm('draft', mode)">
        暂存
      </t-button>
      <t-button v-if="submitButtonShow" :loading="props.buttonLoading" theme="primary" @click="submitForm('submit', mode)">提 交</t-button>
      <t-button v-if="approvalButtonShow" :loading="props.buttonLoading" theme="primary" @click="approvalVerifyOpen">审批</t-button>
      <t-button v-if="props.id && props.status !== 'draft'" theme="primary" @click="handleApprovalRecord">流程进度</t-button>
      <slot />
    </div>
    <div>
      <t-button style="float: right" theme="default" variant="outline" @click="goBack()">返回</t-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { propTypes } from '@/utils/propTypes';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const props = defineProps({
  status: propTypes.string.def(''),
  pageType: propTypes.string.def(''),
  buttonLoading: propTypes.bool.def(false),
  id: propTypes.string.def('') || propTypes.number.def(),
  mode: propTypes.bool.def(false)
});
const emits = defineEmits(['submitForm', 'approvalVerifyOpen', 'handleApprovalRecord']);
//暂存，提交
const submitForm = async (type, mode) => {
  emits('submitForm', type, mode);
};
//审批
const approvalVerifyOpen = async () => {
  emits('approvalVerifyOpen');
};
//审批记录
const handleApprovalRecord = () => {
  emits('handleApprovalRecord');
};

//校验提交按钮是否显示
const submitButtonShow = computed(() => {
  return (
    props.pageType === 'add' ||
    (props.pageType === 'update' && props.status && (props.status === 'draft' || props.status === 'cancel' || props.status === 'back'))
  );
});

//校验审批按钮是否显示
const approvalButtonShow = computed(() => {
  return props.pageType === 'approval' && props.status && props.status === 'waiting';
});

//返回
const goBack = () => {
  proxy.$tab.closePage(proxy.$route);
  proxy.$router.go(-1);
};
</script>
