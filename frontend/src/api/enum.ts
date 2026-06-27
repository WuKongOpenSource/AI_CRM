import { get } from '@/utils/request'

export interface EnumOption {
  value: string
  label: string
  description?: string | null
  color?: string
}

/** 客户阶段枚举（真相源：后端 crm_custom_field.options） */
export const getCustomerStageEnum = () => get<EnumOption[]>('/enum/customerStage')

/** 候选人阶段枚举（真相源：后端 crm_custom_field.options） */
export const getCandidateStageEnum = () => get<EnumOption[]>('/enum/candidateStage')

/** 客户级别枚举 */
export const getCustomerLevelEnum = () => get<EnumOption[]>('/enum/customerLevel')

/** 关系类型枚举 */
export const getRelationTypeEnum = () => get<EnumOption[]>('/enum/relationType')

/** 关系来源枚举 */
export const getRelationSourceEnum = () => get<EnumOption[]>('/enum/relationSource')
