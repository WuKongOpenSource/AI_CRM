<template>
  <div class="wecom-message-list">
    <div v-if="loading" class="wecom-message-list__state">
      <span class="material-symbols-outlined animate-spin">progress_activity</span>
    </div>
    <div v-else-if="messages.length === 0" class="wecom-message-list__state">
      <span class="material-symbols-outlined">forum</span>
      <span>{{ emptyText }}</span>
    </div>
    <div v-else class="wecom-message-list__items">
      <div
        v-for="message in messages"
        :key="message.id || message.msgId"
        class="wecom-message-list__row"
        :class="message.senderType === 'employee' ? 'is-employee' : 'is-external'"
      >
        <div class="wecom-message-list__avatar">
          {{ avatarLabel(message.senderId) }}
        </div>
        <div class="wecom-message-list__bubble">
          <div class="wecom-message-list__meta">
            <span class="truncate">{{ message.senderId || '-' }}</span>
            <span>{{ formatDate(message.msgTime) }}</span>
          </div>
          <div v-if="message.recalled" class="wecom-message-list__recalled">
            <span class="material-symbols-outlined">undo</span>
            {{ message.contentText || '消息已撤回' }}
          </div>
          <template v-else>
            <p v-if="message.msgType === 'text' || !message.fileUrl" class="wecom-message-list__text">
              {{ message.contentText || messageTypeLabel(message.msgType) }}
            </p>
            <a
              v-if="message.fileUrl"
              class="wecom-message-list__file"
              :href="message.fileUrl"
              target="_blank"
              rel="noreferrer"
            >
              <span class="material-symbols-outlined">attach_file</span>
              <span class="truncate">{{ message.fileName || messageTypeLabel(message.msgType) }}</span>
            </a>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { WecomMessageVO } from '@/types/wecom'

withDefaults(defineProps<{
  messages: WecomMessageVO[]
  loading?: boolean
  emptyText?: string
}>(), {
  loading: false,
  emptyText: '暂无企微会话记录'
})

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function avatarLabel(value?: string) {
  return (value || '?').trim().slice(0, 1).toUpperCase()
}

function messageTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    image: '[图片]',
    file: '[文件]',
    voice: '[语音]',
    link: '[链接]',
    weapp: '[小程序]',
    revoke: '[撤回]'
  }
  return labels[type || ''] || `[${type || '消息'}]`
}
</script>

<style scoped>
.wecom-message-list {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  background: #f8fafc;
}

.wecom-message-list__items {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
}

.wecom-message-list__row {
  display: flex;
  max-width: 78%;
  gap: 10px;
}

.wecom-message-list__row.is-employee {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.wecom-message-list__row.is-external {
  align-self: flex-start;
}

.wecom-message-list__avatar {
  display: flex;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.wecom-message-list__row.is-employee .wecom-message-list__avatar {
  background: #166534;
}

.wecom-message-list__bubble {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  padding: 10px 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.wecom-message-list__row.is-employee .wecom-message-list__bubble {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.wecom-message-list__meta {
  display: flex;
  max-width: 100%;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.wecom-message-list__text {
  margin: 5px 0 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: #111827;
  font-size: 14px;
  line-height: 22px;
}

.wecom-message-list__file,
.wecom-message-list__recalled {
  margin-top: 6px;
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 13px;
  line-height: 20px;
}

.wecom-message-list__file:hover {
  color: #166534;
}

.wecom-message-list__state {
  display: flex;
  height: 100%;
  min-height: 260px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 13px;
}
</style>
