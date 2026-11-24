# 分页上限与 pageSize 约定

> 记录前端 `pageSize` 与后端分页上限的对应关系，避免再次出现「前端请求超限、列表空白」的故障。
> 参考事故：2026-11-24 商品资料页无法加载（见文末）。

## 一、后端有多少套上限

**不是只有一套。** 目前存在三个互不相同的限制，改分页时要先确认走的是哪一条。

| 路径 | 上限 | 定义位置 | 超限行为 |
|---|---|---|---|
| 通用分页 `PageQuery.build()` | **200** | `jianzao-common-mybatis/.../page/PageQuery.java:59` | `ServiceException`：分页参数有误：每页数量必须在1到200之间 |
| 库存模块 `StockPage.checkSize()` | **100** | `erp-stock/.../domain/bo/StockPage.java:9` | `ServiceException`：pageSize 必须在 1–100 之间 |
| 导出 `collectForExport(exportLimit, fetchPage)` | 参数上限 **5000** | `PageQuery.java:86` | `IllegalArgumentException`；内部按 200 分批拉 |

### 关键区别

- **通用分页**：`PageQuery.build()` 是所有普通列表接口的唯一入口，硬编码 200，**无控制器可绕过**（已核查全部 `*Controller.java`）。
- **库存模块**：走 `StockPage`，独立上限 100。库存日志还是**游标分页**（`beforeDate`/`beforeId`），多取一条判断 `hasMore`，不走 `PageQuery`。
- **导出**：ERP 的导出接口（`ErpProductController` 等）**不接收 `pageQuery`**，只收筛选参数，因此不受 200 限制；`collectForExport` 目前未被 ERP 模块使用，是预留能力。

## 二、前端必须遵守的规则

### 规则 1：普通列表 `pageSize` 不得超过 200

分页器默认选项已是安全值：

```ts
// src/components/Pagination/index.vue:23
pageSizes: { type: Array<number>, default: () => [10, 20, 30, 50] }
```

**不要**在页面里用 `:page-sizes` 传超过 200 的数组。当前无任何页面覆盖该默认值。

### 规则 2：库存相关接口 `pageSize` 不得超过 100

涉及 `/erp/stock/*` 的接口。库存日志下拉已限制为 `[50, 100]`：

```ts
// src/views/erp/stock/index.vue:295
const logPageSizes = [50, 100].map(...);
```

**增加选项时不能超过 100**，否则后端 `StockPage.checkSize` 直接抛 400。

### 规则 3：需要「拉全量」时用循环分页，不要放大单页

正确做法参考 `src/api/erp/master.ts:118`：

```ts
export async function listAllProductCategories(): Promise<ErpProductCategoryVO[]> {
  const rows: ErpProductCategoryVO[] = [];
  let pageNum = 1;
  while (true) {
    const page = await productCategoryOptions({ pageNum, pageSize: 100 });
    rows.push(...page.rows);
    if (!page.hasMore) return rows;
    if (page.nextPageNum == null || page.nextPageNum <= pageNum) {
      throw new Error('商品分类分页缺少有效的下一页');
    }
    pageNum = page.nextPageNum;
  }
}
```

要点：
- 每页取 100（安全区）
- 靠 `hasMore` 判断结束，**不靠返回行数**
- `nextPageNum` 必须递增校验，防止后端异常导致死循环

### 规则 4：「分组/折叠视图」的阈值必须等于后端上限

这是本次事故的直接原因。分组视图需要一次拿到全量才能正确分组，因此阈值天然受后端上限约束：

```ts
// src/views/erp/master/product/index.vue:440
/**
 * 商品折叠阈值：超过即回退普通分页列表。
 * 后端 PageQuery.MAX_PAGE_SIZE 为 200，分组模式按此阈值一次拉全量，
 * 超过 200 会被拒绝（"分页参数有误"），故必须与后端上限一致。
 */
const GROUP_LIMIT = 200;
```

**新增类似的分组/全量视图时，阈值不得超过 200；超过时自动降级为普通分页。**

## 三、当前所有 pageSize 取值清单

静态排查结论（2026-11-24），全部在安全区内：

| 取值 | 用途 |
|---|---|
| 10 | 绝大多数管理列表（系统、财务、单据） |
| 20 | 财务对账、库存余额、期初 |
| 30 | 远程下拉选择器（`ErpPagedSelect`、`SourceOrderSelect`） |
| 50 | 库存日志（可选 50/100） |
| 100 | 商品分类全量循环、分类下拉 |
| **200** | **商品分组视图阈值（`GROUP_LIMIT`）** |

**不用管的一处**：`src/components/IconSelect/index.vue:52` 的 `PAGE_SIZE = 240` 是**纯前端图标分批渲染**（2300+ 图标），不发任何请求，与后端分页无关。

## 四、排查方法（新增分页代码时自查）

```bash
# 1. 找出所有 pageSize 字面量
grep -rn "pageSize:\s*[0-9]\+" src/ --include=*.vue --include=*.ts | grep -v test

# 2. 找出变量型 pageSize，逐个溯源到定义
grep -rn "pageSize:\s*[a-zA-Z_$]" src/ --include=*.vue --include=*.ts | grep -v test

# 3. 找出分页器选项覆盖（确认没有放大）
grep -rn ":page-sizes=\|pageSizes:" src/ --include=*.vue

# 4. 后端确认上限
grep -n "MAX_PAGE_SIZE\|MAX_SIZE" \
  jianzao-backend/jianzao-common/jianzao-common-mybatis/src/main/java/org/dromara/common/mybatis/core/page/PageQuery.java \
  erp-modules/erp-stock/src/main/java/org/erp/stock/domain/bo/StockPage.java
```

## 五、事故记录：商品资料页无法加载（2026-11-24）

**现象**：进入「资料管理 / 商品资料」，表格空白显示"暂无数据"，右上角报错
「分页参数有误：每页数量必须在1到200之间」。

**原因**：
- 提交 `b04085dc` 给后端新增 `PageQuery.MAX_PAGE_SIZE = 200`（此前无上限）
- 同一次提交未同步调整前端商品页的 `GROUP_LIMIT = 500`
- 生产仅 20 个商品，`20 ≤ 500` 判定走"分组视图"分支，请求 `pageSize=500`
- 后端拒绝 → 页面无数据

**为什么测试没发现**：
- 后端单测只覆盖 `PageQuery` 本身（200 通过、201 抛错），不涉及前端传值
- 前端 234 个测试未覆盖商品页分组路径
- HTTP 226 个验收用例走的是普通分页接口，不经过分组视图

**修复**：`GROUP_LIMIT` 500 → 200（提交 `aef135d4`），并加注释说明约束来源。
**未改后端上限** —— 200 是防止白拉大数据集的安全边界，不应为前端妥协。

**教训**：前后端阈值成对修改时，必须搜索另一侧的所有硬编码常量，而不是只改报错的那一处。
