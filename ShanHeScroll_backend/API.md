# MyTravel 后端 API 文档

> 前端开发只需阅读本文档即可对接后端，无需查看后端代码。

---

## 通用说明

**Base URL:** `http://localhost:8080`

**认证方式:** JWT Bearer Token，登录后获取，每次请求放在 Header 中：
```
Authorization: Bearer <token>
```

**统一响应格式:** 所有接口返回以下结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|:----:|------|
| 200 | 成功，data 中为业务数据 |
| 400 | 业务错误，message 中有中文提示 |
| 401 | 未登录或 token 过期 |
| 500 | 服务器内部错误 |

**公开接口（无需 token）：**
- `POST /api/user/register/code`
- `POST /api/user/register`
- `POST /api/user/login`
- `GET /api/public/**`
- `GET /uploads/**`
- `GET /api/album/user/**`
- `GET /api/album/{id}`

**其余接口均需认证。**

---

## 1. 用户模块 `/api/user`

### 1.1 发送注册验证码

```
POST /api/user/register/code
```

**请求体:**
```json
{
  "email": "string（必填）"
}
```

**响应 data:** `"验证码已发送"`

> 限制：同一邮箱 60 秒内只能发一次，每天最多 5 次。验证码 5 分钟有效。
> 仅支持主流邮箱域名：qq.com / 163.com / 126.com / gmail.com / outlook.com / foxmail.com / sina.com / yahoo.com / hotmail.com

---

### 1.2 注册

```
POST /api/user/register
```

**请求体:**
```json
{
  "email":    "string（必填，作为登录账号）",
  "password": "string（必填）",
  "nickname": "string（必填，2-20位，仅允许英文字母、数字、下划线、连字符、点号）",
  "code":     "string（必填，6位数字验证码）"
}
```

**响应 data:** 用户信息
```json
{
  "id": 1,
  "username": "testuser",
  "nickname": "旅行者",
  "avatar": null,
  "email": "test@qq.com",
  "phone": null,
  "gender": null,
  "bio": null,
  "location": null
}
```

> 注册后可用邮箱或用户名登录。昵称格式必须为 2-20 位，仅允许 `a-zA-Z0-9._-`。

---

### 1.3 登录

```
POST /api/user/login
```

**请求体:**
```json
{
  "account":  "string（必填，用户名或邮箱）",
  "password": "string（必填）"
}
```

**响应 data:**
```json
{
  "token": "eyJhbGciOiJI...",
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "avatar": null,
    "email": "test@qq.com",
    "phone": null,
    "gender": null,
    "bio": null,
    "location": null
  }
}
```

> `token` 24 小时有效。每次登录旧 token 自动失效。

---

### 1.4 修改密码

```
PUT /api/user/password
```

**请求体:**
```json
{
  "oldPassword": "string（必填）",
  "newPassword": "string（必填）"
}
```

**响应 data:** `"密码修改成功"`

> 修改密码后旧 token 自动失效，需重新登录。

---

### 1.5 获取个人资料

```
GET /api/user/profile
```

**响应 data:** 用户信息（同 1.1）

---

### 1.6 修改个人资料

```
PUT /api/user/profile
```

**请求体（所有字段选填，传哪个改哪个）:**
```json
{
  "nickname": "string",
  "phone":    "string",
  "gender":   "integer（0/1/2）",
  "bio":      "string",
  "location": "string",
  "avatar":   "string（图片 URL）"
}
```

**响应 data:** 更新后的用户信息

---

### 1.7 绑定手机号

```
PUT /api/user/phone/bind
```

**请求体:**
```json
{
  "phone": "string（必填）"
}
```

**响应 data:** `"手机号绑定成功"`

---

### 1.8 发送邮箱验证码

```
POST /api/user/email/code
```

**请求体:**
```json
{
  "email": "string（必填）"
}
```

**响应 data:** `"验证码已发送"`

> 限制：同一用户/邮箱 60 秒内只能发一次，每天最多 10 次。验证码 5 分钟有效。

---

### 1.9 绑定邮箱

```
PUT /api/user/email/bind
```

**请求体:**
```json
{
  "email": "string（必填）",
  "code":  "string（必填，6 位数字验证码）"
}
```

**响应 data:** `"邮箱绑定成功"`

---

## 2. 旅游推荐模块 `/api/public`

### 2.1 查询地区

```
GET /api/public/regions?level=2
GET /api/public/regions?parentId=1
```

**参数（二选一）:**
| 参数 | 类型 | 说明 |
|------|------|------|
| `level` | Integer | 按层级查询：1=国家 2=省级 3=市级 |
| `parentId` | Long | 查询某地区下的子地区 |

**响应 data:**
```json
[
  {
    "id": 110000,
    "parentId": 1,
    "name": "北京",
    "level": 2,
    "description": "中华人民共和国首都",
    "image": null,
    "sortOrder": 1
  }
]
```

---

### 2.2 查询景点列表

```
GET /api/public/attractions?regionId=110000
GET /api/public/attractions?regionId=110000&sortBy=likes
```

**参数:**
| 参数 | 类型 | 说明 |
|------|------|------|
| `regionId` | Long | 城市/区 ID（必填） |
| `sortBy` | String | `"likes"` 按点赞数排序，不传默认排序 |

**响应 data:**
```json
[
  {
    "id": 1,
    "regionId": 110000,
    "name": "故宫博物院",
    "description": "明清两代的皇家宫殿...",
    "image": "/uploads/2026/06/gugong.jpg",
    "address": "北京市东城区景山前街4号",
    "rating": 4.8,
    "recommendMonth": "4-5月,9-10月",
    "tips": "建议提前预约门票",
    "likeCount": 1280,
    "images": [
      { "id": 1, "attractionId": 1, "url": "/uploads/.../gugong1.jpg", "sortOrder": 1 },
      { "id": 2, "attractionId": 1, "url": "/uploads/.../gugong2.jpg", "sortOrder": 2 }
    ]
  }
]
```

---

### 2.3 景点详情

```
GET /api/public/attractions/{id}
```

**响应 data:** 单个景点对象（同上结构，含图片列表）

---

## 3. 文件上传 `/api/file`

> 限制：仅允许 jpg / png / gif / webp，单文件 ≤10MB，批量最多 9 张。

### 3.1 单文件上传

```
POST /api/file/upload
Content-Type: multipart/form-data
```

**表单字段:** `file` — 文件

**响应 data:** `"/uploads/2026/06/13/abc123def.jpg"`

> 返回的 URL 可直接拼接 base URL 访问：`http://localhost:8080/uploads/2026/06/13/abc123def.jpg`

---

### 3.2 批量上传

```
POST /api/file/upload/batch
Content-Type: multipart/form-data
```

**表单字段:** `files` — 文件列表（最多 9 个）

**响应 data:**
```json
[
  "/uploads/2026/06/13/abc123.jpg",
  "/uploads/2026/06/13/def456.jpg"
]
```

---

## 4. 相册模块 `/api/album`

### 4.1 创建相册

```
POST /api/album
```

**请求体:**
```json
{
  "regionId":   110000,
  "title":      "北京之旅",
  "description": "一次难忘的旅行",
  "coverImage": "/uploads/2026/06/cover.jpg"
}
```

**响应 data:** 相册对象
```json
{
  "id": 1,
  "userId": 1,
  "regionId": 110000,
  "title": "北京之旅",
  "description": "一次难忘的旅行",
  "coverImage": "/uploads/2026/06/cover.jpg",
  "visibility": 0,
  "createdAt": "2026-06-13T10:30:00",
  "updatedAt": "2026-06-13T10:30:00",
  "photos": [],
  "visibilityUsers": null
}
```

> 新建相册默认 `visibility=0`（公开）。

---

### 4.2 我的相册列表

```
GET /api/album
```

**响应 data:** `[相册对象, ...]`（按创建时间倒序）

---

### 4.3 某人相册列表（公开）

```
GET /api/album/user/{userId}
```

**响应 data:** 该用户可见相册列表（受隐私控制过滤）

> - 未登录 → 只能看到公开/黑名单外的相册
> - 已登录 → 按隐私规则展示

---

### 4.4 相册详情（含照片）

```
GET /api/album/{id}
```

**响应 data:** 相册对象，`photos` 字段包含所有照片：
```json
{
  "id": 1,
  "photos": [
    { "id": 10, "albumId": 1, "url": "/uploads/.../photo1.jpg", "description": "天安门", "sortOrder": 0 },
    { "id": 11, "albumId": 1, "url": "/uploads/.../photo2.jpg", "description": "故宫", "sortOrder": 1 }
  ],
  ...
}
```

> 私有相册非所有者查看会返回 `"该相册未公开"`。

---

### 4.5 修改相册

```
PUT /api/album/{id}
```

**请求体（字段选填）:**
```json
{
  "title":       "新标题",
  "description": "新描述",
  "coverImage":  "/uploads/new_cover.jpg"
}
```

**响应 data:** 更新后的相册对象

> 仅所有者可修改。

---

### 4.6 删除相册

```
DELETE /api/album/{id}
```

**响应 data:** `"相册已删除"`

> 级联删除所有照片和上传文件。仅所有者可删。

---

### 4.7 添加照片

```
POST /api/album/{id}/photo
```

**请求体:**
```json
{
  "url":         "/uploads/2026/06/photo.jpg（必填，来自文件上传返回值）",
  "description": "照片描述（选填）"
}
```

**响应 data:**
```json
{
  "id": 10,
  "albumId": 1,
  "url": "/uploads/2026/06/photo.jpg",
  "description": "照片描述",
  "sortOrder": 0,
  "createdAt": "2026-06-13T10:35:00"
}
```

---

### 4.8 删除照片

```
DELETE /api/album/{albumId}/photo/{photoId}
```

**响应 data:** `"照片已删除"`

---

### 4.9 查看隐私设置

```
GET /api/album/{id}/privacy
```

**响应 data:** 相册对象，含 `visibility` 和 `visibilityUsers`

---

### 4.10 设置相册隐私

```
PUT /api/album/{id}/privacy
```

**请求体:**
```json
{
  "visibility": 2,
  "userIds": [100, 200, 300]
}
```

| visibility | 含义 | userIds |
|:--:|------|:--:|
| 0 | 所有人可见 | 不需要 |
| 1 | 仅自己可见 | 不需要 |
| 2 | 白名单（仅列表用户可见） | 需要 |
| 3 | 黑名单（列表用户不可见） | 需要 |

**响应 data:** `"隐私设置已更新"`

---

### 4.11 一键设置所有相册隐私

```
PUT /api/album/privacy/batch
```

**请求体:** 同上

**响应 data:** `"所有相册的隐私设置已更新"`

> 将该用户的所有相册统一设为相同隐私规则。

---

## 5. 漂流瓶模块 `/api/bottle`

### 5.1 发送漂流瓶

```
POST /api/bottle/send
```

**请求体:**
```json
{
  "textContent": "这是一条漂流瓶（必填）",
  "imageUrl":    "/uploads/2026/06/bottle.jpg（选填）",
  "isAnonymous": 1
}
```

| isAnonymous | 含义 |
|:--:|------|
| 0 | 实名（拾取时显示昵称头像） |
| 1 | 匿名（拾取时不显示个人信息） |

**响应 data:**
```json
{
  "id": 1,
  "senderId": 1,
  "isAnonymous": 1,
  "textContent": "这是一条漂流瓶",
  "imageUrl": "/uploads/2026/06/bottle.jpg",
  "status": 0,
  "createdAt": "2026-06-13T11:00:00",
  "likeCount": null,
  "sender": null
}
```

| status | 含义 |
|:--:|------|
| 0 | 待审核 |
| 1 | 审核通过，可被拾取 |
| 2 | 审核拒绝 |
| 3 | 已被拾取 |

> 新瓶子默认为 `status=0`（待审核），需审核员通过后才进入漂流池。

---

### 5.2 随机拾取漂流瓶

```
GET /api/bottle/pick
```

**响应 data:** 随机一个审核通过的瓶子。

实名瓶子会带 `sender`：
```json
{
  "id": 5,
  "senderId": 42,
  "isAnonymous": 0,
  "textContent": "实名漂流",
  "status": 3,
  "likeCount": 12,
  "sender": {
    "id": 42,
    "username": "traveler",
    "nickname": "旅行者",
    "avatar": "/uploads/avatar.jpg"
  }
}
```

匿名瓶子 `sender` 为 `null`。拾取后瓶子状态变为 `3`（已拾取）。

> 如果没有可拾取的瓶子，返回 `"暂时没有可拾取的漂流瓶"`。

---

### 5.3 点赞

```
POST /api/bottle/{id}/like
```

**响应 data:** `"点赞成功"`

> - 匿名实名均可点赞
> - 重复点赞返回 `"你已经赞过这个漂流瓶了"`

---

### 5.4 取消点赞

```
DELETE /api/bottle/{id}/like
```

**响应 data:** `"已取消点赞"`

---

### 5.5 删除漂流瓶

```
DELETE /api/bottle/{id}
```

**响应 data:** `"漂流瓶已删除"`

> 仅发送者本人可删。级联删除所有点赞。

---

### 5.6 我的漂流瓶列表

```
GET /api/bottle/my
```

**响应 data:** `[漂流瓶对象, ...]`（含点赞数）

---

### 5.7 漂流瓶详情

```
GET /api/bottle/{id}
```

**响应 data:**
```json
{
  "bottle": { ... },
  "liked": true
}
```

> `liked` 表示当前用户是否已点赞该漂流瓶。

---

## 6. 管理员模块 `/api/admin`

### 三级角色

| role | 名称 | 权限 |
|:--:|------|------|
| 0 | 普通用户 | 上述所有功能 |
| 1 | 审核员 | 普通用户 + 漂流瓶审核 |
| 2 | 管理员 | 审核员 + 用户管理 |

---

### 6.1 待审核漂流瓶列表（role ≥ 1）

```
GET /api/admin/bottles/pending
```

**响应 data:** `[漂流瓶对象, ...]`（status=0 的瓶子）

---

### 6.2 审核通过（role ≥ 1）

```
PUT /api/admin/bottles/{id}/approve
```

**响应 data:** `"审核通过"`

---

### 6.3 审核拒绝（role ≥ 1）

```
PUT /api/admin/bottles/{id}/reject
```

**响应 data:** `"已拒绝"`

---

### 6.4 用户列表（role = 2）

```
GET /api/admin/users
```

**响应 data:** `[用户对象, ...]`（按注册时间倒序）

---

### 6.5 创建用户（role = 2）

```
POST /api/admin/users
```

**请求体:**
```json
{
  "username": "string（必填）",
  "password": "string（必填）",
  "nickname": "string（选填，默认取 username）",
  "email":    "string（选填）",
  "role":      1
}
```

| role | 含义 |
|:--:|------|
| 0 | 普通用户（默认） |
| 1 | 审核员 |
| 2 | 管理员 |

**响应 data:** 创建好的用户信息

---

### 6.6 重置密码（role = 2）

```
PUT /api/admin/users/{id}/reset-password
```

**请求体:**
```json
{
  "password": "新密码（必填）"
}
```

**响应 data:** `"密码已重置，该用户需重新登录"`

> 重置后用户 token 自动失效。

---

### 6.7 封禁用户（role = 2）

```
PUT /api/admin/users/{id}/ban
```

**响应 data:** `"用户已封禁"`

> 不能封禁管理员（role ≥ 2）。封禁后用户 token 自动失效。

---

### 6.8 解封用户（role = 2）

```
PUT /api/admin/users/{id}/unban
```

**响应 data:** `"用户已解封"`

---

## 附录：接口速查表

| # | 方法 | 路径 | 认证 |
|:--:|------|------|:--:|
| 1 | POST | `/api/user/register/code` | ❌ |
| 2 | POST | `/api/user/register` | ❌ |
| 3 | POST | `/api/user/login` | ❌ |
| 4 | PUT | `/api/user/password` | ✅ |
| 5 | GET | `/api/user/profile` | ✅ |
| 6 | PUT | `/api/user/profile` | ✅ |
| 7 | PUT | `/api/user/phone/bind` | ✅ |
| 8 | POST | `/api/user/email/code` | ✅ |
| 9 | PUT | `/api/user/email/bind` | ✅ |
| 10 | GET | `/api/public/regions` | ❌ |
| 11 | GET | `/api/public/attractions` | ❌ |
| 12 | GET | `/api/public/attractions/{id}` | ❌ |
| 13 | POST | `/api/file/upload` | ✅ |
| 14 | POST | `/api/file/upload/batch` | ✅ |
| 15 | POST | `/api/album` | ✅ |
| 16 | GET | `/api/album` | ✅ |
| 17 | GET | `/api/album/user/{userId}` | ❌ |
| 18 | GET | `/api/album/{id}` | ❌ |
| 19 | PUT | `/api/album/{id}` | ✅ |
| 20 | DELETE | `/api/album/{id}` | ✅ |
| 21 | POST | `/api/album/{id}/photo` | ✅ |
| 22 | DELETE | `/api/album/{albumId}/photo/{photoId}` | ✅ |
| 23 | GET | `/api/album/{id}/privacy` | ✅ |
| 24 | PUT | `/api/album/{id}/privacy` | ✅ |
| 25 | PUT | `/api/album/privacy/batch` | ✅ |
| 26 | POST | `/api/bottle/send` | ✅ |
| 27 | GET | `/api/bottle/pick` | ✅ |
| 28 | POST | `/api/bottle/{id}/like` | ✅ |
| 29 | DELETE | `/api/bottle/{id}/like` | ✅ |
| 30 | DELETE | `/api/bottle/{id}` | ✅ |
| 31 | GET | `/api/bottle/my` | ✅ |
| 32 | GET | `/api/bottle/{id}` | ✅ |
| 33 | GET | `/api/admin/bottles/pending` | ✅ role≥1 |
| 34 | PUT | `/api/admin/bottles/{id}/approve` | ✅ role≥1 |
| 35 | PUT | `/api/admin/bottles/{id}/reject` | ✅ role≥1 |
| 36 | GET | `/api/admin/users` | ✅ role=2 |
| 37 | POST | `/api/admin/users` | ✅ role=2 |
| 38 | PUT | `/api/admin/users/{id}/reset-password` | ✅ role=2 |
| 39 | PUT | `/api/admin/users/{id}/ban` | ✅ role=2 |
| 40 | PUT | `/api/admin/users/{id}/unban` | ✅ role=2 |
