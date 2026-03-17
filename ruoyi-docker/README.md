# ruoyi-docker 部署目录

本目录用于服务器 Docker 部署，默认包含：
- MySQL 8
- Redis 7
- ruoyi-admin (Spring Boot)
- ruoyi-ui (Nginx 静态站点)

## 1. 准备 SQL

将项目根目录的 sql/ry_eqsy_repair.sql 复制到本目录：

```powershell
Copy-Item ..\sql\ry_eqsy_repair.sql .\mysql-init\ry_eqsy_repair.sql
```

Linux/macOS：

```bash
cp ../sql/ry_eqsy_repair.sql ./mysql-init/ry_eqsy_repair.sql
```

## 2. 准备环境变量

```powershell
Copy-Item .env.example .env
```

按需修改 .env 中数据库密码、端口等参数。

## 3. 启动

```powershell
docker compose up -d --build
```

## 4. 查看状态

```powershell
docker compose ps
docker compose logs -f ruoyi-admin
```

默认访问：
- 后台前端: http://服务器IP:${UI_PORT}
- 后端接口: http://服务器IP:${JAVA_PORT}
