<template>
  <el-drawer
    v-model="drawerVisible"
    direction="rtl"
    :size="isMobile ? '100%' : '400px'"
    :with-header="false"
    :modal="isMobile"
    :lock-scroll="isMobile"
    :modal-penetrable="!isMobile"
    class="employee-detail-drawer"
  >
    <div v-if="member" class="h-full flex flex-col bg-white shadow-2xl">
      <div class="flex items-center justify-between border-b border-slate-100 px-4 py-3">
        <h3 class="text-base font-bold text-slate-900">员工详情</h3>
        <button
          class="size-9 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
          aria-label="关闭员工详情"
          type="button"
          @click="drawerVisible = false"
        >
          <span class="material-symbols-outlined text-xl leading-none">close</span>
        </button>
      </div>

      <div class="flex-1 overflow-y-auto">
        <div class="p-8 text-center space-y-4">
          <div class="relative inline-block">
            <div
              class="size-24 rounded-2xl mx-auto flex items-center justify-center text-white text-3xl font-bold shadow-xl ring-4 ring-white"
              :class="getAvatarColor(member.realname)"
            >
              {{ (member.realname || member.username || '?').charAt(0) }}
            </div>
            <div
              class="absolute -bottom-2 -right-2 size-6 rounded-full border-2 border-white"
              :class="member.status === 1 ? 'bg-emerald-500' : 'bg-slate-400'"
            />
          </div>
          <div>
            <h2 class="text-xl font-bold text-slate-900">
              {{ member.realname || member.username }}
            </h2>
            <p class="text-primary font-bold text-sm uppercase tracking-widest mt-1">
              {{ member.post || '员工' }}
            </p>
          </div>
        </div>

        <div class="px-8 space-y-6">
          <div class="grid grid-cols-2 gap-4">
            <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100">
              <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">所属部门</p>
              <p class="text-sm font-bold text-slate-700">{{ member.deptName || '-' }}</p>
            </div>
            <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100">
              <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">直属上级</p>
              <p class="text-sm font-bold text-slate-700">{{ member.parentName || '无' }}</p>
            </div>
            <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100 col-span-2">
              <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">登录账号</p>
              <p class="text-sm font-bold text-slate-700">{{ member.username }}</p>
            </div>
          </div>

          <div class="space-y-3">
            <h4 class="text-xs font-bold text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-2">系统角色</h4>
            <div class="flex flex-wrap gap-2">
              <div
                v-for="roleName in member.roleNames || []"
                :key="roleName"
                class="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-slate-200 rounded-xl shadow-sm"
              >
                <div class="size-1.5 rounded-full bg-primary"></div>
                <span class="text-xs font-bold text-slate-700">{{ roleName }}</span>
              </div>
              <span v-if="!member.roleNames?.length" class="text-sm text-slate-400">未分配角色</span>
            </div>
          </div>

          <div class="space-y-4">
            <h4 class="text-xs font-bold text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-2">联系信息</h4>
            <div class="flex items-center gap-4 p-3 hover:bg-slate-50 rounded-xl transition-colors">
              <div class="size-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-xl leading-none">mail</span>
              </div>
              <div class="min-w-0">
                <p class="text-xs font-bold text-slate-400 uppercase">电子邮箱</p>
                <p class="text-sm font-medium text-slate-700 break-words">{{ member.email || '-' }}</p>
              </div>
            </div>
            <div class="flex items-center gap-4 p-3 hover:bg-slate-50 rounded-xl transition-colors">
              <div class="size-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-xl leading-none">call</span>
              </div>
              <div class="min-w-0">
                <p class="text-xs font-bold text-slate-400 uppercase">手机号码</p>
                <p class="text-sm font-medium text-slate-700 break-words">{{ member.mobile || '-' }}</p>
              </div>
            </div>
          </div>

          <div class="space-y-4 pb-8">
            <h4 class="text-xs font-bold text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-2">工作摘要</h4>
            <div class="grid grid-cols-2 gap-3">
              <div class="text-center p-3 bg-slate-50 rounded-xl border border-slate-100">
                <p class="text-lg font-black text-slate-900">-</p>
                <p class="text-xs font-bold text-slate-400 uppercase mt-1">跟进客户</p>
              </div>
              <div class="text-center p-3 bg-slate-50 rounded-xl border border-slate-100">
                <p class="text-lg font-black text-slate-900">-</p>
                <p class="text-xs font-bold text-slate-400 uppercase mt-1">活跃商机</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-3 border-t border-slate-100 px-4 py-3">
        <el-button class="flex-1" type="primary" size="large" @click="$emit('edit', member)">
          <span class="material-symbols-outlined text-base mr-1">edit</span>
          编辑资料
        </el-button>
        <el-button size="large" type="danger" plain class="!px-3" @click="$emit('delete', member)">
          <span class="material-symbols-outlined text-base">delete</span>
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  member: any
  getAvatarColor: (name: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'edit', member: any): void
  (e: 'delete', member: any): void
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})
</script>

<style>
/* el-drawer is teleported; keep global so body padding resets apply */
.employee-detail-drawer .el-drawer__body {
  padding: 0 !important;
}
</style>
