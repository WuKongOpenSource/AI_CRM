import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  queryCustomerList,
  getCustomerDetail,
  addCustomer,
  updateCustomer,
  deleteCustomer,
  getCustomerStatistics
} from '@/api/customer'
import type { CustomerListVO, CustomerDetailVO, CustomerQueryBO, CustomerAddBO, CustomerUpdateBO } from '@/types/customer'

export const useCustomerStore = defineStore('customer', () => {
  // State
  const customerList = ref<CustomerListVO[]>([])
  const currentCustomer = ref<CustomerDetailVO | null>(null)
  const totalCount = ref(0)
  const loading = ref(false)
  const statistics = ref<any>(null)

  // Query params
  const queryParams = ref<CustomerQueryBO>({
    page: 1,
    limit: 10,
    keyword: '',
    stage: undefined,
    level: undefined
  })

  // Getters
  const hasMore = computed(() => {
    const totalPages = Math.ceil(totalCount.value / (queryParams.value.limit || 10))
    return (queryParams.value.page || 1) < totalPages
  })

  // Actions
  async function fetchCustomerList(reset = false, append = false) {
    if (reset) {
      queryParams.value.page = 1
      customerList.value = []
    }

    loading.value = true
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
      loading.value = false
    }
  }

  async function fetchCustomerDetail(customerId: string) {
    loading.value = true
    try {
      currentCustomer.value = await getCustomerDetail(customerId)
    } finally {
      loading.value = false
    }
  }

  async function createCustomer(data: CustomerAddBO): Promise<string> {
    const customerId = await addCustomer(data)
    await fetchCustomerList(true)
    return customerId
  }

  async function editCustomer(data: CustomerUpdateBO): Promise<void> {
    await updateCustomer(data)
    await fetchCustomerList(true)
    if (currentCustomer.value?.customerId === data.customerId) {
      await fetchCustomerDetail(data.customerId)
    }
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
    queryParams,
    // Getters
    hasMore,
    // Actions
    fetchCustomerList,
    fetchCustomerDetail,
    createCustomer,
    editCustomer,
    removeCustomer,
    fetchStatistics,
    setQueryParams,
    loadMore,
    clearCurrentCustomer
  }
})
