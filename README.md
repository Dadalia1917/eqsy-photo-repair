# e起守忆影像修复平台（社区志愿者版）

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/JDK-17-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)]()
[![Vue](https://img.shields.io/badge/Vue-3.x-42b883.svg)]()
[![uni-app](https://img.shields.io/badge/uni--app-Vue3-2B9939.svg)]()

---

## 目录

- 项目简介
- 技术栈
- 项目结构
- 系统架构图
- UML 用例图
- 核心时序图
- 领域类图
- 数据库 ER 图
- 部署架构图（Docker）
- 业务流程
- 快速开始
- Docker 部署
- 项目背景说明
- 更新记录

---

## 项目简介

e起守忆影像修复平台是面向社区老人的公益修复系统，采用统一志愿者处理模式：

- 老人在小程序上传老照片并填写需求。
- 任务直接进入学生志愿者处理队列。
- 志愿者在后台认领、修复并回传结果。
- 老人端刷新后预览并保存修复成果。

当前版本已经移除 AI/人工选择分支，交互更简单，适老化更强。

当前 README 对应版本：`v1.2.5`

---

## 技术栈

### 后端

- Spring Boot 3.5.x
- Spring Security + JWT
- MyBatis
- MySQL 8
- Redis 7

### 前端

- ruoyi-ui: Vue3 + Element Plus + Vite
- ruoyi-app: uni-app (微信小程序/H5)

### 运维部署

- Docker
- Docker Compose
- Nginx

---

## 项目结构

- ruoyi-admin: Spring Boot 启动与 Web 接口层
- ruoyi-system: 修复任务等核心业务模块
- ruoyi-framework: 安全与权限框架
- ruoyi-common: 公共组件与工具
- ruoyi-ui: 后台管理前端
- ruoyi-app: 小程序前端
- ruoyi-docker: 容器化部署目录
- sql/ry_eqsy_repair.sql: 初始化脚本

---

## 系统架构图

```mermaid
graph TB
    subgraph Client[客户端层]
        Mini[微信小程序 ruoyi-app]
        AdminWeb[后台管理 ruoyi-ui]
    end

    subgraph Server[服务层]
        Java[ruoyi-admin Spring Boot]
        Redis[(Redis)]
        MySQL[(MySQL)]
        Upload[(文件存储 UploadPath)]
    end

    Mini -->|HTTP/JWT| Java
    AdminWeb -->|HTTP/JWT| Java
    Java --> Redis
    Java --> MySQL
    Java --> Upload
```

---

## UML 用例图

```mermaid
graph LR
    Elder[老人用户]
    Student[学生志愿者]
    Admin[管理员]

    U1((上传照片))
    U2((提交修复需求))
    U3((刷新查看结果))
    U4((保存到相册))

    U5((认领任务))
    U6((上传修复结果))
    U7((完成回传))

    U8((任务监管))
    U9((用户与权限管理))

    Elder --> U1
    Elder --> U2
    Elder --> U3
    Elder --> U4

    Student --> U5
    Student --> U6
    Student --> U7

    Admin --> U8
    Admin --> U9
```

---

## 核心时序图

### 提交与处理时序

```mermaid
sequenceDiagram
    participant E as 老人小程序
    participant J as Java后端
    participant DB as MySQL
    participant S as 学生后台

    E->>J: 提交任务(照片+remark)
    J->>DB: 写入 RepairTask (WAIT_STUDENT)
    J-->>E: 返回 taskId

    S->>J: 查询待认领任务
    J->>DB: 查询 WAIT_STUDENT
    J-->>S: 返回任务列表

    S->>J: 认领任务
    J->>DB: 更新 student_id/status
    J-->>S: 认领成功

    S->>J: 上传修复结果
    J->>DB: 写入 result_urls

    S->>J: 完成回传
    J->>DB: 更新状态 COMPLETED

    E->>J: 刷新处理结果
    J->>DB: 查询最新任务
    J-->>E: 返回结果图片
```

---

## 领域类图

```mermaid
classDiagram
    class RepairTask {
      +Long taskId
      +String taskNo
      +Long userId
      +String userName
      +String repairMode
      +String taskType
      +String sourceType
      +String sourceUrls
      +String remark
      +String resultUrls
      +String status
      +Integer progress
      +Long studentId
      +String studentName
      +Date createTime
      +Date finishedTime
    }

    class AppRepairTaskController {
      +submit(body)
      +list(query)
      +detail(taskId)
    }

    class RepairTaskController {
      +list(query)
      +claim(taskId)
      +uploadResult(body)
      +finish(taskId)
    }

    class IRepairTaskService {
      +submitTask(task,userId,userName)
      +claimTask(taskId,studentId,studentName)
      +uploadManualResult(taskId,studentId,resultUrls)
      +finishManualTask(taskId,studentId)
    }

    class RepairTaskServiceImpl
    class RepairTaskMapper

    AppRepairTaskController --> IRepairTaskService
    RepairTaskController --> IRepairTaskService
    RepairTaskServiceImpl ..|> IRepairTaskService
    RepairTaskServiceImpl --> RepairTaskMapper
    RepairTaskMapper --> RepairTask
```

---

## 数据库 ER 图

```mermaid
erDiagram
    SYS_USER ||--o{ REPAIR_TASK : "submit/claim"

    SYS_USER {
      bigint user_id PK
      varchar user_name
      varchar phonenumber
    }

    REPAIR_TASK {
      bigint task_id PK
      varchar task_no
      bigint user_id FK
      varchar user_name
      varchar repair_mode
      varchar task_type
      varchar source_type
      text source_urls
      text remark
      text result_urls
      varchar status
      int progress
      bigint student_id
      varchar student_name
      datetime create_time
      datetime finished_time
    }
```

---

## 部署架构图（Docker）

```mermaid
graph LR
    User[浏览器/小程序] --> Nginx[ruoyi-ui Nginx]
    Nginx --> Java[ruoyi-admin]
    Java --> MySQL[(MySQL)]
    Java --> Redis[(Redis)]
    Java --> Volume[(Upload Volume)]
```

---

## 业务流程

1. 小程序上传照片。
2. 填写留言需求并提交。
3. 后台学生认领并处理。
4. 上传修复结果，完成回传。
5. 小程序刷新查看并保存。

---

## 快速开始

### 1) 启动后端

```powershell
mvn clean package -DskipTests
cd ruoyi-admin
mvn spring-boot:run
```

### 2) 启动后台前端

```powershell
cd ruoyi-ui
npm install
npm run dev
```

### 3) 小程序调试

- 使用 HBuilderX 打开 ruoyi-app，点击「运行 → 小程序-微信」。
- `ruoyi-app/config.js` 同时保留了本地与云端两套地址：
  - `localBaseUrl=http://127.0.0.1:8080`
  - `cloudBaseUrl=https://ruoyi-backend.inmind-lab.com`
- 开发环境通过单个开关 `useCloudInDev` 控制整套地址：
  - `false`：前后端联调、图片/视频资源均走本地 `localhost`
  - `true`：开发环境整套改走云端 `https`
- 当前 `baseUrl` 与 `staticUrl` 保持一致，避免接口地址与图片/视频资源地址分离后不便切换。
- **发行正式版前**：微信小程序线上版要求 HTTPS，需在服务器配置 nginx + SSL 证书，并确保 `config.js` 中的 `cloudBaseUrl` 指向可访问的 HTTPS 后端地址，同时在微信公众平台配置服务器域名白名单。

### 4) uploadPath 目录说明

- 后端上传目录由 `ruoyi.profile` 决定，接口返回的文件相对路径统一为 `/profile/upload/...`。
- **Windows 本地开发**建议使用本地目录，例如：`D:/ruoyi/uploadPath`
- **Linux 服务器部署**保留服务器目录，例如：`/home/ruoyi/uploadPath`
- 当前已支持按运行系统自动切换：Windows 本地运行自动使用 `windowsProfile`，Linux 服务器运行自动使用 `linuxProfile`，无需手动修改配置文件。
- 如果数据库中的图片路径沿用历史记录，但本地 `uploadPath` 下没有对应文件，访问 `/profile/upload/...` 时会出现 404；此时需同步上传目录中的实际文件。

---

## Docker 部署

Docker 目录位于 ruoyi-docker，已包含：

- docker-compose.yml
- ruoyi-admin/Dockerfile
- ruoyi-ui/Dockerfile
- nginx/default.conf
- .env.example

### 部署步骤

```powershell
cd ruoyi-docker
Copy-Item .env.example .env
# 编辑 .env，填入真实 WECHAT_SECRET
docker compose up -d --build
```

### Docker 更新后端代码后的重建步骤

- 如果你修改了 `ruoyi-admin` 的 Java 代码（例如登录、权限、接口、数据库兜底逻辑），仅重新打开小程序**不会生效**，因为 Docker 里的后端镜像还是旧版本。
- 需要在 `ruoyi-docker` 目录重新构建并重启 `ruoyi-admin` 容器：

```powershell
cd ruoyi-docker
docker compose build ruoyi-admin
docker compose up -d ruoyi-admin
```

- 如果你同时改了前端小程序代码，还需要在 HBuilderX 里重新运行/重新发行 `ruoyi-app`。
- 如果你使用的是已有 MySQL 数据卷，`mysql-init` 目录下的初始化 SQL **不会自动重跑**；这类场景应依赖业务代码兜底或手动执行升级 SQL。

### Docker 上传目录说明

- `ruoyi-docker/docker-compose.yml` 中默认通过 `RUOYI_PROFILE` 指定容器内上传根目录，默认值为 `/data/ruoyi/uploadPath`。
- 同一个 `RUOYI_PROFILE` 也会用于 Docker 卷挂载路径，确保后端配置与容器卷目录保持一致。
- 如果你需要修改 Docker 环境下的上传目录，请同时修改 `.env` 中的 `RUOYI_PROFILE`，不要只改其中一处。
- Docker 部署场景下，`RUOYI_PROFILE` 会作为显式覆盖项生效，优先级高于 Windows/Linux 自动切换配置。

**历史 v1.2.0 升级步骤**：
- 复制 `.env.example` 为 `.env` 后，将 `WECHAT_SECRET` 替换为真实密钥（已预填 AppID，只需填 Secret）。
- `mysql-init/ry_eqsy_repair.sql` 中已包含 `wx_openid` 列，全新部署无需额外执行迁移脚本。
- 已有数据库（非 Docker 初始化）：打开 `sql/ry_eqsy_repair.sql`，将文件末尾注释掉的两行 `ALTER TABLE` 取消注释并单独运行（仅执行一次）。

### 默认访问

- 后台前端: http://服务器IP:80
- 后端接口: http://服务器IP:8080

---

## 项目背景说明

- 基于 RuoYi 框架进行二次开发。
- 服务于江苏省常州市武进区社区志愿者活动场景。
- 由江苏理工学院委托开发。

---

## 更新记录

### 2026-04-01（v1.2.5）

- 修复小程序请求地址不符合微信规范问题：微信小程序线上/真机预览环境只允许 HTTPS 请求，`http://127.0.0.1` 在该环境完全不可用，需在微信开发平台添加域名白名单。
- `ruoyi-app/config.js` 中 `useCloudInDev` 由 `false` 改为 `true`，确保开发与生产环境均使用 `cloudBaseUrl`（`https://ruoyi-backend.inmind-lab.com`）。
- 若需本地联调，可临时将 `useCloudInDev` 改回 `false`，并在微信开发者工具中勾选「不校验合法域名」绕过限制。
- `appInfo.version` 同步更新为 `1.2.5`。

### 2026-03-31（v1.2.3）

- 修复微信一键登录报错：生产数据库缺少 `wx_openid` 列导致所有用户查询崩溃，已将迁移语句以注释形式内置于 `sql/ry_eqsy_repair.sql` 末尾（取消注释单独运行两行 `ALTER` 即可）。
- 修复密码登录成功后页面不跳转、无任何反馈的问题：`login.vue` 的 `loginSuccess()` 补充 `getInfo()` 加载状态与 `.catch()` 错误提示。
- 修复 `font-size: undefinedpx` 控制台警告：`avatar/index.vue` 模块层改用 `uni.getWindowInfo()` 获取屏幕信息，并加兜底默认值。
- 修复 `wx.getSystemInfoSync is deprecated` 废弃 API 警告：同上，已替换为新 API。
- 消除 `App.vue` LifeCycle 加载风险：移除顶层 `getCurrentInstance()` 调用，`checkLogin()` 整体收进 `#ifdef H5` 块，改用 `uni.reLaunch()` 原生导航。
- 补充 Docker 场景下后端代码更新后的重建步骤：修改 `ruoyi-admin` 后需执行 `docker compose build ruoyi-admin` 与 `docker compose up -d ruoyi-admin` 使修复真正生效。

### 2026-03-31（v1.2.2）

- README 更新到 `v1.2.2`。
- 补充 `ruoyi-app/config.js` 当前地址切换方式：保留 `localhost` 与云端 `https` 两套地址，通过 `useCloudInDev` 单开关切换整套开发环境。
- 补充 `uploadPath` 双环境说明：Windows 目录用于本地开发联调，Linux 目录用于最终服务器托管部署，两套路径均保留。
- `application.yml` 与 `RuoYiConfig` 已支持按运行系统自动切换 `uploadPath`：Windows 自动使用 `windowsProfile`，Linux 自动使用 `linuxProfile`。
- 补充 Docker 场景下 `RUOYI_PROFILE` 与卷挂载目录的关系说明，避免容器上传目录配置不一致。
- 明确 `/profile/upload/...` 依赖实际上传文件目录，数据库路径存在但物理文件未同步时会出现资源 404。

### 2026-03-30（v1.2.0）

**功能1：微信小程序一键登录 / 自动注册**
- 登录页新增「微信一键登录」绿色按钮（位于密码/短信登录表单下方，分隔线隔开）。
- 前端调用 `uni.login()` 获取临时 `code`，发送至后端 `/wxLogin` 接口。
- 后端通过 `code2session` 接口向微信服务器换取 `openid`；若该 `openid` 首次登录则自动创建用户（无需填写任何信息），返回 JWT Token 直接进入 App。
- `sys_user` 表新增 `wx_openid VARCHAR(64)` 列，并加唯一索引（已同步至 `ry_eqsy_repair.sql`）。
- `SysUser.java` 新增 `wxOpenId` 字段及 getter/setter。
- `SysUserMapper`（接口 + XML）新增 `selectUserByWxOpenId` 查询；`insertUser` SQL 支持 `wx_openid` 写入。
- `ISysUserService` / `SysUserServiceImpl` 新增对应方法。
- `SecurityConfig.java` 白名单追加 `/wxLogin`。
- `application.yml` 已填入真实 `wechat.appid`（`wx1ab607ec500707c0`）与 `wechat.secret`。
- `api/login.js` 新增 `wxLoginApi`。
- ✅ **已验证**：微信登录全链路（code2session → 自动注册 → Token → 进入 App）测试通过。

**功能2：前端双环境自动切换**
- `ruoyi-app/config.js` 改为按 `NODE_ENV` 自动选择后端地址：
  - HBuilderX **「运行」**（开发）→ `http://127.0.0.1:8080`（本地后端）
  - HBuilderX **「发行」**（生产）→ `https://ruoyi-backend.inmind-lab.com`（云端）
- 日常开发无需手动切换 URL。

**部署操作：**
1. **全新数据库**：直接导入 `sql/ry_eqsy_repair.sql`（已包含 `wx_openid` 列及索引）。
2. **已有数据库升级**：打开 `sql/ry_eqsy_repair.sql` 底部迁移说明块，将注释揭掉的两行 `ALTER TABLE` 单独运行（⚠️ 仅执行一次）。
3. 确保小程序 `manifest.json` 中的 `mp-weixin.appid` 与后端 `wechat.appid` 一致（当前均为 `wx1ab607ec500707c0`）。
4. 短信登录注意：当前验证码为**模拟发送**（验证码直接回传到接口响应），若需真实发送需购买短信服务（阿里云 SMS / 腾讯云 SMS）并替换 `sendSmsCode` 实现。

---

### 2026-03-30（v1.1.0）

**功能1：小程序多图上传**
- `work/index.vue` 支持单次最多选择 5 张照片，逐张串行上传，显示 "已选 N/5 张" 及单图移除按钮。

**功能2：视频结果上传与下载**
- 后台 `student/index.vue` 新增"动态视频"上传入口（≤10 MB，mp4/mov），上传后可预览及移除。
- 新增 `PUT /repair/task/manual/video` 接口，将视频 URL 单独存储到 `result_video_url` 字段。
- 小程序工作台收到视频结果后自动播放，并提供"下载视频到手机"按钮。
- `MimeTypeUtils.java` 允许列表加入 `mov` 格式。
- `RepairTaskMapper` 新增 `updateResultVideoUrl` 专用方法，绕开 GBK 实体编码限制。

**待手动操作（部署前必须完成）：**
1. 在 `RepairTask.java` 中添加 `resultVideoUrl` 字段及 getter/setter（见 `sql/v1_1_0_migration.sql`）。
2. 生产库执行 `sql/v1_1_0_migration.sql`（含列存在检查，可安全重复执行）。
   ⚠️ 注意：两步必须同时完成，否则 MyBatis 查询时会报 ReflectionException。

---

### 2026-03-27（v1.0.4）

- 修复 `upload.js` 网络失败时 `error.errMsg` 未处理导致崩溃的问题（与 `request.js` 同类 bug）。
- 修复「保存到相册」功能：`uni.saveImageToPhotosAlbum` 需本地临时路径，改为先 `uni.downloadFile` 再保存。
- 更新生产环境 `config.js` 中 `baseUrl` 为 `https://ruoyi-backend.inmind-lab.com`，区分开发/生产环境。
- Nginx 新增 `/profile/` 反向代理规则，保障生产环境上传图片的正常访问。
- `application.yml` 默认文件上传路径保持 Linux 路径 `/home/ruoyi/uploadPath`，本地 Windows 开发可按需覆盖为本地目录。

### 2026-03-27（v1.0.3）

- 修复短信登录/注册接口 `/sendSmsCode`、`/smsLogin` 未加入 Spring Security 白名单导致 401 的问题。
- 修复 `request.js` 网络请求失败时 `error.message` 为 undefined 引发崩溃的问题。
- 修复工作台图片显示 500 错误：后端返回的 `resultUrls` 为相对路径，统一拼接 `baseUrl` 处理。
- 首页副标题文案更新为「让科技有温度 让记忆有归处」。
- Docker 镜像添加 `version=1.0.3` LABEL。

### 2026-03-17

- 去除小程序 AI/人工选择，统一为志愿者处理。
- 优化工作台步骤文案为 1/2/3。
- 修复结果区按钮可点击性问题。
- 新增刷新 loading 动画。
- 新增 ruoyi-docker 容器化部署目录。
- README 重构为软件工程文档风格（含 UML/时序图/架构图/ER 图）。
