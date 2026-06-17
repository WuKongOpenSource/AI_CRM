# README 模板（开源版）

## AI CRM (Open Source Edition)

- License: MIT
- 分支: `release/oss-mit`

### 启动
```bash
git checkout release/oss-mit
docker compose -f docker/docker-compose.yaml up -d --build
```

### 说明
这是开源版本发布线，仅覆盖 MIT 兼容能力。商业能力请查看 Enterprise 分支。

### 运行检查
- 前端：http://localhost
- 健康接口：按 docker 配置中的端口访问
