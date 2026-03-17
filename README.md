# e起守忆影像修复平台

面向社区老人服务的老照片修复系统，采用统一志愿者处理模式：
- 小程序端：上传照片 + 留言需求，系统直接提交给学生志愿者后台处理。
- 后台管理端：任务查看、接单处理、进度更新与结果回传。
- Java 后端：鉴权、任务流转、数据管理。

项目背景：
- 基于 RuoYi 框架进行二次开发。
- 用于江苏省常州市武进区社区志愿者活动场景。
- 由江苏理工学院委托开发。

## 1. 项目结构

- ruoyi-app：uni-app 小程序端。
- ruoyi-ui：Vue3 + Element Plus 后台管理端。
- ruoyi-admin：Spring Boot 管理接口层。
- ruoyi-system：系统与业务核心模块。
- ruoyi-framework：安全、登录、注册、权限框架。
- ruoyi-common：公共模型、工具、常量。
- sql/ry_eqsy_repair.sql：数据库初始化脚本。
- ruoyi-docker：Docker 部署目录（新增）。

说明：ruoyi-fastapi 已从当前方案移除，不再作为运行依赖。

## 2. 当前业务流程

1. 老人在小程序工作台上传老照片。
2. 可填写修复诉求留言。
3. 点击提交后统一进入志愿者处理队列。
4. 学生志愿者在后台接单、处理并上传结果。
5. 老人在小程序端刷新查看并保存修复结果。

## 3. 本地开发启动

### 3.1 后端

```powershell
mvn clean package -DskipTests
cd ruoyi-admin
mvn spring-boot:run
```

默认接口地址：http://localhost:8080

### 3.2 后台前端

```powershell
cd ruoyi-ui
npm install
npm run dev
```

### 3.3 小程序端

- 使用 HBuilderX 或微信开发者工具打开 ruoyi-app。
- 在 ruoyi-app/config.js 中配置后端地址。

## 4. Docker 服务器部署

已提供独立目录：ruoyi-docker

### 4.1 目录内容

- ruoyi-docker/docker-compose.yml：MySQL + Redis + ruoyi-admin + ruoyi-ui 一键编排。
- ruoyi-docker/ruoyi-admin/Dockerfile：后端镜像构建。
- ruoyi-docker/ruoyi-ui/Dockerfile：后台前端镜像构建。
- ruoyi-docker/nginx/default.conf：前端 Nginx 反向代理配置。
- ruoyi-docker/.env.example：环境变量模板。

### 4.2 部署步骤

1. 复制数据库初始化脚本到 ruoyi-docker/mysql-init。

```powershell
cd ruoyi-docker
Copy-Item ..\sql\ry_eqsy_repair.sql .\mysql-init\ry_eqsy_repair.sql
```

2. 复制并修改环境变量。

```powershell
Copy-Item .env.example .env
```

3. 构建并启动。

```powershell
docker compose up -d --build
```

4. 查看运行状态。

```powershell
docker compose ps
docker compose logs -f ruoyi-admin
```

### 4.3 默认访问

- 后台前端：http://服务器IP:80
- 后端接口：http://服务器IP:8080

## 5. 备注

- 生产环境建议修改 MySQL/Redis 密码并限制端口暴露。
- 上传目录已通过 Docker volume 持久化。
- 如需 HTTPS，请在网关或 Nginx 层补充证书配置。
