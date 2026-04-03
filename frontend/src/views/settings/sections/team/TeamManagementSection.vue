<template>
  <div class="h-full min-h-0">
    <div class="flex h-full min-h-0 bg-slate-50/50 overflow-hidden" :class="{ 'flex-col': isMobile }">
      <DepartmentTreePanel
        v-if="!isMobile"
        variant="desktop"
        :dept-tree="deptTree"
        :selected-dept="selectedDept"
        :loading="loadingDeptTree"
        @select-dept="handleDeptClick"
        @dept-command="handleDeptCommand"
        @add-root-dept="handleAddDept(0)"
      />

      <MemberListPanel
        :is-mobile="isMobile"
        :selected-dept="selectedDept"
        :dept-member-list="deptMemberList"
        :filtered-members="filteredMembers"
        :loading-members="loadingMembers"
        :member-search="memberSearch"
        :member-role-id="memberRoleId"
        :all-role-options="allRoleOptions"
        :dept-count="deptCount"
        :get-avatar-color="getAvatarColor"
        @update:member-search="memberSearch = $event"
        @update:member-role-id="memberRoleId = $event"
        @open-dept-drawer="showDeptDrawer = true"
        @add-member="handleAddMember"
        @row-click="handleMemberRowClick"
        @edit-member="handleEditMember"
        @toggle-status="handleToggleStatus"
      />
    </div>

    <DepartmentTreePanel
      v-if="isMobile"
      variant="drawer"
      :dept-tree="deptTree"
      :selected-dept="selectedDept"
      :loading="loadingDeptTree"
      :show-drawer="showDeptDrawer"
      @update:show-drawer="showDeptDrawer = $event"
      @select-dept="handleDeptClick"
      @dept-command="handleDeptCommand"
      @add-root-dept="handleAddDept(0)"
    />

    <DeptDialog
      :visible="showDeptDialog"
      :is-mobile="isMobile"
      :editing-dept="editingDept"
      :dept-form="deptForm"
      :parent-dept-options="parentDeptOptions"
      :submitting="submittingDept"
      @update:visible="showDeptDialog = $event"
      @save="handleSaveDept"
    />

    <MemberDialog
      :visible="showAddMemberDialog"
      :is-mobile="isMobile"
      :editing-member="editingMember"
      :member-form="memberForm"
      :dept-tree="deptTree"
      :parent-options="parentOptions"
      :all-role-options="allRoleOptions"
      :submitting="submittingMember"
      @update:visible="showAddMemberDialog = $event"
      @save="handleSaveMember"
    />

    <MemberDetailDrawer
      :visible="showMemberDetailDrawer"
      :is-mobile="isMobile"
      :member="detailMember"
      :get-avatar-color="getAvatarColor"
      @update:visible="showMemberDetailDrawer = $event"
      @edit="handleEditMember"
      @delete="handleDeleteMember"
    />
  </div>
</template>

<script setup lang="ts">
import DeptDialog from './components/DeptDialog.vue'
import DepartmentTreePanel from './components/DepartmentTreePanel.vue'
import MemberDialog from './components/MemberDialog.vue'
import MemberDetailDrawer from './components/MemberDetailDrawer.vue'
import MemberListPanel from './components/MemberListPanel.vue'
import { useTeamManagement } from './useTeamManagement'

const {
  isMobile,
  deptTree,
  selectedDept,
  deptMemberList,
  loadingDeptTree,
  loadingMembers,
  showDeptDrawer,
  memberSearch,
  memberRoleId,
  filteredMembers,
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
  allRoleOptions,
  parentOptions,
  showMemberDetailDrawer,
  detailMember,
  getAvatarColor,
  handleDeptClick,
  handleDeptCommand,
  handleAddDept,
  handleSaveDept,
  handleAddMember,
  handleEditMember,
  handleSaveMember,
  handleMemberRowClick,
  handleToggleStatus,
  handleDeleteMember
} = useTeamManagement()
</script>
