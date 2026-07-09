# 山河卷 (ShanHeScroll)

融合中国地图交互、旅游推荐、旅拍相册和漂流瓶社交的全栈 Web 应用。

## 功能概览

- **中国地图探索** — ECharts 地图三级下钻（省 → 市 → 景点），按点赞数排行
- **景点推荐** — 全国 34 省 330+ 城市景点数据，含评分、推荐月份、游玩提示
- **旅拍相册** — 创建/管理相册，批量上传照片，四级隐私控制（公开/私密/白名单/黑名单）
- **漂流瓶** — 匿名或实名发送，随机拾取，点赞互动，审核流
- **用户系统** — 邮箱验证码注册，JWT 认证，tokenVersion 踢下线
- **管理后台** — 三级角色（用户/审核员/管理员），用户管理，漂流瓶审核

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21 · Spring Boot 4.0 · Spring Security · JPA · JWT · MySQL 8.0+ |
| 前端 | Vue 3.5 · TypeScript · Vite 8 · ECharts 6 · Axios · vue-router 4 |
| 部署 | Nginx (HTTPS/HTTP2) · Let's Encrypt / 自签名证书 · systemd |

## 项目结构

```
ShanHeScroll/
├── ShanHeScroll_backend/        # Spring Boot 后端
│   └── src/main/java/com/mytravel/
│       ├── common/              # 公共组件（文件上传、异常处理、JWT、限流）
│       ├── user/                # 用户模块（注册/登录/资料/认证）
│       ├── travel/              # 旅游推荐（地区/景点/搜索）
│       ├── album/               # 相册模块（CRUD/隐私/照片）
│       ├── bottle/              # 漂流瓶（发送/拾取/点赞/审核）
│       └── admin/               # 管理员（用户管理/角色/封禁）
├── ShanHeScroll_front/          # Vue 3 前端
│   └── src/
│       ├── views/               # 页面（Home/Album/Bottle/Profile/Admin/Login...）
│       ├── components/          # 公共组件（TopNav/PuzzleCaptcha）
│       ├── api/                 # API 调用层
│       ├── router/              # 路由配置
│       ├── composables/         # 组合式函数（useTheme）
│       └── utils/               # 工具函数（区域数据/缩略图）
├── deploy.sh                    # 服务器一键部署脚本
├── shanhescroll                 # Nginx 站点配置
├── ssl.sh                       # 自签名证书生成脚本
└── attractions_import.sql       # 景点种子数据
```

## 本地开发

### 环境要求

- JDK 21 · Maven 3.8+ · Node.js 20+ · MySQL 8.0+

### 1. 数据库

```sql
CREATE DATABASE my_travel DEFAULT CHARACTER SET utf8mb4;
```

### 2. 后端配置

在 `ShanHeScroll_backend/src/main/resources/` 下创建 `application-local.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/my_travel?characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update

app.jwt.secret=至少32位的随机字符串
app.smtp.host=smtp.example.com
app.smtp.port=587
app.smtp.username=your-email@example.com
app.smtp.password=邮箱授权码
```

### 3. 启动

```bash
# 后端（端口 8080）
cd ShanHeScroll_backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 前端（端口 5173，自动代理 /api 到 8080）
cd ShanHeScroll_front
npm install
npm run dev
```

### 4. 导入景点数据

```bash
mysql -u root -p my_travel < attractions_import.sql
```

访问 `http://localhost:5173`。

## 生产部署

### 服务器目录结构

```
/opt/shanhescroll/
├── frontend/                    # 前端构建产物
├── uploads/                     # 用户上传文件
├── photo/                       # 默认图片/头像
├── shanhescroll-0.0.1-SNAPSHOT.jar
└── application-prod.properties  # 生产环境配置
```

### 部署步骤

```bash
# 1. 本地构建
cd ShanHeScroll_backend && ./mvnw package -DskipTests
cd ../ShanHeScroll_front && npm run build

# 2. 上传到服务器
scp target/shanhescroll-0.0.1-SNAPSHOT.jar user@server:/opt/shanhescroll/
scp -r dist/* user@server:/opt/shanhescroll/frontend/

# 3. 配置 Nginx（将 shanhescroll 文件上传后）
sudo cp shanhescroll /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/shanhescroll /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# 4. 运行
./deploy.sh
```

### SSL 证书

```bash
# 自签名（开发/测试）
bash ssl.sh

# 生产环境推荐 Let's Encrypt
certbot --nginx -d your-domain.com
```

## API 概览

| 模块 | 端点前缀 | 说明 |
|------|---------|------|
| 公开接口 | `/api/public/*` | 地区/景点查询 |
| 用户模块 | `/api/user/*` | 注册/登录/资料/密码 |
| 相册模块 | `/api/album/*` | CRUD/照片/隐私 |
| 漂流瓶 | `/api/bottle/*` | 发送/拾取/点赞 |
| 文件上传 | `/api/file/*` | 单文件/批量（≤20MB，图片格式） |
| 管理后台 | `/api/admin/*` | 用户管理/漂流瓶审核 |

统一响应格式：`{ code: 200, message: "success", data: ... }`

认证方式：`Authorization: Bearer <JWT_TOKEN>`

## 数据库表

| 表 | 说明 |
|---|------|
| `region` | 全国三级行政区划（省/市/区） |
| `attraction` | 景点（名称/坐标/评分/分类/游玩提示） |
| `attraction_image` | 景点多图 |
| `user` | 用户（三级角色/BCrypt密码/tokenVersion） |
| `album` | 相册（四级隐私/CRUD） |
| `album_photo` | 相册照片 |
| `album_visibility_user` | 白名单/黑名单 |
| `drift_bottle` | 漂流瓶（待审/通过/拒绝/已拾取） |
| `bottle_like` | 漂流瓶点赞 |
