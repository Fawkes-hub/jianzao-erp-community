export default {
  // 路由国际化
  route: {
    dashboard: '首页',
    document: '项目文档'
  },
  // 登录页面国际化
  login: {
    selectPlaceholder: '请选择/输入公司名称',
    company: '所属公司',
    username: '账号',
    usernamePlaceholder: '请输入企业账号或手机号',
    password: '登录密码',
    passwordPlaceholder: '请输入登录密码',
    login: '立即登录',
    logging: '登录中...',
    code: '验证码',
    rememberPassword: '记住账号',
    switchRegisterPage: '注册新账号',
    noAccount: '没有账号？',
    contactAdmin: '请联系管理员开通账号',
    brandTag: '社区版',
    headlinePrefix: '登录到',
    copyright: 'Copyright © 2026 简造ERP社区版',
    commercial: '商业版：生产单 / 委外 / 盘点 / 微信小程序',
    commercialHint: '微信搜索「简造AI开单-进销存」',
    headline: '采购 · 销售 · 库存 · 财务一体化',
    subhead: '基础进销存闭环 · 开源可二次开发',
    slogan: '采购 · 销售 · 库存 · 财务一体化 · 开源可二次开发',
    rule: {
      tenantId: {
        required: '请输入您的租户编号'
      },
      username: {
        required: '请输入您的账号'
      },
      password: {
        required: '请输入您的密码'
      },
      code: {
        required: '请输入验证码'
      }
    },
    social: {
      wechat: '微信登录',
      maxkey: 'MaxKey登录',
      topiam: 'TopIam登录',
      gitee: 'Gitee登录',
      github: 'Github登录'
    }
  },
  // 滑块验证器
  slider: {
    title: '安全验证',
    subtitle: '拖动下方滑块完成拼图，保障企业数据安全',
    trackHint: '向右拖动滑块完成拼图',
    footer: '行为轨迹由服务端校验',
    tipsSuccess: '验证成功',
    tipsError: '验证失败，请重试',
    trigger: '点击完成安全验证',
    verified: '安全验证已通过',
    pending: '请完成安全验证',
    open: '去验证',
    refresh: '重新验证',
    changeOne: '换一张',
    close: '关闭'
  },
  // 注册页面国际化
  register: {
    selectPlaceholder: '请选择/输入公司名称',
    username: '用户名',
    password: '密码',
    confirmPassword: '确认密码',
    register: '注 册',
    registering: '注 册 中...',
    registerSuccess: '恭喜你，您的账号 {username} 注册成功！',
    code: '验证码',
    switchLoginPage: '使用已有账户登录',
    rule: {
      tenantId: {
        required: '请输入您的租户编号'
      },
      username: {
        required: '请输入您的账号',
        length: '用户账号长度必须介于 {min} 和 {max} 之间'
      },
      password: {
        required: '请输入您的密码',
        length: '用户密码长度必须介于 {min} 和 {max} 之间',
        pattern: '不能包含非法字符：{strings}'
      },
      code: {
        required: '请完成安全验证'
      },
      confirmPassword: {
        required: '请再次输入您的密码',
        equalToPassword: '两次输入的密码不一致'
      }
    }
  },
  // 导航栏国际化
  navbar: {
    full: '全屏',
    language: '语言',
    dashboard: '首页',
    document: '项目文档',
    message: '消息',
    layoutSize: '布局大小',
    selectTenant: '选择租户',
    layoutSetting: '布局设置',
    personalCenter: '个人中心',
    logout: '退出登录'
  }
};
