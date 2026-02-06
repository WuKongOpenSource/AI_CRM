<template>
  <div class="h-full flex flex-col bg-gray-50">
    <!-- Header -->
    <div class="h-14 px-4 flex items-center justify-between bg-white border-b border-gray-200">
      <div class="flex items-center">
        <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <span class="ml-4 font-medium text-lg">{{ customer?.companyName || '客户详情' }}</span>
      </div>
      <el-popconfirm
        v-if="customer"
        title="确定要删除此客户吗？删除后不可恢复。"
        confirm-button-text="删除"
        cancel-button-text="取消"
        confirm-button-type="danger"
        @confirm="handleDeleteCustomer"
      >
        <template #reference>
          <el-button type="danger" text :icon="Delete">删除客户</el-button>
        </template>
      </el-popconfirm>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>

    <div v-else-if="customer" class="flex-1 overflow-auto p-4">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <!-- Main Info -->
        <div class="lg:col-span-2 space-y-4">
          <!-- Basic Info Card -->
          <el-card shadow="never">
            <template #header>
              <div class="flex items-center justify-between">
                <span>基本信息</span>
                <el-button text type="primary" @click="handleEdit">编辑</el-button>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="公司名称">{{ customer.companyName }}</el-descriptions-item>
              <el-descriptions-item label="行业">{{ customer.industry || '-' }}</el-descriptions-item>
              <el-descriptions-item label="客户级别">
                <el-tag :type="getLevelType(customer.level)">{{ customer.level }}级</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="商机阶段">
                <el-tag :color="getStageColor(customer.stage)" effect="light">
                  {{ getStageLabel(customer.stage) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="地址">{{ customer.address || '-' }}</el-descriptions-item>
              <el-descriptions-item label="网站">{{ customer.website || '-' }}</el-descriptions-item>
            </el-descriptions>

            <!-- Tags -->
            <div class="mt-4">
              <span class="text-sm text-gray-500 mr-2">标签:</span>
              <el-tag
                v-for="tag in customer.tags"
                :key="tag.tagId"
                :color="tag.color"
                closable
                class="mr-2"
                @close="handleRemoveTag(tag)"
              >
                {{ tag.tagName }}
              </el-tag>
              <el-button text size="small" @click="showAddTagDialog = true">
                + 添加标签
              </el-button>
            </div>
          </el-card>

          <!-- Custom Fields Card -->
          <el-card v-if="customFields.length > 0" shadow="never">
            <template #header>
              <span>扩展信息</span>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item
                v-for="field in customFields"
                :key="field.fieldId"
                :label="field.fieldLabel"
              >
                {{ formatCustomFieldValue(field, customer.customFields?.[field.fieldName]) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- Contacts Card -->
          <el-card shadow="never" v-loading="contactLoading">
            <template #header>
              <div class="flex items-center justify-between">
                <span>联系人 <span class="text-sm text-gray-400 font-normal ml-1">({{ contactTotal }}人)</span></span>
                <el-button text type="primary" @click="handleAddContact">添加联系人</el-button>
              </div>
            </template>
            <div v-if="contacts.length === 0" class="text-center py-8 text-gray-400">
              暂无联系人
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="contact in contacts"
                :key="contact.contactId"
                class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
              >
                <div class="flex items-center">
                  <el-avatar :size="40" class="bg-primary-100 text-primary-600">
                    {{ contact.name?.charAt(0) }}
                  </el-avatar>
                  <div class="ml-3">
                    <div class="font-medium">
                      {{ contact.name }}
                      <el-tag v-if="contact.isPrimary" size="small" type="success" class="ml-2">主联系人</el-tag>
                    </div>
                    <div class="text-sm text-gray-500">
                      {{ contact.position || '' }}
                      <span v-if="contact.phone" class="ml-2">{{ contact.phone }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- Pagination -->
            <div v-if="contactTotal > contactPageSize" class="mt-4 flex justify-center">
              <el-pagination
                v-model:current-page="contactPage"
                :page-size="contactPageSize"
                :total="contactTotal"
                layout="prev, pager, next"
                small
                @current-change="handleContactPageChange"
              />
            </div>
          </el-card>

          <!-- Follow-ups Card -->
          <el-card shadow="never" v-loading="followUpLoading">
            <template #header>
              <div class="flex items-center justify-between">
                <span>跟进记录 <span class="text-sm text-gray-400 font-normal ml-1">({{ followUpTotal }}条)</span></span>
                <el-button text type="primary" @click="handleOpenFollowUpDialog">添加跟进</el-button>
              </div>
            </template>
            <el-timeline v-if="followUps.length > 0">
              <el-timeline-item
                v-for="item in followUps"
                :key="item.followUpId"
                :timestamp="formatDateTime(item.followTime || item.createTime)"
                placement="top"
              >
                <el-card shadow="never" class="!p-2">
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center">
                      <el-tag size="small">{{ getFollowUpTypeLabel(item.type) }}</el-tag>
                      <span class="ml-2 text-sm text-gray-500">{{ item.createUserName }}</span>
                    </div>
                    <el-popconfirm
                      title="确定删除这条跟进记录吗？"
                      confirm-button-text="删除"
                      cancel-button-text="取消"
                      @confirm="handleDeleteFollowUp(item.followUpId)"
                    >
                      <template #reference>
                        <el-button text type="danger" size="small">删除</el-button>
                      </template>
                    </el-popconfirm>
                  </div>
                  <p class="text-gray-700">{{ item.content }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <div v-else class="text-center py-8 text-gray-400">
              暂无跟进记录
            </div>
            <!-- Pagination -->
            <div v-if="followUpTotal > followUpPageSize" class="mt-4 flex justify-center">
              <el-pagination
                v-model:current-page="followUpPage"
                :page-size="followUpPageSize"
                :total="followUpTotal"
                layout="prev, pager, next"
                small
                @current-change="handleFollowUpPageChange"
              />
            </div>
          </el-card>
        </div>

        <!-- Sidebar -->
        <div class="space-y-4">
          <!-- Stage Progress -->
          <el-card shadow="never">
            <template #header>商机进度</template>
            <el-steps :active="getStageIndex(customer.stage)" direction="vertical" :space="50">
              <el-step title="线索" />
              <el-step title="已验证" />
              <el-step title="方案阶段" />
              <el-step title="商务谈判" />
              <el-step title="已成交" />
            </el-steps>
          </el-card>

          <!-- Related Tasks -->
          <el-card shadow="never">
            <template #header>
              <div class="flex items-center justify-between">
                <span>相关任务</span>
                <el-button text size="small" @click="handleAddTask">添加任务</el-button>
              </div>
            </template>
            <div v-if="!customer.tasks?.length" class="text-center py-4 text-gray-400">
              暂无相关任务
            </div>
            <div v-else class="space-y-2">
              <div
                v-for="task in customer.tasks?.slice(0, 5)"
                :key="task.taskId"
                class="p-2 bg-gray-50 rounded text-sm"
              >
                <div class="font-medium">{{ task.title }}</div>
                <div class="text-gray-500 text-xs mt-1">
                  {{ task.dueDate ? formatDate(task.dueDate) : '无截止日期' }}
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- Add Tag Dialog -->
    <el-dialog v-model="showAddTagDialog" title="添加标签" width="400px">
      <el-input v-model="newTagName" placeholder="请输入标签名称" />
      <template #footer>
        <el-button @click="showAddTagDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAddTag">添加</el-button>
      </template>
    </el-dialog>

    <!-- Add Follow-up Dialog -->
    <el-dialog v-model="showAddFollowUpDialog" title="添加跟进记录" width="500px">
      <el-form :model="followUpForm" label-width="80px">
        <el-form-item label="跟进类型">
          <el-select v-model="followUpForm.type" class="w-full">
            <el-option label="电话" value="call" />
            <el-option label="会议" value="meeting" />
            <el-option label="邮件" value="email" />
            <el-option label="拜访" value="visit" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进时间">
          <el-date-picker
            v-model="followUpForm.followTime"
            type="datetime"
            class="w-full"
            placeholder="选择跟进时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input v-model="followUpForm.content" type="textarea" :rows="4" placeholder="请输入跟进内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddFollowUpDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitFollowUp">添加</el-button>
      </template>
    </el-dialog>

    <!-- Add Contact Dialog -->
    <el-dialog v-model="showAddContactDialog" title="添加联系人" width="500px">
      <el-form :model="contactForm" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="contactForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="contactForm.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="contactForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="contactForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="主联系人">
          <el-switch v-model="contactForm.isPrimary" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddContactDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitContact">添加</el-button>
      </template>
    </el-dialog>

    <!-- Edit Customer Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑客户信息" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="公司名称" required>
          <el-input v-model="editForm.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="行业">
              <el-input v-model="editForm.industry" placeholder="请输入行业" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户级别">
              <el-select v-model="editForm.level" class="w-full">
                <el-option label="A级" value="A" />
                <el-option label="B级" value="B" />
                <el-option label="C级" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商机阶段">
              <el-select v-model="editForm.stage" class="w-full">
                <el-option label="线索" value="lead" />
                <el-option label="资格审查" value="qualified" />
                <el-option label="方案报价" value="proposal" />
                <el-option label="谈判中" value="negotiation" />
                <el-option label="已成交" value="closed" />
                <el-option label="已流失" value="lost" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报价金额">
              <el-input-number
                v-model="editForm.quotation"
                :min="0"
                :precision="2"
                :controls="false"
                class="w-full"
                placeholder="报价金额"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同金额">
              <el-input-number
                v-model="editForm.contractAmount"
                :min="0"
                :precision="2"
                :controls="false"
                class="w-full"
                placeholder="合同金额"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="回款金额">
              <el-input-number
                v-model="editForm.revenue"
                :min="0"
                :precision="2"
                :controls="false"
                class="w-full"
                placeholder="回款金额"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="网站">
          <el-input v-model="editForm.website" placeholder="请输入网站地址" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="editForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>

        <!-- Dynamic Custom Fields -->
        <DynamicFieldForm
          ref="editDynamicFieldFormRef"
          entity-type="customer"
          v-model="editCustomFieldValues"
          title="扩展信息"
        />
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { addCustomerTag, removeCustomerTag } from '@/api/customer'
import { addFollowUp, deleteFollowUp, queryFollowUpPageList } from '@/api/followup'
import { addContact, queryContactPageList } from '@/api/contact'
import { getEnabledFieldsByEntity } from '@/api/customField'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading, Delete } from '@element-plus/icons-vue'
import type { CustomerTag, FollowUp, CustomerUpdateBO, Contact } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()

const loading = ref(false)
const submitting = ref(false)
const showAddTagDialog = ref(false)
const showAddFollowUpDialog = ref(false)
const showAddContactDialog = ref(false)
const showEditDialog = ref(false)
const newTagName = ref('')
const followUps = ref<FollowUp[]>([])
const followUpTotal = ref(0)
const followUpPage = ref(1)
const followUpPageSize = ref(5)
const followUpLoading = ref(false)
const contacts = ref<Contact[]>([])
const contactTotal = ref(0)
const contactPage = ref(1)
const contactPageSize = ref(5)
const contactLoading = ref(false)
const customFields = ref<CustomField[]>([])
const editCustomFieldValues = ref<Record<string, any>>({})
const editDynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm>>()

// Edit form data
const editForm = reactive({
  customerId: '',
  companyName: '',
  industry: '',
  level: 'B',
  stage: 'lead',
  quotation: undefined as number | undefined,
  contractAmount: undefined as number | undefined,
  revenue: undefined as number | undefined,
  website: '',
  address: '',
  description: ''
})

// Helper function to format date for API (yyyy-MM-dd HH:mm:ss to match backend Jackson config)
function formatDateForApi(date: Date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const followUpForm = reactive({
  customerId: '',
  type: 'call',
  content: '',
  followTime: formatDateForApi()
})

const contactForm = reactive({
  customerId: '',
  name: '',
  position: '',
  phone: '',
  email: '',
  isPrimary: false
})

const customer = computed(() => customerStore.currentCustomer)

onMounted(async () => {
  const customerId = route.params.id as string
  if (customerId) {
    loading.value = true
    followUpForm.customerId = customerId
    contactForm.customerId = customerId

    // Fetch data independently to prevent cascade failures
    const fetchTasks = [
      // Fetch customer detail
      customerStore.fetchCustomerDetail(customerId).catch(err => {
        console.error('Failed to fetch customer detail:', err)
      }),
      // Fetch follow-ups with pagination
      fetchFollowUps(customerId),
      // Fetch contacts with pagination
      fetchContacts(customerId),
      // Fetch custom field definitions
      getEnabledFieldsByEntity('customer').then(data => {
        customFields.value = data
      }).catch(err => {
        console.error('Failed to fetch custom fields:', err)
        customFields.value = []
      })
    ]

    await Promise.all(fetchTasks)
    loading.value = false
  }
})

// Fetch follow-ups with pagination
async function fetchFollowUps(customerId: string, reset = false) {
  if (reset) {
    followUpPage.value = 1
  }
  followUpLoading.value = true
  try {
    const result = await queryFollowUpPageList({
      customerId,
      page: followUpPage.value,
      limit: followUpPageSize.value
    })
    followUps.value = result.list
    followUpTotal.value = result.totalRow
  } catch (err) {
    console.error('Failed to fetch follow-ups:', err)
    followUps.value = []
    followUpTotal.value = 0
  } finally {
    followUpLoading.value = false
  }
}

function handleFollowUpPageChange(page: number) {
  followUpPage.value = page
  if (customer.value) {
    fetchFollowUps(customer.value.customerId)
  }
}

// Fetch contacts with pagination
async function fetchContacts(customerId: string, reset = false) {
  if (reset) {
    contactPage.value = 1
  }
  contactLoading.value = true
  try {
    const result = await queryContactPageList({
      customerId,
      page: contactPage.value,
      limit: contactPageSize.value
    })
    contacts.value = result.list
    contactTotal.value = result.totalRow
  } catch (err) {
    console.error('Failed to fetch contacts:', err)
    contacts.value = []
    contactTotal.value = 0
  } finally {
    contactLoading.value = false
  }
}

function handleContactPageChange(page: number) {
  contactPage.value = page
  if (customer.value) {
    fetchContacts(customer.value.customerId)
  }
}

function handleEdit() {
  if (!customer.value) return

  // Fill form with current customer data
  editForm.customerId = customer.value.customerId
  editForm.companyName = customer.value.companyName || ''
  editForm.industry = customer.value.industry || ''
  editForm.level = customer.value.level || 'B'
  editForm.stage = customer.value.stage || 'lead'
  editForm.quotation = (customer.value as any).quotation
  editForm.contractAmount = (customer.value as any).contractAmount
  editForm.revenue = (customer.value as any).revenue
  editForm.website = customer.value.website || ''
  editForm.address = customer.value.address || ''
  editForm.description = customer.value.description || ''

  // Fill custom fields with current values
  editCustomFieldValues.value = customer.value.customFields ? { ...customer.value.customFields } : {}

  showEditDialog.value = true
}

async function handleSaveEdit() {
  if (!editForm.companyName.trim()) {
    ElMessage.warning('请输入公司名称')
    return
  }

  // Validate custom fields
  if (editDynamicFieldFormRef.value) {
    const missingFields = editDynamicFieldFormRef.value.getRequiredFieldLabels()
    if (missingFields.length > 0) {
      ElMessage.warning(`请填写必填字段: ${missingFields.join(', ')}`)
      return
    }
  }

  submitting.value = true
  try {
    const updateData: CustomerUpdateBO = {
      customerId: editForm.customerId,
      companyName: editForm.companyName,
      industry: editForm.industry || undefined,
      level: editForm.level as any,
      stage: editForm.stage as any,
      website: editForm.website || undefined,
      address: editForm.address || undefined,
      description: editForm.description || undefined,
      customFields: editCustomFieldValues.value
    }

    await customerStore.editCustomer(updateData)
    await customerStore.fetchCustomerDetail(editForm.customerId)
    showEditDialog.value = false
    ElMessage.success('客户信息更新成功')
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDeleteCustomer() {
  if (!customer.value) return

  try {
    await customerStore.removeCustomer(customer.value.customerId)
    ElMessage.success('客户已删除')
    router.push('/customer')
  } catch {
    // Error handled by interceptor
  }
}

function handleAddContact() {
  if (customer.value) {
    contactForm.customerId = customer.value.customerId
    showAddContactDialog.value = true
  }
}

function handleAddTask() {
  router.push('/task')
}

function handleOpenFollowUpDialog() {
  followUpForm.followTime = formatDateForApi()
  showAddFollowUpDialog.value = true
}

async function handleAddTag() {
  if (!newTagName.value.trim() || !customer.value) return

  submitting.value = true
  try {
    await addCustomerTag(customer.value.customerId, newTagName.value.trim())
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    showAddTagDialog.value = false
    newTagName.value = ''
    ElMessage.success('标签添加成功')
  } finally {
    submitting.value = false
  }
}

async function handleRemoveTag(tag: CustomerTag) {
  if (!customer.value) return

  try {
    await removeCustomerTag(customer.value.customerId, tag.tagId)
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    ElMessage.success('标签已删除')
  } catch {
    // Error handled by interceptor
  }
}

async function handleSubmitFollowUp() {
  if (!followUpForm.content.trim()) {
    ElMessage.warning('请输入跟进内容')
    return
  }
  if (!followUpForm.followTime) {
    ElMessage.warning('请选择跟进时间')
    return
  }

  submitting.value = true
  try {
    const submitData = {
      customerId: followUpForm.customerId,
      type: followUpForm.type,
      content: followUpForm.content,
      followTime: followUpForm.followTime
    }
    await addFollowUp(submitData as any)
    // Refresh with pagination, reset to first page
    await fetchFollowUps(followUpForm.customerId, true)
    showAddFollowUpDialog.value = false
    followUpForm.content = ''
    followUpForm.followTime = formatDateForApi()
    ElMessage.success('跟进记录添加成功')
  } catch {
    // Error already handled by request interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDeleteFollowUp(followUpId: string) {
  try {
    await deleteFollowUp(followUpId)
    // Refresh follow-ups list with pagination
    if (customer.value) {
      await fetchFollowUps(customer.value.customerId)
    }
    ElMessage.success('跟进记录已删除')
  } catch {
    // Error already handled by request interceptor
  }
}

async function handleSubmitContact() {
  if (!contactForm.name.trim()) {
    ElMessage.warning('请输入联系人姓名')
    return
  }

  submitting.value = true
  try {
    const submitData = {
      customerId: contactForm.customerId,
      name: contactForm.name,
      position: contactForm.position,
      phone: contactForm.phone,
      email: contactForm.email,
      isPrimary: contactForm.isPrimary ? 1 : 0
    }
    await addContact(submitData as any)
    // Refresh contacts list with pagination, reset to first page
    await fetchContacts(contactForm.customerId, true)
    showAddContactDialog.value = false
    // Reset form
    contactForm.name = ''
    contactForm.position = ''
    contactForm.phone = ''
    contactForm.email = ''
    contactForm.isPrimary = false
    ElMessage.success('联系人添加成功')
  } catch {
    // Error already handled by request interceptor
  } finally {
    submitting.value = false
  }
}

function getLevelType(level: string): 'success' | 'primary' | 'info' {
  switch (level) {
    case 'A': return 'success'
    case 'B': return 'primary'
    default: return 'info'
  }
}

function getStageLabel(stage: string): string {
  const labels: Record<string, string> = {
    lead: '线索', qualified: '已验证', proposal: '方案',
    negotiation: '谈判', closed: '成交', lost: '流失'
  }
  return labels[stage] || stage
}

function getStageColor(stage: string): string {
  const colors: Record<string, string> = {
    lead: '#6b7280', qualified: '#3b82f6', proposal: '#f59e0b',
    negotiation: '#8b5cf6', closed: '#22c55e', lost: '#ef4444'
  }
  return colors[stage] || '#6b7280'
}

function getStageIndex(stage: string): number {
  const stages = ['lead', 'qualified', 'proposal', 'negotiation', 'closed']
  return stages.indexOf(stage)
}

function getFollowUpTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    call: '电话', meeting: '会议', email: '邮件', visit: '拜访', other: '其他'
  }
  return labels[type] || type
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

function formatCustomFieldValue(field: CustomField, value: any): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  switch (field.fieldType) {
    case 'checkbox':
      return value ? '是' : '否'
    case 'select':
      // Find label from options
      const option = field.options?.find(opt => opt.value === value)
      return option?.label || String(value)
    case 'multiselect':
      // Value is an array, find labels
      if (Array.isArray(value)) {
        return value
          .map(v => field.options?.find(opt => opt.value === v)?.label || v)
          .join(', ') || '-'
      }
      return String(value)
    case 'date':
      return formatDate(value)
    case 'datetime':
      return formatDateTime(value)
    default:
      return String(value)
  }
}
</script>
