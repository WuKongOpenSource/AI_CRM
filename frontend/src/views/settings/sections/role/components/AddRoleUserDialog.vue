<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加用户到角色"
    :width="isMobile ? '95%' : '460px'"
    :fullscreen="isMobile"
    append-to-body
    class="wk-dialog--flush"
  >
    <el-input v-model="searchValue" placeholder="搜索用户..." clearable class="mb-3" />
    <div class="max-h-64 overflow-auto space-y-1">
      <div
        v-for="user in availableUsersForRole"
        :key="user.userId"
        class="flex items-center justify-between p-2 hover:bg-slate-50 rounded cursor-pointer"
        @click="$emit('toggle-user', user.userId)"
      >
        <div class="flex items-center gap-2">
          <el-checkbox
            :model-value="selectedUserIds.includes(String(user.userId))"
            @click.stop
            @change="$emit('toggle-user', user.userId)"
          />
          <div class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-medium" :class="getAvatarColor(user.realname || user.username)">
            {{ (user.realname || user.username || '?').charAt(0) }}
          </div>
          <span class="text-sm">{{ user.realname || user.username }}</span>
        </div>
        <span class="text-xs text-slate-400">{{ user.email || '' }}</span>
      </div>
      <div v-if="availableUsersForRole.length === 0" class="text-center py-4 text-slate-400 text-sm">
        没有可添加的用户
      </div>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="addingRoleUsers" :disabled="selectedUserIds.length === 0" @click="$emit('confirm')">
        确认添加 ({{ selectedUserIds.length }})
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  allUserSearch: string
  availableUsersForRole: any[]
  selectedUserIds: string[]
  addingRoleUsers: boolean
  getAvatarColor: (name: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:allUserSearch', value: string): void
  (e: 'toggle-user', userId: string | number): void
  (e: 'confirm'): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

const searchValue = computed({
  get: () => props.allUserSearch,
  set: (value: string) => emit('update:allUserSearch', value)
})
</script>
