<template>
  <div class="flex h-screen bg-background-light">
    <aside v-if="!isMobile" class="relative flex h-screen w-64 flex-shrink-0 flex-col border-r border-slate-200 bg-white">
      <div class="flex items-center gap-3 p-6">
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

      <nav class="flex-1 space-y-1 overflow-y-auto px-4 py-4">
        <button
          v-for="item in mainNavItems"
          :key="item.route"
          @click="navigateTo(item.route)"
          class="flex w-full items-center gap-3 rounded-lg px-3 py-2 transition-colors"
          :class="isActive(item.route) ? 'bg-primary/10 text-primary' : 'text-slate-600 hover:bg-slate-100'"
        >
          <WkIcon :name="item.icon" :size="22" class="shrink-0" />
          <span class="text-sm font-medium">{{ item.label }}</span>
        </button>

        <div v-if="showConfigSection" class="pb-2 pt-4">
          <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">配置与服务</p>
        </div>

        <template v-if="showConfigSection">
          <button
            v-for="item in configNavItems"
            :key="item.route"
            @click="navigateTo(item.route)"
            class="flex w-full items-center gap-3 rounded-lg px-3 py-2 transition-colors"
            :class="isActive(item.route) ? 'bg-primary/10 text-primary' : 'text-slate-600 hover:bg-slate-100'"
          >
            <WkIcon :name="item.icon" :size="22" class="shrink-0" />
            <span class="text-sm font-medium">{{ item.label }}</span>
          </button>
        </template>
      </nav>

      <div class="border-t border-slate-200 p-4">
        <div class="flex cursor-pointer items-center gap-3 rounded-xl bg-slate-50 p-2" @click="showUserMenu = !showUserMenu">
          <div v-if="userStore.avatar" class="size-8 overflow-hidden rounded-full">
            <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
          </div>
          <div v-else class="flex size-8 items-center justify-center rounded-full bg-primary text-xs font-bold text-white">
            {{ userStore.realname?.charAt(0) || 'U' }}
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-xs font-semibold text-slate-900">{{ userStore.realname || userStore.username }}</p>
            <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
          </div>
          <span class="material-symbols-outlined text-sm text-slate-400">unfold_more</span>
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
                <WkIcon name="settings" :size="24" class="text-slate-400 transition-colors group-hover:text-primary" />
                <span class="text-sm font-medium">账号设置</span>
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

    <div v-if="isMobile" class="fixed left-0 right-0 top-0 z-50 flex h-14 items-center justify-between border-b border-slate-200 bg-white px-4">
      <button @click="drawerVisible = true" class="flex size-10 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100">
        <span class="material-symbols-outlined">menu</span>
      </button>
      <div class="mx-2 flex min-w-0 flex-1 items-center gap-2">
        <div v-if="enterpriseStore.hasLogo" class="size-7 flex-shrink-0 overflow-hidden rounded-lg border border-slate-200 bg-transparent">
          <img :src="enterpriseStore.logoUrl!" class="h-full w-full object-cover" alt="logo" />
        </div>
        <div v-else class="size-7 flex-shrink-0 overflow-hidden rounded-lg border border-slate-200 bg-transparent">
          <img :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
        </div>
        <span class="truncate text-sm font-bold text-slate-900">{{ enterpriseStore.displayName }}</span>
      </div>
      <button @click="handleLogout" class="flex size-10 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100">
        <span class="material-symbols-outlined text-sm">logout</span>
      </button>
    </div>

    <Teleport to="body">
      <Transition name="drawer-overlay">
        <div v-if="isMobile && drawerVisible" class="fixed inset-0 z-[100] bg-slate-900/40 backdrop-blur-sm" @click="drawerVisible = false"></div>
      </Transition>
      <Transition name="drawer-panel">
        <aside v-if="isMobile && drawerVisible" class="fixed bottom-0 left-0 top-0 z-[101] flex w-72 flex-col bg-white shadow-2xl">
          <div class="flex items-center gap-3 p-6">
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
          <nav class="flex-1 space-y-1 overflow-y-auto px-4 py-4">
            <button
              v-for="item in mainNavItems"
              :key="item.route"
              @click="mobileNavigate(item.route)"
              class="flex w-full items-center gap-3 rounded-lg px-3 py-2 transition-colors"
              :class="isActive(item.route) ? 'bg-primary/10 text-primary' : 'text-slate-600 hover:bg-slate-100'"
            >
              <WkIcon :name="item.icon" :size="22" class="shrink-0" />
              <span class="text-sm font-medium">{{ item.label }}</span>
            </button>

            <div v-if="showConfigSection" class="pb-2 pt-4">
              <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">配置与服务</p>
            </div>

            <template v-if="showConfigSection">
              <button
                v-for="item in configNavItems"
                :key="item.route"
                @click="mobileNavigate(item.route)"
                class="flex w-full items-center gap-3 rounded-lg px-3 py-2 transition-colors"
                :class="isActive(item.route) ? 'bg-primary/10 text-primary' : 'text-slate-600 hover:bg-slate-100'"
              >
                <WkIcon :name="item.icon" :size="22" class="shrink-0" />
                <span class="text-sm font-medium">{{ item.label }}</span>
              </button>
            </template>
          </nav>
        </aside>
      </Transition>
    </Teleport>

    <div class="flex flex-1 flex-col overflow-hidden" :class="{ 'pt-14': isMobile }">
      <header class="relative z-[200] flex h-16 shrink-0 items-center justify-between border-b border-slate-200 bg-white px-8">
        <div class="flex flex-1 items-center gap-4">
          <div ref="searchPanelRef" class="relative w-full max-w-md">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-xl text-slate-400">search</span>
            <input
              ref="globalSearchInputRef"
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
          <button class="flex size-10 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <button
            @click="showCreateCustomer = true"
            class="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white shadow-sm shadow-primary/20 transition-colors hover:bg-primary/90"
          >
            <span class="material-symbols-outlined wk-plus-button-icon">add</span>
            新增客户
          </button>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto">
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
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import defaultLogoImg from '@/assets/images/logo.png'
import FloatingActionButton from '@/components/common/FloatingActionButton.vue'
import AiChatDrawer from '@/components/common/AiChatDrawer.vue'
import AccountSettingsModal from '@/views/profile/components/AccountSettingsModal.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
import type { WkIconName } from '@/components/common/wkIcon'
import { queryGlobalSearch, type GlobalSearchResult } from '@/api/search'
import { useChatDrawer } from '@/composables/useChatDrawer'
import { useEnterpriseStore } from '@/stores/enterprise'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'
import { appEvents, APP_EVENT } from '@/utils/events'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const enterpriseStore = useEnterpriseStore()
const { isMobile } = useResponsive()
const { isOpen: chatDrawerOpen } = useChatDrawer()

const drawerVisible = ref(false)
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
const globalSearchInputRef = ref<HTMLInputElement | null>(null)

let globalSearchTimer: ReturnType<typeof setTimeout> | null = null
let globalSearchRequestId = 0

type MainNavItem = {
  icon: WkIconName
  label: string
  route: string
  permission: string
}

type ConfigNavItem = {
  icon: WkIconName
  label: string
  route: string
  permission: string[]
}

const allMainNavItems: MainNavItem[] = [
  { icon: 'ai', label: 'AI 助手', route: '/chat', permission: 'chat' },
  { icon: 'customer', label: '客户管理', route: '/customer', permission: 'customer' },
  { icon: 'task', label: '任务管理', route: '/task', permission: 'task' },
  { icon: 'meetingRecord', label: '日程安排', route: '/calendar', permission: 'schedule' },
  { icon: 'knowledge', label: '知识库', route: '/knowledge', permission: 'knowledge' },
]

const allConfigNavItems: ConfigNavItem[] = [
  { icon: 'settings', label: '系统设置', route: '/settings', permission: ['user', 'role', 'config', 'dept', 'customField'] },
]

const mainNavItems = computed(() =>
  allMainNavItems.filter(item => item.permission === 'chat' || userStore.hasPermission(item.permission))
)

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

watch(
  () => route.fullPath,
  () => {
    showUserMenu.value = false
    closeGlobalSearchDropdown()
  }
)

onMounted(() => {
  enterpriseStore.loadConfig()
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  if (globalSearchTimer) {
    clearTimeout(globalSearchTimer)
    globalSearchTimer = null
  }
})

function isActive(routePath: string) {
  return activeMenu.value === routePath
}

function navigateTo(path: string) {
  void router.push(path)
  showUserMenu.value = false
}

function mobileNavigate(path: string) {
  void router.push(path)
  drawerVisible.value = false
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
      return 'meetingRecord'
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

function handleCreateCustomerSuccess() {
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
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

.line-clamp-2 {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
