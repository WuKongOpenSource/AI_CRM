<template>
  <div class="flex h-full" :class="{ 'flex-col': isMobile }">
    <MobileChatTopHeader
      :visible="showMobileChatHeader"
      :kind="mobileChatHeaderKind"
      :title="mobileChatHeaderTitle"
      :avatar-url="mobileChatHeaderAvatarUrl"
      :detailable="Boolean(chatObjectKind)"
      @menu="openMobileMainMenu"
      @title="handleMobileHeaderTitle"
      @detail="openMobileObjectDetail"
      @new-session="handleNewSession"
    />

    <!-- Internal Sidebar: Chat History -->
    <aside
      v-if="!isMobile || mobilePanel === 'sessions'"
      :class="isMobile ? 'flex-1 flex flex-col bg-slate-50/50' : 'w-72 border-r border-slate-100 bg-slate-50/50 flex flex-col shrink-0'"
    >
      <div class="p-6 pb-2">
        <button
          class="w-full flex items-center justify-center gap-2 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-700 shadow-sm hover:bg-slate-50 transition-all"
          @click="handleNewSession"
        >
          <span class="material-symbols-outlined wk-plus-button-icon">add</span>
          开启新对话
        </button>
      </div>

      <!-- System Notifications Menu Item -->
      <!-- <div class="px-3 py-4">
        <button
          class="w-full flex items-center gap-3 p-3 rounded-xl transition-all text-left"
          :class="currentView === 'notifications'
            ? 'bg-primary/10 text-primary border border-primary/20 shadow-sm'
            : 'hover:bg-slate-100/50 text-slate-600 border border-transparent'"
          @click="currentView = 'notifications'; isMobile && (mobilePanel = 'chat')"
        >
          <span class="material-symbols-outlined" :class="currentView === 'notifications' ? 'fill-1' : ''">notifications</span>
          <div class="flex-1">
            <p class="text-sm font-bold">系统通知</p>
            <p class="text-xs opacity-60">{{ notifications.length }} 条未读消息</p>
          </div>
          <div v-if="currentView !== 'notifications'" class="size-2 rounded-full bg-primary animate-pulse"></div>
        </button>
      </div> -->

      <!-- Divider -->
      <div class="mx-6 h-px bg-slate-100 mb-4"></div>

      <!-- Session List -->
      <div class="flex-1 overflow-y-auto px-3 space-y-1">
        <p class="px-3 text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">最近对话</p>

        <div v-if="chatStore.sessionsLoading && chatStore.sessions.length === 0" class="flex justify-center py-8">
          <span class="material-symbols-outlined text-slate-300 animate-spin">progress_activity</span>
        </div>

        <div v-else-if="chatStore.sessions.length === 0" class="px-3 py-8 text-center text-slate-400 text-xs">
          暂无对话记录
        </div>

        <template v-else>
          <!-- Today -->
          <template v-if="groupedSessions.today.length > 0">
            <p class="px-3 pt-3 pb-1 text-xs font-bold text-slate-400 uppercase tracking-widest">今天</p>
            <button
              v-for="session in groupedSessions.today"
              :key="session.sessionId"
              class="w-full min-w-0 overflow-hidden rounded-xl border p-3 text-left transition-all group"
              :class="isSessionActive(session.sessionId)
                ? 'border-slate-200 bg-white shadow-sm shadow-slate-200/80'
                : 'border-transparent hover:bg-slate-100/50'"
              @click="handleSelectSession(session.sessionId)"
            >
              <div class="flex min-w-0 items-center gap-2">
                <span
                  class="block min-w-0 flex-1 truncate text-sm font-semibold leading-5"
                  :class="isSessionActive(session.sessionId) ? 'text-primary' : 'text-slate-700'"
                  :title="session.title || '新对话'"
                >{{ session.title || '新对话' }}</span>
                <span
                  class="material-symbols-outlined inline-flex w-5 shrink-0 items-center justify-center text-[14px] leading-none transition-all"
                  :class="session.pinned
                    ? 'text-primary opacity-100'
                    : isSessionActive(session.sessionId)
                      ? 'text-slate-400 opacity-100 hover:text-primary'
                      : 'pointer-events-none text-slate-300 opacity-0 group-hover:pointer-events-auto group-hover:opacity-100 hover:text-primary'"
                  @click.stop="handleToggleSessionPin(session)"
                >push_pin</span>
                <span
                  class="material-symbols-outlined inline-flex w-5 shrink-0 items-center justify-center text-[14px] leading-none text-slate-300 transition-all"
                  :class="isSessionActive(session.sessionId)
                    ? 'pointer-events-auto opacity-100'
                    : 'pointer-events-none opacity-0 group-hover:pointer-events-auto group-hover:opacity-100 hover:text-red-500'"
                  @click.stop="handleDeleteSession(session.sessionId)"
                >close</span>
              </div>
              <span class="text-xs text-slate-400 font-medium">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
            </button>
          </template>

          <!-- Yesterday -->
          <template v-if="groupedSessions.yesterday.length > 0">
            <p class="px-3 pt-3 pb-1 text-xs font-bold text-slate-400 uppercase tracking-widest">昨天</p>
            <button
              v-for="session in groupedSessions.yesterday"
              :key="session.sessionId"
              class="w-full min-w-0 overflow-hidden rounded-xl border p-3 text-left transition-all group"
              :class="isSessionActive(session.sessionId)
                ? 'border-slate-200 bg-white shadow-sm shadow-slate-200/80'
                : 'border-transparent hover:bg-slate-100/50'"
              @click="handleSelectSession(session.sessionId)"
            >
              <div class="flex min-w-0 items-center gap-2">
                <span
                  class="block min-w-0 flex-1 truncate text-sm font-semibold leading-5"
                  :class="isSessionActive(session.sessionId) ? 'text-primary' : 'text-slate-700'"
                  :title="session.title || '新对话'"
                >{{ session.title || '新对话' }}</span>
                <span
                  class="material-symbols-outlined inline-flex w-5 shrink-0 items-center justify-center text-[14px] leading-none transition-all"
                  :class="session.pinned
                    ? 'text-primary opacity-100'
                    : isSessionActive(session.sessionId)
                      ? 'text-slate-400 opacity-100 hover:text-primary'
                      : 'pointer-events-none text-slate-300 opacity-0 group-hover:pointer-events-auto group-hover:opacity-100 hover:text-primary'"
                  @click.stop="handleToggleSessionPin(session)"
                >push_pin</span>
                <span
                  class="material-symbols-outlined inline-flex w-5 shrink-0 items-center justify-center text-[14px] leading-none text-slate-300 transition-all"
                  :class="isSessionActive(session.sessionId)
                    ? 'pointer-events-auto opacity-100'
                    : 'pointer-events-none opacity-0 group-hover:pointer-events-auto group-hover:opacity-100 hover:text-red-500'"
                  @click.stop="handleDeleteSession(session.sessionId)"
                >close</span>
              </div>
              <span class="text-xs text-slate-400 font-medium">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
            </button>
          </template>

          <!-- Earlier -->
          <template v-if="groupedSessions.earlier.length > 0">
            <p class="px-3 pt-3 pb-1 text-xs font-bold text-slate-400 uppercase tracking-widest">更早</p>
            <button
              v-for="session in groupedSessions.earlier"
              :key="session.sessionId"
              class="w-full min-w-0 overflow-hidden rounded-xl border p-3 text-left transition-all group"
              :class="isSessionActive(session.sessionId)
                ? 'border-slate-200 bg-white shadow-sm shadow-slate-200/80'
                : 'border-transparent hover:bg-slate-100/50'"
              @click="handleSelectSession(session.sessionId)"
            >
              <div class="flex min-w-0 items-center gap-2">
                <span
                  class="block min-w-0 flex-1 truncate text-sm font-semibold leading-5"
                  :class="isSessionActive(session.sessionId) ? 'text-primary' : 'text-slate-700'"
                  :title="session.title || '新对话'"
                >{{ session.title || '新对话' }}</span>
                <span
                  class="material-symbols-outlined inline-flex w-5 shrink-0 items-center justify-center text-[14px] leading-none transition-all"
                  :class="session.pinned
                    ? 'text-primary opacity-100'
                    : isSessionActive(session.sessionId)
                      ? 'text-slate-400 opacity-100 hover:text-primary'
                      : 'pointer-events-none text-slate-300 opacity-0 group-hover:pointer-events-auto group-hover:opacity-100 hover:text-primary'"
                  @click.stop="handleToggleSessionPin(session)"
                >push_pin</span>
                <span
                  class="material-symbols-outlined inline-flex w-5 shrink-0 items-center justify-center text-[14px] leading-none text-slate-300 transition-all"
                  :class="isSessionActive(session.sessionId)
                    ? 'pointer-events-auto opacity-100'
                    : 'pointer-events-none opacity-0 group-hover:pointer-events-auto group-hover:opacity-100 hover:text-red-500'"
                  @click.stop="handleDeleteSession(session.sessionId)"
                >close</span>
              </div>
              <span class="text-xs text-slate-400 font-medium">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
            </button>
          </template>
        </template>
      </div>

      <!-- AI Service Status -->
      <div class="p-4 border-t border-slate-100">
        <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/60">
          <div
            class="flex items-center justify-between gap-3 mb-3"
          >
            <p class="text-xs font-bold uppercase tracking-widest text-slate-400">AI 服务</p>
            <span
              class="inline-flex rounded-full px-2 py-1 text-[11px] font-bold"
              :class="aiStatusBadgeClass"
            >
              {{ aiStatusBadgeText }}
            </span>
          </div>

          <div class="flex gap-2">
            <button
              class="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 transition-colors hover:bg-slate-50"
              @click="goToAiSettings"
            >
              AI 设置
            </button>
            <button
              class="flex-1 rounded-xl bg-primary px-3 py-2 text-xs font-bold text-white transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="!canManageAiConfig"
              @click="openApiKeySetup"
            >
              配置 AI 服务
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Area -->
    <div
      v-if="!isMobile || mobilePanel === 'chat'"
      class="flex-1 flex flex-col relative bg-white overflow-hidden"
    >
      <!-- Chat View -->
      <template v-if="currentView === 'chat'">
        <div class="flex-1 flex flex-col overflow-hidden">
          <!-- Messages Area -->
          <div
            ref="messagesContainer"
            class="flex-1 overflow-y-auto p-4 md:p-8 space-y-8 scroll-smooth pb-4"
            :class="{ 'wk-chat-messages--mobile-header': showMobileChatHeader }"
          >
            <!-- Welcome Section (no messages) -->
            <template v-if="chatStore.messages.length === 0">
              <div class="max-w-3xl mx-auto flex flex-col items-center text-center space-y-4 py-12">
                <div class="size-16 bg-primary/5 rounded-2xl flex items-center justify-center text-primary mb-2 border border-primary/10">
                  <WkIcon name="ai" class="text-4xl" />
                </div>
                <h1 class="text-2xl font-bold tracking-tight text-slate-900">
                  您好，{{ userStore.realname || '用户' }}。
                </h1>
                <p class="text-slate-400 text-base max-w-md">
                  我是您的智能销售助手。今天想处理哪些客户或商机？
                </p>
              </div>
            </template>

            <!-- Messages -->
            <template v-else>
              <div
                v-for="message in chatStore.messages"
                :key="message.id"
                class="max-w-3xl mx-auto message-enter"
              >
                <!-- AI Message -->
                <div v-if="message.role !== 'user'" class="flex gap-4 md:gap-5">
                  <div class="size-9 rounded-xl bg-primary flex items-center justify-center text-white shrink-0 shadow-lg shadow-primary/20">
                    <WkIcon name="ai" class="text-lg" />
                  </div>
                  <div class="flex-1 space-y-3 min-w-0">
                    <div class="bg-slate-50 text-slate-700 rounded-2xl rounded-tl-none p-4 inline-block max-w-full text-sm leading-relaxed border border-slate-100">
                      <div
                        class="wk-markdown"
                        :class="{ 'streaming-cursor': message.isStreaming }"
                        v-html="renderAssistantMessage(message.content || '...')"
                      />
                    </div>
                    <!-- Attachments -->
                    <div v-if="message.attachments && message.attachments.length > 0" class="space-y-2">
                      <div v-for="att in message.attachments" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="rounded-xl max-h-[200px] border border-slate-100"
                            :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                            lazy
                          />
                          <div class="text-xs text-slate-400 mt-1">{{ att.fileName }}</div>
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex items-center gap-3 p-3 rounded-xl border border-slate-100 hover:bg-slate-50 transition-colors max-w-xs"
                          >
                            <span class="material-symbols-outlined text-slate-400">description</span>
                            <div class="flex-1 min-w-0">
                              <div class="text-sm text-slate-700 truncate">{{ att.fileName }}</div>
                              <div class="text-xs text-slate-400">{{ formatFileSize(att.fileSize) }}</div>
                            </div>
                          </a>
                        </template>
                      </div>
                    </div>
                    <div class="text-xs text-slate-400 font-medium">{{ formatTime(message.timestamp) }}</div>
                  </div>
                </div>

                <!-- User Message -->
                <div v-else class="flex gap-4 md:gap-5 flex-row-reverse">
                  <div class="size-9 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-200 flex items-center justify-center">
                    <img
                      v-if="showUserAvatarImage"
                      :src="userStore.avatar"
                      class="h-full w-full object-cover"
                      alt="user avatar"
                      @error="userAvatarLoadFailed = true"
                    />
                    <span v-else class="text-sm font-bold text-slate-600">
                      {{ userAvatarFallback }}
                    </span>
                  </div>
                  <div class="space-y-3 min-w-0" :class="isMobile ? 'max-w-[85%]' : 'max-w-[70%]'">
                    <div class="bg-primary text-white rounded-2xl rounded-tr-none p-4 shadow-lg shadow-primary/10 text-sm leading-relaxed">
                      <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                    </div>
                    <!-- User Attachments -->
                    <div v-if="message.attachments && message.attachments.length > 0" class="space-y-2">
                      <div v-for="att in message.attachments" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="rounded-xl max-h-[200px]"
                            :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                            lazy
                          />
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex items-center gap-3 p-3 rounded-xl border border-white/20 hover:bg-white/10 transition-colors max-w-xs"
                          >
                            <span class="material-symbols-outlined text-white/70">description</span>
                            <div class="flex-1 min-w-0">
                              <div class="text-sm text-white truncate">{{ att.fileName }}</div>
                              <div class="text-xs text-white/60">{{ formatFileSize(att.fileSize) }}</div>
                            </div>
                          </a>
                        </template>
                      </div>
                    </div>
                    <div class="text-xs text-slate-400 font-medium text-right">{{ formatTime(message.timestamp) }}</div>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- Input Area -->
          <div class="shrink-0 p-4 md:p-8 bg-gradient-to-t from-white via-white to-transparent">
            <div class="max-w-4xl mx-auto space-y-4">
              <div v-if="chatStore.applications.length > 0" class="flex flex-wrap gap-2 justify-center">
                <button
                  v-for="app in chatStore.applications"
                  :key="app.code"
                  type="button"
                  class="inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-bold transition-all"
                  :class="chatStore.currentAppCode === app.code
                    ? 'border-primary/25 bg-primary/10 text-primary shadow-sm shadow-primary/10'
                    : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:text-slate-700'"
                  :title="app.description || app.label"
                  @click="chatStore.setCurrentAppCode(app.code)"
                >
                  <span class="material-symbols-outlined text-[16px] leading-none">
                    {{ resolveChatAppIcon(app.code) }}
                  </span>
                  <span>{{ app.label }}</span>
                </button>
              </div>

              <!-- Quick Action Chips -->
              <div v-if="chatStore.messages.length === 0" class="flex flex-wrap gap-2 justify-center">
                <button
                  v-for="action in activeQuickActions"
                  :key="action.label"
                  class="px-4 py-1.5 bg-white border border-slate-200 rounded-full text-sm text-slate-500 hover:border-primary hover:text-primary transition-all shadow-sm"
                  @click="sendQuickMessage(action.text)"
                >
                  {{ action.label }}
                </button>
              </div>

              <!-- Selected Attachments Preview -->
              <div v-if="selectedFiles.length > 0 || selectedKnowledgeItems.length > 0" class="flex flex-wrap gap-2">
                <div
                  v-for="item in selectedKnowledgeItems"
                  :key="item.knowledgeId"
                  class="flex items-center gap-2 px-3 py-2 bg-emerald-50 rounded-xl text-sm text-emerald-700 border border-emerald-100"
                >
                  <span class="material-symbols-outlined text-sm text-emerald-500">menu_book</span>
                  <span class="truncate max-w-[140px]">{{ item.name }}</span>
                  <span
                    class="material-symbols-outlined text-sm text-emerald-400 hover:text-red-500 cursor-pointer"
                    @click="removeSelectedKnowledge(item.knowledgeId)"
                  >close</span>
                </div>
                <div
                  v-for="(file, index) in selectedFiles"
                  :key="index"
                  class="flex items-center gap-2 px-3 py-2 bg-slate-50 rounded-xl text-sm text-slate-700 border border-slate-100"
                >
                  <span class="material-symbols-outlined text-sm" :class="file.type.startsWith('image/') ? 'text-blue-500' : 'text-slate-400'">
                    {{ file.type.startsWith('image/') ? 'image' : 'description' }}
                  </span>
                  <span class="truncate max-w-[120px]">{{ file.name }}</span>
                  <span class="text-xs text-slate-400">{{ formatFileSize(file.size) }}</span>
                  <span
                    class="material-symbols-outlined text-sm text-slate-400 hover:text-red-500 cursor-pointer"
                    @click="removeSelectedFile(index)"
                  >close</span>
                </div>
              </div>

              <!-- Input Box -->
              <div class="relative group">
                <div class="absolute inset-0 bg-primary/5 blur-xl rounded-2xl group-focus-within:bg-primary/10 transition-all opacity-0 group-focus-within:opacity-100"></div>
                <div class="relative flex items-center bg-white border border-slate-200 rounded-2xl p-2 shadow-xl shadow-slate-200/40 focus-within:border-primary transition-all">
                  <input
                    ref="fileInputRef"
                    type="file"
                    multiple
                    :accept="CHAT_ATTACHMENT_ACCEPT"
                    class="hidden"
                    @change="handleFileSelect"
                  />
                  <button
                    class="size-10 flex items-center justify-center text-slate-400 hover:text-primary transition-colors"
                    :disabled="isUploading"
                    @click="handleUpload"
                  >
                    <span class="material-symbols-outlined">attach_file</span>
                  </button>
                  <button
                    class="size-10 flex items-center justify-center text-slate-400 hover:text-primary transition-colors disabled:opacity-50"
                    :disabled="isUploading || selectedFiles.length + selectedKnowledgeItems.length >= MAX_FILE_COUNT"
                    title="选择知识库文件"
                    @click="openKnowledgePicker"
                  >
                    <span class="material-symbols-outlined">menu_book</span>
                  </button>
                  <input
                    ref="chatInputRef"
                    v-model="inputText"
                    type="text"
                    class="flex-1 bg-transparent border-none focus:ring-0 focus:outline-none text-slate-900 px-3 py-3 text-sm placeholder:text-slate-400"
                    placeholder="输入指令，如：总结今天与张总的会议..."
                    :disabled="chatStore.currentSessionIsStreaming || isUploading"
                    @keydown.enter.exact.prevent="handleSend"
                    @paste="handlePaste"
                  />
                  <div class="flex items-center gap-2 pr-1">
                    <button
                      type="button"
                      class="h-10 rounded-full border px-3.5 text-sm shadow-sm transition-all"
                      :class="chatStore.ragEnabled
                        ? 'border-primary/25 bg-primary/10 text-primary shadow-primary/10'
                        : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:text-slate-700'"
                      :aria-pressed="chatStore.ragEnabled"
                      :title="chatStore.ragEnabled ? '已启用 知识库 检索' : '点击启用 知识库 检索'"
                      @click="chatStore.setRagEnabled(!chatStore.ragEnabled)"
                    >
                      <span class="flex items-center gap-1.5">
                        <span class="material-symbols-outlined text-[18px] leading-none">
                          menu_book
                        </span>
                        <span>知识库检索</span>
                      </span>
                    </button>
                    <button
                      class="size-10 rounded-xl bg-primary text-white flex items-center justify-center hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all disabled:opacity-50"
                      :disabled="(!inputText.trim() && selectedFiles.length === 0 && selectedKnowledgeItems.length === 0) || chatStore.currentSessionIsStreaming || isUploading"
                      @click="handleSend"
                    >
                      <span v-if="chatStore.currentSessionIsStreaming || isUploading" class="material-symbols-outlined text-xl animate-spin">progress_activity</span>
                      <span v-else class="material-symbols-outlined text-xl">send</span>
                    </button>
                  </div>
                </div>
              </div>
              <p class="text-center text-xs text-slate-300 uppercase tracking-[0.4em]">内容由AI生成，请核查重要信息</p>
            </div>
          </div>
        </div>
      </template>

      <!-- Notifications View -->
      <template v-else>
        <div class="flex-1 overflow-y-auto p-6 md:p-12">
          <!-- Mobile back button -->
          <button v-if="isMobile" class="flex items-center gap-1 text-sm text-slate-500 mb-4" @click="mobilePanel = 'sessions'">
            <span class="material-symbols-outlined text-sm">arrow_back</span>
            返回
          </button>

          <div class="max-w-4xl mx-auto">
            <div class="flex items-center justify-between mb-10">
              <div>
                <h2 class="text-2xl font-bold text-slate-900">系统通知</h2>
                <p class="text-slate-500 text-sm mt-1">查看来自 悟空AI CRM 的重要更新和安全提醒</p>
              </div>
              <button class="px-4 py-2 text-sm font-bold text-primary hover:bg-primary/5 rounded-lg transition-all">
                全部标记为已读
              </button>
            </div>

            <div class="space-y-8">
              <div v-for="notif in systemNotifications" :key="notif.id" class="space-y-3">
                <!-- AI Header -->
                <div class="flex items-center gap-3 px-2">
                  <div class="size-8 rounded-lg bg-primary flex items-center justify-center text-white shadow-sm">
                    <WkIcon name="ai" class="text-sm" />
                  </div>
                  <div>
                    <p class="text-sm font-bold text-slate-900">悟空AI CRM 助手</p>
                    <p class="text-xs text-slate-400 font-medium">系统自动发送</p>
                  </div>
                </div>

                <!-- Notification Card -->
                <div
                  class="p-6 bg-white border rounded-2xl transition-all flex flex-col gap-6 group relative"
                  :class="notif.unread ? 'border-primary/20 shadow-sm' : 'border-slate-100 shadow-none'"
                >
                  <div class="flex gap-5">
                    <div v-if="notif.unread" class="absolute top-6 right-6 flex items-center gap-1.5">
                      <span class="size-2 rounded-full bg-primary"></span>
                      <span class="text-xs font-bold text-primary uppercase tracking-tight">未读</span>
                    </div>

                    <div
                      class="size-12 rounded-xl flex items-center justify-center shrink-0"
                      :class="{
                        'bg-blue-50 text-blue-500': notif.type === 'info',
                        'bg-amber-50 text-amber-500': notif.type === 'warning',
                        'bg-emerald-50 text-emerald-500': notif.type === 'success'
                      }"
                    >
                      <span class="material-symbols-outlined">
                        {{ notif.type === 'info' ? 'upgrade' : notif.type === 'warning' ? 'security' : 'analytics' }}
                      </span>
                    </div>

                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-1">
                        <span
                          class="text-xs font-bold px-2 py-0.5 rounded uppercase tracking-tight"
                          :class="{
                            'bg-blue-50 text-blue-600': notif.type === 'info',
                            'bg-amber-50 text-amber-600': notif.type === 'warning',
                            'bg-emerald-50 text-emerald-600': notif.type === 'success'
                          }"
                        >{{ notif.category }}</span>
                        <span class="text-xs text-slate-400 font-medium">{{ notif.time }}</span>
                      </div>
                      <h3 class="font-bold text-slate-900 text-lg mb-2">{{ notif.title }}</h3>
                      <p class="text-slate-600 text-sm leading-relaxed max-w-2xl">{{ notif.content }}</p>
                      <div class="mt-6 flex items-center gap-4">
                        <button class="px-4 py-2 bg-slate-900 text-white text-xs font-bold rounded-lg hover:bg-slate-800 transition-all">
                          立即查看
                        </button>
                        <button class="px-4 py-2 bg-white border border-slate-200 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-50 transition-all">
                          忽略
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Empty State -->
            <div class="mt-12 p-12 border-2 border-dashed border-slate-100 rounded-[2.5rem] flex flex-col items-center text-center">
              <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mb-4">
                <span class="material-symbols-outlined text-4xl">history</span>
              </div>
              <p class="text-slate-400 text-sm font-medium">没有更多历史通知了</p>
            </div>
          </div>
        </div>
      </template>
    </div>

    <aside
      v-if="showDesktopObjectPanel"
      class="hidden w-[360px] shrink-0 border-l border-slate-100 bg-white lg:flex lg:flex-col"
    >
      <div v-if="objectPanelLoading" class="flex flex-1 items-center justify-center text-slate-400">
        <span class="material-symbols-outlined animate-spin text-2xl">progress_activity</span>
      </div>
      <div v-else-if="objectPanelError" class="flex flex-1 items-center justify-center px-6 text-center text-sm text-slate-400">
        {{ objectPanelError }}
      </div>
      <CustomerChatInfoPanel
        v-else-if="chatObjectKind === 'customer' && currentObjectId"
        :customer-id="currentObjectId"
      />
      <EmployeeChatInfoPanel
        v-else-if="chatObjectKind === 'employee' && employeeDetail"
        :employee="employeeDetail"
        @add-task="handleObjectAddTask"
        @add-schedule="handleObjectAddSchedule"
        @add-attachment="handleObjectAddAttachment"
        @view-task="handleObjectViewTask"
        @view-schedule="handleObjectViewSchedule"
        @view-attachment="handleObjectViewAttachment"
      />
      <RelationChatInfoPanel
        v-else-if="chatObjectKind === 'relation' && relationDetail"
        :detail="relationDetail"
        @add-task="handleObjectAddTask"
        @add-schedule="handleObjectAddSchedule"
        @add-attachment="handleObjectAddAttachment"
        @view-task="handleObjectViewTask"
        @view-schedule="handleObjectViewSchedule"
        @view-attachment="handleObjectViewAttachment"
      />
      <ProductChatInfoPanel
        v-else-if="chatObjectKind === 'product' && productDetail"
        :product="productDetail"
        @edit="handleObjectEditProduct"
      />
      <div v-else class="flex flex-1 items-center justify-center px-6 text-center text-sm text-slate-400">
        暂无关联对象详情
      </div>
    </aside>

    <ApiKeySetupModal
      :model-value="isApiKeyModalOpen"
      :loading="savingApiKey"
      :provider-options="apiKeySetupProviderOptions"
      :initial-config="apiKeySetupInitialConfig"
      @update:model-value="handleApiKeyModalVisibleChange"
      @save="handleSaveApiKey"
    />
    <ChatKnowledgePickerModal
      v-model="chatKnowledgePickerVisible"
      :remaining-slots="Math.max(0, MAX_FILE_COUNT - selectedFiles.length - selectedKnowledgeItems.length)"
      @confirm="handleKnowledgePickerConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { getAiConfig, getAiConfigDetail, updateAiConfig } from '@/api/systemConfig'
import { getAddressBookDetail } from '@/api/addressBook'
import { getRelationDetail } from '@/api/relation'
import { getProductDetail } from '@/api/product'
import ApiKeySetupModal from '@/components/common/ApiKeySetupModal.vue'
import ChatKnowledgePickerModal from '@/components/chat/ChatKnowledgePickerModal.vue'
import CustomerChatInfoPanel from './components/CustomerChatInfoPanel.vue'
import EmployeeChatInfoPanel from './components/EmployeeChatInfoPanel.vue'
import MobileChatTopHeader from './components/MobileChatTopHeader.vue'
import ProductChatInfoPanel from './components/ProductChatInfoPanel.vue'
import RelationChatInfoPanel from './components/RelationChatInfoPanel.vue'
import { renderMarkdown } from '@/utils/markdown'
import { appEvents, APP_EVENT } from '@/utils/events'
import { isRequestErrorHandled } from '@/utils/requestError'
import { shouldRefocusChatComposerAfterSend } from '@/utils/chatComposerFocus'
import {
  CHAT_ATTACHMENT_ACCEPT,
  MAX_CHAT_ATTACHMENT_COUNT,
  extractClipboardFiles,
  mergeChatFiles
} from '@/utils/chatAttachment'
import type { ScheduleVO } from '@/api/schedule'
import type { AddressBookDetail } from '@/types/addressBook'
import type { ProductVO } from '@/types/product'
import type { RelationDetailVO } from '@/types/relation'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, Knowledge, Task } from '@/types/common'
import type { AiConfig, AiConfigUpdateBO, AiProvider, AiProviderPreset } from '@/types/systemConfig'

const chatStore = useChatStore()
const agentStore = useAgentStore()
const userStore = useUserStore()
const router = useRouter()
const { isMobile } = useResponsive()

const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const mobilePanel = ref<'sessions' | 'chat'>('chat')
const fileInputRef = ref<HTMLInputElement | null>(null)
const chatInputRef = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])
const selectedKnowledgeItems = ref<Knowledge[]>([])
const chatKnowledgePickerVisible = ref(false)
const isUploading = ref(false)
const currentView = ref<'chat' | 'notifications'>('chat')
const userAvatarLoadFailed = ref(false)
const aiConfig = ref<AiConfig | null>(null)
const aiConfigLoaded = ref(false)
const isApiKeyModalOpen = ref(false)
const apiKeySetupInitialConfig = ref<Partial<AiConfigUpdateBO> | null>(null)
const apiKeySetupProviderOptions = ref<AiProviderPreset[]>([])
const savingApiKey = ref(false)
const resumeSendAfterApiKeySave = ref(false)
const employeeDetail = ref<AddressBookDetail | null>(null)
const relationDetail = ref<RelationDetailVO | null>(null)
const productDetail = ref<ProductVO | null>(null)
const objectPanelLoading = ref(false)
const objectPanelError = ref('')
let objectDetailRequestId = 0

const MAX_FILE_COUNT = MAX_CHAT_ATTACHMENT_COUNT
const DEFAULT_CHAT_AI_CONFIG: AiConfigUpdateBO = {
  provider: 'dashscope',
  apiUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
  apiKey: '',
  model: 'qwen3.5-plus',
  temperature: 0.7,
  maxTokens: 4096
}

// Notifications mock data
// const notifications = ref([
//   { id: 1, content: '客户张三的项目进度已更新', time: '5分钟前', color: 'bg-blue-500' },
//   { id: 2, content: '有3个任务即将到期', time: '1小时前', color: 'bg-orange-500' },
//   { id: 3, content: '知识库同步完成', time: '2小时前', color: 'bg-green-500' }
// ])

// System notifications for notification view
const systemNotifications = ref([
  {
    id: 1,
    title: '系统核心引擎升级完成',
    content: '悟空AI CRM 已升级至最新版本。本次更新优化了长文本理解能力，并新增了对多语种会议摘要的支持。',
    time: '1小时前',
    type: 'info',
    category: '系统更新',
    unread: true
  },
  {
    id: 2,
    title: '异地登录安全提醒',
    content: '检测到您的账号存在异地登录行为。如果这不是您的操作，请立即重置密码并开启两步验证。',
    time: '3小时前',
    type: 'warning',
    category: '安全警报',
    unread: true
  },
  {
    id: 3,
    title: '本月销售业绩分析报告',
    content: '您上个月的销售目标达成率为 112%。AI 已为您生成了详细的客户贡献度分析和下月潜力客户预测，建议优先关注。',
    time: '昨天',
    type: 'success',
    category: '业务报告',
    unread: false
  }
])

// Group sessions by date
const groupedSessions = computed(() => {
  const today: ChatSession[] = []
  const yesterday: ChatSession[] = []
  const earlier: ChatSession[] = []
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const yesterdayStart = todayStart - 86400000

  for (const session of chatStore.sessions) {
    const time = new Date(session.updateTime || session.createTime).getTime()
    if (time >= todayStart) {
      today.push(session)
    } else if (time >= yesterdayStart) {
      yesterday.push(session)
    } else {
      earlier.push(session)
    }
  }

  return { today, yesterday, earlier }
})

const quickActions = [
  { label: '创建新客户', text: '帮我创建一个新客户' },
  { label: '查询客户状态', text: '帮我查询客户列表' },
  { label: '生成跟进任务', text: '帮我生成跟进任务' },
  { label: '分析本月销售目标', text: '分析本月销售目标的缺口' }
]

const activeQuickActions = computed(() => {
  const recommended = chatStore.currentApplication?.recommendedQuestions || []
  if (recommended.length > 0) {
    return recommended.map(text => ({ label: text, text }))
  }
  return quickActions
})

const aiReady = computed(() => Boolean(aiConfig.value?.ready))
const canManageAiConfig = computed(() => userStore.hasPermission('config:ai'))
const aiStatusBadgeText = computed(() => {
  return aiReady.value ? '模型已就绪' : '待配置'
})
const aiStatusBadgeClass = computed(() => {
  return aiReady.value
    ? 'bg-blue-50 text-blue-600'
    : 'bg-slate-100 text-slate-500'
})
const showUserAvatarImage = computed(() => Boolean(userStore.avatar) && !userAvatarLoadFailed.value)
const userAvatarFallback = computed(() => (userStore.realname || userStore.username || 'U').charAt(0).toUpperCase())
const currentChatSession = computed(() => chatStore.currentSession)
const chatObjectKind = computed<'customer' | 'employee' | 'relation' | 'product' | ''>(() => {
  const session = currentChatSession.value
  if (!session) return ''
  if (session.customerId) return 'customer'
  if (session.employeeId) return 'employee'
  if (session.relationId) return 'relation'
  if (session.productId) return 'product'
  return ''
})
const currentObjectId = computed(() => {
  const session = currentChatSession.value
  if (!session) return ''
  if (chatObjectKind.value === 'customer') return String(session.customerId || '')
  if (chatObjectKind.value === 'employee') return String(session.employeeId || '')
  if (chatObjectKind.value === 'relation') return String(session.relationId || '')
  if (chatObjectKind.value === 'product') return String(session.productId || '')
  return ''
})
const showDesktopObjectPanel = computed(() =>
  !isMobile.value && currentView.value === 'chat' && Boolean(chatObjectKind.value && currentObjectId.value)
)
const showMobileChatHeader = computed(() =>
  isMobile.value && mobilePanel.value === 'chat' && currentView.value === 'chat'
)
const mobileChatHeaderKind = computed<'chat' | 'customer' | 'employee' | 'relation' | 'product'>(() =>
  chatObjectKind.value || 'chat'
)
const mobileChatHeaderTitle = computed(() => {
  const session = currentChatSession.value
  if (chatObjectKind.value === 'customer') {
    return session?.customerName || session?.title || '客户对话'
  }
  if (chatObjectKind.value === 'employee') {
    return employeeDetail.value?.realname || session?.title || '通讯录对话'
  }
  if (chatObjectKind.value === 'relation') {
    return relationDetail.value?.relation?.name || session?.relationName || session?.title || '关系对话'
  }
  if (chatObjectKind.value === 'product') {
    return productDetail.value?.productName || session?.productName || session?.title || '产品对话'
  }
  return session?.title || 'AI 对话'
})
const mobileChatHeaderAvatarUrl = computed(() => {
  if (chatObjectKind.value === 'employee') return employeeDetail.value?.imgUrl || employeeDetail.value?.img || ''
  if (chatObjectKind.value === 'relation') return relationDetail.value?.relation?.avatarUrl || relationDetail.value?.relation?.avatar || ''
  if (chatObjectKind.value === 'product') return productDetail.value?.mainImageUrl || ''
  return ''
})

onMounted(async () => {
  await Promise.all([
    chatStore.fetchApplications(),
    chatStore.fetchSessions(),
    agentStore.fetchEnabledAgents(),
    loadAiConfig()
  ])
})

// Auto scroll to bottom when new messages arrive or during streaming
let scrollTimer: ReturnType<typeof setTimeout> | null = null
function scrollToBottom() {
  if (scrollTimer) return
  scrollTimer = setTimeout(() => {
    scrollTimer = null
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }, 100)
}

watch(
  () => {
    const msgs = chatStore.messages
    const last = msgs[msgs.length - 1]
    return { length: msgs.length, content: last?.content?.length ?? 0 }
  },
  () => {
    nextTick(scrollToBottom)
  }
)

watch(
  () => userStore.avatar,
  () => {
    userAvatarLoadFailed.value = false
  }
)

watch(
  () => [chatObjectKind.value, currentObjectId.value] as const,
  () => {
    void loadObjectPanelDetail()
  },
  { immediate: true }
)

async function loadObjectPanelDetail() {
  const kind = chatObjectKind.value
  const id = currentObjectId.value
  const requestId = ++objectDetailRequestId

  employeeDetail.value = null
  relationDetail.value = null
  productDetail.value = null
  objectPanelError.value = ''

  if (!kind || !id || kind === 'customer') {
    objectPanelLoading.value = false
    return
  }

  objectPanelLoading.value = true
  try {
    if (kind === 'employee') {
      const detail = await getAddressBookDetail(id)
      if (requestId === objectDetailRequestId) {
        employeeDetail.value = detail
      }
    } else if (kind === 'relation') {
      const detail = await getRelationDetail(id)
      if (requestId === objectDetailRequestId) {
        relationDetail.value = detail
      }
    } else if (kind === 'product') {
      const detail = await getProductDetail(id)
      if (requestId === objectDetailRequestId) {
        productDetail.value = detail
      }
    }
  } catch (error) {
    console.error('Load chat object detail failed:', error)
    if (requestId === objectDetailRequestId) {
      objectPanelError.value = '关联对象详情加载失败'
    }
  } finally {
    if (requestId === objectDetailRequestId) {
      objectPanelLoading.value = false
    }
  }
}

function normalizeAiConfig(config?: Partial<AiConfig> | Partial<AiConfigUpdateBO> | null): AiConfig {
  return {
    provider: config?.provider || DEFAULT_CHAT_AI_CONFIG.provider || 'dashscope',
    apiUrl: config?.apiUrl || DEFAULT_CHAT_AI_CONFIG.apiUrl,
    apiKey: config?.apiKey || '',
    model: config?.model || DEFAULT_CHAT_AI_CONFIG.model,
    temperature: config?.temperature ?? DEFAULT_CHAT_AI_CONFIG.temperature ?? 0.7,
    maxTokens: config?.maxTokens ?? DEFAULT_CHAT_AI_CONFIG.maxTokens ?? 4096,
    extraHeadersConfigured: (config as Partial<AiConfig> | null)?.extraHeadersConfigured ?? false,
    extraHeadersJson: (config as Partial<AiConfig> | null)?.extraHeadersJson ?? '',
    capabilities: (config as Partial<AiConfig> | null)?.capabilities,
    modelHint: (config as Partial<AiConfig> | null)?.modelHint,
    extraHeadersHint: (config as Partial<AiConfig> | null)?.extraHeadersHint,
    availableProviders: (config as Partial<AiConfig> | null)?.availableProviders,
    mode: (config as Partial<AiConfig> | null)?.mode || 'custom',
    customConfigSaved: (config as Partial<AiConfig> | null)?.customConfigSaved ?? false,
    ready: (config as Partial<AiConfig> | null)?.ready ?? Boolean(config?.apiKey?.trim()),
    updateTime: config && 'updateTime' in config ? config.updateTime : undefined
  }
}

async function loadAiConfig(force = false): Promise<AiConfig | null> {
  if (aiConfigLoaded.value && !force) {
    return aiConfig.value
  }

  try {
    const config = await getAiConfig()
    aiConfig.value = normalizeAiConfig(config)
  } catch {
    if (!aiConfig.value) {
      aiConfig.value = normalizeAiConfig()
    }
  } finally {
    aiConfigLoaded.value = true
  }

  return aiConfig.value
}

async function ensureAiAvailable(): Promise<boolean> {
  if (!aiConfigLoaded.value || !aiConfig.value?.ready) {
    await loadAiConfig(true)
  }

  if (aiConfig.value?.ready) {
    return true
  }

  if (!canManageAiConfig.value) {
    ElMessage.warning('当前 AI 服务未就绪，请联系管理员配置。')
    return false
  }

  resumeSendAfterApiKeySave.value = true
  await prepareApiKeySetupModal()
  isApiKeyModalOpen.value = true
  return false
}

function handleApiKeyModalVisibleChange(visible: boolean) {
  isApiKeyModalOpen.value = visible

  if (!visible && !savingApiKey.value) {
    apiKeySetupInitialConfig.value = null
    resumeSendAfterApiKeySave.value = false
  }
}

async function prepareApiKeySetupModal() {
  if (!canManageAiConfig.value) return

  try {
    const detailConfig = await getAiConfigDetail()
    apiKeySetupProviderOptions.value = detailConfig.availableProviders?.length
      ? detailConfig.availableProviders
      : []
    apiKeySetupInitialConfig.value = {
      provider: (detailConfig.provider || DEFAULT_CHAT_AI_CONFIG.provider) as AiProvider,
      apiUrl: detailConfig.apiUrl || DEFAULT_CHAT_AI_CONFIG.apiUrl,
      apiKey: '',
      model: detailConfig.model || DEFAULT_CHAT_AI_CONFIG.model,
      temperature: detailConfig.temperature ?? DEFAULT_CHAT_AI_CONFIG.temperature,
      maxTokens: detailConfig.maxTokens ?? DEFAULT_CHAT_AI_CONFIG.maxTokens,
      extraHeadersJson: detailConfig.extraHeadersJson ?? ''
    }
  } catch {
    apiKeySetupProviderOptions.value = []
    apiKeySetupInitialConfig.value = { ...DEFAULT_CHAT_AI_CONFIG }
  }
}

function resolveProviderLabel(provider?: AiProvider): string {
  return apiKeySetupProviderOptions.value.find((item) => item.value === provider)?.label || 'AI 服务商'
}

async function handleSaveApiKey(payload: AiConfigUpdateBO) {
  const resolvedProvider = (payload.provider || DEFAULT_CHAT_AI_CONFIG.provider) as AiProvider
  const trimmedApiKey = payload.apiKey.trim()
  const trimmedApiUrl = payload.apiUrl.trim()
  const trimmedModel = payload.model.trim()
  const canReuseSavedApiKey = Boolean(
    trimmedApiKey
    || apiKeySetupProviderOptions.value.find((item) => item.value === resolvedProvider)?.apiKeyConfigured
  )

  if (!canReuseSavedApiKey) {
    ElMessage.warning('请输入 API Key，或先保存当前服务商的 API Key')
    return
  }
  if (!trimmedApiUrl) {
    ElMessage.warning('请输入 API 地址')
    return
  }
  if (!trimmedModel) {
    ElMessage.warning('请输入模型名称')
    return
  }

  savingApiKey.value = true

  try {
    const nextPayload: AiConfigUpdateBO = {
      ...DEFAULT_CHAT_AI_CONFIG,
      ...payload,
      provider: resolvedProvider,
      apiUrl: trimmedApiUrl,
      apiKey: trimmedApiKey,
      model: trimmedModel,
      extraHeadersJson: payload.extraHeadersJson?.trim() || ''
    }

    await updateAiConfig(nextPayload)
    await loadAiConfig(true)
    isApiKeyModalOpen.value = false
    apiKeySetupInitialConfig.value = null
    ElMessage.success(`${resolveProviderLabel(nextPayload.provider)} 配置保存成功`)

    const shouldResumeSend = resumeSendAfterApiKeySave.value
    resumeSendAfterApiKeySave.value = false

    if (shouldResumeSend) {
      await nextTick()
      await handleSend()
    }
  } catch {
    // Error handled by interceptor
  } finally {
    savingApiKey.value = false
  }
}

function goToAiSettings() {
  router.push('/settings/system/api')
}

function openApiKeySetup() {
  if (!canManageAiConfig.value) {
    ElMessage.warning('当前账号没有 AI 配置权限，请联系管理员。')
    return
  }
  resumeSendAfterApiKeySave.value = false
  prepareApiKeySetupModal().then(() => {
    isApiKeyModalOpen.value = true
  })
}

async function handleSend() {
  const text = inputText.value.trim()
  const hasFiles = selectedFiles.value.length > 0
  const hasKnowledge = selectedKnowledgeItems.value.length > 0
  if ((!text && !hasFiles && !hasKnowledge) || chatStore.currentSessionIsStreaming || isUploading.value) return
  if (!(await ensureAiAvailable())) return

  const effectiveAppCode = chatStore.currentAppCode
  const content = text || (hasKnowledge ? '请结合选中的知识库文件回答' : '请分析这些文件')
  const selectedKnowledgeSnapshot = [...selectedKnowledgeItems.value]
  inputText.value = ''

  let attachmentDTOs: ChatAttachmentDTO[] | undefined
  let attachmentVOs: ChatAttachmentVO[] | undefined

  // Upload files to MinIO if any
  if (hasFiles) {
    isUploading.value = true
    try {
      const files = [...selectedFiles.value]
      selectedFiles.value = []

      const results = await Promise.all(
        files.map(async (file) => {
          const presigned = await getPresignedUploadUrl(file.name, file.type)
          await uploadToMinIO(file, presigned.uploadUrl)
          return {
            dto: {
              fileName: file.name,
              filePath: presigned.objectKey,
              fileSize: file.size,
              mimeType: file.type || 'application/octet-stream'
            } as ChatAttachmentDTO,
            vo: {
              id: '',
              fileName: file.name,
              filePath: presigned.objectKey,
              fileSize: file.size,
              mimeType: file.type || 'application/octet-stream',
              accessUrl: presigned.accessUrl
            } as ChatAttachmentVO
          }
        })
      )

      attachmentDTOs = results.map(r => r.dto)
      attachmentVOs = results.map(r => r.vo)
    } catch (e) {
      console.error('文件上传失败:', e)
      if (!isRequestErrorHandled(e)) {
        ElMessage.error('文件上传失败，请重试')
      }
      isUploading.value = false
      return
    }
    isUploading.value = false
  }

  const knowledgeIds = hasKnowledge ? selectedKnowledgeSnapshot.map(item => item.knowledgeId) : undefined
  if (hasKnowledge) {
    selectedKnowledgeItems.value = []
    if (chatStore.currentAppCode === 'general') {
      chatStore.setCurrentAppCode('knowledge')
    }
  }

  // Switch to chat view when sending
  currentView.value = 'chat'
  await chatStore.sendMessage(content, attachmentDTOs, attachmentVOs, chatStore.ragEnabled || hasKnowledge, knowledgeIds)
  if (effectiveAppCode === 'crm') {
    appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH, { source: 'chat' })
  }
  await refocusChatInputAfterSend()
}

async function refocusChatInputAfterSend() {
  if (!shouldRefocusChatComposerAfterSend(isMobile.value)) return
  await nextTick()
  chatInputRef.value?.focus()
}

function sendQuickMessage(text: string) {
  inputText.value = text
  handleSend()
}

function renderAssistantMessage(content: string): string {
  return renderMarkdown(content)
}

function handleUpload() {
  if (selectedFiles.value.length + selectedKnowledgeItems.value.length >= MAX_FILE_COUNT) {
    ElMessage.warning(`最多只能上传${MAX_FILE_COUNT}个文件`)
    return
  }
  fileInputRef.value?.click()
}

function openKnowledgePicker() {
  if (selectedFiles.value.length + selectedKnowledgeItems.value.length >= MAX_FILE_COUNT) {
    ElMessage.warning(`最多只能上传${MAX_FILE_COUNT}个文件`)
    return
  }
  chatKnowledgePickerVisible.value = true
}

function handleKnowledgePickerConfirm(items: Knowledge[]) {
  const room = MAX_FILE_COUNT - selectedFiles.value.length - selectedKnowledgeItems.value.length
  if (room <= 0) {
    ElMessage.warning(`最多只能上传${MAX_FILE_COUNT}个文件`)
    return
  }
  const existingIds = new Set(selectedKnowledgeItems.value.map(item => item.knowledgeId))
  const toAppend: Knowledge[] = []
  for (const item of items) {
    if (toAppend.length >= room) break
    if (existingIds.has(item.knowledgeId)) continue
    existingIds.add(item.knowledgeId)
    toAppend.push(item)
  }
  if (toAppend.length === 0) {
    ElMessage.warning('所选文件已在列表中')
    return
  }
  selectedKnowledgeItems.value = [...selectedKnowledgeItems.value, ...toAppend]
  if (chatStore.currentAppCode === 'general') {
    chatStore.setCurrentAppCode('knowledge')
  }
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files) return

  const result = mergeChatFiles(
    selectedFiles.value,
    Array.from(input.files),
    selectedKnowledgeItems.value.length
  )
  if (result.error) {
    ElMessage.warning(result.error)
    input.value = ''
    return
  }

  selectedFiles.value = result.files
  input.value = '' // Reset input for re-selecting same file
}

function handlePaste(event: ClipboardEvent) {
  const files = extractClipboardFiles(event)
  if (files.length === 0) return

  const result = mergeChatFiles(selectedFiles.value, files, selectedKnowledgeItems.value.length)
  if (result.error) {
    ElMessage.warning(result.error)
    return
  }

  event.preventDefault()
  selectedFiles.value = result.files
}

function removeSelectedFile(index: number) {
  selectedFiles.value.splice(index, 1)
}

function removeSelectedKnowledge(knowledgeId: string) {
  selectedKnowledgeItems.value = selectedKnowledgeItems.value.filter(item => item.knowledgeId !== knowledgeId)
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function handleNewSession() {
  chatStore.clearMessages()
  await chatStore.startNewSession('新对话', undefined, undefined, chatStore.currentAppCode)
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
}

async function handleSelectSession(sessionId: string) {
  if (chatStore.currentSessionId === sessionId && currentView.value === 'chat') return
  currentView.value = 'chat'
  await chatStore.selectSession(sessionId)
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
}

function isSessionActive(sessionId: string): boolean {
  return currentView.value === 'chat' && chatStore.currentSessionId === sessionId
}

async function handleDeleteSession(sessionId: string) {
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？删除后无法恢复。', '删除对话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await chatStore.removeSession(sessionId)
    ElMessage.success('对话已删除')
  } catch {
    // User cancelled
  }
}

async function handleToggleSessionPin(session: ChatSession) {
  try {
    await chatStore.setSessionPinned(session.sessionId, session.pinned !== true)
  } catch (error: unknown) {
    console.error('Toggle chat session pin failed:', error)
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('操作失败，请稍后重试')
    }
  }
}

function openMobileMainMenu() {
  appEvents.emit(APP_EVENT.MOBILE_MAIN_MENU_OPEN)
}

function handleMobileHeaderTitle() {
  if (chatObjectKind.value) {
    openMobileObjectDetail()
  }
}

function openMobileObjectDetail() {
  const id = currentObjectId.value
  if (!id) return

  if (chatObjectKind.value === 'customer') {
    void router.push(`/customer/${id}`)
    return
  }
  if (chatObjectKind.value === 'relation') {
    void router.push({ path: '/relation', query: { openRelationId: id } })
    return
  }
  if (chatObjectKind.value === 'product') {
    void router.push({ path: '/product', query: { openProductId: id } })
    return
  }

  void router.push('/address-book')
}

function handleObjectAddTask() {
  void router.push('/task')
}

function handleObjectAddSchedule() {
  void router.push('/calendar')
}

function handleObjectAddAttachment() {
  void router.push('/knowledge')
}

function handleObjectViewTask(task: Task) {
  void router.push({ path: '/task', query: { openTaskId: String(task.taskId) } })
}

function handleObjectViewSchedule(schedule: ScheduleVO) {
  void router.push({ path: '/calendar', query: { openScheduleId: String(schedule.scheduleId) } })
}

function handleObjectViewAttachment(attachment: Knowledge) {
  void router.push({ path: '/knowledge', query: { openKnowledgeId: String(attachment.knowledgeId) } })
}

function handleObjectEditProduct(product: ProductVO) {
  void router.push({ path: '/product', query: { editProductId: String(product.productId) } })
}

function formatSessionTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const time = date.getTime()

  if (time >= todayStart) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function formatTime(date: Date): string {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function resolveChatAppIcon(code: string): string {
  const iconMap: Record<string, string> = {
    general: 'auto_awesome',
    crm: 'groups',
    knowledge: 'menu_book',
    address_book: 'badge',
    relation: 'contacts',
    product: 'inventory_2',
    project: 'task_alt'
  }
  return iconMap[code] || 'auto_awesome'
}

</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Message animation */
.message-enter {
  animation: messageSlideIn 0.3s ease-out;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Streaming cursor */
.streaming-cursor::after {
  content: '▊';
  animation: blink 1s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Material Symbols fill variant */
.fill-1 {
  font-variation-settings: 'FILL' 1;
}

.wk-chat-messages--mobile-header {
  padding-top: calc(62px + max(8px, var(--safe-area-inset-top))) !important;
  scroll-padding-top: calc(62px + max(8px, var(--safe-area-inset-top)));
}

</style>
