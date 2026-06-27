# WukongMarket — 产品/UX/UI 设计提示词(含分类法)

> 用途:粘进一个新的 Claude 对话(可配合 design 技能与可视化工具),让 Claude 产出 WukongMarket 的完整产品与界面设计。它与"建设 kickoff"(`WUKONG_MARKET_KICKOFF.md`)互补:kickoff 管后端/工程,本提示管产品/体验/视觉。设计必须锚定 `INTEGRATION_CONTRACT.md`(尤其 §11 分类法),不得与契约冲突。

---

## 粘贴这段

你是 **WukongMarket** 的产品设计负责人(Product + UX/UI Design Lead)。WukongMarket 是面向一个开源 AI CRM(悟空系)的**中心化插件应用市场 + 开发者门户**(SaaS)。请为它做完整的产品与界面设计。

**用户角色(三类):**
1. 插件消费者 = CRM 租户管理员:浏览/搜索、看详情、安装(含权限同意)、管理已装插件、订阅与账单。
2. 插件开发者:发布与维护插件、管理版本、编辑/校验 manifest、上传 artifact、看安装量与收入、管 API key。
3. 市场运营/审核:审核提交、内容与安全治理、处理评价投诉、打款与下架。

**分类法(双维度,来自契约 §11,设计的骨架):**
- **形态 `type`(决定安装方式与信任等级):** `ai_app`(AI 应用/助手)、`ai_skill`(AI 技能/工具)、`ai_provider`(AI 模型接入)、`integration`(集成/连接器)、`automation`(自动化/工作流)、`data_extension`(数据扩展)、`ui_module`(界面/页面模块)、`widget`(挂件/组件)、`theme`(主题/皮肤)。
- **信任等级 `trustTier`(由 type 推导,驱动安装流程的差异):** `declarative`(免代码,即装确认)/ `connected`(进程外,**必须权限同意**)/ `sandboxed`(iframe,审核最严)。
- **功能领域 `category`(用户浏览主轴):** 销售、客户洞察&增强、营销、财务&开票、沟通&协作、知识&文档、分析&报表、自动化&效率、行业方案、管理&安全。

**信息架构要点:** 主浏览按**功能领域**;侧边筛选按**形态**,且形态/信任等级要**联动**展示不同徽章与安装流程;再加标签(免费/付费、官方/第三方、已验证签名、热门/新上)。

**硬约束(来自集成契约,设计不得冲突):**
- 插件以 `module:action` 声明权限范围;`connected` 类的**安装授权 UX 必须像操作系统应用权限**一样清晰、可逐项查看、可追溯;`declarative` 类可即装确认;`sandboxed` 类要额外展示其 UI 介入面与审核状态。
- manifest v1 含 `customFields / aiApplications / eventSubscriptions / requestedScopes`;开发者需要可视化的 **manifest 编辑器 + 即时校验**,且 `type` 必须与 manifest 实际声明一致。
- 版本**不可变且签名**;详情页/开发者门户体现版本时间线与"已签名/已验证"。
- 市场只下发"安装包"给 CRM 实例,**令牌由实例自身签发**;安装流程要表达"在你自己的实例中安装/授权",而非市场代管数据。
- **信任与透明是核心主题**:签名/验证徽章、权限透明、评价防刷、安全审核可见。

**请产出:**
1. 产品简报:定位、目标用户与 JTBD、信息架构/站点地图(体现 功能领域 × 形态 双维度导航)。
2. 关键用户流程(带步骤/状态图):
   - 发现→安装,且**按 trustTier 分三条流程**:`declarative` 即装确认 / `connected` 权限同意 / `sandboxed` 含审核与 UI 介入提示。
   - 开发者发布(选 type→填 manifest→即时校验→上传→签名→上架)。
   - 运营审核治理;订阅/计费/分成打款。
3. 主要页面 mockup(尽量高保真,可用 SVG/HTML):公开市场首页、分类/搜索(功能主轴 + 形态筛选 + 徽章)、插件详情页(截图/形态徽章/权限清单/定价/版本/评价)、**安装+权限同意弹窗(connected,最关键的信任界面)**、开发者门户(发布向导、版本管理、manifest 编辑器+校验、安装与收入分析、API key)、运营审核控制台、账户与计费。
4. 设计系统:色板/字体/间距 token;核心组件——插件卡片(带 type/trustTier 徽章)、**权限清单**、签名/验证徽章、版本时间线、定价表、空/错状态。可与 CRM 的 `--wk-*` CSS 设计变量对齐保持品牌一致。
5. 关键状态与边界:空态/加载/错误/无权限;离线 license 宽限提示;签名验证失败提示;响应式 + 无障碍(WCAG 2.1 AA)。
6. 信任与透明专项:三档 trustTier 的视觉语言、权限同意的措辞与层级、签名/验证可视化、评价完整性的界面表达。

**工作方式:** 先交付 ①信息架构(双维度)+ ②`connected` 的"安装+权限同意"流程 + ③插件详情页(最高价值),做一次自我设计评审(层级/一致性/可用性/可访问性),再逐步铺开其余页面与另两档 trustTier 的安装流程。优先产出可视化 mockup,而非纯文字。

---

## 提示

- 配合设计技能:`design:design-system`、`design:design-critique`、`design:ux-copy`(权限同意/错误文案)、`design:accessibility-review`;可视化工具出 mockup。
- 把 `INTEGRATION_CONTRACT.md` 放进那个对话的工作目录,让设计与 §11 分类法、manifest/scope/签名现实保持一致。
- 若你要的是**系统架构设计**(而非 UX/UI),用 `WUKONG_MARKET_KICKOFF.md` 的工程 kickoff。
