# 🏠 居家手工美食烘焙园地

> 聚焦家庭烘焙、面点制作爱好者的配方分享平台

## 📋 项目简介

本平台主打烘焙配方分享、制作过程记录、成品展示交流，专注居家美食创作分享。不对接食材电商、线下课程等外部商业服务，依靠本地标签区分烘焙品类。

## 🛠️ 技术栈

### 前端
- **框架**: Vue 3.4 + Vite 5.0
- **路由**: Vue Router 4.2
- **状态管理**: Pinia 2.1
- **UI 组件**: Element Plus 2.5
- **HTTP 客户端**: Axios 1.6
- **性能优化**: 图片懒加载、Gzip 压缩、草稿实时缓存

### 后端
- **框架**: Spring Boot 3.3.0 + JDK 17
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0
- **缓存**: Redis 7
- **图片处理**: Thumbnailator 0.4.20

### 部署
- **容器化**: Docker + Docker Compose
- **Web 服务器**: Nginx (带 Gzip 压缩)
- **国内镜像加速**: 阿里云 Maven、npmmirror NPM

## 🌐 端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 3008 | Nginx 静态资源服务 |
| 后端 | 8088 | Spring Boot API 服务 |
| MySQL | 3311 | 关系型数据库 |
| Redis | 6380 | 缓存服务 |

> 所有端口统一在 `.env` 文件中配置，便于统一管理。

## 🎯 核心功能

### 🍞 烘焙配方发布
- 记录食材配比、制作步骤、烘焙温度与时长
- 支持多图上传，自动压缩处理
- 草稿实时保存到本地，防止内容丢失

### 🔍 配方分类浏览
- 按面包、蛋糕、饼干、甜点、披萨、中式面点等品类筛选
- 热门配方推荐，浏览量实时统计
- 支持关键词搜索

### ❤️ 配方收藏汇总
- 一键收藏心仪配方
- 支持收藏夹分类管理
- 形成个人专属烘焙手册

### 💬 制作心得交流
- 配方下方留言讨论
- 评论点赞互动
- 交流制作技巧、问题难点

## 🚀 快速启动

### 前置要求
- Docker 20.10+
- Docker Compose 2.0+

### 一键启动

```bash
./start.sh
```

脚本会自动：
1. 检测端口是否被占用（冲突会报错并提示占用进程）
2. 构建并启动所有服务
3. 构建完成后自动输出访问地址

### 手动启动

```bash
# 构建并启动
docker compose up -d --build

# 查看日志
docker compose logs -f

# 停止服务
docker compose down
```

## 🔧 配置说明

### 环境变量 (.env)

```bash
# 项目名称（容器命名前缀）
PROJECT_NAME=ju-jia-hong-bei-yuan-di

# Docker 镜像仓库前缀，留空则使用默认 Docker Hub
# 国内用户可配置为：
#   DOCKER_REGISTRY=docker.mirrors.ustc.edu.cn/
#   DOCKER_REGISTRY=hub-mirror.c.163.com/
# 注意：末尾需要带斜杠
DOCKER_REGISTRY=docker.m.daocloud.io/library/

# 端口配置
FRONTEND_PORT=3008
BACKEND_PORT=8088
MYSQL_PORT=3311
REDIS_PORT=6380

# 数据库配置
MYSQL_ROOT_PASSWORD=baking123
MYSQL_DATABASE=baking
MYSQL_USER=baking
MYSQL_PASSWORD=baking123

# Redis 配置
REDIS_PASSWORD=baking123
```

### Docker 构建缓存机制

项目采用 Docker 原生分层缓存策略：

1. **依赖层**：先复制 `pom.xml` / `package*.json`，下载依赖
2. **代码层**：再复制源代码，执行编译构建
3. **缓存复用**：依赖文件无变更时，直接复用缓存层，不再重新下载

## 📦 项目结构

```
qd-108/
├── .env                      # 全局环境变量配置
├── .gitignore                # Git 忽略配置
├── docker-compose.yml        # Docker Compose 编排
├── start.sh                  # 一键启动脚本
├── README.md                 # 项目说明文档
│
├── backend/                  # 后端 Spring Boot 项目
│   ├── pom.xml               # Maven 依赖配置
│   ├── settings.xml          # Maven 镜像配置
│   ├── Dockerfile            # 后端 Dockerfile
│   └── src/                  # 源代码
│
├── frontend/                 # 前端 Vue3 项目
│   ├── package.json          # NPM 依赖配置
│   ├── vite.config.js        # Vite 构建配置
│   ├── Dockerfile            # 前端 Dockerfile
│   ├── nginx.conf            # Nginx 配置
│   └── src/                  # 源代码
│
└── docker/                   # Docker 定制镜像
    └── mysql/                # MySQL 定制
        ├── Dockerfile
        ├── my.cnf            # MySQL 配置
        └── init/             # 初始化脚本
```

## 🌐 访问地址

启动成功后，访问以下地址：

| 地址 | 说明 |
|------|------|
| http://localhost:3008 | 前端页面 |
| http://127.0.0.1:3008 | 前端页面（IPv4） |
| http://localhost:8088/api | 后端 API |
| http://127.0.0.1:8088/api | 后端 API（IPv4） |

> 确保 `localhost` 和 `127.0.0.1` 访问的是同一服务。

## 🔍 端口自检命令

```bash
# 检查端口占用
lsof -nP -iTCP:3008 -sTCP:LISTEN
lsof -nP -iTCP:8088 -sTCP:LISTEN

# 验证服务响应
curl -sS http://127.0.0.1:3008 | head
curl -sS http://localhost:3008 | head
```

## 📝 默认数据

数据库初始化时已预置：

| 类型 | 内容 |
|------|------|
| 分类 | 面包、蛋糕、饼干、甜点、披萨、中式面点 |
| 测试用户 | admin / admin |

## 🛡️ 安全说明

- 所有服务仅绑定 `127.0.0.1`，不对外暴露
- 数据库和 Redis 均设置密码
- 生产环境请务必修改默认密码

## 📄 License

MIT
