<template>
  <el-dialog
    v-model="dialogVisible"
    title="导入客户"
    :width="isMobile ? '95%' : '800px'"
    :fullscreen="isMobile"
    class="wk-dialog--flush"
    @close="resetImport"
  >
    <div v-if="importStep === 1" class="text-center py-6">
      <el-upload
        ref="importUploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleImportFileChange"
        drag
      >
        <span class="material-symbols-outlined text-4xl text-slate-400 mb-2">upload_file</span>
        <div class="text-slate-600">将 Excel 文件拖到此处，或<em class="text-primary not-italic">点击上传</em></div>
        <template #tip>
          <div class="text-xs text-slate-400 mt-2">支持 .xlsx / .xls 格式，表头需包含「公司名称」列</div>
          <div class="mt-2">
            <button class="text-primary text-sm font-medium hover:underline" @click.stop="handleDownloadTemplate">
              下载导入模板
            </button>
          </div>
        </template>
      </el-upload>
    </div>

    <div v-else-if="importStep === 2">
      <div class="flex gap-4 mb-4 flex-wrap">
        <span class="text-xs font-bold px-2 py-1 bg-slate-100 rounded">总计 {{ importPreview!.totalRows }} 行</span>
        <span class="text-xs font-bold px-2 py-1 bg-emerald-50 text-emerald-600 rounded">有效 {{ importPreview!.validRows }} 行</span>
        <span v-if="importPreview!.duplicateRows > 0" class="text-xs font-bold px-2 py-1 bg-amber-50 text-amber-600 rounded">重复 {{ importPreview!.duplicateRows }} 行</span>
        <span v-if="importPreview!.errorRows > 0" class="text-xs font-bold px-2 py-1 bg-red-50 text-red-600 rounded">错误 {{ importPreview!.errorRows }} 行</span>
      </div>

      <div v-if="importPreview!.duplicateRows > 0" class="mb-4 p-3 bg-yellow-50 rounded-lg">
        <span class="text-sm text-yellow-700 mr-3">重复行统一处理：</span>
        <el-radio-group v-model="globalDuplicateMode" @change="applyGlobalDuplicateMode">
          <el-radio value="skip">全部跳过</el-radio>
          <el-radio value="overwrite">全部覆盖</el-radio>
        </el-radio-group>
      </div>

      <el-table
        :data="importPreview!.rows"
        :max-height="400"
        size="small"
        :row-class-name="importRowClassName"
      >
        <el-table-column label="行号" prop="rowNum" width="60" />
        <el-table-column label="公司名称" prop="companyName" min-width="120" />
        <el-table-column label="行业" prop="industry" width="100" />
        <el-table-column label="阶段" prop="stage" width="90">
          <template #default="{ row }">{{ getStageLabel(row.stage) }}</template>
        </el-table-column>
        <el-table-column label="联系人" prop="contactName" width="90" />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <span v-if="row.errors && row.errors.length > 0" class="text-xs font-bold px-2 py-0.5 bg-red-50 text-red-600 rounded">{{ row.errors[0] }}</span>
            <template v-else-if="row.duplicate">
              <el-radio-group v-model="row.handleMode" size="small">
                <el-radio value="skip">跳过</el-radio>
                <el-radio value="overwrite">覆盖</el-radio>
              </el-radio-group>
            </template>
            <span v-else class="text-xs font-bold px-2 py-0.5 bg-emerald-50 text-emerald-600 rounded">正常</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="importPreview!.errors && importPreview!.errors.length > 0" class="mt-3">
        <el-alert
          v-for="(err, idx) in importPreview!.errors"
          :key="idx"
          :title="err"
          type="error"
          :closable="false"
          class="mb-1"
        />
      </div>
    </div>

    <div v-else class="text-center py-6">
      <span class="material-symbols-outlined text-5xl text-green-500 mb-3">check_circle</span>
      <h3 class="text-lg font-bold mb-4">导入完成</h3>
      <div class="flex justify-center gap-6 text-sm">
        <div>新增 <span class="text-primary font-bold text-lg">{{ importResult!.imported }}</span> 条</div>
        <div>更新 <span class="text-orange-500 font-bold text-lg">{{ importResult!.updated }}</span> 条</div>
        <div>跳过 <span class="text-slate-500 font-bold text-lg">{{ importResult!.skipped }}</span> 条</div>
      </div>
      <div v-if="importResult!.errors && importResult!.errors.length > 0" class="mt-4 text-left">
        <el-alert
          v-for="(err, idx) in importResult!.errors"
          :key="idx"
          :title="err"
          type="warning"
          :closable="false"
          class="mb-1"
        />
      </div>
    </div>

    <template #footer>
      <template v-if="importStep === 1">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="!importFile" @click="handleImportPreview">
          解析文件
        </el-button>
      </template>
      <template v-else-if="importStep === 2">
        <el-button @click="importStep = 1">上一步</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImportConfirm">确认导入</el-button>
      </template>
      <template v-else>
        <el-button type="primary" @click="dialogVisible = false">完成</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { UploadFile, UploadInstance } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { confirmCustomerImport, downloadImportTemplate, importCustomerPreview } from '@/api/customer'
import type { CustomerImportPreview, CustomerImportResult, CustomerImportRow } from '@/types/customer'

const STAGE_LABELS: Record<string, string> = {
  lead: '线索',
  qualified: '资格审查',
  proposal: '方案报价',
  negotiation: '谈判中',
  closed: '已成交',
  lost: '已流失'
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', result: CustomerImportResult): void
}>()

const { isMobile } = useResponsive()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const importStep = ref<1 | 2 | 3>(1)
const importFile = ref<File | null>(null)
const importLoading = ref(false)
const importPreview = ref<CustomerImportPreview | null>(null)
const importResult = ref<CustomerImportResult | null>(null)
const globalDuplicateMode = ref<CustomerImportRow['handleMode']>('')
const importUploadRef = ref<UploadInstance>()

function getStageLabel(stage: string): string {
  return STAGE_LABELS[stage] || stage
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadImportTemplate()
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = '客户导入模板.xlsx'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    URL.revokeObjectURL(url)
  } catch {
    // Error handled by interceptor
  }
}

function handleImportFileChange(uploadFile: UploadFile) {
  importFile.value = uploadFile.raw || null
}

async function handleImportPreview() {
  if (!importFile.value) return

  importLoading.value = true
  try {
    importPreview.value = await importCustomerPreview(importFile.value)
    importStep.value = 2
    importPreview.value.rows.forEach((row) => {
      if (row.duplicate && !row.handleMode) {
        row.handleMode = 'skip'
      }
    })
  } catch {
    // Error handled by interceptor
  } finally {
    importLoading.value = false
  }
}

function applyGlobalDuplicateMode(mode: string | number | boolean) {
  if (!importPreview.value) return
  if (mode !== 'skip' && mode !== 'overwrite') return

  importPreview.value.rows.forEach((row) => {
    if (row.duplicate) {
      row.handleMode = mode
    }
  })
}

function importRowClassName({ row }: { row: CustomerImportRow }): string {
  if (row.errors && row.errors.length > 0) return 'bg-red-50'
  if (row.duplicate) return 'bg-yellow-50'
  return ''
}

async function handleImportConfirm() {
  if (!importPreview.value) return

  importLoading.value = true
  try {
    const result = await confirmCustomerImport(importPreview.value.rows)
    importResult.value = result
    importStep.value = 3
    emit('success', result)
  } catch {
    // Error handled by interceptor
  } finally {
    importLoading.value = false
  }
}

function resetImport() {
  importStep.value = 1
  importFile.value = null
  importPreview.value = null
  importResult.value = null
  globalDuplicateMode.value = ''
  importUploadRef.value?.clearFiles()
}
</script>
