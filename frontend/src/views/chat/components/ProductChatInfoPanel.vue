<template>
  <div class="wk-product-chat-info-panel h-full overflow-y-auto px-5 pb-5 pt-4">
    <section class="border-b border-[var(--wk-border-subtle)] pb-4">
      <div class="flex items-start gap-3">
        <div class="flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-[#f5f5f5] text-[#8f8f8f]">
          <img v-if="product.mainImageUrl" :src="product.mainImageUrl" :alt="product.productName || '产品主图'" class="size-full object-cover" />
          <span v-else class="material-symbols-outlined">inventory_2</span>
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex min-w-0 items-center gap-2">
            <h3 class="min-w-0 truncate text-base font-bold text-slate-900">{{ product.productName || '未命名产品' }}</h3>
            <el-tag :type="product.status === 'active' ? 'success' : 'info'" size="small" class="shrink-0">
              {{ product.status === 'active' ? '启用' : '停用' }}
            </el-tag>
          </div>
          <p class="mt-1 truncate text-sm text-slate-500">{{ product.productCode || '无编码' }}</p>
        </div>
        <button
          type="button"
          class="wk-product-panel-edit-btn"
          title="编辑产品"
          aria-label="编辑产品"
          @click="$emit('edit', product)"
        >
          <span class="material-symbols-outlined text-[18px]">edit</span>
        </button>
      </div>
    </section>

    <section class="mt-4 space-y-2 text-sm">
      <div
        v-for="item in basicInfoRows"
        :key="item.label"
        class="wk-product-info-row"
      >
        <span class="wk-product-info-row__label">{{ item.label }}</span>
        <span class="wk-product-info-row__value">{{ item.value }}</span>
      </div>
    </section>

    <section class="mt-5">
      <h4 class="mb-2 text-sm font-bold text-slate-900">描述</h4>
      <p class="min-h-20 whitespace-pre-wrap rounded-lg border border-[var(--wk-border-subtle)] bg-[var(--wk-bg-surface-subtle)] p-3 text-sm leading-6 text-slate-600">
        {{ product.description || '暂无描述' }}
      </p>
    </section>

    <section v-if="customFieldEntries.length" class="mt-5">
      <h4 class="mb-2 text-sm font-bold text-slate-900">自定义字段</h4>
      <div class="space-y-2">
        <div
          v-for="item in customFieldEntries"
          :key="item.key"
          class="wk-product-info-row"
        >
          <span class="wk-product-info-row__label">{{ item.key }}</span>
          <span class="wk-product-info-row__value">{{ String(item.value ?? '-') }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductVO } from '@/types/product'
import { formatProductCreatorName, formatProductTypeLabel } from '@/utils/productDisplay'

const props = defineProps<{
  product: ProductVO
}>()

defineEmits<{
  edit: [product: ProductVO]
}>()

const product = computed(() => props.product)
const basicInfoRows = computed(() => [
  { label: '产品名称', value: product.value.productName || '-' },
  { label: '产品编号', value: product.value.productCode || '-' },
  { label: '状态', value: formatStatusLabel(product.value.status) },
  { label: '类目', value: product.value.categoryPath || product.value.categoryName || '-' },
  { label: '类型', value: formatProductTypeLabel(product.value.productType) },
  { label: '单位', value: product.value.unit || '-' },
  { label: '标准价', value: formatMoney(product.value.standardPrice) },
  { label: '成本价', value: formatMoney(product.value.costPrice) },
  { label: '负责人', value: product.value.ownerName || '-' },
  { label: '创建人', value: formatProductCreatorName(product.value) },
  { label: '创建时间', value: formatDateTime(product.value.createTime) },
  { label: '更新时间', value: formatDateTime(product.value.updateTime || product.value.createTime) }
])
const customFieldEntries = computed(() => Object.entries(product.value.customFields || {})
  .filter(([, value]) => value !== null && value !== undefined && value !== '')
  .map(([key, value]) => ({ key, value })))

function formatMoney(value?: number | string): string {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  if (Number.isNaN(n)) return String(value)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatStatusLabel(value?: string): string {
  return value === 'inactive' ? '停用' : '启用'
}

function formatDateTime(value?: string): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.wk-product-chat-info-panel {
  background: var(--wk-bg-surface);
}

.wk-product-info-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: start;
  gap: 12px;
}

.wk-product-info-row__label {
  min-width: 0;
  color: var(--wk-text-muted);
  font-size: 13px;
  font-weight: 500;
  line-height: 22px;
}

.wk-product-info-row__value {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--wk-text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;
  text-align: left;
}

.wk-product-panel-edit-btn {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  border-radius: 8px;
  color: var(--wk-text-muted);
  transition: background-color 0.15s ease, color 0.15s ease;
}

.wk-product-panel-edit-btn:hover {
  background: var(--wk-bg-surface-subtle);
  color: var(--wk-text-primary);
}
</style>
