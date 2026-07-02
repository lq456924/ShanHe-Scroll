<script setup lang="ts">
import { ref } from 'vue'
import http, { extractData } from '@/api'
import { sendBottle, likeBottle, reportBottle } from '@/api/bottle'

interface Bottle {
  id: number
  senderId: number
  isAnonymous: number
  textContent: string
  imageUrl: string | null
  status: number
  createdAt: string
  likeCount: number | null
  sender: { id: number; username: string; nickname: string; avatar: string } | null
}

const loading = ref(false)
const pickedBottle = ref<Bottle | null>(null)
const errorMsg = ref('')
const showBottle = ref(false)
const likingBottle = ref(false)
const likedBottle = ref(false)
const currentLikeCount = ref(0)
const reportedBottle = ref(false)

async function handleReport() {
  if (!pickedBottle.value || reportedBottle.value) return
  if (!confirm('确定举报该漂流瓶吗？')) return
  try {
    await reportBottle(pickedBottle.value.id)
    reportedBottle.value = true
  } catch (err: any) { alert(err.message) }
}

async function handleLike() {
  if (!pickedBottle.value || likingBottle.value) return
  likingBottle.value = true
  try {
    await likeBottle(pickedBottle.value.id)
    likedBottle.value = true
    currentLikeCount.value++
  } catch (err: any) {
    if (err.message?.includes('已经赞过')) {
      likedBottle.value = true
    }
  }
  likingBottle.value = false
}

// 发送漂流瓶
const showSend = ref(false)
const sending = ref(false)
const sendError = ref('')
const sendSuccess = ref('')
const sendForm = ref({
  textContent: '',
  imageUrl: '',
  isAnonymous: 0,
})
const uploadingImage = ref(false)
const bottleFileInput = ref<HTMLInputElement>()

function openSend() {
  sendForm.value = { textContent: '', imageUrl: '', isAnonymous: 0 }
  sendError.value = ''
  sendSuccess.value = ''
  showSend.value = true
}

function triggerBottleUpload() {
  bottleFileInput.value?.click()
}

async function handleBottleImage(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const form = new FormData()
  form.append('file', file)
  uploadingImage.value = true
  try {
    const url = await http.post<string>('/file/upload', form).then(extractData)
    sendForm.value.imageUrl = url
  } catch { /* ignore */ }
  uploadingImage.value = false
  if (bottleFileInput.value) bottleFileInput.value.value = ''
}

async function handleSend() {
  sendError.value = ''
  sendSuccess.value = ''
  if (!sendForm.value.textContent.trim()) {
    sendError.value = '请输入漂流瓶内容'
    return
  }

  sending.value = true
  try {
    await sendBottle({
      textContent: sendForm.value.textContent.trim(),
      imageUrl: sendForm.value.imageUrl.trim() || undefined,
      isAnonymous: sendForm.value.isAnonymous,
    } as any)
    sendSuccess.value = '漂流瓶已扔出！审核通过后可被拾取'
    setTimeout(() => { showSend.value = false }, 2000)
  } catch (err: any) {
    sendError.value = err.message || '发送失败'
  }
  sending.value = false
}


// 海面上的瓶子 — 随机位置、大小、浮动节奏
function randomBottle(id: number) {
  return {
    id,
    x: 5 + Math.random() * 88,        // 水平 5%-93%
    top: 15 + Math.random() * 40,      // 垂直 15%-55%（海中不同深度）
    size: 0.8 + Math.random() * 0.6,   // 0.8-1.4x
    delay: Math.random() * 3,
    duration: 3 + Math.random() * 3,   // 浮动周期 3-6s
    drift: (Math.random() - 0.5) * 15, // 水平偏移幅度
  }
}

const floatingBottles = Array.from({ length: 7 }, (_, i) => randomBottle(i))

// 星空：分层次
// 暗星（背景层，小而多）
const dimStars = Array.from({ length: 120 }, (_, i) => ({
  id: i,
  x: Math.random() * 100,
  y: Math.random() * 55,
  size: 0.5 + Math.random() * 1.5,
  delay: Math.random() * 4,
  duration: 2 + Math.random() * 3,
  opacity: 0.15 + Math.random() * 0.3,
}))
// 亮星（中层）
const brightStars = Array.from({ length: 40 }, (_, i) => ({
  id: i + 200,
  x: Math.random() * 100,
  y: Math.random() * 50,
  size: 1.5 + Math.random() * 2.5,
  delay: Math.random() * 3,
  duration: 1.5 + Math.random() * 2,
}))

// 波光粼粼 — 不同层次
const deepSparkles = Array.from({ length: 50 }, (_, i) => ({
  id: i,
  x: Math.random() * 100,
  y: 58 + Math.random() * 30,
  delay: Math.random() * 3,
  size: 0.5 + Math.random() * 1.5,
  opacity: 0.3 + Math.random() * 0.3,
}))
const brightSparkles = Array.from({ length: 25 }, (_, i) => ({
  id: i + 100,
  x: Math.random() * 100,
  y: 55 + Math.random() * 15,
  delay: Math.random() * 2,
  size: 1.5 + Math.random() * 3,
}))

async function pickBottle() {
  errorMsg.value = ''
  loading.value = true
  showBottle.value = false
  try {
    pickedBottle.value = await http.get<Bottle>('/bottle/pick').then(extractData)
    showBottle.value = true
    likedBottle.value = false
    reportedBottle.value = false
    currentLikeCount.value = pickedBottle.value?.likeCount || 0
  } catch (err: any) {
    errorMsg.value = err.message || '暂时没有可拾取的漂流瓶'
    pickedBottle.value = null
  }
  loading.value = false
}

function closeBottle() {
  showBottle.value = false
  pickedBottle.value = null
}

</script>

<template>
  <div class="bottle-page">
    <!-- 星空 -->
    <div class="sky">
      <!-- 银河光带 -->
      <div class="milky-way"></div>

      <div class="moon"></div>

      <!-- 暗星层 -->
      <div
        v-for="s in dimStars"
        :key="'d'+s.id"
        class="star dim"
        :style="{
          left: s.x + '%',
          top: s.y + '%',
          width: s.size + 'px',
          height: s.size + 'px',
          animationDelay: s.delay + 's',
          animationDuration: s.duration + 's',
          opacity: s.opacity,
        }"
      ></div>

      <!-- 亮星层 -->
      <div
        v-for="s in brightStars"
        :key="'b'+s.id"
        class="star bright"
        :style="{
          left: s.x + '%',
          top: s.y + '%',
          width: s.size + 'px',
          height: s.size + 'px',
          animationDelay: s.delay + 's',
          animationDuration: s.duration + 's',
        }"
      ></div>

    </div>

    <!-- 海洋 -->
    <div class="ocean">
      <div class="wave wave-1"></div>
      <div class="wave wave-2"></div>
      <div class="wave wave-3"></div>
      <div class="wave wave-4"></div>
      <div class="wave wave-5"></div>

      <!-- 深水波光 -->
      <div
        v-for="sp in deepSparkles"
        :key="'d'+sp.id"
        class="sparkle deep"
        :style="{
          left: sp.x + '%',
          top: sp.y + '%',
          width: sp.size + 'px',
          height: sp.size + 'px',
          animationDelay: sp.delay + 's',
          opacity: sp.opacity,
        }"
      ></div>
      <!-- 亮波光 -->
      <div
        v-for="sp in brightSparkles"
        :key="'b'+sp.id"
        class="sparkle bright"
        :style="{
          left: sp.x + '%',
          top: sp.y + '%',
          width: sp.size + 'px',
          height: sp.size + 'px',
          animationDelay: sp.delay + 's',
        }"
      ></div>
      <!-- 月光倒影 -->
      <div class="moon-reflection"></div>

      <!-- 漂浮的瓶子 -->
      <div
        v-for="fb in floatingBottles"
        :key="fb.id"
        class="float-bottle"
        :style="{
          left: fb.x + '%',
          top: fb.top + '%',
          animationDelay: fb.delay + 's',
          animationDuration: fb.duration + 's',
          fontSize: (28 * fb.size) + 'px',
          '--drift': fb.drift + 'px',
        }"
        @click="pickBottle"
        title="拾取漂流瓶"
      >
        🍾
      </div>
    </div>

    <!-- 沙滩 -->
    <div class="beach">
      <div class="sand"></div>
      <div class="sand-light"></div>
    </div>

    <!-- 按钮区 -->
    <div class="action-area">
      <button class="pick-btn" :disabled="loading" @click="pickBottle">
        {{ loading ? '寻找中...' : '🫧 拾取漂流瓶' }}
      </button>
      <button class="send-btn" @click="openSend">📝 扔出漂流瓶</button>
    </div>

    <!-- 错误提示 -->
    <p v-if="errorMsg" class="bottle-error">{{ errorMsg }}</p>

    <!-- 漂流瓶内容弹窗 -->
    <div v-if="showBottle && pickedBottle" class="bottle-modal" @click.self="closeBottle">
      <div class="bottle-message">
        <button class="bottle-close" @click="closeBottle">✕</button>
        <div class="bottle-icon">🍾</div>
        <div class="bottle-text">{{ pickedBottle.textContent }}</div>
        <img v-if="pickedBottle.imageUrl" :src="pickedBottle.imageUrl" class="bottle-img" alt="" />
        <div class="bottle-footer">
          <span v-if="pickedBottle.isAnonymous === 0 && pickedBottle.sender" class="bottle-sender">
            — {{ pickedBottle.sender.nickname || pickedBottle.sender.username }}
          </span>
          <span v-else class="bottle-sender anonymous">— 匿名</span>
          <span class="bottle-time">{{ pickedBottle.createdAt?.replace('T', ' ').substring(0, 16) }}</span>
        </div>
        <div class="bottle-likes">
          <button
            class="like-btn"
            :class="{ liked: likedBottle }"
            :disabled="likingBottle || likedBottle"
            @click="handleLike"
          >
            {{ likedBottle ? '❤️' : '🤍' }} {{ currentLikeCount }}
          </button>
          <button
            class="report-btn"
            :disabled="reportedBottle"
            @click="handleReport"
          >
            {{ reportedBottle ? '已举报' : '🚩 举报' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 发送漂流瓶弹窗 -->
    <div v-if="showSend" class="modal-overlay" @click.self="showSend = false">
      <div class="send-card">
        <h3>📝 扔出漂流瓶</h3>
        <div class="field">
          <label>内容 *</label>
          <textarea v-model="sendForm.textContent" rows="5" maxlength="500"
            placeholder="写下你想说的话...&#10;（审核通过后才会被他人拾取）"></textarea>
          <span class="char-count">{{ sendForm.textContent.length }}/500</span>
        </div>
        <div class="field">
          <label>图片（选填）</label>
          <div class="img-upload-row">
            <button class="upload-btn" @click="triggerBottleUpload" :disabled="uploadingImage">
              {{ uploadingImage ? '上传中...' : '选择图片' }}
            </button>
            <span v-if="sendForm.imageUrl" class="upload-ok">✅ 已上传</span>
          </div>
          <img v-if="sendForm.imageUrl" :src="sendForm.imageUrl" class="send-preview" alt="" />
        </div>
        <input ref="bottleFileInput" type="file" accept="image/*" style="display:none" @change="handleBottleImage" />
        <div class="field">
          <label class="anon-label">
            <input type="checkbox" v-model="sendForm.isAnonymous" :true-value="1" :false-value="0" />
            匿名发送（拾取者看不到你的信息）
          </label>
        </div>
        <p v-if="sendError" class="send-error">{{ sendError }}</p>
        <p v-if="sendSuccess" class="send-success">{{ sendSuccess }}</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showSend = false">取消</button>
          <button class="btn-submit" :disabled="sending" @click="handleSend">
            {{ sending ? '发送中...' : '扔出' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bottle-page {
  position: relative;
  height: calc(100vh - 73px);
  overflow: hidden;
  background: linear-gradient(180deg, #0a0a2e 0%, #0d1b3e 25%, #162d50 40%, #1a5276 50%, #1f6f8b 55%, #1a5276 58%, #0d3b5c 62%, #2980b9 64%, #3498db 68%, #85c1e9 76%, #f9d89c 80%, #f5c77a 90%, #e8b960 95%, #d4a44a 100%);
}

/* 星空 */
.sky {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 55%;
}

/* 银河光带 */
.milky-way {
  position: absolute;
  top: 5%;
  left: -20%;
  width: 140%;
  height: 50%;
  background:
    radial-gradient(ellipse at 30% 40%, rgba(167,139,250,0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 60% 30%, rgba(120,100,200,0.06) 0%, transparent 45%),
    radial-gradient(ellipse at 45% 50%, rgba(140,110,220,0.05) 0%, transparent 55%);
  filter: blur(20px);
  transform: rotate(-15deg);
  pointer-events: none;
}

.moon {
  position: absolute;
  top: 8%;
  right: 15%;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #fffde7, #fff9c4 40%, #ffcc80 100%);
  box-shadow: 0 0 40px rgba(255,249,196,0.6), 0 0 80px rgba(255,249,196,0.3), 0 0 120px rgba(255,200,100,0.15);
}

.star {
  position: absolute;
  border-radius: 50%;
  background: #fff;
}

.star.dim {
  animation: twinkleDim 3s ease-in-out infinite;
}

.star.bright {
  animation: twinkleBright 2s ease-in-out infinite;
  box-shadow: 0 0 2px #fff, 0 0 6px rgba(200,210,255,0.6);
}

@keyframes twinkleDim {
  0%, 100% { opacity: 0.2; transform: scale(0.6); }
  40% { opacity: 0.7; transform: scale(1.1); }
  70% { opacity: 0.3; transform: scale(0.8); }
}

@keyframes twinkleBright {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.5); box-shadow: 0 0 3px #fff, 0 0 10px rgba(200,210,255,0.8); }
}

/* 海洋 */
.ocean {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  bottom: 16%;
  overflow: hidden;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(26,82,118,0.1) 5%,
    rgba(26,82,118,0.15) 10%,
    rgba(13,59,92,0.25) 50%,
    rgba(26,82,118,0.15) 90%,
    transparent 100%
  );
}

/* 波浪层 */
.wave {
  position: absolute;
  left: -50%;
  width: 200%;
  border-radius: 42%;
  opacity: 0.7;
}

.wave-1 {
  top: -8px;
  height: 70px;
  background: linear-gradient(180deg,
    rgba(133,193,233,0.5) 0%,
    rgba(52,152,219,0.4) 40%,
    rgba(41,128,185,0.2) 100%
  );
  animation: waveDrift 7s ease-in-out infinite;
}

.wave-2 {
  top: 12px;
  height: 55px;
  background: linear-gradient(180deg,
    rgba(174,214,241,0.45) 0%,
    rgba(93,173,226,0.35) 50%,
    rgba(46,134,193,0.15) 100%
  );
  animation: waveDrift 5.5s ease-in-out infinite reverse;
}

.wave-3 {
  top: 0px;
  height: 65px;
  background: linear-gradient(180deg,
    rgba(165,203,235,0.4) 0%,
    rgba(65,163,216,0.3) 50%,
    rgba(33,125,176,0.1) 100%
  );
  animation: waveDrift 9s ease-in-out infinite;
}

.wave-4 {
  top: 25px;
  height: 40px;
  background: rgba(133,193,233,0.25);
  animation: waveDrift 12s ease-in-out infinite reverse;
}

.wave-5 {
  top: -15px;
  height: 35px;
  background: rgba(200,225,245,0.35);
  animation: waveDrift 4s ease-in-out infinite;
}

@keyframes waveDrift {
  0% { transform: translateX(0) translateY(0); }
  25% { transform: translateX(12%) translateY(-3px); }
  50% { transform: translateX(25%) translateY(0); }
  75% { transform: translateX(38%) translateY(2px); }
  100% { transform: translateX(50%) translateY(0); }
}

/* 波光 */
.sparkle {
  position: absolute;
  border-radius: 50%;
  background: #fff;
}

.sparkle.deep {
  animation: sparkleDeep 2.5s ease-in-out infinite;
}

.sparkle.bright {
  animation: sparkleBright 2s ease-in-out infinite;
  box-shadow: 0 0 3px rgba(255,255,255,0.8), 0 0 8px rgba(255,255,255,0.3);
}

@keyframes sparkleDeep {
  0%, 100% { opacity: 0.05; transform: scale(0.5); }
  30% { opacity: 0.4; transform: scale(1); }
  60% { opacity: 0.1; transform: scale(0.7); }
}

@keyframes sparkleBright {
  0%, 100% { opacity: 0.1; transform: scale(0.6); }
  40% { opacity: 0.9; transform: scale(1.3); }
  70% { opacity: 0.2; transform: scale(0.8); }
}

/* 月光倒影 */
.moon-reflection {
  position: absolute;
  top: 8%;
  right: 12%;
  width: 20px;
  height: 160px;
  background: linear-gradient(180deg,
    rgba(255,249,196,0.2) 0%,
    rgba(255,249,196,0.08) 30%,
    rgba(255,249,196,0.02) 60%,
    transparent 100%
  );
  border-radius: 50%;
  filter: blur(8px);
  animation: reflectionShimmer 3s ease-in-out infinite;
  pointer-events: none;
}

@keyframes reflectionShimmer {
  0%, 100% { opacity: 0.6; width: 20px; }
  50% { opacity: 1; width: 30px; }
}

/* 漂浮瓶子 */
.float-bottle {
  position: absolute;
  cursor: pointer;
  filter: drop-shadow(0 2px 6px rgba(0,0,0,0.3));
  animation: bobFloat var(--duration, 4s) ease-in-out infinite;
  z-index: 5;
  transition: filter 0.3s;
}

.float-bottle:hover {
  filter: drop-shadow(0 4px 12px rgba(255,255,255,0.4));
}

@keyframes bobFloat {
  0%, 100% { translate: 0 -2px; rotate: -2deg; }
  20% { translate: var(--drift, 5px) -16px; rotate: 2deg; }
  50% { translate: calc(var(--drift, 5px) * -0.5) -6px; rotate: -1deg; }
  75% { translate: var(--drift, 5px) -20px; rotate: 3deg; }
}

/* 沙滩 */
.beach {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 18%;
}

.sand {
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #f9d89c 0%, #f5c77a 20%, #e8b960 50%, #d4a44a 80%, #c4963c 100%);
  border-radius: 60% 70% 0 0 / 20px 30px 0 0;
}

.sand-light {
  position: absolute;
  top: 8px;
  left: 10%;
  right: 10%;
  height: 40px;
  background: rgba(255,255,255,0.12);
  border-radius: 50%;
  filter: blur(10px);
}

/* 按钮区 */
.action-area {
  position: absolute;
  bottom: 8%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  gap: 16px;
}

.pick-btn, .send-btn {
  padding: 14px 36px;
  border: 2px solid rgba(255,255,255,0.3);
  border-radius: 30px;
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(10px);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.pick-btn:hover:not(:disabled), .send-btn:hover {
  background: rgba(255,255,255,0.2);
  border-color: rgba(255,255,255,0.5);
  box-shadow: 0 4px 20px rgba(255,255,255,0.2);
}

.pick-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bottle-error {
  position: absolute;
  bottom: 4%;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255,255,255,0.7);
  font-size: 14px;
  z-index: 10;
}

/* 漂流瓶内容 */
.bottle-modal {
  position: fixed;
  inset: 0;
  z-index: 500;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottle-message {
  position: relative;
  width: 420px;
  max-height: 80vh;
  overflow-y: auto;
  padding: 40px 32px 28px;
  background: linear-gradient(135deg, #fffef5, #fef9e7, #fdf2d1);
  border: 2px solid #e8d5a3;
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.3), inset 0 0 30px rgba(0,0,0,0.05);
  animation: bottleIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes bottleIn {
  from { opacity: 0; transform: scale(0.7) rotate(-10deg); }
  to { opacity: 1; transform: scale(1) rotate(0); }
}

.bottle-close {
  position: absolute;
  top: 12px;
  right: 14px;
  width: 28px; height: 28px;
  border: none; border-radius: 50%;
  background: rgba(0,0,0,0.1);
  color: #999;
  font-size: 14px;
  cursor: pointer;
}

.bottle-close:hover { background: rgba(0,0,0,0.2); }

.bottle-icon {
  text-align: center;
  font-size: 48px;
  margin-bottom: 12px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15));
}

.bottle-text {
  font-family: 'KaiTi', 'STKaiti', '楷体', serif;
  font-size: 17px;
  color: #4a3f2f;
  line-height: 1.8;
  text-align: center;
  padding: 16px;
  background: rgba(255,255,255,0.5);
  border-radius: 12px;
  border: 1px dashed #e0d5b8;
}

.bottle-img {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border-radius: 10px;
  margin-top: 12px;
}

.bottle-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 13px;
}

.bottle-sender {
  color: #8b7355;
  font-weight: 500;
}

.bottle-sender.anonymous {
  color: #bbb;
  font-style: italic;
}

.bottle-time {
  color: #bbb;
}

.bottle-likes {
  text-align: center;
  margin-top: 10px;
}

.like-btn {
  padding: 6px 20px;
  border: 1px solid #e8d5a3;
  border-radius: 20px;
  background: #fffef5;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.like-btn:hover:not(:disabled) {
  border-color: #e74c3c;
  background: #fff5f5;
}

.like-btn.liked {
  border-color: #e74c3c;
  background: #fff0f0;
  color: #e74c3c;
  cursor: default;
}

.like-btn:disabled { cursor: default; }

.report-btn {
  padding: 6px 16px;
  border: 1px solid #e8d5a3;
  border-radius: 20px;
  background: #fffef5;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s;
}

.report-btn:hover:not(:disabled) {
  border-color: #e74c3c;
  color: #e74c3c;
}

.report-btn:disabled { color: #ccc; cursor: default; }

/* 发送弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 500;
  background: rgba(0,0,0,0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-card {
  width: 440px;
  background: #fff;
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.3);
}

.send-card h3 {
  margin: 0 0 20px;
  font-size: 18px;
  color: #333;
}

.send-card .field {
  margin-bottom: 16px;
}

.send-card label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.send-card textarea,
.send-card input[type="text"] {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  resize: vertical;
}

.send-card textarea:focus,
.send-card input:focus {
  border-color: #667eea;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 12px;
  color: #bbb;
  margin-top: 4px;
}

.anon-label {
  display: flex !important;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px !important;
  color: #555 !important;
}

.anon-label input {
  width: auto !important;
}

.send-error { color: #e74c3c; font-size: 13px; margin: 8px 0 0; }
.send-success { color: #27ae60; font-size: 13px; margin: 8px 0 0; }

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #fff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
}

.btn-submit {
  padding: 8px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.img-upload-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-btn {
  padding: 8px 16px;
  border: 1px solid #667eea;
  border-radius: 8px;
  background: #fff;
  color: #667eea;
  font-size: 13px;
  cursor: pointer;
}

.upload-btn:hover { background: #f0edff; }
.upload-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.upload-ok { font-size: 13px; color: #27ae60; }

.send-preview {
  width: 100%;
  max-height: 150px;
  object-fit: cover;
  border-radius: 8px;
  margin-top: 8px;
}
</style>
