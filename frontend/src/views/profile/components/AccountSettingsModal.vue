<template>
  <Teleport to="body">
    <Transition name="account-settings-overlay">
      <div v-if="modelValue" class="fixed inset-0 z-[300] flex items-center justify-center p-4 sm:p-6">
        <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="handleClose" />

        <Transition name="account-settings-panel">
          <div
            v-if="modelValue"
            class="relative w-full bg-white shadow-2xl overflow-hidden flex flex-col"
            :class="isMobile ? 'max-w-full max-h-full h-full rounded-none' : 'max-w-2xl max-h-[90vh] rounded-2xl'"
          >
            <div class="bg-white border-b border-slate-200 px-6 md:px-8 py-5 flex items-center justify-between shrink-0">
              <div class="flex items-center gap-4">
                <div class="size-12 rounded-2xl bg-primary/10 flex items-center justify-center text-primary">
                  <WkIcon name="settings" :size="24" />
                </div>
                <div>
                  <h2 class="text-xl font-bold text-slate-900">账号设置</h2>
                  <p class="text-xs text-slate-500">管理您的个人信息与账户安全</p>
                </div>
              </div>
              <button
                class="size-10 rounded-full flex items-center justify-center text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-all"
                @click="handleClose"
              >
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>

            <div class="flex-1 overflow-y-auto p-6 md:p-8 space-y-10">
              <div class="space-y-6">
                <div class="flex items-center justify-between gap-4">
                  <h3 class="text-xs font-bold text-slate-900 flex items-center gap-2 uppercase tracking-wider">
                    <span class="w-1.5 h-4 bg-primary rounded-full"></span>
                    基本信息
                  </h3>
                  <button
                    class="px-5 py-2 bg-primary text-white rounded-xl text-sm font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all disabled:opacity-50"
                    :disabled="savingProfile"
                    @click="handleSaveProfile"
                  >
                    {{ savingProfile ? '保存中...' : '保存资料' }}
                  </button>
                </div>

                <div class="flex flex-col md:flex-row gap-8 items-start">
                  <div class="flex flex-col items-center gap-3 py-2 shrink-0">
                    <div class="relative group">
                      <div class="size-28 rounded-full bg-slate-100 border-4 border-white shadow-md overflow-hidden flex items-center justify-center">
                        <img
                          v-if="avatarPreviewUrl || profileForm.imgUrl"
                          :src="avatarPreviewUrl || profileForm.imgUrl"
                          alt="Avatar Preview"
                          class="w-full h-full object-cover"
                        />
                        <span v-else class="text-4xl font-black text-primary/70">
                          {{ (profileForm.realname || userStore.realname || 'U').charAt(0) }}
                        </span>
                      </div>
                      <button
                        class="absolute inset-0 flex items-center justify-center bg-black/40 text-white opacity-0 group-hover:opacity-100 rounded-full transition-opacity"
                        type="button"
                        @click="avatarInputRef?.click()"
                      >
                        <span v-if="avatarUploading" class="material-symbols-outlined text-2xl animate-spin">progress_activity</span>
                        <span v-else class="material-symbols-outlined text-2xl">photo_camera</span>
                      </button>
                      <input ref="avatarInputRef" type="file" class="hidden" accept="image/*" @change="handleAvatarChange" />
                    </div>
                    <p class="text-[10px] text-slate-400 font-bold uppercase tracking-tight">点击更换头像</p>
                  </div>

                  <div class="flex-1 grid grid-cols-1 sm:grid-cols-2 gap-4 w-full">
                    <div class="space-y-1.5">
                      <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">姓名</label>
                      <input
                        v-model="profileForm.realname"
                        type="text"
                        class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">部门</label>
                      <input
                        v-model="profileForm.department"
                        type="text"
                        disabled
                        class="w-full px-4 py-2.5 bg-slate-100 border border-slate-200 rounded-xl text-sm text-slate-500 outline-none transition-all cursor-not-allowed"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">职位</label>
                      <input
                        v-model="profileForm.position"
                        type="text"
                        class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">电子邮箱</label>
                      <input
                        v-model="profileForm.email"
                        type="email"
                        class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all"
                      />
                    </div>
                    <div class="space-y-1.5 sm:col-span-2">
                      <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">手机号码</label>
                      <input
                        v-model="profileForm.phone"
                        type="text"
                        class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all"
                      />
                    </div>
                  </div>
                </div>

                <div class="flex justify-end gap-3">
                  <button
                    class="px-5 py-2.5 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100 transition-colors"
                    type="button"
                    @click="resetProfileForm"
                  >
                    重置资料
                  </button>
                </div>
              </div>

              <div class="h-px bg-slate-100 w-full" />

              <div class="space-y-6">
                <div class="flex items-center justify-between gap-4">
                  <h3 class="text-xs font-bold text-slate-900 flex items-center gap-2 uppercase tracking-wider">
                    <span class="w-1.5 h-4 bg-rose-500 rounded-full"></span>
                    安全设置
                  </h3>
                  <button
                    class="px-5 py-2 bg-rose-500 text-white rounded-xl text-sm font-bold shadow-lg shadow-rose-500/20 hover:bg-rose-500/90 transition-all disabled:opacity-50"
                    :disabled="submittingPassword"
                    @click="handleChangePassword"
                  >
                    {{ submittingPassword ? '提交中...' : '更新密码' }}
                  </button>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div class="space-y-1.5">
                    <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">当前密码</label>
                    <input
                      v-model="passwordForm.oldPassword"
                      type="password"
                      placeholder="请输入当前密码"
                      class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">新密码</label>
                    <input
                      v-model="passwordForm.newPassword"
                      type="password"
                      placeholder="请输入新密码"
                      class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">确认新密码</label>
                    <input
                      v-model="passwordForm.confirmPassword"
                      type="password"
                      placeholder="请再次输入新密码"
                      class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all"
                    />
                  </div>
                </div>

                <div class="rounded-2xl bg-slate-50 border border-slate-100 px-4 py-3 text-xs text-slate-500">
                  密码修改成功后将立即生效。为安全起见，建议使用至少 6 位并包含字母与数字的组合。
                </div>
              </div>
            </div>

            <div class="bg-slate-50 border-t border-slate-200 px-6 md:px-8 py-5 flex items-center justify-end shrink-0">
              <button
                class="px-6 py-2.5 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-200 transition-colors"
                type="button"
                @click="handleClose"
              >
                关闭
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useResponsive } from '@/composables/useResponsive'
import { useAccountSettings } from '../useAccountSettings'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const { isMobile } = useResponsive()
const {
  userStore,
  savingProfile,
  avatarUploading,
  avatarInputRef,
  avatarPreviewUrl,
  submittingPassword,
  profileForm,
  passwordForm,
  loadProfile,
  resetAll,
  resetProfileForm,
  handleAvatarChange,
  handleSaveProfile,
  handleChangePassword
} = useAccountSettings()

watch(() => props.modelValue, async (visible) => {
  if (visible) {
    await loadProfile()
    return
  }
  resetAll()
})

function handleClose() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.account-settings-overlay-enter-active,
.account-settings-overlay-leave-active {
  transition: opacity 0.22s ease;
}

.account-settings-overlay-enter-from,
.account-settings-overlay-leave-to {
  opacity: 0;
}

.account-settings-panel-enter-active,
.account-settings-panel-leave-active {
  transition: transform 0.22s ease, opacity 0.22s ease;
}

.account-settings-panel-enter-from,
.account-settings-panel-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.98);
}
</style>
