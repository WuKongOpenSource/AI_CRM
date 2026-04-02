<template>
  <div class="max-w-4xl mx-auto space-y-8">
    <section class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <h3 class="text-base font-bold mb-6 flex items-center gap-2">
        <span class="w-1 h-4 bg-primary rounded-full"></span>
        企业信息
        <el-tag v-if="enterpriseForm.updateTime" size="small" type="info" class="ml-auto font-normal">
          最后更新: {{ enterpriseForm.updateTime }}
        </el-tag>
      </h3>

      <div class="mb-6">
        <label class="block text-sm font-medium text-slate-700 mb-2">企业 Logo</label>
        <div class="flex items-center gap-6">
          <div
            class="size-20 rounded-xl border-2 border-dashed border-slate-300 flex items-center justify-center cursor-pointer hover:border-primary hover:bg-primary/5 transition-all overflow-hidden"
            :class="{ '!border-solid !border-slate-200': enterpriseForm.logoUrl }"
            @click="triggerLogoUpload"
          >
            <img
              v-if="enterpriseForm.logoUrl"
              :src="enterpriseForm.logoUrl"
              class="w-full h-full object-cover"
              alt="企业Logo"
            />
            <div v-else class="text-center">
              <el-icon :size="24" class="text-slate-400"><Upload /></el-icon>
              <p class="text-xs text-slate-400 mt-1">上传Logo</p>
            </div>
          </div>
          <div class="text-xs text-slate-500">
            <p>建议尺寸: 200x200px</p>
            <p>支持 JPG、PNG 格式</p>
            <el-button
              v-if="enterpriseForm.logoUrl"
              text
              type="danger"
              size="small"
              @click="removeLogo"
            >
              移除 Logo
            </el-button>
          </div>
        </div>
        <input
          ref="logoInputRef"
          type="file"
          accept="image/jpeg,image/png,image/jpg"
          class="hidden"
          @change="handleLogoChange"
        />
      </div>

      <el-form label-position="top">
        <el-form-item label="企业名称">
          <el-input
            v-model="enterpriseForm.name"
            placeholder="请输入企业名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="企业说明">
          <el-input
            v-model="enterpriseForm.description"
            placeholder="请输入企业说明（显示在侧边栏企业名称下方）"
            maxlength="50"
            show-word-limit
          />
          <div class="text-xs text-slate-400 mt-1">侧边栏最多显示12个字符，超出部分将以省略号显示</div>
        </el-form-item>
      </el-form>

      <div class="flex justify-end pt-4 border-t border-slate-100">
        <el-button type="primary" :loading="savingEnterprise" @click="saveEnterpriseConfig">
          保存设置
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { getEnterpriseConfig, updateEnterpriseConfig } from '@/api/systemConfig'
import { useEnterpriseStore } from '@/stores/enterprise'
import { isRequestErrorHandled } from '@/utils/requestError'

const enterpriseStore = useEnterpriseStore()

const savingEnterprise = ref(false)
const logoInputRef = ref<HTMLInputElement | null>(null)
const enterpriseForm = reactive({
  name: '',
  logo: '',
  logoUrl: '',
  description: '',
  updateTime: ''
})

onMounted(async () => {
  await loadEnterpriseConfig()
})

async function loadEnterpriseConfig() {
  try {
    const config = await getEnterpriseConfig()
    Object.assign(enterpriseForm, {
      name: config.name || '',
      logo: config.logo || '',
      logoUrl: config.logoUrl || '',
      description: config.description || '',
      updateTime: config.updateTime || ''
    })
  } catch {
    // Error handled by interceptor
  }
}

function triggerLogoUpload() {
  logoInputRef.value?.click()
}

async function handleLogoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG 格式')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  try {
    const presigned = await getPresignedUploadUrl(file.name, file.type)
    await uploadToMinIO(file, presigned.uploadUrl)
    enterpriseForm.logo = presigned.objectKey
    enterpriseForm.logoUrl = presigned.accessUrl
    ElMessage.success('Logo 上传成功')
  } catch (error) {
    console.error('Logo upload failed:', error)
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('Logo 上传失败')
    }
  } finally {
    input.value = ''
  }
}

function removeLogo() {
  enterpriseForm.logo = ''
  enterpriseForm.logoUrl = ''
}

async function saveEnterpriseConfig() {
  savingEnterprise.value = true
  try {
    await updateEnterpriseConfig({
      name: enterpriseForm.name,
      logo: enterpriseForm.logo,
      description: enterpriseForm.description
    })
    ElMessage.success('企业信息保存成功')
    enterpriseStore.updateLocal({
      name: enterpriseForm.name || null,
      logo: enterpriseForm.logo || null,
      logoUrl: enterpriseForm.logoUrl || null,
      description: enterpriseForm.description || null
    })
    await loadEnterpriseConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    savingEnterprise.value = false
  }
}
</script>
