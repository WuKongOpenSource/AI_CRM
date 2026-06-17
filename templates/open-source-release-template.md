# 开源版发布模板（MIT）

## 版本信息
- 版本号：`oss-vX.Y.Z`
- 发布分支：`release/oss-mit`
- 发布时间：`YYYY-MM-DD`
- 协议：MIT

## 本次变更
- 功能：
  - 
- 修复：
  - 
- 安全修复：
  - 

## 发布步骤
1. 切到开源分支：`git checkout release/oss-mit`
2. 拉取最新：`git pull`
3. 创建发布：`git tag -a oss-vX.Y.Z -m "..."`
4. 推送：`git push origin release/oss-mit --tags`
5. 发布说明同步更新

## 兼容性与迁移
- 数据库与配置变更：
  - 
- 破坏性变更：
  - 

## 验证
- [ ] 健康检查可用
- [ ] 核心流程回归通过
- [ ] 安全修复点清单覆盖
- [ ] 升级说明完整
