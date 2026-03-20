import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword, getLoginUserDetail, updateProfile } from '@/api/auth'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'

export function useAccountSettings() {
  const userStore = useUserStore()

  const savingProfile = ref(false)
  const avatarUploading = ref(false)
  const avatarInputRef = ref<HTMLInputElement | null>(null)
  const avatarPreviewUrl = ref('')
  const submittingPassword = ref(false)

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
    } catch {
      ElMessage.error('头像上传失败')
    } finally {
      avatarUploading.value = false
      if (avatarInputRef.value) {
        avatarInputRef.value.value = ''
      }
    }
  }

  async function handleSaveProfile() {
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
    } catch {
      // Error handled by interceptor
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

  return {
    userStore,
    savingProfile,
    avatarUploading,
    avatarInputRef,
    avatarPreviewUrl,
    submittingPassword,
    profileForm,
    passwordForm,
    loadProfile,
    resetProfileForm,
    resetPasswordForm,
    resetAll,
    handleAvatarChange,
    handleSaveProfile,
    handleChangePassword
  }
}
