<template>
  <div class="flex h-screen bg-background-light">
    <!-- Desktop Sidebar -->
    <aside v-if="!isMobile" class="relative w-64 flex-shrink-0 bg-white border-r border-slate-200 flex flex-col h-screen">
      <!-- Logo -->
      <div class="p-6 flex items-center gap-3 ">
        <div v-if="enterpriseStore.hasLogo" class="size-10 flex-shrink-0 rounded-xl overflow-hidden bg-transparent border border-slate-200">
          <img :src="enterpriseStore.logoUrl!" class="w-full h-full object-cover" alt="logo" />
        </div>
        <div v-else class="size-10 flex-shrink-0 rounded-xl overflow-hidden bg-transparent border border-slate-200">
          <img :src="defaultLogoImg" class="w-full h-full object-cover" alt="logo" />
        </div>
        <div class="min-w-0">
          <h1 class="text-slate-900 font-bold text-lg leading-tight line-clamp-2">{{ enterpriseStore.displayName }}</h1>
          <p class="text-slate-500 text-xs uppercase tracking-widest">{{ enterpriseStore.displayDescription }}</p>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
        <button
          v-for="item in mainNavItems"
          :key="item.route"
          @click="navigateTo(item.route)"
          class="w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
          :class="isActive(item.route)
            ? 'bg-primary/10 text-primary'
            : 'text-slate-600 hover:bg-slate-100'"
        >
          <WkIcon :name="item.icon" :size="22" class="shrink-0" />
          <span class="text-sm font-medium">{{ item.label }}</span>
        </button>

        <div v-if="showConfigSection" class="pt-4 pb-2">
          <p class="px-3 text-xs font-bold text-slate-400 uppercase tracking-wider">系统配置</p>
        </div>

        <template v-if="showConfigSection">
        <button
          v-for="item in configNavItems"
          :key="item.route"
          @click="navigateTo(item.route)"
          class="w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
          :class="isActive(item.route)
            ? 'bg-primary/10 text-primary'
            : 'text-slate-600 hover:bg-slate-100'"
        >
          <WkIcon :name="item.icon" :size="22" class="shrink-0" />
          <span class="text-sm font-medium">{{ item.label }}</span>
        </button>
        </template>
      </nav>

      <!-- User Info -->
      <div class="p-4 border-t border-slate-200">
        <div class="flex items-center gap-3 p-2 bg-slate-50 rounded-xl cursor-pointer" @click="showUserMenu = !showUserMenu">
          <div v-if="userStore.avatar" class="size-8 rounded-full overflow-hidden">
            <img :src="userStore.avatar" class="w-full h-full object-cover" alt="avatar" />
          </div>
          <div v-else class="size-8 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold">
            {{ userStore.realname?.charAt(0) || 'U' }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-xs font-semibold text-slate-900 truncate">{{ userStore.realname || userStore.username }}</p>
            <p class="text-xs text-slate-500 truncate">{{ userStore.userInfo?.deptName || '用户' }}</p>
          </div>
          <span class="material-symbols-outlined text-slate-400 text-sm">unfold_more</span>
        </div>
      </div>

      <Transition name="profile-drawer">
        <div
          v-if="showUserMenu"
          class="absolute bottom-0 left-0 right-0 z-20 bg-white border-t border-slate-200 shadow-[0_-8px_30px_rgb(0,0,0,0.12)] rounded-t-2xl overflow-hidden"
        >
          <div class="p-4">
            <div class="space-y-1">
              <div class="px-3 py-2 flex items-center justify-between">
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">个人账户</p>
                <button class="text-slate-300 hover:text-slate-500 transition-colors" @click="showUserMenu = false">
                  <span class="material-symbols-outlined text-sm">close</span>
                </button>
              </div>

              <div class="px-3 py-2 flex items-center gap-3 mb-4">
                <div v-if="userStore.avatar" class="size-10 rounded-full overflow-hidden bg-slate-300">
                  <img :src="userStore.avatar" class="w-full h-full object-cover" alt="avatar" />
                </div>
                <div v-else class="size-10 rounded-full bg-primary flex items-center justify-center text-white text-sm font-bold">
                  {{ userStore.realname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
                </div>
                <div class="min-w-0">
                  <p class="text-sm font-bold text-slate-900 truncate">{{ userStore.realname || userStore.username }}</p>
                  <p class="text-xs text-slate-500 truncate">{{ userStore.userInfo?.deptName || '用户' }}</p>
                </div>
              </div>

              <div class="h-px bg-slate-100 mx-3 my-1" />

              <button
                class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-700 hover:bg-slate-50 transition-colors text-left group"
                @click="handleOpenAccountSettings"
              >
                <WkIcon name="settings" :size="24" class="text-slate-400 group-hover:text-primary transition-colors" />
                <span class="text-sm font-medium">账号设置</span>
              </button>

              <button
                class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-rose-600 hover:bg-rose-50 transition-colors text-left group"
                @click="handleLogout"
              >
                <span class="material-symbols-outlined text-rose-400 group-hover:text-rose-600 transition-colors">logout</span>
                <span class="text-sm font-medium">退出登录</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </aside>

    <!-- Mobile Top Bar -->
    <div v-if="isMobile" class="fixed top-0 left-0 right-0 z-50 h-14 bg-white border-b border-slate-200 flex items-center justify-between px-4">
      <button @click="drawerVisible = true" class="size-10 flex items-center justify-center text-slate-600 hover:bg-slate-100 rounded-lg">
        <span class="material-symbols-outlined">menu</span>
      </button>
      <div class="flex items-center gap-2 min-w-0 flex-1 mx-2">
        <div v-if="enterpriseStore.hasLogo" class="size-7 flex-shrink-0 rounded-lg overflow-hidden bg-transparent border border-slate-200">
          <img :src="enterpriseStore.logoUrl!" class="w-full h-full object-cover" alt="logo" />
        </div>
        <div v-else class="size-7 flex-shrink-0 rounded-lg overflow-hidden bg-transparent border border-slate-200">
          <img :src="defaultLogoImg" class="w-full h-full object-cover" alt="logo" />
        </div>
        <span class="text-sm font-bold text-slate-900 truncate">{{ enterpriseStore.displayName }}</span>
      </div>
      <button @click="handleLogout" class="size-10 flex items-center justify-center text-slate-600 hover:bg-slate-100 rounded-lg">
        <span class="material-symbols-outlined text-sm">logout</span>
      </button>
    </div>

    <!-- Mobile Drawer Navigation -->
    <Teleport to="body">
      <Transition name="drawer-overlay">
        <div v-if="isMobile && drawerVisible" class="fixed inset-0 z-[100] bg-slate-900/40 backdrop-blur-sm" @click="drawerVisible = false"></div>
      </Transition>
      <Transition name="drawer-panel">
        <aside v-if="isMobile && drawerVisible" class="fixed left-0 top-0 bottom-0 w-72 bg-white flex flex-col shadow-2xl z-[101]">
          <div class="p-6 flex items-center gap-3">
            <div v-if="enterpriseStore.hasLogo" class="size-10 flex-shrink-0 rounded-xl overflow-hidden bg-transparent border border-slate-200">
              <img :src="enterpriseStore.logoUrl!" class="w-full h-full object-cover" alt="logo" />
            </div>
            <div v-else class="size-10 flex-shrink-0 rounded-xl overflow-hidden bg-transparent border border-slate-200">
              <img :src="defaultLogoImg" class="w-full h-full object-cover" alt="logo" />
            </div>
            <div class="min-w-0">
              <h1 class="text-slate-900 font-bold text-lg leading-tight line-clamp-2">{{ enterpriseStore.displayName }}</h1>
              <p class="text-slate-500 text-xs uppercase tracking-widest">{{ enterpriseStore.displayDescription }}</p>
            </div>
          </div>
          <nav class="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
            <button
              v-for="item in mainNavItems"
              :key="item.route"
              @click="mobileNavigate(item.route)"
              class="w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
              :class="isActive(item.route) ? 'bg-primary/10 text-primary' : 'text-slate-600 hover:bg-slate-100'"
            >
              <WkIcon :name="item.icon" :size="22" class="shrink-0" />
              <span class="text-sm font-medium">{{ item.label }}</span>
            </button>
            <div v-if="showConfigSection" class="pt-4 pb-2">
              <p class="px-3 text-xs font-bold text-slate-400 uppercase tracking-wider">系统配置</p>
            </div>
            <template v-if="showConfigSection">
            <button
              v-for="item in configNavItems"
              :key="item.route"
              @click="mobileNavigate(item.route)"
              class="w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
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

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col overflow-hidden" :class="{ 'pt-14': isMobile }">
      <!-- Header -->
      <header class="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-8 z-10 shrink-0">
        <div class="flex items-center gap-4 flex-1">
          <div class="relative w-full max-w-md">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-xl">search</span>
            <input
              v-model="globalSearchKeyword"
              type="text"
              class="w-full bg-slate-100 border-none rounded-lg py-2 pl-10 pr-4 text-sm focus:ring-2 focus:ring-primary/50 transition-all outline-none"
              placeholder="搜索客户、联系人、任务、日程、知识库..."
              @keydown.enter="handleGlobalSearch"
              @input="debouncedGlobalSearch"
            />
          </div>
        </div>
        <div class="flex items-center gap-3">
          <button class="size-10 flex items-center justify-center text-slate-500 hover:bg-slate-100 rounded-full transition-colors">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <button
            @click="showCreateCustomer = true"
            class="bg-primary text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:bg-primary/90 transition-colors shadow-sm shadow-primary/20"
          >
            <span class="material-symbols-outlined wk-plus-button-icon">add</span>
            新增客户
          </button>
        </div>
      </header>

      <!-- Page Content -->
      <main class="flex-1 overflow-y-auto">
        <router-view v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>

    <CustomerUpsertDialog
      v-model="showCreateCustomer"
      mode="create"
      @success="handleCreateCustomerSuccess"
    />

    <AccountSettingsModal v-model="showAccountSettingsModal" />

    <!-- Floating Action Button (shown on non-chat pages, hidden when drawer is open) -->
    <FloatingActionButton v-if="route.path !== '/chat' && !chatDrawerOpen" />

    <!-- AI Chat Drawer -->
    <AiChatDrawer />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useEnterpriseStore } from '@/stores/enterprise'
import defaultLogoImg from '@/assets/images/logo.png'
import { useResponsive } from '@/composables/useResponsive'
import { useChatDrawer } from '@/composables/useChatDrawer'
import { ElMessageBox } from 'element-plus'
import FloatingActionButton from '@/components/common/FloatingActionButton.vue'
import AiChatDrawer from '@/components/common/AiChatDrawer.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
import AccountSettingsModal from '@/views/profile/components/AccountSettingsModal.vue'
import type { WkIconName } from '@/components/common/wkIcon'
import { appEvents, APP_EVENT } from '@/utils/events'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const enterpriseStore = useEnterpriseStore()
const { isMobile } = useResponsive()
const globalSearchKeyword = ref('')
const showCreateCustomer = ref(false)

watch(() => route.fullPath, () => {
  showUserMenu.value = false
})

watch(
  () => [route.path, route.query.keyword],
  () => {
    globalSearchKeyword.value = route.path === '/search' ? firstQueryValue(route.query.keyword) : ''
  },
  { immediate: true }
)

onMounted(() => enterpriseStore.loadConfig())

const drawerVisible = ref(false)
const showUserMenu = ref(false)
const showAccountSettingsModal = ref(false)
const { isOpen: chatDrawerOpen } = useChatDrawer()

type MainNavItem = {
  icon: WkIconName
  label: string
  route: string
  permission?: string
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
  { icon: 'profile', label: '通讯录', route: '/addressBook' },
  { icon: 'task', label: '任务管理', route: '/task', permission: 'task' },
  { icon: 'meetingRecord', label: '日程安排', route: '/calendar', permission: 'schedule' },
  { icon: 'knowledge', label: '知识库', route: '/knowledge', permission: 'knowledge' },
  { icon: 'profile', label: '关系', route: '/relation' },
  { icon: 'contract', label: '产品管理', route: '/product' },
  { icon: 'task', label: '项目管理', route: '/project' },
]

const allConfigNavItems: ConfigNavItem[] = [
  { icon: 'settings', label: '系统设置', route: '/settings', permission: ['user', 'role', 'config', 'dept', 'customField'] },
]

// AI 助手（chat）默认所有人可见
const mainNavItems = computed(() =>
  allMainNavItems.filter(item => !item.permission || item.permission === 'chat' || userStore.hasPermission(item.permission))
)

const configNavItems = computed(() =>
  allConfigNavItems.filter(item => item.permission.some(p => userStore.hasPermission(p)))
)

const showConfigSection = computed(() => configNavItems.value.length > 0)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/customer')) return '/customer'
  if (path.startsWith('/addressBook')) return '/addressBook'
  if (path.startsWith('/relation')) return '/relation'
  if (path.startsWith('/product')) return '/product'
  if (path.startsWith('/project')) return '/project'
  if (path.startsWith('/settings')) return '/settings'
  return path
})

function isActive(routePath: string) {
  return activeMenu.value === routePath
}

function navigateTo(path: string) {
  router.push(path)
  showUserMenu.value = false
}

function mobileNavigate(path: string) {
  router.push(path)
  drawerVisible.value = false
}

function handleOpenAccountSettings() {
  showUserMenu.value = false
  showAccountSettingsModal.value = true
}

function handleGlobalSearch() {
  const keyword = globalSearchKeyword.value.trim()
  const target = keyword ? { path: '/search', query: { keyword } } : { path: '/search' }
  router.push(target)
}

let globalSearchTimer: ReturnType<typeof setTimeout> | null = null
function debouncedGlobalSearch() {
  if (globalSearchTimer) clearTimeout(globalSearchTimer)
  globalSearchTimer = setTimeout(() => {
    if (route.path === '/search') {
      const keyword = globalSearchKeyword.value.trim()
      const target = keyword ? { path: '/search', query: { keyword } } : { path: '/search' }
      router.replace(target)
    }
  }, 500)
}

function firstQueryValue(value: unknown): string {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : ''
  return value == null ? '' : String(value)
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    showUserMenu.value = false
    drawerVisible.value = false
    await userStore.logout()
    router.push('/login')
  } catch {
    // User cancelled
  }
}

function handleCreateCustomerSuccess() {
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
}
</script>

<style scoped>
/* Drawer animations */
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

/* Dropdown animation */
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
</style>
