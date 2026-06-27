<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingCandidate ? '编辑候选人' : '新增候选人'"
    width="720px"
    destroy-on-close
    @close="emit('update:modelValue', false)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="candidate-form">
      <el-form-item label="头像">
        <CustomerLogoUploader
          :logo-url="avatarUrl"
          :alt="form.name || '候选人头像'"
          label="头像"
          :disabled="saving"
          :size="72"
          @uploaded="handleAvatarUploaded"
          @removed="handleAvatarRemoved"
        />
      </el-form-item>
      <div class="grid grid-cols-1 gap-x-4 md:grid-cols-2">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="候选人姓名" />
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="form.stage" class="w-full">
            <el-option v-for="option in stageOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="微信">
          <el-input v-model="form.wechat" placeholder="微信号" />
        </el-form-item>
        <el-form-item label="招聘岗位">
          <div class="flex w-full gap-2">
            <el-select
              v-model="form.recruitmentJobId"
              class="min-w-0 flex-1"
              filterable
              clearable
              placeholder="选择招聘岗位"
            >
              <el-option
                v-for="job in recruitmentJobs"
                :key="job.recruitmentJobId"
                :label="job.jobName"
                :value="String(job.recruitmentJobId)"
              />
            </el-select>
            <el-button :icon="Plus" @click="jobDialogVisible = true" />
          </div>
        </el-form-item>
        <el-form-item label="当前公司">
          <el-input v-model="form.currentCompany" placeholder="当前公司" />
        </el-form-item>
        <el-form-item label="当前职位">
          <el-input v-model="form.currentPosition" placeholder="当前职位" />
        </el-form-item>
        <el-form-item label="学历">
          <el-input v-model="form.education" placeholder="学历" />
        </el-form-item>
        <el-form-item label="学校">
          <el-input v-model="form.school" placeholder="学校" />
        </el-form-item>
        <el-form-item label="期望城市">
          <el-input v-model="form.expectedCity" placeholder="期望城市" />
        </el-form-item>
        <el-form-item label="期望薪资">
          <el-input v-model="form.expectedSalary" placeholder="期望薪资" />
        </el-form-item>
      </div>
      <el-form-item label="技能标签">
        <el-input v-model="form.skillTags" placeholder="多个标签用逗号分隔" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="补充备注" />
      </el-form-item>
    </el-form>

    <RecruitmentJobUpsertDialog
      v-model="jobDialogVisible"
      @saved="handleRecruitmentJobSaved"
    />

    <template #footer>
      <div class="flex justify-end gap-2">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { addCandidate, updateCandidate } from '@/api/candidate'
import { listRecruitmentJobOptions } from '@/api/recruitmentJob'
import { useEnumStore } from '@/stores/enums'
import type { CandidateAddBO, CandidateListVO } from '@/types/candidate'
import { CANDIDATE_STAGE_OPTIONS } from '@/types/candidate'
import type { RecruitmentJobVO } from '@/types/recruitmentJob'
import CustomerLogoUploader from '@/views/customer/components/CustomerLogoUploader.vue'
import RecruitmentJobUpsertDialog from './RecruitmentJobUpsertDialog.vue'

const props = defineProps<{
  modelValue: boolean
  editingCandidate?: CandidateListVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const jobDialogVisible = ref(false)
const recruitmentJobs = ref<RecruitmentJobVO[]>([])
const avatarUrl = ref('')
const enumStore = useEnumStore()
const stageOptions = computed(() => enumStore.candidateStage.length ? enumStore.candidateStage : CANDIDATE_STAGE_OPTIONS)
const form = reactive<CandidateAddBO>({
  name: '',
  avatar: '',
  stage: 'new'
})

onMounted(() => {
  void enumStore.ensureCandidateStage()
  void loadRecruitmentJobs()
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }]
}

watch(
  () => [props.modelValue, props.editingCandidate] as const,
  () => {
    if (!props.modelValue) return
    const candidate = props.editingCandidate
    Object.assign(form, {
      name: candidate?.name || '',
      avatar: candidate?.avatar || '',
      phone: candidate?.phone || '',
      email: candidate?.email || '',
      wechat: candidate?.wechat || '',
      currentCompany: candidate?.currentCompany || '',
      currentPosition: candidate?.currentPosition || '',
      recruitmentJobId: candidate?.recruitmentJobId ? String(candidate.recruitmentJobId) : undefined,
      stage: candidate?.stage || 'new',
      source: candidate?.source || 'manual',
      education: candidate?.education || '',
      school: candidate?.school || '',
      expectedCity: candidate?.expectedCity || '',
      expectedSalary: candidate?.expectedSalary || '',
      skillTags: candidate?.skillTags || '',
      remark: candidate?.remark || ''
    })
    avatarUrl.value = candidate?.avatarUrl || ''
  },
  { immediate: true }
)

async function loadRecruitmentJobs() {
  recruitmentJobs.value = await listRecruitmentJobOptions({ limit: 100 })
}

async function handleRecruitmentJobSaved(jobId?: string) {
  await loadRecruitmentJobs()
  if (jobId) {
    form.recruitmentJobId = String(jobId)
  }
}

function handleAvatarUploaded(payload: { logo: string; logoUrl: string }) {
  form.avatar = payload.logo
  avatarUrl.value = payload.logoUrl
}

function handleAvatarRemoved() {
  form.avatar = ''
  avatarUrl.value = ''
}

async function handleSubmit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (props.editingCandidate?.candidateId) {
      await updateCandidate({ ...form, candidateId: String(props.editingCandidate.candidateId) })
      ElMessage.success('候选人已更新')
    } else {
      await addCandidate(form)
      ElMessage.success('候选人已创建')
    }
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.candidate-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}
</style>
