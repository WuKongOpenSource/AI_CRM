import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as apiLogin,
  logout as apiLogout,
  getUserInfo as apiGetUserInfo,
  updateProfile as apiUpdateProfile,
  getUserAuth as apiGetUserAuth
} from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/request'
import type { UserInfo, LoginParams, LoginResult } from '@/types/api'
import { useEnterpriseStore } from './enterprise'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<Record<string, any>>({})
  const permissionsLoaded = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realname = computed(() => userInfo.value?.realname || '')
  const userId = computed(() => userInfo.value?.userId || '')
  const avatar = computed(() => userInfo.value?.imgUrl || '')

  function hasPermission(permission: string): boolean {
    if (!permissionsLoaded.value) return true
    if (!permissions.value || Object.keys(permissions.value).length === 0) return false

    if (permission.includes(':')) {
      const [module] = permission.split(':')
      const modulePermission = permissions.value[module]
      return !!(modulePermission && typeof modulePermission === 'object' && modulePermission[permission])
    }

    return !!permissions.value[permission]
  }

  async function login(params: LoginParams): Promise<LoginResult> {
    const result = await apiLogin(params)
    if (result.requiresTenantSelection) {
      return result
    }

    await applyLoginResult(result)
    return result
  }

  async function applyLoginResult(result: LoginResult): Promise<void> {
    if (!result.token || !result.userInfo) {
      throw new Error('Invalid login response')
    }

    token.value = result.token
    setToken(result.token)
    userInfo.value = result.userInfo
    permissions.value = await apiGetUserAuth() || {}
    permissionsLoaded.value = true
  }

  async function logout(): Promise<void> {
    try {
      await apiLogout()
    } finally {
      token.value = null
      userInfo.value = null
      permissions.value = {}
      permissionsLoaded.value = false
      removeToken()
      const enterpriseStore = useEnterpriseStore()
      enterpriseStore.reset()
    }
  }

  async function fetchUserInfo(): Promise<UserInfo | null> {
    if (!token.value) return null

    try {
      const [info, auth] = await Promise.all([apiGetUserInfo(), apiGetUserAuth()])
      userInfo.value = info
      permissions.value = auth || {}
      permissionsLoaded.value = true
      return info
    } catch (error) {
      token.value = null
      userInfo.value = null
      permissions.value = {}
      permissionsLoaded.value = false
      removeToken()
      throw error
    }
  }

  function setUserInfo(info: UserInfo): void {
    userInfo.value = info
  }

  function resetState(): void {
    token.value = null
    userInfo.value = null
    permissions.value = {}
    permissionsLoaded.value = false
    removeToken()
  }

  async function updateProfile(data: Parameters<typeof apiUpdateProfile>[0]): Promise<void> {
    await apiUpdateProfile(data)
    await fetchUserInfo()
  }

  return {
    token,
    userInfo,
    permissions,
    permissionsLoaded,
    isLoggedIn,
    username,
    realname,
    userId,
    avatar,
    login,
    applyLoginResult,
    logout,
    fetchUserInfo,
    setUserInfo,
    resetState,
    updateProfile,
    hasPermission
  }
})
