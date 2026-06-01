<template>
  <div ref="chatViewRef" class="flex h-full" :class="{ 'flex-col': isMobile }">
    <!-- Internal Sidebar: Chat History -->
    <aside v-if="isMobile && mobilePanel === 'sessions'" class="flex flex-1 flex-col bg-slate-50/50">
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

      <!-- Session List -->
      <div class="flex-1 overflow-y-auto px-3 pt-4 space-y-1">
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
      ref="chatMainAreaRef"
      class="wk-chat-shell flex-1 min-w-0 flex flex-col relative overflow-hidden"
    >
      <!-- Chat View -->
      <template v-if="currentView === 'chat'">
        <!-- Mobile top bar (chat detail) -->
        <div
          v-if="showMobileFloatingBar"
          class="wk-mobile-chat-floating-bar pointer-events-none absolute inset-x-0 z-30 px-4 py-3"
          :style="{ top: mobileChatFloatingBarTop }"
        >
          <button
            type="button"
            class="wk-mobile-chat-menu-fab pointer-events-auto"
            aria-label="打开菜单"
            title="打开菜单"
            @click="openMobileMainMenu"
          >
            <span class="material-symbols-outlined text-[22px] leading-none">menu</span>
          </button>
          <div v-if="showMobileChatFloatingActions" class="pointer-events-auto flex items-center justify-end gap-3">
            <div class="wk-mobile-chat-actions" :class="{ 'wk-mobile-chat-actions--single': mobileChatFloatingActionCount === 1 }">
              <button
                v-if="showMobileNewSessionAction"
                type="button"
                class="wk-mobile-chat-actions__btn"
                aria-label="新建会话"
                title="新建会话"
                @click="handleNewSession"
              >
                <span class="material-symbols-outlined text-[20px] leading-none">edit_square</span>
              </button>
              <span v-if="showMobileNewSessionAction && showMobileCustomerSummaryAction" class="wk-mobile-chat-actions__divider" aria-hidden="true"></span>
              <button
                v-if="showMobileCustomerSummaryAction"
                type="button"
                class="wk-mobile-chat-actions__btn"
                aria-label="客户详情"
                title="客户详情"
                @click="openMobileCustomerSummary"
              >
                <span class="material-symbols-outlined text-[24px] leading-none">more_horiz</span>
              </button>
            </div>
          </div>
        </div>

          <div
            class="flex-1 flex flex-col overflow-hidden relative"
            :class="isCenteredEmptyChat && !isMobile ? 'justify-center -translate-y-[100px]' : ''"
          >
          <div
            v-if="selectedCustomer || (isMobile && isCustomerContextChat)"
            class="wk-chat-customer-header relative shrink-0 border-b py-2"
            :class="isMobile ? 'px-3' : 'pl-4 pr-1 md:pl-8'"
          >
            <div class="mx-auto flex h-9 w-full items-center justify-between gap-2" :class="isMobile ? '' : 'pr-20'">
              <button
                v-if="isMobile"
                type="button"
                class="wk-mobile-customer-header-menu flex size-9 shrink-0 items-center justify-center rounded-full text-[#0d0d0d] transition-colors active:bg-[#f1f1f1]"
                aria-label="打开菜单"
                title="打开菜单"
                @click="openMobileMainMenu"
              >
                <span class="material-symbols-outlined text-[22px] leading-none">menu</span>
              </button>
              <div class="flex min-w-0 flex-1 items-center gap-2">
                <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
                  <img
                    v-if="selectedCustomer?.logoUrl"
                    :src="selectedCustomer.logoUrl"
                    :alt="mobileCustomerHeaderName || 'company logo'"
                    class="size-full bg-white object-contain"
                  />
                  <span v-else class="text-xs font-bold text-slate-400">
                    {{ mobileCustomerHeaderName.charAt(0) || '?' }}
                  </span>
                </div>
                <button
                  v-if="isMobile"
                  type="button"
                  class="min-w-[80px] max-w-[190px] truncate text-left text-[15px] font-semibold leading-5 text-[#0d0d0d]"
                  :title="mobileCustomerHeaderName"
                  @click="openSelectedCustomerBasicInfo"
                >
                  {{ mobileCustomerHeaderName }}
                </button>
                <h2 v-else class="min-w-[80px] max-w-[190px] truncate text-[15px] font-semibold leading-5 text-[#0d0d0d]">
                  {{ selectedCustomer?.companyName }}
                </h2>
                <div
                  v-if="selectedCustomer && !isMobile && (selectedCustomer.tags?.length || canEditSelectedCustomerTags)"
                  class="flex min-w-0 shrink items-center gap-1.5 overflow-hidden"
                >
                  <span
                    v-for="tag in selectedCustomerVisibleTags"
                    :key="tag.tagId"
                    class="group/tag inline-flex h-6 max-w-[88px] shrink-0 items-center gap-1 rounded-lg bg-[var(--wk-bg-surface-muted)] px-2 text-[11px] font-medium text-[var(--wk-text-secondary)]"
                    :title="tag.tagName"
                  >
                    <span class="min-w-0 truncate">{{ tag.tagName }}</span>
                    <button
                      v-if="canEditSelectedCustomerTags"
                      type="button"
                      class="hidden shrink-0 text-slate-400 transition-colors hover:text-red-500 group-hover/tag:inline-flex"
                      title="删除标签"
                      aria-label="删除标签"
                      @click.stop="handleRemoveSelectedCustomerTag(tag)"
                    >
                      <span class="material-symbols-outlined text-[12px] leading-none">close</span>
                    </button>
                  </span>
                  <el-popover
                    v-if="selectedCustomerHiddenTags.length > 0"
                    trigger="hover"
                    placement="bottom-start"
                    :width="220"
                    popper-class="wk-customer-tags-popover"
                  >
                    <template #reference>
                      <span
                        class="inline-flex h-6 shrink-0 cursor-default items-center rounded-lg bg-[var(--wk-bg-surface-muted)] px-2 text-[11px] font-medium text-[var(--wk-text-muted)]"
                      >
                        +{{ selectedCustomerHiddenTags.length }}
                      </span>
                    </template>
                    <div class="flex max-h-48 flex-wrap gap-1.5 overflow-y-auto">
                      <span
                        v-for="tag in selectedCustomerHiddenTags"
                        :key="tag.tagId"
                        class="group/tag inline-flex max-w-full items-center gap-1 rounded-lg bg-[#f4f4f4] px-2 py-1 text-[12px] font-medium text-[#5f5f5f]"
                        :title="tag.tagName"
                      >
                        <span class="min-w-0 truncate">{{ tag.tagName }}</span>
                        <button
                          v-if="canEditSelectedCustomerTags"
                          type="button"
                          class="inline-flex shrink-0 text-slate-400 transition-colors hover:text-red-500"
                          title="删除标签"
                          aria-label="删除标签"
                          @click.stop="handleRemoveSelectedCustomerTag(tag)"
                        >
                          <span class="material-symbols-outlined text-[12px] leading-none">close</span>
                        </button>
                      </span>
                    </div>
                  </el-popover>
                  <button
                    v-if="canEditSelectedCustomerTags"
                    type="button"
                    class="inline-flex size-6 shrink-0 items-center justify-center rounded-lg border border-dashed border-[var(--wk-border-muted)] text-[var(--wk-text-primary)] transition-colors hover:bg-[var(--wk-bg-surface-hover)]"
                    title="添加标签"
                    aria-label="添加标签"
                    @click.stop="showSelectedCustomerTagDialog = true"
                  >
                    <span class="material-symbols-outlined text-[15px] leading-none">add</span>
                  </button>
                </div>
              </div>
              <button
                v-if="isMobile"
                type="button"
                class="wk-mobile-customer-header-detail flex size-9 shrink-0 items-center justify-center rounded-full text-[#0d0d0d] transition-colors active:bg-[#f1f1f1]"
                aria-label="客户详情"
                title="客户详情"
                @click="openMobileCustomerSummary"
              >
                <span class="material-symbols-outlined text-[24px] leading-none">more_horiz</span>
              </button>
              <div v-else-if="selectedCustomer" class="flex shrink-0 items-center gap-2">
                <button
                  type="button"
                  class="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-[8px] bg-primary px-2.5 text-[12px] font-semibold text-white shadow-sm shadow-primary/20 transition-colors hover:bg-primary/90"
                  aria-label="基本信息"
                  @click="showSelectedCustomerBasicInfoDrawer = true"
                >
                  <span class="material-symbols-outlined text-[15px] leading-none">description</span>
                  <span>基本信息</span>
                </button>
                <el-dropdown trigger="click" @command="handleCustomerStageCommand">
                  <button
                    type="button"
                    class="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-[8px] px-2.5 text-[12px] font-semibold transition-colors"
                    :class="selectedCustomerStageButtonClass"
                  >
                    <span class="material-symbols-outlined text-[15px] leading-none">
                      {{ getCustomerStageIcon(selectedCustomer.stage) }}
                    </span>
                    <span>{{ selectedCustomerStageText }}</span>
                    <span class="material-symbols-outlined text-[15px] leading-none">expand_more</span>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="stage in CUSTOMER_STAGE_FLOW"
                        :key="stage.value"
                        :command="stage.value"
                      >
                        <span class="flex items-center gap-2">
                          <span class="material-symbols-outlined shrink-0 text-[16px] leading-none">
                            {{ getCustomerStageIcon(stage.value) }}
                          </span>
                          {{ stage.label }}
                        </span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <button
              v-if="showCustomerPanelShell && customerPanelVisible"
              type="button"
              class="group/sb-toggle absolute right-4 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#efefef] md:right-4"
              aria-label="收起客户侧栏"
              @click="customerPanelVisible = false"
            >
              <WkIcon name="fold" :size="18" class="shrink-0" />
              <span
                class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/sb-toggle:opacity-100"
                role="tooltip"
              >
                收起客户侧栏
              </span>
            </button>
          </div>

          <!-- Messages Area -->
          <div
            ref="messagesContainer"
            class="wk-chat-messages"
            :class="chatMessagesAreaClass"
            @scroll="handleMessagesScroll"
          >
            <div class="wk-chat-messages__inner px-4 md:px-8">
            <!-- Welcome Section (no messages) -->
            <template v-if="chatStore.messages.length === 0 && !selectedCustomer">
              <div class="mx-auto flex max-w-3xl flex-col items-center space-y-5 py-6 text-center">
                <!-- <div class="size-16 bg-primary/5 rounded-2xl flex items-center justify-center text-primary mb-2 border border-primary/10">
                  <WkIcon name="ai" class="text-4xl" />
                </div> -->
                <div class="flex flex-col items-center gap-4 sm:flex-row">
                  <div
                    v-if="enterpriseStore.hasLogo"
                    class="size-10 shrink-0 overflow-hidden rounded-xl bg-transparent"
                  >
                    <img :src="enterpriseStore.logoUrl!" class="h-full w-full object-cover" alt="logo" />
                  </div>
                  <div v-else class="size-10 shrink-0 overflow-hidden rounded-xl bg-transparent">
                    <img :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
                  </div>
                  <h1 class="text-[28px] font-semibold leading-tight tracking-normal text-[#0d0d0d] md:text-[32px]">
                    今天有什么可以帮您的？
                  </h1>
                </div>
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
                class="wk-chat-message mx-auto message-enter"
                :class="[
                  message.role === 'user' ? 'wk-chat-message--user' : 'wk-chat-message--assistant'
                ]"
              >
                <div
                  v-if="getDocumentAttachments(message).length > 0"
                  class="mb-2 flex"
                  :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
                >
                  <div
                    class="flex max-w-[80%] flex-wrap gap-2"
                    :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
                  >
                    <a
                      v-for="att in getDocumentAttachments(message)"
                      :key="att.id || att.fileName"
                      :href="att.accessUrl"
                      target="_blank"
                      class="group flex max-w-xs items-center gap-3 rounded-2xl border border-[#e5e5e5] bg-white px-3 py-2.5 transition-colors hover:bg-[#f7f7f7]"
                    >
                      <div class="flex size-9 shrink-0 items-center justify-center rounded-xl bg-[#f4f4f4] text-[#5d5d5d]">
                        <span class="material-symbols-outlined text-[20px] leading-none">description</span>
                      </div>
                      <div class="min-w-0 flex-1">
                        <div class="truncate text-sm font-medium text-[#0d0d0d]">{{ att.fileName }}</div>
                        <div class="mt-0.5 text-xs text-[#8f8f8f]">{{ formatFileSize(att.fileSize) }}</div>
                      </div>
                      <span class="material-symbols-outlined text-[18px] leading-none text-[#b4b4b4] transition-colors group-hover:text-[#0d0d0d]">open_in_new</span>
                    </a>
                  </div>
                </div>

                <!-- AI Message -->
                <div v-if="message.role !== 'user'" class="group flex w-full gap-3 pb-4 md:gap-4 md:pb-8">
                  <div v-if="false" class="size-9 rounded-xl bg-primary flex items-center justify-center text-white shrink-0 shadow-lg shadow-primary/20">
                    <WkIcon name="ai" class="text-lg" />
                  </div>
                  <div class="min-w-0 flex-1 space-y-3">
                    <div class="max-w-full text-left text-[16px] leading-7 text-[#0d0d0d]">
                      <div
                        class="wk-markdown"
                        :class="{ 'wk-thinking-shimmer': message.isThinking && message.content.length === 0 }"
                        v-html="renderAssistantMessage(message.content, message.isStreaming, message.isThinking)"
                      />
                      <div v-if="message.isThinking && message.content.length > 0" class="wk-thinking-shimmer mt-1 text-sm">
                        <p>思考中</p>
                      </div>
                    </div>
                    <!-- Attachments -->
                    <div v-if="getInlineAttachments(message).length > 0" class="space-y-2">
                      <div v-for="att in getInlineAttachments(message)" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="max-h-[220px] rounded-2xl border border-[#e5e5e5]"
                            :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                            lazy
                          />
                          <div class="mt-1 text-xs text-[#8f8f8f]">{{ att.fileName }}</div>
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex max-w-xs items-center gap-3 rounded-2xl border border-[#e5e5e5] bg-white p-3 transition-colors hover:bg-[#f7f7f7]"
                          >
                            <span class="material-symbols-outlined text-[#8f8f8f]">description</span>
                            <div class="min-w-0 flex-1">
                              <div class="truncate text-sm text-[#0d0d0d]">{{ att.fileName }}</div>
                              <div class="text-xs text-[#8f8f8f]">{{ formatFileSize(att.fileSize) }}</div>
                            </div>
                          </a>
                        </template>
                      </div>
                    </div>
                    <div v-if="!message.isStreaming" class="flex h-8 items-center">
                      <button
                        type="button"
                        class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-all hover:bg-[#f1f1f1] hover:text-[#0d0d0d]"
                        aria-label="复制内容"
                        @click="copyMessageContent(message, 'assistant')"
                      >
                        <WkIcon name="copy" :box-size="18" class="shrink-0 material-symbols-outlined leading-none" title="复制内容" />
                      </button>
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
                <div v-else class="group flex w-full flex-wrap flex-row-reverse pb-0">
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
                  <div class="min-w-[50px] space-y-3" :class="isMobile ? 'max-w-[88%]' : 'max-w-[72%]'">
                    <!-- <div class="bg-primary text-white rounded-2xl rounded-tr-none p-4 shadow-lg shadow-primary/10 text-sm leading-relaxed"> -->
                    <div class="rounded-[24px] bg-[#f4f4f4] px-4 py-2.5 text-[15px] leading-7 text-[#0d0d0d]">
                      <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                    </div>
                    <!-- User Attachments -->
                    <div v-if="getInlineAttachments(message).length > 0" class="flex flex-col items-end gap-2">
                      <div v-for="att in getInlineAttachments(message)" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="max-h-[220px] rounded-2xl border border-[#e5e5e5]"
                            :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                            lazy
                          />
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex max-w-xs items-center gap-3 rounded-2xl border border-[#e5e5e5] bg-white p-3 transition-colors hover:bg-[#f7f7f7]"
                          >
                            <span class="material-symbols-outlined text-[#8f8f8f]">description</span>
                            <div class="min-w-0 flex-1">
                              <div class="truncate text-sm text-[#0d0d0d]">{{ att.fileName }}</div>
                              <div class="text-xs text-[#8f8f8f]">{{ formatFileSize(att.fileSize) }}</div>
                            </div>
                          </a>
                        </template>
                      </div>
                    </div>
                    <!-- <div class="text-xs text-slate-400 font-medium text-right">{{ formatTime(message.timestamp) }}</div> -->
                  </div>
                  <div
                    class="z-10 flex h-8 w-full basis-full items-center justify-end"
                  >
                    <button
                      type="button"
                      class="pointer-events-none flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] opacity-0 transition-all hover:bg-[#f1f1f1] hover:text-[#0d0d0d] group-hover:pointer-events-auto group-hover:opacity-100"
                      aria-label="复制内容"
                      @click="copyMessageContent(message, 'user')"
                    >
                      <WkIcon name="copy" :box-size="18" class="shrink-0 material-symbols-outlined leading-none" title="复制内容" />
                    </button>
                  </div>
                </div>
              </div>
            </template>
            </div>
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
            class="wk-chat-composer-wrap shrink-0 pb-2 md:pb-2 px-2"
            :class="isCenteredEmptyChat ? 'bg-transparent pt-0' : ''"
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
              <div
                class="relative group min-w-0"
                :class="isMobile ? '' : 'w-[768px] max-w-full mx-auto'"
                :style="chatComposerShellStyle"
              >
                <!-- <div class="absolute inset-0 bg-primary/5 blur-xl rounded-2xl group-focus-within:bg-primary/10 transition-all opacity-0 group-focus-within:opacity-100"></div> -->
                <div class="absolute inset-0 rounded-2xl opacity-0 transition-all"></div>
                <!-- focus-within:border-primary -->
                <div
                  class="wk-chat-composer relative flex rounded-2xl p-2 transition-all"
                  :class="isMobile ? 'flex-col items-stretch gap-2' : 'min-w-0 items-center rounded-[28px] p-[6px]'"
                  @mousedown="handleInputBoxMouseDown"
                >
                  <div class="w-full min-w-0">
                  <input
                    ref="fileInputRef"
                    type="file"
                    multiple
                    :accept="CHAT_ATTACHMENT_ACCEPT"
                    class="hidden"
                    @change="handleFileSelect"
                  />

                  <div
                    v-if="composerAttachmentPreviewItems.length > 0"
                    class="relative min-w-0 px-2 pt-2 mb-2"
                  >
                    <div
                      v-if="composerAttachmentShowScrollArrows && composerAttachmentCanScrollLeft"
                      class="pointer-events-none absolute left-0 top-2 bottom-2 z-[1] w-12 rounded-l-xl bg-gradient-to-r from-white to-transparent h-full"
                      aria-hidden="true"
                    />
                    <div
                      v-if="composerAttachmentShowScrollArrows && composerAttachmentCanScrollRight"
                      class="pointer-events-none absolute right-0 top-2 bottom-2 z-[1] w-12 rounded-r-xl bg-gradient-to-l from-white to-transparent h-full"
                      aria-hidden="true"
                    />
                    <button
                      v-if="composerAttachmentShowScrollArrows && composerAttachmentCanScrollLeft"
                      type="button"
                      class="absolute left-3 top-1/2 z-[2] flex size-7 -translate-y-1/2 items-center justify-center rounded-full border border-black/[0.06] bg-[#f0f0f0] text-[#0d0d0d] shadow-sm transition-colors hover:bg-[#e6e6e6]"
                      aria-label="向左查看附件"
                      @click="scrollComposerAttachmentStep(-1)"
                    >
                      <span class="material-symbols-outlined text-[20px] leading-none">chevron_left</span>
                    </button>
                    <button
                      v-if="composerAttachmentShowScrollArrows && composerAttachmentCanScrollRight"
                      type="button"
                      class="absolute right-3 top-1/2 z-[2] flex size-7 -translate-y-1/2 items-center justify-center rounded-full border border-black/[0.06] bg-[#f0f0f0] text-[#0d0d0d] shadow-sm transition-colors hover:bg-[#e6e6e6]"
                      aria-label="向右查看附件"
                      @click="scrollComposerAttachmentStep(1)"
                    >
                      <span class="material-symbols-outlined text-[20px] leading-none">chevron_right</span>
                    </button>
                    <div
                      ref="composerAttachmentScrollRef"
                      class="flex min-h-[54px] min-w-0 w-full flex-nowrap gap-2 overflow-x-auto overflow-y-hidden scroll-smooth"
                      :class="composerAttachmentShowScrollArrows ? '' : ''"
                      @scroll.passive="updateComposerAttachmentScrollState"
                    >
                    <template v-for="item in composerAttachmentPreviewItems" :key="item.key">
                      <div
                        v-if="item.kind === 'knowledge'"
                        class="relative flex h-[54px] min-w-[200px] max-w-[320px] shrink-0 items-center gap-3 overflow-hidden rounded-2xl bg-[#f5f5f5] pl-3 pr-[30px]"
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
                        class="relative flex h-[54px] min-w-[200px] max-w-[320px] shrink-0 items-center gap-3 overflow-hidden rounded-2xl bg-[#f5f5f5] pl-3 pr-[30px]"
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
                  <div v-if="!isMobile" class="w-full min-w-0">
                    <textarea
                      ref="pcChatInputRef"
                      v-model="inputText"
                      rows="1"
                      class="w-full bg-transparent border-none focus:ring-0 focus:outline-none px-3 pt-3 text-[#0d0d0d] text-[16px] leading-[26px] placeholder:text-[#909090] placeholder:text-[16px] resize-none font-weight: overflow-x-hidden overflow-y-auto min-h-[50px]"
                      :placeholder="chatInputPlaceholder"
                      :disabled="isUploading"
                      style="min-height: 90px;"
                      @input="resizeChatTextarea"
                      @keydown.enter.exact.prevent="handleSend"
                      @paste="handlePaste"
                    />
                  </div>

                  <!-- PC: controls (3rd line) -->
                  <div v-if="!isMobile" class="flex min-w-0 items-center justify-between w-full px-1 pb-1 select-none mt-1">
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
                        <div
                          class="wk-chat-upload-menu"
                          @mouseenter="clearChatUploadMenuLeaveTimer"
                          @mouseleave="handleChatUploadMenuMouseLeave"
                        >
                          <button
                            type="button"
                            class="wk-chat-upload-menu__item"
                            :disabled="isUploading"
                            @click="handleChatUploadMenuAddFile"
                          >
                            <WkIcon name="file" :box-size="18" class="shrink-0" />
                            <!-- <span class="wk-chat-upload-menu__icon material-symbols-outlined">attach_file</span> -->
                            <span class="wk-chat-upload-menu__label">上传图片和文件</span>
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
                            v-model:visible="chatUploadSubmenuVisible"
                            trigger="hover"
                            placement="right-end"
                            :show-arrow="false"
                            :disabled="isUploading"
                            :teleported="false"
                            :offset="8"
                            :hide-after="220"
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
                                <span class="wk-chat-upload-menu__label">悟空技能</span>
                                <span class="wk-chat-upload-menu__chevron material-symbols-outlined">chevron_right</span>
                              </div>
                            </template>
                            <div
                              class="wk-chat-upload-submenu"
                              @mouseenter="clearChatUploadMenuLeaveTimer"
                              @mouseleave="handleChatUploadMenuMouseLeave"
                            >
                              <button
                                type="button"
                                class="wk-chat-upload-menu__item wk-chat-upload-submenu__btn"
                                @click="handleChatUploadMenuSelectApp('crm')"
                              >
                                <WkIcon
                                  name="customer"
                                  :size="18"
                                  class="shrink-0"
                                  :class="chatStore.selectedAppCode === 'crm' ? '!text-[var(--wk-text-primary)]' : ''"
                                />
                                <span class="wk-chat-upload-menu__label wk-chat-upload-submenu__label" :class="chatStore.selectedAppCode === 'crm' ? 'text-[var(--wk-text-primary)]' : 'text-[#0d0d0d]'">CRM管理</span>
                                <span
                                  v-if="chatStore.selectedAppCode === 'crm'"
                                  class="wk-chat-upload-menu__check material-symbols-outlined fill-1 text-primary"
                                >check</span>
                              </button>
                              <button
                                type="button"
                                class="wk-chat-upload-menu__item wk-chat-upload-submenu__btn"
                                @click="handleChatUploadMenuSelectApp('knowledge')"
                              >
                                <WkIcon
                                  name="knowledge-1"
                                  :size="18"
                                  class="shrink-0"
                                  :class="chatStore.selectedAppCode === 'knowledge' ? '!text-[var(--wk-text-primary)]' : ''"
                                />
                                <span class="wk-chat-upload-menu__label wk-chat-upload-submenu__label" :class="chatStore.selectedAppCode === 'knowledge' ? 'text-[var(--wk-text-primary)]' : 'text-[#0d0d0d]'">知识库检索</span>
                                <span
                                  v-if="chatStore.selectedAppCode === 'knowledge'"
                                  class="wk-chat-upload-menu__check material-symbols-outlined fill-1 text-primary"
                                >check</span>
                              </button>
                            </div>
                          </el-popover>
                        </div>
                      </el-popover>
                      <button
                        v-if="chatStore.selectedAppCode !== 'general'"
                        type="button"
                        class="group/crm-toolbar h-[36px] rounded-full pl-1 pr-3.5 text-sm text-[var(--wk-text-primary)] transition-all hover:bg-[var(--wk-bg-surface-hover)]"
                        aria-pressed="true"
                        :title="`已启用 ${selectedChatAppLabel}，点击关闭`"
                        @click="chatStore.setSelectedAppCode('general')"
                      >
                        <span class="flex items-center gap-1.5">
                          <span class="relative flex size-[22px] shrink-0 items-center justify-center">
                            <span
                              class="flex size-full items-center justify-center transition-opacity duration-150 max-sm:pointer-events-none max-sm:opacity-0 group-hover/crm-toolbar:pointer-events-none group-hover/crm-toolbar:opacity-0"
                            >
                              <WkIcon :name="selectedChatAppIcon" :size="18" class="shrink-0" />
                            </span>
                            <span
                              class="pointer-events-none absolute inset-0 flex items-center justify-center rounded-full bg-[var(--wk-bg-surface-active)] text-[var(--wk-text-primary)] opacity-0 transition-opacity duration-150 max-sm:opacity-100 group-hover/crm-toolbar:opacity-100"
                              aria-hidden="true"
                            >
                              <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                            </span>
                          </span>
                          <span>{{ selectedChatAppLabel }}</span>
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

                    <div class="flex items-center gap-2 pr-1 shrink-0">
                      <el-popover
                        v-model:visible="chatModelPopoverVisible"
                        trigger="click"
                        placement="top-end"
                        width="340"
                        :show-arrow="false"
                        :teleported="true"
                        :disabled="chatModelPickerDisabled || showAiQuotaEmptyCta"
                        transition="el-zoom-in-bottom"
                        popper-class="wk-chat-model-popper"
                      >
                        <template #reference>
                          <button
                            type="button"
                            class="inline-flex h-9 max-w-[260px] shrink-0 items-center gap-1.5 rounded-[18px] border pl-2 pr-2 text-left text-[13px] transition-colors disabled:cursor-not-allowed disabled:opacity-50"
                            :class="chatModelTriggerClass"
                            :disabled="chatModelPickerDisabled"
                            :title="`当前模型：${chatComposerModelLabel}`"
                            @click="handleChatModelTriggerClick"
                          >
                            <span
                              class="relative flex h-[20px] w-[20px] shrink-0 items-center justify-center overflow-hidden rounded-md"
                              :class="showAiQuotaEmptyCta ? 'bg-red-50' : 'bg-[#ececec]'"
                              aria-hidden="true"
                            >
                              <span
                                v-if="showAiQuotaEmptyCta"
                                class="material-symbols-outlined text-[16px] leading-none text-[#8d4f34]"
                              >bolt</span>
                              <template v-else-if="chatStore.selectedModel">
                                <img
                                  v-if="chatModelShowImage(chatStore.selectedModel)"
                                  :src="chatModelIconSrc(chatStore.selectedModel)"
                                  alt=""
                                  class="size-full object-fill"
                                  @error="onChatModelImageError($event)"
                                />
                                <span
                                  v-else
                                  class="flex size-full items-center justify-center text-[11px] font-semibold text-[#909090]"
                                >
                                  {{ modelOptionLabel(chatStore.selectedModel).slice(0, 1) }}
                                </span>
                              </template>
                              <span
                                v-else
                                class="flex size-full items-center justify-center text-[11px] font-semibold text-[#909090]"
                              >
                                ?
                              </span>
                            </span>
                            <span class="min-w-0 flex-1 truncate">{{ chatComposerModelLabel }}</span>
                            <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-[#8f8f8f]">expand_more</span>
                          </button>
                        </template>
                        <div class="wk-chat-model-menu">
                          <template v-for="group in chatModelOptionGroups" :key="group.source">
                            <div
                              v-if="chatModelOptionGroups.length > 1"
                              class="wk-chat-model-menu__group-label"
                            >
                              {{ group.label }}
                            </div>
                            <button
                              v-for="option in group.options"
                              :key="chatStore.toModelKey(option)"
                              type="button"
                              class="wk-chat-model-menu__item"
                              @click="handleChatModelChange(chatStore.toModelKey(option))"
                            >
                              <span
                                class="relative flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-lg"
                                aria-hidden="true"
                              >
                                <img
                                  v-if="chatModelShowImage(option)"
                                  :src="chatModelIconSrc(option)"
                                  alt=""
                                  class="size-5 object-fill"
                                  @error="onChatModelImageError($event)"
                                />
                                <span
                                  v-else
                                  class="flex size-full items-center justify-center text-[12px] font-semibold text-[#909090]"
                                >
                                  {{ modelOptionLabel(option).slice(0, 1) }}
                                </span>
                              </span>
                              <div class="min-w-0 flex-1">
                                <div class="flex items-center justify-start gap-2">
                                  <div class="min-w-0 text-[14px] leading-tight text-[#0d0d0d]">
                                    {{ modelOptionLabel(option) }}
                                  </div>
                                  <span
                                    v-if="shouldShowModelMultiplier(option)"
                                    class="shrink-0 text-xs text-slate-400"
                                  >
                                    {{ formatModelMultiplier(option.creditMultiplier) }}
                                  </span>
                                </div>
                                <!-- <div class="mt-0.5 truncate text-[12px] leading-snug text-[#909090]">
                                  {{ option.providerLabel || option.provider }}
                                </div> -->
                              </div>
                              <span
                                class="material-symbols-outlined flex size-5 shrink-0 items-center justify-center text-[20px] leading-none"
                                :class="chatStore.selectedModelKey === chatStore.toModelKey(option) ? 'text-primary' : 'invisible'"
                                aria-hidden="true"
                              >
                                check
                              </span>
                            </button>
                            <button
                              v-if="group.source === 'custom' && canManageAiConfig"
                              type="button"
                              class="wk-chat-model-menu__more"
                              @click="handleOpenMoreModels"
                            >
                              <span class="material-symbols-outlined text-[18px] leading-none" aria-hidden="true">
                                add_circle
                              </span>
                              <span class="min-w-0 flex-1 truncate">更多模型</span>
                              <span class="material-symbols-outlined text-[18px] leading-none text-[#8f8f8f]" aria-hidden="true">
                                chevron_right
                              </span>
                            </button>
                          </template>
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
                        <span
                          v-if="chatStore.currentSessionIsStreaming"
                          class="inline-block size-[10px] rounded-[2px] shrink-0 bg-[#000]"
                          aria-hidden="true"
                        />
                        <span v-else-if="isUploading" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                        <span v-else-if="isTranscribing" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                        <span v-else-if="isRecording" class="wk-recording-indicator" aria-hidden="true">
                          <span class="material-symbols-outlined wk-recording-indicator__stop">stop</span>
                          <span class="wk-recording-indicator__bars">
                            <span />
                            <span />
                            <span />
                            <span />
                          </span>
                        </span>
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

                  <!-- Mobile: controls + input in one row -->
                  <div v-else class="wk-mobile-composer-row flex w-full items-end gap-2">
                    <el-popover
                      v-model:visible="chatUploadMenuVisible"
                      trigger="click"
                      placement="top-start"
                      width="calc(100vw - 32px)"
                      :show-arrow="false"
                      :disabled="isUploading"
                      :teleported="true"
                      transition="wk-chat-mobile-sheet"
                      popper-class="wk-chat-upload-menu-popper wk-chat-upload-menu-popper--mobile"
                    >
                      <template #reference>
                        <button
                          type="button"
                          class="wk-mobile-composer-icon-button"
                          :disabled="isUploading"
                          aria-label="添加文件等"
                        >
                          <WkIcon name="add-1" :box-size="16" class="shrink-0" />
                        </button>
                      </template>
                      <div
                        class="wk-chat-upload-menu wk-chat-upload-menu--mobile"
                        @mouseenter="clearChatUploadMenuLeaveTimer"
                        @mouseleave="handleChatUploadMenuMouseLeave"
                      >
                        <button
                          type="button"
                          class="wk-chat-upload-menu__item"
                          :disabled="isUploading"
                          @click="handleChatUploadMenuAddFile"
                        >
                          <span class="wk-chat-upload-menu__icon material-symbols-outlined">attach_file</span>
                          <span class="wk-chat-upload-menu__label">上传图片和文件</span>
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
                        <button
                          type="button"
                          class="wk-chat-upload-menu__item"
                          :disabled="isUploading"
                          @click="handleChatUploadMenuSelectApp('crm')"
                        >
                          <WkIcon
                            name="customer"
                            :size="18"
                            class="shrink-0"
                            :class="chatStore.selectedAppCode === 'crm' ? '!text-[var(--wk-text-primary)]' : ''"
                          />
                          <span class="wk-chat-upload-menu__label wk-chat-upload-menu__label--grow" :class="chatStore.selectedAppCode === 'crm' ? 'text-[var(--wk-text-primary)]' : 'text-[#0d0d0d]'">CRM管理</span>
                          <span
                            v-if="chatStore.selectedAppCode === 'crm'"
                            class="wk-chat-upload-menu__check material-symbols-outlined fill-1 text-primary"
                          >check</span>
                        </button>
                        <button
                          type="button"
                          class="wk-chat-upload-menu__item"
                          :disabled="isUploading"
                          @click="handleChatUploadMenuSelectApp('knowledge')"
                        >
                          <WkIcon
                            name="knowledge-1"
                            :size="18"
                            class="shrink-0"
                            :class="chatStore.selectedAppCode === 'knowledge' ? '!text-[var(--wk-text-primary)]' : ''"
                          />
                          <span class="wk-chat-upload-menu__label wk-chat-upload-menu__label--grow" :class="chatStore.selectedAppCode === 'knowledge' ? 'text-[var(--wk-text-primary)]' : 'text-[#0d0d0d]'">知识库检索</span>
                          <span
                            v-if="chatStore.selectedAppCode === 'knowledge'"
                            class="wk-chat-upload-menu__check material-symbols-outlined fill-1 text-primary"
                          >check</span>
                        </button>
                      </div>
                    </el-popover>

                    <textarea
                      ref="mobileChatInputRef"
                      v-model="inputText"
                      rows="1"
                      class="wk-mobile-composer-textarea min-w-0 flex-1 bg-transparent border-none focus:ring-0 focus:outline-none px-1 py-2 text-[#0d0d0d] text-[16px] leading-[24px] placeholder:text-[#0d0d0d] placeholder:text-[16px] resize-none overflow-x-hidden overflow-y-auto"
                      :placeholder="chatInputPlaceholder"
                      :disabled="isUploading"
                      @input="resizeChatTextarea"
                      @keydown.enter.exact.prevent="handleSend"
                      @paste="handlePaste"
                    />

                    <el-popover
                      v-model:visible="chatModelPopoverVisible"
                      trigger="click"
                      placement="top-start"
                      :width="300"
                      :show-arrow="false"
                      :teleported="true"
                      :disabled="chatModelPickerDisabled || showAiQuotaEmptyCta"
                      transition="el-zoom-in-bottom"
                      popper-class="wk-chat-model-popper wk-chat-model-popper--mobile"
                    >
                      <template #reference>
                        <button
                          type="button"
                          class="wk-mobile-composer-icon-button wk-mobile-model-trigger--icon"
                          :class="chatModelTriggerClass"
                          :disabled="chatModelPickerDisabled"
                          :title="`当前模型：${chatComposerModelLabel}`"
                          aria-label="切换模型"
                          @click="handleChatModelTriggerClick"
                        >
                          <span
                            class="relative flex size-[22px] shrink-0 items-center justify-center overflow-hidden rounded-md"
                            :class="showAiQuotaEmptyCta ? 'bg-red-50' : 'bg-[#ececec]'"
                            aria-hidden="true"
                          >
                            <span
                              v-if="showAiQuotaEmptyCta"
                              class="material-symbols-outlined text-[17px] leading-none text-[#8d4f34]"
                            >bolt</span>
                            <template v-else-if="chatStore.selectedModel">
                              <img
                                v-if="chatModelShowImage(chatStore.selectedModel)"
                                :src="chatModelIconSrc(chatStore.selectedModel)"
                                alt=""
                                class="size-full object-fill"
                                @error="onChatModelImageError($event)"
                              />
                              <span
                                v-else
                                class="flex size-full items-center justify-center text-[11px] font-semibold text-[#909090]"
                              >
                                {{ modelOptionLabel(chatStore.selectedModel).slice(0, 1) }}
                              </span>
                            </template>
                            <span
                              v-else
                              class="flex size-full items-center justify-center text-[11px] font-semibold text-[#909090]"
                            >
                              ?
                            </span>
                          </span>
                        </button>
                      </template>
                      <div class="wk-chat-model-menu">
                        <template v-for="group in chatModelOptionGroups" :key="group.source">
                          <div
                            v-if="chatModelOptionGroups.length > 1"
                            class="wk-chat-model-menu__group-label"
                          >
                            {{ group.label }}
                          </div>
                          <button
                            v-for="option in group.options"
                            :key="chatStore.toModelKey(option)"
                            type="button"
                            class="wk-chat-model-menu__item"
                            @click="handleChatModelChange(chatStore.toModelKey(option))"
                          >
                            <span
                              class="relative flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-lg"
                              aria-hidden="true"
                            >
                              <img
                                v-if="chatModelShowImage(option)"
                                :src="chatModelIconSrc(option)"
                                alt=""
                                class="size-5 object-fill"
                                @error="onChatModelImageError($event)"
                              />
                              <span
                                v-else
                                class="flex size-full items-center justify-center text-[12px] font-semibold text-[#909090]"
                              >
                                {{ modelOptionLabel(option).slice(0, 1) }}
                              </span>
                            </span>
                            <div class="min-w-0 flex-1">
                              <div class="flex items-center justify-start gap-2">
                                <div class="min-w-0 text-[14px] leading-tight text-[#0d0d0d]">
                                  {{ modelOptionLabel(option) }}
                                </div>
                                <span
                                  v-if="shouldShowModelMultiplier(option)"
                                  class="shrink-0 text-xs text-slate-400"
                                >
                                  {{ formatModelMultiplier(option.creditMultiplier) }}
                                </span>
                              </div>
                            </div>
                            <span
                              class="material-symbols-outlined flex size-5 shrink-0 items-center justify-center text-[20px] leading-none"
                              :class="chatStore.selectedModelKey === chatStore.toModelKey(option) ? 'text-primary' : 'invisible'"
                              aria-hidden="true"
                            >
                              check
                            </span>
                          </button>
                          <button
                            v-if="group.source === 'custom' && canManageAiConfig"
                            type="button"
                            class="wk-chat-model-menu__more"
                            @click="handleOpenMoreModels"
                          >
                            <span class="material-symbols-outlined text-[18px] leading-none" aria-hidden="true">
                              add_circle
                            </span>
                            <span class="min-w-0 flex-1 truncate">更多模型</span>
                            <span class="material-symbols-outlined text-[18px] leading-none text-[#8f8f8f]" aria-hidden="true">
                              chevron_right
                            </span>
                          </button>
                        </template>
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
                      <span
                        v-if="chatStore.currentSessionIsStreaming"
                        class="inline-block size-[10px] rounded-[2px] shrink-0 bg-[#000]"
                        aria-hidden="true"
                      />
                      <span v-else-if="isUploading" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                      <span v-else-if="isTranscribing" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                      <span v-else-if="isRecording" class="wk-recording-indicator" aria-hidden="true">
                        <span class="material-symbols-outlined wk-recording-indicator__stop">stop</span>
                        <span class="wk-recording-indicator__bars">
                          <span />
                          <span />
                          <span />
                          <span />
                        </span>
                      </span>
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

                  <div v-if="isMobile && chatStore.selectedAppCode !== 'general'" class="wk-mobile-selected-app-row flex min-w-0 items-center justify-start">
                    <button
                      type="button"
                      class="group/crm-toolbar inline-flex h-[34px] max-w-full rounded-xl pr-3.5 text-sm text-[var(--wk-text-primary)] transition-all hover:bg-[var(--wk-bg-surface-hover)]"
                      aria-pressed="true"
                      :title="`已启用 ${selectedChatAppLabel}，点击关闭`"
                      @click="chatStore.setSelectedAppCode('general')"
                    >
                      <span class="flex min-w-0 items-center justify-center gap-1.5">
                        <span class="relative flex size-[22px] shrink-0 items-center justify-center">
                          <span
                            class="flex size-full items-center justify-center transition-opacity duration-150 max-sm:pointer-events-none max-sm:opacity-0 group-hover/crm-toolbar:pointer-events-none group-hover/crm-toolbar:opacity-0"
                          >
                            <WkIcon :name="selectedChatAppIcon" :size="18" class="shrink-0" />
                          </span>
                          <span
                            class="pointer-events-none absolute inset-0 flex items-center justify-center rounded-full bg-[var(--wk-bg-surface-active)] text-[var(--wk-text-primary)] opacity-0 transition-opacity duration-150 max-sm:opacity-100 group-hover/crm-toolbar:opacity-100"
                            aria-hidden="true"
                          >
                            <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                          </span>
                        </span>
                        <span class="min-w-0 truncate">{{ selectedChatAppLabel }}</span>
                      </span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Quick Action Chips -->
              <!-- <div v-if="chatStore.messages.length === 0" class="mt-6 min-h-[88px]">
                <div v-if="chatStore.selectedAppCode === 'crm'" class="flex flex-wrap gap-2 justify-center" >
                  <button
                    v-for="action in quickActions"
                    :key="action.label"
                    class="px-4 py-[9px] bg-white border border-slate-200 rounded-full text-[14px] text-[#5d5d5d] transition-all shadow-sm hover:bg-[#F1F1F1] hover:border-[#E0E0E0] hover:text-[#0d0d0d]"
                    @click="sendQuickMessage(action.text)"
                  >
                    {{ action.label }}
                  </button>
                </div>
              </div> -->

              <p v-if="chatStore.messages.length > 0" class="text-center text-xs text-[#5d5d5d] uppercase !mt-[10px]">内容由AI生成，请核查重要信息</p>
              <div v-else class="h-[16px] !mt-[10px]"></div>
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

    <el-dialog
      v-model="showSelectedCustomerTagDialog"
      title="添加标签"
      width="400px"
      class="wk-dialog--flush"
    >
      <el-input
        v-model="newSelectedCustomerTagName"
        placeholder="请输入标签名称"
        maxlength="20"
        show-word-limit
        @keyup.enter="handleAddSelectedCustomerTag"
      />
      <template #footer>
        <el-button @click="showSelectedCustomerTagDialog = false">取消</el-button>
        <el-button type="primary" :loading="selectedCustomerTagSubmitting" @click="handleAddSelectedCustomerTag">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showAiQuotaChoiceDialog"
      :width="isMobile ? '92%' : '560px'"
      class="wk-ai-quota-choice-dialog wk-dialog--flush"
      align-center
    >
      <div class="px-1 pb-6 pt-1">
        <div class="mb-5 flex items-start gap-3">
          <span class="mt-0.5 inline-flex size-10 shrink-0 items-center justify-center rounded-2xl bg-[#f4f1ed] text-[#8d4f34]">
            <span class="material-symbols-outlined text-[22px] leading-none">bolt</span>
          </span>
          <div class="min-w-0">
            <h3 class="text-[20px] font-black leading-tight text-[#171717]">免费体验额度已用完</h3>
            <p class="mt-2 text-sm leading-6 text-slate-500">
              继续使用 AI CRM，可选择购买套餐，或接入自己的大模型 API。
            </p>
          </div>
        </div>

        <div class="space-y-3">
          <div
            role="button"
            tabindex="0"
            class="group flex w-full cursor-pointer items-center gap-4 rounded-2xl border border-[#ead8cc] bg-[#fbf7f4] p-4 text-left transition-all hover:border-[#dec4b5] hover:bg-[#f7eee8] hover:shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-[#ead8cc] focus-visible:ring-offset-2"
            @click="handleAiQuotaChoicePurchase"
            @keydown.enter.prevent="handleAiQuotaChoicePurchase"
            @keydown.space.prevent="handleAiQuotaChoicePurchase"
          >
            <span class="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-[#f4f1ed] text-[#8d4f34] shadow-sm shadow-slate-100">
              <span class="material-symbols-outlined text-[22px] leading-none">payments</span>
            </span>
            <span class="min-w-0 flex-1">
              <span class="flex min-w-0 flex-wrap items-center gap-2">
                <span class="text-sm font-black text-slate-900">购买套餐</span>
                <span class="rounded-full bg-[#f4e9e2] px-2 py-0.5 text-[11px] font-bold text-[#8d4f34]">推荐</span>
              </span>
              <span class="mt-1 block text-xs leading-5 text-slate-500">购买 AI 算力包，恢复全部模型与 AI 功能。</span>
            </span>
            <button
              type="button"
              class="inline-flex h-9 shrink-0 items-center rounded-xl bg-[#8d4f34] px-3 text-xs font-bold text-white transition-colors hover:bg-[#7a432c]"
              @click.stop="handleAiQuotaChoicePurchase"
            >
              去购买
            </button>
          </div>

          <div
            role="button"
            tabindex="0"
            class="group flex w-full cursor-pointer items-center gap-4 rounded-2xl border border-[#dce5d2] bg-[#f7faf4] p-4 text-left transition-all hover:border-[#cddabc] hover:bg-[#eef4e8] hover:shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-[#dce5d2] focus-visible:ring-offset-2"
            @click="handleAiQuotaChoiceConfigure"
            @keydown.enter.prevent="handleAiQuotaChoiceConfigure"
            @keydown.space.prevent="handleAiQuotaChoiceConfigure"
          >
            <span class="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-[#eef1ea] text-[#5f704a] shadow-sm shadow-slate-100">
              <span class="material-symbols-outlined text-[22px] leading-none">construction</span>
            </span>
            <span class="min-w-0 flex-1">
              <span class="flex min-w-0 flex-wrap items-center gap-2">
                <span class="text-sm font-black text-slate-900">配置自定义大模型</span>
                <span class="rounded-full bg-[#e8efdf] px-2 py-0.5 text-[11px] font-bold text-[#5f704a]">更灵活</span>
              </span>
              <span class="mt-1 block text-xs leading-5 text-slate-500">支持 OpenAI、DeepSeek、Qwen、Kimi 等兼容 API。</span>
            </span>
            <button
              type="button"
              class="inline-flex h-9 shrink-0 items-center rounded-xl bg-[#5f704a] px-3 text-xs font-bold text-white transition-colors hover:bg-[#536241]"
              @click.stop="handleAiQuotaChoiceConfigure"
            >
              去配置
            </button>
          </div>
        </div>
      </div>
    </el-dialog>

    <CustomerBasicInfoDrawer
      v-model="showSelectedCustomerBasicInfoDrawer"
      :customer="selectedCustomer"
      :contacts="selectedCustomerContacts"
      :custom-fields="[]"
      @contacts-updated="handleSelectedCustomerContactsUpdated"
      @edit="handleSelectedCustomerBasicInfoEdit"
    />

    <CustomerUpsertDialog
      v-model="showSelectedCustomerEditDialog"
      mode="edit"
      :customer="selectedCustomer"
      @success="handleSelectedCustomerEditSuccess"
    />

    <Transition name="wk-mobile-customer-summary-fade">
      <div
        v-if="mobileCustomerSummaryVisible && isMobile && isCustomerContextChat"
        class="wk-mobile-customer-summary"
        role="dialog"
        aria-modal="true"
        aria-labelledby="wk-mobile-customer-summary-title"
      >
        <button
          type="button"
          class="wk-mobile-customer-summary__backdrop"
          aria-label="关闭客户摘要"
          @click="closeMobileCustomerSummary"
        ></button>
        <section
          class="wk-mobile-customer-summary__sheet"
          :class="{
            'is-dragging': customerSummarySheetDragging,
            'has-voice-action': showMobileSummaryVoiceAction
          }"
          :style="{ height: `${customerSummarySheetHeight}vh` }"
        >
          <header
            class="wk-mobile-customer-summary__header"
            @pointerdown="handleCustomerSummaryDragStart"
          >
            <span class="wk-mobile-customer-summary__handle" aria-hidden="true"></span>
            <div class="wk-mobile-customer-summary__title-row">
              <div class="min-w-0 flex-1">
                <p id="wk-mobile-customer-summary-title" class="wk-mobile-customer-summary__title">
                  {{ mobileCustomerSummaryName }}
                </p>
                <p class="wk-mobile-customer-summary__subtitle">客户摘要</p>
              </div>
              <button
                type="button"
                class="wk-mobile-customer-summary__close"
                aria-label="关闭客户摘要"
                @pointerdown.stop
                @click.stop="closeMobileCustomerSummary"
              >
                <span class="material-symbols-outlined text-[24px] leading-none">close</span>
              </button>
            </div>
          </header>
          <div class="wk-mobile-customer-summary__body">
            <div v-if="selectedCustomerLoading" class="flex h-full items-center justify-center">
              <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
            </div>
            <div v-else-if="!selectedCustomer" class="flex h-full flex-col items-center justify-center px-6 text-center">
              <div class="mb-4 flex size-12 items-center justify-center rounded-2xl bg-[#f5f5f5] text-slate-400">
                <WkIcon name="customer" :size="24" />
              </div>
              <p class="text-sm font-semibold text-slate-700">暂无客户摘要</p>
              <p class="mt-1 text-xs leading-relaxed text-slate-400">客户资料加载后会在这里展示。</p>
            </div>
            <CustomerDetailView
              v-else
              ref="customerSummaryDetailRef"
              embedded
              force-mobile
              hide-embedded-follow-up-action
              :customer-id="selectedCustomer.customerId"
              @quote-attachment="handleQuoteCustomerAttachment"
            />
          </div>
          <Transition name="wk-mobile-summary-voice">
            <button
              v-if="showMobileSummaryVoiceAction"
              type="button"
              class="wk-mobile-customer-summary__voice"
              aria-label="语音录入"
              @click="openMobileSummaryVoiceInput"
            >
              <span class="material-symbols-outlined text-[20px] leading-none">keyboard_voice</span>
              <span>语音录入</span>
            </button>
          </Transition>
        </section>
      </div>
    </Transition>

    <button
      v-if="showCustomerPanelShell && !customerPanelVisible"
      type="button"
      class="group/sb-toggle absolute right-3 top-3 z-30 flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#efefef]"
      aria-label="展开客户侧栏"
      @click="customerPanelVisible = true"
    >
      <WkIcon name="fold" :size="18" class="shrink-0" />
      <span
        class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/sb-toggle:opacity-100"
        role="tooltip"
      >
        显示客户侧栏
      </span>
    </button>

    <aside
      v-if="showCustomerPanelShell && customerPanelVisible"
      class="wk-chat-customer-panel relative flex shrink-0 flex-col border-l"
      :class="{ 'select-none': customerPanelResizing }"
      :style="customerPanelStyle"
    >
      <div
        class="group absolute bottom-0 left-0 top-0 z-20 w-3 cursor-col-resize transition-colors hover:bg-[#0d0d0d0d]"
        aria-label="拖拽调整宽度"
        title="拖拽调整宽度"
        @mousedown="startCustomerPanelResize"
      >
        <span class="absolute left-0 top-1/2 h-10 w-px -translate-y-1/2 bg-transparent transition-colors group-hover:bg-[#cfcfcf]"></span>
      </div>
      <div class="min-h-0 flex-1 overflow-hidden pb-4">
        <div v-if="selectedCustomerLoading" class="flex h-full items-center justify-center">
          <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
        </div>
        <div v-else-if="!selectedCustomer" class="flex h-full flex-col items-center justify-center text-center">
          <div class="mb-4 flex size-12 items-center justify-center rounded-2xl bg-[#f5f5f5] text-slate-400">
            <WkIcon name="customer" :size="24" />
          </div>
          <p class="text-sm font-semibold text-slate-600">从左侧客户列表选择客户</p>
          <p class="mt-1 max-w-[220px] text-xs leading-relaxed text-slate-400">选择后会创建或打开该客户的 CRM 对话，并在这里展示相关资料。</p>
        </div>
        <div v-else class="h-full pr-0">
          <CustomerDetailView
            embedded
            force-mobile
            :customer-id="selectedCustomer.customerId"
            @quote-attachment="handleQuoteCustomerAttachment"
          />
        </div>
      </div>
    </aside>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import { useEnterpriseStore } from '@/stores/enterprise'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage } from 'element-plus'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { transcribeFollowUpAudio } from '@/api/followup'
import { getAiConfig } from '@/api/systemConfig'
import { addCustomerTag, getCustomerDetail, removeCustomerTag, updateCustomerStage } from '@/api/customer'
import CustomerDetailView from '@/views/customer/CustomerDetailView.vue'
import CustomerBasicInfoDrawer from '@/views/customer/components/CustomerBasicInfoDrawer.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
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
import { confirmDeleteChatSession } from '@/utils/confirmDeleteChatSession'
import { formatFileSize, resolveKnowledgeFileSizeBytes } from '@/utils/formatFileSize'
import { appEvents, APP_EVENT } from '@/utils/events'
import { shouldShowMobileCustomerSummaryAction } from '@/utils/chatMobileActions'
import {
  canCaptureMobileAudioFile,
  captureMobileAudioFile,
  hasMobileAudioInputSupport,
  requestMobileAudioStream
} from '@/utils/mobileAudioRecording'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, Knowledge, ChatModelOption } from '@/types/common'
import type { Contact, CustomerDetailVO, CustomerTag, FollowUp, FollowUpAttachment } from '@/types/customer'
import { wkIconNames } from '@/components/common/wkIcon'
import type { WkIconName } from '@/components/common/wkIcon'
import ChatKnowledgePickerModal from '@/components/chat/ChatKnowledgePickerModal.vue'
import dashscopeBrandUrl from '@/assets/model-provider-brands/dashscope.svg?url'
import openaiBrandUrl from '@/assets/model-provider-brands/openai.svg?url'
import deepseekBrandUrl from '@/assets/model-provider-brands/deepseek.svg?url'
import moonshotBrandUrl from '@/assets/model-provider-brands/moonshot.svg?url'
import arkBrandUrl from '@/assets/model-provider-brands/ark.svg?url'
import hunyuanBrandUrl from '@/assets/model-provider-brands/hunyuan.svg?url'
import minimaxBrandUrl from '@/assets/model-provider-brands/minimax.svg?url'
import zhipuBrandUrl from '@/assets/model-provider-brands/zhipu.svg?url'
import defaultLogoImg from '@/assets/images/logo.png'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const { composerFocusNonce } = storeToRefs(chatStore)
const agentStore = useAgentStore()
const userStore = useUserStore()
const enterpriseStore = useEnterpriseStore()
const { isMobile } = useResponsive()
const {
  aiConfig,
  aiConfigLoaded,
  canManageAiConfig,
  creditRemaining,
  loadAiConfig,
  ensureAiAvailableForSend,
  goToAiSettings,
  openTokenPurchaseDialog,
  resumeSendAfterTokenPurchase,
} = useAiQuota()
const boundCustomerId = computed(() => {
  const customerId = chatStore.currentSession?.customerId
  return customerId ? String(customerId) : ''
})
const boundCustomerName = computed(() =>
  boundCustomerId.value ? (chatStore.currentSession?.customerName || chatStore.currentSession?.title || '') : ''
)

const inputText = ref('')
const chatViewRef = ref<HTMLElement | null>(null)
const chatMainAreaRef = ref<HTMLElement | null>(null)
const messagesContainer = ref<HTMLElement | null>(null)
const showScrollToBottomButton = ref(false)
const mobilePanel = ref<'sessions' | 'chat'>('chat')
const fileInputRef = ref<HTMLInputElement | null>(null)
/** 桌面端多行输入（模板中 v-if="!isMobile" 的 textarea） */
const pcChatInputRef = ref<HTMLTextAreaElement | null>(null)
/** 移动端输入 */
const mobileChatInputRef = ref<HTMLTextAreaElement | null>(null)

function activeChatInputEl(): HTMLTextAreaElement | null {
  return isMobile.value ? mobileChatInputRef.value : pcChatInputRef.value
}

const chatUploadMenuVisible = ref(false)
const chatUploadSubmenuVisible = ref(false)
let chatUploadMenuLeaveTimer: ReturnType<typeof setTimeout> | null = null
/** 关闭上传菜单后跳过自动聚焦（如打开知识库选择器） */
let skipComposerFocusOnUploadMenuClose = false

function clearChatUploadMenuLeaveTimer() {
  if (chatUploadMenuLeaveTimer != null) {
    clearTimeout(chatUploadMenuLeaveTimer)
    chatUploadMenuLeaveTimer = null
  }
}

function isInsideChatUploadMenuPopover(target: EventTarget | null): boolean {
  if (!(target instanceof Node)) return false
  const el = target instanceof Element ? target : target.parentElement
  return Boolean(el?.closest('.wk-chat-upload-menu-popper'))
}

function handleChatUploadMenuMouseLeave(event: MouseEvent) {
  if (isInsideChatUploadMenuPopover(event.relatedTarget)) {
    clearChatUploadMenuLeaveTimer()
    return
  }
  clearChatUploadMenuLeaveTimer()
  chatUploadMenuLeaveTimer = setTimeout(() => {
    chatUploadMenuLeaveTimer = null
    chatUploadMenuVisible.value = false
    chatUploadSubmenuVisible.value = false
  }, 120)
}

watch(chatUploadMenuVisible, (visible) => {
  if (!visible) {
    chatUploadSubmenuVisible.value = false
    clearChatUploadMenuLeaveTimer()
    if (!skipComposerFocusOnUploadMenuClose) {
      void nextTick(() => focusChatTextarea())
    }
  }
})

const chatKnowledgePickerVisible = ref(false)
const showKnowledgeFollowUpChips = ref(false)
const chatModelPopoverVisible = ref(false)
const showAiQuotaChoiceDialog = ref(false)
const resumeSendAfterAiQuotaChoice = ref(false)
/** 按图片最终 URL 记录加载失败（含接口 icon 与本地 /model-provider-logos/{provider}.svg） */
const chatModelImageLoadFailed = ref<Record<string, boolean>>({})

const selectedCustomerId = ref<string | null>(null)
const selectedCustomer = ref<CustomerDetailVO | null>(null)
const selectedCustomerLoading = ref(false)
const mobileCustomerSummaryVisible = ref(false)
const customerSummaryDetailRef = ref<InstanceType<typeof CustomerDetailView> | null>(null)
const customerSummarySheetHeight = ref(58)
const customerSummarySheetDragging = ref(false)
const showSelectedCustomerTagDialog = ref(false)
const showSelectedCustomerBasicInfoDrawer = ref(false)
const showSelectedCustomerEditDialog = ref(false)
const newSelectedCustomerTagName = ref('')
const selectedCustomerTagSubmitting = ref(false)
const customerPanelVisible = ref(true)
const customerPanelWidth = ref(380)
const customerPanelResizing = ref(false)
const CUSTOMER_PANEL_MIN_WIDTH = 380
const CUSTOMER_PANEL_MAX_WIDTH_RATIO = 0.5
const CHAT_COMPOSER_MIN_WIDTH_PX = 468
const CHAT_CUSTOMER_AI_POLL_INTERVAL_MS = 2500
const CHAT_CUSTOMER_AI_POLL_MAX_ATTEMPTS = 24
const CUSTOMER_SUMMARY_SHEET_LEVELS = [58, 78, 94] as const
const CUSTOMER_SUMMARY_SHEET_MAX_HEIGHT = CUSTOMER_SUMMARY_SHEET_LEVELS[CUSTOMER_SUMMARY_SHEET_LEVELS.length - 1]
let chatCustomerAiPollTimer: ReturnType<typeof setTimeout> | null = null
let chatCustomerAiPollAttempts = 0
let offSelectedCustomerDetailRefresh: (() => void) | null = null
let customerSummaryDragStartY = 0
let customerSummaryDragStartHeight = 58

type CustomerDetailRefreshModule = 'aiAnalysis' | 'contacts' | 'followUps' | 'tasks' | 'schedules'

type CustomerDetailRefreshPayload = {
  customerId?: string | number
  source?: string
  modules?: CustomerDetailRefreshModule[]
}

const chatComposerShellStyle = computed(() => (
  isMobile.value ? undefined : { minWidth: `${CHAT_COMPOSER_MIN_WIDTH_PX}px` }
))

const CUSTOMER_STAGE_FLOW = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' },
] as const

const selectedCustomerStageText = computed(() => {
  const c = selectedCustomer.value
  if (!c) return '--'
  if ('stageName' in c && c.stageName) return c.stageName
  const stage = CUSTOMER_STAGE_FLOW.find(item => item.value === c.stage)
  return stage?.label || c.stage || '--'
})
const selectedCustomerStageButtonClass = computed(() => getCustomerStageButtonClass(selectedCustomer.value?.stage || 'lead'))
const canEditSelectedCustomerTags = computed(() => userStore.hasPermission('customer:edit'))
const selectedCustomerVisibleTags = computed(() => selectedCustomer.value?.tags?.slice(0, 3) || [])
const selectedCustomerHiddenTags = computed(() => selectedCustomer.value?.tags?.slice(3) || [])
const selectedCustomerContacts = computed<Contact[]>(() => selectedCustomer.value?.contacts || [])
const currentSessionCustomerId = computed(() => {
  const customerId = chatStore.currentSession?.customerId
  return customerId ? String(customerId) : ''
})
const showCustomerPanelShell = computed(() => !isMobile.value && Boolean(currentSessionCustomerId.value))
const customerPanelStyle = computed(() => ({
  width: `${customerPanelWidth.value}px`,
  minWidth: `min(${CUSTOMER_PANEL_MIN_WIDTH}px, ${CUSTOMER_PANEL_MAX_WIDTH_RATIO * 100}%)`,
  maxWidth: `${CUSTOMER_PANEL_MAX_WIDTH_RATIO * 100}%`
}))

const chatModelOptionGroups = computed(() => {
  const customOptions = chatStore.modelOptions.filter(option => option.modelSource === 'custom')
  const systemOptions = chatStore.modelOptions.filter(option => option.modelSource !== 'custom')
  return [
    { source: 'custom', label: '自定义模型', options: customOptions },
    { source: 'system', label: '系统模型', options: systemOptions },
  ].filter(group => group.options.length > 0)
})

const chatComposerModelLabel = computed(() => {
  if (chatStore.modelOptionsLoading) return '加载模型...'
  if (showAiQuotaEmptyCta.value) return '额度已用完，续费/接入模型'
  const m = chatStore.selectedModel
  return m ? modelOptionLabel(m) : '选择模型'
})

const showAiQuotaEmptyCta = computed(() =>
  !chatStore.modelOptionsLoading
  && (chatStore.modelOptions.length === 0 || (aiConfigLoaded.value && creditRemaining.value <= 0))
)

const chatModelPickerDisabled = computed(() =>
  chatStore.currentSessionIsStreaming || isUploading.value || chatStore.modelOptionsLoading
)

const chatModelTriggerClass = computed(() =>
  showAiQuotaEmptyCta.value
    ? 'border-[#ead8cc] bg-[#fbf7f4] text-[#8d4f34] hover:border-[#dec4b5] hover:bg-[#f7eee8]'
    : 'border-[#ececec] bg-[#f5f5f5] text-[#0d0d0d] hover:bg-[#ececec]'
)

function modelOptionLabel(option: ChatModelOption): string {
  return option.displayName || option.modelName
}

const selectedChatAppLabel = computed(() => {
  if (chatStore.selectedAppCode === 'general') return ''
  if (chatStore.selectedAppCode === 'crm' && (selectedCustomerId.value || currentSessionCustomerId.value)) {
    return selectedCustomer.value?.companyName || boundCustomerName.value || '客户'
  }
  if (chatStore.selectedApp?.label) {
    return chatStore.selectedApp.label == '知识库' ? '知识库检索' : chatStore.selectedApp.label
  }
  if (chatStore.selectedAppCode === 'crm') return 'CRM管理'
  if (chatStore.selectedAppCode === 'knowledge') return '知识库检索'
  return ''
})

const selectedChatAppIcon = computed<WkIconName>(() => {
  if (chatStore.selectedAppCode === 'general') return 'application'
  if (chatStore.selectedApp?.iconName && isWkIconName(chatStore.selectedApp.iconName)) {
    return chatStore.selectedApp.iconName
  }
  if (chatStore.selectedAppCode === 'crm') return 'customer'
  if (chatStore.selectedAppCode === 'knowledge') return 'knowledge-1'
  return 'application'
})

function isWkIconName(iconName: string): iconName is WkIconName {
  return (wkIconNames as readonly string[]).includes(iconName)
}

/**
 * 厂商标识 SVG（与接口 `provider` / 后端 AiProviderRegistry.code 一致）。
 * 资源位于 `src/assets/model-provider-brands/`。
 */
const MODEL_PROVIDER_BRAND_URL: Record<string, string> = {
  dashscope: dashscopeBrandUrl,
  openai: openaiBrandUrl,
  deepseek: deepseekBrandUrl,
  moonshot: moonshotBrandUrl,
  ark: arkBrandUrl,
  arkl: arkBrandUrl,
  hunyuan: hunyuanBrandUrl,
  minimax: minimaxBrandUrl,
  zhipu: zhipuBrandUrl,
}

function providerBrandAssetUrl(provider: string): string | undefined {
  const id = provider?.trim().toLowerCase()
  if (!id || !/^[-a-z0-9._]+$/.test(id)) return undefined
  return MODEL_PROVIDER_BRAND_URL[id]
}

function chatModelIconSrc(option: ChatModelOption): string | undefined {
  const fromApi = option.icon?.trim()
  if (fromApi) return fromApi
  return providerBrandAssetUrl(option.provider)
}

function chatModelShowImage(option: ChatModelOption): boolean {
  const src = chatModelIconSrc(option)
  if (!src) return false
  return !chatModelImageLoadFailed.value[src]
}

function onChatModelImageError(ev: Event) {
  const t = ev.target as HTMLImageElement | null
  const src = t?.currentSrc || t?.src
  if (!src) return
  chatModelImageLoadFailed.value = { ...chatModelImageLoadFailed.value, [src]: true }
}

function handleChatModelChange(modelKey: string) {
  chatStore.setSelectedModelKey(modelKey)
  chatModelPopoverVisible.value = false
  focusChatTextarea()
}

function openAiQuotaChoiceDialog(resumeAfterAction = false) {
  chatModelPopoverVisible.value = false
  resumeSendAfterAiQuotaChoice.value = resumeAfterAction
  showAiQuotaChoiceDialog.value = true
}

function handleChatModelTriggerClick() {
  if (showAiQuotaEmptyCta.value) {
    openAiQuotaChoiceDialog()
  }
}

function handleAiQuotaChoicePurchase() {
  const shouldResumeSend = resumeSendAfterAiQuotaChoice.value
  resumeSendAfterAiQuotaChoice.value = false
  openTokenPurchaseDialog()
  resumeSendAfterTokenPurchase.value = shouldResumeSend
  showAiQuotaChoiceDialog.value = false
}

function handleAiQuotaChoiceConfigure() {
  resumeSendAfterAiQuotaChoice.value = false
  showAiQuotaChoiceDialog.value = false
  goToAiSettings()
}

function handleOpenMoreModels() {
  chatModelPopoverVisible.value = false
  goToAiSettings()
}

watch(
  () => chatStore.modelOptions,
  () => {
    chatModelImageLoadFailed.value = {}
  },
  { deep: true }
)

let hasSeenInitialAiConfigForModelRefresh = false
watch(
  () => [
    aiConfig.value?.mode,
    aiConfig.value?.customConfigSaved,
    aiConfig.value?.updateTime,
    aiConfig.value?.creditRemaining,
    aiConfig.value?.creditAvailable,
  ],
  () => {
    if (!hasSeenInitialAiConfigForModelRefresh) {
      hasSeenInitialAiConfigForModelRefresh = true
      return
    }
    void chatStore.fetchModelOptions()
  }
)

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

function getCustomerPanelContainerWidth(): number {
  const width = chatViewRef.value?.clientWidth || 0
  return width > 0 ? width : window.innerWidth
}

function updateCustomerPanelContainerWidth() {
  const containerWidth = getCustomerPanelContainerWidth()
  if (containerWidth <= 0) return
  customerPanelWidth.value = resolveCustomerPanelWidth(customerPanelWidth.value, containerWidth)
}

function resolveCustomerPanelWidth(width: number, containerWidth = getCustomerPanelContainerWidth()): number {
  if (containerWidth <= 0) return Math.max(CUSTOMER_PANEL_MIN_WIDTH, width)
  const maxWidth = containerWidth * CUSTOMER_PANEL_MAX_WIDTH_RATIO
  const minWidth = Math.min(CUSTOMER_PANEL_MIN_WIDTH, maxWidth)
  return Math.min(maxWidth, Math.max(minWidth, width))
}

function applyCustomerPanelWidth(width: number) {
  customerPanelWidth.value = resolveCustomerPanelWidth(width)
}

function handleCustomerPanelResize(event: MouseEvent) {
  if (!customerPanelResizing.value) return
  const rect = chatViewRef.value?.getBoundingClientRect()
  const rawWidth = rect ? rect.right - event.clientX : window.innerWidth - event.clientX
  applyCustomerPanelWidth(rawWidth)
}

function stopCustomerPanelResize() {
  if (!customerPanelResizing.value) return
  customerPanelResizing.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', handleCustomerPanelResize)
  window.removeEventListener('mouseup', stopCustomerPanelResize)
}

function startCustomerPanelResize(event: MouseEvent) {
  event.preventDefault()
  customerPanelVisible.value = true
  customerPanelResizing.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleCustomerPanelResize)
  window.addEventListener('mouseup', stopCustomerPanelResize)
}

/** Persist unsent input per session; debounced to reduce localStorage writes. */
let composerDraftSaveTimer: ReturnType<typeof setTimeout> | null = null
function schedulePersistComposerDraft() {
  const sid = chatStore.currentSessionId
  if (!sid) return
  if (composerDraftSaveTimer != null) clearTimeout(composerDraftSaveTimer)
  composerDraftSaveTimer = setTimeout(() => {
    composerDraftSaveTimer = null
    chatStore.setComposerDraft(sid, inputText.value)
  }, 400)
}

watch(
  () => chatStore.currentSessionId,
  (newId, oldId) => {
    if (composerDraftSaveTimer != null) {
      clearTimeout(composerDraftSaveTimer)
      composerDraftSaveTimer = null
    }
    if (oldId != null) {
      chatStore.setComposerDraft(oldId, inputText.value)
    }
    inputText.value = newId ? chatStore.getComposerDraft(newId) : ''
    void nextTick(() => resizeChatTextarea())
  },
  { flush: 'post' }
)

watch(inputText, () => {
  void nextTick(resizeChatTextarea)
  schedulePersistComposerDraft()
})
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

const composerAttachmentPreviewItems = computed(() => {
  type Item =
    | { kind: 'knowledge'; knowledge: Knowledge; key: string }
    | { kind: 'file'; file: File; fileIndex: number; key: string }
  const out: Item[] = []
  for (const k of selectedKnowledgeItems.value) {
    out.push({ kind: 'knowledge', knowledge: k, key: `k-${k.knowledgeId}` })
  }
  for (let index = 0; index < selectedFiles.value.length; index++) {
    const file = selectedFiles.value[index]!
    out.push({ kind: 'file', file, fileIndex: index, key: `f-${file.name}-${index}` })
  }
  return out
})

const composerAttachmentScrollRef = ref<HTMLElement | null>(null)
const composerAttachmentShowScrollArrows = ref(false)
const composerAttachmentCanScrollLeft = ref(false)
const composerAttachmentCanScrollRight = ref(false)

let composerAttachmentScrollResizeObserver: ResizeObserver | null = null

function disconnectComposerAttachmentScrollResizeObserver() {
  composerAttachmentScrollResizeObserver?.disconnect()
  composerAttachmentScrollResizeObserver = null
}

function updateComposerAttachmentScrollState() {
  const el = composerAttachmentScrollRef.value
  if (!el) {
    composerAttachmentShowScrollArrows.value = false
    composerAttachmentCanScrollLeft.value = false
    composerAttachmentCanScrollRight.value = false
    return
  }
  const { scrollLeft, scrollWidth, clientWidth } = el
  const overflow = scrollWidth > clientWidth + 2
  composerAttachmentShowScrollArrows.value = overflow
  if (!overflow) {
    composerAttachmentCanScrollLeft.value = false
    composerAttachmentCanScrollRight.value = false
    return
  }
  composerAttachmentCanScrollLeft.value = scrollLeft > 2
  composerAttachmentCanScrollRight.value = scrollLeft + clientWidth < scrollWidth - 2
}

function scrollComposerAttachmentStep(dir: -1 | 1) {
  const el = composerAttachmentScrollRef.value
  if (!el) return
  const step = Math.max(160, Math.floor(el.clientWidth * 0.65))
  el.scrollBy({ left: dir * step, behavior: 'smooth' })
}

function bindComposerAttachmentScrollResizeObserver() {
  disconnectComposerAttachmentScrollResizeObserver()
  const el = composerAttachmentScrollRef.value
  if (!el || typeof ResizeObserver === 'undefined') return
  composerAttachmentScrollResizeObserver = new ResizeObserver(() => {
    updateComposerAttachmentScrollState()
  })
  composerAttachmentScrollResizeObserver.observe(el)
}

function scheduleComposerAttachmentScrollLayoutCheck() {
  void nextTick(() => {
    updateComposerAttachmentScrollState()
    bindComposerAttachmentScrollResizeObserver()
  })
}

watch(
  () => [
    selectedFiles.value.length,
    selectedKnowledgeItems.value.length,
    composerAttachmentPreviewItems.value.length,
    isMobile.value,
  ],
  () => {
    const hasStrip = composerAttachmentPreviewItems.value.length > 0
    if (!hasStrip) {
      disconnectComposerAttachmentScrollResizeObserver()
      composerAttachmentShowScrollArrows.value = false
      composerAttachmentCanScrollLeft.value = false
      composerAttachmentCanScrollRight.value = false
      return
    }
    scheduleComposerAttachmentScrollLayoutCheck()
  },
  { flush: 'post' }
)

const sendBarActionButtonClass = computed(() => {
  if (chatStore.currentSessionIsStreaming) return 'bg-[#e5e5e5] text-[#0d0d0d] hover:bg-[#d4d4d4]'
  if (isRecording.value) return 'bg-red-500 text-white hover:bg-red-600'
  if (isTranscribing.value || isUploading.value) return 'bg-[#e5e5e5] text-[#909090]'
  if (isChatInputEmpty.value) return 'bg-[#000] text-white hover:bg-[#575757]'
  return 'bg-[#0d0d0d] text-white hover:bg-[#0d0d0d]/90'
})

const sendBarActionDisabled = computed(
  () => isUploading.value || isTranscribing.value
)

const sendBarActionTitle = computed(() => {
  if (chatStore.currentSessionIsStreaming) return '停止生成'
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
const isCustomerContextChat = computed(() => Boolean(selectedCustomerId.value || currentSessionCustomerId.value))
const isCenteredEmptyChat = computed(() => isChatEmpty.value && !isCustomerContextChat.value)
const mobileChatFloatingBarTop = computed(() => '0px')
const showMobileFloatingBar = computed(() =>
  isMobile.value
    && currentView.value === 'chat'
    && mobilePanel.value === 'chat'
    && !isCustomerContextChat.value
)
const showMobileCustomerSummaryAction = computed(() =>
  shouldShowMobileCustomerSummaryAction({
    isMobile: isMobile.value,
    currentView: currentView.value,
    mobilePanel: mobilePanel.value,
    hasCustomerContext: isCustomerContextChat.value,
  })
)
const showMobileNewSessionAction = computed(() =>
  isMobile.value
    && currentView.value === 'chat'
    && mobilePanel.value === 'chat'
    && !isCustomerContextChat.value
    && Boolean(chatStore.currentSessionId)
    && !chatStore.isNewSessionPending
)
const mobileChatFloatingActionCount = computed(() =>
  (showMobileNewSessionAction.value ? 1 : 0)
  + (showMobileCustomerSummaryAction.value ? 1 : 0)
)
const showMobileChatFloatingActions = computed(() => mobileChatFloatingActionCount.value > 0)
const mobileCustomerSummaryName = computed(() =>
  selectedCustomer.value?.companyName || boundCustomerName.value || '客户'
)
const mobileCustomerHeaderName = computed(() =>
  selectedCustomer.value?.companyName || boundCustomerName.value || chatStore.currentSession?.title || '客户'
)
const showMobileSummaryVoiceAction = computed(() =>
  mobileCustomerSummaryVisible.value
    && isMobile.value
    && isCustomerContextChat.value
    && customerSummarySheetHeight.value >= CUSTOMER_SUMMARY_SHEET_MAX_HEIGHT - 1
)
const chatMessagesAreaClass = computed(() => {
  if (!isChatEmpty.value) return 'wk-chat-messages--scrollable flex-1 overflow-y-auto pb-4 pt-6 md:pt-8'
  if (isMobile.value) return 'flex-1 overflow-hidden py-6'
  return isCustomerContextChat.value ? 'flex-1 overflow-hidden py-6' : 'overflow-hidden py-6'
})

watch(
  () => [isMobile.value, isCustomerContextChat.value, currentSessionCustomerId.value, selectedCustomerId.value] as const,
  ([mobile, hasCustomer]) => {
    if (!mobile || !hasCustomer) {
      closeMobileCustomerSummary()
    }
  }
)

const SCROLL_TO_BOTTOM_THRESHOLD_PX = 100
/** When true, new content / streaming keeps the viewport pinned to the bottom. */
const isPinnedToBottom = ref(true)

function updateMessagesScrollbarOffset() {
  const el = messagesContainer.value
  if (!el) return
  const scrollbarOffset = Math.max(0, el.offsetWidth - el.clientWidth)
  el.style.setProperty('--wk-chat-messages-scrollbar-offset', `${scrollbarOffset}px`)
}

function getMessagesDistanceToBottom(el: HTMLElement) {
  return el.scrollHeight - (el.scrollTop + el.clientHeight)
}

function updateScrollToBottomVisibility() {
  const el = messagesContainer.value
  if (!el || chatStore.messages.length === 0) {
    showScrollToBottomButton.value = false
    return
  }
  showScrollToBottomButton.value = getMessagesDistanceToBottom(el) > SCROLL_TO_BOTTOM_THRESHOLD_PX
}

function handleMessagesScroll() {
  const el = messagesContainer.value
  if (el) {
    isPinnedToBottom.value = getMessagesDistanceToBottom(el) <= SCROLL_TO_BOTTOM_THRESHOLD_PX
  }
  updateScrollToBottomVisibility()
}

function scrollToBottomSmooth() {
  const el = messagesContainer.value
  if (!el) return
  isPinnedToBottom.value = true
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

/** Ensure the composer is mounted before focusing on mobile. */
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

function clampCustomerSummaryHeight(value: number) {
  return Math.min(CUSTOMER_SUMMARY_SHEET_MAX_HEIGHT, Math.max(CUSTOMER_SUMMARY_SHEET_LEVELS[0], value))
}

function snapCustomerSummaryHeight(value: number) {
  return CUSTOMER_SUMMARY_SHEET_LEVELS.reduce((best, level) => (
    Math.abs(level - value) < Math.abs(best - value) ? level : best
  ), CUSTOMER_SUMMARY_SHEET_LEVELS[0])
}

function openMobileCustomerSummary() {
  const customerId = currentSessionCustomerId.value || selectedCustomerId.value
  if (!customerId) return
  customerSummarySheetHeight.value = CUSTOMER_SUMMARY_SHEET_LEVELS[0]
  mobileCustomerSummaryVisible.value = true
  void ensureSelectedCustomerDetail(customerId, { silent: Boolean(selectedCustomer.value) })
}

function openSelectedCustomerBasicInfo() {
  const customerId = currentSessionCustomerId.value || selectedCustomerId.value
  if (!customerId) return
  showSelectedCustomerBasicInfoDrawer.value = true
  void ensureSelectedCustomerDetail(customerId, { silent: Boolean(selectedCustomer.value) })
}

function closeMobileCustomerSummary() {
  mobileCustomerSummaryVisible.value = false
  customerSummarySheetDragging.value = false
  window.removeEventListener('pointermove', handleCustomerSummaryDragMove)
  window.removeEventListener('pointerup', handleCustomerSummaryDragEnd)
  window.removeEventListener('pointercancel', handleCustomerSummaryDragEnd)
}

function handleCustomerSummaryDragStart(event: PointerEvent) {
  if (!mobileCustomerSummaryVisible.value) return
  customerSummarySheetDragging.value = true
  customerSummaryDragStartY = event.clientY
  customerSummaryDragStartHeight = customerSummarySheetHeight.value
  window.addEventListener('pointermove', handleCustomerSummaryDragMove)
  window.addEventListener('pointerup', handleCustomerSummaryDragEnd)
  window.addEventListener('pointercancel', handleCustomerSummaryDragEnd)
}

function handleCustomerSummaryDragMove(event: PointerEvent) {
  if (!customerSummarySheetDragging.value) return
  event.preventDefault()
  const viewportHeight = Math.max(1, window.innerHeight)
  const deltaY = customerSummaryDragStartY - event.clientY
  const nextHeight = customerSummaryDragStartHeight + (deltaY / viewportHeight) * 100
  customerSummarySheetHeight.value = clampCustomerSummaryHeight(nextHeight)
}

function handleCustomerSummaryDragEnd() {
  if (!customerSummarySheetDragging.value) return
  customerSummarySheetDragging.value = false
  customerSummarySheetHeight.value = snapCustomerSummaryHeight(customerSummarySheetHeight.value)
  window.removeEventListener('pointermove', handleCustomerSummaryDragMove)
  window.removeEventListener('pointerup', handleCustomerSummaryDragEnd)
  window.removeEventListener('pointercancel', handleCustomerSummaryDragEnd)
}

function openMobileSummaryVoiceInput() {
  customerSummaryDetailRef.value?.openAiFollowUp?.()
}

function handleMobileSummaryTouchMove(event: TouchEvent) {
  if (customerSummarySheetDragging.value) {
    event.preventDefault()
  }
}

watch(composerFocusNonce, () => {
  focusChatTextarea()
}, { flush: 'post' })

function handleInputBoxMouseDown(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (!target) return
  // Don't steal focus from controls embedded in the composer.
  if (target.closest('button, input, textarea, select, a, [role="button"], .el-select, .el-input')) return
  if (isUploading.value) return
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

async function ensureSelectedCustomerDetail(customerId: string, options: { silent?: boolean } = {}) {
  const requestCustomerId = String(customerId)
  const silent = Boolean(options.silent)
  if (!silent) {
    selectedCustomerLoading.value = true
  }
  try {
    const detail = await getCustomerDetail(requestCustomerId)
    if (selectedCustomerId.value && selectedCustomerId.value !== requestCustomerId) return
    selectedCustomer.value = detail
  } catch (err) {
    console.error('Load selected customer detail failed:', err)
    if (!isRequestErrorHandled(err)) {
      ElMessage.warning('客户详情加载失败')
    }
  } finally {
    if (!silent && (!selectedCustomerId.value || selectedCustomerId.value === requestCustomerId)) {
      selectedCustomerLoading.value = false
    }
  }
}

function isCustomerAiAnalysisPending(customer: CustomerDetailVO | null) {
  return customer?.aiAnalysisStatus === 'pending' || customer?.aiAnalysisStatus === 'running'
}

function clearChatCustomerAiPolling(resetAttempts = true) {
  if (chatCustomerAiPollTimer) {
    clearTimeout(chatCustomerAiPollTimer)
    chatCustomerAiPollTimer = null
  }
  if (resetAttempts) {
    chatCustomerAiPollAttempts = 0
  }
}

function scheduleChatCustomerAiPolling(customerId?: string, resetAttempts = false) {
  if (!customerId) return
  if (resetAttempts) {
    clearChatCustomerAiPolling()
  }
  if (!isCustomerAiAnalysisPending(selectedCustomer.value)) {
    clearChatCustomerAiPolling(resetAttempts)
    return
  }
  if (chatCustomerAiPollTimer || chatCustomerAiPollAttempts >= CHAT_CUSTOMER_AI_POLL_MAX_ATTEMPTS) {
    return
  }

  chatCustomerAiPollTimer = setTimeout(async () => {
    chatCustomerAiPollTimer = null
    if (currentSessionCustomerId.value !== customerId) {
      clearChatCustomerAiPolling()
      return
    }

    chatCustomerAiPollAttempts += 1
    const previousStatus = selectedCustomer.value?.aiAnalysisStatus || ''
    const wasPending = isCustomerAiAnalysisPending(selectedCustomer.value)
    await ensureSelectedCustomerDetail(customerId, { silent: true })
    const isPending = isCustomerAiAnalysisPending(selectedCustomer.value)
    const nextStatus = selectedCustomer.value?.aiAnalysisStatus || ''

    if (currentSessionCustomerId.value !== customerId) {
      clearChatCustomerAiPolling()
      return
    }

    if (isPending && chatCustomerAiPollAttempts < CHAT_CUSTOMER_AI_POLL_MAX_ATTEMPTS) {
      scheduleChatCustomerAiPolling(customerId)
      return
    }

    if (wasPending && previousStatus !== nextStatus && !isPending) {
      appEvents.emit(APP_EVENT.CUSTOMER_DETAIL_REFRESH, {
        customerId,
        source: 'chat-ai-poll',
        modules: ['aiAnalysis']
      })
    }

    clearChatCustomerAiPolling()
  }, CHAT_CUSTOMER_AI_POLL_INTERVAL_MS)
}

function handleSelectedCustomerDetailRefresh(payload?: CustomerDetailRefreshPayload) {
  if (payload?.source === 'chat' || payload?.source === 'chat-ai-poll') return
  const currentCustomerId = currentSessionCustomerId.value
  const targetCustomerId = payload?.customerId ? String(payload.customerId) : ''
  if (!currentCustomerId || (targetCustomerId && targetCustomerId !== currentCustomerId)) return

  void ensureSelectedCustomerDetail(currentCustomerId, { silent: Boolean(selectedCustomer.value) }).then(() => {
    if (isCustomerAiAnalysisPending(selectedCustomer.value)) {
      scheduleChatCustomerAiPolling(currentCustomerId, true)
    }
  })
}

async function handleSelectCustomerById(customerId: string) {
  selectedCustomerId.value = customerId
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
  isPinnedToBottom.value = true
  chatStore.setSelectedAppCode('crm')
  await ensureSelectedCustomerDetail(customerId)
  const customerName = selectedCustomer.value?.companyName || '客户'
  if (chatStore.sessions.length === 0) {
    await chatStore.fetchSessions()
  }

  const existingSession = chatStore.sessions.find(session => String(session.customerId) === String(customerId))
  if (existingSession) {
    await chatStore.selectSession(existingSession.sessionId)
  } else {
    await chatStore.startNewSession(`与${customerName}对话`, undefined, customerId, 'crm')
  }

  if (chatStore.messages.length === 0 && !inputText.value.trim()) {
    // inputText.value = `请帮我分析客户「${customerName}」的当前情况，并给出下一步建议。`
  }
  await nextTick()
  focusChatTextarea()
}

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
let customerPanelResizeObserver: ResizeObserver | null = null
let chatMainAreaResizeObserver: ResizeObserver | null = null
let chatMessagesResizeObserver: ResizeObserver | null = null
let lastChatComposerNarrow: boolean | null = null
let lastChatComposerWidth = 0

function emitChatComposerNarrowState(force = false) {
  const width = chatMainAreaRef.value?.getBoundingClientRect().width || 0
  const narrow = !isMobile.value && width > 0 && width < CHAT_COMPOSER_MIN_WIDTH_PX
  const roundedWidth = Math.round(width)
  if (!force && narrow === lastChatComposerNarrow && roundedWidth === lastChatComposerWidth) return
  lastChatComposerNarrow = narrow
  lastChatComposerWidth = roundedWidth
  appEvents.emit(APP_EVENT.CHAT_COMPOSER_NARROW_CHANGE, {
    narrow,
    width,
    minWidth: CHAT_COMPOSER_MIN_WIDTH_PX
  })
}

onMounted(async () => {
  offSelectedCustomerDetailRefresh = appEvents.on<CustomerDetailRefreshPayload>(
    APP_EVENT.CUSTOMER_DETAIL_REFRESH,
    handleSelectedCustomerDetailRefresh
  )
  registerAiQuotaResumeSendHandler(handleSend)
  await nextTick()
  updateCustomerPanelContainerWidth()
  updateMessagesScrollbarOffset()
  if (chatViewRef.value) {
    customerPanelResizeObserver = new ResizeObserver(updateCustomerPanelContainerWidth)
    customerPanelResizeObserver.observe(chatViewRef.value)
  }
  if (chatMainAreaRef.value && typeof ResizeObserver !== 'undefined') {
    chatMainAreaResizeObserver = new ResizeObserver(() => emitChatComposerNarrowState())
    chatMainAreaResizeObserver.observe(chatMainAreaRef.value)
  }
  if (messagesContainer.value && typeof ResizeObserver !== 'undefined') {
    chatMessagesResizeObserver = new ResizeObserver(updateMessagesScrollbarOffset)
    chatMessagesResizeObserver.observe(messagesContainer.value)
  }
  document.addEventListener('touchmove', handleMobileSummaryTouchMove, { passive: false })
  emitChatComposerNarrowState(true)
  resizeChatTextarea()
  await Promise.all([
    chatStore.fetchSessions(),
    chatStore.fetchModelOptions(),
    chatStore.fetchAppOptions(),
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
  stopCustomerPanelResize()
  clearChatCustomerAiPolling()
  offSelectedCustomerDetailRefresh?.()
  offSelectedCustomerDetailRefresh = null
  if (composerDraftSaveTimer != null) {
    clearTimeout(composerDraftSaveTimer)
    composerDraftSaveTimer = null
  }
  const sid = chatStore.currentSessionId
  if (sid) {
    chatStore.setComposerDraft(sid, inputText.value)
  }
  abortChatVoiceRecording()
  transcriptionToken += 1
  closeMobileCustomerSummary()
  document.removeEventListener('touchmove', handleMobileSummaryTouchMove)
  unregisterAiQuotaResumeSendHandler()
  customerPanelResizeObserver?.disconnect()
  customerPanelResizeObserver = null
  chatMainAreaResizeObserver?.disconnect()
  chatMainAreaResizeObserver = null
  chatMessagesResizeObserver?.disconnect()
  chatMessagesResizeObserver = null
  appEvents.emit(APP_EVENT.CHAT_COMPOSER_NARROW_CHANGE, {
    narrow: false,
    width: 0,
    minWidth: CHAT_COMPOSER_MIN_WIDTH_PX
  })
  disconnectComposerAttachmentScrollResizeObserver()
})

watch(isMobile, () => {
  void nextTick(() => {
    emitChatComposerNarrowState(true)
    updateMessagesScrollbarOffset()
  })
})

watch(chatMessagesAreaClass, () => {
  void nextTick(updateMessagesScrollbarOffset)
}, { flush: 'post' })

// Auto scroll to bottom when new messages arrive or during streaming (only while pinned).
// No debounce when not streaming so session switches jump instantly (no smooth scroll).
let scrollTimer: ReturnType<typeof setTimeout> | null = null
function scrollToBottom() {
  if (!isPinnedToBottom.value) return
  if (scrollTimer) return
  const delayMs = chatStore.currentSessionIsStreaming ? 100 : 0
  scrollTimer = setTimeout(() => {
    scrollTimer = null
    if (!isPinnedToBottom.value) return
    const el = messagesContainer.value
    if (el) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'auto' })
      updateScrollToBottomVisibility()
    }
  }, delayMs)
}

watch(
  () => {
    const msgs = chatStore.messages
    const last = msgs[msgs.length - 1]
    return {
      length: msgs.length,
      content: last?.content?.length ?? 0,
      isStreaming: Boolean(last?.isStreaming),
      isThinking: Boolean(last?.isThinking)
    }
  },
  () => {
    nextTick(() => {
      updateMessagesScrollbarOffset()
      scrollToBottom()
    })
  }
)

watch(
  () => userStore.avatar,
  () => {
    userAvatarLoadFailed.value = false
  }
)

function resolveChatSendAppCode(hasKnowledge: boolean): string {
  if (hasKnowledge && chatStore.selectedAppCode === 'general') {
    return 'knowledge'
  }
  return chatStore.selectedAppCode || 'general'
}

async function refreshAiAvailabilitySnapshot() {
  const tasks: Promise<unknown>[] = []

  if (!aiConfig.value || creditRemaining.value <= 0) {
    tasks.push(loadAiConfig(true))
  }

  if (chatStore.modelOptions.length === 0) {
    tasks.push(chatStore.fetchModelOptions())
  }

  if (tasks.length === 0) return

  try {
    await Promise.all(tasks)
  } catch (error) {
    console.error('Refresh AI availability snapshot failed:', error)
  }
}

async function ensureChatAiAvailableForSend(): Promise<boolean> {
  await refreshAiAvailabilitySnapshot()

  if (!chatStore.modelOptions.length || creditRemaining.value <= 0) {
    openAiQuotaChoiceDialog(true)
    return false
  }

  return ensureAiAvailableForSend()
}

async function handleSend() {
  const text = inputText.value.trim()
  const hasFiles = selectedFiles.value.length > 0
  const hasKnowledge = selectedKnowledgeItems.value.length > 0
  if ((!text && !hasFiles && !hasKnowledge) || chatStore.currentSessionIsStreaming || isUploading.value) return
  if (!(await ensureChatAiAvailableForSend())) return

  isPinnedToBottom.value = true

  const content =
    text ||
    (hasFiles && hasKnowledge
      ? '请结合上传的附件与选中的知识库文档回答'
      : hasFiles
        ? '请分析这些文件'
        : '请结合选中的知识库文档回答')
  const draftSessionId = chatStore.currentSessionId
  inputText.value = ''
  if (draftSessionId) {
    chatStore.setComposerDraft(draftSessionId, '')
  }
  if (composerDraftSaveTimer != null) {
    clearTimeout(composerDraftSaveTimer)
    composerDraftSaveTimer = null
  }

  const knowledgeIdsPayload = hasKnowledge
    ? selectedKnowledgeItems.value.map((k) => k.knowledgeId)
    : undefined
  const effectiveAppCode = resolveChatSendAppCode(hasKnowledge)
  if (effectiveAppCode !== chatStore.selectedAppCode) {
    chatStore.setSelectedAppCode(effectiveAppCode)
  }
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
  const sendPromise = chatStore.sendMessage(
    content,
    attachmentDTOs,
    attachmentVOs,
    effectiveAppCode,
    knowledgeIdsPayload
  )
  await nextTick()
  focusChatTextarea()
  const completedSessionId = await sendPromise
  await refreshCrmContextAfterSend(effectiveAppCode, completedSessionId)
  await sendPromise
  await loadAiConfig(true)
}

function getSessionCustomerId(sessionId?: string) {
  if (!sessionId) return ''
  const session = chatStore.sessions.find(item => item.sessionId === sessionId)
  return session?.customerId ? String(session.customerId) : ''
}

async function refreshCrmContextAfterSend(effectiveAppCode: string, completedSessionId?: string) {
  if (effectiveAppCode !== 'crm') return

  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH, { source: 'chat', preserveScroll: true })

  const customerId = getSessionCustomerId(completedSessionId)
  if (!customerId) return
  appEvents.emit(APP_EVENT.CUSTOMER_DETAIL_REFRESH, {
    customerId,
    source: 'chat',
    modules: ['contacts', 'followUps', 'tasks', 'schedules']
  })
}

function handleSendBarClick() {
  if (isUploading.value || isTranscribing.value) return
  if (chatStore.currentSessionIsStreaming) {
    chatStore.stopStreaming()
    return
  }
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
  const useMobileAudioApi = isMobile.value
  const hasAudioInput = useMobileAudioApi
    ? hasMobileAudioInputSupport()
    : Boolean(navigator.mediaDevices?.getUserMedia)
  const useMobileAudioFileCapture = useMobileAudioApi
    && canCaptureMobileAudioFile()
    && (!hasAudioInput || typeof MediaRecorder === 'undefined')

  if (useMobileAudioFileCapture) {
    speechInputBase = inputText.value.trim()
    const capturedFile = await captureMobileAudioFile()
    if (!capturedFile) return
    if (!(await ensureChatAudioTranscriptionSupported())) return
    await transcribeChatRecordedAudio(capturedFile)
    return
  }

  if (!(await ensureChatAudioTranscriptionSupported())) return
  if (!hasAudioInput || typeof MediaRecorder === 'undefined') {
    ElMessage.warning('当前浏览器不支持录音，请改用文字输入')
    return
  }
  try {
    speechInputBase = inputText.value.trim()
    skipNextTranscription = false
    recordedChunks = []
    mediaStream = useMobileAudioApi
      ? await requestMobileAudioStream({ audio: true })
      : await navigator.mediaDevices.getUserMedia({ audio: true })
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

function handleQuoteCustomerAttachment(payload: { followUp: FollowUp; attachment: FollowUpAttachment }) {
  const fileName = payload.attachment.fileName || '附件'
  const time = formatAttachmentQuoteTime(payload.attachment.analysisTime || payload.followUp.followTime || payload.followUp.createTime)
  const quoteText = `引用附件：${fileName}${time ? `（${time}）` : ''}`
  inputText.value = inputText.value.trim()
    ? `${inputText.value.trimEnd()}\n${quoteText}`
    : quoteText
  focusChatTextarea()
}

function formatAttachmentQuoteTime(value?: string): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function shouldShowModelMultiplier(option: ChatModelOption): boolean {
  return option.modelSource !== 'custom'
}

function formatModelMultiplier(value?: number | null) {
  const multiplier = Number(value || 1)
  return `${multiplier.toFixed(multiplier % 1 === 0 ? 0 : 2)}x 积分`
}

function renderAssistantMessage(content: string, isStreaming = false, isThinking = false): string {
  const normalized = normalizeAssistantMessageContent(content, isStreaming)
  const html = renderMarkdown(normalized || (isThinking ? getAssistantMessagePlaceholder(true) : ''), {
    streaming: isStreaming
  })
  return wrapMarkdownHeadingLeadingEmoji(html)
}

const HEADING_LEADING_EMOJI_PATTERN = /^([\u2600-\u27BF]\uFE0F?|\p{Extended_Pictographic}\uFE0F?)/u

function wrapMarkdownHeadingLeadingEmoji(html: string): string {
  if (!html.includes('<h')) return html

  const template = document.createElement('template')
  template.innerHTML = html

  template.content.querySelectorAll('h1,h2,h3,h4,h5,h6').forEach(heading => {
    const textNode = findFirstNonEmptyTextNode(heading)
    if (!textNode) return

    const text = textNode.data
    const leadingWhitespaceLength = text.search(/\S/)
    const contentStart = leadingWhitespaceLength < 0 ? 0 : leadingWhitespaceLength
    const content = text.slice(contentStart)
    const match = content.match(HEADING_LEADING_EMOJI_PATTERN)
    if (!match) return

    const icon = match[0]
    const iconSpan = document.createElement('span')
    iconSpan.className = 'wk-markdown-heading-icon'
    iconSpan.textContent = icon

    const fragment = document.createDocumentFragment()
    if (contentStart > 0) {
      fragment.appendChild(document.createTextNode(text.slice(0, contentStart)))
    }
    fragment.appendChild(iconSpan)
    fragment.appendChild(document.createTextNode(content.slice(icon.length)))
    textNode.replaceWith(fragment)
  })

  return template.innerHTML
}

function findFirstNonEmptyTextNode(root: Element): Text | null {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  while (walker.nextNode()) {
    const node = walker.currentNode as Text
    if (node.data.trim()) return node
  }
  return null
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
  skipComposerFocusOnUploadMenuClose = true
  chatUploadMenuVisible.value = false
  chatKnowledgePickerVisible.value = true
  void nextTick(() => {
    skipComposerFocusOnUploadMenuClose = false
  })
}

function handleChatUploadMenuSelectApp(appCode: string) {
  if (isMobile.value) {
    chatUploadMenuVisible.value = false
  }
  chatStore.setSelectedAppCode(chatStore.selectedAppCode === appCode ? 'general' : appCode)
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
  if (chatStore.selectedAppCode === 'general') {
    chatStore.setSelectedAppCode('knowledge')
  }
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

function openMobileMainMenu() {
  appEvents.emit(APP_EVENT.MOBILE_MAIN_MENU_OPEN)
}

async function handleNewSession() {
  isPinnedToBottom.value = true
  closeMobileCustomerSummary()
  if (route.query.customerId || route.query.sessionId) {
    await router.replace({ path: '/chat' })
  }
  chatStore.beginNewSessionDraft('新对话')
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
  chatStore.requestComposerFocus()
}

async function handleSelectSession(sessionId: string) {
  // if (chatStore.currentSessionId === sessionId && currentView.value === 'chat') return
  isPinnedToBottom.value = true
  closeMobileCustomerSummary()
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
  await chatStore.selectSession(sessionId)
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

async function applyCustomerRouteQuery() {
  const raw = route.query.customerId
  const customerId = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  if (!customerId || selectedCustomerId.value === customerId) return
  await handleSelectCustomerById(customerId)
}

watch(
  () => route.query.customerId,
  () => {
    void applyCustomerRouteQuery()
  },
  { immediate: true }
)

watch(
  currentSessionCustomerId,
  (customerId) => {
    if (!customerId) {
      clearChatCustomerAiPolling()
      selectedCustomerId.value = null
      selectedCustomer.value = null
      selectedCustomerLoading.value = false
      return
    }

    clearChatCustomerAiPolling()
    if (selectedCustomerId.value === customerId && selectedCustomer.value) return
    selectedCustomerId.value = customerId
    void ensureSelectedCustomerDetail(customerId)
  },
  { immediate: true }
)

watch(
  () => [currentSessionCustomerId.value, selectedCustomer.value?.aiAnalysisStatus || ''] as const,
  ([customerId, status], previousValue) => {
    const previousCustomerId = previousValue?.[0] || ''
    const previousStatus = previousValue?.[1] || ''
    if (!customerId) {
      clearChatCustomerAiPolling()
      return
    }
    if (status === 'pending' || status === 'running') {
      scheduleChatCustomerAiPolling(
        customerId,
        customerId !== previousCustomerId || (previousStatus !== 'pending' && previousStatus !== 'running')
      )
      return
    }
    clearChatCustomerAiPolling()
  }
)

function isSessionActive(sessionId: string): boolean {
  return currentView.value === 'chat' && chatStore.currentSessionId === sessionId
}

async function handleDeleteSession(sessionId: string) {
  try {
    await confirmDeleteChatSession()
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

function getCustomerStageButtonClass(stage?: string): string {
  const map: Record<string, string> = {
    lead: 'bg-[var(--wk-bg-surface-hover)] text-[var(--wk-text-primary)] hover:bg-[var(--wk-bg-surface-active)]',
    qualified: 'bg-[var(--wk-bg-surface-hover)] text-[var(--wk-text-primary)] hover:bg-[var(--wk-bg-surface-active)]',
    proposal: 'bg-[#f8eadf] text-[#8d4f34] hover:bg-[#f3ddcf]',
    negotiation: 'bg-[#f8eadf] text-[#8d4f34] hover:bg-[#f3ddcf]',
    closed: 'bg-[#e8efe6] text-[#46643e] hover:bg-[#dfe9dc]',
    lost: 'bg-[var(--wk-bg-surface-muted)] text-[var(--wk-text-muted)] hover:bg-[var(--wk-bg-surface-active)]',
  }
  return map[stage || ''] || 'bg-[var(--wk-bg-surface-muted)] text-[var(--wk-text-muted)] hover:bg-[var(--wk-bg-surface-active)]'
}

function getCustomerStageIcon(stage?: string): string {
  const map: Record<string, string> = {
    lead: 'person_search',
    qualified: 'verified',
    proposal: 'description',
    negotiation: 'forum',
    closed: 'handshake',
    lost: 'block',
  }
  return map[stage || ''] || 'lens'
}

async function handleCustomerStageCommand(stage: string) {
  if (!selectedCustomer.value || selectedCustomer.value.stage === stage) return
  try {
    await updateCustomerStage(selectedCustomer.value.customerId, stage)
    await ensureSelectedCustomerDetail(selectedCustomer.value.customerId)
    ElMessage.success('客户阶段已更新')
  } catch (err) {
    console.error('Update customer stage failed:', err)
    if (!isRequestErrorHandled(err)) ElMessage.error('客户阶段更新失败')
  }
}

async function handleAddSelectedCustomerTag() {
  if (!canEditSelectedCustomerTags.value || !selectedCustomer.value) return
  const tagName = newSelectedCustomerTagName.value.trim()
  if (!tagName) return
  selectedCustomerTagSubmitting.value = true
  try {
    await addCustomerTag(selectedCustomer.value.customerId, tagName)
    await ensureSelectedCustomerDetail(selectedCustomer.value.customerId)
    showSelectedCustomerTagDialog.value = false
    newSelectedCustomerTagName.value = ''
    ElMessage.success('标签添加成功')
  } catch (err) {
    console.error('Add customer tag failed:', err)
    if (!isRequestErrorHandled(err)) ElMessage.error('标签添加失败')
  } finally {
    selectedCustomerTagSubmitting.value = false
  }
}

async function handleRemoveSelectedCustomerTag(tag: CustomerTag) {
  if (!canEditSelectedCustomerTags.value || !selectedCustomer.value) return
  try {
    await removeCustomerTag(selectedCustomer.value.customerId, tag.tagId)
    await ensureSelectedCustomerDetail(selectedCustomer.value.customerId)
    ElMessage.success('标签已删除')
  } catch (err) {
    console.error('Remove customer tag failed:', err)
    if (!isRequestErrorHandled(err)) ElMessage.error('标签删除失败')
  }
}

function handleSelectedCustomerContactsUpdated(contacts: Contact[]) {
  if (!selectedCustomer.value) return
  selectedCustomer.value = {
    ...selectedCustomer.value,
    contacts
  }
}

function handleSelectedCustomerBasicInfoEdit() {
  if (!canEditSelectedCustomerTags.value || !selectedCustomer.value) return
  showSelectedCustomerBasicInfoDrawer.value = false
  showSelectedCustomerEditDialog.value = true
}

async function handleSelectedCustomerEditSuccess(payload: { mode: 'create' | 'edit'; customerId?: string; detail?: CustomerDetailVO | null }) {
  if (payload.mode !== 'edit') return
  const customerId = payload.customerId || selectedCustomer.value?.customerId || selectedCustomerId.value
  if (payload.detail) {
    selectedCustomer.value = payload.detail
  } else if (customerId) {
    await ensureSelectedCustomerDetail(String(customerId), { silent: Boolean(selectedCustomer.value) })
  }
  await chatStore.fetchSessions()
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
}

function _formatTime(date: Date): string {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

void _getAssistantMessageStatus
void _formatTime
void quickActions
void sendQuickMessage

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

.wk-chat-messages {
  --wk-chat-messages-scrollbar-offset: 0px;
  /* background: var(--wk-bg-page); */
  overflow-x: hidden;
}

.wk-chat-shell {
  background:
    linear-gradient(180deg, var(--wk-bg-surface) 0%, rgb(var(--wk-bg-page-rgb) / 0.96) 62%, var(--wk-bg-page) 100%);
}

.wk-chat-customer-header {
  border-color: var(--wk-border-subtle);
  background: color-mix(in srgb, var(--wk-bg-surface) 92%, transparent);
  backdrop-filter: blur(14px);
}

.wk-chat-composer-wrap {
  background: linear-gradient(to top, var(--wk-bg-page) 72%, rgb(var(--wk-bg-page-rgb) / 0));
}

.wk-chat-composer {
  border: 1px solid var(--wk-border-subtle);
  background: var(--wk-bg-surface);
  box-shadow:
    0 20px 70px rgb(var(--wk-shadow-color) / 0.08),
    0 2px 8px rgb(var(--wk-shadow-color) / 0.05);
}

.wk-chat-composer:focus-within {
  border-color: var(--wk-border-muted);
  box-shadow:
    0 22px 78px rgb(var(--wk-shadow-color) / 0.11),
    0 0 0 1px rgb(var(--wk-primary-rgb) / 0.12);
}

.wk-mobile-chat-floating-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.wk-mobile-chat-menu-fab {
  display: inline-flex;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(255 255 255 / 0.78);
  border-radius: 9999px;
  background: rgb(255 255 255 / 0.72);
  color: #0d0d0d;
  box-shadow:
    0 16px 42px rgb(15 23 42 / 0.14),
    inset 0 1px 0 rgb(255 255 255 / 0.92);
  backdrop-filter: blur(18px);
  transition: background-color 140ms ease, transform 140ms ease;
}

.wk-mobile-chat-menu-fab:active {
  transform: scale(0.94);
}

.wk-mobile-chat-menu-fab:hover {
  background: rgb(255 255 255 / 0.88);
}

.wk-mobile-chat-actions {
  display: inline-flex;
  height: 42px;
  min-width: 116px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 0.75);
  border-radius: 9999px;
  background: rgb(255 255 255 / 0.5);
  box-shadow:
    0 18px 50px rgb(15 23 42 / 0.12),
    inset 0 1px 0 rgb(255 255 255 / 0.9);
  backdrop-filter: blur(18px);
}

.wk-mobile-chat-actions--single {
  min-width: 58px;
}

.wk-mobile-chat-actions__btn {
  display: inline-flex;
  width: 56px;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #0d0d0d;
  transition: background-color 140ms ease, transform 140ms ease;
}

.wk-mobile-chat-actions__btn:active {
  transform: scale(0.94);
}

.wk-mobile-chat-actions__btn:hover {
  background: rgb(13 13 13 / 0.05);
}

.wk-mobile-chat-actions__divider {
  width: 1px;
  height: 22px;
  background: rgb(13 13 13 / 0.08);
}

.wk-mobile-composer-row {
  min-height: 40px;
}

.wk-mobile-composer-icon-button {
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  color: #0d0d0d;
  transition: background-color 140ms ease, color 140ms ease, transform 140ms ease;
}

.wk-mobile-composer-icon-button:hover {
  background: var(--wk-bg-surface-hover);
}

.wk-mobile-composer-icon-button:active {
  transform: scale(0.94);
}

.wk-mobile-composer-icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.wk-mobile-model-trigger--icon {
  border-width: 1px;
  border-style: solid;
  padding: 0;
}

.wk-mobile-composer-textarea {
  min-height: 40px;
  max-height: 184px;
  scrollbar-gutter: stable;
}

.wk-mobile-selected-app-row {
  padding: 0 2px 2px;
}

.wk-mobile-customer-summary {
  position: fixed;
  inset: 0;
  z-index: 3400;
  pointer-events: auto;
}

.wk-mobile-customer-summary__backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  border: 0;
  background: rgb(0 0 0 / 0.18);
  backdrop-filter: blur(1px);
}

.wk-mobile-customer-summary__sheet {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 28px 28px 0 0;
  background: var(--wk-bg-surface);
  box-shadow: 0 -18px 55px rgb(15 23 42 / 0.18);
  transition: height 220ms cubic-bezier(0.22, 1, 0.36, 1), transform 220ms cubic-bezier(0.22, 1, 0.36, 1);
}

.wk-mobile-customer-summary__sheet.is-dragging {
  transition: none;
}

.wk-mobile-customer-summary__header {
  flex-shrink: 0;
  padding: 10px 16px 12px;
  border-bottom: 1px solid var(--wk-border-subtle);
  background: color-mix(in srgb, var(--wk-bg-surface) 96%, transparent);
  touch-action: none;
  user-select: none;
}

.wk-mobile-customer-summary__handle {
  display: block;
  width: 44px;
  height: 5px;
  margin: 0 auto 10px;
  border-radius: 9999px;
  background: #d1d5db;
}

.wk-mobile-customer-summary__title-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.wk-mobile-customer-summary__title {
  min-width: 0;
  overflow: hidden;
  color: var(--wk-text-primary);
  font-size: 17px;
  font-weight: 700;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wk-mobile-customer-summary__subtitle {
  margin-top: 2px;
  color: var(--wk-text-muted);
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
}

.wk-mobile-customer-summary__close {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: #f1f1f1;
  color: #0d0d0d;
  transition: background-color 140ms ease, transform 140ms ease;
}

.wk-mobile-customer-summary__close:active {
  transform: scale(0.94);
}

.wk-mobile-customer-summary__body {
  min-height: 0;
  flex: 1;
  overflow: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  padding-bottom: max(20px, env(safe-area-inset-bottom));
}

.wk-mobile-customer-summary__sheet.has-voice-action .wk-mobile-customer-summary__body {
  padding-bottom: max(92px, calc(env(safe-area-inset-bottom) + 92px));
}

.wk-mobile-customer-summary__voice {
  position: absolute;
  right: 18px;
  bottom: max(22px, env(safe-area-inset-bottom));
  z-index: 2;
  display: inline-flex;
  height: 52px;
  align-items: center;
  gap: 8px;
  border-radius: 9999px;
  background: rgb(255 255 255 / 0.92);
  padding: 0 18px;
  color: #0d0d0d;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 16px 42px rgb(15 23 42 / 0.18);
  backdrop-filter: blur(14px);
  transition: transform 160ms ease, background-color 160ms ease;
}

.wk-mobile-customer-summary__voice:active {
  transform: scale(0.96);
}

.wk-mobile-customer-summary-fade-enter-active,
.wk-mobile-customer-summary-fade-leave-active {
  transition: opacity 180ms ease;
}

.wk-mobile-customer-summary-fade-enter-from,
.wk-mobile-customer-summary-fade-leave-to {
  opacity: 0;
}

.wk-mobile-customer-summary-fade-enter-active .wk-mobile-customer-summary__sheet,
.wk-mobile-customer-summary-fade-leave-active .wk-mobile-customer-summary__sheet {
  transition: transform 220ms cubic-bezier(0.22, 1, 0.36, 1), height 220ms cubic-bezier(0.22, 1, 0.36, 1);
}

.wk-mobile-customer-summary-fade-enter-from .wk-mobile-customer-summary__sheet,
.wk-mobile-customer-summary-fade-leave-to .wk-mobile-customer-summary__sheet {
  transform: translateY(100%);
}

.wk-mobile-summary-voice-enter-active,
.wk-mobile-summary-voice-leave-active {
  transition: opacity 150ms ease, transform 150ms ease;
}

.wk-mobile-summary-voice-enter-from,
.wk-mobile-summary-voice-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.96);
}

.wk-recording-indicator {
  position: relative;
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
}

.wk-recording-indicator::before {
  position: absolute;
  inset: -4px;
  border: 1px solid rgb(255 255 255 / 0.55);
  border-radius: 9999px;
  content: "";
  transition: opacity 0.14s ease;
  animation: wk-recording-pulse 1.2s ease-out infinite;
}

.wk-recording-indicator__stop {
  position: relative;
  font-size: 20px;
  line-height: 1;
  opacity: 0;
  transform: scale(0.84);
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.wk-recording-indicator__bars {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.wk-recording-indicator__bars span {
  display: block;
  width: 2px;
  border-radius: 9999px;
  background: #fff;
  animation: wk-recording-bar 0.72s ease-in-out infinite;
}

.wk-recording-indicator__bars span:nth-child(1) {
  height: 7px;
}

.wk-recording-indicator__bars span:nth-child(2) {
  height: 13px;
  animation-delay: 0.12s;
}

.wk-recording-indicator__bars span:nth-child(3) {
  height: 10px;
  animation-delay: 0.24s;
}

.wk-recording-indicator__bars span:nth-child(4) {
  height: 15px;
  animation-delay: 0.36s;
}

.group\/send-bar-action:hover .wk-recording-indicator::before {
  opacity: 0;
  animation-play-state: paused;
}

.group\/send-bar-action:hover .wk-recording-indicator__stop {
  opacity: 1;
  transform: scale(1);
}

.group\/send-bar-action:hover .wk-recording-indicator__bars {
  opacity: 0;
  transform: scaleX(0.7);
}

.group\/send-bar-action:hover .wk-recording-indicator__bars span {
  animation-play-state: paused;
}

.wk-chat-customer-panel {
  border-color: var(--wk-border-subtle);
  background: var(--wk-bg-surface);
}

.wk-chat-messages__inner {
  min-width: 0;
  width: 100%;
}

.wk-chat-messages--scrollable .wk-chat-messages__inner {
  width: calc(100% + var(--wk-chat-messages-scrollbar-offset));
}

.wk-chat-message {
  width: min(100%, 768px);
  min-width: min(100%, 468px);
  max-width: 768px;
  overflow-wrap: anywhere;
}

.wk-chat-message--user :deep(.rounded-\[24px\]) {
  background-color: var(--wk-bg-surface-muted) !important;
  color: var(--wk-text-primary) !important;
}

.wk-chat-message--assistant :deep(.wk-markdown) {
  color: var(--wk-text-primary);
  font-size: 15px;
  line-height: 1.75;
}

.wk-chat-message--assistant :deep(.wk-markdown > *:first-child) {
  margin-top: 0;
}

.wk-chat-message--assistant :deep(.wk-markdown > *:last-child) {
  margin-bottom: 0;
}

.wk-chat-message--assistant :deep(.wk-markdown p) {
  margin: 0 0 0.85em;
}

.wk-chat-message--assistant :deep(.wk-markdown-heading-icon) {
  display: inline-block;
  margin-right: 0.18em;
  font-size: 0.72em;
  line-height: 1;
  vertical-align: 0.04em;
}

.wk-chat-message--assistant :deep(.wk-markdown ul),
.wk-chat-message--assistant :deep(.wk-markdown ol) {
  margin: 0.85em 0;
  padding-left: 1.5em;
}

.wk-chat-message--assistant :deep(.wk-markdown li) {
  margin: 0.25em 0;
}

.wk-chat-message--assistant :deep(.wk-markdown pre) {
  margin: 1em 0;
  overflow-x: auto;
  border-radius: 12px;
  background: #1f1e1c;
  padding: 14px 16px;
  color: #f7f7f7;
}

:global(html.dark) .wk-chat-message--assistant :deep(.wk-markdown pre) {
  background: #030712;
  color: #e5edf8;
}

.wk-chat-message--assistant :deep(.wk-markdown code) {
  border-radius: 6px;
  background: var(--wk-bg-surface-muted);
  padding: 0.15em 0.35em;
  color: var(--wk-text-primary);
  font-size: 0.92em;
}

.wk-chat-message--assistant :deep(.wk-markdown pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.wk-chat-message--assistant :deep(.wk-markdown blockquote) {
  margin: 1em 0;
  border-left: 3px solid var(--wk-border-strong);
  padding-left: 1em;
  color: var(--wk-text-secondary);
}

.wk-chat-message--assistant :deep(.wk-markdown table) {
  width: 100%;
  margin: 1em 0;
  border-collapse: collapse;
  font-size: 14px;
}

.wk-chat-message--assistant :deep(.wk-markdown th),
.wk-chat-message--assistant :deep(.wk-markdown td) {
  border: 1px solid var(--wk-border-subtle);
  padding: 8px 10px;
  text-align: left;
}

.wk-chat-message--assistant :deep(.wk-markdown th) {
  background: var(--wk-bg-surface-subtle);
  font-weight: 600;
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

@keyframes wk-recording-pulse {
  0% {
    opacity: 0.8;
    transform: scale(0.72);
  }
  80%,
  100% {
    opacity: 0;
    transform: scale(1.18);
  }
}

@keyframes wk-recording-bar {
  0%,
  100% {
    transform: scaleY(0.55);
  }
  50% {
    transform: scaleY(1);
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

.wk-chat-upload-menu--mobile {
  padding: 12px 8px max(12px, env(safe-area-inset-bottom));
}

.wk-chat-upload-menu__mobile-handle {
  width: 44px;
  height: 5px;
  margin: 0 auto 10px;
  border-radius: 999px;
  background: #d1d5db;
}

.wk-chat-upload-menu__item {
  width: 100%;
  display: flex;
  height: 36px;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 8px;
  color: var(--wk-text-primary);
  transition: background-color 150ms ease, color 150ms ease;
}

.wk-chat-upload-menu__item:hover {
  background: var(--wk-bg-surface-hover);
}

.wk-chat-upload-menu__item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.wk-chat-upload-menu--mobile .wk-chat-upload-menu__item {
  height: 52px;
  gap: 14px;
  padding: 8px 12px;
  border-radius: 14px;
}

.wk-chat-upload-menu__icon {
  font-size: 18px;
  line-height: 1;
  color: var(--wk-text-primary);
}

.wk-chat-upload-menu--mobile .wk-chat-upload-menu__icon {
  font-size: 22px;
}

.wk-chat-upload-menu__label {
  font-size: 14px;
  line-height: 1.2;
  font-weight: 400;
}

.wk-chat-upload-menu--mobile .wk-chat-upload-menu__label {
  font-size: 14px;
  font-weight: 600;
}

.wk-chat-upload-menu__label--grow {
  flex: 1 1 auto;
  min-width: 0;
  text-align: left;
}

.wk-chat-upload-menu__apps-ref {
  width: 100%;
  height: 36px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 8px;
  color: var(--wk-text-primary);
  cursor: default;
  outline: none;
  transition: background-color 150ms ease;
}

.wk-chat-upload-menu__apps-ref:hover {
  background: var(--wk-bg-surface-hover);
}

.wk-chat-upload-menu__chevron {
  margin-left: auto;
  font-size: 18px;
  color: var(--wk-text-muted);
}

.wk-chat-upload-submenu {
  padding: 10px;
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
.wk-ai-quota-choice-dialog .el-dialog__header {
  height: 40px;
  padding: 0 !important;
}

.wk-ai-quota-choice-dialog .el-dialog__headerbtn {
  top: 4px !important;
  right: 12px !important;
  width: 32px;
  height: 32px;
}

.wk-ai-quota-choice-dialog .el-dialog__body {
  padding-top: 1.5rem !important;
}

/* Popover 挂载到 body，scoped 选不中外层 .wk-chat-upload-menu-popper */
.wk-chat-upload-menu-popper.el-popper {
  padding: 0 !important;
  border-radius: 16px !important;
  overflow: hidden;
  border: 1px solid var(--wk-border-subtle) !important;
  background: var(--wk-bg-surface) !important;
  box-shadow: 0 12px 36px rgb(var(--wk-shadow-color) / 0.28) !important;
  /* box-shadow: 0 12px 40px rgba(15, 23, 42, 0.14); */
}

/* 主菜单（不含子菜单类名）在更底层，子菜单气泡叠在上面；overflow: visible 避免非 teleport 子菜单被裁切 */
.wk-chat-upload-menu-popper.el-popper:not(.wk-chat-upload-submenu-popper) {
  z-index: 3000 !important;
  overflow: visible !important;
}

.wk-chat-upload-menu-popper--mobile.el-popper {
  position: fixed !important;
  left: 16px !important;
  right: 16px !important;
  bottom: max(16px, env(safe-area-inset-bottom)) !important;
  top: auto !important;
  width: auto !important;
  max-width: none !important;
  transform: none !important;
  border-radius: 24px !important;
  box-shadow: 0 18px 48px rgb(var(--wk-shadow-color) / 0.32) !important;
}

.wk-chat-mobile-sheet-enter-active,
.wk-chat-mobile-sheet-leave-active {
  transform-origin: center bottom;
  transition:
    transform 220ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 180ms ease;
}

.wk-chat-mobile-sheet-enter-from,
.wk-chat-mobile-sheet-leave-to {
  opacity: 0;
  transform: translateY(calc(100% + 24px)) !important;
}

.wk-chat-mobile-sheet-enter-to,
.wk-chat-mobile-sheet-leave-from {
  opacity: 1;
  transform: translateY(0) !important;
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
  border-radius: 8px !important;
  overflow: hidden;
  border: 1px solid var(--wk-border-subtle) !important;
  background: var(--wk-bg-surface) !important;
  box-shadow: 0 12px 36px rgb(var(--wk-shadow-color) / 0.28) !important;
  z-index: 3000 !important;
}

.wk-chat-model-popper .el-popper__arrow,
.wk-chat-model-popper .el-popper__arrow::before {
  display: none !important;
}

.wk-chat-model-menu {
  padding: 6px;
  max-height: min(52vh, 360px);
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.wk-chat-model-menu__group-label {
  padding: 8px 10px 4px;
  font-size: 12px;
  line-height: 16px;
  font-weight: 600;
  color: var(--wk-text-muted);
}

.wk-chat-model-menu__item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 5px;
  border-radius: 8px;
  border: none;
  background: transparent;
  padding: 2px 10px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.12s ease;
}

.wk-chat-model-menu__item:hover {
  background: var(--wk-bg-surface-hover);
}

.wk-chat-model-menu__more {
  display: flex;
  width: calc(100% - 4px);
  align-items: center;
  gap: 8px;
  margin: 4px 2px 6px;
  border-radius: 8px;
  border: 0px solid var(--wk-border-subtle);
  background: var(--wk-bg-surface-subtle);
  padding: 8px 10px;
  color: var(--wk-primary);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.12s ease, border-color 0.12s ease;
}

.wk-chat-model-menu__more:hover {
  border-color: color-mix(in srgb, var(--wk-primary) 24%, var(--wk-border-subtle));
  background: color-mix(in srgb, var(--wk-primary) 8%, var(--wk-bg-surface));
}

@media (max-width: 767px) {
  :global(.wk-chat-model-select-popper.el-popper) {
    left: 12px !important;
    right: 12px !important;
    width: auto !important;
    min-width: 0 !important;
    max-width: none !important;
    border-radius: 8px !important;
    overflow: hidden;
  }

  :global(.wk-chat-model-select-popper .el-select-dropdown) {
    max-width: 100%;
    border-radius: 8px;
  }

  :global(.wk-chat-model-select-popper .el-select-dropdown__item) {
    padding-right: 12px;
  }

  :global(.wk-chat-model-select-popper .el-popper__arrow) {
    display: none !important;
  }
}


</style>
