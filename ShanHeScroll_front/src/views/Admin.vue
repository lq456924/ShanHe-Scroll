<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import * as api from '@/api/admin'

const router = useRouter()
const role = ref(0)

onMounted(() => {
  const raw = localStorage.getItem('user')
  if (raw) {
    const u = JSON.parse(raw)
    role.value = u.role || 0
    if (role.value < 1) { router.replace('/'); return }
  } else {
    router.replace('/login')
  }
  loadUsers()
})

// ---- 标签切换 ----
const tab = ref<'users' | 'bottles' | 'attractions'>('users')

// ---- 用户管理 ----
const users = ref<any[]>([])
const showCreateUser = ref(false)
const createForm = ref({ username: '', password: '', nickname: '', email: '', role: 0 })
const createError = ref('')
const createLoading = ref(false)
const keyword = ref('')

const filteredUsers = computed(() => {
  if (!keyword.value) return users.value
  const kw = keyword.value.toLowerCase()
  return users.value.filter((u: any) =>
    u.username?.toLowerCase().includes(kw) ||
    u.nickname?.toLowerCase().includes(kw) ||
    u.email?.toLowerCase().includes(kw)
  )
})

async function loadUsers() {
  try { users.value = await api.getUsers() } catch { }
}

async function handleCreateUser() {
  createError.value = ''
  if (!createForm.value.username || !createForm.value.password) {
    createError.value = '用户名和密码不能为空'
    return
  }
  createLoading.value = true
  try {
    await api.createUser(createForm.value)
    showCreateUser.value = false
    createForm.value = { username: '', password: '', nickname: '', email: '', role: 0 }
    await loadUsers()
  } catch (err: any) { createError.value = err.message }
  createLoading.value = false
}

async function handleResetPwd(user: any) {
  const pwd = prompt(`重置「${user.username}」的密码，请输入新密码：`)
  if (!pwd) return
  try { await api.resetPassword(user.id, pwd); alert('密码已重置') } catch (err: any) { alert(err.message) }
}

async function handleBan(user: any) {
  if (!confirm(`确定${user.status === 1 ? '解封' : '封禁'}「${user.username}」吗？`)) return
  try {
    if (user.status === 1) await api.unbanUser(user.id)
    else await api.banUser(user.id)
    await loadUsers()
  } catch (err: any) { alert(err.message) }
}

// ---- 漂流瓶审核 ----
const pendingBottles = ref<any[]>([])
const bottlesLoading = ref(false)

async function loadBottles() {
  bottlesLoading.value = true
  try { pendingBottles.value = await api.getPendingBottles() } catch { }
  bottlesLoading.value = false
}

async function handleBottleApprove(id: number) {
  try { await api.approveBottle(id); await loadBottles() } catch (err: any) { alert(err.message) }
}

async function handleBottleReject(id: number) {
  try { await api.rejectBottle(id); await loadBottles() } catch (err: any) { alert(err.message) }
}

// ---- 景点审核 ----
const pendingAttractions = ref<any[]>([])
const attractionsLoading = ref(false)

async function loadAttractions() {
  attractionsLoading.value = true
  try { pendingAttractions.value = await api.getPendingAttractions() } catch { }
  attractionsLoading.value = false
}

async function handleAttrApprove(id: number) {
  try { await api.approveAttraction(id); await loadAttractions() } catch (err: any) { alert(err.message) }
}

async function handleAttrReject(id: number) {
  try { await api.rejectAttraction(id); await loadAttractions() } catch (err: any) { alert(err.message) }
}

function switchTab(t: typeof tab.value) {
  tab.value = t
  if (t === 'bottles' && !pendingBottles.value.length) loadBottles()
  if (t === 'attractions' && !pendingAttractions.value.length) loadAttractions()
}
</script>

<template>
  <div class="admin-page">
    <div class="admin-container">
      <h1>🔧 管理员面板</h1>
      <p class="admin-role">当前身份：{{ {0:'普通用户',1:'审核员',2:'管理员'}[role] }}</p>

      <div class="tab-bar">
        <button :class="{ active: tab === 'users' }" @click="switchTab('users')">👥 用户管理</button>
        <button :class="{ active: tab === 'bottles' }" @click="switchTab('bottles')">🍾 漂流瓶审核</button>
        <button :class="{ active: tab === 'attractions' }" @click="switchTab('attractions')">📍 景点审核</button>
      </div>

      <!-- 用户管理 -->
      <div v-if="tab === 'users'" class="tab-content">
        <div class="toolbar">
          <input v-model="keyword" placeholder="搜索用户..." class="search-input" />
          <button v-if="role >= 2" class="btn-primary" @click="showCreateUser = true">+ 创建用户</button>
        </div>

        <table class="data-table">
          <thead><tr>
            <th>ID</th><th>用户名</th><th>昵称</th><th>邮箱</th><th>角色</th><th>状态</th>
            <th v-if="role >= 2">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id" :class="{ banned: u.status === 1 }">
              <td>{{ u.id }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.nickname }}</td>
              <td>{{ u.email || '-' }}</td>
              <td>{{ {0:'用户',1:'审核员',2:'管理员'}[u.role] }}</td>
              <td><span :class="u.status===1?'badge-danger':'badge-ok'">{{ u.status===1?'封禁':'正常' }}</span></td>
              <td v-if="role >= 2">
                <button class="btn-sm" @click="handleResetPwd(u)">改密</button>
                <button :class="u.status===1?'btn-sm-ok':'btn-sm-danger'" @click="handleBan(u)">{{ u.status===1?'解封':'封禁' }}</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!filteredUsers.length" class="empty">暂无数据</p>

        <!-- 创建用户弹窗 -->
        <div v-if="showCreateUser" class="modal-mask" @click.self="showCreateUser = false">
          <div class="modal-box">
            <h3>创建用户</h3>
            <div class="field">
              <label>用户名 *</label><input v-model="createForm.username" maxlength="50" />
            </div>
            <div class="field">
              <label>密码 *</label><input v-model="createForm.password" type="password" />
            </div>
            <div class="field">
              <label>昵称</label><input v-model="createForm.nickname" maxlength="50" />
            </div>
            <div class="field">
              <label>邮箱</label><input v-model="createForm.email" type="email" />
            </div>
            <div class="field">
              <label>角色</label>
              <select v-model="createForm.role">
                <option :value="0">普通用户</option>
                <option :value="1">审核员</option>
                <option v-if="role >= 2" :value="2">管理员</option>
              </select>
            </div>
            <p v-if="createError" class="error-msg">{{ createError }}</p>
            <div class="btns">
              <button class="btn-cancel" @click="showCreateUser = false">取消</button>
              <button class="btn-primary" :disabled="createLoading" @click="handleCreateUser">创建</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 漂流瓶审核 -->
      <div v-if="tab === 'bottles'" class="tab-content">
        <div v-if="bottlesLoading" class="empty">加载中...</div>
        <div v-else-if="!pendingBottles.length" class="empty">暂无待审核漂流瓶</div>
        <div v-else class="review-list">
          <div v-for="b in pendingBottles" :key="b.id" class="review-item">
            <div class="review-content">{{ b.textContent }}</div>
            <img v-if="b.imageUrl" :src="b.imageUrl" class="review-img" alt="" />
            <div class="review-meta">
              <span>{{ b.isAnonymous ? '匿名' : '实名' }}</span>
              <span>{{ b.createdAt?.replace('T',' ').substring(0,16) }}</span>
            </div>
            <div class="review-actions">
              <button class="btn-approve" @click="handleBottleApprove(b.id)">通过</button>
              <button class="btn-reject" @click="handleBottleReject(b.id)">拒绝</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 景点审核 -->
      <div v-if="tab === 'attractions'" class="tab-content">
        <div v-if="attractionsLoading" class="empty">加载中...</div>
        <div v-else-if="!pendingAttractions.length" class="empty">暂无待审核景点</div>
        <div v-else class="review-list">
          <div v-for="a in pendingAttractions" :key="a.id" class="review-item">
            <div class="review-title">{{ a.name }}</div>
            <div class="review-content">{{ a.description }}</div>
            <div class="review-meta">
              <span v-if="a.address">📍 {{ a.address }}</span>
              <span>城市ID: {{ a.regionId }}</span>
            </div>
            <div class="review-actions">
              <button class="btn-approve" @click="handleAttrApprove(a.id)">通过</button>
              <button class="btn-reject" @click="handleAttrReject(a.id)">拒绝</button>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.admin-page {
  min-height: calc(100vh - 73px);
  background: linear-gradient(160deg, #f0f4ff, #f8f5ff, #f5f0fa);
}

.admin-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
}

.admin-container h1 { font-size: 24px; color: #1a1a2e; margin-bottom: 4px; }
.admin-role { color: #888; font-size: 14px; margin: 0 0 24px; }

/* 标签栏 */
.tab-bar { display: flex; gap: 4px; margin-bottom: 24px; background: #e8ecf1; border-radius: 10px; padding: 4px; }
.tab-bar button {
  flex: 1; padding: 10px; border: none; background: transparent; border-radius: 8px;
  font-size: 14px; color: #666; cursor: pointer; transition: all 0.2s;
}
.tab-bar button.active { background: #fff; color: #667eea; font-weight: 600; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }

/* 工具栏 */
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.search-input { flex: 1; height: 40px; padding: 0 14px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; }
.search-input:focus { border-color: #667eea; }

/* 表格 */
.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.04); }
.data-table th { background: #f8f9ff; padding: 12px 14px; text-align: left; font-size: 13px; color: #666; font-weight: 600; }
.data-table td { padding: 10px 14px; font-size: 14px; color: #333; border-top: 1px solid #f0f0f0; }
.data-table tr.banned td { color: #ccc; text-decoration: line-through; }

.badge-ok { color: #27ae60; font-weight: 500; }
.badge-danger { color: #e74c3c; font-weight: 500; }

.btn-primary {
  padding: 8px 20px; border: none; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2); color: #fff;
  font-size: 14px; font-weight: 500; cursor: pointer;
}
.btn-primary:hover { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-sm { padding: 4px 12px; border: 1px solid #ddd; border-radius: 6px; background: #fff; color: #667eea; font-size: 12px; cursor: pointer; margin-right: 6px; }
.btn-sm-danger { padding: 4px 12px; border: none; border-radius: 6px; background: #fee; color: #e74c3c; font-size: 12px; cursor: pointer; }
.btn-sm-ok { padding: 4px 12px; border: none; border-radius: 6px; background: #e8f8e8; color: #27ae60; font-size: 12px; cursor: pointer; }

.empty { text-align: center; color: #aaa; padding: 40px 0; font-size: 14px; }

/* 审核列表 */
.review-list { display: flex; flex-direction: column; gap: 12px; }
.review-item { background: #fff; border-radius: 12px; padding: 18px 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.04); }
.review-title { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 8px; }
.review-content { font-size: 14px; color: #555; line-height: 1.7; margin-bottom: 8px; }
.review-img { max-height: 200px; border-radius: 8px; margin-bottom: 8px; }
.review-meta { display: flex; gap: 16px; font-size: 12px; color: #999; margin-bottom: 12px; }
.review-actions { display: flex; gap: 10px; }
.btn-approve { padding: 6px 20px; border: none; border-radius: 6px; background: #27ae60; color: #fff; font-size: 13px; cursor: pointer; }
.btn-approve:hover { opacity: 0.9; }
.btn-reject { padding: 6px 20px; border: none; border-radius: 6px; background: #e74c3c; color: #fff; font-size: 13px; cursor: pointer; }
.btn-reject:hover { opacity: 0.9; }

/* 弹窗 */
.modal-mask { position: fixed; inset: 0; z-index: 500; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; }
.modal-box { width: 420px; background: #fff; border-radius: 14px; padding: 24px 28px; box-shadow: 0 16px 48px rgba(0,0,0,0.2); }
.modal-box h3 { margin: 0 0 16px; font-size: 18px; }
.field { margin-bottom: 12px; }
.field label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.field input, .field select { width: 100%; height: 40px; padding: 0 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; }
.field input:focus, .field select:focus { border-color: #667eea; }
.error-msg { color: #e74c3c; font-size: 13px; }
.btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.btn-cancel { padding: 8px 16px; border: 1px solid #ddd; border-radius: 8px; background: #fff; color: #666; font-size: 14px; cursor: pointer; }
</style>
