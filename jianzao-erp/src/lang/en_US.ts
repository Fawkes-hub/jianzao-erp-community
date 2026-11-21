export default {
  // 路由国际化
  route: {
    dashboard: 'Dashboard',
    document: 'Document'
  },
  // 登录页面国际化
  login: {
    selectPlaceholder: 'Please select/enter a company name',
    company: 'Company',
    username: 'Account',
    usernamePlaceholder: 'Enter your account or mobile number',
    password: 'Password',
    passwordPlaceholder: 'Enter your password',
    login: 'Sign in',
    logging: 'Signing in...',
    code: 'Verification Code',
    rememberPassword: 'Remember account',
    switchRegisterPage: 'Create an account',
    noAccount: 'No account yet? ',
    contactAdmin: 'Please contact your administrator',
    brandTag: 'Light manufacturing suite',
    headlinePrefix: 'Sign in to',
    copyright: 'Copyright © 2026 Jianzao ERP',
    commercial: 'Commercial edition: production, subcontracting, stocktake and mini program',
    commercialHint: 'Search "Jianzao AI Ordering" on WeChat',
    headline: 'Every purchase, sale and stock move, accounted for',
    subhead: 'Purchasing, sales, inventory and receivables in one document trail',
    rule: {
      tenantId: {
        required: 'Please enter your tenant id'
      },
      username: {
        required: 'Please enter your account'
      },
      password: {
        required: 'Please enter your password'
      },
      code: {
        required: 'Please enter a verification code'
      }
    },
    social: {
      wechat: 'Wechat Login',
      maxkey: 'MaxKey Login',
      topiam: 'TopIam Login',
      gitee: 'Gitee Login',
      github: 'Github Login'
    }
  },
  // Slider captcha
  slider: {
    title: 'Security check',
    subtitle: 'Slide to fit the puzzle piece — keeps company data safe',
    trackHint: 'Slide right to fit the puzzle',
    footer: 'Drag trajectory verified on the server',
    tipsSuccess: 'Verified',
    tipsError: 'Verification failed, please retry',
    trigger: 'Click to complete the security check',
    verified: 'Security check passed',
    pending: 'Please complete the security check',
    open: 'Verify',
    refresh: 'Retry',
    changeOne: 'New puzzle',
    close: 'Close'
  },
  // 注册页面国际化
  register: {
    selectPlaceholder: 'Please select/enter a company name',
    username: 'Username',
    password: 'Password',
    confirmPassword: 'Confirm Password',
    register: 'Register',
    registering: 'Registering...',
    registerSuccess: 'Congratulations, your {username} account has been registered!',
    code: 'Verification Code',
    switchLoginPage: 'Log in with an existing account',
    rule: {
      tenantId: {
        required: 'Please enter your tenant id'
      },
      username: {
        required: 'Please enter your account',
        length: 'The length of the user account must be between {min} and {max}'
      },
      password: {
        required: 'Please enter your password',
        length: 'The user password must be between {min} and {max} in length',
        pattern: "Can't contain illegal characters: {strings}"
      },
      code: {
        required: 'Please complete verification'
      },
      confirmPassword: {
        required: 'Please enter your password again',
        equalToPassword: 'The password entered twice is inconsistent'
      }
    }
  },
  // 导航栏国际化
  navbar: {
    full: 'Full Screen',
    language: 'Language',
    dashboard: 'Dashboard',
    document: 'Document',
    message: 'Message',
    layoutSize: 'Layout Size',
    selectTenant: 'Select Tenant',
    layoutSetting: 'Layout Setting',
    personalCenter: 'Personal Center',
    logout: 'Logout'
  }
};
