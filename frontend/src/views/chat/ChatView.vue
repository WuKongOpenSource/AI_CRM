<template>
  <div class="flex h-full" :class="{ 'flex-col': isMobile }">
    <!-- Internal Sidebar: Chat History -->
    <aside v-if="isMobile && mobilePanel === 'sessions'" class="flex flex-1 flex-col bg-slate-50/50">
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
    </aside>

    <!-- Main Area -->
    <div
      v-if="!isMobile || mobilePanel === 'chat'"
      class="flex-1 flex flex-col relative bg-white overflow-hidden"
    >
      <!-- Chat View -->
      <template v-if="currentView === 'chat'">
        <div
          class="flex-1 flex flex-col overflow-hidden relative"
          :class="isChatEmpty ? 'justify-center -translate-y-[100px]' : ''"
        >
          <!-- Mobile top bar (chat detail) -->
          <div
            v-if="isMobile"
            class="shrink-0 sticky top-0 z-10 bg-white/95 backdrop-blur border-b border-slate-100 px-4 py-3"
          >
            <div class="flex items-center justify-between gap-3">
              <button
                type="button"
                class="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-primary transition-colors"
                @click="mobilePanel = 'sessions'"
              >
                <span class="material-symbols-outlined text-[18px] leading-none">arrow_back</span>
                历史对话
              </button>

              <button
                type="button"
                class="h-9 px-3 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-700 shadow-sm hover:bg-slate-50 transition-all inline-flex items-center gap-1.5"
                @click="handleNewSession"
              >
                <span class="material-symbols-outlined wk-plus-button-icon text-[18px] leading-none">add</span>
                开启新对话
              </button>
            </div>
          </div>

          <!-- Messages Area -->
          <div
            ref="messagesContainer"
            class="p-4 md:p-8 space-y-8 scroll-smooth"
            :class="isChatEmpty ? 'overflow-hidden' : 'flex-1 overflow-y-auto pb-4'"
            @scroll="handleMessagesScroll"
          >
            <!-- Welcome Section (no messages) -->
            <template v-if="chatStore.messages.length === 0">
              <div class="max-w-3xl mx-auto flex flex-col items-center text-center space-y-4 py-6">
                <!-- <div class="size-16 bg-primary/5 rounded-2xl flex items-center justify-center text-primary mb-2 border border-primary/10">
                  <WkIcon name="ai" class="text-4xl" />
                </div> -->
                <h1 class="text-2xl font-bold tracking-tight text-slate-900">
                  <!-- 您好，{{ userStore.realname || '用户' }} -->
                   今天有什么可以帮您的？
                </h1>
                <!-- <p class="text-slate-400 text-base max-w-md">
                  我是您的智能销售助手。今天想处理哪些客户或商机？
                </p> -->
              </div>
            </template>

            <!-- Messages -->
            <template v-else>
              <div
                v-for="message in chatStore.messages"
                :key="message.id"
                class="mx-auto message-enter"
                :class="isMobile ? 'w-full' : 'w-[768px] max-w-[768px]'"
              >
                <div
                  v-if="getDocumentAttachments(message).length > 0"
                  class="mb-4 flex"
                  :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
                >
                  <div class="flex flex-wrap gap-3" :class="message.role === 'user' ? 'justify-end' : 'justify-start'">
                    <a
                      v-for="att in getDocumentAttachments(message)"
                      :key="att.id || att.fileName"
                      :href="att.accessUrl"
                      target="_blank"
                      class="group flex max-w-xs items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-lg hover:shadow-slate-200/60"
                    >
                      <div class="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                        <span class="material-symbols-outlined">description</span>
                      </div>
                      <div class="min-w-0 flex-1">
                        <div class="truncate text-sm font-semibold text-slate-800">{{ att.fileName }}</div>
                        <div class="mt-1 text-xs text-slate-400">{{ formatFileSize(att.fileSize) }}</div>
                      </div>
                      <span class="material-symbols-outlined text-slate-300 transition-colors group-hover:text-primary">open_in_new</span>
                    </a>
                  </div>
                </div>

                <!-- AI Message -->
                <div v-if="message.role !== 'user'" class="flex gap-4 md:gap-5">
                  <div v-if="false" class="size-9 rounded-xl bg-primary flex items-center justify-center text-white shrink-0 shadow-lg shadow-primary/20">
                    <WkIcon name="ai" class="text-lg" />
                  </div>
                  <div class="flex-1 space-y-3 min-w-0">
                    <div class="relative text-[#0d0d0d] rounded-2xl rounded-tl-none p-4 inline-block max-w-full text-left leading-relaxed text-[16px]">
                      <div
                        v-if="!message.isStreaming"
                        class="absolute -bottom-7 left-0 z-10 size-10 flex items-center justify-center"
                      >
                        <button
                          type="button"
                          class="size-8 rounded-lg border-slate-200 bg-white text-slate-500 transition-colors hover:bg-[#E7E7E7] hover:text-slate-900 flex items-center justify-center"
                          aria-label="复制内容"
                          @click="copyMessageContent(message, 'assistant')"
                        >
                          <WkIcon name="copy" :box-size="20" class="shrink-0 material-symbols-outlined leading-none" title="复制内容" />
                        </button>
                      </div>
                      <div
                        class="wk-markdown"
                        :class="{ 'streaming-cursor': message.isStreaming && message.content.length == 0 }"
                        v-html="renderAssistantMessage(message.content, message.isStreaming)"
                      />
                    </div>
                    <!-- Attachments -->
                    <div v-if="getInlineAttachments(message).length > 0" class="space-y-2">
                      <div v-for="att in getInlineAttachments(message)" :key="att.id || att.fileName">
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
                    <!-- <div
                      class="text-xs font-medium"
                      :class="message.isStreaming ? 'text-primary/70' : 'text-slate-400'"
                    >
                      {{ _getAssistantMessageStatus(message) }} · {{ _formatTime(message.timestamp) }}
                    </div> -->
                  </div>
                </div>

                <!-- User Message -->
                <div v-else class="flex gap-4 md:gap-5 flex-row-reverse">
                  <div v-if="false" class="size-9 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-200 flex items-center justify-center">
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
                    <!-- <div class="bg-primary text-white rounded-2xl rounded-tr-none p-4 shadow-lg shadow-primary/10 text-sm leading-relaxed"> -->
                    <div class="group relative bg-[#e9e9e980] text-[#0d0d0d] rounded-[24px] px-4 py-[0.6rem] text-[16px] leading-relaxed">
                      <div
                        class="absolute -bottom-10 right-0 z-10 size-10 flex items-center justify-center opacity-0 pointer-events-none transition-all group-hover:opacity-100 group-hover:pointer-events-auto"
                      >
                        <button
                          type="button"
                          class="size-8 rounded-lg border-slate-200 bg-white text-slate-500 transition-colors flex items-center justify-center hover:bg-[#E7E7E7] hover:text-slate-900"
                          aria-label="复制内容"
                          @click="copyMessageContent(message, 'user')"
                        >
                        <WkIcon name="copy" :box-size="20" class="shrink-0 material-symbols-outlined leading-none" title="复制内容" />
                        </button>
                      </div>
                      <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                    </div>
                    <!-- User Attachments -->
                    <div v-if="getInlineAttachments(message).length > 0" class="space-y-2">
                      <div v-for="att in getInlineAttachments(message)" :key="att.id || att.fileName">
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
                    <!-- <div class="text-xs text-slate-400 font-medium text-right">{{ formatTime(message.timestamp) }}</div> -->
                  </div>
                </div>
              </div>
            </template>
          </div>

          <Transition name="scroll-to-bottom">
            <button
              v-if="showScrollToBottomButton"
              type="button"
              class="absolute left-1/2 -translate-x-1/2 bottom-[140px] md:bottom-[220px] z-20 size-8 rounded-full border border-slate-200 bg-white shadow-lg shadow-slate-200/60 text-slate-600 transition-all flex items-center justify-center hover:bg-slate-50 hover:text-slate-900"
              aria-label="回到底部"
              @click="scrollToBottomSmooth"
            >
              <span class="material-symbols-outlined text-[18px] leading-none">arrow_downward</span>
            </button>
          </Transition>

          <!-- Input Area -->
          <div
            class="shrink-0 pb-2 md:pb-2"
            :class="isChatEmpty ? 'bg-transparent pt-0' : 'bg-gradient-to-t from-white via-white to-transparent'"
          >
            <div class="mx-auto space-y-8 w-[calc(100%-20px)] max-w-4xl md:w-full">

              <!-- Selected Files Preview -->
              <div v-if="false && selectedFiles.length > 0" class="flex flex-wrap gap-2">
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
              <div class="relative group" :class="isMobile ? '' : 'w-[768px] mx-auto'">
                <!-- <div class="absolute inset-0 bg-primary/5 blur-xl rounded-2xl group-focus-within:bg-primary/10 transition-all opacity-0 group-focus-within:opacity-100"></div> -->
                <div class="absolute inset-0 bg-primary/5 blur-xl rounded-2xl transition-all opacity-0"></div>
                <!-- focus-within:border-primary -->
                <div
                  class="relative flex bg-white rounded-2xl p-2 shadow-[0_0_#0000,0_0_#0000,0_0_#0000,0_0_#0000,0px_3px_6px_0px_#0000000a,0px_4px_80px_8px_#0000000a,0px_0px_1px_0px_#0000009e]  transition-all"
                  :class="isMobile ? 'flex-col items-stretch gap-2' : 'items-center rounded-[28px] p-[6px]'"
                  @mousedown="handleInputBoxMouseDown"
                >
                  <div class="w-full">
                  <input
                    ref="fileInputRef"
                    type="file"
                    multiple
                    :accept="CHAT_ATTACHMENT_ACCEPT"
                    class="hidden"
                    @change="handleFileSelect"
                  />

                  <div
                    v-if="composerAttachmentPreviewItems.length > 0 || composerAttachmentPreviewMoreCount > 0"
                    class="flex flex-wrap gap-2 px-2 pt-2 mb-2"
                  >
                    <template v-for="item in composerAttachmentPreviewItems" :key="item.key">
                      <div
                        v-if="item.kind === 'knowledge'"
                        class="relative h-[54px] min-w-[200px] max-w-[320px] flex-1 rounded-2xl bg-[#f5f5f5] overflow-hidden shrink-0 flex items-center gap-3 px-3"
                      >
                        <div
                          class="size-10 rounded-xl flex items-center justify-center shrink-0"
                          :class="getKnowledgeDocIconMeta(item.knowledge).bg"
                        >
                          <span
                            class="material-symbols-outlined text-[22px] leading-none"
                            :class="getKnowledgeDocIconMeta(item.knowledge).color"
                          >{{ getKnowledgeDocIconMeta(item.knowledge).icon }}</span>
                        </div>
                        <div class="min-w-0 flex-1">
                          <div class="text-[14px] leading-[18px] text-[#0d0d0d] truncate">{{ item.knowledge.name }}</div>
                          <div class="text-[12px] leading-[14px] text-[#909090]">{{ getKnowledgeCardSubtitle(item.knowledge) }}</div>
                        </div>
                        <button
                          type="button"
                          class="absolute top-2 right-2 size-5 rounded-full bg-[#0d0d0d] text-white flex items-center justify-center"
                          @click="removeSelectedKnowledgeById(item.knowledge.knowledgeId)"
                        >
                          <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                        </button>
                      </div>

                      <div
                        v-else-if="item.kind === 'file' && item.file.type.startsWith('image/')"
                        class="relative w-[54px] h-[54px] rounded-xl border border-[#0d0d0d0d] bg-white overflow-hidden shrink-0"
                      >
                        <img
                          :src="getSelectedFilePreviewUrl(item.file)"
                          :alt="item.file.name"
                          class="size-full object-cover"
                        />
                        <button
                          type="button"
                          class="absolute top-[0.3rem] right-[-0.01rem] size-5 rounded-full bg-[#0d0d0d] text-white flex items-center justify-center"
                          @click="removeSelectedFile(item.fileIndex)"
                        >
                          <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                        </button>
                      </div>

                      <div
                        v-else-if="item.kind === 'file'"
                        class="relative h-[54px] min-w-[200px] max-w-[320px] flex-1 rounded-2xl bg-[#f5f5f5] overflow-hidden shrink-0 flex items-center gap-3 px-3"
                      >
                        <div
                          class="size-10 rounded-xl flex items-center justify-center shrink-0"
                          :class="getChatDocIconMeta(item.file).bg"
                        >
                          <span
                            class="material-symbols-outlined text-[22px] leading-none"
                            :class="getChatDocIconMeta(item.file).color"
                          >{{ getChatDocIconMeta(item.file).icon }}</span>
                        </div>
                        <div class="min-w-0 flex-1">
                          <div class="text-[14px] leading-[18px] text-[#0d0d0d] truncate">{{ item.file.name }}</div>
                          <div class="text-[12px] leading-[14px] text-[#909090]">{{ getChatDocumentSubtitle(item.file) }}</div>
                        </div>
                        <button
                          type="button"
                          class="absolute top-2 right-2 size-5 rounded-full bg-[#0d0d0d] text-white flex items-center justify-center"
                          @click="removeSelectedFile(item.fileIndex)"
                        >
                          <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                        </button>
                      </div>
                    </template>

                    <div
                      v-if="composerAttachmentPreviewMoreCount > 0"
                      class="h-12 flex items-center text-xs text-[#909090] pr-1"
                    >
                      +{{ composerAttachmentPreviewMoreCount }}
                    </div>
                  </div>

                  <div
                    v-if="showKnowledgeFollowUpChips && (selectedFiles.length > 0 || selectedKnowledgeItems.length > 0)"
                    class="flex flex-wrap gap-2 px-2 pb-2"
                  >
                    <button
                      v-for="(t, i) in KNOWLEDGE_DOC_PROMPTS"
                      :key="i"
                      type="button"
                      class="inline-flex max-w-full items-center gap-1.5 rounded-xl bg-[#f5f5f5] px-3 py-2 text-left text-[13px] leading-snug text-[#0d0d0d] transition-colors hover:bg-[#ececec]"
                      @click="applyKnowledgeDocPrompt(t)"
                    >
                      <span class="min-w-0">{{ t }}</span>
                      <span class="material-symbols-outlined shrink-0 text-[16px] leading-none text-[#909090]">arrow_forward</span>
                    </button>
                  </div>

                  <!-- PC: input (2nd line) -->
                  <div v-if="!isMobile" class="w-full">
                    <textarea
                      ref="pcChatInputRef"
                      v-model="inputText"
                      rows="1"
                      class="w-full bg-transparent border-none focus:ring-0 focus:outline-none px-3 pt-3 text-[#0d0d0d] text-[16px] leading-[26px] placeholder:text-[#909090] placeholder:text-[16px] resize-none overflow-x-hidden overflow-y-auto min-h-[50px]"
                      :placeholder="chatInputPlaceholder"
                      :disabled="chatStore.isStreaming || isUploading"
                      style="min-height: 90px;"
                      @input="resizeChatTextarea"
                      @keydown.enter.exact.prevent="handleSend"
                      @paste="handlePaste"
                    />
                  </div>

                  <!-- PC: controls (3rd line) -->
                  <div v-if="!isMobile" class="flex items-center justify-between w-full px-1 pb-1 select-none mt-1">
                    <div class="flex items-center gap-2">
                      <el-popover
                        v-model:visible="chatUploadMenuVisible"
                        trigger="click"
                        placement="top-start"
                        width="200"
                        :show-arrow="false"
                        :disabled="isUploading"
                        :teleported="true"
                        transition="el-zoom-in-bottom"
                        popper-class="wk-chat-upload-menu-popper"
                      >
                        <template #reference>
                          <button
                            type="button"
                            class="group/chat-upload-trigger relative flex size-8 items-center justify-center rounded-full text-[#0d0d0d] transition-colors hover:bg-[#F1F1F1]"
                            :disabled="isUploading"
                            aria-label="添加文件等"
                          >
                            <WkIcon name="add-1" :box-size="16" class="shrink-0" />
                            <span
                              class="pointer-events-none absolute left-1/2 top-full z-[200] mt-2 -translate-x-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/chat-upload-trigger:opacity-100"
                              role="tooltip"
                            >
                              添加文件等
                            </span>
                          </button>
                        </template>
                        <div class="wk-chat-upload-menu">
                          <button
                            type="button"
                            class="wk-chat-upload-menu__item"
                            :disabled="isUploading"
                            @click="handleChatUploadMenuAddFile"
                          >
                            <span class="wk-chat-upload-menu__icon material-symbols-outlined">attach_file</span>
                            <span class="wk-chat-upload-menu__label">上传照片和文件</span>
                          </button>
                          <button
                            type="button"
                            class="wk-chat-upload-menu__item"
                            :disabled="isUploading"
                            @click="handleChatUploadMenuChooseKnowledge"
                          >
                            <WkIcon name="knowledge-1" :size="18" class="shrink-0" />
                            <span class="wk-chat-upload-menu__label">选择知识库文件</span>
                          </button>
                          <el-popover
                            :trigger="isMobile ? 'click' : 'hover'"
                            placement="right-end"
                            :show-arrow="false"
                            :disabled="isUploading"
                            :teleported="true"
                            :offset="8"
                            :hide-after="isMobile ? 0 : 220"
                            width="200"
                            popper-class="wk-chat-upload-menu-popper wk-chat-upload-submenu-popper"
                          >
                            <template #reference>
                              <div
                                class="wk-chat-upload-menu__apps-ref"
                                role="button"
                                tabindex="0"
                              >
                                <WkIcon name="application" :size="18" class="shrink-0" />
                                <!-- <span class="wk-chat-upload-menu__icon material-symbols-outlined">apps</span> -->
                                <span class="wk-chat-upload-menu__label">应用</span>
                                <span class="wk-chat-upload-menu__chevron material-symbols-outlined">chevron_right</span>
                              </div>
                            </template>
                            <div class="wk-chat-upload-submenu">
                              <button
                                type="button"
                                class="wk-chat-upload-menu__item wk-chat-upload-submenu__btn"
                                @click="handleChatUploadMenuOpenCrm"
                              >
                                <WkIcon
                                  name="customer"
                                  :size="18"
                                  class="shrink-0"
                                  :class="chatStore.crmContextEnabled ? '!text-[#0169cc]' : ''"
                                />
                                <span class="wk-chat-upload-menu__label wk-chat-upload-submenu__label" :class="chatStore.crmContextEnabled ? 'text-[#0169cc]' : 'text-[#0d0d0d]'">CRM管理</span>
                                <span
                                  v-if="chatStore.crmContextEnabled"
                                  class="wk-chat-upload-menu__check material-symbols-outlined fill-1 text-primary"
                                >check</span>
                              </button>
                            </div>
                          </el-popover>
                        </div>
                      </el-popover>
                      <button
                        v-if="chatStore.crmContextEnabled"
                        type="button"
                        class="group/crm-toolbar h-[36px] rounded-full hover:bg-primary/10 pl-1 pr-3.5 text-sm text-[#0285FF] shadow-primary/10 transition-all"
                        aria-pressed="true"
                        title="已启用 CRM 管理，点击关闭"
                        @click="chatStore.setCrmContextEnabled(false)"
                      >
                        <span class="flex items-center gap-1.5">
                          <span class="relative flex size-[22px] shrink-0 items-center justify-center">
                            <span
                              class="flex size-full items-center justify-center transition-opacity duration-150 max-sm:pointer-events-none max-sm:opacity-0 group-hover/crm-toolbar:pointer-events-none group-hover/crm-toolbar:opacity-0"
                            >
                              <WkIcon name="customer" :size="18" class="shrink-0" />
                            </span>
                            <span
                              class="pointer-events-none absolute inset-0 flex items-center justify-center rounded-full bg-primary/25 text-primary opacity-0 transition-opacity duration-150 max-sm:opacity-100 group-hover/crm-toolbar:opacity-100"
                              aria-hidden="true"
                            >
                              <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                            </span>
                          </span>
                          <span>CRM管理</span>
                        </span>
                      </button>
                      <!-- <button
                        type="button"
                        class="h-10 rounded-full pl-1 pr-3.5 text-sm transition-all"
                        :class="chatStore.ragEnabled
                          ? 'border-primary/25 text-primary shadow-primary/10'
                          : 'border-slate-200 bg-white text-[#0d0d0d] hover:border-slate-300 hover:text-slate-700'"
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
                      </button> -->
                    </div>

                    <div class="flex items-center gap-1.5 pr-1 shrink-0">
                      <el-popover
                        v-model:visible="chatModelPopoverVisible"
                        trigger="click"
                        placement="top-end"
                        width="280"
                        :show-arrow="false"
                        :teleported="true"
                        transition="el-zoom-in-bottom"
                        popper-class="wk-chat-model-popper"
                      >
                        <template #reference>
                          <button
                            type="button"
                            class="inline-flex h-9 max-w-[178px] shrink-0 items-center gap-1.5 rounded-[18px] border border-[#ececec] bg-[#f5f5f5] pl-2 pr-2 text-left text-[13px] text-[#0d0d0d] transition-colors hover:bg-[#ececec]"
                            :title="`当前模型：${selectedChatModel.label}`"
                          >
                            <span
                              class="relative size-7 shrink-0 overflow-hidden rounded-md bg-[#ececec]"
                              aria-hidden="true"
                            >
                              <img
                                v-if="chatModelShowImage(selectedChatModel)"
                                :src="chatModelIconSrc(selectedChatModel)"
                                alt=""
                                class="size-full object-cover"
                                @error="onChatModelIconError(selectedChatModel.id)"
                              />
                              <span
                                v-else
                                class="flex size-full items-center justify-center text-[11px] font-semibold text-[#909090]"
                              >
                                {{ selectedChatModel.label.slice(0, 1) }}
                              </span>
                            </span>
                            <span class="min-w-0 flex-1 truncate">{{ selectedChatModel.label }}</span>
                            <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-[#8f8f8f]">expand_more</span>
                          </button>
                        </template>
                        <div class="wk-chat-model-menu">
                          <button
                            v-for="m in CHAT_MODEL_OPTIONS"
                            :key="m.id"
                            type="button"
                            class="wk-chat-model-menu__item"
                            @click="selectChatModel(m.id)"
                          >
                            <span
                              class="relative mt-0.5 size-8 shrink-0 overflow-hidden rounded-lg bg-[#ececec]"
                              aria-hidden="true"
                            >
                              <img
                                v-if="chatModelShowImage(m)"
                                :src="chatModelIconSrc(m)"
                                alt=""
                                class="size-full object-cover"
                                @error="onChatModelIconError(m.id)"
                              />
                              <span
                                v-else
                                class="flex size-full items-center justify-center text-[12px] font-semibold text-[#909090]"
                              >
                                {{ m.label.slice(0, 1) }}
                              </span>
                            </span>
                            <div class="min-w-0 flex-1">
                              <div class="text-[14px] leading-tight text-[#0d0d0d]">{{ m.label }}</div>
                              <div class="mt-0.5 text-[12px] leading-snug text-[#909090]">{{ m.description }}</div>
                            </div>
                            <span
                              v-if="selectedChatModelId === m.id"
                              class="material-symbols-outlined mt-0.5 shrink-0 text-[20px] leading-none text-primary"
                            >
                              check
                            </span>
                          </button>
                        </div>
                      </el-popover>
                      <button
                        type="button"
                        class="group/send-bar-action relative flex h-[36px] w-[36px] shrink-0 items-center justify-center rounded-full transition-colors"
                        :class="sendBarActionButtonClass"
                        :disabled="sendBarActionDisabled"
                        :aria-label="sendBarActionTitle"
                        @click="handleSendBarClick"
                      >
                        <span v-if="chatStore.isStreaming" class="material-symbols-outlined text-[20px] leading-none">stop</span>
                        <span v-else-if="isUploading" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                        <span v-else-if="isTranscribing" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                        <span v-else-if="isRecording" class="material-symbols-outlined text-[20px] leading-none">stop</span>
                        <WkIcon
                          v-else-if="isChatInputEmpty"
                          name="voice"
                          :box-size="20"
                          class="material-symbols-outlined text-[20px] leading-none"
                        />
                        <span v-else class="material-symbols-outlined text-[20px] leading-none">arrow_upward</span>
                        <span
                          class="pointer-events-none absolute left-1/2 top-full z-[200] mt-2 -translate-x-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/send-bar-action:opacity-100"
                          role="tooltip"
                        >
                          {{ sendBarActionTitle }}
                        </span>
                      </button>
                    </div>
                  </div>

                  <!-- Mobile: upload + multiline input -->
                  <div v-else class="flex items-end w-full">
                    <el-popover
                      v-model:visible="chatUploadMenuVisible"
                      trigger="click"
                      placement="top-start"
                      width="240"
                      :show-arrow="false"
                      :disabled="isUploading"
                      :teleported="true"
                      popper-class="wk-chat-upload-menu-popper"
                    >
                      <template #reference>
                        <button
                          type="button"
                          class="size-12 flex items-center justify-center text-[#0d0d0d] hover:text-primary transition-colors shrink-0"
                          :disabled="isUploading"
                        >
                          <span class="material-symbols-outlined text-[0.875rem] leading-none">add</span>
                        </button>
                      </template>
                      <div class="wk-chat-upload-menu">
                        <button
                          type="button"
                          class="wk-chat-upload-menu__item"
                          :disabled="isUploading"
                          @click="handleChatUploadMenuAddFile"
                        >
                          <span class="wk-chat-upload-menu__icon material-symbols-outlined">attach_file</span>
                          <span class="wk-chat-upload-menu__label">上传照片和文件</span>
                        </button>
                        <button
                          type="button"
                          class="wk-chat-upload-menu__item"
                          :disabled="isUploading"
                          @click="handleChatUploadMenuChooseKnowledge"
                        >
                          <span class="wk-chat-upload-menu__icon material-symbols-outlined">menu_book</span>
                          <span class="wk-chat-upload-menu__label">选择知识库文件</span>
                        </button>
                        <el-popover
                          :trigger="isMobile ? 'click' : 'hover'"
                          placement="right-end"
                          :show-arrow="false"
                          :disabled="isUploading"
                          :teleported="true"
                          :offset="8"
                          :hide-after="isMobile ? 0 : 220"
                          width="200"
                          popper-class="wk-chat-upload-menu-popper wk-chat-upload-submenu-popper"
                        >
                          <template #reference>
                            <div
                              class="wk-chat-upload-menu__apps-ref"
                              role="button"
                              tabindex="0"
                            >
                              <span class="wk-chat-upload-menu__icon material-symbols-outlined">apps</span>
                              <span class="wk-chat-upload-menu__label">应用</span>
                              <span class="wk-chat-upload-menu__chevron material-symbols-outlined">chevron_right</span>
                            </div>
                          </template>
                          <div class="wk-chat-upload-submenu">
                            <button
                              type="button"
                              class="wk-chat-upload-menu__item wk-chat-upload-submenu__btn"
                              @click="handleChatUploadMenuOpenCrm"
                            >
                              <WkIcon
                                name="customer"
                                :size="18"
                                class="shrink-0"
                                :class="chatStore.crmContextEnabled ? 'text-primary' : ''"
                              />
                              <span class="wk-chat-upload-menu__label wk-chat-upload-submenu__label">CRM管理</span>
                              <span
                                v-if="chatStore.crmContextEnabled"
                                class="wk-chat-upload-menu__check material-symbols-outlined fill-1 text-primary"
                              >check</span>
                            </button>
                          </div>
                        </el-popover>
                      </div>
                    </el-popover>
                    <textarea
                      ref="mobileChatInputRef"
                      v-model="inputText"
                      rows="1"
                      class="flex-1 bg-transparent border-none focus:ring-0 focus:outline-none pl-1 pr-3 py-3 text-[#0d0d0d] text-[16px] leading-[26px] placeholder:text-[#0d0d0d] placeholder:text-[16px] resize-none overflow-x-hidden overflow-y-auto min-h-[50px]"
                      :placeholder="chatInputPlaceholder"
                      :disabled="chatStore.isStreaming || isUploading"
                      @input="resizeChatTextarea"
                      @keydown.enter.exact.prevent="handleSend"
                      @paste="handlePaste"
                    />
                  </div>
                  </div>

                  <div v-if="isMobile" class="flex items-center justify-between gap-2">
                    <div class="flex min-w-0 flex-1 flex-wrap items-center gap-2">
                      <button
                        v-if="chatStore.crmContextEnabled"
                        type="button"
                        class="group/crm-toolbar h-[36px] self-start inline-flex hover:bg-primary/10 rounded-xl px-3.5 text-sm text-primary shadow-sm shadow-primary/10 transition-all"
                        aria-pressed="true"
                        title="已启用 CRM 管理，点击关闭"
                        @click="chatStore.setCrmContextEnabled(false)"
                      >
                        <span class="flex items-center justify-center gap-1.5">
                          <span class="relative flex size-[22px] shrink-0 items-center justify-center">
                            <span
                              class="flex size-full items-center justify-center transition-opacity duration-150 max-sm:pointer-events-none max-sm:opacity-0 group-hover/crm-toolbar:pointer-events-none group-hover/crm-toolbar:opacity-0"
                            >
                              <WkIcon name="customer" :size="18" class="shrink-0" />
                            </span>
                            <span
                              class="pointer-events-none absolute inset-0 flex items-center justify-center rounded-full bg-primary/25 text-primary opacity-0 transition-opacity duration-150 max-sm:opacity-100 group-hover/crm-toolbar:opacity-100"
                              aria-hidden="true"
                            >
                              <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                            </span>
                          </span>
                          <span>CRM管理</span>
                        </span>
                      </button>
                      <!-- <button
                        type="button"
                        class="h-10 self-start inline-flex rounded-xl border px-3.5 text-sm shadow-sm transition-all"
                        :class="chatStore.ragEnabled
                          ? 'border-primary/25 bg-primary/10 text-primary shadow-primary/10'
                          : 'border-slate-200 bg-white text-[#0d0d0d] hover:border-slate-300 hover:text-slate-700'"
                        :aria-pressed="chatStore.ragEnabled"
                        :title="chatStore.ragEnabled ? '已启用 知识库 检索' : '点击启用 知识库 检索'"
                        @click="chatStore.setRagEnabled(!chatStore.ragEnabled)"
                      >
                        <span class="flex items-center justify-center gap-1.5">
                          <span class="material-symbols-outlined text-[18px] leading-none">
                            menu_book
                          </span>
                          <span>知识库检索</span>
                        </span>
                      </button> -->
                    </div>

                    <button
                      type="button"
                      class="group/send-bar-action relative flex h-[36px] w-[36px] shrink-0 items-center justify-center rounded-full transition-colors"
                      :class="sendBarActionButtonClass"
                      :disabled="sendBarActionDisabled"
                      :aria-label="sendBarActionTitle"
                      @click="handleSendBarClick"
                    >
                      <span v-if="chatStore.isStreaming" class="material-symbols-outlined text-[20px] leading-none">stop</span>
                      <span v-else-if="isUploading" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                      <span v-else-if="isTranscribing" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                      <span v-else-if="isRecording" class="material-symbols-outlined text-[20px] leading-none">stop</span>
                      <WkIcon
                        v-else-if="isChatInputEmpty"
                        name="voice"
                        :box-size="20"
                        class="material-symbols-outlined text-[20px] leading-none"
                      />
                      <span v-else class="material-symbols-outlined text-[20px] leading-none">arrow_upward</span>
                      <span
                        class="pointer-events-none absolute left-1/2 top-full z-[200] mt-2 -translate-x-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/send-bar-action:opacity-100"
                        role="tooltip"
                      >
                        {{ sendBarActionTitle }}
                      </span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Quick Action Chips -->
              <div v-if="chatStore.messages.length === 0" class="flex flex-wrap gap-2 justify-center mt-6">
                <button
                  v-for="action in quickActions"
                  :key="action.label"
                  class="px-4 py-[9px] bg-white border border-slate-200 rounded-full text-[14px] text-[#5d5d5d] transition-all shadow-sm hover:bg-[#F1F1F1] hover:border-[#E0E0E0] hover:text-[#0d0d0d]"
                  @click="sendQuickMessage(action.text)"
                >
                  {{ action.label }}
                </button>
              </div>
              <p v-if="chatStore.messages.length > 0" class="text-center text-xs text-[#5d5d5d] uppercase " style="margin-top: 10px !important;">内容由AI生成，请核查重要信息</p>
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

    <ChatKnowledgePickerModal
      v-model="chatKnowledgePickerVisible"
      :remaining-slots="Math.max(0, MAX_FILE_COUNT - selectedFiles.length - selectedKnowledgeItems.length)"
      @confirm="onKnowledgePickerConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { transcribeFollowUpAudio } from '@/api/followup'
import { getAiConfig } from '@/api/systemConfig'
import {
  registerAiQuotaResumeSendHandler,
  unregisterAiQuotaResumeSendHandler,
  useAiQuota,
} from '@/composables/useAiQuota'
import {
  CHAT_ATTACHMENT_ACCEPT,
  extractClipboardFiles,
  MAX_CHAT_ATTACHMENT_COUNT,
  MAX_CHAT_ATTACHMENT_SIZE,
  mergeChatFiles
} from '@/utils/chatAttachment'
import {
  getAssistantMessagePlaceholder,
  getAssistantMessageStatusLabel,
  normalizeAssistantMessageContent
} from '@/utils/chatMessage'
import { renderMarkdown } from '@/utils/markdown'
import { isRequestErrorHandled } from '@/utils/requestError'
import { formatFileSize, resolveKnowledgeFileSizeBytes } from '@/utils/formatFileSize'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, Knowledge } from '@/types/common'
import ChatKnowledgePickerModal from '@/components/chat/ChatKnowledgePickerModal.vue'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const { composerFocusNonce } = storeToRefs(chatStore)
const agentStore = useAgentStore()
const userStore = useUserStore()
const { isMobile } = useResponsive()
const { loadAiConfig, ensureAiAvailableForSend } = useAiQuota()

const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const showScrollToBottomButton = ref(false)
const mobilePanel = ref<'sessions' | 'chat'>('sessions')
const fileInputRef = ref<HTMLInputElement | null>(null)
/** 桌面端多行输入（模板中 v-if="!isMobile" 的 textarea） */
const pcChatInputRef = ref<HTMLTextAreaElement | null>(null)
/** 移动端输入 */
const mobileChatInputRef = ref<HTMLTextAreaElement | null>(null)

function activeChatInputEl(): HTMLTextAreaElement | null {
  return isMobile.value ? mobileChatInputRef.value : pcChatInputRef.value
}

const chatUploadMenuVisible = ref(false)
const chatKnowledgePickerVisible = ref(false)
const showKnowledgeFollowUpChips = ref(false)

type ChatModelOption = {
  id: string
  label: string
  description: string
  /** 模型图标图片路径（如 `/logo.png` 或完整 URL），缺省或加载失败时显示占位 */
  icon?: string
}

/** 模型切换（测试数据，后续可对接接口） */
const CHAT_MODEL_OPTIONS: ChatModelOption[] = [
  {
    id: 'sonnet-46',
    label: 'Sonnet 4.6',
    description: '适合日常工作的均衡模型',
    icon: '/logo.png',
  },
  { id: 'haiku-45', label: 'Haiku 4.5', description: '更快、更省资源' },
]

const chatModelIconLoadFailed = ref<Record<string, boolean>>({})

function onChatModelIconError(id: string) {
  chatModelIconLoadFailed.value = { ...chatModelIconLoadFailed.value, [id]: true }
}

function chatModelIconSrc(m: ChatModelOption): string | undefined {
  const u = m.icon?.trim()
  return u || undefined
}

function chatModelShowImage(m: ChatModelOption): boolean {
  return Boolean(chatModelIconSrc(m)) && !chatModelIconLoadFailed.value[m.id]
}

const chatModelPopoverVisible = ref(false)
const selectedChatModelId = ref(CHAT_MODEL_OPTIONS[0]!.id)

const selectedChatModel = computed(
  () => CHAT_MODEL_OPTIONS.find(m => m.id === selectedChatModelId.value) ?? CHAT_MODEL_OPTIONS[0]!
)

function selectChatModel(id: string) {
  selectedChatModelId.value = id
  chatModelPopoverVisible.value = false
}

/** 纵向最多展示约 10 行，再继续增高则出现纵向滚动条 */
function getChatTextareaMaxHeightPx(el: HTMLTextAreaElement): number {
  const cs = window.getComputedStyle(el)
  const lh = parseFloat(cs.lineHeight)
  const linePx = Number.isFinite(lh) && lh > 0 ? lh : 26
  const pt = parseFloat(cs.paddingTop) || 0
  const pb = parseFloat(cs.paddingBottom) || 0
  return pt + pb + linePx * 10
}

function resizeChatTextarea() {
  const el = activeChatInputEl()
  if (!el) return
  el.style.height = 'auto'
  const maxH = getChatTextareaMaxHeightPx(el)
  el.style.height = `${Math.min(el.scrollHeight, maxH)}px`
}

watch(inputText, () => nextTick(resizeChatTextarea))
watch(isMobile, () => nextTick(resizeChatTextarea))
const selectedFiles = ref<File[]>([])
const selectedKnowledgeItems = ref<Knowledge[]>([])
watch(
  () => selectedFiles.value.length + selectedKnowledgeItems.value.length,
  (total) => {
    if (total === 0) showKnowledgeFollowUpChips.value = false
  }
)
const isUploading = ref(false)
const isRecording = ref(false)
const isTranscribing = ref(false)

const isChatInputEmpty = computed(
  () =>
    !inputText.value.trim() &&
    selectedFiles.value.length === 0 &&
    selectedKnowledgeItems.value.length === 0
)

const chatInputPlaceholder = computed(() =>
  selectedFiles.value.length > 0 || selectedKnowledgeItems.value.length > 0
    ? '发消息...'
    : '发消息...'
)

const COMPOSER_PREVIEW_MAX = 3

const composerAttachmentPreviewItems = computed(() => {
  type Item =
    | { kind: 'knowledge'; knowledge: Knowledge; key: string }
    | { kind: 'file'; file: File; fileIndex: number; key: string }
  const out: Item[] = []
  for (const k of selectedKnowledgeItems.value) {
    if (out.length >= COMPOSER_PREVIEW_MAX) break
    out.push({ kind: 'knowledge', knowledge: k, key: `k-${k.knowledgeId}` })
  }
  for (let index = 0; index < selectedFiles.value.length; index++) {
    if (out.length >= COMPOSER_PREVIEW_MAX) break
    const file = selectedFiles.value[index]!
    out.push({ kind: 'file', file, fileIndex: index, key: `f-${file.name}-${index}` })
  }
  return out
})

const composerAttachmentPreviewMoreCount = computed(() =>
  Math.max(0, selectedKnowledgeItems.value.length + selectedFiles.value.length - COMPOSER_PREVIEW_MAX)
)

const sendBarActionButtonClass = computed(() => {
  if (chatStore.isStreaming) return 'bg-[#e5e5e5] text-[#0d0d0d]'
  if (isRecording.value) return 'bg-red-500 text-white hover:bg-red-600'
  if (isTranscribing.value || isUploading.value) return 'bg-[#e5e5e5] text-[#909090]'
  if (isChatInputEmpty.value) return 'bg-[#000] text-white hover:bg-[#575757]'
  return 'bg-[#0d0d0d] text-white hover:bg-[#0d0d0d]/90'
})

const sendBarActionDisabled = computed(
  () => isUploading.value || isTranscribing.value || chatStore.isStreaming
)

const sendBarActionTitle = computed(() => {
  if (chatStore.isStreaming) return '回复生成中'
  if (isUploading.value) return '上传中'
  if (isTranscribing.value) return '语音识别中…'
  if (isRecording.value) return '点击结束录音'
  if (isChatInputEmpty.value) return '使用语音功能'
  return '发送提示'
})

let mediaRecorder: MediaRecorder | null = null
let mediaStream: MediaStream | null = null
let recordedChunks: Blob[] = []
let recordedMimeType = ''
let skipNextTranscription = false
let transcriptionToken = 0
let speechInputBase = ''
const currentView = ref<'chat' | 'notifications'>('chat')
const userAvatarLoadFailed = ref(false)

const isChatEmpty = computed(() => chatStore.messages.length === 0)

const SCROLL_TO_BOTTOM_THRESHOLD_PX = 200
function updateScrollToBottomVisibility() {
  const el = messagesContainer.value
  if (!el || chatStore.messages.length === 0) {
    showScrollToBottomButton.value = false
    return
  }
  const distanceToBottom = el.scrollHeight - (el.scrollTop + el.clientHeight)
  showScrollToBottomButton.value = distanceToBottom > SCROLL_TO_BOTTOM_THRESHOLD_PX
}

function handleMessagesScroll() {
  updateScrollToBottomVisibility()
}

function scrollToBottomSmooth() {
  const el = messagesContainer.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  showScrollToBottomButton.value = false
}

const selectedFilePreviewUrlMap = new WeakMap<File, string>()
function getSelectedFilePreviewUrl(file: File): string {
  const cached = selectedFilePreviewUrlMap.get(file)
  if (cached) return cached
  const url = window.URL.createObjectURL(file)
  selectedFilePreviewUrlMap.set(file, url)
  return url
}

function revokeSelectedFilePreviewUrl(file: File) {
  const url = selectedFilePreviewUrlMap.get(file)
  if (!url) return
  window.URL.revokeObjectURL(url)
  selectedFilePreviewUrlMap.delete(file)
}

function revokeAllSelectedFilePreviewUrls() {
  for (const file of selectedFiles.value) revokeSelectedFilePreviewUrl(file)
}

/** 侧栏切换会话时移动端默认停在「历史」列表，输入区未挂载，需先切到聊天主区才能 focus。 */
function prepareComposerForFocus() {
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
}

function focusChatTextarea() {
  prepareComposerForFocus()
  const maxAttempts = 20
  const tryFocus = (attempt: number) => {
    const el = activeChatInputEl()
    if (el && !el.disabled) {
      try {
        el.focus({ preventScroll: true })
      } catch {
        el.focus()
      }
      if (document.activeElement === el) return
    }
    if (attempt + 1 >= maxAttempts) return
    void nextTick(() => {
      requestAnimationFrame(() => {
        setTimeout(() => tryFocus(attempt + 1), 20)
      })
    })
  }
  void nextTick(() => {
    requestAnimationFrame(() => tryFocus(0))
  })
}

watch(composerFocusNonce, () => {
  focusChatTextarea()
}, { flush: 'post' })

function handleInputBoxMouseDown(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (!target) return
  // Don't steal focus from buttons / interactive elements.
  if (target.closest('button')) return
  if (chatStore.isStreaming || isUploading.value) return
  activeChatInputEl()?.focus()
}

const MAX_FILE_SIZE = MAX_CHAT_ATTACHMENT_SIZE
const MAX_FILE_COUNT = MAX_CHAT_ATTACHMENT_COUNT

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
  { label: '把今天新增但还没跟进的客户列出来', text: '把今天新增但还没跟进的客户列出来' },
  { label: '找出快丢单的客户', text: '帮我找出快丢单的客户' },
  { label: '筛选出高意向客户', text: '帮我筛选出高意向客户' },
  { label: '总结本周的销售情况', text: '总结本周的销售情况，并生成销售报告' }
]

const KNOWLEDGE_DOC_PROMPTS = [
  '详细总结这些文档内容',
  '用通俗易懂的话，说说这些文档讲了什么',
  '生成一个简短摘要'
] as const

const showUserAvatarImage = computed(() => Boolean(userStore.avatar) && !userAvatarLoadFailed.value)
const userAvatarFallback = computed(() => (userStore.realname || userStore.username || 'U').charAt(0).toUpperCase())

let abortChatViewMountSequence = false

onMounted(async () => {
  registerAiQuotaResumeSendHandler(handleSend)
  await nextTick()
  resizeChatTextarea()
  await Promise.all([
    chatStore.fetchSessions(),
    agentStore.fetchEnabledAgents(),
    loadAiConfig(),
  ])
  if (abortChatViewMountSequence) return
  await nextTick()
  if (abortChatViewMountSequence) return
  chatStore.requestComposerFocus()
})

onBeforeUnmount(() => {
  abortChatViewMountSequence = true
  abortChatVoiceRecording()
  transcriptionToken += 1
  unregisterAiQuotaResumeSendHandler()
})

// Auto scroll to bottom when new messages arrive or during streaming
let scrollTimer: ReturnType<typeof setTimeout> | null = null
function scrollToBottom() {
  if (scrollTimer) return
  scrollTimer = setTimeout(() => {
    scrollTimer = null
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      updateScrollToBottomVisibility()
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

async function handleSend() {
  const text = inputText.value.trim()
  const hasFiles = selectedFiles.value.length > 0
  const hasKnowledge = selectedKnowledgeItems.value.length > 0
  if ((!text && !hasFiles && !hasKnowledge) || chatStore.isStreaming || isUploading.value) return
  if (!(await ensureAiAvailableForSend())) return

  const content =
    text ||
    (hasFiles && hasKnowledge
      ? '请结合上传的附件与选中的知识库文档回答'
      : hasFiles
        ? '请分析这些文件'
        : '请结合选中的知识库文档回答')
  inputText.value = ''

  const knowledgeIdsPayload = hasKnowledge
    ? selectedKnowledgeItems.value.map((k) => k.knowledgeId)
    : undefined
  selectedKnowledgeItems.value = []

  let attachmentDTOs: ChatAttachmentDTO[] | undefined
  let attachmentVOs: ChatAttachmentVO[] | undefined

  // Upload files to MinIO if any
  if (hasFiles) {
    isUploading.value = true
    try {
      const files = [...selectedFiles.value]
      revokeAllSelectedFilePreviewUrls()
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

  // Switch to chat view when sending
  currentView.value = 'chat'
  await chatStore.sendMessage(
    content,
    attachmentDTOs,
    attachmentVOs,
    chatStore.ragEnabled,
    knowledgeIdsPayload
  )
}

function handleSendBarClick() {
  if (isUploading.value || isTranscribing.value || chatStore.isStreaming) return
  if (isRecording.value) {
    handleStopChatAudioRecording()
    return
  }
  const text = inputText.value.trim()
  const hasFiles = selectedFiles.value.length > 0
  const hasKnowledge = selectedKnowledgeItems.value.length > 0
  if (text || hasFiles || hasKnowledge) {
    void handleSend()
    return
  }
  void handleStartChatAudioRecording()
}

function releaseChatMediaStream() {
  mediaStream?.getTracks().forEach(track => track.stop())
  mediaStream = null
}

function abortChatVoiceRecording() {
  skipNextTranscription = true
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  mediaRecorder = null
  releaseChatMediaStream()
  recordedChunks = []
  recordedMimeType = ''
  isRecording.value = false
}

async function ensureChatAudioTranscriptionSupported(): Promise<boolean> {
  try {
    const aiConfig = await getAiConfig()
    if (aiConfig.capabilities?.supportsAudioTranscription) {
      return true
    }
    ElMessage.warning('当前模型不支持语音识别，请配置支持的模型')
    return false
  } catch (err: unknown) {
    console.error('Load AI config failed:', err)
    if (!isRequestErrorHandled(err)) {
      ElMessage.warning('暂时无法获取语音识别能力，请稍后再试')
    }
    return false
  }
}

function resolveChatRecordingMimeType(): string {
  const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']
  if (typeof MediaRecorder === 'undefined' || typeof MediaRecorder.isTypeSupported !== 'function') {
    return ''
  }
  return candidates.find(type => MediaRecorder.isTypeSupported(type)) || ''
}

function resolveChatAudioExtension(mimeType: string): string {
  if (mimeType.includes('mp4')) return 'm4a'
  if (mimeType.includes('mpeg')) return 'mp3'
  if (mimeType.includes('wav')) return 'wav'
  return 'webm'
}

function buildChatRecordedAudioFile(): File | null {
  if (recordedChunks.length === 0) return null
  const mimeType = recordedMimeType || recordedChunks[0]?.type || 'audio/webm'
  const blob = new Blob(recordedChunks, { type: mimeType })
  return new File([blob], `chat-recording.${resolveChatAudioExtension(mimeType)}`, { type: mimeType })
}

async function transcribeChatRecordedAudio(file: File | null) {
  if (!file) {
    ElMessage.warning('未采集到有效录音，请重试')
    return
  }
  const currentToken = ++transcriptionToken
  isTranscribing.value = true
  try {
    const transcript = (await transcribeFollowUpAudio(file)).trim()
    if (currentToken !== transcriptionToken) return
    if (!transcript) {
      ElMessage.warning('未识别到有效语音内容，请重试')
      return
    }
    inputText.value = speechInputBase ? `${speechInputBase}\n${transcript}` : transcript
    void nextTick(resizeChatTextarea)
    ElMessage.success('语音已转成文字，可继续编辑后发送')
  } catch (err: unknown) {
    console.error('Audio transcription failed:', err)
    if (!isRequestErrorHandled(err)) {
      ElMessage.warning('语音识别失败，请稍后重试')
    }
  } finally {
    if (currentToken === transcriptionToken) {
      isTranscribing.value = false
    }
  }
}

async function handleChatRecordedAudioStop() {
  const shouldSkip = skipNextTranscription
  skipNextTranscription = false
  isRecording.value = false
  const file = shouldSkip ? null : buildChatRecordedAudioFile()
  mediaRecorder = null
  releaseChatMediaStream()
  recordedChunks = []
  recordedMimeType = ''
  if (shouldSkip) return
  await transcribeChatRecordedAudio(file)
}

async function handleStartChatAudioRecording() {
  if (isRecording.value || isTranscribing.value) return
  if (!(await ensureChatAudioTranscriptionSupported())) return
  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
    ElMessage.warning('当前浏览器不支持录音，请改用文字输入')
    return
  }
  try {
    speechInputBase = inputText.value.trim()
    skipNextTranscription = false
    recordedChunks = []
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const mimeType = resolveChatRecordingMimeType()
    mediaRecorder = mimeType
      ? new MediaRecorder(mediaStream, { mimeType })
      : new MediaRecorder(mediaStream)
    recordedMimeType = mediaRecorder.mimeType || mimeType || 'audio/webm'
    mediaRecorder.ondataavailable = (event: BlobEvent) => {
      if (event.data && event.data.size > 0) {
        recordedChunks.push(event.data)
      }
    }
    mediaRecorder.onstop = () => {
      void handleChatRecordedAudioStop()
    }
    mediaRecorder.onerror = (event: Event) => {
      console.error('MediaRecorder error:', event)
      ElMessage.warning('录音失败，请检查麦克风权限后重试')
    }
    mediaRecorder.start()
    isRecording.value = true
  } catch (err) {
    console.error('Start recording failed:', err)
    abortChatVoiceRecording()
    ElMessage.warning('无法启动录音，请检查浏览器和麦克风权限')
  }
}

function handleStopChatAudioRecording() {
  if (!mediaRecorder) return
  skipNextTranscription = false
  if (mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
}

function sendQuickMessage(text: string) {
  inputText.value = text
  handleSend()
}

function renderAssistantMessage(content: string, isStreaming = false): string {
  const normalized = normalizeAssistantMessageContent(content, isStreaming)
  return renderMarkdown(normalized || getAssistantMessagePlaceholder(isStreaming))
}

function htmlToText(html: string): string {
  const div = document.createElement('div')
  div.innerHTML = html
  return (div.textContent || '').replace(/\u00a0/g, ' ').trim()
}

async function copyToClipboard(text: string) {
  const value = text.trim()
  if (!value) return

  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = value
    textarea.setAttribute('readonly', 'true')
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (ok) ElMessage.success('已复制')
    else ElMessage.warning('复制失败')
  }
}

async function copyMessageContent(message: { content: string; isStreaming?: boolean }, kind: 'assistant' | 'user') {
  if (kind === 'assistant') {
    const html = renderAssistantMessage(message.content || '', Boolean(message.isStreaming))
    await copyToClipboard(htmlToText(html))
    return
  }
  await copyToClipboard(message.content || '')
}

function _getAssistantMessageStatus(message: { isStreaming?: boolean }): string {
  return getAssistantMessageStatusLabel(Boolean(message.isStreaming))
}

function handleUpload() {
  fileInputRef.value?.click()
}

function handleChatUploadMenuAddFile() {
  chatUploadMenuVisible.value = false
  handleUpload()
}

async function handleChatUploadMenuChooseKnowledge() {
  if (selectedFiles.value.length + selectedKnowledgeItems.value.length >= MAX_CHAT_ATTACHMENT_COUNT) {
    ElMessage.warning(`最多只能上传${MAX_CHAT_ATTACHMENT_COUNT}个文件`)
    return
  }
  chatUploadMenuVisible.value = false
  chatKnowledgePickerVisible.value = true
}

function handleChatUploadMenuOpenCrm() {
  chatUploadMenuVisible.value = false
  chatStore.setCrmContextEnabled(!chatStore.crmContextEnabled)
}

function appendSelectedFiles(files: File[]) {
  const slotsLeft =
    MAX_CHAT_ATTACHMENT_COUNT - selectedKnowledgeItems.value.length - selectedFiles.value.length
  if (slotsLeft <= 0) {
    ElMessage.warning(`最多只能上传${MAX_CHAT_ATTACHMENT_COUNT}个文件`)
    return
  }
  const capped = files.slice(0, slotsLeft)
  const result = mergeChatFiles(selectedFiles.value, capped)
  if (result.error) {
    ElMessage.warning(result.error)
    return
  }

  selectedFiles.value = result.files
}

function onKnowledgePickerConfirm(items: Knowledge[]) {
  const cap = MAX_CHAT_ATTACHMENT_COUNT
  const room = cap - selectedFiles.value.length - selectedKnowledgeItems.value.length
  if (room <= 0) {
    ElMessage.warning(`最多只能上传${MAX_CHAT_ATTACHMENT_COUNT}个文件`)
    return
  }
  const existing = new Set(selectedKnowledgeItems.value.map((k) => k.knowledgeId))
  const toAdd: Knowledge[] = []
  for (const it of items) {
    if (toAdd.length >= room) break
    if (existing.has(it.knowledgeId)) continue
    existing.add(it.knowledgeId)
    toAdd.push(it)
  }
  if (toAdd.length === 0) {
    ElMessage.warning('所选文档已在列表中或超出数量上限')
    return
  }
  selectedKnowledgeItems.value = [...selectedKnowledgeItems.value, ...toAdd]
  showKnowledgeFollowUpChips.value = true
}

function removeSelectedKnowledgeById(knowledgeId: string) {
  selectedKnowledgeItems.value = selectedKnowledgeItems.value.filter((k) => k.knowledgeId !== knowledgeId)
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files) return

  const newFiles = Array.from(input.files)

  // Validate file count
  if (selectedFiles.value.length + newFiles.length + selectedKnowledgeItems.value.length > MAX_FILE_COUNT) {
    ElMessage.warning(`最多只能上传${MAX_FILE_COUNT}个文件`)
    input.value = ''
    return
  }

  // Validate file size
  for (const file of newFiles) {
    if (file.size > MAX_FILE_SIZE) {
      ElMessage.warning(`文件"${file.name}"超过50MB限制`)
      input.value = ''
      return
    }
  }

  selectedFiles.value.push(...newFiles)
  input.value = '' // Reset input for re-selecting same file
}

function handlePaste(event: ClipboardEvent) {
  const clipboardFiles = extractClipboardFiles(event)
  if (clipboardFiles.length === 0) {
    return
  }

  appendSelectedFiles(clipboardFiles)
}

function removeSelectedFile(index: number) {
  const file = selectedFiles.value[index]
  if (file) revokeSelectedFilePreviewUrl(file)
  selectedFiles.value.splice(index, 1)
}

function isImageAttachment(attachment?: ChatAttachmentVO | null): boolean {
  return Boolean(attachment?.mimeType?.startsWith('image/'))
}

function isDocumentAttachment(attachment?: ChatAttachmentVO | null): boolean {
  return Boolean(attachment) && !isImageAttachment(attachment)
}

function getInlineAttachments(message: { attachments?: ChatAttachmentVO[] }): ChatAttachmentVO[] {
  return (message.attachments || []).filter(isImageAttachment)
}

function getDocumentAttachments(message: { attachments?: ChatAttachmentVO[] }): ChatAttachmentVO[] {
  return (message.attachments || []).filter(isDocumentAttachment)
}

function getFriendlyFileKind(file: File): string {
  const m = (file.type || '').toLowerCase()
  if (m.includes('spreadsheetml') || m.includes('ms-excel') || m === 'application/vnd.ms-excel') return 'Excel'
  if (m.includes('wordprocessingml') || m.includes('msword') || m === 'application/msword') return 'Word'
  if (m === 'application/pdf') return 'PDF'
  if (m.startsWith('text/')) return '文本'
  if (m.startsWith('image/')) return '图片'
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (ext === 'xlsx' || ext === 'xls') return 'Excel'
  if (ext === 'docx' || ext === 'doc') return 'Word'
  if (ext === 'pdf') return 'PDF'
  return '文件'
}

function getChatDocumentSubtitle(file: File): string {
  return `${getFriendlyFileKind(file)} · ${formatFileSize(file.size)}`
}

function getChatDocIconMeta(file: File): { icon: string; bg: string; color: string } {
  const m = (file.type || '').toLowerCase()
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (
    m.includes('spreadsheetml') ||
    m.includes('ms-excel') ||
    m === 'application/vnd.ms-excel' ||
    ext === 'xlsx' ||
    ext === 'xls'
  ) {
    return { icon: 'table_chart', bg: 'bg-emerald-50', color: 'text-emerald-700' }
  }
  if (
    m.includes('wordprocessingml') ||
    m.includes('msword') ||
    m === 'application/msword' ||
    ext === 'docx' ||
    ext === 'doc'
  ) {
    return { icon: 'article', bg: 'bg-blue-50', color: 'text-blue-700' }
  }
  if (m === 'application/pdf' || ext === 'pdf') {
    return { icon: 'picture_as_pdf', bg: 'bg-red-50', color: 'text-red-600' }
  }
  if (m.startsWith('image/')) return { icon: 'image', bg: 'bg-violet-50', color: 'text-violet-700' }
  return { icon: 'description', bg: 'bg-[#0d0d0d0d]', color: 'text-[#0d0d0d]' }
}

function getKnowledgeDocIconMeta(k: Knowledge) {
  return getChatDocIconMeta(
    new File([], k.name || 'document', { type: k.mimeType || 'application/octet-stream' })
  )
}

function getKnowledgeCardSubtitle(k: Knowledge): string {
  const stub = new File([], k.name || 'document', { type: k.mimeType || 'application/octet-stream' })
  const kind = getFriendlyFileKind(stub)
  const formatted = k.fileSizeFormatted?.trim()
  if (formatted) return `${kind} · ${formatted}`
  const bytes = resolveKnowledgeFileSizeBytes(k.fileSize)
  if (bytes > 0) return `${kind} · ${formatFileSize(bytes)}`
  return kind
}

function applyKnowledgeDocPrompt(text: string) {
  inputText.value = text
  nextTick(() => resizeChatTextarea())
  chatStore.requestComposerFocus()
}

async function handleNewSession() {
  await chatStore.startNewSessionIfNeeded('新对话')
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
  chatStore.requestComposerFocus()
}

async function handleSelectSession(sessionId: string) {
  // if (chatStore.currentSessionId === sessionId && currentView.value === 'chat') return
  currentView.value = 'chat'
  await chatStore.selectSession(sessionId)
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
  chatStore.requestComposerFocus()
}

async function applyChatSessionRouteQuery() {
  const raw = route.query.sessionId
  const sessionId = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  if (!sessionId) return
  if (!chatStore.sessions.some(s => s.sessionId === sessionId)) {
    await chatStore.fetchSessions()
  }
  if (!chatStore.sessions.some(s => s.sessionId === sessionId)) {
    ElMessage.warning('找不到该对话')
    await router.replace({ path: '/chat' })
    return
  }
  await handleSelectSession(sessionId)
  await router.replace({ path: '/chat' })
}

watch(
  () => route.query.sessionId,
  () => {
    void applyChatSessionRouteQuery()
  },
  { immediate: true }
)

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

function _formatTime(date: Date): string {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

void _getAssistantMessageStatus
void _formatTime

</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  line-clamp: 1;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  line-clamp: 2;
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
  content: '';
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-left: 6px;
  border-radius: 9999px;
  background: #0d0d0d;
  transform-origin: center;
  animation: breathingDot 1.2s ease-in-out infinite;
}

@keyframes breathingDot {
  0%, 100% {
    transform: scale(1);
    opacity: 0.35;
  }
  50% {
    transform: scale(1.2);
    opacity: 1;
  }
}

/* Scroll-to-bottom button transition */
.scroll-to-bottom-enter-active,
.scroll-to-bottom-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.scroll-to-bottom-enter-from,
.scroll-to-bottom-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}

.scroll-to-bottom-enter-to,
.scroll-to-bottom-leave-from {
  opacity: 1;
  transform: translate(-50%, 0);
}

/* Material Symbols fill variant */
.fill-1 {
  font-variation-settings: 'FILL' 1;
}

.wk-chat-upload-menu {
  padding: 10px;
}

.wk-chat-upload-menu__item {
  width: 100%;
  display: flex;
  height: 36px;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 16px;
  color: #0d0d0d;
  transition: background-color 150ms ease, color 150ms ease;
}

.wk-chat-upload-menu__item:hover {
  background: #f9f9f9;
}

.wk-chat-upload-menu__item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.wk-chat-upload-menu__icon {
  font-size: 18px;
  line-height: 1;
  color: #0d0d0d;
}

.wk-chat-upload-menu__label {
  font-size: 14px;
  line-height: 1.2;
  font-weight: 400;
}

.wk-chat-upload-menu__apps-ref {
  width: 100%;
  height: 36px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 16px;
  color: #0d0d0d;
  cursor: default;
  outline: none;
  transition: background-color 150ms ease;
}

.wk-chat-upload-menu__apps-ref:hover {
  background: #f9f9f9;
}

.wk-chat-upload-menu__chevron {
  margin-left: auto;
  font-size: 18px;
  color: rgba(13, 13, 13, 0.55);
}

.wk-chat-upload-submenu {
  padding: 2px;
}

.wk-chat-upload-submenu__btn {
  justify-content: flex-start;
}

.wk-chat-upload-submenu__label {
  flex: 1 1 auto;
  min-width: 0;
  text-align: left;
}

.wk-chat-upload-menu__check {
  margin-left: auto;
  font-size: 18px;
  line-height: 1;
}

</style>

<style>
/* Popover 挂载到 body，scoped 选不中外层 .wk-chat-upload-menu-popper */
.wk-chat-upload-menu-popper.el-popper {
  padding: 0 !important;
  border-radius: 16px !important;
  overflow: hidden;
  border: unset !important;
  box-shadow: 0 0 #0000, 0 0 #0000, 0 0 #0000, 0 0 #0000, 0px 8px 12px 0px #00000014, 0px 0px 1px 0px #0000009e !important;
  /* box-shadow: 0 12px 40px rgba(15, 23, 42, 0.14); */
}

/* 主菜单（不含子菜单类名）在更底层，子菜单气泡叠在上面 */
.wk-chat-upload-menu-popper.el-popper:not(.wk-chat-upload-submenu-popper) {
  z-index: 3000 !important;
}

.wk-chat-upload-menu-popper .el-popper__arrow,
.wk-chat-upload-menu-popper .el-popper__arrow::before {
  display: none !important;
}

.wk-chat-upload-submenu-popper.el-popper {
  z-index: 3100 !important;
}

.wk-chat-model-popper.el-popper {
  padding: 0 !important;
  border-radius: 14px !important;
  overflow: hidden;
  border: unset !important;
  box-shadow: 0 0 #0000, 0 0 #0000, 0 0 #0000, 0 0 #0000, 0px 8px 12px 0px #00000014, 0px 0px 1px 0px #0000009e !important;
  z-index: 3000 !important;
}

.wk-chat-model-popper .el-popper__arrow,
.wk-chat-model-popper .el-popper__arrow::before {
  display: none !important;
}

.wk-chat-model-menu {
  padding: 6px;
}

.wk-chat-model-menu__item {
  display: flex;
  width: 100%;
  align-items: flex-start;
  gap: 10px;
  border-radius: 20px;
  border: none;
  background: transparent;
  padding: 10px 10px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.12s ease;
}

.wk-chat-model-menu__item:hover {
  background: #f5f5f5;
}
</style>
