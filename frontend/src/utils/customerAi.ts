export type CustomerAiStatus = '高意向' | '活跃状态' | '需跟进' | '休眠'

type CustomerAiStatusMeta = {
  label: CustomerAiStatus
  badgeClass: string
  dotClass: string
}

const CUSTOMER_AI_STATUS_META: Record<CustomerAiStatus, CustomerAiStatusMeta> = {
  高意向: {
    label: '高意向',
    badgeClass: 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200/90',
    dotClass: 'bg-emerald-500'
  },
  活跃状态: {
    label: '活跃状态',
    badgeClass: 'bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-200/90',
    dotClass: 'bg-blue-500'
  },
  需跟进: {
    label: '需跟进',
    badgeClass: 'bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-200/90',
    dotClass: 'bg-amber-500'
  },
  休眠: {
    label: '休眠',
    badgeClass: 'bg-slate-100 text-slate-600 ring-1 ring-inset ring-slate-200',
    dotClass: 'bg-slate-400'
  }
}

const CUSTOMER_AI_STATUS_KEYWORDS: Array<{ status: CustomerAiStatus; keywords: string[] }> = [
  { status: '休眠', keywords: ['休眠', '沉睡', '长期未跟进', '长期未联系', '长期未互动', '超过30天', '超30天', '流失风险'] },
  { status: '高意向', keywords: ['高意向', '强意向', '高潜', '高潜力', '高优先', '签约', '成交'] },
  { status: '活跃状态', keywords: ['活跃状态', '活跃', '持续沟通', '频繁互动', '跟进中', '推进中', '已安排下次跟进', '最近跟进于'] },
  { status: '需跟进', keywords: ['需跟进', '待跟进', '需要跟进', '待联系', '待推进', '待回访', '待确认'] }
]

export function normalizeCustomerAiStatus(value?: string | null): CustomerAiStatus | '' {
  const normalized = String(value || '').trim()
  if (!normalized) return ''

  if (normalized in CUSTOMER_AI_STATUS_META) {
    return normalized as CustomerAiStatus
  }

  const compact = normalized.replace(/\s+/g, '')
  for (const item of CUSTOMER_AI_STATUS_KEYWORDS) {
    if (item.keywords.some(keyword => compact.includes(keyword))) {
      return item.status
    }
  }

  return ''
}

export function getCustomerAiStatusMeta(value?: string | null): CustomerAiStatusMeta | null {
  const status = normalizeCustomerAiStatus(value)
  if (!status) return null
  return CUSTOMER_AI_STATUS_META[status]
}

