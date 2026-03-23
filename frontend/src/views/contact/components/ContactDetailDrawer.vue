<template>
  <el-drawer
    v-model="open"
    direction="rtl"
    :size="isMobile ? '100%' : '400px'"
    destroy-on-close
    :with-header="false"
    :modal="isMobile"
    :lock-scroll="isMobile"
    :modal-penetrable="!isMobile"
    class="contact-detail-drawer"
  >
    <template v-if="contact">
      <div class="flex h-full flex-col bg-white shadow-2xl">
        <!-- Header -->
        <div
          class="flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50/50 px-6 py-6 sm:px-8"
        >
          <div class="flex min-w-0 items-center gap-3">
            <div
              class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-lg font-bold text-primary"
            >
              {{ displayInitial }}
            </div>
            <div class="min-w-0">
              <h3 class="truncate text-sm font-bold text-slate-900">联系人详情</h3>
              <p
                class="truncate text-[10px] font-bold uppercase tracking-widest text-slate-400"
              >
                {{ contact.position || '联系人' }}
              </p>
            </div>
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <button
              v-if="canEdit"
              type="button"
              class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200 hover:text-primary"
              title="编辑联系人"
              aria-label="编辑联系人"
              @click="handleEdit"
            >
              <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
            </button>
            <button
              v-if="canDelete"
              type="button"
              class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-red-50 hover:text-red-500"
              title="删除联系人"
              aria-label="删除联系人"
              @click="confirmDelete"
            >
              <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
            </button>
            <button
              type="button"
              class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200"
              title="关闭"
              aria-label="关闭"
              @click="closeDrawer"
            >
              <span class="material-symbols-outlined text-[18px] leading-none">close</span>
            </button>
          </div>
        </div>

        <!-- Body -->
        <div class="min-h-0 flex-1 overflow-y-auto px-6 py-8 sm:px-8">
          <div class="space-y-8">
            <!-- Profile -->
            <div
              class="flex flex-col items-center border-b border-slate-100 pb-8 text-center"
            >
              <div
                class="mb-4 flex size-24 items-center justify-center rounded-[2rem] bg-slate-100 text-slate-400 shadow-inner"
              >
                <span class="material-symbols-outlined text-5xl leading-none">person</span>
              </div>
              <h4 class="mb-1 text-2xl font-black text-slate-900">
                {{ contact.name || '—' }}
              </h4>
              <div class="flex flex-wrap items-center justify-center gap-2">
                <span
                  class="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-primary"
                >
                  {{ contact.position || '联系人' }}
                </span>
                <span
                  v-if="contact.isPrimary"
                  class="rounded-full bg-amber-100 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-amber-600"
                >
                  主要联系人
                </span>
              </div>
            </div>

            <!-- Primary contact rows -->
            <div class="space-y-4">
              <div
                class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-all hover:border-primary/20"
              >
                <div class="flex items-center gap-4">
                  <div
                    class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white text-slate-400 shadow-sm transition-colors group-hover:text-primary"
                  >
                    <span class="material-symbols-outlined text-xl leading-none">call</span>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p
                      class="mb-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400"
                    >
                      电话
                    </p>
                    <p class="font-mono text-sm font-bold text-slate-900">
                      {{ contact.phone || '未填写' }}
                    </p>
                  </div>
                  <button
                    v-if="contact.phone"
                    type="button"
                    class="flex size-8 shrink-0 items-center justify-center rounded-lg text-slate-300 transition-all hover:bg-white hover:text-primary"
                    title="复制电话"
                    aria-label="复制电话"
                    @click="copyText(contact.phone, '电话已复制')"
                  >
                    <span class="material-symbols-outlined text-lg leading-none">content_copy</span>
                  </button>
                </div>
              </div>

              <div
                class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-all hover:border-primary/20"
              >
                <div class="flex items-center gap-4">
                  <div
                    class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white text-slate-400 shadow-sm transition-colors group-hover:text-primary"
                  >
                    <span class="material-symbols-outlined text-xl leading-none">mail</span>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p
                      class="mb-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400"
                    >
                      邮箱
                    </p>
                    <p class="break-all text-sm font-bold text-slate-900">
                      {{ contact.email || '未填写' }}
                    </p>
                  </div>
                  <a
                    v-if="contact.email"
                    class="flex size-8 shrink-0 items-center justify-center rounded-lg text-slate-300 transition-all hover:bg-white hover:text-primary"
                    :href="`mailto:${contact.email}`"
                    title="发送邮件"
                    aria-label="发送邮件"
                  >
                    <span class="material-symbols-outlined text-lg leading-none">send</span>
                  </a>
                </div>
              </div>

              <div class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
                <div class="flex items-center gap-4">
                  <div
                    class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white text-slate-400 shadow-sm"
                  >
                    <span class="material-symbols-outlined text-xl leading-none">chat</span>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p
                      class="mb-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400"
                    >
                      微信
                    </p>
                    <p class="text-sm font-bold text-slate-900">
                      {{ contact.wechat || '未填写' }}
                    </p>
                  </div>
                  <button
                    v-if="contact.wechat"
                    type="button"
                    class="flex size-8 shrink-0 items-center justify-center rounded-lg text-slate-300 transition-all hover:bg-white hover:text-primary"
                    title="复制微信号"
                    aria-label="复制微信号"
                    @click="copyText(contact.wechat, '微信号已复制')"
                  >
                    <span class="material-symbols-outlined text-lg leading-none">content_copy</span>
                  </button>
                </div>
              </div>

              <div class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
                <div class="flex items-center gap-4">
                  <div
                    class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white text-slate-400 shadow-sm"
                  >
                    <span class="material-symbols-outlined text-xl leading-none">calendar_today</span>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p
                      class="mb-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400"
                    >
                      创建时间
                    </p>
                    <p class="text-sm font-bold text-slate-900">
                      {{ contact.createTime ? formatDateTime(contact.createTime) : '—' }}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Notes -->
            <div class="space-y-4 border-t border-slate-100 pt-4">
              <div class="rounded-xl border border-slate-100 bg-slate-50/50 p-3">
                <p class="mb-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  备注
                </p>
                <p class="text-xs font-medium leading-relaxed text-slate-600">
                  {{ contact.notes || '—' }}
                </p>
              </div>
            </div>

            <!-- AI insight (visual parity with product reference) -->
            <section class="relative overflow-hidden rounded-[2rem] bg-slate-900 p-6 text-white">
              <div class="absolute right-0 top-0 p-4 opacity-10">
                <WkIcon name="ai" class="text-6xl text-primary" />
              </div>
              <div class="relative z-10">
                <div class="mb-4 flex items-center gap-2">
                  <WkIcon name="ai" class="text-primary text-xl" />
                  <h3 class="text-sm font-bold tracking-wider">AI 互动建议</h3>
                </div>
                <p class="text-xs italic leading-relaxed text-slate-400">
                  「跟进时可结合客户近期沟通节奏，优先确认关键决策人诉求，再推进下一步方案。」
                </p>
              </div>
            </section>
          </div>
        </div>

        <!-- Footer -->
        <div class="shrink-0 flex gap-3 border-t border-slate-100 bg-white p-6">
          <button
            v-if="contact && !contact.isPrimary && canSetPrimary"
            type="button"
            class="min-w-0 flex-1 rounded-xl bg-primary py-3 text-sm font-bold text-white shadow-lg shadow-primary/25 transition-all hover:bg-primary/90 active:scale-[0.99]"
            @click="handleSetPrimary"
          >
            设为主联系人
          </button>
          <button
            type="button"
            class="min-w-0 flex-1 rounded-xl bg-slate-100 py-3 text-sm font-bold text-slate-600 transition-all hover:bg-slate-200"
            @click="closeDrawer"
          >
            关闭
          </button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import type { Contact } from '@/types/customer'
import WkIcon from '@/components/common/WkIcon.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    contact?: Contact | null
    canEdit?: boolean
    canDelete?: boolean
    canSetPrimary?: boolean
  }>(),
  {
    contact: null,
    canEdit: false,
    canDelete: false,
    canSetPrimary: false
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'edit', contact: Contact): void
  (e: 'delete', contactId: string): void
  (e: 'set-primary', contactId: string): void
}>()

const { isMobile } = useResponsive()

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const displayInitial = computed(() => {
  const n = props.contact?.name?.trim()
  if (!n) return '?'
  return n.charAt(0)
})

function closeDrawer() {
  open.value = false
}

function handleEdit() {
  if (!props.contact) return
  emit('edit', props.contact)
  open.value = false
}

function handleSetPrimary() {
  if (!props.contact) return
  emit('set-primary', props.contact.contactId)
  open.value = false
}

function handleDelete() {
  if (!props.contact) return
  emit('delete', props.contact.contactId)
  open.value = false
}

async function confirmDelete() {
  if (!props.contact) return
  try {
    await ElMessageBox.confirm('确定删除该联系人吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  handleDelete()
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function copyText(text: string, successMsg: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMsg)
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}
</script>

<style>
/* el-drawer is teleported; keep global */
.contact-detail-drawer .el-drawer__body {
  padding: 0 !important;
}
</style>
