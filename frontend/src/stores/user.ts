import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getUserInfo as apiGetUserInfo, updateProfile as apiUpdateProfile } from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/request'
import type { UserInfo, LoginParams } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realname = computed(() => userInfo.value?.realname || '')
  const userId = computed(() => userInfo.value?.userId || '')

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
      removeToken()
    }
  }

  async function fetchUserInfo(): Promise<UserInfo | null> {
    if (!token.value) return null

    try {
      const info = await apiGetUserInfo()
      userInfo.value = info
      return info
    } catch (error) {
      // Token might be invalid
      token.value = null
      userInfo.value = null
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
    removeToken()
  }

  async function updateProfile(data: { realname?: string; mobile?: string }): Promise<void> {
    await apiUpdateProfile(data)
    // Refresh user info after update
    await fetchUserInfo()
  }

  return {
    // State
    token,
    userInfo,
    // Getters
    isLoggedIn,
    username,
    realname,
    userId,
    // Actions
    login,
    logout,
    fetchUserInfo,
    setUserInfo,
    resetState,
    updateProfile
  }
})
