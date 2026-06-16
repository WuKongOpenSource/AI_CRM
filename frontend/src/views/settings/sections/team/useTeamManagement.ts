import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { queryUserList, addUser, updateUserInfo, deleteUsers, resetUsername } from '@/api/auth'
import { queryDeptTree as fetchDeptTree, addDept, updateDept, deleteDept } from '@/api/dept'
import { queryRoleList } from '@/api/role'
import { useUserStore } from '@/stores/user'
import type { DeptVO } from '@/types/dept'
import type { RoleVO } from '@/types/role'
import { TEAM_AVATAR_COLORS } from './constants'

// Keep in sync with com.kakarote.ai_crm.common.Const.AUTH_DATA_RECURSION_NUM
const AUTH_DATA_RECURSION_NUM = 20
const MEMBER_PAGE_SIZE = 10

interface DeptOption {
  label: string
  value: string
  depth: number
}

export function useTeamManagement() {
  const { isMobile } = useResponsive()
  const userStore = useUserStore()

  const deptTree = ref<DeptVO[]>([])
  const selectedDept = ref<DeptVO | null>(null)
  const deptMemberList = ref<any[]>([])
  const memberList = ref<any[]>([])
  const loadingDeptTree = ref(false)
  const loadingMembers = ref(false)
  const showDeptDrawer = ref(false)
  const memberSearch = ref('')
  const memberRoleId = ref('0')
  const memberPage = ref(1)
  const memberPageSize = ref(MEMBER_PAGE_SIZE)
  const memberTotal = ref(0)
  const showDeptDialog = ref(false)
  const submittingDept = ref(false)
  const editingDept = ref<DeptVO | null>(null)
  const showAddMemberDialog = ref(false)
  const submittingMember = ref(false)
  const editingMember = ref<any>(null)
  const allRoleOptions = ref<RoleVO[]>([])
  const allUserListForParent = ref<any[]>([])
  const showMemberDetailDrawer = ref(false)
  const detailMember = ref<any>(null)
  const showResetUsernameDialog = ref(false)
  const resettingUsername = ref(false)
  const resetUsernameMember = ref<any>(null)
  let memberListRequestId = 0

  const deptForm = reactive({
    deptName: '',
    parentId: '0',
    sortOrder: 0
  })

  const memberForm = reactive({
    username: '',
    realname: '',
    password: '',
    mobile: '',
    email: '',
    post: '',
    deptId: null as number | string | null,
    parentId: null as number | string | null,
    status: 1 as number,
    employeeStatus: 'active',
    roleIds: [] as string[]
  })

  const resetUsernameForm = reactive({
    username: '',
    currentPassword: ''
  })

  const filteredMembers = computed(() => {
    return memberList.value || []
  })

  const memberTotalPages = computed(() => Math.max(1, Math.ceil(memberTotal.value / memberPageSize.value)))

  const visibleMemberPages = computed(() => {
    const total = memberTotalPages.value
    const current = memberPage.value
    const maxVisible = 5
    const start = Math.max(1, Math.min(current - Math.floor(maxVisible / 2), total - maxVisible + 1))
    const end = Math.min(total, start + maxVisible - 1)
    const pages: number[] = []
    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
    return pages
  })

  const deptCount = computed(() => countDepts(deptTree.value))

  const parentOptions = computed(() => {
    const currentId = editingMember.value?.userId
    return allUserListForParent.value.filter((user: any) =>
      user.status === 1 && (!currentId || String(user.userId) !== String(currentId))
    )
  })

  const parentDeptOptions = computed(() => {
    const excludedDeptIds = new Set<string>()

    if (editingDept.value) {
      excludedDeptIds.add(String(editingDept.value.deptId))
      collectDeptIds(editingDept.value.children || [], excludedDeptIds)
    }

    return flattenDeptOptions(deptTree.value).filter((option) => !excludedDeptIds.has(option.value))
  })

  onMounted(async () => {
    await Promise.all([loadDeptTree(), loadRoleOptions()])
  })

  watch(memberRoleId, () => {
    if (selectedDept.value) {
      memberPage.value = 1
      loadMembers()
    }
  })

  watch(memberSearch, () => {
    if (selectedDept.value) {
      memberPage.value = 1
      loadMembers()
    }
  })

  function getAvatarColor(name: string): string {
    if (!name) return TEAM_AVATAR_COLORS[0]
    const index = name.charCodeAt(0) % TEAM_AVATAR_COLORS.length
    return TEAM_AVATAR_COLORS[index]
  }

  function countDepts(tree: any[], depth = AUTH_DATA_RECURSION_NUM, visited = new Set<string>()): number {
    if (depth <= 0) {
      return 0
    }

    let count = 0
    for (const node of tree) {
      const deptId = node?.deptId != null ? String(node.deptId) : null
      if (deptId && visited.has(deptId)) {
        continue
      }
      if (deptId) {
        visited.add(deptId)
      }

      count++
      if (node.children) {
        count += countDepts(node.children, depth - 1, visited)
      }
    }
    return count
  }

  function normalizeDeptId(value?: string | number | null): string {
    if (value === null || value === undefined || value === '') {
      return '0'
    }
    return String(value)
  }

  function findDeptById(tree: DeptVO[], deptId: string | number): DeptVO | null {
    const targetId = String(deptId)
    for (const node of tree) {
      if (String(node.deptId) === targetId) {
        return node
      }
      const child = findDeptById(node.children || [], deptId)
      if (child) {
        return child
      }
    }
    return null
  }

  function flattenDeptOptions(tree: DeptVO[], depth = 0): DeptOption[] {
    return tree.flatMap((node) => {
      return [
        {
          label: node.deptName,
          value: String(node.deptId),
          depth
        },
        ...flattenDeptOptions(node.children || [], depth + 1)
      ]
    })
  }

  function collectDeptIds(tree: DeptVO[], ids: Set<string>) {
    for (const node of tree) {
      ids.add(String(node.deptId))
      collectDeptIds(node.children || [], ids)
    }
  }

  async function loadDeptTree() {
    loadingDeptTree.value = true
    try {
      deptTree.value = await fetchDeptTree()
      if (selectedDept.value) {
        selectedDept.value = findDeptById(deptTree.value, selectedDept.value.deptId) || deptTree.value[0] || null
      } else if (deptTree.value.length > 0) {
        selectedDept.value = deptTree.value[0]
      } else {
        selectedDept.value = null
      }
      await loadMembers()
    } catch {
      // Error handled by interceptor
    } finally {
      loadingDeptTree.value = false
    }
  }

  async function loadMembers() {
    if (!selectedDept.value) {
      deptMemberList.value = []
      memberList.value = []
      memberTotal.value = 0
      return
    }

    const requestId = ++memberListRequestId
    loadingMembers.value = true

    try {
      const deptQuery = { deptId: selectedDept.value.deptId, limit: 500 }
      const listQuery: Parameters<typeof queryUserList>[0] = {
        deptId: selectedDept.value.deptId,
        page: memberPage.value,
        limit: memberPageSize.value
      }
      const keyword = memberSearch.value.trim()
      const roleId = String(memberRoleId.value || '0')
      if (keyword) {
        listQuery.search = keyword
      }
      if (roleId !== '0') {
        listQuery.roleId = roleId
      }

      const [deptRes, listRes] = await Promise.all([
        queryUserList(deptQuery),
        queryUserList(listQuery)
      ])
      if (memberListRequestId !== requestId) return

      deptMemberList.value = deptRes?.list || deptRes?.records || deptRes || []
      const nextMembers = listRes?.list || listRes?.records || (Array.isArray(listRes) ? listRes : [])
      const nextTotal = Number(listRes?.totalRow ?? listRes?.total ?? nextMembers.length)
      const nextTotalPages = Math.max(1, Math.ceil(nextTotal / memberPageSize.value))

      if (nextTotal > 0 && memberPage.value > nextTotalPages) {
        memberPage.value = nextTotalPages
        await loadMembers()
        return
      }

      memberList.value = nextMembers
      memberTotal.value = nextTotal
    } catch {
      if (memberListRequestId === requestId) {
        deptMemberList.value = []
        memberList.value = []
        memberTotal.value = 0
      }
    } finally {
      if (memberListRequestId === requestId) {
        loadingMembers.value = false
      }
    }
  }

  function handleDeptClick(dept: DeptVO) {
    selectedDept.value = dept
    memberPage.value = 1
    loadMembers()
  }

  function handleMemberPageChange(page: number) {
    if (page < 1 || page > memberTotalPages.value) return
    if (memberPage.value === page) return
    memberPage.value = page
    loadMembers()
  }

  function handleDeptCommand(command: string, dept: DeptVO) {
    if (command === 'edit') {
      handleEditDept(dept)
      return
    }
    if (command === 'addChild') {
      handleAddDept(dept.deptId)
      return
    }
    if (command === 'delete') {
      handleDeleteDept(dept)
    }
  }

  function handleAddDept(parentId: string | number) {
    editingDept.value = null
    Object.assign(deptForm, { deptName: '', parentId: normalizeDeptId(parentId), sortOrder: 0 })
    showDeptDialog.value = true
  }

  function handleEditDept(dept: DeptVO) {
    editingDept.value = dept
    Object.assign(deptForm, {
      deptName: dept.deptName,
      parentId: normalizeDeptId(dept.parentId),
      sortOrder: dept.sortOrder || 0
    })
    showDeptDialog.value = true
  }

  async function handleDeleteDept(dept: DeptVO) {
    try {
      await ElMessageBox.confirm(`确定要删除部门「${dept.deptName}」吗？`, '提示', { type: 'warning' })
      await deleteDept(dept.deptId)
      ElMessage.success('部门删除成功')
      if (selectedDept.value?.deptId === dept.deptId) {
        selectedDept.value = null
        memberList.value = []
      }
      await loadDeptTree()
    } catch {
      // cancelled
    }
  }

  async function handleSaveDept() {
    const deptName = deptForm.deptName.trim()

    if (!deptName) {
      ElMessage.warning('请输入部门名称')
      return
    }

    submittingDept.value = true
    try {
      const payload = {
        deptName,
        parentId: normalizeDeptId(deptForm.parentId),
        sortOrder: deptForm.sortOrder
      }

      if (editingDept.value) {
        await updateDept({
          deptId: editingDept.value.deptId,
          ...payload
        })
        ElMessage.success('部门更新成功')
      } else {
        await addDept(payload)
        ElMessage.success('部门添加成功')
      }
      showDeptDialog.value = false
      await loadDeptTree()
    } catch {
      // Error handled by interceptor
    } finally {
      submittingDept.value = false
    }
  }

  function resetMemberForm() {
    editingMember.value = null
    Object.assign(memberForm, {
      username: '',
      realname: '',
      password: '',
      mobile: '',
      email: '',
      post: '',
      deptId: selectedDept.value ? selectedDept.value.deptId : null,
      parentId: null,
      status: 1,
      employeeStatus: 'active',
      roleIds: []
    })
  }

  async function loadAllUsersForParent() {
    try {
      const res = await queryUserList({ limit: 500 })
      allUserListForParent.value = res?.list || res?.records || []
    } catch {
      // ignore
    }
  }

  async function loadRoleOptions() {
    try {
      allRoleOptions.value = await queryRoleList()
    } catch {
      // ignore
    }
  }

  function handleAddMember() {
    resetMemberForm()
    loadAllUsersForParent()
    loadRoleOptions()
    showAddMemberDialog.value = true
  }

  function handleEditMember(member: any) {
    editingMember.value = member
    loadAllUsersForParent()
    loadRoleOptions()
    Object.assign(memberForm, {
      username: member.username || '',
      realname: member.realname || '',
      password: '',
      mobile: member.mobile || '',
      email: member.email || '',
      post: member.post || '',
      deptId: member.deptId || null,
      parentId: member.parentId || null,
      status: member.status ?? 1,
      employeeStatus: member.employeeStatus || 'active',
      roleIds: (member.roleIds || []).map(String)
    })
    showAddMemberDialog.value = true
  }

  async function handleSaveMember() {
    if (editingMember.value) {
      if (!memberForm.realname.trim()) {
        ElMessage.warning('请输入姓名')
        return
      }
      submittingMember.value = true
      try {
        const payload: Parameters<typeof updateUserInfo>[0] = {
          userId: editingMember.value.userId,
          realname: memberForm.realname,
          mobile: memberForm.mobile || undefined,
          email: memberForm.email || undefined,
          post: memberForm.post || undefined,
          deptId: memberForm.deptId || undefined,
          parentId: memberForm.parentId || 0,
          employeeStatus: memberForm.employeeStatus,
          password: memberForm.password || undefined,
          status: memberForm.status,
          roleIds: memberForm.roleIds
        }
        await updateUserInfo(payload)
        ElMessage.success('员工更新成功')
        showAddMemberDialog.value = false
        editingMember.value = null
        await loadMembers()
        await loadDeptTree()
      } catch {
        // Error handled by interceptor
      } finally {
        submittingMember.value = false
      }
      return
    }

    if (!memberForm.username.trim() || !memberForm.realname.trim()) {
      ElMessage.warning('请填写用户名和姓名')
      return
    }

    submittingMember.value = true
    try {
      await addUser({
        username: memberForm.username,
        password: memberForm.password || '123456',
        realname: memberForm.realname,
        mobile: memberForm.mobile || undefined,
        email: memberForm.email || undefined,
        deptId: memberForm.deptId || (selectedDept.value ? selectedDept.value.deptId : undefined),
        post: memberForm.post || undefined,
        parentId: memberForm.parentId || undefined,
        status: memberForm.status,
        employeeStatus: memberForm.employeeStatus,
        roleIds: memberForm.roleIds.length > 0 ? memberForm.roleIds : undefined
      })
      ElMessage.success('员工添加成功')
      showAddMemberDialog.value = false
      resetMemberForm()
      await loadMembers()
      await loadDeptTree()
    } catch {
      // Error handled by interceptor
    } finally {
      submittingMember.value = false
    }
  }

  function handleMemberRowClick(member: any) {
    detailMember.value = member
    showMemberDetailDrawer.value = true
  }

  async function handleToggleStatus(member: any) {
    const newStatus = member.status === 1 ? 0 : 1
    const action = newStatus === 0 ? '禁用' : '启用'
    try {
      await ElMessageBox.confirm(`确定要${action}成员「${member.realname || member.username}」吗？`, '提示', { type: 'warning' })
      await updateUserInfo({ userId: member.userId, status: newStatus })
      ElMessage.success(`成员已${action}`)
      await loadMembers()
    } catch {
      // cancelled
    }
  }

  function handleResetUsername(member: any) {
    resetUsernameMember.value = member
    Object.assign(resetUsernameForm, {
      username: member.username || '',
      currentPassword: ''
    })
    showResetUsernameDialog.value = true
  }

  async function handleSaveResetUsername() {
    const member = resetUsernameMember.value
    if (!member) return

    const username = resetUsernameForm.username.trim()
    if (!username) {
      ElMessage.warning('请输入新用户名')
      return
    }
    if (username === String(member.username || '').trim()) {
      ElMessage.warning('新用户名不能与当前用户名相同')
      return
    }
    const resettingCurrentUser = String(member.userId) === String(userStore.userId)
    if (resettingCurrentUser && !resetUsernameForm.currentPassword.trim()) {
      ElMessage.warning('请输入当前登录密码')
      return
    }

    resettingUsername.value = true
    try {
      await resetUsername({
        userId: member.userId,
        username,
        currentPassword: resettingCurrentUser ? resetUsernameForm.currentPassword : undefined
      })
      ElMessage.success('用户名已重置')
      showResetUsernameDialog.value = false
      resetUsernameMember.value = null
      await loadMembers()
      if (String(member.userId) === String(userStore.userId)) {
        await userStore.fetchUserInfo()
      }
    } catch {
      // Error handled by interceptor
    } finally {
      resettingUsername.value = false
    }
  }

  async function handleDeleteMember(member: any) {
    try {
      await ElMessageBox.confirm(
        `确定要删除员工「${member.realname || member.username}」吗？此操作不可撤销。`,
        '删除确认',
        { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
      )
      await deleteUsers([member.userId])
      ElMessage.success('员工已删除')
      showMemberDetailDrawer.value = false
      await loadMembers()
      await loadDeptTree()
    } catch {
      // cancelled
    }
  }

  return {
    isMobile,
    deptTree,
    selectedDept,
    deptMemberList,
    memberList,
    loadingDeptTree,
    loadingMembers,
    showDeptDrawer,
    memberSearch,
    memberRoleId,
    memberPage,
    memberPageSize,
    memberTotal,
    filteredMembers,
    memberTotalPages,
    visibleMemberPages,
    deptCount,
    showDeptDialog,
    submittingDept,
    editingDept,
    deptForm,
    parentDeptOptions,
    showAddMemberDialog,
    submittingMember,
    editingMember,
    memberForm,
    showResetUsernameDialog,
    resettingUsername,
    resetUsernameMember,
    resetUsernameForm,
    allRoleOptions,
    parentOptions,
    showMemberDetailDrawer,
    detailMember,
    getAvatarColor,
    loadMembers,
    handleDeptClick,
    handleMemberPageChange,
    handleDeptCommand,
    handleAddDept,
    handleSaveDept,
    handleAddMember,
    handleEditMember,
    handleSaveMember,
    handleMemberRowClick,
    handleToggleStatus,
    handleResetUsername,
    handleSaveResetUsername,
    handleDeleteMember
  }
}
