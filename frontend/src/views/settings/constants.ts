import type { SettingsMainTab, SettingsTabItem, SystemSettingsTab } from './types'

export const SETTINGS_MAIN_TABS: SettingsTabItem<SettingsMainTab>[] = [
  { value: 'team', label: '组织员工管理' },
  { value: 'role', label: '角色权限管理' },
  { value: 'system', label: '系统参数设置' }
]

export const SYSTEM_SETTINGS_TABS: SettingsTabItem<SystemSettingsTab>[] = [
  { value: 'profile', label: '个人资料' },
  { value: 'enterprise', label: '企业信息' },
  { value: 'api', label: 'AI/API 配置' },
  { value: 'agent', label: '智能体' },
  { value: 'weknora', label: '知识库服务' },
  { value: 'storage', label: '对象存储' },
  { value: 'customField', label: '自定义字段' }
]
