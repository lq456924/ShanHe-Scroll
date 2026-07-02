<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getProfile, updateProfile, changePassword, type UserInfo } from '@/api/user'
import { getMyBottles, getMyLikedBottles } from '@/api/bottle'
import { getMyAlbums } from '@/api/album'

const profile = ref<UserInfo | null>(null)
const tab = ref<'bottles' | 'liked' | 'albums'>('bottles')
const loading = ref(true)

// 数据
const myBottles = ref<any[]>([])
const likedBottles = ref<any[]>([])
const albums = ref<any[]>([])
const dataLoading = ref(false)

// 编辑弹窗
const showEdit = ref(false)
const editForm = ref({ nickname: '', bio: '', location: '', gender: null as number | null })
const editLoading = ref(false)
const editMsg = ref('')

// 改密弹窗
const showPwd = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdLoading = ref(false)
const pwdMsg = ref('')

// 查看漂流瓶内容
const viewBottle = ref<any>(null)

function openViewBottle(b: any) {
  viewBottle.value = b
}

onMounted(async () => {
  try {
    profile.value = await getProfile()
    localStorage.setItem('user', JSON.stringify(profile.value))
  } catch { }
  loading.value = false
  loadData()
})

async function loadData() {
  dataLoading.value = true
  try {
    const [bottles, liked, albs] = await Promise.all([
      getMyBottles(), getMyLikedBottles(), getMyAlbums()
    ])
    myBottles.value = bottles
    likedBottles.value = liked
    albums.value = albs
  } catch { }
  dataLoading.value = false
}

// 编辑资料
function openEdit() {
  editForm.value = {
    nickname: profile.value?.nickname || '',
    bio: profile.value?.bio || '',
    location: profile.value?.location || '',
    gender: profile.value?.gender,
  }
  showEdit.value = true
}

async function handleUpdateProfile() {
  editLoading.value = true
  try {
    const updated = await updateProfile(editForm.value)
    profile.value = updated
    showEdit.value = false
    editMsg.value = ''
  } catch (err: any) { editMsg.value = err.message }
  editLoading.value = false
}

// 修改密码
async function handleChangePwd() {
  pwdMsg.value = ''
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    pwdMsg.value = '两次密码不一致'; return
  }
  pwdLoading.value = true
  try {
    await changePassword(pwdForm.value.oldPassword, pwdForm.value.newPassword)
    pwdMsg.value = '密码修改成功'
  } catch (err: any) { pwdMsg.value = err.message }
  pwdLoading.value = false
}
</script>

<template>
  <div class="profile-page-tiktok">
    <!-- 顶部信息 -->
    <div class="profile-header" v-if="profile">
      <img :src="profile.avatar || '/default-avatar.svg'" class="ph-avatar" alt="" />
      <h2 class="ph-name">{{ profile.nickname || profile.username }}</h2>
      <p class="ph-bio" v-if="profile.bio">{{ profile.bio }}</p>
      <div class="ph-stats">
        <div class="stat"><strong>{{ myBottles.length }}</strong> <span>扔出</span></div>
        <div class="stat"><strong>{{ likedBottles.length }}</strong> <span>点赞</span></div>
        <div class="stat"><strong>{{ albums.length }}</strong> <span>相册</span></div>
      </div>
      <div class="ph-actions">
        <button class="ph-btn" @click="openEdit">编辑资料</button>
        <button class="ph-btn outline" @click="showPwd = true">修改密码</button>
      </div>
    </div>

    <!-- 标签栏 -->
    <div class="ph-tabs">
      <button :class="{ active: tab === 'bottles' }" @click="tab = 'bottles'">我扔出的</button>
      <button :class="{ active: tab === 'liked' }" @click="tab = 'liked'">我点赞的</button>
      <button :class="{ active: tab === 'albums' }" @click="tab = 'albums'">我的相册</button>
    </div>

    <!-- 内容区 -->
    <div v-if="dataLoading" class="ph-loading">加载中...</div>
    <div v-else class="ph-content">
      <template v-if="tab === 'bottles'">
        <div v-if="!myBottles.length" class="ph-empty">还没有扔出漂流瓶</div>
        <div v-for="b in myBottles" :key="b.id" class="bottle-feed-item" @click="openViewBottle(b)">
          <p class="bf-text">{{ b.textContent }}</p>
          <img v-if="b.imageUrl" :src="b.imageUrl" class="bf-img" alt="" />
          <div class="bf-meta">
            <span>{{ b.isAnonymous ? '匿名' : '实名' }}</span>
            <span>❤️ {{ b.likeCount || 0 }}</span>
            <span class="bf-status" :class="'s'+b.status">{{ {0:'待审核',1:'漂流中',2:'已拒绝',3:'已拾取'}[b.status] }}</span>
            <span>{{ b.createdAt?.replace('T',' ').substring(0,10) }}</span>
          </div>
        </div>
      </template>

      <template v-if="tab === 'liked'">
        <div v-if="!likedBottles.length" class="ph-empty">还没有点赞漂流瓶</div>
        <div v-for="b in likedBottles" :key="b.id" class="bottle-feed-item" @click="openViewBottle(b)">
          <p class="bf-text">{{ b.textContent }}</p>
          <img v-if="b.imageUrl" :src="b.imageUrl" class="bf-img" alt="" />
          <div class="bf-meta">
            <span>{{ b.isAnonymous ? '匿名' : '实名' }}</span>
            <span>❤️ {{ b.likeCount || 0 }}</span>
            <span>{{ b.createdAt?.replace('T',' ').substring(0,10) }}</span>
          </div>
        </div>
      </template>

      <template v-if="tab === 'albums'">
        <div v-if="!albums.length" class="ph-empty">还没有相册</div>
        <div class="album-mini-grid">
          <div v-for="a in albums" :key="a.id" class="album-mini-item">
            <img v-if="a.coverImage" :src="a.coverImage" alt="" />
            <span v-else class="no-cover">📷</span>
            <div class="album-mini-name">{{ a.title }}</div>
          </div>
        </div>
      </template>
    </div>

    <!-- 编辑弹窗 -->
    <div v-if="showEdit" class="modal-mask" @click.self="showEdit = false">
      <div class="modal-box">
        <h3>编辑资料</h3>
        <div class="field"><label>昵称</label><input v-model="editForm.nickname" maxlength="20" /></div>
        <div class="field"><label>简介</label><input v-model="editForm.bio" maxlength="200" /></div>
        <div class="field"><label>所在地</label><input v-model="editForm.location" maxlength="100" /></div>
        <p v-if="editMsg" class="msg-error">{{ editMsg }}</p>
        <div class="btns">
          <button class="btn-cancel" @click="showEdit = false">取消</button>
          <button class="btn-ok" :disabled="editLoading" @click="handleUpdateProfile">保存</button>
        </div>
      </div>
    </div>

    <!-- 改密弹窗 -->
    <div v-if="showPwd" class="modal-mask" @click.self="showPwd = false">
      <div class="modal-box">
        <h3>修改密码</h3>
        <div class="field"><label>原密码</label><input v-model="pwdForm.oldPassword" type="password" /></div>
        <div class="field"><label>新密码</label><input v-model="pwdForm.newPassword" type="password" /></div>
        <div class="field"><label>确认密码</label><input v-model="pwdForm.confirmPassword" type="password" /></div>
        <p v-if="pwdMsg" :class="pwdMsg.includes('成功')?'msg-ok':'msg-error'">{{ pwdMsg }}</p>
        <div class="btns">
          <button class="btn-cancel" @click="showPwd = false">取消</button>
          <button class="btn-ok" :disabled="pwdLoading" @click="handleChangePwd">确认</button>
        </div>
      </div>
    </div>

    <!-- 查看漂流瓶 -->
    <div v-if="viewBottle" class="modal-mask" @click.self="viewBottle = null">
    <div class="modal-bottle">
      <button class="modal-close" @click="viewBottle = null">✕</button>
      <div class="bottle-icon">🍾</div>
      <p class="bottle-text">{{ viewBottle.textContent }}</p>
      <img v-if="viewBottle.imageUrl" :src="viewBottle.imageUrl" class="bottle-img" alt="" />
      <div class="bottle-footer">
        <span>{{ viewBottle.isAnonymous ? '匿名' : '实名' }}</span>
        <span>❤️ {{ viewBottle.likeCount || 0 }}</span>
        <span>{{ viewBottle.createdAt?.replace('T',' ').substring(0,16) }}</span>
      </div>
    </div>
  </div>
  </div>
</template>

<style scoped>
.profile-page-tiktok {
  min-height: calc(100vh - 73px);
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  padding-bottom: 40px;
}

/* 顶部 */
.profile-header {
  text-align: center;
  padding: 40px 24px 24px;
}

.ph-avatar {
  width: 96px; height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255,255,255,0.2);
  margin-bottom: 12px;
}

.ph-name {
  font-size: 22px; font-weight: 700;
  color: #fff; margin: 0 0 6px;
}

.ph-bio { font-size: 14px; color: rgba(255,255,255,0.5); margin: 0 0 20px; }

.ph-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 20px;
}

.stat { text-align: center; }
.stat strong { font-size: 20px; color: #fff; display: block; }
.stat span { font-size: 13px; color: rgba(255,255,255,0.5); }

.ph-actions { display: flex; justify-content: center; gap: 12px; }
.ph-btn {
  padding: 8px 28px; border: none; border-radius: 8px;
  background: rgba(255,255,255,0.15); color: #fff;
  font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s;
}
.ph-btn:hover { background: rgba(255,255,255,0.25); }
.ph-btn.outline { background: transparent; border: 1px solid rgba(255,255,255,0.2); }

/* 标签 */
.ph-tabs {
  display: flex;
  justify-content: center;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  padding: 0 24px;
}

.ph-tabs button {
  padding: 14px 24px;
  border: none; background: none;
  font-size: 15px; color: rgba(255,255,255,0.4); cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.2s;
}
.ph-tabs button.active {
  color: #fff; font-weight: 600;
  border-bottom-color: #a78bfa;
}

/* 内容 */
.ph-content { max-width: 600px; margin: 0 auto; padding: 20px 24px; }
.ph-loading, .ph-empty { text-align: center; color: rgba(255,255,255,0.4); padding: 60px 0; font-size: 14px; }

/* 漂流瓶卡片 */
.bottle-feed-item {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.bottle-feed-item:hover {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.15);
}

.bf-text {
  font-size: 15px; color: rgba(255,255,255,0.85);
  line-height: 1.7; margin: 0 0 10px;
}

.bf-img {
  width: 100%; max-height: 240px; object-fit: cover;
  border-radius: 8px; margin-bottom: 10px;
}

.bf-meta {
  display: flex; gap: 14px; font-size: 12px; color: rgba(255,255,255,0.4);
}

.bf-status.s0 { color: #f39c12; }
.bf-status.s1 { color: #27ae60; }
.bf-status.s2 { color: #e74c3c; }
.bf-status.s3 { color: rgba(255,255,255,0.3); }

/* 相册迷你 */
.album-mini-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.album-mini-item { aspect-ratio: 1; border-radius: 10px; overflow: hidden; background: rgba(255,255,255,0.05); position: relative; display: flex; align-items: center; justify-content: center; font-size: 40px; }
.album-mini-item img { width: 100%; height: 100%; object-fit: cover; }
.album-mini-name { position: absolute; bottom: 0; left: 0; right: 0; padding: 4px 8px; background: rgba(0,0,0,0.5); color: #fff; font-size: 12px; text-align: center; }

/* 弹窗 */
.modal-mask { position: fixed; inset: 0; z-index: 500; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.modal-box { width: 380px; background: #fff; border-radius: 14px; padding: 24px 28px; }
.modal-box h3 { margin: 0 0 16px; font-size: 18px; color: #333; }
.field { margin-bottom: 12px; }
.field label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.field input { width: 100%; height: 40px; padding: 0 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; }
.field input:focus { border-color: #667eea; }
.btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.btn-cancel { padding: 8px 16px; border: 1px solid #ddd; border-radius: 8px; background: #fff; color: #666; cursor: pointer; font-size: 14px; }
.btn-ok { padding: 8px 20px; border: none; border-radius: 8px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; font-weight: 500; cursor: pointer; font-size: 14px; }
.btn-ok:disabled { opacity: 0.5; cursor: not-allowed; }
.msg-error { color: #e74c3c; font-size: 13px; }
.msg-ok { color: #27ae60; font-size: 13px; }

/* 查看漂流瓶弹窗 */
.modal-bottle {
  width: 400px;
  max-height: 80vh;
  overflow-y: auto;
  padding: 32px 28px 24px;
  background: linear-gradient(135deg, #fffef5, #fef9e7, #fdf2d1);
  border: 2px solid #e8d5a3;
  border-radius: 16px;
  text-align: center;
  position: relative;
}

.modal-close {
  position: absolute;
  top: 10px; right: 12px;
  width: 28px; height: 28px;
  border: none; border-radius: 50%;
  background: rgba(0,0,0,0.1);
  color: #999; font-size: 14px; cursor: pointer;
}

.bottle-icon { font-size: 40px; margin-bottom: 10px; }

.bottle-text {
  font-family: 'KaiTi', 'STKaiti', '楷体', serif;
  font-size: 16px; color: #4a3f2f; line-height: 1.8;
  padding: 14px;
  background: rgba(255,255,255,0.5);
  border-radius: 10px; border: 1px dashed #e0d5b8;
}

.bottle-img { width: 100%; max-height: 180px; object-fit: cover; border-radius: 8px; margin-top: 10px; }

.bottle-footer { display: flex; justify-content: center; gap: 16px; margin-top: 14px; font-size: 13px; color: #999; }
</style>
