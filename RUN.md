# 山川游记（ShanHeScroll）— 运行指南

## 环境要求

| 依赖 | 版本 | 说明 |
|------|:--:|------|
| Java | **21** | 后端运行环境 |
| Maven | 3.8+ | 后端构建（项目已自带 mvnw，可免安装） |
| MySQL | **8.0+** | 数据库 |
| Node.js | 18+ / 20+ | 前端运行环境 |
| npm | 9+ | 随 Node.js 附带 |

---

## 一、后端（Spring Boot）

### 1. 创建数据库

启动 MySQL，执行：

```sql
CREATE DATABASE IF NOT EXISTS my_travel DEFAULT CHARACTER SET utf8mb4;
```

### 2. 配置密钥

编辑 `ShanHeScroll_backend/src/main/resources/application-local.properties`：

```properties
# 数据库密码
spring.datasource.password=你的MySQL密码

# JWT 密钥（建议用 openssl rand -base64 32 生成）
jwt.secret=你的JWT密钥

# 邮箱（用于发送验证码，可选）
spring.mail.username=你的QQ邮箱@qq.com
spring.mail.password=你的SMTP授权码
```

> 如果不需要邮箱功能，可以暂时不改邮箱配置，不影响核心功能使用。

### 3. 启动

```bash
cd ShanHeScroll_backend

# Windows
./mvnw spring-boot:run 



启动成功后会看到：`Started ShanHeScrollApplication in ...`，默认运行在 **`http://localhost:8080`**。

---

## 二、前端（Vue 3 + Vite）

### 1. 安装依赖

```bash
cd ShanHeScroll_front
npm install
```

### 2. 启动

```bash
npm run dev
```

启动后访问 **`http://localhost:5173`**，Vite 会自动将 `/api` 请求代理到后端的 `localhost:8080`。

---

## 三、验证

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 浏览器打开 `http://localhost:5173` | 看到中国地图首页 |
| 2 | 点击地图上的某个省份（如 陕西省） | 进入省份地图，显示各城市 |
| 3 | 点击某个城市（如 西安） | 右侧显示景区推荐面板 |
| 4 | 在浏览器中直接访问 `http://localhost:8080/api/public/regions?level=2` | 返回 JSON 省份数据 |

---

## 四、常见问题

### Q1: 启动后端报 `Access denied for user 'root'`

MySQL 密码不正确，检查 `application-local.properties` 中的密码。

### Q2: 启动后端报 `Unknown database 'my_travel'`

数据库未创建，执行 `CREATE DATABASE my_travel;` 后重试。

### Q3: 前端地图显示但省份数据加载失败

后端未启动或未正常运行。确认 `http://localhost:8080/api/public/regions?level=2` 能正常访问。

### Q4: 点击城市不显示景区

确认后端正在运行，打开浏览器 **F12 → Network** 查看 `/api/public/attractions` 请求是否返回 200。

### Q5: 端口被占用

- 后端端口在 `application.properties` 中配置（默认 8080）
- 前端端口在 `vite.config.ts` 中配置（默认 5173）

---

## 项目结构

```
ShanHeScroll/
├── RUN.md                        ← 本文件
├── ShanHeScroll_backend/         ← Spring Boot 后端（Java 21）
│   ├── src/main/java/com/mytravel/
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── application-local.properties
│   ├── mvnw.cmd / mvnw           ← Maven Wrapper
│   └── API.md                    ← 后端 API 文档
└── ShanHeScroll_front/           ← Vue 3 前端
    ├── src/
    │   ├── api/                  ← API 调用层
    │   ├── components/           ← 公共组件
    │   ├── views/                ← 页面
    │   └── router/               ← 路由
    └── API.md                    ← 前端 API 参考
```
//
前端打包
cd ShanHeScroll_front
npm run build
产出 dist/ 文件夹
后端
cd ShanHeScroll_backend
./mvnw package -DskipTests
产出 target/shanhescroll-0.0.1-SNAPSHOT.jar