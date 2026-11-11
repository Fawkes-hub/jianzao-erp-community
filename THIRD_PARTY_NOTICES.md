# 第三方声明 / Third-Party Notices

本项目包含来自第三方的代码与组件。以下许可证原文随本项目一并分发，并保留各自原始版权声明。

本项目自研部分采用 **AGPL-3.0**（见 [LICENSE](LICENSE)）。MIT 为宽松许可证，与 AGPL-3.0 单向兼容：MIT 代码可并入 AGPL 项目，前提是保留其 MIT 版权与许可声明。以下各组件的 MIT 声明在各分发副本中继续有效。

---

## 1. RuoYi-Vue-Plus（后端底座）

- 来源：<https://gitee.com/dromara/RuoYi-Vue-Plus>，5.X，commit `815087e733eddd641dce8ee3bdb72f21e1b08e28`
- 用于：`jianzao-backend/`（含 `jianzao-common-*`、`jianzao-system`、`jianzao-admin` 等；已裁剪、重命名与本地化改造）
- 许可证：MIT
- 原始声明文件：`jianzao-backend/LICENSE`（**发布时必须原样保留，不得删改**）

```
MIT License

Copyright (c) 2019 RuoYi-Vue-Plus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### 1.1 plus-ui（前端脚手架，历史来源）

- 来源：<https://gitee.com/JavaLionLi/plus-ui>
- 用于：`jianzao-erp/` 的初始脚手架；已彻底解耦、全量迁移至 TDesign，独立维护，不再同步上游
- 许可证：MIT
- 原始声明文件：`jianzao-erp/LICENSE`（**发布时必须原样保留，不得删改**）

```
MIT License

Copyright (c) 2019 RuoYi-Vue-Plus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### 1.2 初始化 SQL

`deploy/mysql/init/01-ruoyi-system.sql` 复制自 `jianzao-backend/script/sql/`，MIT，来源同上。**发布前须在该文件头部补一行 MIT 版权与许可声明**（当前缺文件内声明）。

---

## 2. TDesign（UI 组件库）

- 来源：<https://github.com/Tencent/tdesign-vue-next>、<https://github.com/Tencent/tdesign-icons>
- 组件：`tdesign-vue-next@1.20.10`、`tdesign-icons-vue-next@0.4.12`
- 许可证：MIT
- 通过 npm/pnpm 依赖引入，未 vendored 源码到本仓库；其 LICENSE 随依赖分发

```
MIT License

Copyright (c) 2021-present TDesign

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

> 说明：使用 UI 库的组件不会对本仓库自有源码施加许可证义务；仅当把 TDesign 自身源码或图标素材直接复制进本仓库时，才需随附上述声明。

---

## 3. 其他 npm / Maven 依赖

其余依赖通过包管理器引入，各自许可证见 `package.json` / `pom.xml` 与依赖包内 LICENSE 文件。发布时请确保 `pnpm-lock.yaml` 与 Maven 依赖可正常解析。

---

## 4. ⚠️ 发布前必须处理的授权未明项

以下素材当前**授权状态不明或缺乏许可证声明**，在源码导入前必须替换、取得授权或移除，不得随开源仓分发：

| 项 | 位置 | 问题 |
|---|---|---|
| 天爱验证码 SDK | `jianzao-erp/public/tac/js/tac.min.js`、`tac.css`、`images/dun.jpeg` | 压缩 SDK 无任何 license / `@license` / copyright 头 |
| 登录背景图 | `jianzao-erp/src/assets/images/login-background.jpg` | 来源与授权不明 |
| 默认头像 | `jianzao-erp/src/assets/images/profile.jpg` | 来源与授权不明 |
| 第三方品牌图标 | `jianzao-erp/src/assets/icons/svg/` 中 wechat / qq / github / gitee / maxkey / topiam / swagger / druid 等 | 商标与字形授权混杂 |
| 错误页插画 | `jianzao-erp/src/assets/401_images/401.gif`、`404_images/*` | RuoYi 时代遗留，需核实上游授权并补归因 |

字体无风险：全站为系统字体栈（MiSans / HarmonyOS Sans SC / PingFang SC / Noto Sans SC / Microsoft YaHei），未引入任何 Web 字体文件。

---

## 5. 发布前核对清单

- [ ] `jianzao-backend/LICENSE` 原文保留
- [ ] `jianzao-erp/LICENSE` 原文保留
- [ ] `deploy/mysql/init/01-ruoyi-system.sql` 补 MIT 头
- [ ] `VENDOR.md`（上游来源与本地改动清单）保留
- [ ] §4 授权未明项全部替换或移除
- [ ] 根 `LICENSE` 为 AGPL-3.0 官方全文（未修改）
- [ ] `package.json` 的 `license` 字段同步为 `AGPL-3.0`
- [ ] 新增自研源码文件头标注版权与 AGPL-3.0（建议，非强制）
