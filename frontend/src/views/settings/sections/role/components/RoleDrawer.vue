<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="visible" class="fixed inset-0 z-[100]">
        <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click="drawerVisible = false" />

        <Transition
          enter-active-class="transition duration-300 ease-out"
          enter-from-class="translate-x-full"
          enter-to-class="translate-x-0"
          leave-active-class="transition duration-200 ease-in"
          leave-from-class="translate-x-0"
          leave-to-class="translate-x-full"
        >
          <div v-if="visible" class="fixed inset-y-0 right-0 w-full max-w-3xl bg-slate-50 shadow-2xl flex flex-col">
            <div class="bg-white px-8 pt-8 pb-0 border-b border-slate-200 shrink-0">
              <div class="flex items-start justify-between mb-6">
                <div>
                  <div class="flex items-center gap-3 mb-2">
                    <h2 class="text-2xl font-bold text-slate-900">{{ role }}</h2>
                    <span v-if="roleDrawerRole?.realm === 'super_admin'" class="px-2 py-1 bg-slate-100 text-slate-600 text-xs font-bold rounded-md uppercase tracking-wider border border-slate-200">
                      系统预设角色
                    </span>
                  </div>
                  <p class="text-sm text-slate-500">{{ roleDrawerRole?.description || '暂无描述' }}</p>
                </div>
                <button
                  class="size-8 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 transition-colors"
                  @click="drawerVisible = false"
                >
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>

              <div class="flex gap-8 relative">
                <button
                  class="pb-4 text-sm font-bold transition-colors relative"
                  :class="activeRoleDrawerTab === 'members' ? 'text-primary' : 'text-slate-500 hover:text-slate-800'"
                  @click="$emit('update:tab', 'members')"
                >
                  角色成员
                  <span class="ml-2 px-2 py-0.5 bg-slate-100 text-slate-600 rounded-full text-xs">{{ roleDrawerRole?.userCount || 0 }}</span>
                  <div v-if="activeRoleDrawerTab === 'members'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary rounded-t-full"></div>
                </button>
                <button
                  v-if="canConfigurePermissions"
                  class="pb-4 text-sm font-bold transition-colors relative"
                  :class="activeRoleDrawerTab === 'permissions' ? 'text-primary' : 'text-slate-500 hover:text-slate-800'"
                  @click="$emit('update:tab', 'permissions')"
                >
                  权限配置
                  <div v-if="activeRoleDrawerTab === 'permissions'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary rounded-t-full"></div>
                </button>
              </div>
            </div>

            <div class="flex-1 overflow-y-auto p-8">
              <div v-if="activeRoleDrawerTab === 'members'" class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
                <div class="p-5 border-b border-slate-100 flex items-center justify-between bg-white">
                  <div class="relative">
                    <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">search</span>
                    <input
                      v-model="roleUserSearchValue"
                      type="text"
                      placeholder="搜索成员姓名或邮箱..."
                      class="pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/20 focus:border-primary focus:bg-white outline-none w-72 transition-all"
                    />
                  </div>
                  <button
                    class="bg-primary text-white px-4 py-2 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-primary/90 transition-all shadow-sm shadow-primary/20"
                    @click="$emit('open-add-user')"
                  >
                    <span class="material-symbols-outlined wk-plus-button-icon">person_add</span>
                    添加成员
                  </button>
                </div>
                <div class="overflow-x-auto">
                  <div v-if="loadingRoleUsers" class="text-center py-16">
                    <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
                  </div>
                  <table v-else-if="filteredRoleUsers.length > 0" class="w-full text-left text-sm">
                    <thead class="bg-slate-50/50 border-b border-slate-100 text-slate-500">
                      <tr>
                        <th class="px-6 py-4 font-bold">成员信息</th>
                        <th class="px-6 py-4 font-bold">邮箱</th>
                        <th class="px-6 py-4 font-bold text-right">操作</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-50">
                      <tr v-for="user in filteredRoleUsers" :key="user.userId" class="hover:bg-slate-50/80 transition-colors group">
                        <td class="px-6 py-4">
                          <div class="flex items-center gap-3">
                            <div class="size-9 rounded-full flex items-center justify-center text-white font-bold shadow-sm border border-white/20" :class="getAvatarColor(user.realname || user.username)">
                              {{ (user.realname || user.username || '?').charAt(0) }}
                            </div>
                            <div class="font-bold text-slate-900">{{ user.realname || user.username }}</div>
                          </div>
                        </td>
                        <td class="px-6 py-4 text-slate-500">{{ user.email || '-' }}</td>
                        <td class="px-6 py-4 text-right">
                          <button
                            class="text-slate-400 hover:text-red-600 font-medium transition-colors p-2 rounded-lg hover:bg-red-50 opacity-0 group-hover:opacity-100"
                            @click="$emit('remove-user', user)"
                          >
                            <span class="material-symbols-outlined text-sm">person_remove</span>
                          </button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-else class="py-10 text-center text-slate-400">
                    <span class="material-symbols-outlined text-3xl mb-2 opacity-50">group_off</span>
                    <p class="text-xs">暂无成员</p>
                  </div>
                </div>
              </div>

              <div v-else class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
                <div class="p-5 border-b border-slate-100 flex items-center justify-between bg-white sticky top-0 z-10">
                  <div>
                    <h3 class="text-base font-bold text-slate-900">功能权限配置</h3>
                    <p class="text-xs text-slate-500 mt-1">配置该角色在各个业务模块中的操作权限及数据可见范围。</p>
                  </div>
                  <button
                    class="bg-primary text-white px-5 py-2 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-primary/90 transition-all shadow-sm shadow-primary/20 disabled:opacity-50"
                    :disabled="savingPermissions"
                    @click="$emit('save-permissions')"
                  >
                    <span v-if="savingPermissions" class="size-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    <span v-else class="material-symbols-outlined text-sm">save</span>
                    保存配置
                  </button>
                </div>
                <div v-if="loadingPermissions" class="text-center py-16">
                  <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
                </div>
                <div v-else class="p-6 space-y-6 bg-slate-50/30">
                  <div v-for="moduleGroup in permissionList" :key="moduleGroup.module" class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm hover:border-primary/30 transition-colors">
                    <div class="bg-slate-50/80 px-6 py-4 flex items-center justify-between border-b border-slate-100">
                      <div class="flex items-center gap-3">
                        <span class="text-sm font-bold text-slate-900">{{ moduleGroup.moduleName }}</span>
                      </div>
                      <span class="text-xs font-medium text-slate-400">{{ moduleGroup.actions.filter((action: any) => action.enabled).length }}/{{ moduleGroup.actions.length }} 已启用</span>
                    </div>
                    <div class="p-6 grid grid-cols-1 sm:grid-cols-2 gap-6">
                      <div v-for="action in moduleGroup.actions" :key="action.action" class="flex flex-col gap-3">
                        <label class="flex items-center gap-3 text-sm text-slate-700 cursor-pointer hover:text-primary transition-colors font-bold group/action">
                          <div class="relative flex items-center">
                            <input v-model="action.enabled" type="checkbox" class="peer sr-only" />
                            <div class="w-4 h-4 border-2 border-slate-300 rounded peer-checked:bg-primary peer-checked:border-primary transition-all flex items-center justify-center group-hover/action:border-primary/50">
                              <span class="material-symbols-outlined text-white text-[12px]" :class="action.enabled ? 'opacity-100' : 'opacity-0'">check</span>
                            </div>
                          </div>
                          {{ action.actionName }}
                        </label>
                        <div v-if="action.hasScopeOption" class="pl-7">
                          <div class="relative">
                            <select
                              v-model="action.dataScope"
                              :disabled="!action.enabled"
                              class="w-full appearance-none text-xs bg-slate-50 border border-slate-200 rounded-lg pl-3 pr-8 py-2 focus:ring-2 focus:ring-primary/20 focus:border-primary focus:bg-white text-slate-700 outline-none transition-all font-medium cursor-pointer hover:border-slate-300 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              <option v-for="option in dataScopeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                            </select>
                            <span class="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 text-sm pointer-events-none">
                              expand_more
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RolePermissionVO, RoleVO } from '@/types/role'

const props = defineProps<{
  visible: boolean
  roleDrawerRole: RoleVO | null
  roleDrawerTab: 'members' | 'permissions'
  loadingRoleUsers: boolean
  roleUserSearch: string
  filteredRoleUsers: any[]
  loadingPermissions: boolean
  permissionList: RolePermissionVO[]
  savingPermissions: boolean
  dataScopeOptions: Array<{ value: number; label: string }>
  getAvatarColor: (name: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:tab', value: 'members' | 'permissions'): void
  (e: 'update:roleUserSearch', value: string): void
  (e: 'open-add-user'): void
  (e: 'remove-user', user: any): void
  (e: 'save-permissions'): void
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

// 将抽屉内的成员搜索输入同步回父级状态，便于直接在当前组件里使用 v-model。
const roleUserSearchValue = computed({
  get: () => props.roleUserSearch,
  set: (value: string) => emit('update:roleUserSearch', value)
})

// 超级管理员属于系统预设角色，这里不提供权限配置入口。
const canConfigurePermissions = computed(() => !!props.roleDrawerRole && props.roleDrawerRole.realm !== 'super_admin')
// 当当前角色不可配置权限时，统一回退到成员页，避免外部 tab 状态残留到权限页。
const activeRoleDrawerTab = computed<'members' | 'permissions'>(() => (canConfigurePermissions.value ? props.roleDrawerTab : 'members'))
const role = computed(() => props.roleDrawerRole?.roleName || '')
</script>
