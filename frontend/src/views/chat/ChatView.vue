<template>
  <div class="flex h-full" :class="{ 'flex-col': isMobile }">
    <Teleport to="body">
      <div
        v-if="showMobileTopViewportShield"
        class="wk-mobile-chat-top-viewport-shield"
        :style="mobileTopViewportShieldStyle"
        aria-hidden="true"
      ></div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showMobileFloatingBar"
        class="wk-mobile-chat-floating-bar pointer-events-none px-4 py-3"
        :style="mobileTopFixedLayerStyle"
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
          </div>
        </div>
      </div>
    </Teleport>

    <MobileChatTopHeader
      :visible="showMobileObjectHeader"
      :kind="mobileChatHeaderKind"
      :title="mobileChatHeaderTitle"
      :avatar-url="mobileChatHeaderAvatarUrl"
      :fixed-style="mobileTopFixedLayerStyle"
      @menu="openMobileMainMenu"
      @title="handleMobileHeaderTitle"
      @detail="openMobileObjectDetail"
    />

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
      class="wk-chat-shell flex-1 min-w-0 flex flex-col relative overflow-hidden"
    >
      <!-- Chat View -->
      <template v-if="currentView === 'chat'">
        <div
          class="flex-1 flex flex-col overflow-hidden"
          :class="isCenteredEmptyChat && !isMobile ? 'justify-center -translate-y-[100px]' : ''"
        >
          <div
            v-if="showDesktopCustomerHeader"
            class="wk-chat-customer-header relative z-20 shrink-0 border-b border-[#ececec] bg-white py-2 pl-4 pr-4 md:pl-8"
          >
            <div class="flex h-9 w-full items-center justify-between gap-3">
              <div class="flex min-w-0 flex-1 items-center gap-2">
                <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
                  <img
                    v-if="customerHeaderLogoUrl"
                    :src="customerHeaderLogoUrl"
                    :alt="customerHeaderTitle || 'company logo'"
                    class="size-full bg-white object-contain"
                  />
                  <span v-else class="text-xs font-bold text-slate-400">
                    {{ customerHeaderTitle.charAt(0) || '?' }}
                  </span>
                </div>
                <h2
                  class="min-w-[80px] max-w-[220px] truncate text-[15px] font-semibold leading-5 text-[#0d0d0d]"
                  :title="customerHeaderTitle"
                >
                  {{ customerHeaderTitle }}
                </h2>
                <span
                  v-if="selectedCustomerLoading"
                  class="material-symbols-outlined shrink-0 animate-spin text-[16px] leading-none text-slate-300"
                >
                  progress_activity
                </span>
                <div
                  v-if="selectedCustomer && (selectedCustomer.tags?.length || canEditSelectedCustomerTags)"
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
                      <span class="inline-flex h-6 shrink-0 cursor-default items-center rounded-lg bg-[var(--wk-bg-surface-muted)] px-2 text-[11px] font-medium text-[var(--wk-text-muted)]">
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
              <div class="flex shrink-0 items-center gap-2">
                <button
                  type="button"
                  class="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-[8px] bg-primary px-2.5 text-[12px] font-semibold text-white shadow-sm shadow-primary/20 transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="!selectedCustomer"
                  aria-label="基本信息"
                  @click="showSelectedCustomerBasicInfoDrawer = true"
                >
                  <span class="material-symbols-outlined text-[15px] leading-none">description</span>
                  <span>基本信息</span>
                </button>
                <el-dropdown
                  v-if="selectedCustomer && canChangeSelectedCustomerStage"
                  trigger="click"
                  @command="handleSelectedCustomerStageCommand"
                >
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
                        v-for="stage in customerStageOptions"
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
                <span
                  v-else-if="selectedCustomer"
                  class="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-[8px] px-2.5 text-[12px] font-semibold"
                  :class="selectedCustomerStageButtonClass"
                >
                  <span class="material-symbols-outlined text-[15px] leading-none">
                    {{ getCustomerStageIcon(selectedCustomer.stage) }}
                  </span>
                  <span>{{ selectedCustomerStageText }}</span>
                </span>
              </div>
            </div>
          </div>

          <div
            v-else-if="showDesktopObjectHeader"
            class="wk-chat-customer-header relative z-20 shrink-0 border-b border-[#ececec] bg-white py-2 pl-4 pr-4 md:pl-8"
          >
            <div class="flex h-9 w-full items-center justify-between gap-3">
              <div class="flex min-w-0 flex-1 items-center gap-2">
                <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
                  <img
                    v-if="desktopObjectHeaderAvatarUrl"
                    :src="desktopObjectHeaderAvatarUrl"
                    :alt="desktopObjectHeaderTitle"
                    :class="desktopObjectHeaderImageClass"
                  />
                  <span
                    v-else-if="desktopObjectHeaderIcon"
                    class="material-symbols-outlined text-[17px] leading-none text-slate-400"
                  >
                    {{ desktopObjectHeaderIcon }}
                  </span>
                  <span v-else class="text-xs font-bold text-slate-400">
                    {{ desktopObjectHeaderTitle.charAt(0) || '?' }}
                  </span>
                </div>
                <h2
                  class="min-w-[80px] max-w-[220px] truncate text-[15px] font-semibold leading-5 text-[#0d0d0d]"
                  :title="desktopObjectHeaderTitle"
                >
                  {{ desktopObjectHeaderTitle }}
                </h2>
                <span class="inline-flex h-6 shrink-0 items-center rounded-lg bg-[var(--wk-bg-surface-muted)] px-2 text-[11px] font-medium text-[var(--wk-text-secondary)]">
                  {{ desktopObjectHeaderBadge }}
                </span>
                <span v-if="desktopObjectHeaderMeta" class="hidden min-w-0 truncate text-xs text-slate-400 md:inline">
                  {{ desktopObjectHeaderMeta }}
                </span>
              </div>
            </div>
          </div>

          <!-- Messages Area -->
          <div
            ref="messagesContainer"
            class="wk-chat-messages scroll-smooth"
            :class="[
              chatMessagesAreaClass,
              {
                'wk-chat-messages--empty-chat': isCenteredEmptyChat,
                'wk-chat-messages--mobile-header': showMobileTopChrome,
                'wk-chat-messages--mobile-floating-actions': showMobileFloatingBar
              }
            ]"
            @scroll="handleMessagesScroll"
            @touchmove.passive="dismissMobileKeyboardForConversationScroll"
          >
            <div class="wk-chat-messages__inner px-4 md:px-8">
            <!-- Welcome Section (no messages) -->
            <template v-if="isChatEmpty && !isObjectContextChat">
              <div class="wk-chat-empty-welcome mx-auto flex max-w-3xl flex-col items-center space-y-5 py-6 text-center">
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
              </div>
            </template>

            <!-- Messages -->
            <template v-else>
              <div
                v-for="message in chatStore.messages"
                :key="message.id"
                class="wk-chat-message mx-auto message-enter"
                :class="message.role === 'user' ? 'wk-chat-message--user' : 'wk-chat-message--assistant'"
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
                  <div class="flex-1 space-y-3 min-w-0">
                    <div class="max-w-full text-left text-[16px] leading-7 text-[#0d0d0d]">
                      <div
                        class="wk-markdown"
                        :class="{ 'wk-thinking-shimmer': message.isThinking && message.content.length === 0 }"
                        v-html="renderAssistantMessage(message.content, Boolean(message.isStreaming), Boolean(message.isThinking))"
                      />
                      <div v-if="message.isThinking && message.content.length > 0" class="wk-thinking-shimmer mt-1 text-sm">
                        <p>思考中</p>
                      </div>
                    </div>
                    <!-- Attachments -->
                    <div v-if="getInlineAttachments(message).length > 0" class="space-y-2">
                      <div v-for="att in getInlineAttachments(message)" :key="att.id || att.fileName">
                        <el-image
                          :src="att.accessUrl"
                          :preview-src-list="[att.accessUrl]"
                          fit="cover"
                          class="rounded-xl max-h-[220px] border border-[#e5e5e5]"
                          :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                          lazy
                        />
                        <div class="mt-1 text-xs text-[#8f8f8f]">{{ att.fileName }}</div>
                      </div>
                    </div>
                    <div class="wk-chat-message-actions wk-chat-message-actions--assistant flex h-8 items-center gap-2">
                      <button
                        v-if="!message.isStreaming"
                        type="button"
                        class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-all hover:bg-[#f1f1f1] hover:text-[#0d0d0d]"
                        aria-label="复制内容"
                        title="复制内容"
                        @click="copyMessageContent(message, 'assistant')"
                      >
                        <WkIcon name="copy" :box-size="18" class="shrink-0 leading-none" />
                      </button>
                      <span
                        class="text-xs font-medium"
                        :class="message.isStreaming ? 'text-primary/70' : 'text-slate-400'"
                      >
                        {{ formatTime(message.timestamp) }}
                      </span>
                    </div>
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
                    <div class="rounded-[24px] bg-[#f4f4f4] px-4 py-2.5 text-[15px] leading-7 text-[#0d0d0d]">
                      <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                    </div>
                    <!-- User Attachments -->
                    <div v-if="getInlineAttachments(message).length > 0" class="flex flex-col items-end gap-2">
                      <div v-for="att in getInlineAttachments(message)" :key="att.id || att.fileName">
                        <el-image
                          :src="att.accessUrl"
                          :preview-src-list="[att.accessUrl]"
                          fit="cover"
                          class="rounded-xl max-h-[220px] border border-[#e5e5e5]"
                          :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                          lazy
                        />
                      </div>
                    </div>
                    <div class="wk-chat-message-actions wk-chat-message-actions--user flex h-8 w-full basis-full items-center justify-end gap-2">
                      <span class="text-xs font-medium text-slate-400">{{ formatTime(message.timestamp) }}</span>
                      <button
                        type="button"
                        class="pointer-events-none flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] opacity-0 transition-all hover:bg-[#f1f1f1] hover:text-[#0d0d0d] group-hover:pointer-events-auto group-hover:opacity-100"
                        aria-label="复制内容"
                        title="复制内容"
                        @click="copyMessageContent(message, 'user')"
                      >
                        <WkIcon name="copy" :box-size="18" class="shrink-0 leading-none" />
                      </button>
                    </div>
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
              class="wk-scroll-to-bottom-button absolute left-1/2 z-20 flex items-center justify-center rounded-full border bg-white text-slate-600 transition-all hover:bg-slate-50 hover:text-slate-900"
              :class="isMobile ? 'bottom-[118px]' : 'bottom-[196px]'"
              aria-label="滚动到底部"
              title="滚动到底部"
              @click="scrollToBottomSmooth"
            >
              <span class="material-symbols-outlined text-[22px] leading-none">arrow_downward</span>
            </button>
          </Transition>

          <!-- Input Area -->
          <div
            class="shrink-0 p-4 md:p-8 bg-gradient-to-t from-white via-white to-transparent"
            :style="chatComposerWrapStyle"
          >
            <div class="max-w-4xl mx-auto space-y-4">
              <div v-if="chatStore.appOptions.length > 0" class="flex flex-wrap gap-2 justify-center">
                <button
                  v-for="app in chatStore.appOptions"
                  :key="app.code"
                  type="button"
                  class="inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-bold transition-all"
                  :class="chatStore.selectedAppCode === app.code
                    ? 'border-primary/25 bg-primary/10 text-primary shadow-sm shadow-primary/10'
                    : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:text-slate-700'"
                  :title="app.description || app.label"
                  @click="chatStore.setSelectedAppCode(app.code)"
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
              <div
                v-if="composerAttachmentPreviewItems.length > 0"
                class="relative min-w-0"
              >
                <div
                  v-if="composerAttachmentShowScrollArrows && composerAttachmentCanScrollLeft"
                  class="pointer-events-none absolute bottom-1 left-0 top-0 z-[1] w-12 rounded-l-xl bg-gradient-to-r from-white to-transparent"
                  aria-hidden="true"
                />
                <div
                  v-if="composerAttachmentShowScrollArrows && composerAttachmentCanScrollRight"
                  class="pointer-events-none absolute bottom-1 right-0 top-0 z-[1] w-12 rounded-r-xl bg-gradient-to-l from-white to-transparent"
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
                  class="wk-chat-attachment-preview flex min-h-[54px] min-w-0 w-full flex-nowrap gap-2 overflow-x-auto overflow-y-hidden scroll-smooth pb-1"
                  @scroll.passive="updateComposerAttachmentScrollState"
                >
                  <template v-for="item in composerAttachmentPreviewItems" :key="item.key">
                    <div
                      v-if="item.kind === 'knowledge'"
                      class="relative flex h-[54px] min-w-[200px] max-w-[320px] shrink-0 items-center gap-3 overflow-hidden rounded-2xl bg-[#f5f5f5] pl-3 pr-[30px]"
                    >
                      <div
                        class="flex size-10 shrink-0 items-center justify-center rounded-xl"
                        :class="getKnowledgeDocIconMeta(item.knowledge).bg"
                      >
                        <span
                          class="material-symbols-outlined text-[22px] leading-none"
                          :class="getKnowledgeDocIconMeta(item.knowledge).color"
                        >
                          {{ getKnowledgeDocIconMeta(item.knowledge).icon }}
                        </span>
                      </div>
                      <div class="min-w-0 flex-1">
                        <div class="truncate text-[14px] leading-[18px] text-[#0d0d0d]">{{ item.knowledge.name }}</div>
                        <div class="text-[12px] leading-[14px] text-[#909090]">{{ getKnowledgeCardSubtitle(item.knowledge) }}</div>
                      </div>
                      <button
                        type="button"
                        class="absolute right-2 top-2 flex size-5 items-center justify-center rounded-full bg-[#0d0d0d] text-white"
                        aria-label="移除知识库文件"
                        title="移除"
                        @click="removeSelectedKnowledge(item.knowledge.knowledgeId)"
                      >
                        <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                      </button>
                    </div>

                    <div
                      v-else-if="item.kind === 'file' && item.file.type.startsWith('image/')"
                      class="relative h-[54px] w-[54px] shrink-0 overflow-hidden rounded-xl border border-[#0d0d0d0d] bg-white"
                    >
                      <img
                        :src="getSelectedFilePreviewUrl(item.file)"
                        :alt="item.file.name"
                        class="size-full object-cover"
                      />
                      <button
                        type="button"
                        class="absolute right-1 top-1 flex size-5 items-center justify-center rounded-full bg-[#0d0d0d] text-white"
                        aria-label="移除图片"
                        title="移除"
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
                        class="flex size-10 shrink-0 items-center justify-center rounded-xl"
                        :class="getChatDocIconMeta(item.file).bg"
                      >
                        <span
                          class="material-symbols-outlined text-[22px] leading-none"
                          :class="getChatDocIconMeta(item.file).color"
                        >
                          {{ getChatDocIconMeta(item.file).icon }}
                        </span>
                      </div>
                      <div class="min-w-0 flex-1">
                        <div class="truncate text-[14px] leading-[18px] text-[#0d0d0d]">{{ item.file.name }}</div>
                        <div class="text-[12px] leading-[14px] text-[#909090]">{{ getChatDocumentSubtitle(item.file) }}</div>
                      </div>
                      <button
                        type="button"
                        class="absolute right-2 top-2 flex size-5 items-center justify-center rounded-full bg-[#0d0d0d] text-white"
                        aria-label="移除文件"
                        title="移除"
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
                class="flex flex-wrap gap-2 px-2"
              >
                <button
                  v-for="(prompt, index) in KNOWLEDGE_DOC_PROMPTS"
                  :key="index"
                  type="button"
                  class="inline-flex max-w-full items-center gap-1.5 rounded-xl bg-[#f5f5f5] px-3 py-2 text-left text-[13px] leading-snug text-[#0d0d0d] transition-colors hover:bg-[#ececec]"
                  @click="applyKnowledgeDocPrompt(prompt)"
                >
                  <span class="min-w-0">{{ prompt }}</span>
                  <span class="material-symbols-outlined shrink-0 text-[16px] leading-none text-[#909090]">arrow_forward</span>
                </button>
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
                  <el-popover
                    v-model:visible="chatUploadMenuVisible"
                    placement="top-start"
                    trigger="click"
                    :width="224"
                    :teleported="true"
                    :disabled="isUploading"
                    popper-class="wk-chat-upload-menu-popper"
                  >
                    <template #reference>
                      <button
                        type="button"
                        class="size-10 flex items-center justify-center rounded-xl text-slate-400 transition-colors hover:bg-slate-50 hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="isUploading"
                        aria-label="添加附件"
                        title="添加附件"
                      >
                        <span class="material-symbols-outlined">add_circle</span>
                      </button>
                    </template>
                    <div class="wk-chat-upload-menu">
                      <button
                        type="button"
                        class="wk-chat-upload-menu__item"
                        :disabled="selectedFiles.length + selectedKnowledgeItems.length >= MAX_FILE_COUNT"
                        @click="handleUploadMenuAddFile"
                      >
                        <span class="wk-chat-upload-menu__icon">
                          <span class="material-symbols-outlined text-[19px] leading-none">attach_file</span>
                        </span>
                        <span class="min-w-0 flex-1">
                          <span class="block truncate text-sm font-semibold text-[#0d0d0d]">上传本地文件</span>
                          <span class="block truncate text-xs text-slate-400">图片、文档或音视频</span>
                        </span>
                      </button>
                      <button
                        type="button"
                        class="wk-chat-upload-menu__item"
                        :disabled="selectedFiles.length + selectedKnowledgeItems.length >= MAX_FILE_COUNT"
                        @click="handleUploadMenuChooseKnowledge"
                      >
                        <span class="wk-chat-upload-menu__icon">
                          <span class="material-symbols-outlined text-[19px] leading-none">menu_book</span>
                        </span>
                        <span class="min-w-0 flex-1">
                          <span class="block truncate text-sm font-semibold text-[#0d0d0d]">选择知识库文件</span>
                          <span class="block truncate text-xs text-slate-400">引用已上传资料</span>
                        </span>
                      </button>
                      <div v-if="chatUploadAppOptions.length > 0" class="mt-1 border-t border-slate-100 pt-1">
                        <button
                          v-for="app in chatUploadAppOptions"
                          :key="app.code"
                          type="button"
                          class="wk-chat-upload-menu__item"
                          @click="handleUploadMenuSelectApp(app.code)"
                        >
                          <span class="wk-chat-upload-menu__icon">
                            <span class="material-symbols-outlined text-[19px] leading-none">
                              {{ resolveChatAppIcon(app.code) }}
                            </span>
                          </span>
                          <span class="min-w-0 flex-1">
                            <span class="block truncate text-sm font-semibold text-[#0d0d0d]">{{ app.label }}</span>
                            <span class="block truncate text-xs text-slate-400">{{ app.description || '切换聊天应用' }}</span>
                          </span>
                          <span
                            v-if="chatStore.selectedAppCode === app.code"
                            class="material-symbols-outlined fill-1 text-[18px] leading-none text-primary"
                          >
                            check
                          </span>
                        </button>
                      </div>
                    </div>
                  </el-popover>
                  <button
                    v-if="selectedChatAppLabel"
                    type="button"
                    class="group/crm-toolbar hidden h-10 shrink-0 items-center gap-1.5 rounded-full bg-slate-50 pl-2 pr-3 text-sm font-semibold text-[var(--wk-text-primary)] transition-colors hover:bg-slate-100 sm:inline-flex"
                    :title="`已启用 ${selectedChatAppLabel}，点击关闭`"
                    @click="chatStore.setSelectedAppCode('general')"
                  >
                    <span class="material-symbols-outlined text-[18px] leading-none">{{ selectedChatAppIcon }}</span>
                    <span class="max-w-[96px] truncate">{{ selectedChatAppLabel }}</span>
                    <span class="material-symbols-outlined text-[16px] leading-none text-slate-400 transition-colors group-hover/crm-toolbar:text-slate-700">close</span>
                  </button>
                  <textarea
                    ref="chatInputRef"
                    v-model="inputText"
                    rows="1"
                    class="max-h-32 min-h-10 flex-1 resize-none overflow-y-auto border-none bg-transparent px-3 py-2.5 text-sm leading-5 text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-0"
                    placeholder="输入指令，如：总结今天与张总的会议..."
                    :disabled="chatStore.currentSessionIsStreaming || isUploading"
                    @input="resizeChatTextarea"
                    @keydown.enter.exact.prevent="handleSend"
                    @paste="handlePaste"
                  ></textarea>
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
                    <el-popover
                      v-model:visible="chatModelPopoverVisible"
                      placement="top-end"
                      trigger="click"
                      :width="300"
                      :teleported="true"
                      :disabled="chatStore.modelOptionsLoading"
                      popper-class="wk-chat-model-popper"
                    >
                      <template #reference>
                        <button
                          type="button"
                          class="wk-chat-model-trigger"
                          :disabled="chatStore.modelOptionsLoading"
                          :title="`当前模型：${composerModelLabel}`"
                        >
                          <span class="wk-chat-model-trigger__icon" aria-hidden="true">
                            <template v-if="chatStore.selectedModel">
                              <img
                                v-if="modelShowImage(chatStore.selectedModel)"
                                :src="modelIconSrc(chatStore.selectedModel)"
                                alt=""
                                class="size-full object-fill"
                                @error="onModelImageError($event)"
                              />
                              <span v-else>{{ selectedModelInitial }}</span>
                            </template>
                            <span v-else>{{ selectedModelInitial }}</span>
                          </span>
                          <span class="wk-chat-model-trigger__label min-w-0 flex-1 truncate">{{ composerModelLabel }}</span>
                          <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-[#8f8f8f]">expand_more</span>
                        </button>
                      </template>

                      <div class="wk-chat-model-menu">
                        <template v-if="modelOptionGroups.length > 0">
                          <template v-for="group in modelOptionGroups" :key="group.source">
                            <div v-if="modelOptionGroups.length > 1" class="wk-chat-model-menu__group-label">
                              {{ group.label }}
                            </div>
                            <button
                              v-for="option in group.options"
                              :key="chatStore.toModelKey(option)"
                              type="button"
                              class="wk-chat-model-menu__item"
                              @click="handleModelChange(chatStore.toModelKey(option))"
                            >
                              <span class="wk-chat-model-menu__logo" aria-hidden="true">
                                <img
                                  v-if="modelShowImage(option)"
                                  :src="modelIconSrc(option)"
                                  alt=""
                                  class="size-5 object-fill"
                                  @error="onModelImageError($event)"
                                />
                                <span v-else>{{ modelOptionLabel(option).slice(0, 1) }}</span>
                              </span>
                              <span class="min-w-0 flex-1 truncate text-[14px] text-[#0d0d0d]">
                                {{ modelOptionLabel(option) }}
                              </span>
                              <span
                                class="material-symbols-outlined flex size-5 shrink-0 items-center justify-center text-[20px] leading-none"
                                :class="chatStore.selectedModelKey === chatStore.toModelKey(option) ? 'text-primary' : 'invisible'"
                                aria-hidden="true"
                              >
                                check
                              </span>
                            </button>
                          </template>
                        </template>
                        <button
                          v-if="canManageAiConfig"
                          type="button"
                          class="wk-chat-model-menu__more"
                          @click="handleOpenMoreModels"
                        >
                          <span class="material-symbols-outlined text-[18px] leading-none" aria-hidden="true">
                            add_circle
                          </span>
                          <span class="min-w-0 flex-1 truncate">
                            {{ modelOptionGroups.length > 0 ? '管理模型配置' : '配置自建模型' }}
                          </span>
                          <span class="material-symbols-outlined text-[18px] leading-none text-[#8f8f8f]" aria-hidden="true">
                            chevron_right
                          </span>
                        </button>
                        <p v-else-if="modelOptionGroups.length === 0" class="wk-chat-model-menu__empty">
                          暂无可用模型，请联系管理员配置。
                        </p>
                      </div>
                    </el-popover>
                    <button
                      type="button"
                      class="group/chat-send size-10 rounded-xl bg-primary text-white flex items-center justify-center hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all disabled:cursor-not-allowed disabled:opacity-50"
                      :class="sendActionButtonClass"
                      :disabled="sendActionDisabled"
                      :aria-label="sendActionTitle"
                      :title="sendActionTitle"
                      @click="handleSendAction"
                    >
                      <span v-if="chatStore.currentSessionIsStreaming || isUploading || isTranscribing" class="material-symbols-outlined text-xl animate-spin">progress_activity</span>
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
                        v-else-if="!hasComposerSendPayload"
                        name="voice"
                        :box-size="20"
                        class="text-[20px] leading-none"
                      />
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

    <Teleport to="body">
      <Transition name="wk-mobile-object-detail">
        <div
          v-if="mobileObjectDetailOpen && isMobile"
          class="wk-mobile-object-detail"
          role="dialog"
          aria-modal="true"
          aria-labelledby="wk-mobile-object-detail-title"
        >
          <button
            type="button"
            class="wk-mobile-object-detail__backdrop"
            aria-label="关闭详情"
            @click="closeMobileObjectDetail"
          ></button>
          <section class="wk-mobile-object-detail__sheet">
            <header class="wk-mobile-object-detail__header">
              <span class="wk-mobile-object-detail__handle" aria-hidden="true"></span>
              <div class="wk-mobile-object-detail__title-row">
                <p id="wk-mobile-object-detail-title" class="wk-mobile-object-detail__title">
                  {{ mobileObjectDetailTitle }}
                </p>
                <button
                  type="button"
                  class="wk-mobile-object-detail__close"
                  aria-label="关闭详情"
                  @click="closeMobileObjectDetail"
                >
                  <span class="material-symbols-outlined text-[24px] leading-none">close</span>
                </button>
              </div>
            </header>
            <div class="wk-mobile-object-detail__body">
              <div v-if="mobileObjectPanelLoading" class="flex h-full items-center justify-center text-slate-300">
                <span class="material-symbols-outlined animate-spin text-2xl">progress_activity</span>
              </div>
              <div v-else-if="objectPanelError" class="flex h-full items-center justify-center px-6 text-center text-sm text-slate-400">
                {{ objectPanelError }}
              </div>
              <CustomerChatInfoPanel
                v-else-if="chatObjectKind === 'customer' && currentObjectId"
                :customer-id="currentObjectId"
              />
              <EmployeeChatInfoPanel
                v-else-if="chatObjectKind === 'employee' && employeeDetail"
                :employee="employeeDetail"
                @add-task="handleMobileObjectAddTask"
                @add-schedule="handleMobileObjectAddSchedule"
                @add-attachment="handleMobileObjectAddAttachment"
                @view-task="handleMobileObjectViewTask"
                @view-schedule="handleMobileObjectViewSchedule"
                @view-attachment="handleMobileObjectViewAttachment"
              />
              <RelationChatInfoPanel
                v-else-if="chatObjectKind === 'relation' && relationDetail"
                :detail="relationDetail"
                @add-task="handleMobileObjectAddTask"
                @add-schedule="handleMobileObjectAddSchedule"
                @add-attachment="handleMobileObjectAddAttachment"
                @view-task="handleMobileObjectViewTask"
                @view-schedule="handleMobileObjectViewSchedule"
                @view-attachment="handleMobileObjectViewAttachment"
              />
              <ProductChatInfoPanel
                v-else-if="chatObjectKind === 'product' && productDetail"
                :product="productDetail"
                @edit="handleMobileObjectEditProduct"
              />
              <div v-else class="flex h-full flex-col items-center justify-center px-6 text-center">
                <div class="mb-4 flex size-12 items-center justify-center rounded-2xl bg-[#f5f5f5] text-slate-400">
                  <span class="material-symbols-outlined text-[24px] leading-none">info</span>
                </div>
                <p class="text-sm font-semibold text-slate-700">暂无详情</p>
                <p class="mt-1 text-xs leading-relaxed text-slate-400">资料加载后会在这里展示。</p>
              </div>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>

    <CustomerBasicInfoDrawer
      v-model="showSelectedCustomerBasicInfoDrawer"
      :customer="selectedCustomer"
      :contacts="selectedCustomer?.contacts || []"
      @contacts-updated="handleSelectedCustomerContactsUpdated"
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
import { ref, computed, onBeforeUnmount, onMounted, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useEnterpriseStore } from '@/stores/enterprise'
import { useEnumStore } from '@/stores/enums'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { transcribeFollowUpAudio } from '@/api/followup'
import { getAiConfig, getAiConfigDetail, updateAiConfig } from '@/api/systemConfig'
import { addCustomerTag, getCustomerDetail, removeCustomerTag, updateCustomerStage } from '@/api/customer'
import { getAddressBookDetail } from '@/api/addressBook'
import { getRelationDetail } from '@/api/relation'
import { getProductDetail } from '@/api/product'
import ApiKeySetupModal from '@/components/common/ApiKeySetupModal.vue'
import ChatKnowledgePickerModal from '@/components/chat/ChatKnowledgePickerModal.vue'
import CustomerBasicInfoDrawer from '@/views/customer/components/CustomerBasicInfoDrawer.vue'
import CustomerChatInfoPanel from './components/CustomerChatInfoPanel.vue'
import EmployeeChatInfoPanel from './components/EmployeeChatInfoPanel.vue'
import MobileChatTopHeader from './components/MobileChatTopHeader.vue'
import ProductChatInfoPanel from './components/ProductChatInfoPanel.vue'
import RelationChatInfoPanel from './components/RelationChatInfoPanel.vue'
import { renderMarkdown } from '@/utils/markdown'
import { getAssistantMessagePlaceholder, normalizeAssistantMessageContent } from '@/utils/chatMessage'
import { appEvents, APP_EVENT } from '@/utils/events'
import { resolveKnowledgeFileSizeBytes } from '@/utils/formatFileSize'
import { isRequestErrorHandled } from '@/utils/requestError'
import { shouldRefocusChatComposerAfterSend } from '@/utils/chatComposerFocus'
import { hideCapacitorKeyboard, registerNativeKeyboardInsetListeners } from '@/utils/capacitorKeyboard'
import {
  resolveMobileKeyboardInset,
  resolveMobileViewportTopOffset
} from '@/utils/mobileKeyboardViewport'
import {
  CHAT_ATTACHMENT_ACCEPT,
  MAX_CHAT_ATTACHMENT_COUNT,
  extractClipboardFiles,
  mergeChatFiles
} from '@/utils/chatAttachment'
import {
  canCaptureMobileAudioFile,
  captureMobileAudioFile,
  hasMobileAudioInputSupport,
  requestMobileAudioStream,
  shouldPreferMobileAudioFileCapture,
  shouldUseMobileAudioFileCapture
} from '@/utils/mobileAudioRecording'
import type { ScheduleVO } from '@/api/schedule'
import type { AddressBookDetail } from '@/types/addressBook'
import type { ProductVO } from '@/types/product'
import type { RelationDetailVO } from '@/types/relation'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, ChatModelOption, Knowledge, Task } from '@/types/common'
import type { Contact, CustomerDetailVO, CustomerTag } from '@/types/customer'
import type { AiConfig, AiConfigUpdateBO, AiProvider, AiProviderPreset } from '@/types/systemConfig'
import dashscopeBrandUrl from '@/assets/model-provider-brands/dashscope.svg?url'
import openaiBrandUrl from '@/assets/model-provider-brands/openai.svg?url'
import deepseekBrandUrl from '@/assets/model-provider-brands/deepseek.svg?url'
import moonshotBrandUrl from '@/assets/model-provider-brands/moonshot.svg?url'
import arkBrandUrl from '@/assets/model-provider-brands/ark.svg?url'
import hunyuanBrandUrl from '@/assets/model-provider-brands/hunyuan.svg?url'
import minimaxBrandUrl from '@/assets/model-provider-brands/minimax.svg?url'
import zhipuBrandUrl from '@/assets/model-provider-brands/zhipu.svg?url'
import defaultLogoImg from '@/assets/images/logo.png'

type ComposerAttachmentPreviewItem =
  | { kind: 'knowledge'; key: string; knowledge: Knowledge }
  | { kind: 'file'; key: string; file: File; fileIndex: number }

const chatStore = useChatStore()
const agentStore = useAgentStore()
const enterpriseStore = useEnterpriseStore()
const enumStore = useEnumStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { isMobile } = useResponsive()

const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const showScrollToBottomButton = ref(false)
const mobileKeyboardInset = ref(0)
const nativeKeyboardInset = ref(0)
const mobileViewportTopOffset = ref(0)
const mobilePanel = ref<'sessions' | 'chat'>('chat')
const fileInputRef = ref<HTMLInputElement | null>(null)
const chatInputRef = ref<HTMLTextAreaElement | null>(null)
const selectedFiles = ref<File[]>([])
const selectedKnowledgeItems = ref<Knowledge[]>([])
const showKnowledgeFollowUpChips = ref(false)
const composerAttachmentScrollRef = ref<HTMLElement | null>(null)
const composerAttachmentShowScrollArrows = ref(false)
const composerAttachmentCanScrollLeft = ref(false)
const composerAttachmentCanScrollRight = ref(false)
const chatModelPopoverVisible = ref(false)
const chatUploadMenuVisible = ref(false)
const chatModelImageLoadFailed = ref<Record<string, boolean>>({})
const isRecording = ref(false)
const isTranscribing = ref(false)
const composerAttachmentPreviewItems = computed<ComposerAttachmentPreviewItem[]>(() => {
  const items: ComposerAttachmentPreviewItem[] = []

  for (const item of selectedKnowledgeItems.value) {
    items.push({ kind: 'knowledge', key: `knowledge-${item.knowledgeId}`, knowledge: item })
  }

  selectedFiles.value.forEach((file, index) => {
    items.push({ kind: 'file', key: `file-${file.name}-${file.lastModified}-${index}`, file, fileIndex: index })
  })

  return items
})
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
const selectedCustomer = ref<CustomerDetailVO | null>(null)
const selectedCustomerLoading = ref(false)
const showSelectedCustomerBasicInfoDrawer = ref(false)
const showSelectedCustomerTagDialog = ref(false)
const newSelectedCustomerTagName = ref('')
const selectedCustomerTagSubmitting = ref(false)
const objectPanelLoading = ref(false)
const objectPanelError = ref('')
const mobileObjectDetailOpen = ref(false)
let objectDetailRequestId = 0
let customerDetailRequestId = 0
let offSelectedCustomerDetailRefresh: (() => void) | null = null
let mediaRecorder: MediaRecorder | null = null
let mediaStream: MediaStream | null = null
let recordedChunks: Blob[] = []
let recordedMimeType = ''
let skipNextTranscription = false
let transcriptionToken = 0
let speechInputBase = ''
let applyingChatRouteQuery = false
let composerAttachmentScrollResizeObserver: ResizeObserver | null = null
let mobileKeyboardInsetTimer: ReturnType<typeof setTimeout> | null = null
let removeNativeKeyboardInsetListeners: (() => void) | null = null

const CHAT_CONTEXT_QUERY_KEYS = ['sessionId', 'customerId', 'employeeId', 'relationId', 'productId'] as const
type ChatContextQueryKey = (typeof CHAT_CONTEXT_QUERY_KEYS)[number]

const MAX_FILE_COUNT = MAX_CHAT_ATTACHMENT_COUNT
const DEFAULT_CHAT_AI_CONFIG: AiConfigUpdateBO = {
  provider: 'dashscope',
  apiUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
  apiKey: '',
  model: 'qwen3.5-plus',
  temperature: 0.7,
  maxTokens: 4096
}
const SCROLL_TO_BOTTOM_THRESHOLD_PX = 100
const MOBILE_KEYBOARD_INSET_GAP_PX = 8
const MOBILE_KEYBOARD_VISIBLE_THRESHOLD_PX = 24
const KNOWLEDGE_DOC_PROMPTS = [
  '详细总结这些文档内容',
  '用通俗易懂的话，说说这些文档讲了什么',
  '生成一个简短摘要'
] as const
const CUSTOMER_STAGE_FALLBACK_OPTIONS = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' }
]

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
  const recommended = chatStore.selectedApp?.recommendedQuestions || []
  if (recommended.length > 0) {
    return recommended.map(text => ({ label: text, text }))
  }
  return quickActions
})

const modelOptionGroups = computed(() => {
  const customOptions = chatStore.modelOptions.filter(option => option.modelSource === 'custom')
  const systemOptions = chatStore.modelOptions.filter(option => option.modelSource !== 'custom')
  return [
    { source: 'custom', label: '自建模型', options: customOptions },
    { source: 'system', label: '系统模型', options: systemOptions }
  ].filter(group => group.options.length > 0)
})

const canManageAiConfig = computed(() => userStore.hasPermission('config:ai'))
const composerModelLabel = computed(() => {
  if (chatStore.modelOptionsLoading) return '加载模型...'
  const model = chatStore.selectedModel
  return model ? modelOptionLabel(model) : '选择模型'
})
const selectedModelInitial = computed(() => composerModelLabel.value.slice(0, 1) || '?')
const hasComposerSendPayload = computed(() =>
  Boolean(inputText.value.trim())
  || selectedFiles.value.length > 0
  || selectedKnowledgeItems.value.length > 0
)
const chatUploadAppOptions = computed(() =>
  chatStore.appOptions.filter(app => app.code !== 'general')
)
const selectedChatAppLabel = computed(() => {
  if (chatStore.selectedAppCode === 'general') return ''
  return chatStore.selectedApp?.label || chatStore.selectedAppCode
})
const selectedChatAppIcon = computed(() => resolveChatAppIcon(chatStore.selectedAppCode))
const sendActionButtonClass = computed(() => {
  if (isRecording.value) return '!bg-red-500 hover:!bg-red-600'
  if (isTranscribing.value || isUploading.value) return '!bg-slate-200 !text-slate-500 hover:!bg-slate-200'
  return ''
})
const sendActionDisabled = computed(() =>
  chatStore.currentSessionIsStreaming || isUploading.value || isTranscribing.value
)
const sendActionTitle = computed(() => {
  if (chatStore.currentSessionIsStreaming) return '正在回复中'
  if (isUploading.value) return '文件上传中'
  if (isTranscribing.value) return '语音识别中'
  if (isRecording.value) return '点击结束录音'
  if (!hasComposerSendPayload.value) return '使用语音输入'
  return '发送'
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
const isChatEmpty = computed(() => chatStore.messages.length === 0)
const isObjectContextChat = computed(() => Boolean(chatObjectKind.value && currentObjectId.value))
const isCenteredEmptyChat = computed(() => isChatEmpty.value && !isObjectContextChat.value)
const showDesktopCustomerHeader = computed(() =>
  !isMobile.value && currentView.value === 'chat' && chatObjectKind.value === 'customer' && Boolean(currentObjectId.value)
)
const showDesktopObjectHeader = computed(() =>
  !isMobile.value
  && currentView.value === 'chat'
  && Boolean(currentObjectId.value)
  && (chatObjectKind.value === 'employee' || chatObjectKind.value === 'relation' || chatObjectKind.value === 'product')
)
const showDesktopObjectPanel = computed(() =>
  !isMobile.value && currentView.value === 'chat' && Boolean(chatObjectKind.value && currentObjectId.value)
)
const showMobileChatHeader = computed(() =>
  isMobile.value && mobilePanel.value === 'chat' && currentView.value === 'chat'
)
const showMobileObjectHeader = computed(() =>
  showMobileChatHeader.value && Boolean(chatObjectKind.value)
)
const showMobileFloatingBar = computed(() =>
  showMobileChatHeader.value && !chatObjectKind.value
)
const showMobileTopChrome = computed(() =>
  showMobileObjectHeader.value || showMobileFloatingBar.value
)
const showMobileTopViewportShield = computed(() => showMobileTopChrome.value)
const showMobileNewSessionAction = computed(() =>
  showMobileFloatingBar.value && Boolean(chatStore.currentSessionId)
)
const mobileChatFloatingActionCount = computed(() =>
  showMobileNewSessionAction.value ? 1 : 0
)
const showMobileChatFloatingActions = computed(() => mobileChatFloatingActionCount.value > 0)
const effectiveMobileKeyboardInset = computed(() => Math.max(
  mobileKeyboardInset.value,
  nativeKeyboardInset.value
))
const chatComposerWrapStyle = computed(() =>
  isMobile.value && effectiveMobileKeyboardInset.value > 0
    ? { transform: `translate3d(0, -${effectiveMobileKeyboardInset.value}px, 0)` }
    : undefined
)
const mobileTopViewportShieldStyle = computed(() => ({
  '--wk-mobile-viewport-top-offset': `${mobileViewportTopOffset.value}px`
}))
const mobileTopFixedLayerStyle = computed(() =>
  isMobile.value && mobileViewportTopOffset.value > 0
    ? {
        transform: `translate3d(0, ${mobileViewportTopOffset.value}px, 0)`,
        willChange: 'transform'
      }
    : undefined
)
const chatMessagesAreaClass = computed(() => {
  if (!isChatEmpty.value) return 'wk-chat-messages--scrollable flex-1 overflow-y-auto pb-4 pt-6 md:pt-8'
  if (isMobile.value) return 'flex-1 overflow-hidden py-6'
  return isObjectContextChat.value ? 'flex-1 overflow-hidden py-6' : 'overflow-hidden py-6'
})
const mobileChatHeaderKind = computed<'customer' | 'employee' | 'relation' | 'product'>(() => {
  if (chatObjectKind.value === 'employee') return 'employee'
  if (chatObjectKind.value === 'relation') return 'relation'
  if (chatObjectKind.value === 'product') return 'product'
  return 'customer'
})
const mobileChatHeaderTitle = computed(() => {
  const session = currentChatSession.value
  if (chatObjectKind.value === 'customer') {
    return session?.customerName || session?.title || '客户对话'
  }
  if (chatObjectKind.value === 'employee') {
    return employeeDetail.value?.realname || session?.employeeName || session?.title || '通讯录对话'
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
  const session = currentChatSession.value
  if (chatObjectKind.value === 'customer') return session?.customerLogoUrl || ''
  if (chatObjectKind.value === 'employee') return employeeDetail.value?.imgUrl || session?.employeeAvatarUrl || employeeDetail.value?.img || ''
  if (chatObjectKind.value === 'relation') return relationDetail.value?.relation?.avatarUrl || session?.relationAvatarUrl || relationDetail.value?.relation?.avatar || ''
  if (chatObjectKind.value === 'product') return productDetail.value?.mainImageUrl || session?.productImageUrl || ''
  return ''
})
const mobileObjectPanelLoading = computed(() =>
  chatObjectKind.value !== 'customer' && objectPanelLoading.value
)
const customerHeaderTitle = computed(() =>
  selectedCustomer.value?.companyName || currentChatSession.value?.customerName || currentChatSession.value?.title || '客户对话'
)
const customerHeaderLogoUrl = computed(() =>
  selectedCustomer.value?.logoUrl || currentChatSession.value?.customerLogoUrl || ''
)
const customerStageOptions = computed(() =>
  enumStore.customerStage.length
    ? enumStore.customerStage.map(item => ({ value: item.value, label: item.label }))
    : CUSTOMER_STAGE_FALLBACK_OPTIONS
)
const selectedCustomerStageText = computed(() => {
  const customer = selectedCustomer.value
  if (!customer) return '-'
  const stage = customer.stage || ''
  const stageOption = customerStageOptions.value.find(item => item.value === stage)
  if (stageOption?.label) return stageOption.label
  if (customer.stageName && customer.stageName !== stage) return customer.stageName
  return enumStore.stageLabel(stage) || stage || '-'
})
const selectedCustomerStageButtonClass = computed(() => getCustomerStageButtonClass(selectedCustomer.value?.stage))
const canChangeSelectedCustomerStage = computed(() => userStore.hasPermission('customer:change_stage'))
const canEditSelectedCustomerTags = computed(() => userStore.hasPermission('customer:edit'))
const selectedCustomerVisibleTags = computed(() => selectedCustomer.value?.tags?.slice(0, 3) || [])
const selectedCustomerHiddenTags = computed(() => selectedCustomer.value?.tags?.slice(3) || [])
const desktopObjectHeaderTitle = computed(() => mobileChatHeaderTitle.value)
const desktopObjectHeaderAvatarUrl = computed(() => mobileChatHeaderAvatarUrl.value)
const desktopObjectHeaderImageClass = computed(() =>
  chatObjectKind.value === 'employee'
    ? 'size-full bg-white object-cover'
    : 'size-full bg-white object-contain'
)
const desktopObjectHeaderIcon = computed(() => {
  if (chatObjectKind.value === 'product') return 'inventory_2'
  if (chatObjectKind.value === 'relation') return 'diversity_3'
  return ''
})
const desktopObjectHeaderBadge = computed(() => {
  if (chatObjectKind.value === 'employee') return employeeDetail.value?.post || '员工'
  if (chatObjectKind.value === 'relation') return relationDetail.value?.relation?.relationTypeName || relationDetail.value?.relation?.relationType || '关系'
  if (chatObjectKind.value === 'product') return productDetail.value?.productCode || '无编码'
  return '对象'
})
const desktopObjectHeaderMeta = computed(() => {
  if (chatObjectKind.value === 'employee') return employeeDetail.value?.deptName || ''
  if (chatObjectKind.value === 'relation') return relationDetail.value?.relation?.company || relationDetail.value?.relation?.customerName || ''
  if (chatObjectKind.value === 'product') return productDetail.value?.categoryPath || productDetail.value?.categoryName || productDetail.value?.productType || ''
  return ''
})
const mobileObjectDetailTitle = computed(() => {
  if (chatObjectKind.value === 'customer') return '客户详情'
  if (chatObjectKind.value === 'employee') return '通讯录详情'
  if (chatObjectKind.value === 'relation') return '关系详情'
  if (chatObjectKind.value === 'product') return '产品详情'
  return '对象详情'
})

onMounted(async () => {
  registerMobileKeyboardInsetListeners()
  offSelectedCustomerDetailRefresh = appEvents.on<{ customerId?: string | number }>(
    APP_EVENT.CUSTOMER_DETAIL_REFRESH,
    handleSelectedCustomerDetailRefresh
  )
  await Promise.all([
    chatStore.fetchAppOptions(),
    chatStore.fetchModelOptions(),
    chatStore.fetchSessions(),
    agentStore.fetchEnabledAgents(),
    enterpriseStore.loadConfig(),
    enumStore.ensureCustomerStage(),
    loadAiConfig()
  ])
  await applyChatRouteQuery()
})

onBeforeUnmount(() => {
  unregisterMobileKeyboardInsetListeners()
  offSelectedCustomerDetailRefresh?.()
  offSelectedCustomerDetailRefresh = null
  disconnectComposerAttachmentScrollObserver()
  revokeAllSelectedFilePreviewUrls()
  abortVoiceRecording()
  if (scrollTimer) {
    clearTimeout(scrollTimer)
    scrollTimer = null
  }
})

watch(isMobile, (mobile) => {
  if (mobile) {
    scheduleMobileKeyboardInsetUpdate()
    return
  }
  mobileKeyboardInset.value = 0
  nativeKeyboardInset.value = 0
  mobileViewportTopOffset.value = 0
})

watch(
  () => composerAttachmentPreviewItems.value.length,
  async (count) => {
    await nextTick()
    if (count > 0) {
      observeComposerAttachmentScroll()
      updateComposerAttachmentScrollState()
      return
    }
    disconnectComposerAttachmentScrollObserver()
    showKnowledgeFollowUpChips.value = false
    composerAttachmentShowScrollArrows.value = false
    composerAttachmentCanScrollLeft.value = false
    composerAttachmentCanScrollRight.value = false
  }
)

// Auto scroll to bottom when new messages arrive or during streaming while the user stays near the bottom.
let scrollTimer: ReturnType<typeof setTimeout> | null = null
const isPinnedToBottom = ref(true)

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

function updateComposerAttachmentScrollState() {
  const el = composerAttachmentScrollRef.value
  if (!el) {
    composerAttachmentShowScrollArrows.value = false
    composerAttachmentCanScrollLeft.value = false
    composerAttachmentCanScrollRight.value = false
    return
  }

  const { clientWidth, scrollLeft, scrollWidth } = el
  const hasOverflow = scrollWidth - clientWidth > 2
  composerAttachmentShowScrollArrows.value = hasOverflow
  composerAttachmentCanScrollLeft.value = hasOverflow && scrollLeft > 2
  composerAttachmentCanScrollRight.value = hasOverflow && scrollLeft + clientWidth < scrollWidth - 2
}

function scrollComposerAttachmentStep(direction: -1 | 1) {
  const el = composerAttachmentScrollRef.value
  if (!el) return

  const distance = Math.max(180, Math.floor(el.clientWidth * 0.75))
  el.scrollBy({ left: direction * distance, behavior: 'smooth' })
  window.setTimeout(updateComposerAttachmentScrollState, 240)
}

function disconnectComposerAttachmentScrollObserver() {
  composerAttachmentScrollResizeObserver?.disconnect()
  composerAttachmentScrollResizeObserver = null
}

function observeComposerAttachmentScroll() {
  disconnectComposerAttachmentScrollObserver()
  const el = composerAttachmentScrollRef.value
  if (!el || typeof ResizeObserver === 'undefined') return

  composerAttachmentScrollResizeObserver = new ResizeObserver(() => {
    updateComposerAttachmentScrollState()
  })
  composerAttachmentScrollResizeObserver.observe(el)
}

function scrollToBottom() {
  if (!isPinnedToBottom.value) {
    updateScrollToBottomVisibility()
    return
  }
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

function scrollToBottomSmooth() {
  const el = messagesContainer.value
  if (!el) return

  isPinnedToBottom.value = true
  el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  showScrollToBottomButton.value = false
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
      scrollToBottom()
      updateScrollToBottomVisibility()
    })
  }
)

watch(
  () => userStore.avatar,
  () => {
    userAvatarLoadFailed.value = false
  }
)

watch(
  () => chatStore.modelOptions,
  () => {
    chatModelImageLoadFailed.value = {}
  },
  { deep: true }
)

watch(
  () => [chatObjectKind.value, currentObjectId.value] as const,
  () => {
    void loadObjectPanelDetail()
  },
  { immediate: true }
)

watch(
  () => [chatObjectKind.value, currentObjectId.value] as const,
  ([kind, id]) => {
    if (kind === 'customer' && id) {
      void loadSelectedCustomerDetail(id, { silent: Boolean(selectedCustomer.value) })
      return
    }
    selectedCustomer.value = null
    selectedCustomerLoading.value = false
    showSelectedCustomerBasicInfoDrawer.value = false
    showSelectedCustomerTagDialog.value = false
  },
  { immediate: true }
)

watch(
  () => [isMobile.value, chatObjectKind.value, currentObjectId.value] as const,
  ([mobile, kind, id]) => {
    if (!mobile || !kind || !id) {
      closeMobileObjectDetail()
    }
  }
)

watch(
  () => CHAT_CONTEXT_QUERY_KEYS.map(key => getRouteQueryString(key)).join('|'),
  () => {
    void applyChatRouteQuery()
  }
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

async function loadSelectedCustomerDetail(customerId: string, options: { silent?: boolean } = {}) {
  const requestId = ++customerDetailRequestId
  if (!options.silent) {
    selectedCustomerLoading.value = true
  }

  try {
    const detail = await getCustomerDetail(customerId)
    if (requestId === customerDetailRequestId && currentObjectId.value === customerId) {
      selectedCustomer.value = detail
    }
  } catch (error) {
    console.error('Load selected customer detail failed:', error)
    if (requestId === customerDetailRequestId) {
      selectedCustomer.value = null
    }
  } finally {
    if (requestId === customerDetailRequestId) {
      selectedCustomerLoading.value = false
    }
  }
}

function emitSelectedCustomerRefresh(customerId: string) {
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH, { source: 'chat' })
  appEvents.emit(APP_EVENT.CUSTOMER_DETAIL_REFRESH, { customerId, source: 'chatHeader' })
}

function handleSelectedCustomerDetailRefresh(payload?: { customerId?: string | number; source?: string }) {
  if (payload?.source === 'chatHeader') return
  const targetCustomerId = payload?.customerId ? String(payload.customerId) : ''
  const activeCustomerId = currentObjectId.value
  if (!activeCustomerId || (targetCustomerId && targetCustomerId !== activeCustomerId)) return
  void loadSelectedCustomerDetail(activeCustomerId, { silent: Boolean(selectedCustomer.value) })
}

async function handleSelectedCustomerStageCommand(value: string | number | object) {
  if (!selectedCustomer.value) return
  const stage = String(value)
  if (!stage || selectedCustomer.value.stage === stage) return

  const customerId = String(selectedCustomer.value.customerId)
  try {
    await updateCustomerStage(customerId, stage)
    await loadSelectedCustomerDetail(customerId, { silent: true })
    emitSelectedCustomerRefresh(customerId)
    ElMessage.success('客户阶段已更新')
  } catch (error) {
    console.error('Update customer stage failed:', error)
    if (!isRequestErrorHandled(error)) ElMessage.error('客户阶段更新失败')
  }
}

async function handleAddSelectedCustomerTag() {
  if (!canEditSelectedCustomerTags.value || !selectedCustomer.value) return
  const tagName = newSelectedCustomerTagName.value.trim()
  if (!tagName) {
    ElMessage.warning('请输入标签名称')
    return
  }

  const customerId = String(selectedCustomer.value.customerId)
  selectedCustomerTagSubmitting.value = true
  try {
    await addCustomerTag(customerId, tagName)
    await loadSelectedCustomerDetail(customerId, { silent: true })
    emitSelectedCustomerRefresh(customerId)
    showSelectedCustomerTagDialog.value = false
    newSelectedCustomerTagName.value = ''
    ElMessage.success('标签添加成功')
  } catch (error) {
    console.error('Add customer tag failed:', error)
    if (!isRequestErrorHandled(error)) ElMessage.error('标签添加失败')
  } finally {
    selectedCustomerTagSubmitting.value = false
  }
}

async function handleRemoveSelectedCustomerTag(tag: CustomerTag) {
  if (!canEditSelectedCustomerTags.value || !selectedCustomer.value) return

  const customerId = String(selectedCustomer.value.customerId)
  try {
    await removeCustomerTag(customerId, tag.tagId)
    await loadSelectedCustomerDetail(customerId, { silent: true })
    emitSelectedCustomerRefresh(customerId)
    ElMessage.success('标签已删除')
  } catch (error) {
    console.error('Remove customer tag failed:', error)
    if (!isRequestErrorHandled(error)) ElMessage.error('标签删除失败')
  }
}

function handleSelectedCustomerContactsUpdated(contacts: Contact[]) {
  if (!selectedCustomer.value) return
  selectedCustomer.value = {
    ...selectedCustomer.value,
    contacts
  }
  emitSelectedCustomerRefresh(String(selectedCustomer.value.customerId))
}

function getCustomerStageButtonClass(stage?: string) {
  switch (stage) {
    case 'closed':
      return 'bg-emerald-50 text-emerald-700'
    case 'lost':
      return 'bg-rose-50 text-rose-700'
    case 'negotiation':
      return 'bg-orange-50 text-orange-700'
    case 'proposal':
      return 'bg-indigo-50 text-indigo-700'
    default:
      return 'bg-primary/10 text-primary'
  }
}

function getCustomerStageIcon(stage?: string) {
  const map: Record<string, string> = {
    lead: 'radio_button_unchecked',
    qualified: 'task_alt',
    proposal: 'description',
    negotiation: 'forum',
    closed: 'handshake',
    lost: 'block'
  }
  return map[stage || ''] || 'lens'
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

function modelOptionLabel(option: ChatModelOption): string {
  return option.displayName || option.modelName
}

const MODEL_PROVIDER_BRAND_URL: Record<string, string> = {
  dashscope: dashscopeBrandUrl,
  openai: openaiBrandUrl,
  deepseek: deepseekBrandUrl,
  moonshot: moonshotBrandUrl,
  ark: arkBrandUrl,
  arkl: arkBrandUrl,
  hunyuan: hunyuanBrandUrl,
  minimax: minimaxBrandUrl,
  zhipu: zhipuBrandUrl
}

function providerBrandAssetUrl(provider: string): string | undefined {
  const id = provider?.trim().toLowerCase()
  if (!id || !/^[-a-z0-9._]+$/.test(id)) return undefined
  return MODEL_PROVIDER_BRAND_URL[id]
}

function modelIconSrc(option: ChatModelOption): string | undefined {
  const fromApi = option.icon?.trim()
  if (fromApi) return fromApi
  return providerBrandAssetUrl(option.provider)
}

function modelShowImage(option: ChatModelOption): boolean {
  const src = modelIconSrc(option)
  if (!src) return false
  return !chatModelImageLoadFailed.value[src]
}

function onModelImageError(event: Event) {
  const target = event.target as HTMLImageElement | null
  const src = target?.currentSrc || target?.src
  if (!src) return
  chatModelImageLoadFailed.value = { ...chatModelImageLoadFailed.value, [src]: true }
}

function handleModelChange(modelKey: string) {
  chatStore.setSelectedModelKey(modelKey)
  chatModelPopoverVisible.value = false
  void nextTick(() => chatInputRef.value?.focus())
}

function handleOpenMoreModels() {
  chatModelPopoverVisible.value = false
  openApiKeySetup()
}

function releaseMediaStream() {
  mediaStream?.getTracks().forEach(track => track.stop())
  mediaStream = null
}

function abortVoiceRecording() {
  skipNextTranscription = true
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  mediaRecorder = null
  releaseMediaStream()
  recordedChunks = []
  recordedMimeType = ''
  isRecording.value = false
}

async function ensureAudioTranscriptionSupported(): Promise<boolean> {
  const selectedCapabilities = chatStore.selectedModel?.capabilities
  if (selectedCapabilities?.supportsAudioTranscription) {
    return true
  }
  if (selectedCapabilities && !selectedCapabilities.supportsAudioTranscription) {
    ElMessage.warning('当前模型不支持语音识别，请切换支持语音的模型')
    return false
  }

  try {
    const config = await getAiConfig()
    if (config.capabilities?.supportsAudioTranscription) {
      return true
    }
    ElMessage.warning('当前模型不支持语音识别，请配置支持的模型')
    return false
  } catch (error: unknown) {
    console.error('Load AI config failed:', error)
    if (!isRequestErrorHandled(error)) {
      ElMessage.warning('暂时无法获取语音识别能力，请稍后再试')
    }
    return false
  }
}

function resolveRecordingMimeType(): string {
  const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']
  if (typeof MediaRecorder === 'undefined' || typeof MediaRecorder.isTypeSupported !== 'function') {
    return ''
  }
  return candidates.find(type => MediaRecorder.isTypeSupported(type)) || ''
}

function resolveAudioExtension(mimeType: string): string {
  if (mimeType.includes('mp4')) return 'm4a'
  if (mimeType.includes('mpeg')) return 'mp3'
  if (mimeType.includes('wav')) return 'wav'
  return 'webm'
}

function buildRecordedAudioFile(): File | null {
  if (recordedChunks.length === 0) return null
  const mimeType = recordedMimeType || recordedChunks[0]?.type || 'audio/webm'
  const blob = new Blob(recordedChunks, { type: mimeType })
  return new File([blob], `chat-recording.${resolveAudioExtension(mimeType)}`, { type: mimeType })
}

async function transcribeRecordedAudio(file: File | null) {
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
    void nextTick(() => {
      resizeChatTextarea()
      chatInputRef.value?.focus()
    })
    ElMessage.success('语音已转成文字，可继续编辑后发送')
  } catch (error: unknown) {
    console.error('Audio transcription failed:', error)
    if (!isRequestErrorHandled(error)) {
      ElMessage.warning('语音识别失败，请稍后重试')
    }
  } finally {
    if (currentToken === transcriptionToken) {
      isTranscribing.value = false
    }
  }
}

async function handleRecordedAudioStop() {
  const shouldSkip = skipNextTranscription
  skipNextTranscription = false
  isRecording.value = false
  const file = shouldSkip ? null : buildRecordedAudioFile()
  mediaRecorder = null
  releaseMediaStream()
  recordedChunks = []
  recordedMimeType = ''
  if (shouldSkip) return
  await transcribeRecordedAudio(file)
}

async function handleStartAudioRecording() {
  if (isRecording.value || isTranscribing.value) return
  const useMobileAudioApi = isMobile.value
  const hasAudioInput = useMobileAudioApi
    ? hasMobileAudioInputSupport()
    : Boolean(navigator.mediaDevices?.getUserMedia)
  const useMobileAudioFileCapture = shouldUseMobileAudioFileCapture({
    useMobileAudioApi,
    hasAudioInput,
    hasMediaRecorder: typeof MediaRecorder !== 'undefined',
    canCaptureAudioFile: canCaptureMobileAudioFile(),
    preferFileCapture: shouldPreferMobileAudioFileCapture()
  })

  if (useMobileAudioFileCapture) {
    speechInputBase = inputText.value.trim()
    const capturedFile = await captureMobileAudioFile()
    if (!capturedFile) return
    if (!(await ensureAudioTranscriptionSupported())) return
    await transcribeRecordedAudio(capturedFile)
    return
  }

  if (!(await ensureAudioTranscriptionSupported())) return
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
    const mimeType = resolveRecordingMimeType()
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
      void handleRecordedAudioStop()
    }
    mediaRecorder.onerror = (event: Event) => {
      console.error('MediaRecorder error:', event)
      ElMessage.warning('录音失败，请检查麦克风权限后重试')
    }
    mediaRecorder.start()
    isRecording.value = true
  } catch (error) {
    console.error('Start recording failed:', error)
    abortVoiceRecording()
    ElMessage.warning('无法启动录音，请检查浏览器和麦克风权限')
  }
}

function handleStopAudioRecording() {
  if (!mediaRecorder) return
  skipNextTranscription = false
  if (mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
}

function handleSendAction() {
  if (sendActionDisabled.value) return
  if (isRecording.value) {
    handleStopAudioRecording()
    return
  }
  if (hasComposerSendPayload.value) {
    void handleSend()
    return
  }
  void handleStartAudioRecording()
}

async function handleSend() {
  const text = inputText.value.trim()
  const hasFiles = selectedFiles.value.length > 0
  const hasKnowledge = selectedKnowledgeItems.value.length > 0
  if ((!text && !hasFiles && !hasKnowledge) || chatStore.currentSessionIsStreaming || isUploading.value) return
  if (!(await ensureAiAvailable())) return

  const effectiveAppCode = chatStore.selectedAppCode
  const content = text || (hasKnowledge ? '请结合选中的知识库文件回答' : '请分析这些文件')
  const selectedKnowledgeSnapshot = [...selectedKnowledgeItems.value]
  inputText.value = ''
  isPinnedToBottom.value = true
  void nextTick(resizeChatTextarea)

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

  const knowledgeIds = hasKnowledge ? selectedKnowledgeSnapshot.map(item => item.knowledgeId) : undefined
  if (hasKnowledge) {
    selectedKnowledgeItems.value = []
    if (chatStore.selectedAppCode === 'general') {
      chatStore.setSelectedAppCode('knowledge')
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

function resizeChatTextarea() {
  const el = chatInputRef.value
  if (!el) return

  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 128)}px`
}

function isMobileComposerFocused(): boolean {
  if (typeof document === 'undefined') return false
  return document.activeElement === chatInputRef.value
}

function updateMobileViewportTopOffset() {
  if (!isMobile.value || typeof window === 'undefined') {
    mobileViewportTopOffset.value = 0
    return
  }

  const visualViewport = window.visualViewport
  if (!visualViewport) {
    mobileViewportTopOffset.value = 0
    return
  }

  mobileViewportTopOffset.value = resolveMobileViewportTopOffset({
    layoutHeight: window.innerHeight,
    visualHeight: visualViewport.height,
    offsetTop: visualViewport.offsetTop
  })
}

function updateMobileKeyboardInset() {
  updateMobileViewportTopOffset()

  if (!isMobile.value || typeof window === 'undefined' || !isMobileComposerFocused()) {
    mobileKeyboardInset.value = 0
    nativeKeyboardInset.value = 0
    return
  }

  const visualViewport = window.visualViewport
  if (!visualViewport) {
    mobileKeyboardInset.value = 0
    return
  }

  mobileKeyboardInset.value = resolveMobileKeyboardInset(
    {
      layoutHeight: window.innerHeight,
      visualHeight: visualViewport.height,
      offsetTop: visualViewport.offsetTop
    },
    {
      gap: MOBILE_KEYBOARD_INSET_GAP_PX,
      threshold: MOBILE_KEYBOARD_VISIBLE_THRESHOLD_PX
    }
  )
}

function scheduleMobileKeyboardInsetUpdate() {
  if (mobileKeyboardInsetTimer != null) {
    clearTimeout(mobileKeyboardInsetTimer)
  }
  mobileKeyboardInsetTimer = setTimeout(() => {
    mobileKeyboardInsetTimer = null
    updateMobileKeyboardInset()
  }, 0)
}

function applyNativeKeyboardInset(keyboardHeight: number) {
  if (!isMobile.value || !isMobileComposerFocused()) return
  nativeKeyboardInset.value = Math.max(0, Math.round(keyboardHeight + MOBILE_KEYBOARD_INSET_GAP_PX))
  updateMobileViewportTopOffset()
}

function dismissMobileKeyboardForConversationScroll() {
  if (!isMobile.value || !isMobileComposerFocused()) return
  hideCapacitorKeyboard()
  chatInputRef.value?.blur()
  mobileKeyboardInset.value = 0
  nativeKeyboardInset.value = 0
}

function clearNativeKeyboardInset() {
  nativeKeyboardInset.value = 0
  scheduleMobileKeyboardInsetUpdate()
}

function registerMobileKeyboardInsetListeners() {
  if (typeof window === 'undefined' || typeof document === 'undefined') return

  removeNativeKeyboardInsetListeners = registerNativeKeyboardInsetListeners({
    onShow: applyNativeKeyboardInset,
    onHide: clearNativeKeyboardInset
  })
  window.visualViewport?.addEventListener('resize', scheduleMobileKeyboardInsetUpdate)
  window.visualViewport?.addEventListener('scroll', scheduleMobileKeyboardInsetUpdate)
  window.addEventListener('orientationchange', scheduleMobileKeyboardInsetUpdate)
  document.addEventListener('focusin', scheduleMobileKeyboardInsetUpdate, true)
  document.addEventListener('focusout', scheduleMobileKeyboardInsetUpdate, true)
}

function unregisterMobileKeyboardInsetListeners() {
  removeNativeKeyboardInsetListeners?.()
  removeNativeKeyboardInsetListeners = null

  if (typeof window !== 'undefined' && typeof document !== 'undefined') {
    window.visualViewport?.removeEventListener('resize', scheduleMobileKeyboardInsetUpdate)
    window.visualViewport?.removeEventListener('scroll', scheduleMobileKeyboardInsetUpdate)
    window.removeEventListener('orientationchange', scheduleMobileKeyboardInsetUpdate)
    document.removeEventListener('focusin', scheduleMobileKeyboardInsetUpdate, true)
    document.removeEventListener('focusout', scheduleMobileKeyboardInsetUpdate, true)
  }

  if (mobileKeyboardInsetTimer != null) {
    clearTimeout(mobileKeyboardInsetTimer)
    mobileKeyboardInsetTimer = null
  }
  mobileKeyboardInset.value = 0
  nativeKeyboardInset.value = 0
  mobileViewportTopOffset.value = 0
}

function sendQuickMessage(text: string) {
  inputText.value = text
  void nextTick(resizeChatTextarea)
  handleSend()
}

function renderAssistantMessage(content: string, isStreaming = false, isThinking = false): string {
  const normalized = normalizeAssistantMessageContent(content, isStreaming)
  return renderMarkdown(normalized || (isThinking ? getAssistantMessagePlaceholder(true) : ''), {
    streaming: isStreaming
  })
}

function htmlToPlainText(html: string): string {
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
    if (ok) {
      ElMessage.success('已复制')
    } else {
      ElMessage.warning('复制失败')
    }
  }
}

async function copyMessageContent(message: { content: string; isStreaming?: boolean }, kind: 'assistant' | 'user') {
  if (kind === 'assistant') {
    const html = renderAssistantMessage(message.content || '', Boolean(message.isStreaming))
    await copyToClipboard(htmlToPlainText(html))
    return
  }

  await copyToClipboard(message.content || '')
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

function closeChatUploadMenu() {
  chatUploadMenuVisible.value = false
}

function handleUploadMenuAddFile() {
  closeChatUploadMenu()
  handleUpload()
}

function handleUploadMenuChooseKnowledge() {
  closeChatUploadMenu()
  openKnowledgePicker()
}

function handleUploadMenuSelectApp(appCode: string) {
  closeChatUploadMenu()
  chatStore.setSelectedAppCode(chatStore.selectedAppCode === appCode ? 'general' : appCode)
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
  if (chatStore.selectedAppCode === 'general') {
    chatStore.setSelectedAppCode('knowledge')
  }
  showKnowledgeFollowUpChips.value = true
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
  if (result.files.length > 0) {
    showKnowledgeFollowUpChips.value = true
  }
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
  if (result.files.length > 0) {
    showKnowledgeFollowUpChips.value = true
  }
}

function removeSelectedFile(index: number) {
  const file = selectedFiles.value[index]
  if (file) {
    revokeSelectedFilePreviewUrl(file)
  }
  selectedFiles.value.splice(index, 1)
}

function removeSelectedKnowledge(knowledgeId: string) {
  selectedKnowledgeItems.value = selectedKnowledgeItems.value.filter(item => item.knowledgeId !== knowledgeId)
}

function applyKnowledgeDocPrompt(prompt: string) {
  inputText.value = prompt
  void nextTick(() => {
    resizeChatTextarea()
    chatStore.requestComposerFocus()
  })
}

function isImageAttachment(attachment?: ChatAttachmentVO | null): attachment is ChatAttachmentVO {
  return Boolean(attachment?.mimeType?.startsWith('image/'))
}

function isDocumentAttachment(attachment?: ChatAttachmentVO | null): attachment is ChatAttachmentVO {
  return Boolean(attachment) && !isImageAttachment(attachment)
}

function getInlineAttachments(message: { attachments?: ChatAttachmentVO[] }): ChatAttachmentVO[] {
  return (message.attachments || []).filter(isImageAttachment)
}

function getDocumentAttachments(message: { attachments?: ChatAttachmentVO[] }): ChatAttachmentVO[] {
  return (message.attachments || []).filter(isDocumentAttachment)
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const selectedFilePreviewUrlMap = new WeakMap<File, string>()

function getSelectedFilePreviewUrl(file: File): string {
  const cached = selectedFilePreviewUrlMap.get(file)
  if (cached) return cached

  const url = URL.createObjectURL(file)
  selectedFilePreviewUrlMap.set(file, url)
  return url
}

function revokeSelectedFilePreviewUrl(file: File) {
  const url = selectedFilePreviewUrlMap.get(file)
  if (!url) return

  URL.revokeObjectURL(url)
  selectedFilePreviewUrlMap.delete(file)
}

function revokeAllSelectedFilePreviewUrls() {
  for (const file of selectedFiles.value) {
    revokeSelectedFilePreviewUrl(file)
  }
}

function getFileExtension(fileName?: string): string {
  const name = fileName || ''
  if (!name.includes('.')) return ''
  return name.split('.').pop()?.toLowerCase() || ''
}

function getFriendlyFileKind(fileName: string, mimeType?: string): string {
  const normalizedType = (mimeType || '').toLowerCase()
  const extension = getFileExtension(fileName)
  if (normalizedType.includes('spreadsheetml') || normalizedType.includes('ms-excel') || extension === 'xlsx' || extension === 'xls') return 'Excel'
  if (normalizedType.includes('wordprocessingml') || normalizedType.includes('msword') || extension === 'docx' || extension === 'doc') return 'Word'
  if (normalizedType === 'application/pdf' || extension === 'pdf') return 'PDF'
  if (normalizedType.startsWith('text/') || extension === 'txt' || extension === 'md') return '文本'
  if (normalizedType.startsWith('image/')) return '图片'
  if (normalizedType.startsWith('audio/')) return '音频'
  if (normalizedType.startsWith('video/')) return '视频'
  return '文件'
}

function getDocumentIconMeta(fileName: string, mimeType?: string): { icon: string; bg: string; color: string } {
  const normalizedType = (mimeType || '').toLowerCase()
  const extension = getFileExtension(fileName)
  if (normalizedType.includes('spreadsheetml') || normalizedType.includes('ms-excel') || extension === 'xlsx' || extension === 'xls') {
    return { icon: 'table_chart', bg: 'bg-emerald-50', color: 'text-emerald-700' }
  }
  if (normalizedType.includes('wordprocessingml') || normalizedType.includes('msword') || extension === 'docx' || extension === 'doc') {
    return { icon: 'article', bg: 'bg-blue-50', color: 'text-blue-700' }
  }
  if (normalizedType === 'application/pdf' || extension === 'pdf') {
    return { icon: 'picture_as_pdf', bg: 'bg-red-50', color: 'text-red-600' }
  }
  if (normalizedType.startsWith('image/')) {
    return { icon: 'image', bg: 'bg-violet-50', color: 'text-violet-700' }
  }
  return { icon: 'description', bg: 'bg-[#0d0d0d0d]', color: 'text-[#0d0d0d]' }
}

function getChatDocumentSubtitle(file: File): string {
  return `${getFriendlyFileKind(file.name, file.type)} · ${formatFileSize(file.size)}`
}

function getChatDocIconMeta(file: File): { icon: string; bg: string; color: string } {
  return getDocumentIconMeta(file.name, file.type)
}

function getKnowledgeDocIconMeta(item: Knowledge): { icon: string; bg: string; color: string } {
  return getDocumentIconMeta(item.name, item.mimeType)
}

function getKnowledgeCardSubtitle(item: Knowledge): string {
  const kind = getFriendlyFileKind(item.name, item.mimeType)
  const formattedSize = item.fileSizeFormatted?.trim()
  if (formattedSize) return `${kind} · ${formattedSize}`
  const fileSize = resolveKnowledgeFileSizeBytes(item.fileSize)
  if (fileSize > 0) return `${kind} · ${formatFileSize(fileSize)}`
  return kind
}

async function handleNewSession() {
  await clearChatContextRouteQuery()
  isPinnedToBottom.value = true
  showScrollToBottomButton.value = false
  chatStore.clearMessages()
  await chatStore.startNewSession('新对话', undefined, undefined, chatStore.selectedAppCode)
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
}

async function handleSelectSession(sessionId: string, options: { keepRouteQuery?: boolean } = {}) {
  if (!options.keepRouteQuery) {
    await clearChatContextRouteQuery()
  }
  if (chatStore.currentSessionId === sessionId && currentView.value === 'chat') {
    activateChatPanel()
    return
  }
  isPinnedToBottom.value = true
  showScrollToBottomButton.value = false
  currentView.value = 'chat'
  await chatStore.selectSession(sessionId)
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
}

function getRouteQueryString(key: ChatContextQueryKey): string {
  const value = route.query[key]
  if (typeof value === 'string') return value.trim()
  if (Array.isArray(value)) return (typeof value[0] === 'string' ? value[0] : '').trim()
  return ''
}

function hasChatContextRouteQuery(): boolean {
  return CHAT_CONTEXT_QUERY_KEYS.some(key => Boolean(getRouteQueryString(key)))
}

async function clearChatContextRouteQuery() {
  if (!hasChatContextRouteQuery()) return
  const query = { ...route.query }
  CHAT_CONTEXT_QUERY_KEYS.forEach(key => {
    delete query[key]
  })
  await router.replace({ path: route.path, query })
}

function activateChatPanel() {
  isPinnedToBottom.value = true
  showScrollToBottomButton.value = false
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
}

function focusComposerWhenReady() {
  if (!isMobile.value) {
    chatStore.requestComposerFocus()
  }
}

async function openRouteCustomerChat(customerId: string) {
  try {
    const customer = await getCustomerDetail(customerId)
    await chatStore.openCustomerChat({
      customerId,
      companyName: customer.companyName || '客户对话'
    })
  } catch (error) {
    console.warn('Load route customer detail failed:', error)
    await chatStore.openCustomerChat({ customerId, companyName: '客户对话' })
  }
}

async function openRouteEmployeeChat(employeeId: string) {
  try {
    const employee = await getAddressBookDetail(employeeId)
    await chatStore.openEmployeeChat({
      userId: employee.userId || employeeId,
      realname: employee.realname || '员工'
    })
  } catch (error) {
    console.warn('Load route employee detail failed:', error)
    await chatStore.openEmployeeChat({ userId: employeeId, realname: '员工' })
  }
}

async function openRouteRelationChat(relationId: string) {
  try {
    const detail = await getRelationDetail(relationId)
    const relation = detail.relation
    await chatStore.openRelationChat({
      relationId: relation?.relationId || relationId,
      name: relation?.name || '关系人'
    })
  } catch (error) {
    console.warn('Load route relation detail failed:', error)
    await chatStore.openRelationChat({ relationId, name: '关系人' })
  }
}

async function openRouteProductChat(productId: string) {
  try {
    const product = await getProductDetail(productId)
    await chatStore.openProductChat({
      productId: product.productId || productId,
      productName: product.productName || '产品对话'
    })
  } catch (error) {
    console.warn('Load route product detail failed:', error)
    await chatStore.openProductChat({ productId, productName: '产品对话' })
  }
}

async function applyChatRouteQuery() {
  if (applyingChatRouteQuery) return

  const sessionId = getRouteQueryString('sessionId')
  const customerId = getRouteQueryString('customerId')
  const employeeId = getRouteQueryString('employeeId')
  const relationId = getRouteQueryString('relationId')
  const productId = getRouteQueryString('productId')

  if (!sessionId && !customerId && !employeeId && !relationId && !productId) return

  applyingChatRouteQuery = true
  try {
    if (sessionId) {
      if (!chatStore.sessions.some(session => session.sessionId === sessionId)) {
        await chatStore.fetchSessions()
      }
      if (!chatStore.sessions.some(session => session.sessionId === sessionId)) {
        ElMessage.warning('找不到该对话')
        await clearChatContextRouteQuery()
        return
      }
      await handleSelectSession(sessionId, { keepRouteQuery: true })
      focusComposerWhenReady()
      return
    }

    activateChatPanel()

    if (customerId) {
      await openRouteCustomerChat(customerId)
    } else if (employeeId) {
      await openRouteEmployeeChat(employeeId)
    } else if (relationId) {
      await openRouteRelationChat(relationId)
    } else if (productId) {
      await openRouteProductChat(productId)
    }

    focusComposerWhenReady()
  } finally {
    applyingChatRouteQuery = false
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

  mobileObjectDetailOpen.value = true

  if (chatObjectKind.value !== 'customer' && !objectPanelLoading.value) {
    void loadObjectPanelDetail()
  }
}

function closeMobileObjectDetail() {
  mobileObjectDetailOpen.value = false
}

function handleMobileObjectAddTask() {
  closeMobileObjectDetail()
  handleObjectAddTask()
}

function handleMobileObjectAddSchedule() {
  closeMobileObjectDetail()
  handleObjectAddSchedule()
}

function handleMobileObjectAddAttachment() {
  closeMobileObjectDetail()
  handleObjectAddAttachment()
}

function handleMobileObjectViewTask(task: Task) {
  closeMobileObjectDetail()
  handleObjectViewTask(task)
}

function handleMobileObjectViewSchedule(schedule: ScheduleVO) {
  closeMobileObjectDetail()
  handleObjectViewSchedule(schedule)
}

function handleMobileObjectViewAttachment(attachment: Knowledge) {
  closeMobileObjectDetail()
  handleObjectViewAttachment(attachment)
}

function handleMobileObjectEditProduct(product: ProductVO) {
  closeMobileObjectDetail()
  handleObjectEditProduct(product)
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

.wk-chat-shell {
  background:
    linear-gradient(180deg, var(--wk-bg-surface) 0%, rgb(var(--wk-bg-page-rgb) / 0.96) 62%, var(--wk-bg-page) 100%);
}

.wk-chat-customer-header {
  border-color: var(--wk-border-subtle);
  background: color-mix(in srgb, var(--wk-bg-surface) 92%, transparent);
  backdrop-filter: blur(14px);
}

.wk-chat-messages__inner {
  min-width: 0;
  width: 100%;
}

.wk-chat-messages--scrollable .wk-chat-messages__inner {
  width: calc(100% + var(--wk-chat-messages-scrollbar-offset, 0px));
}

.wk-chat-messages--mobile-floating-actions {
  --wk-mobile-floating-bar-clearance: calc(58px + max(0px, var(--safe-area-inset-top)));

  padding-top: 0 !important;
  scroll-padding-top: var(--wk-mobile-floating-bar-clearance);
}

.wk-chat-messages--mobile-floating-actions .wk-chat-messages__inner {
  padding-top: var(--wk-mobile-floating-bar-clearance);
}

.wk-chat-messages--empty-chat .wk-chat-messages__inner {
  display: flex;
  min-height: 100%;
  align-items: center;
  justify-content: center;
}

.wk-chat-messages--empty-chat.wk-chat-messages--mobile-floating-actions .wk-chat-messages__inner {
  align-items: flex-start;
  padding-top: clamp(220px, 42dvh, 340px);
}

.wk-chat-message {
  width: min(100%, 768px);
  min-width: min(100%, 468px);
  max-width: 768px;
  overflow-wrap: anywhere;
}

.wk-chat-message-actions--assistant,
.wk-chat-message-actions--user {
  position: relative;
  z-index: 0;
}

.wk-chat-message--user :deep(.rounded-\[24px\]) {
  background-color: var(--wk-bg-surface-muted) !important;
  color: var(--wk-text-primary) !important;
}

.wk-chat-message :deep(.wk-markdown) {
  color: var(--wk-text-primary, #0d0d0d);
  font-size: 15px;
  line-height: 1.75;
}

.wk-chat-message :deep(.wk-markdown > *:first-child) {
  margin-top: 0;
}

.wk-chat-message :deep(.wk-markdown > *:last-child) {
  margin-bottom: 0;
}

.wk-chat-message :deep(.wk-markdown p) {
  margin: 0 0 0.85em;
}

.wk-chat-message :deep(.wk-markdown ul),
.wk-chat-message :deep(.wk-markdown ol) {
  margin: 0.85em 0;
  padding-left: 1.5em;
}

.wk-chat-message :deep(.wk-markdown li) {
  margin: 0.25em 0;
}

.wk-chat-message :deep(.wk-markdown pre) {
  margin: 1em 0;
  overflow-x: auto;
  border-radius: 12px;
  background: #1f1e1c;
  padding: 14px 16px;
  color: #f7f7f7;
}

:global(html.dark) .wk-chat-message :deep(.wk-markdown pre) {
  background: #030712;
  color: #e5edf8;
}

.wk-chat-message :deep(.wk-markdown code) {
  border-radius: 6px;
  background: var(--wk-bg-surface-muted, #f4f4f4);
  padding: 0.15em 0.35em;
  color: var(--wk-text-primary, #0d0d0d);
  font-size: 0.92em;
}

.wk-chat-message :deep(.wk-markdown pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.wk-chat-message :deep(.wk-markdown blockquote) {
  margin: 1em 0;
  border-left: 3px solid var(--wk-border-strong, #d4d4d4);
  padding-left: 1em;
  color: var(--wk-text-secondary, #5f5f5f);
}

.wk-chat-message :deep(.wk-markdown table) {
  width: 100%;
  margin: 1em 0;
  border-collapse: collapse;
  font-size: 14px;
}

.wk-chat-message :deep(.wk-markdown th),
.wk-chat-message :deep(.wk-markdown td) {
  border: 1px solid var(--wk-border-subtle, #e5e5e5);
  padding: 8px 10px;
  text-align: left;
}

.wk-chat-message :deep(.wk-markdown th) {
  background: var(--wk-bg-surface-subtle, #fafafa);
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

.wk-mobile-chat-top-viewport-shield {
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 89;
  height: calc(64px + max(8px, var(--safe-area-inset-top)) + var(--wk-mobile-viewport-top-offset, 0px));
  pointer-events: none;
  background: var(--wk-bg-surface, #fff);
}

.wk-mobile-chat-floating-bar {
  display: flex;
  position: fixed;
  top: var(--safe-area-inset-top);
  right: var(--safe-area-inset-right);
  left: var(--safe-area-inset-left);
  z-index: 90;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
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

.wk-mobile-object-detail {
  position: fixed;
  inset: 0;
  z-index: 3400;
  pointer-events: auto;
}

.wk-mobile-object-detail__backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  border: 0;
  background: rgb(0 0 0 / 0.18);
  backdrop-filter: blur(1px);
}

.wk-mobile-object-detail__sheet {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  height: min(84dvh, calc(100dvh - max(16px, var(--safe-area-inset-top))));
  flex-direction: column;
  overflow: hidden;
  border-radius: 28px 28px 0 0;
  background: var(--wk-bg-surface, #fff);
  box-shadow: 0 -18px 55px rgb(15 23 42 / 0.18);
  transition: height 220ms cubic-bezier(0.22, 1, 0.36, 1), transform 220ms cubic-bezier(0.22, 1, 0.36, 1);
}

.wk-mobile-object-detail__header {
  flex-shrink: 0;
  border-bottom: 1px solid var(--wk-border-subtle, #ececec);
  background: color-mix(in srgb, var(--wk-bg-surface, #fff) 96%, transparent);
  padding: 12px 16px 14px;
  touch-action: none;
  user-select: none;
}

.wk-mobile-object-detail__handle {
  display: block;
  width: 44px;
  height: 5px;
  margin: 0 auto 10px;
  border-radius: 9999px;
  background: #d1d5db;
}

.wk-mobile-object-detail__title-row {
  display: flex;
  min-height: 44px;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.wk-mobile-object-detail__title {
  display: -webkit-box;
  min-width: 0;
  flex: 1;
  max-height: 44px;
  overflow: hidden;
  color: var(--wk-text-primary, #0d0d0d);
  font-size: 17px;
  font-weight: 700;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.wk-mobile-object-detail__close {
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

.wk-mobile-object-detail__close:active {
  transform: scale(0.94);
}

.wk-mobile-object-detail__body {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding-bottom: max(20px, var(--safe-area-inset-bottom));
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.wk-mobile-object-detail__body :deep(aside) {
  width: 100% !important;
  border-left: 0;
}

.wk-mobile-object-detail-enter-active,
.wk-mobile-object-detail-leave-active {
  transition: opacity 180ms ease;
}

.wk-mobile-object-detail-enter-from,
.wk-mobile-object-detail-leave-to {
  opacity: 0;
}

.wk-mobile-object-detail-enter-active .wk-mobile-object-detail__sheet,
.wk-mobile-object-detail-leave-active .wk-mobile-object-detail__sheet {
  transition: transform 220ms cubic-bezier(0.22, 1, 0.36, 1), height 220ms cubic-bezier(0.22, 1, 0.36, 1);
}

.wk-mobile-object-detail-enter-from .wk-mobile-object-detail__sheet,
.wk-mobile-object-detail-leave-to .wk-mobile-object-detail__sheet {
  transform: translateY(100%);
}

.wk-scroll-to-bottom-button {
  width: 44px;
  height: 44px;
  border-color: #d4d4d8;
  box-shadow:
    0 12px 34px rgb(15 23 42 / 0.11),
    0 0 18px 8px rgb(203 213 225 / 0.12);
}

.wk-chat-model-trigger {
  display: inline-flex;
  width: min(190px, 28vw);
  min-width: 132px;
  height: 40px;
  flex-shrink: 1;
  align-items: center;
  gap: 6px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  padding: 0 10px;
  color: #0d0d0d;
  font-size: 13px;
  line-height: 1;
  text-align: left;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.04);
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

.wk-chat-model-trigger:hover:not(:disabled) {
  border-color: #d1d5db;
  background: #f9fafb;
}

.wk-chat-model-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.wk-chat-model-trigger__icon {
  display: inline-flex;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 7px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 11px;
  font-weight: 700;
}

.wk-chat-model-trigger__label {
  display: block;
}

.wk-chat-model-menu {
  display: flex;
  max-height: min(420px, 60vh);
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  padding: 6px;
}

.wk-chat-model-menu__group-label {
  padding: 8px 8px 4px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.wk-chat-model-menu__item,
.wk-chat-model-menu__more {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  padding: 9px 10px;
  text-align: left;
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.wk-chat-model-menu__item:hover,
.wk-chat-model-menu__more:hover {
  background: #f5f5f5;
}

.wk-chat-model-menu__logo {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 10px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.wk-chat-model-menu__more {
  margin-top: 4px;
  border-top: 1px solid #f1f5f9;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.wk-chat-model-menu__empty {
  margin: 0;
  padding: 14px 10px;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.6;
}

.wk-chat-upload-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 6px;
}

.wk-chat-upload-menu__item {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  padding: 10px;
  text-align: left;
  transition:
    background-color 160ms ease,
    opacity 160ms ease;
}

.wk-chat-upload-menu__item:hover:not(:disabled) {
  background: #f5f5f5;
}

.wk-chat-upload-menu__item:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.wk-chat-upload-menu__icon {
  display: inline-flex;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  background: #f1f5f9;
  color: #475569;
}

.wk-recording-indicator {
  position: relative;
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.wk-recording-indicator::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 999px;
  background: rgb(255 255 255 / 0.18);
  animation: wk-recording-pulse 1.2s ease-out infinite;
}

.wk-recording-indicator__stop {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  line-height: 1;
  opacity: 0;
  transition: opacity 140ms ease;
}

.wk-recording-indicator__bars {
  position: relative;
  display: inline-flex;
  height: 18px;
  align-items: center;
  gap: 2px;
  transition: opacity 140ms ease;
}

.wk-recording-indicator__bars span {
  display: block;
  width: 3px;
  border-radius: 999px;
  background: currentColor;
  animation: wk-recording-bar 0.72s ease-in-out infinite;
}

.wk-recording-indicator__bars span:nth-child(1) {
  height: 8px;
  animation-delay: 0s;
}

.wk-recording-indicator__bars span:nth-child(2) {
  height: 15px;
  animation-delay: 0.12s;
}

.wk-recording-indicator__bars span:nth-child(3) {
  height: 11px;
  animation-delay: 0.24s;
}

.wk-recording-indicator__bars span:nth-child(4) {
  height: 17px;
  animation-delay: 0.36s;
}

.group\/chat-send:hover .wk-recording-indicator__stop {
  opacity: 1;
}

.group\/chat-send:hover .wk-recording-indicator__bars {
  opacity: 0;
}

@keyframes wk-recording-pulse {
  0% {
    opacity: 0.85;
    transform: scale(0.8);
  }
  100% {
    opacity: 0;
    transform: scale(1.35);
  }
}

@keyframes wk-recording-bar {
  0%, 100% {
    transform: scaleY(0.68);
  }
  50% {
    transform: scaleY(1);
  }
}

@media (max-width: 640px) {
  .wk-chat-model-trigger {
    width: 40px;
    min-width: 40px;
    flex: 0 0 40px;
    justify-content: center;
    padding: 0;
  }

  .wk-chat-model-trigger__label,
  .wk-chat-model-trigger > .material-symbols-outlined {
    display: none;
  }
}

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

.wk-chat-messages--mobile-header {
  padding-top: calc(62px + max(8px, var(--safe-area-inset-top))) !important;
  scroll-padding-top: calc(62px + max(8px, var(--safe-area-inset-top)));
}

</style>

<style>
.wk-chat-model-popper.el-popper {
  border: 1px solid #e5e7eb !important;
  border-radius: 16px !important;
  box-shadow: 0 18px 60px rgb(15 23 42 / 0.16) !important;
}

.wk-chat-model-popper .el-popper__arrow::before {
  border-color: #e5e7eb !important;
}

.wk-chat-upload-menu-popper.el-popper {
  border: 1px solid #e5e7eb !important;
  border-radius: 16px !important;
  box-shadow: 0 18px 60px rgb(15 23 42 / 0.16) !important;
}

.wk-chat-upload-menu-popper .el-popper__arrow::before {
  border-color: #e5e7eb !important;
}
</style>
