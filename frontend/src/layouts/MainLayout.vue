<template>
  <div class="flex wk-screen bg-background-light">
    <aside
      v-if="!isMobile"
      class="wk-primary-sidebar relative z-[110] flex wk-screen flex-shrink-0 flex-col overflow-x-visible overflow-y-visible border-r border-[#ececec] bg-white transition-[width] duration-200 ease-in-out"
      :class="primarySidebarCollapsed ? 'w-[52px]' : 'w-64'"
      @mouseenter="onCollapsedPrimarySidebarEnter"
      @mouseleave="onCollapsedPrimarySidebarLeave"
    >
      <!-- 展开：logo + 标题 + 折叠按钮；折叠：仅顶栏切换（ChatGPT 式） -->
      <div
        v-if="!primarySidebarCollapsed"
        class="relative z-30 flex items-start justify-between gap-2 overflow-visible px-3 pb-1 pt-3"
      >
        <div class="flex min-w-0 flex-1 items-start gap-2.5 pl-[10px]">
          <div
            v-if="enterpriseStore.hasLogo"
            class="size-8 shrink-0 overflow-hidden rounded-lg bg-transparent"
          >
            <img :src="enterpriseStore.logoUrl!" class="h-full w-full object-cover" alt="logo" />
          </div>
          <div v-else class="size-8 shrink-0 overflow-hidden rounded-lg bg-transparent">
            <img :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
          </div>
          <div v-if="false" class="min-w-0 flex-1 pr-1 pt-0.5">
            <h1 class="line-clamp-2 text-[15px] font-semibold leading-snug text-[#0d0d0d]">
              {{ enterpriseStore.displayName }}
            </h1>
            <p class="mt-0.5 line-clamp-1 text-[11px] uppercase tracking-wider text-slate-500">
              {{ enterpriseStore.displayDescription }}
            </p>
          </div>
        </div>
        <div class="relative shrink-0 pt-0.5">
          <button
            type="button"
            class="group/sb-toggle relative flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#efefef]"
            aria-label="关闭边栏"
            @click="primarySidebarCollapsed = true"
          >
            <WkIcon name="fold" :size="18" class="shrink-0" />
            <span
              class="pointer-events-none absolute left-full top-1/2 z-[200] ml-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/sb-toggle:opacity-100"
              role="tooltip"
            >
              关闭边栏
            </span>
          </button>
        </div>
      </div>
      <div v-else class="relative z-30 flex justify-center overflow-visible py-2 px-0 pl-[5px]">
        <button
          type="button"
          class="relative flex size-8 items-center justify-center overflow-hidden rounded-lg bg-[#f5f5f5] text-[#0d0d0d] transition-colors hover:bg-[#ececec]"
          aria-label="打开边栏"
          @click="primarySidebarCollapsed = false"
        >
          <span
            class="absolute inset-0 flex items-center justify-center transition-opacity duration-150"
            :class="collapsedSidebarAsideHovered ? 'opacity-0' : 'opacity-100'"
            aria-hidden="true"
          >
            <span class="size-7 shrink-0 overflow-hidden rounded-lg border border-[#ececec] bg-transparent">
              <img :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
            </span>
          </span>
          <span
            class="absolute inset-0 flex items-center justify-center transition-opacity duration-150"
            :class="collapsedSidebarAsideHovered ? 'opacity-100' : 'opacity-0'"
            aria-hidden="true"
          >
            <WkIcon name="fold" :size="18" class="shrink-0" />
          </span>
          <span
            class="pointer-events-none absolute left-full top-1/2 z-[200] ml-2 -translate-y-1/2 whitespace-nowrap rounded-full bg-black px-3 py-1.5 text-[13px] font-medium text-white shadow-md transition-opacity duration-150"
            :class="collapsedSidebarAsideHovered ? 'opacity-100' : 'opacity-0'"
            role="tooltip"
          >
            打开边栏
          </span>
        </button>
      </div>
      <!-- pl-[10px] pr-[10px] py-[6px] mt-[0px] ml-[2px] mr-[6px] -->
      <!-- Chat: New session (fixed, not scroll with nav) -->
      <div class="px-3 pt-1 mb-[4px]" :class="primarySidebarCollapsed ? '!px-2' : ''">
        <button
          class="flex w-full items-center rounded-lg py-2 text-sm font-normal text-[#0d0d0d] transition-colors hover:bg-[#f9f9f9] pl-[10px] pr-[10px] ml-[2px] mr-[6px] "
          :class="primarySidebarCollapsed ? 'justify-center' : 'gap-2'"
          :title="primarySidebarCollapsed ? '新对话' : undefined"
          @click="handleNewSession"
        >
          <!-- <span class="material-symbols-outlined wk-plus-button-icon text-[18px] leading-none">edit_square</span> -->
          <WkIcon name="new-chat" :size="18" class="shrink-0" />
          <span style="margin-left: 2px;" v-if="!primarySidebarCollapsed">新对话</span>
        </button>
      </div>

      <!-- <div
        v-if="primaryNavHasScrollbar"
        class="mx-3 h-px bg-slate-100"
        :class="primarySidebarCollapsed ? '!mx-2' : ''"
      /> -->

      <nav
        ref="primaryNavRef"
        class="wk-scrollbar-gutter-stable flex-1 overflow-y-auto px-3 pb-4"
        :class="primarySidebarCollapsed ? '!pl-2 !pr-[3px]' : ''"
      >
        <div class="space-y-1">

          <template v-for="group in pcMainNavGroups" :key="group.title || 'default'">
            <div v-if="group.title && !primarySidebarCollapsed" class="pt-[8px] pb-[3px]" style="margin-top: 8px; margin-bottom: 0px;">
              <p class="px-3 text-[14px] uppercase font-semibold tracking-tight text-[#0d0d0d]">{{ group.title }}</p>
            </div>
            <button
              v-for="item in group.items"
              :key="item.key"
              :title="primarySidebarCollapsed ? item.label : undefined"
              @click="handlePrimaryNavClick(item)"
              class="flex w-full items-center rounded-lg pt-[8px] pb-[8px] transition-colors pl-[10px] pr-[10px] ml-[2px] mr-[6px]"
              :class="[
                isPrimaryActive(item) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] hover:bg-[#f9f9f9]',
                primarySidebarCollapsed ? 'justify-center' : 'gap-2',
              ]"
            >
              <WkIcon :name="item.icon" :box-size="20" class="shrink-0" />
              <span v-if="!primarySidebarCollapsed" class="truncate text-sm font-normal">{{ item.label }}</span>
              <span
                v-if="item.children?.length && !primarySidebarCollapsed"
                class="material-symbols-outlined ml-auto inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none"
                :class="isPrimarySelected(item) ? 'text-[#c9c9c9]' : 'text-[#c9c9c9]'"
              >
                chevron_right
              </span>
            </button>
          </template>

          <!-- Chat: recent sessions under Knowledge menu -->
          <template v-if="!primarySidebarCollapsed">
          <!-- <div class="pt-2" /> -->
          <!-- <div class="mx-1 h-px bg-slate-100" /> -->
          <div class="space-y-1 pt-0">
            <button
              type="button"
              class="group flex w-full items-center justify-between gap-1 rounded-lg pb-0 pl-3 pr-1 text-left transition-colors mt-[12px] mb-[0px]"
              :title="recentChatSessionsExpanded ? '收起最近对话' : '展开最近对话'"
              :aria-expanded="recentChatSessionsExpanded"
              aria-controls="recent-chat-sessions-panel"
              @click="recentChatSessionsExpanded = !recentChatSessionsExpanded"
            >
              <span class="min-w-0 text-[14px] font-semibold tracking-tight text-[#0d0d0d]">最近</span>
              <span
                class="flex size-7 shrink-0 items-center justify-center rounded-md text-slate-400 transition-all duration-150 "
                :class="recentChatSessionsExpanded ? 'opacity-0 group-hover:opacity-100' : 'opacity-100'"
                aria-hidden="true"
              >
                <span class="material-symbols-outlined inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none text-[#c9c9c9]">
                  {{ recentChatSessionsExpanded ? 'keyboard_arrow_down' : 'chevron_right' }}
                </span>
              </span>
            </button>

            <div id="recent-chat-sessions-panel" v-show="recentChatSessionsExpanded">
            <div v-if="chatStore.sessionsLoading && chatStore.sessions.length === 0" class="flex justify-center py-6">
              <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
            </div>

            <div v-else-if="chatStore.sessions.length === 0" class="px-3 py-6 text-center text-xs text-slate-400">
              暂无对话记录
            </div>

            <template v-else>
              <template v-if="groupedChatSessions.today.length > 0">
                <!-- <p class="px-3 pt-2 pb-1 text-xs font-bold uppercase tracking-widest text-slate-400">今天</p> -->
                <button
                  v-for="session in groupedChatSessions.today"
                  :key="session.sessionId"
                  class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isSessionActive(session.sessionId)
                    ? 'bg-[#f3f3f3]'
                    : 'hover:bg-[#f9f9f9]'"
                  @click="handleSelectSession(session.sessionId)"
                >
                  <ChatSessionActionsPopover
                    :session="session"
                    :active="isSessionActive(session.sessionId)"
                    @share="handleShareChatSession"
                    @delete="handleDeleteSession"
                  />
                  <!-- <span class="text-xs font-medium text-slate-400">{{ formatSessionTime(session.updateTime || session.createTime) }}</span> -->
                </button>
              </template>

              <template v-if="groupedChatSessions.yesterday.length > 0">
                <!-- <p class="px-3 pt-2 pb-1 text-xs font-bold uppercase tracking-widest text-slate-400">昨天</p> -->
                <button
                  v-for="session in groupedChatSessions.yesterday"
                  :key="session.sessionId"
                  class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[0px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isSessionActive(session.sessionId)
                    ? 'bg-[#f3f3f3]'
                    : 'hover:bg-[#f9f9f9]'"
                  @click="handleSelectSession(session.sessionId)"
                >
                  <ChatSessionActionsPopover
                    :session="session"
                    :active="isSessionActive(session.sessionId)"
                    @share="handleShareChatSession"
                    @delete="handleDeleteSession"
                  />
                  <!-- <span class="text-xs font-medium text-slate-400">{{ formatSessionTime(session.updateTime || session.createTime) }}</span> -->
                </button>
              </template>

              <template v-if="groupedChatSessions.earlier.length > 0">
                <!-- <p class="px-3 pt-2 pb-1 text-xs font-bold uppercase tracking-widest text-slate-400">更早</p> -->
                <button
                  v-for="session in groupedChatSessions.earlier"
                  :key="session.sessionId"
                  class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isSessionActive(session.sessionId)
                    ? 'bg-[#f3f3f3]'
                    : 'hover:bg-[#f9f9f9]'"
                  @click="handleSelectSession(session.sessionId)"
                >
                  <ChatSessionActionsPopover
                    :session="session"
                    :active="isSessionActive(session.sessionId)"
                    @share="handleShareChatSession"
                    @delete="handleDeleteSession"
                  />
                  <!-- <span class="text-xs font-medium text-slate-400">{{ formatSessionTime(session.updateTime || session.createTime) }}</span> -->
                </button>
              </template>
            </template>
            </div>
          </div>
          </template>

        </div>
      </nav>

      <div v-if="false && showConfigSection" class="shrink-0 border-t border-slate-200 px-4 py-3">
        <div class="pb-2 pt-1">
          <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">配置与服务</p>
        </div>
        <button
          v-for="item in configNavItems"
          :key="item.route"
          @click="navigateTo(item.route, item.query)"
          class="flex w-full items-center gap-2 rounded-lg px-3 py-2 transition-colors"
          :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-[#0d0d0d] hover:bg-slate-100'"
        >
          <WkIcon :name="item.icon" :size="22" class="shrink-0" />
          <span class="truncate text-sm font-medium">{{ item.label }}</span>
        </button>
      </div>

      <div class="border-t border-[#ececec] p-3" :class="primarySidebarCollapsed ? '!px-2 !py-2' : ''">
        <div
          class="flex cursor-pointer items-center rounded-xl bg-[#fff] transition-colors hover:bg-[#f9f9f9]"
          :class="primarySidebarCollapsed ? 'justify-center' : 'gap-3 p-2'"
          :title="primarySidebarCollapsed ? (userStore.realname || userStore.username || '用户') : undefined"
          @click="showUserMenu = !showUserMenu"
        >
          <div v-if="userStore.avatar" class="size-8 overflow-hidden rounded-full">
            <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
          </div>
          <div v-else class="flex size-8 items-center justify-center rounded-full bg-primary text-xs font-bold text-white">
            {{ userStore.realname?.charAt(0) || 'U' }}
          </div>
          <template v-if="!primarySidebarCollapsed">
            <div class="min-w-0 flex-1">
              <p class="truncate text-xs font-semibold text-slate-900">{{ userStore.realname || userStore.username }}</p>
              <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
            </div>
            <span class="material-symbols-outlined text-sm text-slate-400">unfold_more</span>
          </template>
        </div>
      </div>

      <Transition name="profile-drawer">
        <div
          v-if="showUserMenu"
          class="absolute bottom-0 left-0 right-0 z-20 overflow-hidden rounded-t-2xl border-t border-slate-200 bg-white shadow-[0_-8px_30px_rgb(0,0,0,0.12)]"
        >
          <div class="p-4">
            <div class="space-y-1">
              <div class="flex items-center justify-between px-3 py-2">
                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">个人账户</p>
                <button class="text-slate-300 transition-colors hover:text-slate-500" @click="showUserMenu = false">
                  <span class="material-symbols-outlined text-sm">close</span>
                </button>
              </div>

              <div class="mb-4 flex items-center gap-3 px-3 py-2">
                <div v-if="userStore.avatar" class="size-10 overflow-hidden rounded-full bg-slate-300">
                  <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
                </div>
                <div v-else class="flex size-10 items-center justify-center rounded-full bg-primary text-sm font-bold text-white">
                  {{ userStore.realname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-bold text-slate-900">{{ userStore.realname || userStore.username }}</p>
                  <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
                </div>
              </div>

              <div class="mx-3 my-1 h-px bg-slate-100" />

              <button
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-slate-700 transition-colors hover:bg-slate-50"
                @click="handleOpenAccountSettings"
              >
                <WkIcon name="set" :size="24" class="text-slate-400 transition-colors group-hover:text-primary" />
                <span class="text-sm font-medium">账号设置</span>
              </button>

              <button
                v-for="item in configNavItems"
                :key="item.route"
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors"
                :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
                @click="navigateTo(item.route, item.query)"
              >
                <WkIcon
                  :name="item.icon"
                  :size="24"
                  class="transition-colors"
                  :class="isActive(item.route, item.query) ? 'text-primary' : 'text-slate-400 group-hover:text-primary'"
                />
                <span class="text-sm font-medium">{{ item.label }}</span>
              </button>

              <button
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-rose-600 transition-colors hover:bg-rose-50"
                @click="handleLogout"
              >
                <span class="material-symbols-outlined text-rose-400 transition-colors group-hover:text-rose-600">logout</span>
                <span class="text-sm font-medium">退出登录</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </aside>

    <Transition name="secondary-panel">
      <aside
        v-if="!isMobile && showSecondaryPanel"
        class="relative z-10 flex wk-screen w-64 flex-shrink-0 flex-col border-r border-slate-200 bg-white"
      >
        <div class="flex items-center justify-between px-4 py-5">
          <p class="truncate text-xs font-bold tracking-wider text-slate-400">{{ secondaryTitle }}</p>
          <!-- <button class="text-slate-300 transition-colors hover:text-slate-500" @click="selectedPrimaryKey = ''">
            <span class="material-symbols-outlined text-sm">close</span>
          </button> -->
        </div>

        <div class="flex-1 overflow-y-auto px-4 pb-4">
          <div class="space-y-1">
            <button
              v-for="child in activeSecondaryItems"
              :key="child.key"
              class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors"
              :class="isActive(child.route, child.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
              @click="navigateTo(child.route, child.query)"
            >
              <WkIcon
                v-if="child.icon"
                :name="child.icon"
                :size="22"
                class="shrink-0"
                :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-400'"
              />
              <span v-else class="material-symbols-outlined shrink-0 text-[20px]" :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-300'">
                subdirectory_arrow_right
              </span>
              <span class="truncate text-sm font-medium">{{ child.label }}</span>
            </button>
          </div>
        </div>
      </aside>
    </Transition>

    <div v-if="isMobile" class="fixed left-0 right-0 top-0 z-50 flex h-14 items-center gap-2 border-b border-slate-200 bg-white px-4">
      <button
        @click="drawerVisible = true"
        class="flex size-10 shrink-0 items-center justify-center rounded-lg text-[#0d0d0d] hover:bg-slate-100"
      >
        <span class="material-symbols-outlined">menu</span>
      </button>

      <div class="flex min-w-0 flex-1 items-center gap-2">
        <div ref="searchPanelRef" class="relative w-full min-w-0">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-xl text-slate-400">search</span>
          <input
            v-model="globalSearchKeyword"
            type="text"
            class="w-full rounded-lg border-none bg-slate-100 py-2 pl-10 pr-4 text-sm outline-none transition-all focus:ring-2 focus:ring-primary/50"
            placeholder="搜索客户、联系人、任务、日程、知识库..."
            @focus="handleGlobalSearchFocus"
            @keydown.enter.prevent="handleGlobalSearchEnter"
            @keydown.down.prevent="handleGlobalSearchArrow(1)"
            @keydown.up.prevent="handleGlobalSearchArrow(-1)"
            @keydown.esc.prevent="closeGlobalSearchDropdown"
            @input="debouncedGlobalSearch"
          />

          <Transition name="dropdown">
            <div
              v-if="showGlobalSearchDropdown"
              class="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-[210] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-900/10"
              @mousedown.prevent
            >
              <div v-if="globalSearchLoading" class="flex items-center gap-3 px-4 py-4 text-sm text-slate-500">
                <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
                正在搜索...
              </div>

              <template v-else-if="globalSearchResults.length > 0">
                <button
                  v-for="(result, index) in globalSearchResults"
                  :key="`${result.entityType}-${result.entityId}`"
                  type="button"
                  class="flex w-full items-start gap-3 px-4 py-3 text-left transition-colors"
                  :class="index === activeSearchResultIndex ? 'bg-primary/5' : 'hover:bg-slate-50'"
                  @mouseenter="activeSearchResultIndex = index"
                  @click="navigateToSearchResult(result)"
                >
                  <div class="mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                    <WkIcon :name="getSearchResultIcon(result.entityType)" :size="18" />
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-semibold text-slate-900">{{ result.title }}</p>
                    <p v-if="result.subtitle" class="mt-0.5 truncate text-xs text-slate-500">{{ result.subtitle }}</p>
                    <p v-if="result.summary" class="line-clamp-2 mt-1 text-xs leading-5 text-slate-400">{{ result.summary }}</p>
                  </div>
                  <span class="material-symbols-outlined mt-1 shrink-0 text-base text-slate-300">chevron_right</span>
                </button>
                <div class="border-t border-slate-100 px-4 py-2 text-center text-xs text-slate-400">
                  显示前 {{ globalSearchResults.length }} 条，共 {{ globalSearchTotal }} 条结果
                </div>
              </template>

              <div v-else class="px-4 py-5 text-center text-sm text-slate-400">
                没有找到匹配结果
              </div>
            </div>
          </Transition>
        </div>


      </div>

      <div class="flex shrink-0 items-center gap-2">
        <AiQuotaHeaderPopover />
        <!-- <button class="flex size-10 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100">
          <span class="material-symbols-outlined">notifications</span>
        </button> -->
        <!-- <button
          type="button"
          class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full bg-slate-100"
          @click="showUserMenu = !showUserMenu"
        >
          <img v-if="userStore.avatar" :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
          <span v-else class="text-xs font-bold text-primary">{{ userStore.realname?.charAt(0) || 'U' }}</span>
        </button> -->
      </div>
    </div>

    <Teleport to="body">
      <Transition name="drawer-overlay">
        <div v-if="isMobile && drawerVisible" class="fixed inset-0 z-[100] bg-slate-900/40 backdrop-blur-sm" @click="drawerVisible = false"></div>
      </Transition>
      <Transition name="drawer-panel">
        <aside v-if="isMobile && drawerVisible" class="fixed bottom-0 left-0 top-0 z-[101] flex w-72 flex-col bg-white shadow-2xl">
          <div class="flex items-center gap-3 pt-6 pb-[6px] px-6">
            <div v-if="enterpriseStore.hasLogo" class="size-10 flex-shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-transparent">
              <img :src="enterpriseStore.logoUrl!" class="h-full w-full object-cover" alt="logo" />
            </div>
            <div v-else class="size-10 flex-shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-transparent">
              <img :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
            </div>
            <div class="min-w-0">
              <h1 class="line-clamp-2 text-lg font-bold leading-tight text-slate-900">{{ enterpriseStore.displayName }}</h1>
              <p class="text-xs uppercase tracking-widest text-slate-500">{{ enterpriseStore.displayDescription }}</p>
            </div>
          </div>
          <nav class="wk-scrollbar-gutter-stable flex-1 space-y-1 overflow-y-auto px-4 py-4">
            <template v-for="group in mobileMainNavGroups" :key="group.title || 'default'">
              <div v-if="group.title" class="pb-2 pt-4">
                <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">{{ group.title }}</p>
              </div>
              <div v-for="item in group.items" :key="item.key" class="space-y-1">
                <button
                  class="flex w-full items-center gap-3 rounded-lg px-3 py-2 transition-colors"
                  :class="isPrimaryActive(item) ? 'bg-primary/10 text-primary' : 'text-[#0d0d0d] hover:bg-slate-100'"
                  @click="handleMobilePrimaryNavClick(item)"
                >
                  <WkIcon :name="item.icon" :size="22" class="shrink-0" />
                  <span class="text-sm font-normal">{{ item.label }}</span>
                  <span
                    v-if="item.children?.length"
                    class="material-symbols-outlined ml-auto shrink-0 text-base transition-transform"
                    :class="isMobilePrimaryExpanded(item.key) ? 'rotate-90 text-primary' : 'text-slate-300'"
                  >
                    chevron_right
                  </span>
                </button>

                <div v-if="item.children?.length && isMobilePrimaryExpanded(item.key)" class="ml-3 border-l border-slate-100 pl-3">
                  <button
                    v-for="child in item.children"
                    :key="child.key"
                    class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors"
                    :class="isActive(child.route, child.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
                    @click="mobileNavigate(child.route, child.query)"
                  >
                    <WkIcon
                      v-if="child.icon"
                      :name="child.icon"
                      :size="20"
                      class="shrink-0"
                      :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-400'"
                    />
                    <span v-else class="material-symbols-outlined shrink-0 text-[18px]" :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-300'">
                      subdirectory_arrow_right
                    </span>
                    <span class="truncate text-sm font-medium">{{ child.label }}</span>
                  </button>
                </div>
              </div>
            </template>

            <div v-if="showConfigSection" class="pb-2 pt-4">
              <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">配置与服务</p>
            </div>

            <template v-if="showConfigSection">
              <button
                v-for="item in configNavItems"
                :key="item.route"
                @click="mobileNavigate(item.route, item.query)"
                class="flex w-full items-center gap-3 rounded-lg px-3 py-2 transition-colors"
                :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-[#0d0d0d] hover:bg-slate-100'"
              >
                <WkIcon :name="item.icon" :size="22" class="shrink-0" />
                <span class="text-sm font-medium">{{ item.label }}</span>
              </button>
            </template>
          </nav>

          <div class="border-t border-slate-200 p-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
            <div class="flex cursor-pointer items-center gap-3 rounded-xl bg-slate-50 p-3" @click="showUserMenu = !showUserMenu">
              <div v-if="userStore.avatar" class="size-9 overflow-hidden rounded-full">
                <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
              </div>
              <div v-else class="flex size-9 items-center justify-center rounded-full bg-primary text-xs font-bold text-white">
                {{ userStore.realname?.charAt(0) || 'U' }}
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-semibold text-slate-900">{{ userStore.realname || userStore.username }}</p>
                <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
              </div>
              <span class="material-symbols-outlined text-base text-slate-400">unfold_more</span>
            </div>
          </div>

          <Transition name="profile-drawer">
            <div
              v-if="isMobile && drawerVisible && showUserMenu"
              class="absolute bottom-0 left-0 right-0 z-20 overflow-hidden rounded-t-2xl border-t border-slate-200 bg-white shadow-[0_-8px_30px_rgb(0,0,0,0.12)]"
            >
              <div class="p-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
                <div class="space-y-1">
                  <div class="flex items-center justify-between px-3 py-2">
                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">个人账户</p>
                    <button class="text-slate-300 transition-colors hover:text-slate-500" @click="showUserMenu = false">
                      <span class="material-symbols-outlined text-sm">close</span>
                    </button>
                  </div>

                  <div class="mb-4 flex items-center gap-3 px-3 py-2">
                    <div v-if="userStore.avatar" class="size-10 overflow-hidden rounded-full bg-slate-300">
                      <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
                    </div>
                    <div v-else class="flex size-10 items-center justify-center rounded-full bg-primary text-sm font-bold text-white">
                      {{ userStore.realname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-bold text-slate-900">{{ userStore.realname || userStore.username }}</p>
                      <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
                    </div>
                  </div>

                  <div class="mx-3 my-1 h-px bg-slate-100" />

                  <button
                    class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-slate-700 transition-colors hover:bg-slate-50"
                    @click="handleOpenAccountSettings"
                  >
                    <WkIcon name="set" :size="24" class="text-slate-400 transition-colors group-hover:text-primary" />
                    <span class="text-sm font-medium">账号设置</span>
                  </button>

                  <button
                    v-for="item in configNavItems"
                    :key="item.route"
                    class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors"
                    :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
                    @click="navigateTo(item.route, item.query)"
                  >
                    <WkIcon
                      :name="item.icon"
                      :size="24"
                      class="transition-colors"
                      :class="isActive(item.route, item.query) ? 'text-primary' : 'text-slate-400 group-hover:text-primary'"
                    />
                    <span class="text-sm font-medium">{{ item.label }}</span>
                  </button>

                  <button
                    class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-rose-600 transition-colors hover:bg-rose-50"
                    @click="handleLogout"
                  >
                    <span class="material-symbols-outlined text-rose-400 transition-colors group-hover:text-rose-600">logout</span>
                    <span class="text-sm font-medium">退出登录</span>
                  </button>
                </div>
              </div>
            </div>
          </Transition>
        </aside>
      </Transition>

      <Transition name="drawer-overlay">
        <div
          v-if="isMobile && !drawerVisible && showUserMenu"
          class="fixed inset-0 z-[110] bg-slate-900/40 backdrop-blur-sm"
          @click="showUserMenu = false"
        />
      </Transition>
      <Transition name="profile-drawer">
        <div
          v-if="isMobile && !drawerVisible && showUserMenu"
          class="fixed bottom-0 left-0 right-0 z-[111] overflow-hidden rounded-t-2xl border-t border-slate-200 bg-white shadow-[0_-8px_30px_rgb(0,0,0,0.12)]"
        >
          <div class="p-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
            <div class="space-y-1">
              <div class="flex items-center justify-between px-3 py-2">
                <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">个人账户</p>
                <button class="text-slate-300 transition-colors hover:text-slate-500" @click="showUserMenu = false">
                  <span class="material-symbols-outlined text-sm">close</span>
                </button>
              </div>

              <div class="mb-4 flex items-center gap-3 px-3 py-2">
                <div v-if="userStore.avatar" class="size-10 overflow-hidden rounded-full bg-slate-300">
                  <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
                </div>
                <div v-else class="flex size-10 items-center justify-center rounded-full bg-primary text-sm font-bold text-white">
                  {{ userStore.realname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-bold text-slate-900">{{ userStore.realname || userStore.username }}</p>
                  <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
                </div>
              </div>

              <div class="mx-3 my-1 h-px bg-slate-100" />

              <button
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-slate-700 transition-colors hover:bg-slate-50"
                @click="handleOpenAccountSettings"
              >
                <WkIcon name="set" :size="24" class="text-slate-400 transition-colors group-hover:text-primary" />
                <span class="text-sm font-medium">账号设置</span>
              </button>

              <button
                v-for="item in configNavItems"
                :key="item.route"
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors"
                :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
                @click="navigateTo(item.route, item.query)"
              >
                <WkIcon
                  :name="item.icon"
                  :size="24"
                  class="transition-colors"
                  :class="isActive(item.route, item.query) ? 'text-primary' : 'text-slate-400 group-hover:text-primary'"
                />
                <span class="text-sm font-medium">{{ item.label }}</span>
              </button>

              <button
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-rose-600 transition-colors hover:bg-rose-50"
                @click="handleLogout"
              >
                <span class="material-symbols-outlined text-rose-400 transition-colors group-hover:text-rose-600">logout</span>
                <span class="text-sm font-medium">退出登录</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <div
      ref="mainContentColumnRef"
      class="flex flex-1 flex-col overflow-hidden"
      :class="{ 'pt-14': isMobile }"
    >
      <header
        v-if="showDesktopHeader || showChatDesktopQuotaBar"
        class="relative z-[100] flex h-16 shrink-0 items-center bg-white px-[10px] md:px-8"
        :class="showDesktopHeader ? 'justify-between border-b border-slate-200' : 'border-b-0'"
      >
        <template v-if="showDesktopHeader">
        <div class="flex flex-1 items-center gap-4 mr-1">
          <div ref="searchPanelRef" class="relative w-full max-w-md">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-xl text-slate-400">search</span>
            <input
              v-model="globalSearchKeyword"
              type="text"
              class="w-full rounded-lg border-none bg-slate-100 py-2 pl-10 pr-4 text-sm outline-none transition-all focus:ring-2 focus:ring-primary/50"
              placeholder="搜索客户、联系人、任务、日程、知识库..."
              @focus="handleGlobalSearchFocus"
              @keydown.enter.prevent="handleGlobalSearchEnter"
              @keydown.down.prevent="handleGlobalSearchArrow(1)"
              @keydown.up.prevent="handleGlobalSearchArrow(-1)"
              @keydown.esc.prevent="closeGlobalSearchDropdown"
              @input="debouncedGlobalSearch"
            />
            <Transition name="dropdown">
              <div
                v-if="showGlobalSearchDropdown"
                class="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-[210] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-900/10"
                @mousedown.prevent
              >
                <div v-if="globalSearchLoading" class="flex items-center gap-3 px-4 py-4 text-sm text-slate-500">
                  <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
                  正在搜索...
                </div>

                <template v-else-if="globalSearchResults.length > 0">
                  <button
                    v-for="(result, index) in globalSearchResults"
                    :key="`${result.entityType}-${result.entityId}`"
                    type="button"
                    class="flex w-full items-start gap-3 px-4 py-3 text-left transition-colors"
                    :class="index === activeSearchResultIndex ? 'bg-primary/5' : 'hover:bg-slate-50'"
                    @mouseenter="activeSearchResultIndex = index"
                    @click="navigateToSearchResult(result)"
                  >
                    <div class="mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                      <WkIcon :name="getSearchResultIcon(result.entityType)" :size="18" />
                    </div>
                    <div class="min-w-0 flex-1">
                      <p class="truncate text-sm font-semibold text-slate-900">{{ result.title }}</p>
                      <p v-if="result.subtitle" class="mt-0.5 truncate text-xs text-slate-500">{{ result.subtitle }}</p>
                      <p v-if="result.summary" class="line-clamp-2 mt-1 text-xs leading-5 text-slate-400">{{ result.summary }}</p>
                    </div>
                    <span class="material-symbols-outlined mt-1 shrink-0 text-base text-slate-300">chevron_right</span>
                  </button>
                  <div class="border-t border-slate-100 px-4 py-2 text-center text-xs text-slate-400">
                    显示前 {{ globalSearchResults.length }} 条，共 {{ globalSearchTotal }} 条结果
                  </div>
                </template>

                <div v-else class="px-4 py-5 text-center text-sm text-slate-400">
                  没有找到匹配结果
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <AiQuotaHeaderPopover />
          <button class="flex size-10 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <button
            @click="showCreateCustomer = true"
            class="hidden md:flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white shadow-sm shadow-primary/20 transition-colors hover:bg-primary/90"
          >
            <span class="material-symbols-outlined wk-plus-button-icon">add</span>
            新增客户
          </button>
        </div>
        </template>
        <template v-else>
          <div
            class="flex w-full min-w-0 items-center gap-3 justify-end"
          >
            <div class="flex shrink-0 items-center gap-3">
              <AiQuotaHeaderPopover />
            </div>
          </div>
        </template>
      </header>

      <main class="flex-1 overflow-y-auto wk-safe-bottom">
        <router-view v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>

    <CustomerUpsertDialog v-model="showCreateCustomer" mode="create" @success="handleCreateCustomerSuccess" />
    <AccountSettingsModal v-model="showAccountSettingsModal" />
    <FloatingActionButton v-if="route.path !== '/chat' && !chatDrawerOpen" />
    <AiChatDrawer />
    <AiQuotaModals />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import defaultLogoImg from '@/assets/images/logo.png'
import FloatingActionButton from '@/components/common/FloatingActionButton.vue'
import AiChatDrawer from '@/components/common/AiChatDrawer.vue'
import AiQuotaHeaderPopover from '@/components/layout/AiQuotaHeaderPopover.vue'
import AiQuotaModals from '@/components/layout/AiQuotaModals.vue'
import AccountSettingsModal from '@/views/profile/components/AccountSettingsModal.vue'
import ChatSessionActionsPopover from '@/components/layout/ChatSessionActionsPopover.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
import type { WkIconName } from '@/components/common/wkIcon'
import { queryGlobalSearch, type GlobalSearchResult } from '@/api/search'
import { useChatDrawer } from '@/composables/useChatDrawer'
import { useEnterpriseStore } from '@/stores/enterprise'
import { useResponsive } from '@/composables/useResponsive'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { appEvents, APP_EVENT } from '@/utils/events'
import type { ChatSession } from '@/types/common'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const enterpriseStore = useEnterpriseStore()
const chatStore = useChatStore()
const { isMobile } = useResponsive()
const { isOpen: chatDrawerOpen } = useChatDrawer()

/**
 * 主内容列（顶栏 + router-view 所在 flex 列）在侧栏展开时的宽度小于该值且仍为 PC 时，自动收起左侧一级导航；
 * 宽度变回足够时恢复进入窄区前的展开/收起状态（用户在窄区内手动切换侧栏会更新该状态）。
 *
 * 恢复时用「收起侧栏下的主列宽度 − 侧栏宽度差」估算展开后的主列宽度，与阈值比较，避免收起后主列变宽立刻触发恢复导致抖动。
 */
const PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX = 780
/** 与模板 `w-64` / `w-[52px]` 一致，用于主列宽度滞回 */
const PRIMARY_SIDEBAR_WIDTH_EXPANDED_PX = 256
const PRIMARY_SIDEBAR_WIDTH_COLLAPSED_PX = 52
const PRIMARY_SIDEBAR_WIDTH_DELTA_PX = PRIMARY_SIDEBAR_WIDTH_EXPANDED_PX - PRIMARY_SIDEBAR_WIDTH_COLLAPSED_PX

function mainContentWidthAsIfSidebarExpanded(sidebarCollapsed: boolean, mainColumnWidth: number) {
  return sidebarCollapsed ? mainColumnWidth - PRIMARY_SIDEBAR_WIDTH_DELTA_PX : mainColumnWidth
}

const mainContentColumnRef = ref<HTMLElement | null>(null)
const mainContentColumnWidth = ref(0)
const mainContentColumnMeasured = ref(false)

const layoutNarrowAutoCollapseActive = ref(false)
/** 解除窄区限制后是否保持侧栏展开（非 collapsed） */
const layoutNarrowSavedExpanded = ref(true)
const layoutNarrowProgrammatic = ref(false)

function updateMainContentColumnWidth() {
  const el = mainContentColumnRef.value
  if (!el) return
  mainContentColumnWidth.value = el.getBoundingClientRect().width
  mainContentColumnMeasured.value = true
}

function runLayoutNarrowProgrammatic(fn: () => void) {
  layoutNarrowProgrammatic.value = true
  try {
    fn()
  } finally {
    queueMicrotask(() => {
      layoutNarrowProgrammatic.value = false
    })
  }
}

const drawerVisible = ref(false)
/** PC：一级侧栏收起为图标栏 */
const primarySidebarCollapsed = ref(false)
/** 一级侧栏收起时：鼠标在整块 aside 内则顶栏 logo 区显示为折叠图标 */
const collapsedSidebarAsideHovered = ref(false)

function onCollapsedPrimarySidebarEnter() {
  if (primarySidebarCollapsed.value) collapsedSidebarAsideHovered.value = true
}

function onCollapsedPrimarySidebarLeave() {
  collapsedSidebarAsideHovered.value = false
}

const primaryNavRef = ref<HTMLElement | null>(null)
const primaryNavHasScrollbar = ref(false)
/** PC 侧栏「最近」对话列表折叠 */
const recentChatSessionsExpanded = ref(true)
const showUserMenu = ref(false)
const showAccountSettingsModal = ref(false)
const showCreateCustomer = ref(false)

const globalSearchKeyword = ref('')
const globalSearchLoading = ref(false)
const showGlobalSearchDropdown = ref(false)
const globalSearchResults = ref<GlobalSearchResult[]>([])
const globalSearchTotal = ref(0)
const activeSearchResultIndex = ref(-1)
const searchPanelRef = ref<HTMLElement | null>(null)

let globalSearchTimer: ReturnType<typeof setTimeout> | null = null
let globalSearchRequestId = 0

type MainNavItem = {
  key: string
  icon: WkIconName
  label: string
  route: string
  permission: string
  groupTitle?: string
  secondaryTitle?: string
  children?: SecondaryNavItem[]
}

type ConfigNavItem = {
  icon: WkIconName
  label: string
  route: string
  permission: string[]
  query?: Record<string, string>
}

type SecondaryNavItem = {
  key: string
  icon?: WkIconName
  label: string
  route: string
  query?: Record<string, string>
}

const allMainNavItems: MainNavItem[] = [
  { key: 'chat', icon: 'ai', label: 'AI 助手', route: '/chat', permission: 'chat', groupTitle: 'AI 助手' },
  { key: 'knowledge', icon: 'knowledge-1', label: '知识库', route: '/knowledge', permission: 'knowledge' },
  {
    key: 'crm',
    icon: 'crm',
    label: 'CRM管理',
    route: '/customer',
    permission: 'customer',
    groupTitle: '悟空技能',
    secondaryTitle: 'CRM管理',
    children: [
      { key: 'crm-customer', icon: 'customer', label: '客户管理', route: '/customer' },
      { key: 'crm-task', icon: 'task-1', label: '任务管理', route: '/task' },
      { key: 'crm-calendar', icon: 'event', label: '日程安排', route: '/calendar' },
      { key: 'crm-settings', icon: 'set', label: '系统设置', route: '/settings/role', query: { scope: 'crm' } },
    ],
  },
]

const allConfigNavItems: ConfigNavItem[] = [
  { icon: 'set', label: '系统设置', route: '/settings/team', permission: ['user', 'role', 'config', 'dept', 'customField'], query: { scope: 'profile' } },
]

const mainNavItems = computed(() =>
  allMainNavItems.filter(item => item.permission === 'chat' || userStore.hasPermission(item.permission))
)

const pcMainNavItems = computed(() => mainNavItems.value.filter(item => item.key !== 'chat'))

type MainNavGroup = {
  title?: string
  items: MainNavItem[]
}

function groupMainNavItems(items: MainNavItem[]): MainNavGroup[] {
  const groups: MainNavGroup[] = []
  for (const item of items) {
    const title = item.groupTitle?.trim() || ''
    const last = groups[groups.length - 1]
    if (!last || (last.title || '') !== title) {
      groups.push({ title: title || undefined, items: [item] })
    } else {
      last.items.push(item)
    }
  }
  return groups
}

const pcMainNavGroups = computed(() => groupMainNavItems(pcMainNavItems.value))
const mobileMainNavGroups = computed(() => groupMainNavItems(mainNavItems.value))

const configNavItems = computed(() =>
  allConfigNavItems.filter(item => item.permission.some(permission => userStore.hasPermission(permission)))
)

const showConfigSection = computed(() => configNavItems.value.length > 0)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/customer')) return '/customer'
  if (path.startsWith('/settings')) return '/settings'
  return path
})

const showDesktopHeader = computed(() => {
  if (isMobile.value) return false
  return !route.path.startsWith('/knowledge') && !route.path.startsWith('/chat')
})

/** 桌面端 Chat：顶栏样式与主站一致，仅展示 AI 额度入口 */
const showChatDesktopQuotaBar = computed(() => !isMobile.value && route.path.startsWith('/chat'))

const selectedPrimaryKey = ref<string>('')

const selectedPrimaryItem = computed(() => mainNavItems.value.find(item => item.key === selectedPrimaryKey.value) || null)

const activeSecondaryItems = computed(() => selectedPrimaryItem.value?.children || [])

const showSecondaryPanel = computed(() => activeSecondaryItems.value.length > 0)

watch(showSecondaryPanel, open => {
  if (!isMobile.value && open) {
    runLayoutNarrowProgrammatic(() => {
      primarySidebarCollapsed.value = true
    })
  }
})

const secondaryTitle = computed(() => {
  if (!selectedPrimaryItem.value) return '二级菜单'
  return selectedPrimaryItem.value.secondaryTitle || `${selectedPrimaryItem.value.label} / 二级菜单`
})

const expandedMobilePrimaryKeys = ref<string[]>([])

watch(
  () => drawerVisible.value,
  isOpen => {
    if (!isOpen) return
    // 默认直接展开所有带二级菜单的一级菜单
    expandedMobilePrimaryKeys.value = mainNavItems.value.filter(item => item.children?.length).map(item => item.key)
  }
)

function isMobilePrimaryExpanded(key: string) {
  return expandedMobilePrimaryKeys.value.includes(key)
}

function toggleMobilePrimaryExpanded(key: string) {
  if (isMobilePrimaryExpanded(key)) {
    expandedMobilePrimaryKeys.value = expandedMobilePrimaryKeys.value.filter(k => k !== key)
    return
  }
  expandedMobilePrimaryKeys.value = [...expandedMobilePrimaryKeys.value, key]
}

function handleMobilePrimaryNavClick(item: MainNavItem) {
  if (item.children?.length) {
    toggleMobilePrimaryExpanded(item.key)
    return
  }
  mobileNavigate(item.route)
}

watch(
  () => route.fullPath,
  () => {
    showUserMenu.value = false
    closeGlobalSearchDropdown()
    if (selectedPrimaryKey.value && !selectedPrimaryItem.value) {
      selectedPrimaryKey.value = ''
    }
    const scopeRaw = route.query.scope
    const scope = Array.isArray(scopeRaw) ? scopeRaw[0] : scopeRaw
    if (route.path === '/settings/team' && scope === 'profile') {
      selectedPrimaryKey.value = ''
    }
  }
)

watch(
  () => drawerVisible.value,
  isOpen => {
    if (!isOpen) {
      showUserMenu.value = false
    }
  }
)

watch(showUserMenu, open => {
  if (open && !isMobile.value && primarySidebarCollapsed.value) {
    primarySidebarCollapsed.value = false
  }
})

watch(
  () => [isMobile.value, mainContentColumnWidth.value, mainContentColumnMeasured.value, primarySidebarCollapsed.value] as const,
  ([mobile, w, measured, collapsed]) => {
    if (mobile) {
      if (layoutNarrowAutoCollapseActive.value) {
        runLayoutNarrowProgrammatic(() => {
          primarySidebarCollapsed.value = !layoutNarrowSavedExpanded.value
          layoutNarrowAutoCollapseActive.value = false
        })
      }
      return
    }
    if (!measured) return

    const mainIfExpanded = mainContentWidthAsIfSidebarExpanded(collapsed, w)

    if (!collapsed && mainIfExpanded < PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX) {
      runLayoutNarrowProgrammatic(() => {
        layoutNarrowSavedExpanded.value = true
        primarySidebarCollapsed.value = true
        layoutNarrowAutoCollapseActive.value = true
      })
      return
    }

    if (layoutNarrowAutoCollapseActive.value && collapsed && mainIfExpanded >= PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX) {
      runLayoutNarrowProgrammatic(() => {
        primarySidebarCollapsed.value = !layoutNarrowSavedExpanded.value
        layoutNarrowAutoCollapseActive.value = false
        if (showSecondaryPanel.value) {
          primarySidebarCollapsed.value = true
        }
      })
    }
  },
  { immediate: true }
)

watch(primarySidebarCollapsed, collapsed => {
  collapsedSidebarAsideHovered.value = false
  if (layoutNarrowProgrammatic.value) return
  if (isMobile.value || !layoutNarrowAutoCollapseActive.value) return
  if (!mainContentColumnMeasured.value) return
  const w = mainContentColumnWidth.value
  const mainIfExpanded = mainContentWidthAsIfSidebarExpanded(collapsed, w)
  if (mainIfExpanded >= PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX) return
  layoutNarrowSavedExpanded.value = !collapsed
})

let primaryNavResizeObserver: ResizeObserver | null = null
let mainContentColumnResizeObserver: ResizeObserver | null = null

function updatePrimaryNavScrollbar() {
  const el = primaryNavRef.value
  if (!el) {
    primaryNavHasScrollbar.value = false
    return
  }
  // +1 to avoid off-by-one from subpixel layout
  primaryNavHasScrollbar.value = el.scrollHeight > el.clientHeight + 1
}

onMounted(() => {
  enterpriseStore.loadConfig()
  void chatStore.fetchSessions()
  document.addEventListener('click', handleDocumentClick)

  if (typeof ResizeObserver !== 'undefined') {
    primaryNavResizeObserver = new ResizeObserver(() => updatePrimaryNavScrollbar())
    if (primaryNavRef.value) primaryNavResizeObserver.observe(primaryNavRef.value)
  }
  // Initial measure
  queueMicrotask(() => updatePrimaryNavScrollbar())
})

watch(
  mainContentColumnRef,
  el => {
    if (typeof ResizeObserver === 'undefined') return
    if (!mainContentColumnResizeObserver) {
      mainContentColumnResizeObserver = new ResizeObserver(() => updateMainContentColumnWidth())
    }
    mainContentColumnResizeObserver.disconnect()
    if (el) {
      mainContentColumnResizeObserver.observe(el)
      queueMicrotask(() => updateMainContentColumnWidth())
    } else {
      mainContentColumnWidth.value = 0
      mainContentColumnMeasured.value = false
    }
  },
  { flush: 'post', immediate: true }
)

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  if (mainContentColumnResizeObserver) {
    mainContentColumnResizeObserver.disconnect()
    mainContentColumnResizeObserver = null
  }
  if (primaryNavResizeObserver) {
    primaryNavResizeObserver.disconnect()
    primaryNavResizeObserver = null
  }
  if (globalSearchTimer) {
    clearTimeout(globalSearchTimer)
    globalSearchTimer = null
  }
})

watch(
  () => [
    isMobile.value,
    primarySidebarCollapsed.value,
    recentChatSessionsExpanded.value,
    pcMainNavGroups.value.length,
    chatStore.sessions.length,
    chatStore.sessionsLoading,
  ],
  () => {
    queueMicrotask(() => updatePrimaryNavScrollbar())
  },
  { flush: 'post' }
)

function isActive(routePath: string, itemQuery?: Record<string, string>) {
  const pathMatches = routePath.startsWith('/settings')
    ? route.path.startsWith('/settings')
    : activeMenu.value === routePath
  if (!pathMatches) return false

  const expectedScope = itemQuery?.scope
  const currentScope = typeof route.query.scope === 'string' ? route.query.scope : ''
  if (expectedScope !== undefined) {
    return currentScope === expectedScope
  }
  if (routePath.startsWith('/settings') && currentScope) {
    return false
  }
  return true
}

function isPrimarySelected(item: MainNavItem) {
  return selectedPrimaryKey.value === item.key
}

function isPrimaryActive(item: MainNavItem) {
  if (isPrimarySelected(item)) return true
  if (item.children?.some(child => isActive(child.route, child.query))) return true
  return isActive(item.route)
}

function handlePrimaryNavClick(item: MainNavItem) {
  if (item.children?.length) {
    selectedPrimaryKey.value = item.key
    const firstChild = item.children[0]
    if (firstChild?.route) {
      navigateTo(firstChild.route, firstChild.query)
    }
    return
  }
  selectedPrimaryKey.value = ''
  navigateTo(item.route)
}

const groupedChatSessions = computed(() => {
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

async function handleNewSession() {
  selectedPrimaryKey.value = ''
  await router.push('/chat')
  // HMR can keep an old store instance; fall back to legacy behavior if method missing.
  const api = chatStore as unknown as {
    startNewSessionIfNeeded?: (title?: string) => Promise<string>
    clearMessages?: () => void
    startNewSession?: (title?: string) => Promise<string>
  }
  if (api.startNewSessionIfNeeded) {
    await api.startNewSessionIfNeeded('新对话')
  } else {
    api.clearMessages?.()
    await api.startNewSession?.('新对话')
  }
  chatStore.requestComposerFocus()
}

async function handleSelectSession(sessionId: string) {
  selectedPrimaryKey.value = ''
  await router.push('/chat')
  await chatStore.selectSession(sessionId)
  chatStore.requestComposerFocus()
}

function isSessionActive(sessionId: string): boolean {
  return route.path.startsWith('/chat') && chatStore.currentSessionId === sessionId
}

async function handleDeleteSession(sessionId: string) {
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？删除后无法恢复。', '删除对话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
    await chatStore.removeSession(sessionId)
    ElMessage.success('对话已删除')
  } catch {
    // User cancelled
  }
}

async function handleShareChatSession(session: ChatSession) {
  const resolved = router.resolve({ name: 'Chat', query: { sessionId: session.sessionId } })
  const url = new URL(resolved.href, window.location.href).href
  const title = session.title || '新对话'
  if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
    try {
      await navigator.share({ title, text: title, url })
      return
    } catch (e) {
      if ((e as { name?: string })?.name === 'AbortError') return
    }
  }
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('对话链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制链接')
  }
}

function navigateTo(path: string, query?: Record<string, string>) {
  if (query) {
    void router.push({ path, query })
  } else {
    void router.push(path)
  }
  showUserMenu.value = false
}

function mobileNavigate(path: string, query?: Record<string, string>) {
  if (query) {
    void router.push({ path, query })
  } else {
    void router.push(path)
  }
  drawerVisible.value = false
  showUserMenu.value = false
}

function handleOpenAccountSettings() {
  showUserMenu.value = false
  showAccountSettingsModal.value = true
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node | null
  if (!target) return
  if (searchPanelRef.value?.contains(target)) return
  closeGlobalSearchDropdown()
}

function clearGlobalSearchResults() {
  globalSearchResults.value = []
  globalSearchTotal.value = 0
  activeSearchResultIndex.value = -1
}

function closeGlobalSearchDropdown() {
  showGlobalSearchDropdown.value = false
  activeSearchResultIndex.value = -1
}

async function runGlobalSearch(keyword: string) {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    clearGlobalSearchResults()
    closeGlobalSearchDropdown()
    return
  }

  const requestId = ++globalSearchRequestId
  globalSearchLoading.value = true
  showGlobalSearchDropdown.value = true

  try {
    const result = await queryGlobalSearch({
      keyword: normalizedKeyword,
      page: 1,
      limit: 8,
    })

    if (requestId !== globalSearchRequestId) return

    globalSearchResults.value = result.list || []
    globalSearchTotal.value = result.totalRow || 0
    activeSearchResultIndex.value = globalSearchResults.value.length > 0 ? 0 : -1
  } catch (error) {
    if (requestId !== globalSearchRequestId) return
    console.error('Global search failed:', error)
    clearGlobalSearchResults()
  } finally {
    if (requestId === globalSearchRequestId) {
      globalSearchLoading.value = false
    }
  }
}

function debouncedGlobalSearch() {
  if (globalSearchTimer) {
    clearTimeout(globalSearchTimer)
  }

  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) {
    clearGlobalSearchResults()
    closeGlobalSearchDropdown()
    return
  }

  globalSearchTimer = setTimeout(() => {
    void runGlobalSearch(keyword)
  }, 250)
}

function handleGlobalSearchFocus() {
  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) return

  if (globalSearchResults.value.length > 0 || globalSearchLoading.value) {
    showGlobalSearchDropdown.value = true
    return
  }

  void runGlobalSearch(keyword)
}

function handleGlobalSearchArrow(direction: 1 | -1) {
  if (!showGlobalSearchDropdown.value) {
    handleGlobalSearchFocus()
    return
  }
  if (!globalSearchResults.value.length) return

  const total = globalSearchResults.value.length
  const nextIndex = activeSearchResultIndex.value + direction
  if (nextIndex < 0) {
    activeSearchResultIndex.value = total - 1
    return
  }
  if (nextIndex >= total) {
    activeSearchResultIndex.value = 0
    return
  }
  activeSearchResultIndex.value = nextIndex
}

function handleGlobalSearchEnter() {
  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) {
    closeGlobalSearchDropdown()
    return
  }

  const target = globalSearchResults.value[activeSearchResultIndex.value] || globalSearchResults.value[0]
  if (target) {
    navigateToSearchResult(target)
    return
  }

  void runGlobalSearch(keyword)
}

function navigateToSearchResult(result: GlobalSearchResult) {
  globalSearchKeyword.value = result.title || globalSearchKeyword.value
  closeGlobalSearchDropdown()
  void router.push(result.routePath)
}

function getSearchResultIcon(entityType: GlobalSearchResult['entityType']): WkIconName {
  switch (entityType) {
    case 'customer':
      return 'customer'
    case 'contact':
      return 'profile'
    case 'task':
      return 'task'
    case 'schedule':
      return 'event'
    case 'knowledge':
    default:
      return 'knowledge'
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    showUserMenu.value = false
    drawerVisible.value = false
    await userStore.logout()
    void router.push('/login')
  } catch {
    // User cancelled
  }
}

function handleCreateCustomerSuccess(payload: { mode: 'create' | 'edit'; customerId?: string }) {
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
  if (payload.mode === 'create' && payload.customerId) {
    void router.push(`/customer/${payload.customerId}`)
  }
}
</script>

<style scoped>
.drawer-overlay-enter-active,
.drawer-overlay-leave-active {
  transition: opacity 0.3s ease;
}

.drawer-overlay-enter-from,
.drawer-overlay-leave-to {
  opacity: 0;
}

.drawer-panel-enter-active,
.drawer-panel-leave-active {
  transition: transform 0.3s ease;
}

.drawer-panel-enter-from,
.drawer-panel-leave-to {
  transform: translateX(-100%);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.profile-drawer-enter-active,
.profile-drawer-leave-active {
  transition: transform 0.28s ease, opacity 0.2s ease;
}

.profile-drawer-enter-from,
.profile-drawer-leave-to {
  opacity: 0;
  transform: translateY(100%);
}

.secondary-panel-enter-active,
.secondary-panel-leave-active {
  transition: transform 0.2s ease, opacity 0.15s ease;
}

.secondary-panel-enter-from,
.secondary-panel-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.line-clamp-2 {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
