<template>
  <el-container class="h-full">
    <!-- Mobile Top Bar -->
    <div v-if="isMobile" class="mobile-header">
      <el-icon :size="22" class="cursor-pointer" @click="drawerVisible = true"><Expand /></el-icon>
      <span class="text-lg font-bold text-primary-600">AI CRM</span>
      <el-dropdown trigger="click">
        <el-avatar :size="28" class="bg-primary-500 cursor-pointer">
          {{ userStore.realname?.charAt(0) || 'U' }}
        </el-avatar>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- Desktop Sidebar -->
    <el-aside v-if="!isMobile" width="220px" class="bg-white border-r border-gray-200">
      <div class="h-full flex flex-col">
        <!-- Logo -->
        <div class="h-16 flex items-center justify-center border-b border-gray-200">
          <span class="text-xl font-bold text-primary-600">AI CRM</span>
        </div>

        <!-- Navigation Menu -->
        <el-menu
          :default-active="activeMenu"
          class="flex-1 border-r-0"
          router
        >
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 助手</span>
          </el-menu-item>
          <el-menu-item index="/customer">
            <el-icon><User /></el-icon>
            <span>客户管理</span>
          </el-menu-item>
          <el-menu-item index="/task">
            <el-icon><List /></el-icon>
            <span>任务管理</span>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Document /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>

        <!-- User Info -->
        <div class="p-4 border-t border-gray-200">
          <el-dropdown trigger="click" class="w-full">
            <div class="flex items-center cursor-pointer">
              <el-avatar :size="32" class="bg-primary-500">
                {{ userStore.realname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="ml-2 text-sm text-gray-700 truncate flex-1">
                {{ userStore.realname || userStore.username }}
              </span>
              <el-icon class="ml-2"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-aside>

    <!-- Mobile Drawer Navigation -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      :size="260"
      :show-close="false"
      :with-header="false"
    >
      <div class="h-full flex flex-col">
        <!-- Logo -->
        <div class="h-16 flex items-center justify-center border-b border-gray-200">
          <span class="text-xl font-bold text-primary-600">AI CRM</span>
        </div>

        <!-- Navigation Menu -->
        <el-menu
          :default-active="activeMenu"
          class="flex-1 border-r-0"
          @select="handleMobileMenuSelect"
        >
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 助手</span>
          </el-menu-item>
          <el-menu-item index="/customer">
            <el-icon><User /></el-icon>
            <span>客户管理</span>
          </el-menu-item>
          <el-menu-item index="/task">
            <el-icon><List /></el-icon>
            <span>任务管理</span>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Document /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>

        <!-- User Info -->
        <div class="p-4 border-t border-gray-200">
          <div class="flex items-center">
            <el-avatar :size="32" class="bg-primary-500">
              {{ userStore.realname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="ml-2 text-sm text-gray-700 truncate flex-1">
              {{ userStore.realname || userStore.username }}
            </span>
          </div>
          <el-button class="w-full mt-3" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- Main Content -->
    <el-main class="p-0 bg-gray-50" :class="{ 'mobile-main': isMobile }">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  User,
  List,
  Document,
  Setting,
  ArrowDown,
  SwitchButton,
  Expand
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const drawerVisible = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  // Handle nested routes
  if (path.startsWith('/customer')) return '/customer'
  return path
})

function handleMobileMenuSelect(index: string) {
  router.push(index)
  drawerVisible.value = false
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    drawerVisible.value = false
    await userStore.logout()
    router.push('/login')
  } catch {
    // User cancelled
  }
}
</script>

<style scoped>
.el-menu-item {
  height: 50px;
}

.el-menu-item.is-active {
  background-color: #eff6ff;
}

.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
}

.mobile-main {
  padding-top: 48px !important;
}
</style>
