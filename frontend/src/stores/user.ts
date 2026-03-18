import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getUserInfo as apiGetUserInfo, updateProfile as apiUpdateProfile, getUserAuth as apiGetUserAuth } from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/request'
import type { UserInfo, LoginParams } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<Record<string, any>>({})

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realname = computed(() => userInfo.value?.realname || '')
  const userId = computed(() => userInfo.value?.userId || '')
  const avatar = computed(() => userInfo.value?.imgUrl || '')

  /**
   * 检查用户是否拥有某个模块的权限
   */
  function hasPermission(module: string): boolean {
    // 权限树为空时（未加载）不限制
    if (!permissions.value || Object.keys(permissions.value).length === 0) return true
    return !!permissions.value[module]
  }

  // Actions
  async function login(params: LoginParams): Promise<void> {
    const result = await apiLogin(params)
    token.value = result.token
    setToken(result.token)
    userInfo.value = result.userInfo
  }

  async function logout(): Promise<void> {
    try {
      await apiLogout()
    } finally {
      token.value = null
      userInfo.value = null
      permissions.value = {}
      removeToken()
    }
  }

  async function fetchUserInfo(): Promise<UserInfo | null> {
    if (!token.value) return null

    try {
      const [info, auth] = await Promise.all([apiGetUserInfo(), apiGetUserAuth()])
      userInfo.value = info
      permissions.value = auth || {}
      return info
    } catch (error) {
      // Token might be invalid
      token.value = null
      userInfo.value = null
      permissions.value = {}
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
    removeToken()
  }

  async function updateProfile(data: Parameters<typeof apiUpdateProfile>[0]): Promise<void> {
    await apiUpdateProfile(data)
    // Refresh user info after update
    await fetchUserInfo()
  }

  return {
    // State
    token,
    userInfo,
    permissions,
    // Getters
    isLoggedIn,
    username,
    realname,
    userId,
    avatar,
    // Actions
    login,
    logout,
    fetchUserInfo,
    setUserInfo,
    resetState,
    updateProfile,
    hasPermission
  }
})
