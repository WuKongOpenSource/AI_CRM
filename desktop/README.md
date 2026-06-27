# 悟空 AI CRM 桌面客户端（Electron）

跨平台桌面客户端（Windows + macOS）。把 Vue SPA 打包进来，并内嵌一个本地“静态 + 反向代理”服务：
本地伺服前端，同时把 `/crmapi`、`/syncapi`、`/s3`、`/ws`(WebSocket) 反代到后端。IM 实时消息、AI 对话
SSE、MinIO 文件预览都与网页版一致，**前端零改动**。

## 两种运行模式（首次启动让用户选，托盘可随时切换）

- **本地后端（自带数据库）** —— 启动时 spawn 内置 JRE 运行后端 jar，jar 内含**嵌入式 Postgres + Redis**，
  文件存本地。数据全在本机（`<userData>/backend-data`：`pgdata`/`uploads`/`logs`），无需服务器或 Docker。
  除调用 LLM 外完全本地。
- **云端服务器** —— 瘦客户端，反代到你部署的 CRM 地址（docker 的 nginx 入口），多设备实时同步。

模式存于 `<userData>/config.json`；可用环境变量 `CRM_BACKEND_MODE=local|remote`、`CRM_SERVER_URL=...` 覆盖。

```
desktop/
├── electron/
│   ├── main.cjs            # 主进程：内嵌反代 + 本地后端 spawn + 模式选择 + 托盘
│   ├── server.cjs          # 静态伺服 + 反向代理（本地模式剥 /crmapi 前缀）
│   ├── splash.html         # 本地后端启动页
│   ├── prompt.html         # 服务器地址设置弹窗
│   └── prompt-preload.cjs
├── scripts/
│   ├── sync-renderer.mjs   # frontend/dist -> app/
│   ├── fetch-jre.mjs       # 按系统/架构下载 Temurin 21 JRE -> resources/jre
│   └── make-icon.mjs       # 生成 build/icon.ico + icon.png
├── resources/
│   ├── backend/crm-desktop.jar   # 后端 jar（git 忽略；mvn -Pdesktop 产出，见下）
│   └── jre/                       # 内置 JRE（git 忽略；fetch:jre 产出）
├── app/                    # 打包进来的 SPA（git 忽略）
└── package.json
```

## 后端 jar 怎么来（两端共用同一个）

后端 jar 是**一份通吃**——zonky 嵌入式 Postgres 与 embedded-redis 会按运行时系统/架构自动挑选二进制。
在仓库根的 `backend/` 下用 desktop profile 构建（本机无 Java 时可用 Docker 的 maven 镜像）：

```bash
cd backend
mvn -Pdesktop -DskipTests package      # 产出 target/crm-1.0.0.jar（含 windows + macОS 两架构 PG 二进制）
cp target/crm-1.0.0.jar ../desktop/resources/backend/crm-desktop.jar
```
> 需要在 Linux 上跑该 jar 时，把 `embedded-postgres-binaries-linux-amd64` 加回 `backend/pom.xml` 的 desktop profile。

## 本机开发运行
```bash
cd desktop
npm install
npm run sync:renderer            # 需先有 frontend/dist（在 frontend 下 npm run build）
npm run fetch:jre                # 下载当前平台 JRE
CRM_BACKEND_MODE=local npm start # 本地后端模式（mac/linux 写法）
#  PowerShell: $env:CRM_BACKEND_MODE="local"; npm start
```

## 打包

### Windows（可在 Windows 本机打）
```bash
cd desktop
npm install
npm run dist:win        # = bundle + fetch:jre + make:icon + electron-builder --win
```
产物 `desktop/release/`：`AI CRM Setup <版本>.exe`（安装版）、`AI CRM <版本>.exe`（portable）。

### macOS（必须在 Mac 上，或用下面的 GitHub Actions）
```bash
cd desktop
npm install
npm run dist:mac        # 自动拉 macOS JRE，产出 .dmg / .zip（Apple 芯片为 arm64）
```
> macOS 安装包**无法从 Windows 交叉构建**（需生成 .app/.dmg、签名/公证）。未签名的包用户需右键→打开绕过 Gatekeeper；
> 正式分发需 Apple 开发者账号做签名+公证。

### 没有 Mac？用 GitHub Actions 云端构建
仓库已带 `.github/workflows/desktop-build.yml`：在 macOS + Windows runner 上从源码构建后端 jar、前端、
拉对应 JRE 并打包。把代码推到 GitHub 后：
- 进 **Actions → Build desktop app → Run workflow**（或推一个 `v*` tag 触发）。
- 跑完在该次运行的 **Artifacts** 里下载 `desktop-macos-14`（`.dmg`/`.zip`）和 `desktop-windows-latest`（`.exe`）。

CI 全部从源码构建，**无需提交 jar / app / jre**（都 git 忽略）。

## 备注
- 本地模式默认后端端口 8088；若本机同时跑着 docker 的 `crm` 容器会冲突，先 `docker stop crm`。
- 退出时会连同嵌入式 Postgres/Redis 子进程一起结束（Windows 用 `taskkill /T`，mac 用 SIGTERM 触发 JVM 关停钩子）。
- 后端 CORS 已全开，但客户端通过本地反代统一同源，不依赖 CORS。
