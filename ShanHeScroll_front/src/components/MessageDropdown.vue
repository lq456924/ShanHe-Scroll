<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMessages, getUnreadCount, markAllAsRead } from '@/api/message'
import { acceptInvitation, rejectInvitation } from '@/api/album'
import type { MessageItem } from '@/api/message'

const router = useRouter()
const visible = ref(false)
const messages = ref<MessageItem[]>([])
const unreadCount = ref(0)
let pollTimer: number | null = null
const respondingIds = ref(new Set<number>())

/** 根据消息类型跳转到对应页面 */
const linkableTypes = new Set([
  'BOTTLE_APPROVED', 'BOTTLE_REJECTED', 'BOTTLE_REPORTED',
  'BOTTLE_RE_APPROVED', 'BOTTLE_RE_REJECTED',
  'ATTRACTION_APPROVED', 'ATTRACTION_REJECTED',
  'ALBUM_INVITE',
])

function handleMessageClick(msg: MessageItem) {
  if (msg.type === 'ALBUM_INVITE') {
    visible.value = false
    router.push('/album')
    return
  }
  if (!linkableTypes.has(msg.type)) return
  visible.value = false

  if (msg.type.startsWith('BOTTLE')) {
    const query = msg.relatedId ? { highlight: msg.relatedId } : {}
    router.push({ path: '/bottle', query })
  } else if (msg.type.startsWith('ATTRACTION')) {
    const query = msg.relatedId ? { highlight: msg.relatedId } : {}
    router.push({ path: '/', query })
  }
}

async function handleAcceptInvite(albumId: number) {
  respondingIds.value.add(albumId)
  try {
    await acceptInvitation(albumId)
    messages.value = messages.value.map(m =>
      m.relatedId === albumId && m.type === 'ALBUM_INVITE'
        ? { ...m, content: '你已同意该邀请' }
        : m
    )
  } catch (err: any) { alert(err.message || '操作失败') }
  respondingIds.value.delete(albumId)
}

async function handleRejectInvite(albumId: number) {
  if (!confirm('确定拒绝该邀请？')) return
  respondingIds.value.add(albumId)
  try {
    await rejectInvitation(albumId)
    messages.value = messages.value.map(m =>
      m.relatedId === albumId && m.type === 'ALBUM_INVITE'
        ? { ...m, content: '你已拒绝该邀请' }
        : m
    )
  } catch (err: any) { alert(err.message || '操作失败') }
  respondingIds.value.delete(albumId)
}

const typeLabels: Record<string, string> = {
  BAN: '封禁',
  UNBAN: '解封',
  BOTTLE_APPROVED: '漂流瓶通过',
  BOTTLE_REJECTED: '漂流瓶驳回',
  BOTTLE_REPORTED: '被举报',
  BOTTLE_RE_APPROVED: '重新通过',
  BOTTLE_RE_REJECTED: '重新驳回',
  ATTRACTION_APPROVED: '打卡点通过',
  ATTRACTION_REJECTED: '打卡点驳回',
  ALBUM_INVITE: '相册邀请',
  ALBUM_INVITE_ACCEPTED: '已同意',
  ALBUM_INVITE_REJECTED: '已拒绝',
}

const typeColors: Record<string, string> = {
  BAN: '#e74c3c',
  UNBAN: '#27ae60',
  BOTTLE_APPROVED: '#2ecc71',
  BOTTLE_REJECTED: '#e67e22',
  BOTTLE_REPORTED: '#e74c3c',
  BOTTLE_RE_APPROVED: '#2ecc71',
  BOTTLE_RE_REJECTED: '#e67e22',
  ATTRACTION_APPROVED: '#2ecc71',
  ATTRACTION_REJECTED: '#e67e22',
  ALBUM_INVITE: '#3498db',
  ALBUM_INVITE_ACCEPTED: '#27ae60',
  ALBUM_INVITE_REJECTED: '#999',
}

function formatTime(dateStr: string) {
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

async function loadUnreadCount() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const data = await getUnreadCount()
    unreadCount.value = data.count
  } catch { /* 静默 */ }
}

async function loadMessages() {
  try {
    messages.value = await getMessages()
  } catch { /* 静默 */ }
}

async function togglePanel() {
  visible.value = !visible.value
  if (visible.value) {
    await loadMessages()
    await markAllAsRead()
    unreadCount.value = 0
  }
}

function closePanel(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.msg-wrapper')) {
    visible.value = false
  }
}

function stopProp(e: Event) {
  e.stopPropagation()
}

onMounted(() => {
  loadUnreadCount()
  pollTimer = window.setInterval(loadUnreadCount, 30000)
  document.addEventListener('click', closePanel)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('click', closePanel)
})
</script>

<template>
  <div class="msg-wrapper">
    <button class="msg-icon-btn" @click.stop="togglePanel" title="消息通知">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
      <span v-if="unreadCount > 0" class="msg-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </button>

    <Transition name="msg-panel">
      <div v-if="visible" class="msg-panel" @click.stop="stopProp">
        <div class="msg-panel-header">
          <span>消息通知</span>
        </div>

        <div class="msg-list" v-if="messages.length > 0">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="msg-item"
            :class="{ unread: !msg.isRead, clickable: linkableTypes.has(msg.type) }"
            @click="handleMessageClick(msg)"
          >
            <div class="msg-item-top">
              <span
                class="msg-type-tag"
                :style="{ background: typeColors[msg.type] || '#999' }"
              >{{ typeLabels[msg.type] || msg.type }}</span>
              <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
            <div class="msg-title">{{ msg.title }}</div>
            <div class="msg-content">{{ msg.content }}</div>
            <div v-if="msg.type === 'ALBUM_INVITE' && msg.content !== '你已同意该邀请' && msg.content !== '你已拒绝该邀请'" class="msg-actions">
              <button
                class="btn-accept"
                :disabled="respondingIds.has(msg.relatedId!)"
                @click.stop="handleAcceptInvite(msg.relatedId!)"
              >同意</button>
              <button
                class="btn-reject"
                :disabled="respondingIds.has(msg.relatedId!)"
                @click.stop="handleRejectInvite(msg.relatedId!)"
              >拒绝</button>
            </div>
          </div>
        </div>

        <div v-else class="msg-empty">暂无消息</div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.msg-wrapper {
  position: relative;
}

.msg-icon-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  position: relative;
  flex-shrink: 0;
}

.msg-icon-btn:hover {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.msg-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #e74c3c;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  box-shadow: 0 2px 6px rgba(231, 76, 60, 0.4);
}

/* ---- 浮窗 ---- */
.msg-panel {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  width: 380px;
  max-height: 480px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  z-index: 300;
  overflow: hidden;
}

.msg-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.msg-list {
  overflow-y: auto;
  max-height: 420px;
}

.msg-item {
  padding: 14px 20px;
  border-bottom: 1px solid #f5f5f5;
  cursor: default;
  transition: background 0.15s;
}

.msg-item.clickable {
  cursor: pointer;
}

.msg-item:hover {
  background: #f9f9ff;
}

.msg-item.clickable:hover {
  background: #eef0ff;
}

.msg-item.unread {
  background: #f0f4ff;
}

.msg-item.unread:hover {
  background: #e8ecf8;
}

.msg-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.msg-type-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.msg-time {
  font-size: 12px;
  color: #999;
}

.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.msg-content {
  font-size: 13px;
  color: #888;
  line-height: 1.5;
}

.msg-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.btn-accept, .btn-reject {
  padding: 5px 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-accept {
  background: #27ae60;
  color: #fff;
}

.btn-accept:hover:not(:disabled) {
  background: #219a52;
}

.btn-reject {
  background: #eee;
  color: #888;
}

.btn-reject:hover:not(:disabled) {
  background: #ddd;
  color: #555;
}

.btn-accept:disabled, .btn-reject:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.msg-empty {
  padding: 60px 20px;
  text-align: center;
  color: #bbb;
  font-size: 14px;
}

/* ---- 浮窗动画 ---- */
.msg-panel-enter-active {
  animation: msgSlideIn 0.22s ease-out;
}
.msg-panel-leave-active {
  animation: msgSlideIn 0.16s ease-in reverse;
}

@keyframes msgSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
