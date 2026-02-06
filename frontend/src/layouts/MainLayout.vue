<template>
  <el-container class="h-full">
    <!-- Sidebar -->
    <el-aside width="220px" class="bg-white border-r border-gray-200">
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

    <!-- Main Content -->
    <el-main class="p-0 bg-gray-50">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  User,
  List,
  Document,
  Setting,
  ArrowDown,
  SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const path = route.path
  // Handle nested routes
  if (path.startsWith('/customer')) return '/customer'
  return path
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
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
</style>
