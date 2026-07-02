# MyTravel 旅游社交平台

## 项目简介

MyTravel 是一个结合**旅游推荐**与**社交分享**的 Web 应用后端项目。用户可以在平台上浏览全国各地区的旅游景点推荐，创建个人旅拍相册，并通过"漂流瓶"功能与其他旅行爱好者互动。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 4.0.6 | Web 框架 |
| Spring Data JPA | — | ORM / 数据库操作 |
| Spring Security | — | 认证与授权 |
| MySQL | 8.0+ | 关系型数据库 |
| JWT (java-jwt) | 4.4.0 | 无状态认证 |
| Lombok | — | 代码简化 |
| Spring Mail | — | 邮件发送 |
| Maven | — | 项目构建 |

---

## 项目结构

```
src/main/java/com/mytravel/
├── DemoApplication.java                  ← 启动类
├── common/
│   ├── ApiResponse.java                  ← 统一响应体
│   ├── CacheCleanupScheduler.java        ← 缓存定时清理
│   ├── FileService.java                  ← 文件上传服务
│   ├── FileController.java               ← 文件上传接口
│   ├── GlobalExceptionHandler.java       ← 全局异常处理
│   ├── config/
│   │   ├── SpringSecurityConfig.java     ← Spring Security 配置
│   │   └── WebMvcConfig.java             ← 静态资源映射
│   └── util/
│       ├── EmailCodeCache.java           ← 邮箱验证码缓存
│       ├── EmailValidator.java           ← 邮箱白名单校验
│       ├── JwtUtil.java                  ← JWT 生成/校验
│       └── RateLimiter.java              ← 内存限流器
├── user/                                 ← 用户模块
│   ├── User.java, UserRepository.java
│   ├── AuthService.java                  ← 注册/登录/改密
│   ├── UserProfileService.java           ← 资料/手机绑定
│   ├── EmailVerificationService.java     ← 邮箱验证码/绑定
│   ├── UserMapper.java                   ← Entity ↔ DTO
│   ├── UserController.java
│   ├── EmailService.java
│   ├── JwtAuthenticationFilter.java
│   └── dto/                              ← 请求/响应 DTO
├── travel/                               ← 旅游推荐模块
│   ├── Region.java, RegionRepository.java
│   ├── Attraction.java, AttractionRepository.java
│   ├── AttractionImage.java, AttractionImageRepository.java
│   ├── TravelService.java, TravelController.java
├── album/                                ← 相册模块
│   ├── Album.java, AlbumRepository.java
│   ├── AlbumPhoto.java, AlbumPhotoRepository.java
│   ├── AlbumVisibilityUser.java, AlbumVisibilityUserRepository.java
│   ├── AlbumService.java, AlbumController.java
│   └── CreateAlbumRequest.java, AddPhotoRequest.java,
│       UpdatePrivacyRequest.java
├── bottle/                               ← 漂流瓶模块
│   ├── DriftBottle.java, DriftBottleRepository.java
│   ├── BottleLike.java, BottleLikeRepository.java
│   ├── DriftBottleService.java, DriftBottleController.java
│   └── SendBottleRequest.java, BottleDetailResponse.java
└── resources/
    ├── application.properties
    └── application-local.properties      ← 本地密钥（.gitignore）
```

---

## 统一 API 响应格式

所有接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|:----:|------|
| 200 | 成功 |
| 400 | 业务错误（message 中有中文提示） |
| 401 | 认证失败 |
| 500 | 服务器内部错误 |

---

## 基础设施

### 文件上传

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/file/upload` | 单文件上传（≤10MB，仅图片） |
| POST | `/api/file/upload/batch` | 批量上传（最多 9 张） |

- 存储路径：`./uploads/yyyy/MM/dd/UUID.ext`
- 访问 URL：`/uploads/yyyy/MM/dd/UUID.jpg`（公开）

---

## 1. 旅游推荐模块

### 数据库设计

**地区表 (region)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| parent_id | BIGINT | 父级ID |
| name | VARCHAR(100) | 地区名称 |
| level | TINYINT | 1-国家 2-省级 3-市级 |
| description | VARCHAR(500) | 简介 |
| image | VARCHAR(500) | 封面图 |
| sort_order | INT | 排序 |

**景点表 (attraction)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| region_id | BIGINT | 所属城市ID |
| name | VARCHAR(100) | 景点名称 |
| description | TEXT | 详细介绍 |
| image | VARCHAR(500) | 封面图 |
| address | VARCHAR(200) | 详细地址 |
| rating | DECIMAL(2,1) | 评分 |
| like_count | INT | 点赞数 |
| recommend_month | VARCHAR(100) | 推荐月份 |
| tips | VARCHAR(500) | 游玩提示 |

**景点图片表 (attraction_image)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| attraction_id | BIGINT | 所属景点ID |
| url | VARCHAR(500) | 图片路径 |
| sort_order | INT | 排序 |

### 开放接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/public/regions?level=2` | 获取省级单位 |
| GET | `/api/public/regions?parentId={id}` | 获取子地区 |
| GET | `/api/public/attractions?regionId={id}&sortBy=likes` | 景点列表 |
| GET | `/api/public/attractions/{id}` | 景点详情（含图片） |

---

## 2. 个人旅拍相册

### 数据库设计

**相册表 (album)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 所属用户 |
| region_id | BIGINT | 关联地区ID |
| title | VARCHAR(100) | 相册标题 |
| description | TEXT | 相册描述 |
| cover_image | VARCHAR(500) | 封面图片 |
| visibility | TINYINT | 0=公开 1=仅自己 2=白名单 3=黑名单 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**相册图片表 (album_photo)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| album_id | BIGINT | 所属相册 |
| url | VARCHAR(500) | 图片路径 |
| description | VARCHAR(200) | 图片描述 |
| sort_order | INT | 排序 |
| created_at | DATETIME | 上传时间 |

**可见性用户表 (album_visibility_user)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| album_id | BIGINT | 相册ID |
| user_id | BIGINT | 用户ID |
| type | TINYINT | 0=白名单 1=黑名单 |

### 隐私权限

| visibility | 模式 | 谁可查看 |
|:--:|------|------|
| 0 | 公开 | 所有人 |
| 1 | 私密 | 仅所有者 |
| 2 | 白名单 | 仅列表中的用户 |
| 3 | 黑名单 | 除列表中用户外的所有人 |

支持**一键批量设置**所有相册的隐私。

### 接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:--:|
| POST | `/api/album` | 创建相册 | ✅ |
| GET | `/api/album` | 我的相册列表 | ✅ |
| GET | `/api/album/user/{userId}` | 某人相册列表 | ❌ |
| GET | `/api/album/{id}` | 相册详情（含照片） | ❌ |
| PUT | `/api/album/{id}` | 修改相册 | ✅ |
| DELETE | `/api/album/{id}` | 删除相册（级联删照片） | ✅ |
| POST | `/api/album/{id}/photo` | 添加照片 | ✅ |
| DELETE | `/api/album/{albumId}/photo/{photoId}` | 删除照片 | ✅ |
| GET | `/api/album/{id}/privacy` | 查看隐私设置 | ✅ |
| PUT | `/api/album/{id}/privacy` | 设置隐私 | ✅ |
| PUT | `/api/album/privacy/batch` | 一键设置所有相册隐私 | ✅ |

---

## 3. 漂流瓶功能

### 数据库设计

**漂流瓶表 (drift_bottle)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| sender_id | BIGINT | 发送者ID |
| is_anonymous | TINYINT | 0=实名 1=匿名 |
| text_content | TEXT | 文字内容 |
| image_url | VARCHAR(500) | 图片路径 |
| status | TINYINT | 0=待审核 1=审核通过 2=审核拒绝 3=已拾取 |
| created_at | DATETIME | 发布时间 |

**点赞表 (bottle_like)**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| bottle_id | BIGINT | 漂流瓶ID |
| user_id | BIGINT | 点赞用户ID |
| created_at | DATETIME | 点赞时间 |

### 业务逻辑

```
发送漂流瓶 → 选择是否匿名 → 状态设为"审核通过"
用户拾取   → 随机获取一个审核通过的瓶子 → 状态改为"已拾取"
             → 实名瓶子显示发送者信息（昵称/头像）
             → 匿名瓶子隐藏发送者信息
             → 均可点赞/取消点赞
```

### 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/bottle/send` | 发送漂流瓶 |
| GET | `/api/bottle/pick` | 随机拾取一个 |
| GET | `/api/bottle/my` | 我的漂流瓶列表 |
| GET | `/api/bottle/{id}` | 漂流瓶详情 |
| POST | `/api/bottle/{id}/like` | 点赞 |
| DELETE | `/api/bottle/{id}/like` | 取消点赞 |

---

## 4. 个人中心

### 数据库设计（用户表 user）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 登录用户名（唯一） |
| password | VARCHAR(255) | BCrypt 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(500) | 头像路径 |
| email | VARCHAR(100) | 邮箱（唯一） |
| phone | VARCHAR(20) | 手机号 |
| gender | TINYINT | 性别 |
| birthday | DATE | 生日 |
| bio | VARCHAR(200) | 个人简介 |
| location | VARCHAR(100) | 所在地区 |
| status | TINYINT | 账号状态: 0=正常 1=封禁 |
| role | TINYINT | 0=普通用户 1=审核员 2=管理员 |
| token_version | INT | Token版本号（踢下线机制） |
| created_at | DATETIME | 注册时间 |
| updated_at | DATETIME | 更新时间 |

### 认证方式

采用 **JWT（JSON Web Token）** 无状态认证：

- 邮箱注册（需验证码），密码 BCrypt 加密存储
- 登录支持邮箱或用户名，签发 token，24 小时有效期
- Header：`Authorization: Bearer <token>`
- **tokenVersion** 机制：每次登录 / 改密版本号 +1，旧 token 自动失效

### 接口

| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|:--:|
| POST | `/api/user/register/code` | 发送注册验证码 | ❌ |
| POST | `/api/user/register` | 注册（邮箱+验证码） | ❌ |
| POST | `/api/user/login` | 登录（邮箱或用户名） | ❌ |
| GET | `/api/user/profile` | 获取个人信息 | ✅ |
| PUT | `/api/user/profile` | 修改个人信息 | ✅ |
| PUT | `/api/user/password` | 修改密码 | ✅ |
| PUT | `/api/user/phone/bind` | 绑定手机号 | ✅ |
| POST | `/api/user/email/code` | 发送邮箱验证码（限流） | ✅ |
| PUT | `/api/user/email/bind` | 绑定邮箱 | ✅ |

---

## 已实现功能

- [x] 用户注册（邮箱+验证码）/登录（支持邮箱/用户名，JWT 认证 + tokenVersion）
- [x] 个人信息管理（查看/编辑资料、头像、简介）
- [x] 密码修改（BCrypt 加密，旧 token 自动失效）
- [x] 邮箱绑定（白名单 + SMTP 验证码 + 限流）
- [x] 手机号绑定
- [x] 全国旅游地区/景点数据（34 省，330+ 城市）
- [x] 景点详情展示（含多张图片 + 按点赞数排序）
- [x] 统一 API 响应格式 + 全局异常处理
- [x] 密钥安全（环境变量 / 本地配置文件分离）
- [x] 文件上传（本地存储 + 类型校验）
- [x] 个人旅拍相册（CRUD + 照片管理 + 四级隐私权限 + 批量操作）
- [x] 漂流瓶（匿名/实名发送 + 随机拾取 + 点赞 + 审核状态预留）
- [x] 限流保护（验证码接口三层次限流）
- [x] 缓存定时清理（验证码 + 限流器过期条目）
- [x] 45 个单元测试覆盖

---

## 5. 管理员系统

### 三级角色

| role | 名称 | 权限 |
|:--:|------|------|
| 0 | 普通用户 | 使用基本功能 |
| 1 | 审核员 | 审核漂流瓶（通过/拒绝） |
| 2 | 管理员 | 用户管理 + 审核员所有权限 |

### 接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|:--:|------|
| GET | `/api/admin/bottles/pending` | ≥1 | 待审核漂流瓶列表 |
| PUT | `/api/admin/bottles/{id}/approve` | ≥1 | 审核通过 |
| PUT | `/api/admin/bottles/{id}/reject` | ≥1 | 审核拒绝 |
| GET | `/api/admin/users` | 2 | 用户列表 |
| POST | `/api/admin/users` | 2 | 创建用户（可指定角色） |
| PUT | `/api/admin/users/{id}/reset-password` | 2 | 重置密码 |
| PUT | `/api/admin/users/{id}/ban` | 2 | 封禁用户 |
| PUT | `/api/admin/users/{id}/unban` | 2 | 解封用户 |

---

## 待实现功能

- [ ] 漂流瓶评论功能
- [ ] 照片上传云存储（当前为本地存储）

> 注：个人相册作为独立页面展示（已通过 `/api/album/user/{id}` + `/api/album/{id}` 实现），不聚合到个人主页。
