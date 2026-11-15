<template>
  <div class="profile-container">
    <div class="profile-layout">
      <!-- 左侧：个人概览卡片 -->
      <div class="profile-sidebar">
        <div class="erp-surface erp-panel profile-user-card">
          <div class="user-avatar-wrap">
            <userAvatar />
          </div>
          <div class="user-brief">
            <div class="user-nickname">{{ state.user.nickName || state.user.userName || '用户' }}</div>
            <div class="user-account">@{{ state.user.userName }}</div>
            <div class="user-tags">
              <t-tag theme="primary" variant="light" size="small">{{ state.roleGroup || '普通用户' }}</t-tag>
              <t-tag v-if="state.user.deptName" theme="default" variant="light" size="small">{{ state.user.deptName }}</t-tag>
            </div>
          </div>

          <div class="profile-divider" />

          <div class="profile-meta-list">
            <div class="meta-item">
              <div class="meta-label">
                <UserIcon class="meta-icon" />
                <span>用户名称</span>
              </div>
              <div class="meta-value">{{ state.user.userName || '-' }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">
                <MobileIcon class="meta-icon" />
                <span>手机号码</span>
              </div>
              <div class="meta-value">{{ state.user.phonenumber || '-' }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">
                <MailIcon class="meta-icon" />
                <span>用户邮箱</span>
              </div>
              <div class="meta-value">{{ state.user.email || '未绑定' }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">
                <ViewOrganizationIcon class="meta-icon" />
                <span>所属部门</span>
              </div>
              <div class="meta-value">
                {{ state.user.deptName || '-' }} <span v-if="state.postGroup">/ {{ state.postGroup }}</span>
              </div>
            </div>
            <div class="meta-item">
              <div class="meta-label">
                <UsergroupIcon class="meta-icon" />
                <span>所属角色</span>
              </div>
              <div class="meta-value">{{ state.roleGroup || '-' }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">
                <TimeIcon class="meta-icon" />
                <span>创建日期</span>
              </div>
              <div class="meta-value">{{ state.user.createTime || '-' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：设置 Tab 面板 -->
      <div class="profile-main">
        <div class="erp-surface erp-panel profile-tabs-card">
          <div class="erp-panel__head">
            <div>
              <span class="erp-panel-title">账号设置</span>
              <span class="erp-panel-sub">管理个人基本资料、密码及登录设备</span>
            </div>
          </div>

          <t-tabs v-model="activeTab" theme="normal" size="medium" class="profile-tabs">
            <t-tab-panel label="基本资料" value="userinfo">
              <div class="tab-pane-wrap">
                <userInfo :user="userForm" />
              </div>
            </t-tab-panel>
            <t-tab-panel label="修改密码" value="resetPwd">
              <div class="tab-pane-wrap">
                <resetPwd />
              </div>
            </t-tab-panel>
            <t-tab-panel label="在线设备" value="onlineDevice">
              <div class="tab-pane-wrap">
                <onlineDevice :devices="state.devices" />
              </div>
            </t-tab-panel>
          </t-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="Profile" lang="ts">
import { ref, onMounted } from 'vue';
import { UserIcon, MobileIcon, MailIcon, ViewOrganizationIcon, UsergroupIcon, TimeIcon } from 'tdesign-icons-vue-next';
import UserAvatar from './userAvatar.vue';
import UserInfo from './userInfo.vue';
import ResetPwd from './resetPwd.vue';
import OnlineDevice from './onlineDevice.vue';
import { getUserProfile } from '@/api/system/user';
import { getOnline } from '@/api/monitor/online';
import { UserVO } from '@/api/system/user/types';

const activeTab = ref('userinfo');

interface State {
  user: Partial<UserVO>;
  roleGroup: string;
  postGroup: string;
  devices: any;
}

const state = ref<State>({
  user: {},
  roleGroup: '',
  postGroup: '',
  devices: []
});

const userForm = ref({});

const getUser = async () => {
  const res = await getUserProfile();
  state.value.user = res.data.user;
  userForm.value = { ...res.data.user };
  state.value.roleGroup = res.data.roleGroup;
  state.value.postGroup = res.data.postGroup;
};

const getOnlines = async () => {
  const res = await getOnline();
  state.value.devices = res.rows;
};

onMounted(() => {
  getUser();
  getOnlines();
});
</script>

<style lang="scss" scoped>
.profile-container {
  padding: 16px;
  background-color: transparent;
}

.profile-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;

  @media (max-width: 992px) {
    flex-direction: column;
  }
}

.profile-sidebar {
  width: 330px;
  flex-shrink: 0;

  @media (max-width: 992px) {
    width: 100%;
  }
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 20px;
  min-height: 520px;

  .user-avatar-wrap {
    margin-bottom: 14px;
  }

  .user-brief {
    text-align: center;
    width: 100%;

    .user-nickname {
      font-size: 18px;
      font-weight: 600;
      color: var(--td-text-color-primary);
      line-height: 1.4;
    }

    .user-account {
      font-size: 13px;
      color: var(--td-text-color-placeholder);
      margin-top: 2px;
      margin-bottom: 10px;
    }

    .user-tags {
      display: flex;
      justify-content: center;
      gap: 6px;
      flex-wrap: wrap;
    }
  }

  .profile-divider {
    width: 100%;
    height: 1px;
    background-color: var(--td-border-level-1-color);
    margin: 20px 0 16px;
  }

  .profile-meta-list {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .meta-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 6px 0;
      font-size: 13px;
      border-bottom: 1px dashed var(--td-border-level-1-color);

      &:last-child {
        border-bottom: none;
      }

      .meta-label {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        color: var(--td-text-color-secondary);

        .meta-icon {
          font-size: 15px;
          color: var(--td-text-color-placeholder);
        }
      }

      .meta-value {
        font-weight: 500;
        color: var(--td-text-color-primary);
        max-width: 60%;
        text-align: right;
        word-break: break-all;
      }
    }
  }
}

.profile-tabs-card {
  padding: 20px 24px 28px;
  min-height: 520px;

  .profile-tabs {
    margin-top: 4px;

    :deep(.t-tabs__nav-container) {
      margin-bottom: 16px;
    }
  }

  .tab-pane-wrap {
    padding-top: 12px;
  }
}
</style>
