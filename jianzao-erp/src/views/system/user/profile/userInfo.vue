<template>
  <t-form ref="userRef" :data="userForm" :rules="rules" label-align="right" label-width="84px" class="profile-edit-form">
    <t-form-item label="用户昵称" name="nickName">
      <t-input v-model="userForm.nickName" maxlength="30" placeholder="请输入用户昵称" clearable style="width: 380px" />
    </t-form-item>
    <t-form-item label="手机号码" name="phonenumber">
      <t-input v-model="userForm.phonenumber" maxlength="11" placeholder="请输入手机号码" clearable style="width: 380px" />
    </t-form-item>
    <t-form-item label="常用邮箱" name="email">
      <t-input v-model="userForm.email" maxlength="50" placeholder="请输入常用邮箱地址" clearable style="width: 380px" />
    </t-form-item>
    <t-form-item label="用户性别" name="sex">
      <t-radio-group v-model="userForm.sex">
        <t-radio value="0">男</t-radio>
        <t-radio value="1">女</t-radio>
        <t-radio value="2">未知</t-radio>
      </t-radio-group>
    </t-form-item>
    <t-form-item class="form-actions">
      <t-space size="12px">
        <t-button theme="primary" :loading="saving" @click="submit">保存修改</t-button>
        <t-button theme="default" variant="base" @click="close">关闭</t-button>
      </t-space>
    </t-form-item>
  </t-form>
</template>

<script setup lang="ts">
import { ref, computed, getCurrentInstance, ComponentInternalInstance } from 'vue';
import { updateUserProfile } from '@/api/system/user';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  user: propTypes.any.isRequired
});

const userForm = computed(() => props.user);
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const userRef = ref<any>();
const saving = ref(false);

const rule: Record<string, any> = {
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱地址不能为空', trigger: 'blur' },
    { email: true, message: '请输入正确的邮箱地址', trigger: 'all' }
  ],
  phonenumber: [
    { required: true, message: '手机号码不能为空', trigger: 'blur' },
    { pattern: /^1[3456789][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
};

const rules = ref<Record<string, any>>(rule);

/** 提交按钮 */
const submit = async () => {
  const r = await userRef.value?.validate().catch(() => false);
  const ok = r === true || r === undefined;
  if (!ok) return;

  saving.value = true;
  try {
    await updateUserProfile(props.user);
    proxy?.$modal.msgSuccess('资料修改成功');
  } finally {
    saving.value = false;
  }
};

/** 关闭按钮 */
const close = () => {
  proxy?.$tab.closePage();
};
</script>

<style lang="scss" scoped>
.profile-edit-form {
  max-width: 600px;
  padding: 8px 0;

  :deep(.t-form__item) {
    margin-bottom: 22px;
  }

  .form-actions {
    margin-top: 10px;
  }
}
</style>
