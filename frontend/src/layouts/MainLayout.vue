<template>
  <div class="flex h-screen bg-background-light">
    <!-- Desktop Sidebar -->
    <aside v-if="!isMobile" class="w-64 flex-shrink-0 bg-white border-r border-slate-200 flex flex-col h-screen">
      <!-- Logo -->
      <div class="p-6 flex items-center gap-3">
        <div class="size-10 bg-primary rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary/20">
          <span class="material-symbols-outlined text-2xl">rocket_launch</span>
        </div>
        <div>
          <h1 class="text-slate-900 font-bold text-lg leading-tight">Nexus AI</h1>
          <p class="text-slate-500 text-[10px] uppercase tracking-widest">AI CRM System</p>
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
          <span class="material-symbols-outlined" :class="{ fill: item.fill && isActive(item.route) }">{{ item.icon }}</span>
          <span class="text-sm font-medium">{{ item.label }}</span>
        </button>

        <div class="pt-4 pb-2">
          <p class="px-3 text-[10px] font-bold text-slate-400 uppercase tracking-wider">系统配置</p>
        </div>

        <button
          v-for="item in configNavItems"
          :key="item.route"
          @click="navigateTo(item.route)"
          class="w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
          :class="isActive(item.route)
            ? 'bg-primary/10 text-primary'
            : 'text-slate-600 hover:bg-slate-100'"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          <span class="text-sm font-medium">{{ item.label }}</span>
        </button>
      </nav>

      <!-- User Info -->
      <div class="p-4 border-t border-slate-200">
        <div class="flex items-center gap-3 p-2 bg-slate-50 rounded-xl cursor-pointer" @click="showUserMenu = !showUserMenu">
          <div class="size-8 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold">
            {{ userStore.realname?.charAt(0) || 'U' }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-xs font-semibold text-slate-900 truncate">{{ userStore.realname || userStore.username }}</p>
            <p class="text-[10px] text-slate-500 truncate">{{ userStore.userInfo?.deptName || '用户' }}</p>
          </div>
          <span class="material-symbols-outlined text-slate-400 text-sm">unfold_more</span>
        </div>
        <!-- User dropdown -->
        <Transition name="dropdown">
          <div v-if="showUserMenu" class="mt-2 bg-white border border-slate-200 rounded-lg shadow-lg py-1">
            <button @click="handleLogout" class="w-full flex items-center gap-2 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50">
              <span class="material-symbols-outlined text-sm">logout</span>
              退出登录
            </button>
          </div>
        </Transition>
      </div>
    </aside>

    <!-- Mobile Top Bar -->
    <div v-if="isMobile" class="fixed top-0 left-0 right-0 z-50 h-14 bg-white border-b border-slate-200 flex items-center justify-between px-4">
      <button @click="drawerVisible = true" class="size-10 flex items-center justify-center text-slate-600 hover:bg-slate-100 rounded-lg">
        <span class="material-symbols-outlined">menu</span>
      </button>
      <div class="flex items-center gap-2">
        <div class="size-7 bg-primary rounded-lg flex items-center justify-center text-white">
          <span class="material-symbols-outlined text-sm">rocket_launch</span>
        </div>
        <span class="text-sm font-bold text-slate-900">Nexus AI</span>
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
            <div class="size-10 bg-primary rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary/20">
              <span class="material-symbols-outlined text-2xl">rocket_launch</span>
            </div>
            <div>
              <h1 class="text-slate-900 font-bold text-lg leading-tight">Nexus AI</h1>
              <p class="text-slate-500 text-[10px] uppercase tracking-widest">AI CRM System</p>
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
              <span class="material-symbols-outlined">{{ item.icon }}</span>
              <span class="text-sm font-medium">{{ item.label }}</span>
            </button>
            <div class="pt-4 pb-2">
              <p class="px-3 text-[10px] font-bold text-slate-400 uppercase tracking-wider">系统配置</p>
            </div>
            <button
              v-for="item in configNavItems"
              :key="item.route"
              @click="mobileNavigate(item.route)"
              class="w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
              :class="isActive(item.route) ? 'bg-primary/10 text-primary' : 'text-slate-600 hover:bg-slate-100'"
            >
              <span class="material-symbols-outlined">{{ item.icon }}</span>
              <span class="text-sm font-medium">{{ item.label }}</span>
            </button>
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
              type="text"
              class="w-full bg-slate-100 border-none rounded-lg py-2 pl-10 pr-4 text-sm focus:ring-2 focus:ring-primary/50 transition-all outline-none"
              placeholder="搜索公司、联系人或 AI 标签..."
            />
          </div>
        </div>
        <div class="flex items-center gap-3">
          <button class="size-10 flex items-center justify-center text-slate-500 hover:bg-slate-100 rounded-full transition-colors">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <button
            @click="$router.push('/customer')"
            class="bg-primary text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:bg-primary/90 transition-colors shadow-sm shadow-primary/20"
          >
            <span class="material-symbols-outlined text-sm">add</span>
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

    <!-- Floating Action Button (shown on non-chat pages, hidden when drawer is open) -->
    <FloatingActionButton v-if="route.path !== '/chat' && !chatDrawerOpen" />

    <!-- AI Chat Drawer -->
    <AiChatDrawer />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { useChatDrawer } from '@/composables/useChatDrawer'
import { ElMessageBox } from 'element-plus'
import FloatingActionButton from '@/components/common/FloatingActionButton.vue'
import AiChatDrawer from '@/components/common/AiChatDrawer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const drawerVisible = ref(false)
const showUserMenu = ref(false)
const { openChatDrawer, isOpen: chatDrawerOpen } = useChatDrawer()

const mainNavItems = [
  { icon: 'auto_awesome', label: 'AI 助手', route: '/chat', fill: false },
  { icon: 'group', label: '客户管理', route: '/customer', fill: true },
  { icon: 'task_alt', label: '任务管理', route: '/task', fill: false },
  { icon: 'calendar_today', label: '日程安排', route: '/calendar', fill: false },
  { icon: 'menu_book', label: '知识库', route: '/knowledge', fill: false },
]

const configNavItems = [
  { icon: 'settings', label: '系统设置', route: '/settings' },
]

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/customer')) return '/customer'
  return path
})

function isActive(routePath: string) {
  return activeMenu.value === routePath
}

function navigateTo(path: string) {
  if (path === '/chat') {
    openChatDrawer()
  } else {
    router.push(path)
  }
  showUserMenu.value = false
}

function mobileNavigate(path: string) {
  if (path === '/chat') {
    openChatDrawer()
  } else {
    router.push(path)
  }
  drawerVisible.value = false
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
</style>
