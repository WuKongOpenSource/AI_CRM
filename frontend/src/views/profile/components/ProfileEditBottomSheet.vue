<template>
  <Teleport to="body">
    <Transition name="profile-edit-sheet-overlay">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[330] flex items-end justify-center px-0"
        aria-hidden="false"
        @click="handleClose"
      >
        <Transition name="profile-edit-sheet-panel">
          <section
            v-if="modelValue"
            class="relative flex max-h-[min(86dvh,760px)] w-full max-w-[480px] flex-col overflow-hidden rounded-t-[34px] border border-white/80 bg-white/95 shadow-[0_-18px_60px_rgba(15,23,42,0.18)] backdrop-blur-2xl"
            role="dialog"
            aria-modal="true"
            aria-labelledby="profile-edit-title"
            @click.stop
          >
            <h2 id="profile-edit-title" class="sr-only">账号设置</h2>

            <div class="flex-1 overflow-y-auto px-6 pb-6 pt-8 text-[14px]">
              <div class="flex flex-col items-center">
                <div class="relative">
                  <div class="flex size-[104px] items-center justify-center overflow-hidden rounded-full bg-[#a5ab87] text-[34px] font-medium text-white">
                    <img
                      v-if="avatarPreviewUrl || profileForm.imgUrl"
                      :src="avatarPreviewUrl || profileForm.imgUrl"
                      class="h-full w-full object-cover"
                      alt="avatar"
                    />
                    <span v-else>{{ avatarInitials }}</span>
                  </div>
                  <button
                    type="button"
                    class="absolute bottom-1 right-1 flex size-10 items-center justify-center rounded-full border-[3px] border-white bg-white text-[#5f6368] shadow-[0_6px_18px_rgba(15,23,42,0.18)] transition-colors active:bg-slate-100"
                    aria-label="更换头像"
                    :disabled="avatarUploading"
                    @click="avatarInputRef?.click()"
                  >
                    <span v-if="avatarUploading" class="material-symbols-outlined animate-spin text-[20px] leading-none">progress_activity</span>
                    <span v-else class="material-symbols-outlined text-[20px] leading-none">photo_camera</span>
                  </button>
                  <input ref="avatarInputRef" type="file" class="hidden" accept="image/*" @change="handleAvatarChange" />
                </div>
              </div>

              <form class="mt-7 space-y-6" @submit.prevent="handleSubmit">
                <section class="space-y-5">
                  <div class="flex items-center justify-between gap-4">
                    <h3 class="text-[14px] font-semibold text-[#0d0d0d]">基本信息</h3>
                    <button
                      type="button"
                      class="rounded-full px-3 py-1.5 text-[14px] font-medium text-[#5f6368] transition-colors active:bg-slate-100 disabled:opacity-50"
                      :disabled="savingProfile || avatarUploading"
                      @click="resetProfileForm"
                    >
                      重置资料
                    </button>
                  </div>

                  <div class="space-y-4">
                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-realname">姓名</label>
                      <input
                        id="profile-realname"
                        v-model="profileForm.realname"
                        type="text"
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/80 px-5 text-[14px] font-semibold text-[#0d0d0d] outline-none transition-colors placeholder:text-slate-300 focus:border-[#9a9a9f]"
                      />
                    </div>

                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-username">用户名</label>
                      <input
                        id="profile-username"
                        :value="usernameValue"
                        type="text"
                        disabled
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/70 px-5 text-[14px] font-medium text-[#0d0d0d] outline-none"
                      />
                    </div>

                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-department">部门</label>
                      <input
                        id="profile-department"
                        v-model="profileForm.department"
                        type="text"
                        disabled
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/70 px-5 text-[14px] font-medium text-[#5f6368] outline-none"
                      />
                    </div>

                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-position">职位</label>
                      <input
                        id="profile-position"
                        v-model="profileForm.position"
                        type="text"
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/80 px-5 text-[14px] font-medium text-[#0d0d0d] outline-none transition-colors placeholder:text-slate-300 focus:border-[#9a9a9f]"
                      />
                    </div>

                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-email">电子邮箱</label>
                      <input
                        id="profile-email"
                        v-model="profileForm.email"
                        type="email"
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/80 px-5 text-[14px] font-medium text-[#0d0d0d] outline-none transition-colors placeholder:text-slate-300 focus:border-[#9a9a9f]"
                      />
                    </div>

                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-phone">手机号码</label>
                      <input
                        id="profile-phone"
                        v-model="profileForm.phone"
                        type="text"
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/80 px-5 text-[14px] font-medium text-[#0d0d0d] outline-none transition-colors placeholder:text-slate-300 focus:border-[#9a9a9f]"
                      />
                    </div>
                  </div>

                  <p class="mx-auto max-w-[340px] text-center text-[14px] leading-6 text-[#6b6b6b]">
                    个人资料有助于他人识别你的身份。你的姓名和用户名将用于系统内展示。
                  </p>
                </section>

                <div class="h-px bg-[#ececf0]" />

                <section class="space-y-5">
                  <div class="flex items-center justify-between gap-4">
                    <h3 class="text-[14px] font-semibold text-[#0d0d0d]">安全设置</h3>
                    <button
                      type="button"
                      class="rounded-full bg-[#f04444] px-4 py-2 text-[14px] font-semibold text-white transition-colors active:bg-[#dc3434] disabled:opacity-50"
                      :disabled="submittingPassword"
                      @click="handleChangePassword"
                    >
                      {{ submittingPassword ? '提交中...' : '更新密码' }}
                    </button>
                  </div>

                  <div class="space-y-4">
                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-old-password">当前密码</label>
                      <input
                        id="profile-old-password"
                        v-model="passwordForm.oldPassword"
                        type="password"
                        placeholder="请输入当前密码"
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/80 px-5 text-[14px] font-medium text-[#0d0d0d] outline-none transition-colors placeholder:text-slate-300 focus:border-[#9a9a9f]"
                      />
                    </div>

                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-new-password">新密码</label>
                      <input
                        id="profile-new-password"
                        v-model="passwordForm.newPassword"
                        type="password"
                        placeholder="请输入新密码"
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/80 px-5 text-[14px] font-medium text-[#0d0d0d] outline-none transition-colors placeholder:text-slate-300 focus:border-[#9a9a9f]"
                      />
                    </div>

                    <div class="space-y-2.5">
                      <label class="block px-1 text-[14px] font-medium text-[#5f6368]" for="profile-confirm-password">确认新密码</label>
                      <input
                        id="profile-confirm-password"
                        v-model="passwordForm.confirmPassword"
                        type="password"
                        placeholder="请再次输入新密码"
                        class="h-12 w-full rounded-full border border-[#d9d9de] bg-white/80 px-5 text-[14px] font-medium text-[#0d0d0d] outline-none transition-colors placeholder:text-slate-300 focus:border-[#9a9a9f]"
                      />
                    </div>
                  </div>

                  <p class="rounded-[18px] bg-[#f5f5f7] px-4 py-3 text-[14px] leading-5 text-[#6b6b6b]">
                    密码修改成功后将立即生效。为安全起见，建议使用至少 6 位并包含字母与数字的组合。
                  </p>
                </section>
              </form>
            </div>

            <div class="shrink-0 border-t border-[#ececf0] bg-white/90 px-6 pb-[calc(1rem+env(safe-area-inset-bottom))] pt-4">
              <div class="flex flex-col items-center gap-3">
                <button
                  type="button"
                  class="h-12 min-w-[184px] rounded-full bg-black px-7 text-[14px] font-semibold text-white shadow-[0_8px_22px_rgba(0,0,0,0.18)] transition-colors active:bg-[#232323] disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="savingProfile || avatarUploading"
                  @click="handleSubmit"
                >
                  {{ savingProfile ? '保存中...' : '保存个人资料' }}
                </button>
                <button
                  type="button"
                  class="h-9 px-6 text-[14px] font-medium text-[#0d0d0d] transition-opacity active:opacity-60"
                  @click="handleClose"
                >
                  取消
                </button>
              </div>
            </div>
          </section>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useAccountSettings } from '../useAccountSettings'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

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
  handleChangePassword,
} = useAccountSettings()

const usernameValue = computed(() => userStore.username || userStore.userInfo?.username || '')
const avatarInitials = computed(() => {
  const name = profileForm.realname || userStore.realname || userStore.username || 'U'
  return name.trim().slice(0, 2).toUpperCase() || 'U'
})

watch(() => props.modelValue, async visible => {
  if (visible) {
    await loadProfile()
    return
  }
  resetAll()
})

function handleClose() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  const saved = await handleSaveProfile()
  if (saved) {
    handleClose()
  }
}
</script>

<style scoped>
.profile-edit-sheet-overlay-enter-active,
.profile-edit-sheet-overlay-leave-active {
  transition: opacity 0.22s ease;
}

.profile-edit-sheet-overlay-enter-from,
.profile-edit-sheet-overlay-leave-to {
  opacity: 0;
}

.profile-edit-sheet-panel-enter-active,
.profile-edit-sheet-panel-leave-active {
  transition: transform 0.28s ease, opacity 0.22s ease;
}

.profile-edit-sheet-panel-enter-from,
.profile-edit-sheet-panel-leave-to {
  opacity: 0;
  transform: translateY(100%);
}
</style>
