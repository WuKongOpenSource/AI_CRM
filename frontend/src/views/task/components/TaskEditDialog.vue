<template>
  <el-dialog
    v-model="open"
    :width="isMobile ? '95%' : '720px'"
    :show-close="false"
    destroy-on-close
    top="10vh"
    :fullscreen="isMobile"
    class="!rounded-2xl !p-0 overflow-hidden task-dialog"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div :class="[
            'size-10 rounded-xl flex items-center justify-center',
            editingTask ? 'bg-blue-50' : 'bg-primary/10'
          ]">
            <span :class="[
              'material-symbols-outlined text-xl',
              editingTask ? 'text-blue-500' : 'text-primary'
            ]">
              {{ editingTask ? 'edit_note' : 'task_alt' }}
            </span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-900">{{ editingTask ? '编辑任务' : '新建任务' }}</h2>
            <p class="text-xs text-slate-500 mt-0.5">{{ editingTask ? '修改任务详细信息' : '手动填写或使用 AI 智能解析' }}</p>
          </div>
        </div>
        <button
          @click="open = false"
          class="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
          type="button"
          aria-label="关闭"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </template>

    <div class="space-y-5 bg-slate-50/50 p-5 md:p-6">
      <!-- AI Parse Section (Create only) -->
      <div v-if="!editingTask" class="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div class="flex items-center gap-2 mb-3">
          <span class="material-symbols-outlined text-primary text-sm">auto_awesome</span>
          <span class="text-xs font-bold text-primary">AI 智能解析 (可选)</span>
        </div>
        <div class="relative">
          <textarea
            v-model="aiParseInput"
            placeholder="例如：明天下午两点前给科技创新有限公司的张总发一份 Q4 扩容方案的报价单，标记为高优先级..."
            class="w-full text-sm text-slate-600 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-xl px-4 py-3 outline-none transition-all resize-none h-24"
          />
          <button
            @click="$emit('ai-parse')"
            :disabled="!aiParseInput.trim() || aiParsing"
            class="absolute right-3 bottom-3 flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 text-white text-xs font-bold rounded-lg hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            type="button"
          >
            <span v-if="aiParsing" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
            <span v-else class="material-symbols-outlined text-sm">auto_awesome</span>
            {{ aiParsing ? '解析中...' : '一键解析' }}
          </button>
        </div>
      </div>

      <!-- Form Fields -->
      <div class="bg-white p-4 md:p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <div>
          <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务标题 <span class="text-red-500">*</span></label>
          <input
            v-model="formData.title"
            type="text"
            placeholder="请输入任务标题"
            class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
          />
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">截止时间 <span class="text-red-500">*</span></label>
            <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2.5 transition-all">
              <span class="material-symbols-outlined text-slate-400 text-sm">calendar_today</span>
              <input
                v-model="formData.dueDate"
                type="datetime-local"
                class="w-full text-sm text-slate-900 bg-transparent outline-none"
              />
            </div>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">优先级</label>
            <select
              v-model="formData.priority"
              class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
            >
              <option value="HIGH">高</option>
              <option value="MEDIUM">中</option>
              <option value="LOW">低</option>
            </select>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务类型</label>
            <select
              v-model="formData.taskType"
              class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
            >
              <option value="">请选择</option>
              <option value="跟进">跟进</option>
              <option value="文档">文档</option>
              <option value="会议">会议</option>
              <option value="电话">电话</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">关联客户</label>
            <el-select
              v-model="formData.customerId"
              filterable
              remote
              reserve-keyword
              clearable
              default-first-option
              placeholder="搜索客户名称"
              :remote-method="searchCustomers"
              :loading="customerSearchLoading"
              class="w-full"
              size="default"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </div>

        <div :class="editingTask ? 'grid grid-cols-1 sm:grid-cols-2 gap-4' : ''">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">参与人</label>
            <el-select
              v-model="participants"
              multiple
              filterable
              remote
              reserve-keyword
              clearable
              allow-create
              default-first-option
              placeholder="搜索或输入用户名称"
              :remote-method="searchUsers"
              :loading="userSearchLoading"
              class="w-full"
              size="default"
            >
              <el-option
                v-for="item in userOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
          <div v-if="editingTask">
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">负责人</label>
            <input
              v-model="formData.assignedToName"
              type="text"
              placeholder="请输入负责人"
              class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
            />
          </div>
        </div>

        <div>
          <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务描述</label>
          <textarea
            v-model="formData.description"
            placeholder="请输入详细描述..."
            class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all resize-none h-24"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-3">
        <button
          @click="open = false"
          class="flex-1 py-2.5 text-sm font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          type="button"
        >
          取消
        </button>
        <button
          @click="$emit('submit')"
          :disabled="!formData.title.trim() || submitting"
          class="flex-1 py-2.5 text-sm font-bold text-white bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-colors shadow-sm"
          type="button"
        >
          {{ submitting ? '提交中...' : (editingTask ? '保存修改' : '确认创建') }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Task, TaskAddBO, TaskStatus } from '@/types/common'

type Option = { value: string; label: string }

const props = defineProps<{
  modelValue: boolean
  isMobile: boolean
  editingTask: Task | null
  submitting: boolean
  aiParsing: boolean
  aiParseInput: string
  formData: TaskAddBO & { status?: TaskStatus; customerId?: string; assignedToName?: string }
  selectedParticipants: string[]
  userOptions: Option[]
  userSearchLoading: boolean
  customerOptions: Option[]
  customerSearchLoading: boolean
  searchUsers: (q: string) => void
  searchCustomers: (q: string) => void
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'update:aiParseInput', v: string): void
  (e: 'update:selectedParticipants', v: string[]): void
  (e: 'submit'): void
  (e: 'ai-parse'): void
}>()

const open = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const aiParseInput = computed({
  get: () => props.aiParseInput,
  set: (v: string) => emit('update:aiParseInput', v)
})

const participants = computed({
  get: () => props.selectedParticipants,
  set: (v: string[]) => emit('update:selectedParticipants', v)
})
</script>

<style>
.task-dialog .el-dialog__header {
  padding: 20px 24px 0;
  margin-right: 0;
}
.task-dialog .el-dialog__body {
  padding: 0;
  max-height: 65vh;
  overflow-y: auto;
}
.task-dialog .el-dialog__footer {
  padding: 16px 24px 20px;
}
/* Prevent overlay from scrolling — dialog body scrolls internally */
.el-overlay:has(.task-dialog) {
  overflow: hidden;
}
</style>

