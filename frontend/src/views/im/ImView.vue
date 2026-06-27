<template>
  <div style="display:flex;height:100%;width:100%;overflow:hidden;background:#fff;">
    <!-- ============ MIDDLE: CONVERSATION LIST ============ -->
    <section
      style="width:336px;flex:none;border-right:1px solid #ededef;display:flex;flex-direction:column;background:#fff;"
    >
      <div style="display:flex;align-items:center;justify-content:space-between;padding:16px 16px 10px;">
        <div style="font-size:19px;font-weight:800;color:#1d1c1d;">消息</div>
        <button class="wk-im-newchat" @click="openContacts = true">
          <span class="material-symbols-outlined" style="font-size:16px;">add</span>发起聊天
        </button>
      </div>

      <div style="padding:0 16px 10px;">
        <div class="wk-im-search">
          <span class="material-symbols-outlined" style="font-size:16px;color:#9a9a9e;">search</span>
          <input v-model="listKeyword" placeholder="搜索会话、联系人" />
        </div>
      </div>

      <div style="display:flex;gap:6px;padding:0 16px 8px;">
        <button
          v-for="f in filters"
          :key="f.key"
          class="wk-im-chip"
          :class="{ 'is-active': listFilter === f.key }"
          @click="listFilter = f.key"
        >
          {{ f.label }}
        </button>
      </div>

      <div style="flex:1;overflow-y:auto;padding:2px 8px 12px;display:flex;flex-direction:column;gap:1px;">
        <button
          v-for="c in filteredConversations"
          :key="c.id"
          class="wk-im-conv"
          :class="{ 'is-active': c.id === im.activeConversationId }"
          @click="im.selectConversation(c.id)"
        >
          <div style="position:relative;flex:none;">
            <div
              class="wk-im-avatar"
              :style="{ width: '40px', height: '40px', borderRadius: '11px', fontSize: '15px', background: avatarBg(c.peerUserId, c.peerName) }"
            >
              <img v-if="c.peerAvatarUrl" :src="c.peerAvatarUrl" style="width:100%;height:100%;border-radius:11px;object-fit:cover;" />
              <template v-else>{{ avatarText(c.peerName) }}</template>
            </div>
            <span v-if="im.presence[c.peerUserId]" class="wk-im-dot" />
          </div>
          <div style="flex:1;min-width:0;">
            <div style="display:flex;align-items:center;gap:6px;">
              <span
                style="font-size:14px;color:#1d1c1d;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;"
                :style="{ fontWeight: c.unreadCount ? 700 : 500 }"
              >{{ c.peerName }}</span>
              <span style="margin-left:auto;font-size:11px;color:#a7a7ab;flex:none;">{{ relativeTime(c.lastMessage?.createTime || c.updateTime) }}</span>
            </div>
            <div style="display:flex;align-items:center;gap:8px;margin-top:2px;">
              <span style="font-size:12.5px;color:#86868a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;flex:1;text-align:left;">{{ previewOf(c.lastMessage) }}</span>
              <span v-if="c.unreadCount" class="wk-im-unread">{{ c.unreadCount > 99 ? '99+' : c.unreadCount }}</span>
            </div>
          </div>
        </button>
        <div v-if="!filteredConversations.length" style="padding:24px 12px;text-align:center;color:#a7a7ab;font-size:13px;">
          暂无会话，点击「发起聊天」开始
        </div>
      </div>
    </section>

    <!-- ============ RIGHT: CHAT PANEL ============ -->
    <main style="flex:1;min-width:0;display:flex;flex-direction:column;background:#fff;">
      <template v-if="activeConv">
        <!-- header -->
        <div style="height:61px;flex:none;border-bottom:1px solid #ededef;display:flex;align-items:center;justify-content:space-between;padding:0 20px;">
          <div style="display:flex;align-items:center;gap:11px;min-width:0;">
            <div style="position:relative;flex:none;">
              <div
                class="wk-im-avatar"
                :style="{ width: '38px', height: '38px', borderRadius: '10px', fontSize: '15px', background: avatarBg(activeConv.peerUserId, activeConv.peerName) }"
              >
                <img v-if="activeConv.peerAvatarUrl" :src="activeConv.peerAvatarUrl" style="width:100%;height:100%;border-radius:10px;object-fit:cover;" />
                <template v-else>{{ avatarText(activeConv.peerName) }}</template>
              </div>
              <span v-if="im.presence[activeConv.peerUserId]" class="wk-im-dot" />
            </div>
            <div style="min-width:0;">
              <div style="font-size:15.5px;font-weight:700;color:#1d1c1d;">{{ activeConv.peerName }}</div>
              <div style="font-size:12px;color:#86868a;">{{ im.presence[activeConv.peerUserId] ? '在线' : '离线' }}</div>
            </div>
          </div>
        </div>

        <!-- messages -->
        <div ref="scrollEl" style="flex:1;overflow-y:auto;padding:14px 0 8px;">
          <template v-for="row in groupedMessages" :key="row.msg.id">
            <div v-if="row.divider" style="display:flex;align-items:center;gap:12px;margin:14px 22px 12px;">
              <div style="flex:1;height:1px;background:#ececee;"></div>
              <div style="font-size:11.5px;font-weight:600;color:#9a9a9e;background:#fff;border:1px solid #ececee;border-radius:20px;padding:3px 13px;">{{ row.dividerLabel }}</div>
              <div style="flex:1;height:1px;background:#ececee;"></div>
            </div>
            <div
              class="wk-im-msgrow"
              @mouseenter="hoveredMsg = row.msg.id"
              @mouseleave="hoveredMsg = null"
            >
              <div style="width:38px;flex:none;display:flex;flex-direction:column;align-items:flex-end;">
                <div
                  v-if="row.showAvatar"
                  class="wk-im-avatar"
                  :style="{ width: '38px', height: '38px', borderRadius: '10px', fontSize: '14px', marginTop: '3px', background: row.mine ? '#6d4aff' : avatarBg(activeConv.peerUserId, activeConv.peerName) }"
                >{{ row.mine ? meAvatarText : avatarText(activeConv.peerName) }}</div>
              </div>
              <div style="flex:1;min-width:0;padding-top:2px;">
                <div v-if="row.showHeader" style="display:flex;align-items:baseline;gap:8px;margin-bottom:2px;">
                  <span style="font-weight:700;font-size:14.5px;color:#1d1c1d;">{{ row.mine ? '我' : activeConv.peerName }}</span>
                  <span style="font-size:11px;color:#a7a7ab;">{{ clockTime(row.msg.createTime) }}</span>
                </div>
                <div v-if="row.msg.status === 'recalled'" style="font-size:13.5px;color:#a7a7ab;font-style:italic;">该消息已撤回</div>
                <img
                  v-else-if="row.msg.contentType === 'image'"
                  :src="row.msg.attachmentUrl || ''"
                  style="max-width:260px;max-height:300px;border-radius:10px;border:1px solid #ededef;cursor:pointer;"
                  @click="openImage(row.msg.attachmentUrl)"
                />
                <a
                  v-else-if="row.msg.contentType === 'file'"
                  :href="row.msg.attachmentUrl || ''"
                  target="_blank"
                  class="wk-im-file"
                >
                  <span class="material-symbols-outlined" style="font-size:20px;color:#6d4aff;">description</span>
                  <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ row.msg.attachmentName }}</span>
                </a>
                <div v-else style="font-size:14.5px;line-height:1.5;color:#1d1c1d;white-space:pre-wrap;word-break:break-word;">{{ row.msg.content }}</div>
              </div>
              <!-- hover action: recall own recent message -->
              <div
                v-if="hoveredMsg === row.msg.id && row.mine && row.msg.status === 'normal' && canRecall(row.msg)"
                class="wk-im-hoverbar"
              >
                <button title="撤回" @click="im.recall(activeConv.id, row.msg.id)">
                  <span class="material-symbols-outlined" style="font-size:17px;">undo</span>
                </button>
              </div>
            </div>
          </template>
          <div v-if="!groupedMessages.length" style="padding:40px 22px;text-align:center;color:#b0b0b4;font-size:13px;">
            还没有消息，发送第一条吧
          </div>
        </div>

        <!-- composer -->
        <div style="padding:0 22px 16px;flex:none;">
          <div class="wk-im-composer">
            <textarea
              ref="composerEl"
              v-model="draft"
              :placeholder="`发消息给 ${activeConv.peerName}`"
              rows="1"
              @keydown="onComposerKey"
            ></textarea>
            <div style="display:flex;align-items:center;justify-content:space-between;padding:6px 8px 8px;">
              <div style="display:flex;align-items:center;gap:1px;">
                <el-upload :show-file-list="false" :http-request="doUpload" :disabled="uploading">
                  <button class="wk-im-tool" :title="uploading ? '上传中…' : '附件'">
                    <span class="material-symbols-outlined" style="font-size:18px;">{{ uploading ? 'hourglass_top' : 'attach_file' }}</span>
                  </button>
                </el-upload>
              </div>
              <button
                class="wk-im-send"
                :disabled="!draft.trim()"
                :style="{ background: draft.trim() ? '#6d4aff' : '#d7d7da' }"
                title="发送 (Enter)"
                @click="onSend"
              >
                <span class="material-symbols-outlined" style="font-size:18px;color:#fff;">send</span>
              </button>
            </div>
          </div>
          <div style="font-size:11px;color:#b0b0b4;margin-top:6px;padding-left:2px;">
            <b style="color:#9a9a9e;">Enter</b> 发送 · <b style="color:#9a9a9e;">Shift + Enter</b> 换行
          </div>
        </div>
      </template>
      <div v-else style="flex:1;display:flex;align-items:center;justify-content:center;color:#a7a7ab;font-size:14px;">
        选择一个会话开始聊天
      </div>
    </main>

    <!-- contacts picker -->
    <el-dialog v-model="openContacts" title="发起聊天" width="420px" @open="im.refreshContacts()">
      <el-input v-model="contactKeyword" placeholder="搜索同事" class="mb-2" @input="im.refreshContacts(contactKeyword)" />
      <div style="max-height:320px;overflow-y:auto;">
        <div
          v-for="ct in im.contacts"
          :key="ct.userId"
          class="wk-im-contact"
          @click="startChat(ct.userId)"
        >
          <div class="wk-im-avatar" :style="{ width: '32px', height: '32px', borderRadius: '8px', fontSize: '13px', background: avatarBg(ct.userId, ct.name) }">
            <img v-if="ct.avatarUrl" :src="ct.avatarUrl" style="width:100%;height:100%;border-radius:8px;object-fit:cover;" />
            <template v-else>{{ avatarText(ct.name) }}</template>
          </div>
          <span style="font-size:14px;color:#1d1c1d;">{{ ct.name }}</span>
          <span v-if="ct.online" class="wk-im-dot" style="position:static;border:none;width:8px;height:8px;" />
          <span style="margin-left:auto;font-size:12px;color:#a7a7ab;">{{ ct.deptName }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useImStore } from '@/stores/im'
import { useUserStore } from '@/stores/user'
import { post } from '@/utils/request'
import type { ImMessage } from '@/api/im'
import type { UploadRequestOptions } from 'element-plus'

const im = useImStore()
const route = useRoute()
const userStore = useUserStore()
const myId = computed(() => String(userStore.userId ?? ''))
const meAvatarText = computed(() => '我')

const draft = ref('')
const openContacts = ref(false)
const contactKeyword = ref('')
const listKeyword = ref('')
const listFilter = ref<'all' | 'unread'>('all')
const hoveredMsg = ref<string | null>(null)
const uploading = ref(false)
const scrollEl = ref<HTMLElement | null>(null)
const composerEl = ref<HTMLTextAreaElement | null>(null)

const filters = [
  { key: 'all' as const, label: '全部' },
  { key: 'unread' as const, label: '未读' },
]

const AVATAR_PALETTE = ['#7c5cff', '#f5832a', '#2bb673', '#19a3a3', '#0e9f6e', '#3a82f6', '#e8543f', '#9b5de5', '#f15bb5']

function avatarBg(seed?: string | null, fallback?: string | null): string {
  const s = String(seed || fallback || '?')
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return AVATAR_PALETTE[h % AVATAR_PALETTE.length]
}

function avatarText(name?: string | null): string {
  const n = (name || '?').trim()
  return n ? n.slice(0, 1) : '?'
}

function parseTime(s?: string | null): number {
  if (!s) return 0
  const t = new Date(s.replace(' ', 'T')).getTime()
  return Number.isNaN(t) ? 0 : t
}

function clockTime(s?: string | null): string {
  const t = parseTime(s)
  if (!t) return ''
  const d = new Date(t)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function relativeTime(s?: string | null): string {
  const t = parseTime(s)
  if (!t) return ''
  const diff = Date.now() - t
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}分钟`
  const d = new Date(t)
  const today = new Date()
  const sameDay = d.toDateString() === today.toDateString()
  if (sameDay) return clockTime(s)
  const yest = new Date(today)
  yest.setDate(today.getDate() - 1)
  if (d.toDateString() === yest.toDateString()) return '昨天'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function dividerLabel(s?: string | null): string {
  const t = parseTime(s)
  if (!t) return ''
  const d = new Date(t)
  const today = new Date()
  if (d.toDateString() === today.toDateString()) return '今天'
  const yest = new Date(today)
  yest.setDate(today.getDate() - 1)
  if (d.toDateString() === yest.toDateString()) return '昨天'
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const activeConv = computed(() =>
  im.conversations.find((c) => c.id === im.activeConversationId) || null
)

const filteredConversations = computed(() => {
  const kw = listKeyword.value.trim().toLowerCase()
  return im.conversations.filter((c) => {
    if (listFilter.value === 'unread' && !c.unreadCount) return false
    if (kw && !(c.peerName || '').toLowerCase().includes(kw)) return false
    return true
  })
})

interface GroupedRow {
  msg: ImMessage
  mine: boolean
  divider: boolean
  dividerLabel: string
  showAvatar: boolean
  showHeader: boolean
}

const GROUP_GAP_MS = 5 * 60 * 1000

const groupedMessages = computed<GroupedRow[]>(() => {
  const list = im.activeConversationId ? im.messagesByConv[im.activeConversationId] || [] : []
  const rows: GroupedRow[] = []
  let prevSender: string | null = null
  let prevDay = ''
  let prevTime = 0
  for (const msg of list) {
    const mine = msg.senderId === myId.value
    const day = new Date(parseTime(msg.createTime)).toDateString()
    const newDay = day !== prevDay
    const groupBreak = newDay || msg.senderId !== prevSender || parseTime(msg.createTime) - prevTime > GROUP_GAP_MS
    rows.push({
      msg,
      mine,
      divider: newDay,
      dividerLabel: newDay ? dividerLabel(msg.createTime) : '',
      showAvatar: groupBreak,
      showHeader: groupBreak,
    })
    prevSender = msg.senderId
    prevDay = day
    prevTime = parseTime(msg.createTime)
  }
  return rows
})

function previewOf(m?: ImMessage | null) {
  if (!m) return ''
  if (m.status === 'recalled') return '该消息已撤回'
  if (m.contentType === 'image') return '[图片]'
  if (m.contentType === 'file') return `[文件] ${m.attachmentName || ''}`
  return m.content || ''
}

function canRecall(m: ImMessage) {
  return Date.now() - parseTime(m.createTime) < 2 * 60 * 1000
}

function openImage(url?: string | null) {
  if (url) window.open(url, '_blank')
}

function onComposerKey(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    void onSend()
  }
}

async function onSend() {
  const text = draft.value.trim()
  if (!text || !im.activeConversationId) return
  draft.value = ''
  await im.send({ conversationId: im.activeConversationId, contentType: 'text', content: text })
}

async function startChat(userId: string) {
  openContacts.value = false
  await im.openConversationWith(userId)
}

// Presigned upload: POST /file/presigned-upload {fileName, contentType} -> {objectKey, uploadUrl, method}, PUT to MinIO, then send.
async function doUpload(opt: UploadRequestOptions) {
  const convId = im.activeConversationId
  if (!convId) return
  const file = opt.file as File
  uploading.value = true
  try {
    const presignedRes = await post<{ objectKey: string; accessUrl: string; uploadUrl: string; method: string }>(
      '/file/presigned-upload',
      { fileName: file.name, contentType: file.type || 'application/octet-stream' }
    )
    await fetch(presignedRes.uploadUrl, {
      method: presignedRes.method || 'PUT',
      body: file,
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
    })
    const isImage = file.type.startsWith('image/')
    await im.send({
      conversationId: convId,
      contentType: isImage ? 'image' : 'file',
      attachmentName: file.name,
      attachmentPath: presignedRes.objectKey,
      attachmentSize: file.size,
      attachmentMime: file.type,
    })
  } finally {
    uploading.value = false
  }
}

watch(
  groupedMessages,
  async () => {
    await nextTick()
    if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
  },
  { deep: true }
)

onMounted(async () => {
  im.ensureNotificationPermission()
  im.connect()
  await im.refreshConversations()
  const peer = route.query.peer as string | undefined
  if (peer) await im.openConversationWith(peer)
})
</script>

<style scoped>
.wk-im-newchat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: #fff;
  background: #1d1c1d;
  border: none;
  border-radius: 8px;
  padding: 7px 12px;
  cursor: pointer;
}
.wk-im-newchat:hover { background: #000; }

.wk-im-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f3f3f5;
  border: 1px solid transparent;
  border-radius: 9px;
  padding: 8px 11px;
}
.wk-im-search:focus-within { background: #fff; border-color: #6d4aff; }
.wk-im-search input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  width: 100%;
  color: #1d1c1d;
}

.wk-im-chip {
  font-size: 12px;
  font-weight: 500;
  color: #76767a;
  background: transparent;
  border: none;
  border-radius: 20px;
  padding: 5px 12px;
  cursor: pointer;
}
.wk-im-chip:hover { background: #f3f3f5; }
.wk-im-chip.is-active { color: #5b3fe0; background: rgba(109, 74, 255, 0.1); font-weight: 600; }

.wk-im-conv {
  display: flex;
  gap: 11px;
  width: 100%;
  text-align: left;
  border: none;
  cursor: pointer;
  padding: 9px 10px;
  border-radius: 10px;
  align-items: center;
  background: transparent;
}
.wk-im-conv:hover { background: #f3f3f5; }
.wk-im-conv.is-active { background: rgba(109, 74, 255, 0.08); }

.wk-im-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  overflow: hidden;
}

.wk-im-dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2bb673;
  border: 2.5px solid #fff;
}

.wk-im-unread {
  flex: none;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f23b4d;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wk-im-msgrow {
  position: relative;
  display: flex;
  gap: 11px;
  padding: 2px 22px;
  align-items: flex-start;
}
.wk-im-msgrow:hover { background: #fafafa; }

.wk-im-file {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 260px;
  margin-top: 2px;
  padding: 8px 12px;
  border: 1px solid #ededef;
  border-radius: 10px;
  font-size: 13.5px;
  color: #1d1c1d;
  text-decoration: none;
  background: #fafafa;
}
.wk-im-file:hover { background: #f3f3f5; }

.wk-im-hoverbar {
  position: absolute;
  top: -13px;
  right: 22px;
  display: flex;
  background: #fff;
  border: 1px solid #e8e8ea;
  border-radius: 9px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 5;
}
.wk-im-hoverbar button {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5a5a5e;
}
.wk-im-hoverbar button:hover { background: #f3f3f5; }

.wk-im-composer {
  border: 1px solid #d9d9dc;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.wk-im-composer:focus-within { border-color: #6d4aff; box-shadow: 0 0 0 3px rgba(109, 74, 255, 0.1); }
.wk-im-composer textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: 11px 14px 4px;
  font-size: 14.5px;
  font-family: inherit;
  line-height: 1.5;
  color: #1d1c1d;
  background: transparent;
  min-height: 24px;
  max-height: 150px;
  display: block;
}

.wk-im-tool {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 7px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #76767a;
}
.wk-im-tool:hover { background: #f0f0f2; }

.wk-im-send {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 9px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.wk-im-send:disabled { cursor: default; }
.wk-im-send:not(:disabled):hover { opacity: 0.9; }

.wk-im-contact {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  cursor: pointer;
  border-radius: 8px;
}
.wk-im-contact:hover { background: #f3f3f5; }
</style>
