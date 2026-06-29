<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingJob ? '编辑招聘岗位' : '新增招聘岗位'"
    width="720px"
    destroy-on-close
    @close="emit('update:modelValue', false)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="recruitment-job-form">
      <div class="grid grid-cols-1 gap-x-4 md:grid-cols-2">
        <el-form-item label="岗位名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="例如：Java开发工程师" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="w-full">
            <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.department" placeholder="例如：研发部" />
        </el-form-item>
        <el-form-item label="招聘人数">
          <el-input-number v-model="form.headcount" :min="0" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="工作年限">
          <el-input-number v-model="form.workYears" :min="0" :precision="1" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="学历要求">
          <el-input v-model="form.education" placeholder="例如：本科及以上" />
        </el-form-item>
        <el-form-item label="工作城市">
          <el-input v-model="form.city" placeholder="例如：上海" />
        </el-form-item>
        <el-form-item label="薪资范围">
          <el-input v-model="form.salaryRange" placeholder="例如：20k-35k" />
        </el-form-item>
      </div>
      <el-form-item label="技能标签">
        <el-input v-model="form.skillTags" placeholder="多个标签用逗号分隔" />
      </el-form-item>
      <el-form-item label="岗位职责">
        <el-input v-model="form.responsibilities" type="textarea" :rows="3" placeholder="这个岗位主要负责什么" />
      </el-form-item>
      <el-form-item label="任职要求">
        <el-input v-model="form.requirements" type="textarea" :rows="4" placeholder="经验、能力、技术栈、软素质要求" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="补充招聘偏好、面试关注点等" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { addRecruitmentJob, updateRecruitmentJob } from '@/api/recruitmentJob'
import type { RecruitmentJobAddBO, RecruitmentJobVO } from '@/types/recruitmentJob'
import { RECRUITMENT_JOB_STATUS_OPTIONS } from '@/types/recruitmentJob'

const props = defineProps<{
  modelValue: boolean
  editingJob?: RecruitmentJobVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved', jobId?: string): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const statusOptions = RECRUITMENT_JOB_STATUS_OPTIONS
const form = reactive<RecruitmentJobAddBO>({
  jobName: '',
  status: 'open',
  headcount: null,
  workYears: null
})

const rules: FormRules = {
  jobName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
}

watch(
  () => [props.modelValue, props.editingJob] as const,
  () => {
    if (!props.modelValue) return
    const job = props.editingJob
    Object.assign(form, {
      jobName: job?.jobName || '',
      department: job?.department || '',
      headcount: job?.headcount ?? null,
      workYears: job?.workYears ?? null,
      education: job?.education || '',
      city: job?.city || '',
      salaryRange: job?.salaryRange || '',
      skillTags: job?.skillTags || '',
      responsibilities: job?.responsibilities || '',
      requirements: job?.requirements || '',
      status: job?.status || 'open',
      remark: job?.remark || ''
    })
  },
  { immediate: true }
)

async function handleSubmit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    let savedJobId = props.editingJob?.recruitmentJobId
    if (props.editingJob?.recruitmentJobId) {
      await updateRecruitmentJob({ ...form, recruitmentJobId: String(props.editingJob.recruitmentJobId) })
      ElMessage.success('招聘岗位已更新')
    } else {
      savedJobId = await addRecruitmentJob(form)
      ElMessage.success('招聘岗位已创建')
    }
    emit('update:modelValue', false)
    emit('saved', savedJobId)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.recruitment-job-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}
</style>
