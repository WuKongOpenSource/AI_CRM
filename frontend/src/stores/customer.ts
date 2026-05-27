import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  queryCustomerList,
  getCustomerDetail,
  addCustomer,
  updateCustomer,
  updateCustomerField,
  deleteCustomer,
  getCustomerStatistics
} from '@/api/customer'
import type {
  CustomerListVO,
  CustomerDetailVO,
  CustomerQueryBO,
  CustomerAddBO,
  CustomerUpdateBO,
  CustomerFieldUpdateBO,
  CustomerAiSearchParseVO
} from '@/types/customer'
import { normalizeTaskList } from '@/utils/taskPriority'

const DEFAULT_CUSTOMER_PAGE_SIZE = 10

function createDefaultCustomerQueryParams(): CustomerQueryBO {
  return {
    page: 1,
    limit: DEFAULT_CUSTOMER_PAGE_SIZE,
    keyword: '',
    stage: undefined,
    stages: undefined,
    level: undefined,
    industry: undefined,
    tag: undefined,
    source: undefined,
    quotationMin: undefined,
    quotationMax: undefined,
    lastContactStart: undefined,
    lastContactEnd: undefined,
    includeNoLastContact: undefined,
    nextFollowStart: undefined,
    nextFollowEnd: undefined,
    createTimeStart: undefined,
    createTimeEnd: undefined,
    contactCountMin: undefined,
    contactCountMax: undefined,
    sortBy: undefined,
    sortOrder: undefined
  }
}

export const useCustomerStore = defineStore('customer', () => {
  // State
  const customerList = ref<CustomerListVO[]>([])
  const currentCustomer = ref<CustomerDetailVO | null>(null)
  const totalCount = ref(0)
  const loading = ref(false)
  const statistics = ref<any>(null)
  const aiSearchState = ref<CustomerAiSearchParseVO | null>(null)

  // Query params
  const queryParams = ref<CustomerQueryBO>(createDefaultCustomerQueryParams())

  // Getters
  const hasMore = computed(() => {
    const totalPages = Math.ceil(totalCount.value / (queryParams.value.limit || 10))
    return (queryParams.value.page || 1) < totalPages
  })

  // Actions
  async function fetchCustomerList(
    reset = false,
    append = false,
    options: { silent?: boolean } = {}
  ) {
    if (reset) {
      queryParams.value.page = 1
      customerList.value = []
    }

    if (!options.silent) {
      loading.value = true
    }
    try {
      const result = await queryCustomerList(queryParams.value)
      if (append) {
        // For infinite scroll: append to existing list
        customerList.value = [...customerList.value, ...result.list]
      } else {
        // For pagination: replace with current page data
        customerList.value = result.list
      }
      totalCount.value = result.totalRow
    } finally {
      if (!options.silent) {
        loading.value = false
      }
    }
  }

  async function fetchCustomerDetail(customerId: string): Promise<CustomerDetailVO> {
    loading.value = true
    try {
      const detail = await getCustomerDetail(customerId)
      currentCustomer.value = {
        ...detail,
        tasks: normalizeTaskList(detail.tasks)
      }
      return currentCustomer.value
    } finally {
      loading.value = false
    }
  }

  async function createCustomer(data: CustomerAddBO): Promise<string> {
    return addCustomer(data)
  }

  async function editCustomer(data: CustomerUpdateBO): Promise<CustomerDetailVO | null> {
    await updateCustomer(data)
    if (currentCustomer.value?.customerId === data.customerId) {
      return fetchCustomerDetail(data.customerId)
    }
    return null
  }

  async function editCustomerField(data: CustomerFieldUpdateBO, options: { refreshList?: boolean } = {}): Promise<CustomerDetailVO> {
    const detail = await updateCustomerField(data)
    if (options.refreshList ?? true) {
      await fetchCustomerList(false, false, { silent: true })
    }
    if (currentCustomer.value?.customerId === data.customerId) {
      currentCustomer.value = {
        ...detail,
        tasks: normalizeTaskList(detail.tasks)
      }
    }
    return detail
  }

  async function removeCustomer(customerId: string): Promise<void> {
    await deleteCustomer(customerId)
    await fetchCustomerList(true)
    if (currentCustomer.value?.customerId === customerId) {
      currentCustomer.value = null
    }
  }

  async function fetchStatistics() {
    statistics.value = await getCustomerStatistics()
  }

  function setQueryParams(params: Partial<CustomerQueryBO>) {
    queryParams.value = { ...queryParams.value, ...params, page: 1 }
  }

  function replaceQueryParams(params: Partial<CustomerQueryBO>) {
    queryParams.value = {
      ...createDefaultCustomerQueryParams(),
      limit: queryParams.value.limit || DEFAULT_CUSTOMER_PAGE_SIZE,
      ...params,
      page: params.page ?? 1
    }
  }

  function resetQueryParams() {
    queryParams.value = {
      ...createDefaultCustomerQueryParams(),
      limit: queryParams.value.limit || DEFAULT_CUSTOMER_PAGE_SIZE
    }
  }

  function loadMore() {
    if (hasMore.value && !loading.value) {
      queryParams.value.page = (queryParams.value.page || 1) + 1
      fetchCustomerList(false, true)  // append=true for infinite scroll
    }
  }

  function clearCurrentCustomer() {
    currentCustomer.value = null
  }

  return {
    // State
    customerList,
    currentCustomer,
    totalCount,
    loading,
    statistics,
    aiSearchState,
    queryParams,
    // Getters
    hasMore,
    // Actions
    fetchCustomerList,
    fetchCustomerDetail,
    createCustomer,
    editCustomer,
    editCustomerField,
    removeCustomer,
    fetchStatistics,
    setQueryParams,
    replaceQueryParams,
    resetQueryParams,
    loadMore,
    clearCurrentCustomer
  }
})
