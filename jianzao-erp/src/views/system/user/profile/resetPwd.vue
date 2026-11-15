<template>
  <t-form ref="pwdRef" :data="user" :rules="rules" label-align="right" label-width="84px" class="profile-pwd-form">
    <!-- 密码归全局账户（account），不再按企业各存一套 -->
    <t-alert class="mb-4" theme="info" message="修改后立即生效，下次登录请使用新密码。请勿与其他系统复用同一密码。" />

    <t-form-item label="旧密码" name="oldPassword">
      <t-input v-model="user.oldPassword" placeholder="请输入旧密码" type="password" clearable style="width: 380px" />
    </t-form-item>
    <t-form-item label="新密码" name="newPassword">
      <t-input v-model="user.newPassword" placeholder="请输入 6~20 位新密码" type="password" clearable style="width: 380px" />
    </t-form-item>
    <t-form-item label="确认密码" name="confirmPassword">
      <t-input v-model="user.confirmPassword" placeholder="请再次输入新密码" type="password" clearable style="width: 380px" />
    </t-form-item>
    <t-form-item class="form-actions">
      <t-space size="12px">
        <t-button theme="primary" :loading="saving" @click="submit">确认修改</t-button>
        <t-button theme="default" variant="base" @click="close">关闭</t-button>
      </t-space>
    </t-form-item>
  </t-form>
</template>

<script setup lang="ts">
import { ref, getCurrentInstance, ComponentInternalInstance } from 'vue';
import { updateUserPwd } from '@/api/system/user';
import type { ResetPwdForm } from '@/api/system/user/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const pwdRef = ref<any>();
const saving = ref(false);

const user = ref<ResetPwdForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

/** 确认密码一致性 */
const equalToPassword = (val: string) => {
  if (user.value.newPassword !== val) {
    return { result: false, message: '两次输入的密码不一致' };
  }
  return true;
};

const rules: Record<string, any> = {
  oldPassword: [{ required: true, message: '旧密码不能为空', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符之间', trigger: 'blur' },
    { pattern: /^[^<>"'|\\]+$/, message: '不能包含非法字符：< > " \' \\ |', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ]
};

/** 提交按钮 */
const submit = async () => {
  const result = await pwdRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;

  saving.value = true;
  try {
    await updateUserPwd(user.value.oldPassword, user.value.newPassword);
    proxy?.$modal.msgSuccess('密码修改成功');
    user.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
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
.profile-pwd-form {
  max-width: 600px;
  padding: 8px 0;

  :deep(.t-alert) {
    margin-bottom: 24px;
    border-radius: var(--app-radius-md);
  }

  :deep(.t-form__item) {
    margin-bottom: 22px;
  }

  .form-actions {
    margin-top: 10px;
  }
}
</style>
