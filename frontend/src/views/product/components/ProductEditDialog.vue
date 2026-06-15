<template>
  <el-dialog
    v-model="visible"
    :width="isMobile ? 'calc(100% - 24px)' : '680px'"
    :show-close="false"
    destroy-on-close
    :class="[
      'wk-dialog--flush wk-product-dialog wk-crm-el-field-scope',
      isMobile ? 'wk-product-dialog--mobile' : 'wk-product-dialog--desktop'
    ]"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-[22px]">inventory_2</span>
          </div>
          <div class="min-w-0">
            <h2 class="truncate text-lg font-bold text-slate-900">编辑产品</h2>
            <p class="mt-0.5 text-xs text-slate-500">维护产品资料、类目、价格和负责人。</p>
          </div>
        </div>
        <button
          type="button"
          class="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
          aria-label="关闭"
          @click="visible = false"
        >
          <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
      </div>
    </template>

    <div class="bg-white px-5 pb-6 pt-5 md:px-6 md:pb-7">
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        class="wk-product-form grid grid-cols-1 gap-4 md:grid-cols-2"
      >
        <el-form-item label="产品名称" prop="productName" class="md:col-span-2">
          <el-input v-model="form.productName" maxlength="100" show-word-limit size="large" class="wk-crm-el-field-input" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode" class="md:col-span-2">
          <el-input v-model="form.productCode" maxlength="100" placeholder="系统内唯一" size="large" class="wk-crm-el-field-input" />
        </el-form-item>
        <el-form-item label="产品主图" class="md:col-span-2">
          <div class="product-main-image-field">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :on-change="handleMainImageChange"
              :disabled="mainImageUploading"
            >
              <button type="button" class="product-main-image-uploader">
                <img v-if="form.mainImageUrl" :src="form.mainImageUrl" alt="产品主图" class="size-full object-cover" />
                <span v-else-if="mainImageUploading" class="material-symbols-outlined animate-spin text-[24px]">progress_activity</span>
                <span v-else class="material-symbols-outlined text-[24px]">add_photo_alternate</span>
              </button>
            </el-upload>
            <div class="min-w-0">
              <p class="text-sm font-semibold text-slate-700">{{ form.mainImage ? '已上传产品主图' : '上传产品主图' }}</p>
              <p class="mt-1 text-xs leading-5 text-slate-400">仅支持一张图片，重新上传会替换当前主图。</p>
              <button
                v-if="form.mainImage"
                type="button"
                class="mt-2 text-xs font-semibold text-rose-500 transition-colors hover:text-rose-600"
                @click="removeMainImage"
              >
                移除图片
              </button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="产品类目" class="md:col-span-2">
          <el-tree-select
            v-model="form.categoryId"
            :data="categories"
            node-key="categoryId"
            check-strictly
            clearable
            default-expand-all
            :props="{ label: 'categoryName', children: 'children', value: 'categoryId' }"
            placeholder="请选择产品类目"
            size="large"
            class="wk-crm-el-field-select w-full"
          />
        </el-form-item>
        <el-form-item label="产品类型">
          <el-select v-model="form.productType" clearable placeholder="请选择产品类型" size="large" class="wk-crm-el-field-select w-full">
            <el-option v-for="type in productTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-select v-model="form.unit" clearable placeholder="请选择单位" size="large" class="wk-crm-el-field-select w-full">
            <el-option v-for="unit in unitOptions" :key="unit.value" :label="unit.label" :value="unit.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准价">
          <el-input-number v-model="form.standardPrice" :min="0" :precision="2" size="large" class="wk-crm-el-field-input w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="form.costPrice" :min="0" :precision="2" size="large" class="wk-crm-el-field-input w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="负责人" class="md:col-span-2">
          <el-select
            v-model="form.ownerId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="默认当前用户"
            :remote-method="loadUserOptions"
            :loading="userLoading"
            size="large"
            class="wk-crm-el-field-select w-full"
          >
            <el-option v-for="user in userOptions" :key="user.userId" :label="userLabel(user)" :value="String(user.userId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" class="md:col-span-2">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit resize="none" class="wk-crm-el-field-input" placeholder="补充产品说明" />
        </el-form-item>
        <DynamicFieldForm
          v-model="customFieldValues"
          entity-type="product"
          mode="custom"
          :entity-id="product?.productId || null"
          class="grid grid-cols-1 gap-4 md:col-span-2 md:grid-cols-2"
        />
      </el-form>
    </div>

    <template #footer>
      <div class="flex gap-3">
        <button
          type="button"
          class="flex-1 rounded-xl bg-slate-100 py-2.5 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-200"
          @click="visible = false"
        >
          取消
        </button>
        <button
          type="button"
          class="flex-1 rounded-xl bg-primary py-2.5 text-sm font-bold text-white shadow-sm transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="saving || !form.productName.trim() || (settingsForm.codeRequired && !form.productCode.trim())"
          @click="submitForm"
        >
          {{ saving ? '提交中...' : '保存修改' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { queryUserList } from '@/api/auth'
import { getFormFieldsByEntity } from '@/api/customField'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { getProductCategoryTree, getProductDetail, getProductSettings, updateProduct } from '@/api/product'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'
import { formatProductTypeLabel } from '@/utils/productDisplay'
import type { FieldOption } from '@/types/customField'
import type { ProductCategoryVO, ProductVO } from '@/types/product'

interface UserOption {
  userId: string | number
  realname?: string
  username?: string
  status?: number
}

interface ProductTypeOption {
  value: string
  label: string
}

const props = defineProps<{
  modelValue: boolean
  product: ProductVO | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [product: ProductVO]
}>()

const { isMobile } = useResponsive()
const userStore = useUserStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()
const saving = ref(false)
const mainImageUploading = ref(false)
const categories = ref<ProductCategoryVO[]>([])
const userLoading = ref(false)
const userOptions = ref<UserOption[]>([])
const DEFAULT_PRODUCT_TYPE_OPTIONS: ProductTypeOption[] = [
  { value: 'goods', label: '商品' },
  { value: 'service', label: '服务' }
]
const productTypeOptions = ref<ProductTypeOption[]>([...DEFAULT_PRODUCT_TYPE_OPTIONS])
const DEFAULT_UNIT_OPTIONS: ProductTypeOption[] = [
  { value: '个', label: '个' },
  { value: '套', label: '套' },
  { value: '台', label: '台' },
  { value: '件', label: '件' },
  { value: '年', label: '年' },
  { value: '月', label: '月' },
  { value: '次', label: '次' }
]
const unitOptions = ref<ProductTypeOption[]>([...DEFAULT_UNIT_OPTIONS])
const settingsForm = reactive({ codeRequired: true })
const customFieldValues = ref<Record<string, unknown>>({})

const form = reactive({
  productName: '',
  productCode: '',
  mainImage: '',
  mainImageUrl: '',
  categoryId: '',
  productType: '',
  unit: '',
  standardPrice: undefined as number | undefined,
  costPrice: undefined as number | undefined,
  ownerId: '',
  description: ''
})

const formRules = computed<FormRules>(() => ({
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productCode: settingsForm.codeRequired
    ? [{ required: true, message: '请输入产品编码', trigger: 'blur' }]
    : []
}))

function userLabel(user: UserOption): string {
  return user.realname || user.username || String(user.userId)
}

function normalizeProductTypeOption(option: Partial<FieldOption> | ProductTypeOption | null | undefined): ProductTypeOption | null {
  const value = String(option?.value ?? '').trim()
  if (!value) return null
  return {
    value,
    label: String(option?.label || formatProductTypeLabel(value)).trim()
  }
}

function mergeProductTypeOptions(options: ProductTypeOption[]) {
  const merged = new Map<string, ProductTypeOption>()
  productTypeOptions.value.forEach(option => {
    merged.set(option.value, option)
  })
  options.forEach(option => {
    const normalized = normalizeProductTypeOption(option)
    if (normalized) {
      merged.set(normalized.value, normalized)
    }
  })
  productTypeOptions.value = Array.from(merged.values())
}

function normalizeUnitOption(option: Partial<FieldOption> | ProductTypeOption | null | undefined): ProductTypeOption | null {
  const value = String(option?.value ?? '').trim()
  if (!value) return null
  return {
    value,
    label: String(option?.label || value).trim()
  }
}

function mergeUnitOptions(options: ProductTypeOption[]) {
  const merged = new Map<string, ProductTypeOption>()
  unitOptions.value.forEach(option => {
    merged.set(option.value, option)
  })
  options.forEach(option => {
    const normalized = normalizeUnitOption(option)
    if (normalized) {
      merged.set(normalized.value, normalized)
    }
  })
  unitOptions.value = Array.from(merged.values())
}

function resetForm(product: ProductVO | null) {
  form.productName = product?.productName || ''
  form.productCode = product?.productCode || ''
  form.mainImage = product?.mainImage || ''
  form.mainImageUrl = product?.mainImageUrl || ''
  form.categoryId = product?.categoryId ? String(product.categoryId) : ''
  form.productType = product?.productType || ''
  form.unit = product?.unit || ''
  form.standardPrice = product?.standardPrice === undefined || product?.standardPrice === '' ? undefined : Number(product.standardPrice)
  form.costPrice = product?.costPrice === undefined || product?.costPrice === '' ? undefined : Number(product.costPrice)
  form.ownerId = product?.ownerId ? String(product.ownerId) : ''
  form.description = product?.description || ''
  customFieldValues.value = { ...(product?.customFields || {}) }
  if (product?.productType) {
    mergeProductTypeOptions([{ value: product.productType, label: formatProductTypeLabel(product.productType) }])
  }
  if (product?.unit) {
    mergeUnitOptions([{ value: product.unit, label: product.unit }])
  }
}

function ensureOwnerOption(product: ProductVO | null) {
  if (!product?.ownerId) return
  const ownerId = String(product.ownerId)
  if (userOptions.value.some(user => String(user.userId) === ownerId)) return
  userOptions.value.unshift({
    userId: ownerId,
    realname: product.ownerName || undefined
  })
}

async function loadCategories() {
  categories.value = await getProductCategoryTree()
}

async function loadProductTypeOptions() {
  try {
    const fields = await getFormFieldsByEntity('product')
    const productTypeField = fields.find(field => field.fieldName === 'productType' || field.fieldLabel === '产品类型')
    const unitField = fields.find(field => field.fieldName === 'unit' || field.fieldLabel === '单位')
    mergeProductTypeOptions((productTypeField?.options || [])
      .map(option => normalizeProductTypeOption(option))
      .filter((item): item is ProductTypeOption => Boolean(item)))
    mergeUnitOptions((unitField?.options || [])
      .map(option => normalizeUnitOption(option))
      .filter((item): item is ProductTypeOption => Boolean(item)))
  } catch {
    // Default options keep the dialog usable if field config is unavailable.
  }
}

async function loadSettingsSafe() {
  if (!userStore.hasPermission('product:settings')) return
  try {
    const settings = await getProductSettings()
    settingsForm.codeRequired = settings.codeRequired
  } catch {
    // Defaults still match the product module validation when settings are unavailable.
  }
}

async function loadUserOptions(search = '') {
  userLoading.value = true
  try {
    const result = await queryUserList({ search, page: 1, limit: 30 })
    userOptions.value = (result.list || []).filter((user: UserOption) => user.status === undefined || user.status === 1)
    ensureOwnerOption(props.product)
  } finally {
    userLoading.value = false
  }
}

async function handleMainImageChange(uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }
  mainImageUploading.value = true
  try {
    const presigned = await getPresignedUploadUrl(raw.name, raw.type, 'product/main-images')
    await uploadToMinIO(raw, presigned.uploadUrl)
    form.mainImage = presigned.objectKey
    form.mainImageUrl = presigned.accessUrl
  } finally {
    mainImageUploading.value = false
  }
}

function removeMainImage() {
  form.mainImage = ''
  form.mainImageUrl = ''
}

async function submitForm() {
  if (!props.product?.productId) return
  await formRef.value?.validate()
  saving.value = true
  try {
    await updateProduct({
      productId: String(props.product.productId),
      productName: form.productName.trim(),
      productCode: form.productCode.trim() || undefined,
      mainImage: form.mainImage,
      categoryId: form.categoryId || undefined,
      productType: form.productType.trim() || undefined,
      unit: form.unit.trim() || undefined,
      standardPrice: form.standardPrice,
      costPrice: form.costPrice,
      ownerId: form.ownerId || undefined,
      description: form.description.trim() || undefined,
      customFields: customFieldValues.value
    })
    const updated = await getProductDetail(String(props.product.productId))
    ElMessage.success('产品已更新')
    emit('saved', updated)
    visible.value = false
  } finally {
    saving.value = false
  }
}

watch(
  () => [props.modelValue, props.product?.productId] as const,
  async ([isVisible]) => {
    if (!isVisible) return
    resetForm(props.product)
    await Promise.all([loadCategories(), loadProductTypeOptions(), loadSettingsSafe(), loadUserOptions('')])
    ensureOwnerOption(props.product)
  }
)
</script>

<style scoped>
.product-main-image-field {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 14px;
}

.product-main-image-uploader {
  display: inline-flex;
  width: 88px;
  height: 88px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed rgb(203 213 225);
  border-radius: 12px;
  background: rgb(248 250 252);
  color: rgb(100 116 139);
  transition: border-color 0.15s ease, background-color 0.15s ease, color 0.15s ease;
}

.product-main-image-uploader:hover {
  border-color: rgb(15 23 42);
  background: #fff;
  color: rgb(15 23 42);
}

:global(.wk-product-dialog.el-dialog) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:global(.wk-product-dialog.el-dialog .el-dialog__header) {
  flex-shrink: 0;
  margin-right: 0;
  padding: 22px 24px 14px !important;
}

:global(.wk-product-dialog.el-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 !important;
}

:global(.wk-product-dialog.el-dialog .el-dialog__footer) {
  flex-shrink: 0;
  padding: 14px 24px 22px !important;
}

:global(.wk-product-dialog--desktop.el-dialog) {
  max-height: calc(100vh - 20vh);
  margin-bottom: 10vh;
}

:global(.wk-product-dialog--mobile.el-dialog) {
  height: calc(100dvh - 32px);
  max-height: calc(100dvh - 32px);
  margin: 16px auto !important;
  border-radius: 1rem !important;
}

:global(.wk-product-dialog .wk-product-form .el-form-item) {
  margin-bottom: 0;
}

:global(.wk-product-dialog .wk-product-form .el-form-item__label) {
  margin-bottom: 6px;
  color: rgb(100 116 139);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.25;
}

:global(.wk-product-dialog .el-input-number .el-input__wrapper) {
  width: 100%;
}

:global(.el-overlay:has(.wk-product-dialog)),
:global(.el-overlay-dialog:has(.wk-product-dialog)) {
  overflow: hidden;
}
</style>
