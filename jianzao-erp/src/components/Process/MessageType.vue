<template>
  <t-dialog
    v-model:visible="visible"
    :header="props.title"
    width="50%"
    draggable
    placement="center"
    :close-on-overlay-click="false"
    attach="body"
    @close="cancel"
  >
    <t-loading :loading="loading" show-overlay>
      <t-form ref="ruleFormRef" :data="form" :rules="rules" label-width="120px">
        <t-form-item label="消息提醒" name="messageType">
          <t-checkbox-group v-model="form.messageType" name="type">
            <t-checkbox value="1" disabled>站内信</t-checkbox>
            <t-checkbox value="2">邮件</t-checkbox>
            <t-checkbox value="3">短信</t-checkbox>
          </t-checkbox-group>
        </t-form-item>
        <t-form-item label="消息内容" name="message">
          <t-textarea v-model="form.message" :autosize="{ minRows: 3, maxRows: 6 }" />
        </t-form-item>
      </t-form>
    </t-loading>
    <template #footer>
      <div class="dialog-footer" style="float: right; padding-bottom: 20px">
        <t-button :disabled="buttonDisabled" theme="primary" @click="submit">确认</t-button>
        <t-button :disabled="buttonDisabled" theme="default" variant="outline" @click="cancel">取消</t-button>
      </div>
    </template>
  </t-dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { ComponentInternalInstance } from 'vue';
import type { FormInstanceFunctions } from 'tdesign-vue-next';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const emits = defineEmits(['submitCallback', 'cancelCallback']);
const props = defineProps({
  title: {
    type: String,
    default: '提示'
  }
});
const ruleFormRef = ref<FormInstanceFunctions>();
//遮罩层
const loading = ref(true);
const visible = ref(false);
const buttonDisabled = ref(true);
const form = ref<Record<string, any>>({
  message: undefined,
  messageType: ['1']
});
const rules = reactive<Record<string, any>>({
  messageType: [
    {
      required: true,
      message: '请选择消息提醒',
      trigger: 'change'
    }
  ],
  message: [
    {
      required: true,
      message: '请输入消息内容',
      trigger: 'blur'
    }
  ]
});
//确认
//打开弹窗
const open = async () => {
  reset();
  visible.value = true;
  loading.value = false;
  buttonDisabled.value = false;
};
//关闭弹窗
const close = async () => {
  reset();
  visible.value = false;
};
// TDesign form.validate() 校验失败时 resolve(false)，不会 reject；r===true 或 undefined 视为通过
const submit = async () => {
  if (!ruleFormRef.value) return;
  const r = await ruleFormRef.value.validate().catch(() => false);
  if (r === true || r === undefined) {
    emits('submitCallback', form.value);
  }
};
//取消
const cancel = async () => {
  visible.value = false;
  buttonDisabled.value = false;
  emits('cancelCallback');
};
//重置：TDesign 的 reset() 清校验并复位 data，再用业务默认值覆盖
const reset = async () => {
  ruleFormRef.value?.reset();
  form.value.taskIdList = [];
  form.value.message = '';
  form.value.messageType = ['1'];
};
/**
 * 对外暴露子组件方法
 */
defineExpose({
  open,
  close
});
</script>
