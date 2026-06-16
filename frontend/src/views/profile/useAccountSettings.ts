import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  changePassword,
  getExternalAuthBindings,
  getExternalAuthProviders,
  getExternalBindAuthorizeUrl,
  getLoginUserDetail,
  unbindExternalAuth,
  updateProfile
} from '@/api/auth'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { isRequestErrorHandled } from '@/utils/requestError'
import type { ExternalAuthBinding, ExternalAuthProviderCode } from '@/types/api'

type AccountExternalBinding = ExternalAuthBinding & {
  providerName: string
  bound: boolean
  enabled: boolean
}

export function useAccountSettings() {
  const userStore = useUserStore()

  const savingProfile = ref(false)
  const avatarUploading = ref(false)
  const avatarInputRef = ref<HTMLInputElement | null>(null)
  const avatarPreviewUrl = ref('')
  const submittingPassword = ref(false)
  const externalBindings = ref<AccountExternalBinding[]>([])
  const externalBindingsLoading = ref(false)
  const externalBindingProvider = ref<ExternalAuthProviderCode | ''>('')

  const profileForm = reactive({
    img: '',
    imgUrl: '',
    realname: '',
    email: '',
    phone: '',
    department: '',
    position: ''
  })

  const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })

  function applyProfileData(detail: any) {
    profileForm.img = detail?.img || ''
    profileForm.imgUrl = detail?.imgUrl || ''
    profileForm.realname = detail?.realname || userStore.realname || ''
    profileForm.email = detail?.email || ''
    profileForm.phone = detail?.mobile || ''
    profileForm.department = detail?.deptName || ''
    profileForm.position = detail?.post || ''
  }

  async function loadProfile() {
    try {
      const detail = await getLoginUserDetail()
      applyProfileData(detail)
    } catch {
      applyProfileData(userStore.userInfo as any)
    }
  }

  async function loadExternalBindings() {
    externalBindingsLoading.value = true
    try {
      const [providers, bindings] = await Promise.all([
        getExternalAuthProviders(),
        getExternalAuthBindings()
      ])
      const bindingMap = new Map(bindings.map((binding) => [binding.provider, binding]))
      externalBindings.value = providers
        .filter((provider) =>
          provider.provider === 'google'
          || provider.provider === 'wechat'
          || provider.provider === 'outlook'
        )
        .map((provider) => {
          const binding = bindingMap.get(provider.provider)
          const providerName = externalProviderName(provider.provider)
          return {
            provider: provider.provider,
            name: binding?.name || providerName,
            providerName,
            subject: binding?.subject || '',
            email: binding?.email,
            displayName: binding?.displayName,
            avatarUrl: binding?.avatarUrl,
            bindTime: binding?.bindTime,
            lastLoginTime: binding?.lastLoginTime,
            bound: Boolean(binding),
            enabled: provider.enabled
          }
        })
    } catch (error) {
      console.error('Load external auth bindings failed:', error)
      externalBindings.value = []
    } finally {
      externalBindingsLoading.value = false
    }
  }

  function resetProfileForm() {
    applyProfileData(userStore.userInfo as any)
    avatarPreviewUrl.value = ''
    if (avatarInputRef.value) {
      avatarInputRef.value.value = ''
    }
  }

  function resetPasswordForm() {
    Object.assign(passwordForm, {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
  }

  function resetAll() {
    resetProfileForm()
    resetPasswordForm()
    externalBindingProvider.value = ''
  }

  async function handleAvatarChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      ElMessage.warning('请选择图片文件')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过5MB')
      return
    }

    avatarUploading.value = true
    try {
      const presigned = await getPresignedUploadUrl(file.name, file.type)
      await uploadToMinIO(file, presigned.uploadUrl)
      profileForm.img = presigned.objectKey
      avatarPreviewUrl.value = presigned.accessUrl
      ElMessage.success('头像上传成功，点击保存资料后生效')
    } catch (error) {
      console.error('Avatar upload failed:', error)
      if (!isRequestErrorHandled(error)) {
        ElMessage.error('头像上传失败')
      }
    } finally {
      avatarUploading.value = false
      if (avatarInputRef.value) {
        avatarInputRef.value.value = ''
      }
    }
  }

  async function handleSaveProfile(): Promise<boolean> {
    savingProfile.value = true
    try {
      await updateProfile({
        userId: userStore.userId,
        img: profileForm.img,
        realname: profileForm.realname,
        mobile: profileForm.phone,
        email: profileForm.email,
        post: profileForm.position
      })
      await userStore.fetchUserInfo()
      avatarPreviewUrl.value = ''
      profileForm.imgUrl = userStore.userInfo?.imgUrl || ''
      profileForm.department = userStore.userInfo?.deptName || profileForm.department
      ElMessage.success('个人资料保存成功')
      return true
    } catch {
      // Error handled by interceptor
      return false
    } finally {
      savingProfile.value = false
    }
  }

  async function handleChangePassword() {
    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
      ElMessage.warning('请填写完整信息')
      return
    }
    if (passwordForm.newPassword.length < 6) {
      ElMessage.warning('密码至少6位')
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }

    submittingPassword.value = true
    try {
      await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
      ElMessage.success('密码修改成功')
      resetPasswordForm()
    } catch {
      // Error handled by interceptor
    } finally {
      submittingPassword.value = false
    }
  }

  async function handleBindExternal(provider: ExternalAuthProviderCode) {
    externalBindingProvider.value = provider
    try {
      const { authorizeUrl } = await getExternalBindAuthorizeUrl(provider, window.location.href)
      window.location.href = authorizeUrl
    } catch (error) {
      console.error('Start external auth bind failed:', error)
    } finally {
      externalBindingProvider.value = ''
    }
  }

  async function handleUnbindExternal(provider: ExternalAuthProviderCode) {
    try {
      const providerName = externalProviderName(provider)
      await ElMessageBox.confirm(`确定要解绑${providerName}登录吗？`, '解绑第三方登录', {
        confirmButtonText: '解绑',
        cancelButtonText: '取消',
        type: 'warning'
      })
      externalBindingProvider.value = provider
      await unbindExternalAuth(provider)
      ElMessage.success(`${providerName}登录已解绑`)
      await loadExternalBindings()
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') {
        console.error('Unbind external auth failed:', error)
      }
    } finally {
      externalBindingProvider.value = ''
    }
  }

  function externalProviderName(provider: ExternalAuthProviderCode): string {
    if (provider === 'wechat') return '微信'
    if (provider === 'google') return 'Google'
    if (provider === 'outlook') return 'Microsoft'
    return '第三方'
  }

  return {
    userStore,
    savingProfile,
    avatarUploading,
    avatarInputRef,
    avatarPreviewUrl,
    submittingPassword,
    externalBindings,
    externalBindingsLoading,
    externalBindingProvider,
    profileForm,
    passwordForm,
    loadProfile,
    loadExternalBindings,
    resetProfileForm,
    resetPasswordForm,
    resetAll,
    handleAvatarChange,
    handleSaveProfile,
    handleChangePassword,
    handleBindExternal,
    handleUnbindExternal
  }
}
