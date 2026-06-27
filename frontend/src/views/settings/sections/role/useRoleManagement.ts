import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { queryUserList } from '@/api/auth'
import {
  queryRoleList,
  addRole,
  updateRole,
  deleteRole as deleteRoleApi,
  getRolePermissions,
  saveRolePermissions,
  addUsersToRole,
  removeUserFromRole
} from '@/api/role'
import type { PermItem, RolePermissionVO, RoleVO } from '@/types/role'
import { ROLE_AVATAR_COLORS, ROLE_DATA_SCOPE_OPTIONS } from './constants'

const ROLE_PERMISSION_MODULE_ORDER = ['customer', 'candidate', 'product', 'addressBook', 'contact', 'followup', 'task', 'schedule', 'knowledge', 'chat', 'agent', 'user', 'dept', 'role', 'config', 'customField']
const PRODUCT_PERMISSION_ACTIONS_WITHOUT_SCOPE = new Set(['category_manage', 'settings'])

function sortRolePermissions(permissionList: RolePermissionVO[]): RolePermissionVO[] {
  return [...permissionList].sort((left, right) => {
    const leftIndex = ROLE_PERMISSION_MODULE_ORDER.indexOf(left.module)
    const rightIndex = ROLE_PERMISSION_MODULE_ORDER.indexOf(right.module)
    const normalizedLeft = leftIndex === -1 ? ROLE_PERMISSION_MODULE_ORDER.length : leftIndex
    const normalizedRight = rightIndex === -1 ? ROLE_PERMISSION_MODULE_ORDER.length : rightIndex

    if (normalizedLeft !== normalizedRight) {
      return normalizedLeft - normalizedRight
    }

    return left.moduleName.localeCompare(right.moduleName, 'zh-CN')
  })
}

function normalizeRolePermissions(permissionList: RolePermissionVO[]): RolePermissionVO[] {
  return sortRolePermissions(permissionList.map(moduleGroup => {
    const actions = moduleGroup.actions
      .filter(action => !(moduleGroup.module === 'product' && action.action === 'settings'))
      .map(action => {
        if (moduleGroup.module !== 'product' || !PRODUCT_PERMISSION_ACTIONS_WITHOUT_SCOPE.has(action.action)) {
          return action
        }
        return {
          ...action,
          dataScope: null,
          hasScopeOption: false
        }
      })

    return {
      ...moduleGroup,
      actions
    }
  }))
}

export function useRoleManagement() {
  const { isMobile } = useResponsive()

  const roleList = ref<RoleVO[]>([])
  const loadingRoles = ref(false)
  const showRoleDialog = ref(false)
  const savingRole = ref(false)
  const editingRole = ref<RoleVO | null>(null)
  const showRoleDrawer = ref(false)
  const roleDrawerRole = ref<RoleVO | null>(null)
  const roleDrawerTab = ref<'members' | 'permissions'>('members')
  const permissionRole = ref<RoleVO | null>(null)
  const permissionList = ref<RolePermissionVO[]>([])
  const loadingPermissions = ref(false)
  const savingPermissions = ref(false)
  const roleUsersRole = ref<RoleVO | null>(null)
  const roleUsers = ref<any[]>([])
  const loadingRoleUsers = ref(false)
  const roleUserSearch = ref('')
  const showAddRoleUserDialog = ref(false)
  const allUsers = ref<any[]>([])
  const allUserSearch = ref('')
  const selectedUserIds = ref<string[]>([])
  const addingRoleUsers = ref(false)

  const roleForm = reactive({
    roleName: '',
    description: ''
  })

  const filteredRoleUsers = computed(() => {
    if (!roleUserSearch.value) return roleUsers.value
    const keyword = roleUserSearch.value.toLowerCase()
    return roleUsers.value.filter((user: any) =>
      (user.realname || '').toLowerCase().includes(keyword) ||
      (user.username || '').toLowerCase().includes(keyword) ||
      (user.email || '').toLowerCase().includes(keyword)
    )
  })

  const availableUsersForRole = computed(() => {
    const existingIds = new Set(roleUsers.value.map((user: any) => String(user.userId)))
    let users = allUsers.value.filter((user: any) => !existingIds.has(String(user.userId)))

    if (allUserSearch.value) {
      const keyword = allUserSearch.value.toLowerCase()
      users = users.filter((user: any) =>
        (user.realname || '').toLowerCase().includes(keyword) ||
        (user.username || '').toLowerCase().includes(keyword)
      )
    }

    return users
  })

  onMounted(async () => {
    await loadRoleList()
  })

  watch(roleDrawerTab, async (newTab) => {
    if (!showRoleDrawer.value || !roleDrawerRole.value) return

    if (newTab === 'members') {
      roleUsersRole.value = roleDrawerRole.value
      roleUserSearch.value = ''
      await loadRoleUsers()
      return
    }

    permissionRole.value = roleDrawerRole.value
    loadingPermissions.value = true
    try {
      permissionList.value = normalizeRolePermissions(await getRolePermissions(roleDrawerRole.value.roleId))
    } catch {
      // Error handled by interceptor
    } finally {
      loadingPermissions.value = false
    }
  })

  watch(showAddRoleUserDialog, async (visible) => {
    if (!visible) return
    allUserSearch.value = ''
    selectedUserIds.value = []
    try {
      const res = await queryUserList({ limit: 200 })
      allUsers.value = res?.list || res?.records || (Array.isArray(res) ? res : [])
    } catch {
      // Error handled by interceptor
    }
  })

  function getAvatarColor(name: string): string {
    if (!name) return ROLE_AVATAR_COLORS[0]
    const index = name.charCodeAt(0) % ROLE_AVATAR_COLORS.length
    return ROLE_AVATAR_COLORS[index]
  }

  async function loadRoleList() {
    loadingRoles.value = true
    try {
      roleList.value = await queryRoleList()
    } catch {
      // Error handled by interceptor
    } finally {
      loadingRoles.value = false
    }
  }

  function handleAddRole() {
    editingRole.value = null
    Object.assign(roleForm, { roleName: '', description: '' })
    showRoleDialog.value = true
  }

  function handleEditRole(role: RoleVO) {
    editingRole.value = role
    Object.assign(roleForm, { roleName: role.roleName, description: role.description || '' })
    showRoleDialog.value = true
  }

  async function handleSaveRole() {
    if (!roleForm.roleName.trim()) {
      ElMessage.warning('请输入角色名称')
      return
    }

    savingRole.value = true
    try {
      if (editingRole.value) {
        await updateRole({
          roleId: editingRole.value.roleId,
          roleName: roleForm.roleName,
          description: roleForm.description
        })
        ElMessage.success('角色更新成功')
      } else {
        await addRole({
          roleName: roleForm.roleName,
          description: roleForm.description
        })
        ElMessage.success('角色创建成功')
      }
      showRoleDialog.value = false
      await loadRoleList()
    } catch {
      // Error handled by interceptor
    } finally {
      savingRole.value = false
    }
  }

  async function handleDeleteRole(role: RoleVO) {
    try {
      await ElMessageBox.confirm(`确定要删除角色「${role.roleName}」吗？该角色下的 ${role.userCount} 个用户将失去此角色。`, '提示', { type: 'warning' })
      await deleteRoleApi(role.roleId)
      ElMessage.success('角色删除成功')
      await loadRoleList()
    } catch {
      // cancelled
    }
  }

  async function openRoleDrawer(role: RoleVO, tab: 'members' | 'permissions') {
    roleDrawerRole.value = role
    roleDrawerTab.value = tab
    showRoleDrawer.value = true

    if (tab === 'members') {
      roleUsersRole.value = role
      roleUserSearch.value = ''
      await loadRoleUsers()
      return
    }

    permissionRole.value = role
    loadingPermissions.value = true
    try {
      permissionList.value = normalizeRolePermissions(await getRolePermissions(role.roleId))
    } catch {
      // Error handled by interceptor
    } finally {
      loadingPermissions.value = false
    }
  }

  async function handleSavePermissions() {
    if (!permissionRole.value) return

    savingPermissions.value = true
    const permissions: PermItem[] = []

    for (const moduleGroup of permissionList.value) {
      for (const action of moduleGroup.actions) {
        if (action.enabled) {
          permissions.push({
            menuId: action.menuId,
            dataScope: action.hasScopeOption ? action.dataScope : null
          })
        }
      }
    }

    try {
      await saveRolePermissions({ roleId: permissionRole.value.roleId, permissions })
      ElMessage.success('权限配置保存成功')
      showRoleDrawer.value = false
    } catch {
      // Error handled by interceptor
    } finally {
      savingPermissions.value = false
    }
  }

  async function loadRoleUsers() {
    if (!roleUsersRole.value) return

    loadingRoleUsers.value = true
    try {
      const res = await queryUserList({ roleId: roleUsersRole.value.roleId, limit: 200 })
      roleUsers.value = res?.list || res?.records || (Array.isArray(res) ? res : [])
    } catch {
      // Error handled by interceptor
    } finally {
      loadingRoleUsers.value = false
    }
  }

  async function handleRemoveRoleUser(user: any) {
    if (!roleUsersRole.value) return

    try {
      await ElMessageBox.confirm(`确定要将「${user.realname || user.username}」从该角色中移除吗？`, '提示', { type: 'warning' })
      await removeUserFromRole(String(user.userId), roleUsersRole.value.roleId)
      ElMessage.success('用户已移除')
      await loadRoleUsers()
      await loadRoleList()
    } catch {
      // cancelled
    }
  }

  function toggleSelectUser(userId: string | number) {
    const id = String(userId)
    const index = selectedUserIds.value.indexOf(id)
    if (index >= 0) {
      selectedUserIds.value.splice(index, 1)
      return
    }
    selectedUserIds.value.push(id)
  }

  async function handleConfirmAddRoleUsers() {
    if (!roleUsersRole.value || selectedUserIds.value.length === 0) return

    addingRoleUsers.value = true
    try {
      await addUsersToRole(selectedUserIds.value, roleUsersRole.value.roleId)
      ElMessage.success('用户添加成功')
      showAddRoleUserDialog.value = false
      selectedUserIds.value = []
      await loadRoleUsers()
      await loadRoleList()
    } catch {
      // Error handled by interceptor
    } finally {
      addingRoleUsers.value = false
    }
  }

  return {
    isMobile,
    roleList,
    loadingRoles,
    showRoleDialog,
    savingRole,
    editingRole,
    roleForm,
    showRoleDrawer,
    roleDrawerRole,
    roleDrawerTab,
    permissionList,
    loadingPermissions,
    savingPermissions,
    roleUsers,
    loadingRoleUsers,
    roleUserSearch,
    filteredRoleUsers,
    showAddRoleUserDialog,
    allUserSearch,
    availableUsersForRole,
    selectedUserIds,
    addingRoleUsers,
    dataScopeOptions: ROLE_DATA_SCOPE_OPTIONS,
    getAvatarColor,
    handleAddRole,
    handleEditRole,
    handleSaveRole,
    handleDeleteRole,
    openRoleDrawer,
    handleSavePermissions,
    handleRemoveRoleUser,
    toggleSelectUser,
    handleConfirmAddRoleUsers
  }
}
