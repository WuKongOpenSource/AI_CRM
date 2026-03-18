<template>
  <div class="flex flex-col h-full bg-background-light">
    <!-- Settings Header / Tabs -->
    <div class="bg-white border-b border-slate-200 px-4 md:px-8">
      <div class="flex items-center gap-4 md:gap-8 overflow-x-auto">
        <button
          @click="activeTab = 'team'"
          :class="[
            'py-4 text-sm font-bold transition-all border-b-2 whitespace-nowrap',
            activeTab === 'team' ? 'border-primary text-primary' : 'border-transparent text-slate-400 hover:text-slate-600'
          ]"
        >
          组织员工管理
        </button>
        <button
          @click="activeTab = 'role'"
          :class="[
            'py-4 text-sm font-bold transition-all border-b-2 whitespace-nowrap',
            activeTab === 'role' ? 'border-primary text-primary' : 'border-transparent text-slate-400 hover:text-slate-600'
          ]"
        >
          角色权限管理
        </button>
        <button
          @click="activeTab = 'profile'"
          :class="[
            'py-4 text-sm font-bold transition-all border-b-2 whitespace-nowrap',
            isSystemTab ? 'border-primary text-primary' : 'border-transparent text-slate-400 hover:text-slate-600'
          ]"
        >
          系统参数设置
        </button>
      </div>
    </div>

    <!-- Content Area -->
    <div class="flex-1 min-h-0" :class="activeTab === 'team' ? 'overflow-hidden' : 'overflow-auto'">
      <div :class="activeTab === 'team' ? 'p-0 h-full min-h-0 flex flex-col' : 'p-4 md:p-6'">
          <!-- System Sub-tabs (v-show so it doesn't break v-if/v-else-if chain) -->
          <div v-show="isSystemTab" class="max-w-4xl mx-auto mb-6">
            <div class="flex gap-2 overflow-x-auto">
              <button
                v-for="st in systemSubTabs"
                :key="st.value"
                @click="activeTab = st.value"
                :class="[
                  'px-4 py-1.5 text-xs font-bold rounded-full transition-all whitespace-nowrap',
                  activeTab === st.value
                    ? 'bg-primary text-white'
                    : 'bg-white text-slate-500 border border-slate-200 hover:bg-slate-50'
                ]"
              >
                {{ st.label }}
              </button>
            </div>
          </div>

          <!-- Profile Section -->
          <div v-if="activeTab === 'profile'" class="max-w-4xl mx-auto space-y-8">
            <section class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <h3 class="text-base font-bold mb-6 flex items-center gap-2">
                <span class="w-1 h-4 bg-primary rounded-full"></span>
                个人资料
              </h3>
              <!-- Avatar Section -->
              <div class="flex items-center justify-between pb-6 border-b border-slate-200">
                <div class="flex items-center">
                  <div class="relative group cursor-pointer" @click="avatarInputRef?.click()">
                    <div v-if="avatarPreviewUrl || profileForm.imgUrl" class="size-16 rounded-2xl overflow-hidden">
                      <img :src="avatarPreviewUrl || profileForm.imgUrl" class="w-full h-full object-cover" alt="avatar" />
                    </div>
                    <div v-else class="size-16 rounded-2xl bg-primary/10 text-primary flex items-center justify-center text-2xl font-bold">
                      {{ profileForm.realname?.charAt(0) || userStore.realname?.charAt(0) || 'U' }}
                    </div>
                    <div class="absolute inset-0 rounded-2xl bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                      <span v-if="avatarUploading" class="material-symbols-outlined text-white text-xl animate-spin">progress_activity</span>
                      <span v-else class="material-symbols-outlined text-white text-xl">photo_camera</span>
                    </div>
                    <input ref="avatarInputRef" type="file" accept="image/*" class="hidden" @change="handleAvatarChange" />
                  </div>
                  <div class="ml-4">
                    <div class="text-xl font-bold text-slate-900">{{ profileForm.realname || userStore.realname }}</div>
                    <div class="text-sm text-slate-500">{{ profileForm.position || '员工' }}</div>
                  </div>
                </div>
              </div>

              <!-- Profile Form -->
              <el-form :model="profileForm" label-position="top" class="mt-6">
                <el-row :gutter="24">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="姓名">
                      <el-input v-model="profileForm.realname" placeholder="请输入姓名" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="邮箱">
                      <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="24">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="电话">
                      <el-input v-model="profileForm.phone" placeholder="请输入电话" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="部门">
                      <el-input v-model="profileForm.department" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="24">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="职位">
                      <el-input v-model="profileForm.position" placeholder="请输入职位" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <div class="flex gap-3 pt-4">
                  <button class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20 disabled:opacity-50" :disabled="savingProfile" @click="handleSaveProfile">
                    {{ savingProfile ? '保存中...' : '保存更改' }}
                  </button>
                  <button class="px-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition-colors" @click="resetProfileForm">取消</button>
                </div>
              </el-form>
            </section>

            <!-- Password Change Section -->
            <section class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <h3 class="text-base font-bold mb-6 flex items-center gap-2">
                <span class="w-1 h-4 bg-primary rounded-full"></span>
                密码修改
              </h3>
              <el-form :model="passwordForm" label-position="top" class="max-w-md">
                <el-form-item label="当前密码">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认密码">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <button class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20 disabled:opacity-50" :disabled="submitting" @click="handleChangePassword">
                  {{ submitting ? '修改中...' : '修改密码' }}
                </button>
              </el-form>
            </section>

          </div>

          <!-- Team Management Tab -->
          <div v-else-if="activeTab === 'team'" class="h-full min-h-0">
            <div
              class="flex h-full min-h-0 bg-slate-50/50 overflow-hidden"
              :class="{ 'flex-col': isMobile }"
            >
              <!-- Left Sidebar: Department Tree -->
              <div v-if="!isMobile" class="w-72 shrink-0 bg-white border-r border-slate-200 flex flex-col min-h-0">
                <div class="p-6 border-b border-slate-100 flex items-center justify-between">
                  <h3 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <span class="material-symbols-outlined text-primary text-lg">account_tree</span>
                    组织架构
                  </h3>
                  <button
                    @click="handleAddDept(0)"
                    class="size-8 flex items-center justify-center bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-all"
                    title="添加一级部门"
                  >
                    <span class="material-symbols-outlined text-sm">add</span>
                  </button>
                </div>
                <div class="flex-1 min-h-0 overflow-y-auto p-4">
                  <div v-if="loadingDeptTree" class="text-center py-8">
                    <span class="material-symbols-outlined text-slate-300 animate-spin">progress_activity</span>
                  </div>
                  <el-tree
                    v-else
                    ref="deptTreeRef"
                    :data="deptTree"
                    :props="{ label: 'deptName', children: 'children' }"
                    node-key="deptId"
                    highlight-current
                    default-expand-all
                    :expand-on-click-node="false"
                    @node-click="handleDeptClick"
                  >
                    <template #default="{ data }">
                      <div
                        class="group w-full overflow-hidden px-2 py-1"
                      >
                        <div
                          class="flex items-center justify-between w-full overflow-hidden rounded-xl px-2 py-2 transition-colors"
                          :class="selectedDept?.deptId === data.deptId ? 'bg-primary/10' : 'hover:bg-slate-50'"
                        >
                          <span class="flex items-center gap-2 text-sm min-w-0 overflow-hidden">
                            <span
                              class="material-symbols-outlined text-base shrink-0 transition-colors"
                              :class="selectedDept?.deptId === data.deptId ? 'text-primary' : 'text-slate-400'"
                            >
                              {{ data.children && data.children.length > 0 ? 'folder_open' : 'description' }}
                            </span>
                            <span
                              class="font-semibold truncate transition-colors"
                              :class="selectedDept?.deptId === data.deptId ? 'text-primary' : 'text-slate-700'"
                            >
                              {{ data.deptName }}
                            </span>
                          </span>
                          <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
                          <button
                            @click.stop="handleDeptCommand('addChild', data)"
                            class="size-6 flex items-center justify-center rounded hover:bg-slate-100 text-slate-400 hover:text-primary"
                            title="添加子部门"
                          >
                            <span class="material-symbols-outlined text-sm">add</span>
                          </button>
                          <button
                            @click.stop="handleDeptCommand('edit', data)"
                            class="size-6 flex items-center justify-center rounded hover:bg-slate-100 text-slate-400 hover:text-primary"
                            title="编辑"
                          >
                            <span class="material-symbols-outlined text-sm">edit</span>
                          </button>
                          <button
                            @click.stop="handleDeptCommand('delete', data)"
                            class="size-6 flex items-center justify-center rounded hover:bg-slate-100 text-slate-400 hover:text-red-500"
                            title="删除"
                          >
                            <span class="material-symbols-outlined text-sm">delete</span>
                          </button>
                          </div>
                        </div>
                      </div>
                    </template>
                  </el-tree>
                </div>
              </div>

              <!-- Mobile: Department selector -->
              <div v-if="isMobile" class="flex items-center gap-2 p-4 border-b border-slate-100 bg-white">
                <button @click="showDeptDrawer = true" class="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-xl text-sm max-w-[200px]">
                  <span class="material-symbols-outlined text-slate-400 text-base">account_tree</span>
                  <span class="truncate">{{ selectedDept ? selectedDept.deptName : '选择部门' }}</span>
                </button>
                <button
                  @click="handleAddMember()"
                  :disabled="!selectedDept"
                  class="px-4 py-2 bg-primary text-white rounded-xl text-sm font-bold flex items-center gap-1 hover:bg-primary/90 transition-all disabled:opacity-50"
                >
                  <span class="material-symbols-outlined text-sm">person_add</span>
                  添加成员
                </button>
              </div>

              <!-- Right Content: Employee List -->
              <div class="flex-1 min-h-0 overflow-y-auto" :class="isMobile ? 'p-4' : 'p-8'">
                <div class="max-w-6xl mx-auto w-full space-y-6">
                  <!-- Header -->
                  <div class="flex items-center justify-between">
                    <div>
                      <h2 class="text-2xl font-bold text-slate-900 truncate">
                        {{ selectedDept ? selectedDept.deptName : '组织员工管理' }}
                      </h2>
                      <p class="text-sm text-slate-500 mt-1">管理您的团队成员、部门分配及账号状态</p>
                    </div>
                    <button
                      v-if="!isMobile"
                      @click="handleAddMember()"
                      :disabled="!selectedDept"
                      class="px-6 py-2.5 bg-primary text-white rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all flex items-center gap-2 disabled:opacity-50"
                    >
                      <span class="material-symbols-outlined text-sm">person_add</span>
                      添加员工
                    </button>
                  </div>

                  <!-- Stats Cards -->
                  <div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
                    <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
                      <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">总员工数</p>
                      <p class="text-2xl md:text-3xl font-black text-slate-900">{{ memberList.length }}</p>
                    </div>
                    <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
                      <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">活跃账号</p>
                      <p class="text-2xl md:text-3xl font-black text-emerald-500">{{ memberList.filter(m => m.status === 1).length }}</p>
                    </div>
                    <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
                      <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">待激活</p>
                      <p class="text-2xl md:text-3xl font-black text-amber-500">{{ memberList.filter(m => m.status !== 1 && m.status !== 0).length }}</p>
                    </div>
                    <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
                      <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">部门数量</p>
                      <p class="text-2xl md:text-3xl font-black text-primary">{{ countDepts(deptTree) }}</p>
                    </div>
                  </div>

                  <!-- Employee Table -->
                  <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                    <div v-if="loadingMembers" class="text-center py-16">
                      <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
                    </div>
                    <template v-else-if="memberList.length === 0">
                      <div class="text-center py-16 text-slate-400">
                        <span class="material-symbols-outlined text-3xl mb-2 opacity-50">group_off</span>
                        <p class="text-sm">{{ selectedDept ? '该部门暂无成员' : '请先选择一个部门' }}</p>
                      </div>
                    </template>
                    <template v-else>
                      <!-- Filters -->
                      <div class="p-4 border-b border-slate-100 bg-slate-50/50 flex items-center gap-4">
                        <div class="relative flex-1">
                          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">search</span>
                          <input
                            v-model="memberSearch"
                            type="text"
                            placeholder="搜索员工姓名、邮箱或手机号..."
                            class="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary/50 outline-none transition-shadow"
                          />
                        </div>
                        <select
                          v-model.number="memberRoleId"
                          class="bg-white border border-slate-200 rounded-xl px-4 py-2 text-sm text-slate-600 outline-none focus:ring-2 focus:ring-primary/50"
                        >
                          <option :value="0">所有角色</option>
                          <option v-for="r in allRoleOptions" :key="r.roleId" :value="Number(r.roleId)">
                            {{ r.roleName }}
                          </option>
                        </select>
                      </div>

                      <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                          <thead>
                            <tr class="border-b border-slate-100">
                              <th class="px-6 py-4 text-xs font-bold text-slate-400 uppercase tracking-widest">员工</th>
                              <th class="px-6 py-4 text-xs font-bold text-slate-400 uppercase tracking-widest">部门</th>
                              <th class="px-6 py-4 text-xs font-bold text-slate-400 uppercase tracking-widest">角色</th>
                              <th class="px-6 py-4 text-xs font-bold text-slate-400 uppercase tracking-widest">联系方式</th>
                              <th class="px-6 py-4 text-xs font-bold text-slate-400 uppercase tracking-widest">状态</th>
                              <th class="px-6 py-4 text-xs font-bold text-slate-400 uppercase tracking-widest text-right">操作</th>
                            </tr>
                          </thead>
                          <tbody v-if="filteredMembers.length > 0" class="divide-y divide-slate-50">
                            <tr
                              v-for="member in filteredMembers"
                              :key="member.userId"
                              class="hover:bg-slate-50/50 transition-colors group cursor-pointer"
                              @click="handleMemberRowClick(member)"
                            >
                              <td class="px-6 py-4">
                                <div class="flex items-center gap-3">
                                  <img v-if="member.imgUrl" :src="member.imgUrl" class="size-10 rounded-full object-cover shrink-0 shadow-sm" alt="avatar" />
                                  <div v-else class="size-10 rounded-full flex items-center justify-center text-white text-sm font-bold shrink-0 shadow-sm" :class="getAvatarColor(member.realname)">
                                    {{ (member.realname || member.username || '?').charAt(0) }}
                                  </div>
                                  <div>
                                    <p class="text-sm font-bold text-slate-900 hover:text-primary transition-colors">{{ member.realname || member.username }}</p>
                                    <p class="text-[10px] text-slate-400">{{ member.email || '' }}</p>
                                  </div>
                                </div>
                              </td>
                              <td class="px-6 py-4">
                                <p class="text-sm text-slate-700">{{ selectedDept?.deptName || '-' }}</p>
                              </td>
                              <td class="px-6 py-4">
                                <div class="flex flex-wrap gap-1">
                                  <span v-for="rn in (member.roleNames || [])" :key="rn" class="px-2 py-0.5 bg-amber-50 text-amber-700 rounded text-[10px] font-bold">{{ rn }}</span>
                                  <span v-if="!member.roleNames || member.roleNames.length === 0" class="text-sm text-slate-400">-</span>
                                </div>
                              </td>
                              <td class="px-6 py-4">
                                <div>
                                  <p v-if="member.mobile" class="text-sm text-slate-700">{{ member.mobile }}</p>
                                  <p v-if="member.post" class="text-xs text-slate-400">{{ member.post }}</p>
                                </div>
                              </td>
                              <td class="px-6 py-4">
                                <span :class="[
                                  'px-2 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest',
                                  member.status === 1 ? 'bg-emerald-50 text-emerald-600' : member.status === 0 ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-400'
                                ]">
                                  {{ member.status === 1 ? '活跃' : member.status === 0 ? '禁用' : '未激活' }}
                                </span>
                              </td>
                              <td class="px-6 py-4 text-right">
                                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                  <button
                                    @click.stop="handleEditMember(member)"
                                    class="size-8 flex items-center justify-center rounded-lg hover:bg-white hover:shadow-sm text-slate-400 hover:text-primary transition-all"
                                    title="编辑"
                                  >
                                    <span class="material-symbols-outlined text-sm">edit</span>
                                  </button>
                                  <button
                                    @click.stop="handleToggleStatus(member)"
                                    class="size-8 flex items-center justify-center rounded-lg hover:bg-white hover:shadow-sm text-slate-400 hover:text-amber-500 transition-all"
                                    :title="member.status === 1 ? '停用' : '启用'"
                                  >
                                    <span class="material-symbols-outlined text-sm">{{ member.status === 1 ? 'block' : 'check_circle' }}</span>
                                  </button>
                                </div>
                              </td>
                            </tr>
                          </tbody>
                          <tbody v-else>
                            <tr>
                              <td colspan="6" class="text-center py-16 text-slate-400">
                                <span class="material-symbols-outlined text-3xl mb-2 opacity-50">search_off</span>
                                <div class="text-sm">未找到匹配的员工</div>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </div>

            <!-- Mobile: Department Drawer -->
            <el-drawer
              v-if="isMobile"
              v-model="showDeptDrawer"
              title="组织架构"
              direction="ltr"
              size="80%"
            >
              <button class="w-full mb-3 px-4 py-2 bg-primary/10 text-primary rounded-xl text-sm font-bold flex items-center justify-center gap-2 hover:bg-primary/20 transition-all" @click="handleAddDept(0)">
                <span class="material-symbols-outlined text-sm">add</span>
                添加部门
              </button>
              <el-tree
                :data="deptTree"
                :props="{ label: 'deptName', children: 'children' }"
                node-key="deptId"
                highlight-current
                default-expand-all
                :expand-on-click-node="false"
                @node-click="(data: any) => { handleDeptClick(data); showDeptDrawer = false }"
              >
                <template #default="{ data }">
                  <div class="group flex items-center justify-between w-full pr-1 overflow-hidden">
                    <span class="flex items-center gap-2 text-sm min-w-0 overflow-hidden">
                      <span class="material-symbols-outlined text-base opacity-60 shrink-0">
                        {{ data.children && data.children.length > 0 ? 'folder_open' : 'description' }}
                      </span>
                      <span class="font-medium truncate">{{ data.deptName }}</span>
                    </span>
                    <div class="flex items-center gap-1 shrink-0">
                      <button
                        @click.stop="handleDeptCommand('addChild', data)"
                        class="size-6 flex items-center justify-center rounded hover:bg-slate-100 text-slate-400 hover:text-primary"
                      >
                        <span class="material-symbols-outlined text-sm">add</span>
                      </button>
                      <button
                        @click.stop="handleDeptCommand('edit', data)"
                        class="size-6 flex items-center justify-center rounded hover:bg-slate-100 text-slate-400 hover:text-primary"
                      >
                        <span class="material-symbols-outlined text-sm">edit</span>
                      </button>
                      <button
                        @click.stop="handleDeptCommand('delete', data)"
                        class="size-6 flex items-center justify-center rounded hover:bg-slate-100 text-slate-400 hover:text-red-500"
                      >
                        <span class="material-symbols-outlined text-sm">delete</span>
                      </button>
                    </div>
                  </div>
                </template>
              </el-tree>
            </el-drawer>
          </div>

          <!-- Role Permission Tab -->
          <div v-else-if="activeTab === 'role'" class="bg-slate-50/50 -m-4 md:-m-6 p-4 md:p-8">
            <div class="bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-col overflow-hidden">
              <!-- Header -->
              <div class="p-6 border-b border-slate-100 flex items-center justify-between bg-white">
                <div>
                  <h2 class="text-lg font-bold text-slate-900">角色列表</h2>
                  <p class="text-sm text-slate-500 mt-1">管理系统角色，并为不同角色分配功能权限与数据范围。</p>
                </div>
                <button
                  @click="handleAddRole"
                  class="bg-primary text-white px-4 py-2 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-primary/90 transition-all shadow-sm shadow-primary/20"
                >
                  <span class="material-symbols-outlined text-sm">add</span>
                  新增角色
                </button>
              </div>

              <!-- Table -->
              <div class="flex-1 overflow-auto p-6">
                <div v-if="loadingRoles" class="text-center py-16">
                  <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
                </div>
                <div v-else-if="roleList.length === 0" class="text-center py-16 text-slate-400">
                  <span class="material-symbols-outlined text-3xl mb-2 opacity-50">admin_panel_settings</span>
                  <p class="text-sm">暂无角色，请创建</p>
                </div>
                <table v-else class="w-full text-left text-sm">
                  <thead class="bg-slate-50/80 border-b border-slate-100 text-slate-500">
                    <tr>
                      <th class="px-6 py-4 font-bold rounded-tl-xl">角色名称</th>
                      <th class="px-6 py-4 font-bold">角色描述</th>
                      <th class="px-6 py-4 font-bold">成员数量</th>
                      <th class="px-6 py-4 font-bold text-right rounded-tr-xl">操作</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-50">
                    <tr v-for="role in roleList" :key="role.roleId" class="hover:bg-slate-50/80 transition-colors group">
                      <td class="px-6 py-4">
                        <div class="flex items-center gap-2">
                          <span class="font-bold text-slate-900">{{ role.roleName }}</span>
                          <span v-if="role.realm === 'super_admin'" class="px-2 py-0.5 bg-slate-100 text-slate-500 text-[10px] font-bold rounded-md uppercase tracking-wider">系统预设</span>
                        </div>
                      </td>
                      <td class="px-6 py-4 text-slate-500 whitespace-normal min-w-[200px]">
                        {{ role.description || '暂无描述' }}
                      </td>
                      <td class="px-6 py-4">
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-slate-100 text-slate-600 text-xs font-medium">
                          <span class="material-symbols-outlined text-[14px]">group</span>
                          {{ role.userCount }} 人
                        </span>
                      </td>
                      <td class="px-6 py-4 text-right">
                        <div class="flex items-center justify-end gap-3">
                          <button
                            @click="openRoleDrawer(role, 'members')"
                            class="text-primary hover:text-primary/80 font-bold transition-colors text-xs flex items-center gap-1"
                          >
                            <span class="material-symbols-outlined text-[14px]">manage_accounts</span>
                            管理用户
                          </button>
                          <button
                            @click="openRoleDrawer(role, 'permissions')"
                            class="text-primary hover:text-primary/80 font-bold transition-colors text-xs flex items-center gap-1"
                          >
                            <span class="material-symbols-outlined text-[14px]">admin_panel_settings</span>
                            管理权限
                          </button>
                          <button
                            @click="handleEditRole(role)"
                            class="text-slate-400 hover:text-primary font-bold transition-colors text-xs"
                          >
                            编辑
                          </button>
                          <button
                            v-if="role.realm !== 'super_admin'"
                            @click="handleDeleteRole(role)"
                            class="text-slate-400 hover:text-red-600 font-bold transition-colors text-xs"
                          >
                            删除
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- AI Agent Tab -->
          <div v-else-if="activeTab === 'agent'">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-medium">AI 智能体管理</h3>
              <el-button type="primary" @click="showAgentDialog = true">
                <el-icon class="mr-1"><Plus /></el-icon>
                添加智能体
              </el-button>
            </div>
            <div v-if="agentStore.loading" class="text-center py-8">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div v-else-if="agentStore.allAgents.length === 0" class="text-center py-8 text-slate-400">
              暂无智能体
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="agent in agentStore.allAgents"
                :key="agent.agentId"
                class="flex items-center justify-between p-4 bg-slate-50 rounded-lg border border-slate-200"
              >
                <div class="flex items-center">
                  <el-icon :size="24" class="text-primary"><Promotion /></el-icon>
                  <div class="ml-3">
                    <div class="font-medium">{{ agent.label }}</div>
                    <div class="text-sm text-slate-500 truncate max-w-md">{{ agent.prompt }}</div>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <el-switch
                    :model-value="!!agent.enabled"
                    :disabled="agentStore.updating"
                    @change="(val: boolean) => handleToggleAgent(agent.agentId, val)"
                  />
                  <el-button text @click="handleEditAgent(agent)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button text type="danger" @click="handleDeleteAgent(agent)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- Custom Field Tab -->
          <div v-else-if="activeTab === 'customField'">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-medium">自定义字段管理</h3>
              <el-button type="primary" @click="handleOpenFieldDialog">
                <el-icon class="mr-1"><Plus /></el-icon>
                添加字段
              </el-button>
            </div>

            <!-- Entity Type Tabs -->
            <el-tabs v-model="activeEntityType" class="mb-4" @tab-change="loadCustomFields">
              <el-tab-pane label="客户字段" name="customer" />
              <el-tab-pane label="联系人字段" name="contact" />
            </el-tabs>

            <div v-if="loadingFields" class="text-center py-8">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div v-else-if="customFields.length === 0" class="text-center py-8 text-slate-400">
              暂无自定义字段
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="field in customFields"
                :key="field.fieldId"
                class="flex items-center justify-between p-4 bg-slate-50 rounded-lg border border-slate-200"
              >
                <div class="flex items-center flex-1">
                  <div class="mr-4">
                    <div class="font-medium">
                      {{ field.fieldLabel }}
                      <el-tag v-if="field.isRequired" size="small" type="danger" class="ml-2">必填</el-tag>
                    </div>
                    <div class="text-sm text-slate-500 mt-1">
                      <span>标识: {{ field.fieldName }}</span>
                      <el-tag size="small" class="ml-2">{{ getFieldTypeLabel(field.fieldType) }}</el-tag>
                      <span v-if="field.options && field.options.length > 0" class="ml-2">
                        选项: {{ field.options.map(o => o.label).join(', ') }}
                      </span>
                    </div>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <el-switch
                    :model-value="field.status === 1"
                    @change="(val: boolean) => handleToggleFieldStatus(field, val)"
                  />
                  <el-button text @click="handleEditField(field)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-popconfirm
                    title="删除字段将同时删除数据库列，确定继续吗？"
                    confirm-button-text="删除"
                    cancel-button-text="取消"
                    @confirm="handleDeleteField(field)"
                  >
                    <template #reference>
                      <el-button text type="danger">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>

          <!-- Enterprise Info Tab -->
          <div v-else-if="activeTab === 'enterprise'" class="max-w-4xl mx-auto space-y-8">
            <section class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <h3 class="text-base font-bold mb-6 flex items-center gap-2">
                <span class="w-1 h-4 bg-primary rounded-full"></span>
                企业信息
                <el-tag v-if="enterpriseForm.updateTime" size="small" type="info" class="ml-auto font-normal">
                  最后更新: {{ enterpriseForm.updateTime }}
                </el-tag>
              </h3>

              <!-- Logo Upload -->
              <div class="mb-6">
                <label class="block text-sm font-medium text-slate-700 mb-2">企业 Logo</label>
                <div class="flex items-center gap-6">
                  <div
                    @click="triggerLogoUpload"
                    class="size-20 rounded-xl border-2 border-dashed border-slate-300 flex items-center justify-center cursor-pointer hover:border-primary hover:bg-primary/5 transition-all overflow-hidden"
                    :class="{ '!border-solid !border-slate-200': enterpriseForm.logoUrl }"
                  >
                    <img
                      v-if="enterpriseForm.logoUrl"
                      :src="enterpriseForm.logoUrl"
                      class="w-full h-full object-cover"
                      alt="企业Logo"
                    />
                    <div v-else class="text-center">
                      <el-icon :size="24" class="text-slate-400"><Upload /></el-icon>
                      <p class="text-[10px] text-slate-400 mt-1">上传Logo</p>
                    </div>
                  </div>
                  <div class="text-xs text-slate-500">
                    <p>建议尺寸: 200x200px</p>
                    <p>支持 JPG、PNG 格式</p>
                    <el-button
                      v-if="enterpriseForm.logoUrl"
                      text
                      type="danger"
                      size="small"
                      @click="removeLogo"
                    >
                      移除 Logo
                    </el-button>
                  </div>
                </div>
                <input
                  ref="logoInputRef"
                  type="file"
                  accept="image/jpeg,image/png,image/jpg"
                  class="hidden"
                  @change="handleLogoChange"
                />
              </div>

              <!-- Enterprise Name -->
              <el-form label-position="top">
                <el-form-item label="企业名称">
                  <el-input
                    v-model="enterpriseForm.name"
                    placeholder="请输入企业名称"
                    maxlength="50"
                    show-word-limit
                  />
                </el-form-item>
              </el-form>

              <div class="flex justify-end pt-4 border-t border-slate-100">
                <el-button
                  type="primary"
                  @click="saveEnterpriseConfig"
                  :loading="savingEnterprise"
                >
                  保存设置
                </el-button>
              </div>
            </section>
          </div>

          <!-- API/AI Tab -->
          <div v-else-if="activeTab === 'api'" class="space-y-6">
            <el-card shadow="never" class="!border-slate-200">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="font-medium">AI 大模型配置</span>
                  <el-tag v-if="aiConfigForm.updateTime" size="small" type="info">
                    最后更新: {{ formatTime(aiConfigForm.updateTime) }}
                  </el-tag>
                </div>
              </template>

              <el-form :model="aiConfigForm" label-position="top" class="max-w-2xl">
                <!-- AI 服务提供商 -->
                <el-form-item label="AI 服务提供商">
                  <el-select
                    v-model="aiConfigForm.provider"
                    class="w-full"
                    @change="handleProviderChange"
                  >
                    <el-option
                      v-for="preset in providerPresets"
                      :key="preset.value"
                      :label="preset.label"
                      :value="preset.value"
                    />
                  </el-select>
                </el-form-item>

                <!-- API 地址 -->
                <el-form-item label="API 基础地址">
                  <el-input
                    v-model="aiConfigForm.apiUrl"
                    placeholder="https://api.openai.com/v1"
                  >
                    <template #prepend>URL</template>
                  </el-input>
                  <div class="text-xs text-slate-400 mt-1">
                    OpenAI 兼容接口的基础 URL，末尾不要加斜杠
                  </div>
                </el-form-item>

                <!-- API Key -->
                <el-form-item label="API 密钥">
                  <div class="flex gap-2 w-full">
                    <el-input
                      v-model="aiConfigForm.apiKey"
                      :type="showApiKey ? 'text' : 'password'"
                      placeholder="sk-xxxxxx"
                      class="flex-1"
                    >
                      <template #prepend>Key</template>
                      <template #suffix>
                        <el-icon
                          class="cursor-pointer"
                          @click="showApiKey = !showApiKey"
                        >
                          <View v-if="showApiKey" />
                          <Hide v-else />
                        </el-icon>
                      </template>
                    </el-input>
                    <el-button
                      :loading="testingConnection"
                      @click="handleTestConnection"
                    >
                      <el-icon class="mr-1"><Connection /></el-icon>
                      测试连接
                    </el-button>
                  </div>
                  <div v-if="connectionTestResult" class="mt-2">
                    <el-alert
                      :type="connectionTestResult.success ? 'success' : 'error'"
                      :closable="false"
                      show-icon
                    >
                      <template #title>
                        {{ connectionTestResult.success ? '连接成功' : '连接失败' }}
                        <span class="text-slate-500 ml-2">
                          ({{ connectionTestResult.responseTime }}ms)
                        </span>
                      </template>
                      <template #default>
                        {{ connectionTestResult.message }}
                      </template>
                    </el-alert>
                  </div>
                </el-form-item>

                <!-- 模型选择 -->
                <el-form-item label="模型">
                  <el-select
                    v-model="aiConfigForm.model"
                    class="w-full"
                    filterable
                    allow-create
                  >
                    <el-option
                      v-for="model in currentProviderModels"
                      :key="model"
                      :label="model"
                      :value="model"
                    />
                  </el-select>
                  <div class="text-xs text-slate-400 mt-1">
                    可以从列表选择或直接输入自定义模型名称
                  </div>
                </el-form-item>

                <!-- Temperature -->
                <el-form-item label="Temperature (创造性)">
                  <div class="flex items-center gap-4 w-full">
                    <el-slider
                      v-model="aiConfigForm.temperature"
                      :min="0"
                      :max="2"
                      :step="0.1"
                      :format-tooltip="(val: number) => val.toFixed(1)"
                      class="flex-1"
                    />
                    <el-input-number
                      v-model="aiConfigForm.temperature"
                      :min="0"
                      :max="2"
                      :step="0.1"
                      :precision="1"
                      class="w-24"
                    />
                  </div>
                  <div class="text-xs text-slate-400 mt-1">
                    值越低回复越确定，值越高回复越有创造性。推荐值：0.7
                  </div>
                </el-form-item>

                <!-- Max Tokens -->
                <el-form-item label="最大 Token 数">
                  <el-input-number
                    v-model="aiConfigForm.maxTokens"
                    :min="100"
                    :max="128000"
                    :step="100"
                    class="w-full"
                  />
                  <div class="text-xs text-slate-400 mt-1">
                    单次对话允许的最大 Token 数量，包括输入和输出
                  </div>
                </el-form-item>

                <!-- 操作按钮 -->
                <div class="flex gap-3 pt-4 border-t border-slate-200">
                  <el-button
                    type="primary"
                    :loading="savingAiConfig"
                    @click="handleSaveAiConfig"
                  >
                    <el-icon class="mr-1"><Document /></el-icon>
                    保存 AI 配置
                  </el-button>
                  <el-button @click="loadAiConfig">重置</el-button>
                </div>
              </el-form>
            </el-card>

            <!-- 配置说明卡片 -->
            <el-card shadow="never" class="!border-slate-200">
              <template #header>
                <span class="font-medium">配置说明</span>
              </template>
              <div class="text-sm text-slate-600 space-y-2">
                <p><strong>OpenAI:</strong> 使用 OpenAI 官方 API，需要有效的 API Key</p>
                <p><strong>阿里云 DashScope:</strong> 使用阿里云通义千问系列模型，API 地址为 https://dashscope.aliyuncs.com/compatible-mode/</p>
                <p><strong>自定义:</strong> 任何 OpenAI 兼容的 API 服务，如 LocalAI、Ollama 等</p>
              </div>
            </el-card>
          </div>

          <!-- Object Storage Tab -->
          <div v-else-if="activeTab === 'storage'" class="space-y-6">
            <el-card shadow="never" class="!border-slate-200">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="font-medium">MinIO 对象存储</span>
                  <el-tag v-if="minioConfig.enabled" type="success" size="small">已启用</el-tag>
                  <el-tag v-else type="info" size="small">未启用</el-tag>
                </div>
              </template>

              <div v-if="loadingMinioConfig" class="text-center py-8">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <div v-else class="space-y-6">
                <div class="flex items-center gap-4 p-4 bg-slate-50 rounded-lg">
                  <div class="w-12 h-12 rounded-lg bg-orange-100 flex items-center justify-center">
                    <el-icon :size="24" class="text-orange-500"><Box /></el-icon>
                  </div>
                  <div class="flex-1">
                    <div class="font-medium">MinIO 管理控制台</div>
                    <div class="text-sm text-slate-500 mt-1">
                      {{ minioConfig.consoleUrl || '未配置' }}
                    </div>
                  </div>
                  <el-button
                    type="primary"
                    :disabled="!minioConfig.consoleUrl"
                    @click="handleOpenMinioConsole"
                  >
                    <el-icon class="mr-1"><Link /></el-icon>
                    进入管理后台
                  </el-button>
                </div>

                <el-alert type="info" :closable="false" show-icon>
                  <template #title>SSO 单点登录</template>
                  <template #default>
                    系统已配置 OIDC 单点登录。登录 CRM 系统后，点击上方按钮可直接进入 MinIO 管理后台，无需再次输入密码。
                  </template>
                </el-alert>

                <div class="text-sm text-slate-500">
                  <p class="mb-2"><strong>说明：</strong></p>
                  <ul class="list-disc list-inside space-y-1">
                    <li>MinIO 提供 S3 兼容的对象存储服务</li>
                    <li>用于存储知识库文档、附件等文件</li>
                    <li>如需修改存储配置，请联系系统管理员</li>
                  </ul>
                </div>
              </div>
            </el-card>
          </div>

          <!-- WeKnora Knowledge Service Tab -->
          <div v-else-if="activeTab === 'weknora'" class="space-y-6">
            <el-card shadow="never" class="!border-slate-200">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="font-medium">WeKnora 知识库服务配置</span>
                  <el-tag v-if="weknoraConfigForm.updateTime" size="small" type="info">
                    最后更新: {{ formatTime(weknoraConfigForm.updateTime) }}
                  </el-tag>
                </div>
              </template>

              <div v-if="loadingWeknoraConfig" class="text-center py-8">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <el-form v-else :model="weknoraConfigForm" label-position="top" class="max-w-2xl">
                <!-- 启用开关 -->
                <el-form-item label="启用 WeKnora">
                  <div class="flex items-center gap-4">
                    <el-switch v-model="weknoraConfigForm.enabled" />
                    <span class="text-sm text-slate-500">
                      {{ weknoraConfigForm.enabled ? '已启用 - 文档将同步到知识库' : '已禁用 - 仅使用本地存储' }}
                    </span>
                  </div>
                </el-form-item>

                <!-- API 基础地址 -->
                <el-form-item label="API 基础地址">
                  <el-input
                    v-model="weknoraConfigForm.baseUrl"
                    placeholder="http://localhost:8080/api/v1"
                    :disabled="!weknoraConfigForm.enabled"
                  >
                    <template #prepend>URL</template>
                  </el-input>
                  <div class="text-xs text-slate-400 mt-1">
                    WeKnora 服务的 API 地址，末尾不要加斜杠
                  </div>
                </el-form-item>

                <!-- API Key -->
                <el-form-item label="API 密钥">
                  <div class="flex gap-2 w-full">
                    <el-input
                      v-model="weknoraConfigForm.apiKey"
                      :type="showWeknoraApiKey ? 'text' : 'password'"
                      placeholder="sk-xxxxxx"
                      class="flex-1"
                      :disabled="!weknoraConfigForm.enabled"
                    >
                      <template #prepend>Key</template>
                      <template #suffix>
                        <el-icon
                          class="cursor-pointer"
                          @click="showWeknoraApiKey = !showWeknoraApiKey"
                        >
                          <View v-if="showWeknoraApiKey" />
                          <Hide v-else />
                        </el-icon>
                      </template>
                    </el-input>
                    <el-button
                      :loading="testingWeknoraConnection"
                      :disabled="!weknoraConfigForm.enabled"
                      @click="handleTestWeknoraConnection"
                    >
                      <el-icon class="mr-1"><Connection /></el-icon>
                      测试连接
                    </el-button>
                  </div>
                  <div v-if="weknoraConnectionTestResult" class="mt-2">
                    <el-alert
                      :type="weknoraConnectionTestResult.success ? 'success' : 'error'"
                      :closable="false"
                      show-icon
                    >
                      <template #title>
                        {{ weknoraConnectionTestResult.success ? '连接成功' : '连接失败' }}
                        <span class="text-slate-500 ml-2">
                          ({{ weknoraConnectionTestResult.responseTime }}ms)
                        </span>
                      </template>
                      <template #default>
                        {{ weknoraConnectionTestResult.message }}
                      </template>
                    </el-alert>
                  </div>
                </el-form-item>

                <!-- 知识库 ID -->
                <el-form-item label="默认知识库 ID">
                  <el-input
                    v-model="weknoraConfigForm.knowledgeBaseId"
                    placeholder="请输入知识库 ID"
                    :disabled="!weknoraConfigForm.enabled"
                  />
                  <div class="text-xs text-slate-400 mt-1">
                    CRM 文档将上传到此知识库，可从 WeKnora 管理界面获取
                  </div>
                </el-form-item>

                <!-- 搜索配置 -->
                <el-divider content-position="left">搜索配置</el-divider>

                <!-- 最大匹配数 -->
                <el-form-item label="最大匹配结果数">
                  <el-input-number
                    v-model="weknoraConfigForm.matchCount"
                    :min="1"
                    :max="50"
                    :disabled="!weknoraConfigForm.enabled"
                    class="w-full"
                  />
                  <div class="text-xs text-slate-400 mt-1">
                    语义搜索返回的最大文档片段数量
                  </div>
                </el-form-item>

                <!-- 向量相似度阈值 -->
                <el-form-item label="向量相似度阈值">
                  <div class="flex items-center gap-4 w-full">
                    <el-slider
                      v-model="weknoraConfigForm.vectorThreshold"
                      :min="0"
                      :max="1"
                      :step="0.05"
                      :format-tooltip="(val: number) => val.toFixed(2)"
                      :disabled="!weknoraConfigForm.enabled"
                      class="flex-1"
                    />
                    <el-input-number
                      v-model="weknoraConfigForm.vectorThreshold"
                      :min="0"
                      :max="1"
                      :step="0.05"
                      :precision="2"
                      :disabled="!weknoraConfigForm.enabled"
                      class="w-24"
                    />
                  </div>
                  <div class="text-xs text-slate-400 mt-1">
                    只返回相似度高于此阈值的结果，值越高匹配越精确
                  </div>
                </el-form-item>

                <!-- 自动 RAG 开关 -->
                <el-form-item label="自动 RAG">
                  <div class="flex items-center gap-4">
                    <el-switch
                      v-model="weknoraConfigForm.autoRagEnabled"
                      :disabled="!weknoraConfigForm.enabled"
                    />
                    <span class="text-sm text-slate-500">
                      {{ weknoraConfigForm.autoRagEnabled ? '启用 - 每次对话自动检索相关文档' : '禁用 - 需手动触发知识库搜索' }}
                    </span>
                  </div>
                </el-form-item>

                <!-- 操作按钮 -->
                <div class="flex gap-3 pt-4 border-t border-slate-200">
                  <el-button
                    type="primary"
                    :loading="savingWeknoraConfig"
                    @click="handleSaveWeknoraConfig"
                  >
                    <el-icon class="mr-1"><Document /></el-icon>
                    保存配置
                  </el-button>
                  <el-button @click="loadWeknoraConfig">重置</el-button>
                </div>
              </el-form>
            </el-card>

            <!-- 配置说明卡片 -->
            <el-card shadow="never" class="!border-slate-200">
              <template #header>
                <span class="font-medium">配置说明</span>
              </template>
              <div class="text-sm text-slate-600 space-y-2">
                <p><strong>WeKnora:</strong> 基于向量数据库的知识库服务，支持文档语义搜索和 RAG（检索增强生成）</p>
                <p><strong>自动 RAG:</strong> 启用后，AI 对话时会自动检索知识库中的相关文档，提供更准确的回答</p>
                <p><strong>相似度阈值:</strong> 用于过滤低相关性的搜索结果，建议设置在 0.5-0.7 之间</p>
              </div>
            </el-card>
          </div>

          <!-- Placeholder for other tabs -->
          <div v-else class="text-center py-16 text-slate-400">
            <el-icon :size="48" class="mb-4"><Tools /></el-icon>
            <p class="text-lg">功能开发中</p>
            <p class="text-sm mt-2">该模块正在建设中，敬请期待</p>
          </div>
      </div>
    </div>

    <!-- Agent Dialog -->
    <el-dialog
      v-model="showAgentDialog"
      :title="editingAgent ? '编辑智能体' : '添加智能体'"
      :width="isMobile ? '95%' : '500px'"
      :fullscreen="isMobile"
    >
      <el-form :model="agentForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="agentForm.label" placeholder="智能体名称" />
        </el-form-item>
        <el-form-item label="提示词">
          <el-input v-model="agentForm.prompt" type="textarea" :rows="4" placeholder="AI 提示词或快捷指令" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="agentForm.iconName" placeholder="图标名称（如 Promotion）" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="agentForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAgentDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveAgent">保存</el-button>
      </template>
    </el-dialog>

    <!-- Department Dialog -->
    <el-dialog
      v-model="showDeptDialog"
      :title="editingDept ? '编辑部门' : '添加部门'"
      :width="isMobile ? '95%' : '420px'"
      :fullscreen="isMobile"
    >
      <el-form :model="deptForm" label-width="80px">
        <el-form-item label="部门名称" required>
          <el-input v-model="deptForm.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="deptForm.sortOrder" :min="0" :max="999" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeptDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingDept" @click="handleSaveDept">保存</el-button>
      </template>
    </el-dialog>

    <!-- Member Dialog -->
    <el-dialog
      v-model="showAddMemberDialog"
      :title="editingMember ? '编辑员工' : '新增员工'"
      :width="isMobile ? '95%' : '650px'"
      :fullscreen="isMobile"
    >
      <el-form :model="memberForm" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="姓名" required>
              <el-input v-model="memberForm.realname" placeholder="请输入员工姓名" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="职位">
              <el-input v-model="memberForm.post" placeholder="如：销售经理" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="用户名" :required="!editingMember">
              <el-input v-model="memberForm.username" :disabled="!!editingMember" :placeholder="editingMember ? '' : '用于系统登录'" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="登录密码" :required="!editingMember">
              <el-input v-model="memberForm.password" type="password" show-password :placeholder="editingMember ? '留空则不修改' : '默认密码 123456'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="所属部门">
              <el-tree-select
                v-model="memberForm.deptId"
                :data="deptTree"
                :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
                placeholder="请选择部门"
                clearable
                check-strictly
                :render-after-expand="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="直属上级">
              <el-select v-model="memberForm.parentId" placeholder="无 (顶级员工)" clearable filterable style="width: 100%">
                <el-option :value="null" label="无 (顶级员工)" />
                <el-option
                  v-for="u in parentOptions"
                  :key="u.userId"
                  :label="u.realname || u.username"
                  :value="u.userId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="电子邮箱">
              <el-input v-model="memberForm.email" placeholder="example@wukong.ai" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号码">
              <el-input v-model="memberForm.mobile" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- Status -->
      <div class="mb-4">
        <label class="text-sm font-medium text-slate-700 mb-2 block">状态</label>
        <el-radio-group v-model="memberForm.status">
          <el-radio :value="1">活跃</el-radio>
          <el-radio :value="0">离职/停用</el-radio>
        </el-radio-group>
      </div>

      <!-- Role Selection -->
      <div class="mt-4">
        <div class="flex items-center justify-between mb-3">
          <label class="text-sm font-bold text-slate-900">系统角色权限</label>
          <span class="text-xs text-primary">已选择 {{ memberForm.roleIds.length }} 个角色</span>
        </div>
        <div class="border border-slate-200 rounded-xl p-4 max-h-48 overflow-y-auto bg-slate-50/50">
          <el-checkbox-group v-model="memberForm.roleIds">
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-3">
              <el-checkbox
                v-for="r in allRoleOptions"
                :key="r.roleId"
                :label="r.roleName"
                :value="r.roleId"
                class="!mr-0"
              />
            </div>
          </el-checkbox-group>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3 w-full">
          <el-button class="flex-1" size="large" @click="showAddMemberDialog = false">取消</el-button>
          <el-button class="flex-1" size="large" type="primary" :loading="submittingMember" @click="handleSaveMember">保存员工信息</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Custom Field Dialog -->
    <el-dialog
      v-model="showFieldDialog"
      :title="editingField ? '编辑自定义字段' : '添加自定义字段'"
      :width="isMobile ? '95%' : '550px'"
      :fullscreen="isMobile"
    >
      <el-form :model="fieldForm" label-width="100px">
        <el-form-item label="字段标签" required>
          <el-input v-model="fieldForm.fieldLabel" placeholder="显示名称，如：合同类型" />
        </el-form-item>
        <el-form-item v-if="!editingField" label="字段标识" required>
          <el-input v-model="fieldForm.fieldName" placeholder="英文标识，如：contractType" />
          <div class="text-xs text-slate-400 mt-1">只能包含字母、数字、下划线，以字母开头</div>
        </el-form-item>
        <el-form-item v-if="!editingField" label="字段类型" required>
          <el-select v-model="fieldForm.fieldType" class="w-full" @change="handleFieldTypeChange">
            <el-option label="单行文本" value="text" />
            <el-option label="多行文本" value="textarea" />
            <el-option label="数字" value="number" />
            <el-option label="日期" value="date" />
            <el-option label="日期时间" value="datetime" />
            <el-option label="单选下拉" value="select" />
            <el-option label="多选下拉" value="multiselect" />
            <el-option label="开关" value="checkbox" />
          </el-select>
        </el-form-item>
        <el-form-item label="占位提示">
          <el-input v-model="fieldForm.placeholder" placeholder="输入框提示文字" />
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="fieldForm.defaultValue" placeholder="字段默认值" />
        </el-form-item>

        <!-- Options for select types -->
        <el-form-item v-if="['select', 'multiselect'].includes(fieldForm.fieldType)" label="选项配置">
          <div class="w-full space-y-2">
            <div v-for="(opt, idx) in fieldForm.options" :key="idx" class="flex gap-2">
              <el-input v-model="opt.value" placeholder="值" class="w-1/3" />
              <el-input v-model="opt.label" placeholder="显示文字" class="flex-1" />
              <el-button text type="danger" @click="fieldForm.options.splice(idx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button text @click="fieldForm.options.push({ value: '', label: '' })">
              + 添加选项
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="必填">
          <el-switch v-model="fieldForm.isRequired" />
        </el-form-item>
        <el-form-item label="可搜索">
          <el-switch v-model="fieldForm.isSearchable" />
        </el-form-item>
        <el-form-item label="列表显示">
          <el-switch v-model="fieldForm.isShowInList" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFieldDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveField">保存</el-button>
      </template>
    </el-dialog>

    <!-- Role Create/Edit Dialog -->
    <el-dialog
      v-model="showRoleDialog"
      :title="editingRole ? '编辑角色' : '创建角色'"
      :width="isMobile ? '95%' : '460px'"
      :fullscreen="isMobile"
    >
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingRole" @click="handleSaveRole">保存</el-button>
      </template>
    </el-dialog>

    <!-- Combined Role Drawer (Members + Permissions) -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showRoleDrawer" class="fixed inset-0 z-[100]">
          <!-- Backdrop -->
          <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click="showRoleDrawer = false" />

          <!-- Drawer Panel -->
          <Transition
            enter-active-class="transition duration-300 ease-out"
            enter-from-class="translate-x-full"
            enter-to-class="translate-x-0"
            leave-active-class="transition duration-200 ease-in"
            leave-from-class="translate-x-0"
            leave-to-class="translate-x-full"
          >
            <div v-if="showRoleDrawer" class="fixed inset-y-0 right-0 w-full max-w-3xl bg-slate-50 shadow-2xl flex flex-col">
              <!-- Drawer Header -->
              <div class="bg-white px-8 pt-8 pb-0 border-b border-slate-200 shrink-0">
                <div class="flex items-start justify-between mb-6">
                  <div>
                    <div class="flex items-center gap-3 mb-2">
                      <h2 class="text-2xl font-bold text-slate-900">{{ roleDrawerRole?.roleName }}</h2>
                      <span v-if="roleDrawerRole?.realm === 'super_admin'" class="px-2 py-1 bg-slate-100 text-slate-600 text-[10px] font-bold rounded-md uppercase tracking-wider border border-slate-200">
                        系统预设角色
                      </span>
                    </div>
                    <p class="text-sm text-slate-500">{{ roleDrawerRole?.description || '暂无描述' }}</p>
                  </div>
                  <button
                    @click="showRoleDrawer = false"
                    class="size-8 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 transition-colors"
                  >
                    <span class="material-symbols-outlined">close</span>
                  </button>
                </div>

                <!-- Tabs -->
                <div class="flex gap-8 relative">
                  <button
                    @click="roleDrawerTab = 'members'"
                    :class="[
                      'pb-4 text-sm font-bold transition-colors relative',
                      roleDrawerTab === 'members' ? 'text-primary' : 'text-slate-500 hover:text-slate-800'
                    ]"
                  >
                    角色成员
                    <span class="ml-2 px-2 py-0.5 bg-slate-100 text-slate-600 rounded-full text-xs">{{ roleDrawerRole?.userCount || 0 }}</span>
                    <div v-if="roleDrawerTab === 'members'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary rounded-t-full"></div>
                  </button>
                  <button
                    @click="roleDrawerTab = 'permissions'"
                    :class="[
                      'pb-4 text-sm font-bold transition-colors relative',
                      roleDrawerTab === 'permissions' ? 'text-primary' : 'text-slate-500 hover:text-slate-800'
                    ]"
                  >
                    权限配置
                    <div v-if="roleDrawerTab === 'permissions'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary rounded-t-full"></div>
                  </button>
                </div>
              </div>

              <!-- Drawer Content -->
              <div class="flex-1 overflow-y-auto p-8">
                <!-- Members Tab -->
                <div v-if="roleDrawerTab === 'members'" class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
                  <div class="p-5 border-b border-slate-100 flex items-center justify-between bg-white">
                    <div class="relative">
                      <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">search</span>
                      <input
                        v-model="roleUserSearch"
                        type="text"
                        placeholder="搜索成员姓名或邮箱..."
                        class="pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/20 focus:border-primary focus:bg-white outline-none w-72 transition-all"
                      />
                    </div>
                    <button
                      @click="showAddRoleUserDialog = true"
                      class="bg-primary text-white px-4 py-2 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-primary/90 transition-all shadow-sm shadow-primary/20"
                    >
                      <span class="material-symbols-outlined text-sm">person_add</span>
                      添加成员
                    </button>
                  </div>
                  <div class="overflow-x-auto">
                    <div v-if="loadingRoleUsers" class="text-center py-16">
                      <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
                    </div>
                    <table v-else-if="filteredRoleUsers.length > 0" class="w-full text-left text-sm">
                      <thead class="bg-slate-50/50 border-b border-slate-100 text-slate-500">
                        <tr>
                          <th class="px-6 py-4 font-bold">成员信息</th>
                          <th class="px-6 py-4 font-bold">邮箱</th>
                          <th class="px-6 py-4 font-bold text-right">操作</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-slate-50">
                        <tr v-for="user in filteredRoleUsers" :key="user.userId" class="hover:bg-slate-50/80 transition-colors group">
                          <td class="px-6 py-4">
                            <div class="flex items-center gap-3">
                              <div class="size-9 rounded-full flex items-center justify-center text-white font-bold shadow-sm border border-white/20" :class="getAvatarColor(user.realname || user.username)">
                                {{ (user.realname || user.username || '?').charAt(0) }}
                              </div>
                              <div class="font-bold text-slate-900">{{ user.realname || user.username }}</div>
                            </div>
                          </td>
                          <td class="px-6 py-4 text-slate-500">{{ user.email || '-' }}</td>
                          <td class="px-6 py-4 text-right">
                            <button
                              @click="handleRemoveRoleUser(user)"
                              class="text-slate-400 hover:text-red-600 font-medium transition-colors p-2 rounded-lg hover:bg-red-50 opacity-0 group-hover:opacity-100"
                            >
                              <span class="material-symbols-outlined text-sm">person_remove</span>
                            </button>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                    <div v-else class="py-10 text-center text-slate-400">
                      <span class="material-symbols-outlined text-3xl mb-2 opacity-50">group_off</span>
                      <p class="text-xs">暂无成员</p>
                    </div>
                  </div>
                </div>

                <!-- Permissions Tab -->
                <div v-else-if="roleDrawerTab === 'permissions'" class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
                  <div class="p-5 border-b border-slate-100 flex items-center justify-between bg-white sticky top-0 z-10">
                    <div>
                      <h3 class="text-base font-bold text-slate-900">功能权限配置</h3>
                      <p class="text-xs text-slate-500 mt-1">配置该角色在各个业务模块中的操作权限及数据可见范围。</p>
                    </div>
                    <button
                      @click="handleSavePermissions"
                      :disabled="savingPermissions"
                      class="bg-primary text-white px-5 py-2 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-primary/90 transition-all shadow-sm shadow-primary/20 disabled:opacity-50"
                    >
                      <span v-if="savingPermissions" class="size-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                      <span v-else class="material-symbols-outlined text-sm">save</span>
                      保存配置
                    </button>
                  </div>
                  <div v-if="loadingPermissions" class="text-center py-16">
                    <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
                  </div>
                  <div v-else class="p-6 space-y-6 bg-slate-50/30">
                    <div v-for="moduleGroup in permissionList" :key="moduleGroup.module" class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm hover:border-primary/30 transition-colors">
                      <div class="bg-slate-50/80 px-6 py-4 flex items-center justify-between border-b border-slate-100">
                        <div class="flex items-center gap-3">
                          <span class="text-sm font-bold text-slate-900">{{ moduleGroup.moduleName }}</span>
                        </div>
                        <span class="text-xs font-medium text-slate-400">{{ moduleGroup.actions.filter((a: any) => a.enabled).length }}/{{ moduleGroup.actions.length }} 已启用</span>
                      </div>
                      <div class="p-6 grid grid-cols-1 sm:grid-cols-2 gap-6">
                        <div v-for="action in moduleGroup.actions" :key="action.action" class="flex flex-col gap-3">
                          <label class="flex items-center gap-3 text-sm text-slate-700 cursor-pointer hover:text-primary transition-colors font-bold group/action">
                            <div class="relative flex items-center">
                              <input type="checkbox" v-model="action.enabled" class="peer sr-only" />
                              <div class="w-4 h-4 border-2 border-slate-300 rounded peer-checked:bg-primary peer-checked:border-primary transition-all flex items-center justify-center group-hover/action:border-primary/50">
                                <span class="material-symbols-outlined text-white text-[12px]" :class="action.enabled ? 'opacity-100' : 'opacity-0'">check</span>
                              </div>
                            </div>
                            {{ action.actionName }}
                          </label>
                          <div v-if="action.hasScopeOption" class="pl-7">
                            <div class="relative">
                              <select
                                v-model="action.dataScope"
                                :disabled="!action.enabled"
                                class="w-full appearance-none text-xs bg-slate-50 border border-slate-200 rounded-lg pl-3 pr-8 py-2 focus:ring-2 focus:ring-primary/20 focus:border-primary focus:bg-white text-slate-700 outline-none transition-all font-medium cursor-pointer hover:border-slate-300 disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                <option v-for="opt in dataScopeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                              </select>
                              <span class="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 text-sm pointer-events-none">
                                expand_more
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>

    <!-- Add User to Role Dialog -->
    <el-dialog
      v-model="showAddRoleUserDialog"
      title="添加用户到角色"
      :width="isMobile ? '95%' : '460px'"
      :fullscreen="isMobile"
      append-to-body
    >
      <el-input v-model="allUserSearch" placeholder="搜索用户..." clearable class="mb-3" />
      <div class="max-h-64 overflow-auto space-y-1">
        <div
          v-for="user in availableUsersForRole"
          :key="user.userId"
          class="flex items-center justify-between p-2 hover:bg-slate-50 rounded cursor-pointer"
          @click="toggleSelectUser(user.userId)"
        >
          <div class="flex items-center gap-2">
            <el-checkbox :model-value="selectedUserIds.includes(String(user.userId))" @click.stop />
            <div class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-medium" :class="getAvatarColor(user.realname || user.username)">
              {{ (user.realname || user.username || '?').charAt(0) }}
            </div>
            <span class="text-sm">{{ user.realname || user.username }}</span>
          </div>
          <span class="text-xs text-slate-400">{{ user.email || '' }}</span>
        </div>
        <div v-if="availableUsersForRole.length === 0" class="text-center py-4 text-slate-400 text-sm">
          没有可添加的用户
        </div>
      </div>
      <template #footer>
        <el-button @click="showAddRoleUserDialog = false">取消</el-button>
        <el-button type="primary" :loading="addingRoleUsers" :disabled="selectedUserIds.length === 0" @click="handleConfirmAddRoleUsers">
          确认添加 ({{ selectedUserIds.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- Employee Detail Drawer -->
    <el-drawer
      v-model="showMemberDetailDrawer"
      direction="rtl"
      :size="isMobile ? '100%' : '400px'"
      :with-header="false"
      class="employee-detail-drawer"
    >
      <div v-if="detailMember" class="h-full flex flex-col bg-white shadow-2xl">
        <!-- Header -->
        <div class="flex items-center justify-between p-6 border-b border-slate-100">
          <h3 class="text-lg font-bold text-slate-900">员工详情</h3>
          <button
            @click="showMemberDetailDrawer = false"
            class="size-9 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
            aria-label="关闭员工详情"
            type="button"
          >
            <span class="material-symbols-outlined text-xl leading-none">close</span>
          </button>
        </div>

        <!-- Content -->
        <div class="flex-1 overflow-y-auto">
          <!-- Avatar + Name + Position -->
          <div class="p-8 text-center space-y-4">
            <div class="relative inline-block">
              <div
                class="size-24 rounded-2xl mx-auto flex items-center justify-center text-white text-3xl font-bold shadow-xl ring-4 ring-white"
                :class="getAvatarColor(detailMember.realname)"
              >
                {{ (detailMember.realname || detailMember.username || '?').charAt(0) }}
              </div>
              <div
                class="absolute -bottom-2 -right-2 size-6 rounded-full border-2 border-white"
                :class="detailMember.status === 1 ? 'bg-emerald-500' : 'bg-slate-400'"
              />
            </div>
            <div>
              <h2 class="text-xl font-bold text-slate-900">
                {{ detailMember.realname || detailMember.username }}
              </h2>
              <p class="text-primary font-bold text-sm uppercase tracking-widest mt-1">
                {{ detailMember.post || '员工' }}
              </p>
            </div>
          </div>

          <!-- Info Cards -->
          <div class="px-8 space-y-6">
            <div class="grid grid-cols-2 gap-4">
              <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">所属部门</p>
                <p class="text-sm font-bold text-slate-700">{{ detailMember.deptName || '-' }}</p>
              </div>
              <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">登录账号</p>
                <p class="text-sm font-bold text-slate-700">@{{ detailMember.username }}</p>
              </div>
              <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100 col-span-2">
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">直属上级</p>
                <p class="text-sm font-bold text-slate-700">{{ detailMember.parentName || '无' }}</p>
              </div>
            </div>

            <!-- Roles -->
            <div class="space-y-3">
              <h4 class="text-xs font-bold text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-2">系统角色</h4>
              <div class="flex flex-wrap gap-2">
                <div
                  v-for="rn in (detailMember.roleNames || [])"
                  :key="rn"
                  class="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-slate-200 rounded-xl shadow-sm"
                >
                  <div class="size-1.5 rounded-full bg-primary"></div>
                  <span class="text-xs font-bold text-slate-700">
                    {{ rn }}
                  </span>
                </div>
                <span v-if="!detailMember.roleNames?.length" class="text-sm text-slate-400">未分配角色</span>
              </div>
            </div>

            <!-- Contact Info -->
            <div class="space-y-4">
              <h4 class="text-xs font-bold text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-2">联系信息</h4>
              <div class="flex items-center gap-4 p-3 hover:bg-slate-50 rounded-xl transition-colors">
                <div class="size-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                  <span class="material-symbols-outlined text-xl leading-none">mail</span>
                </div>
                <div class="min-w-0">
                  <p class="text-[10px] font-bold text-slate-400 uppercase">电子邮箱</p>
                  <p class="text-sm font-medium text-slate-700 break-words">{{ detailMember.email || '-' }}</p>
                </div>
              </div>
              <div class="flex items-center gap-4 p-3 hover:bg-slate-50 rounded-xl transition-colors">
                <div class="size-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                  <span class="material-symbols-outlined text-xl leading-none">call</span>
                </div>
                <div class="min-w-0">
                  <p class="text-[10px] font-bold text-slate-400 uppercase">手机号码</p>
                  <p class="text-sm font-medium text-slate-700 break-words">{{ detailMember.mobile || '-' }}</p>
                </div>
              </div>
            </div>

            <!-- Work Summary -->
            <div class="space-y-4 pb-8">
              <h4 class="text-xs font-bold text-slate-900 uppercase tracking-widest border-b border-slate-100 pb-2">工作摘要</h4>
              <div class="grid grid-cols-2 gap-3">
                <div class="text-center p-3 bg-slate-50 rounded-xl border border-slate-100">
                  <p class="text-lg font-black text-slate-900">-</p>
                  <p class="text-[9px] font-bold text-slate-400 uppercase mt-1">跟进客户</p>
                </div>
                <div class="text-center p-3 bg-slate-50 rounded-xl border border-slate-100">
                  <p class="text-lg font-black text-slate-900">-</p>
                  <p class="text-[9px] font-bold text-slate-400 uppercase mt-1">活跃商机</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="p-6 border-t border-slate-100 flex gap-3">
          <el-button class="flex-1" type="primary" size="large" @click="showMemberDetailDrawer = false; handleEditMember(detailMember)">
            <span class="material-symbols-outlined text-base mr-1">edit</span>
            编辑资料
          </el-button>
          <el-button size="large" type="danger" plain class="!px-3" @click="handleDeleteMember(detailMember)">
            <span class="material-symbols-outlined text-base">delete</span>
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAgentStore } from '@/stores/agent'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Promotion, Edit, Delete, Loading,
  Connection, Document, Tools, View, Hide, Box, Link, Upload
} from '@element-plus/icons-vue'
import type { AiAgent } from '@/types/common'
import type { CustomField, EntityType, FieldType, FieldOption } from '@/types/customField'
import type { AiConfigUpdateBO, AiConnectionTestResult, AiProviderPreset, AiProvider, WeKnoraConfigUpdateBO, WeKnoraConnectionTestResult } from '@/types/systemConfig'
import {
  getFieldsByEntity,
  addCustomField,
  updateCustomField,
  deleteCustomField,
  enableCustomField,
  disableCustomField
} from '@/api/customField'
import { getLoginUserDetail, updateProfile, changePassword, queryUserList, addUser, updateUserInfo, deleteUsers } from '@/api/auth'
import { queryDeptTree as fetchDeptTree, addDept, updateDept, deleteDept } from '@/api/dept'
import type { DeptVO } from '@/types/dept'
import { getAiConfig, updateAiConfig, testAiConnection, getMinioConsoleUrl, getMinioSsoUrl, getWeKnoraConfig, updateWeKnoraConfig, testWeKnoraConnection, getEnterpriseConfig, updateEnterpriseConfig } from '@/api/systemConfig'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { useEnterpriseStore } from '@/stores/enterprise'
import type { MinioConsoleConfig } from '@/types/systemConfig'
import { queryRoleList, addRole, updateRole, deleteRole as deleteRoleApi, getRolePermissions, saveRolePermissions, addUsersToRole, removeUserFromRole } from '@/api/role'
import type { RoleVO, RolePermissionVO, PermItem } from '@/types/role'

const userStore = useUserStore()
const enterpriseStore = useEnterpriseStore()
const agentStore = useAgentStore()
const { isMobile } = useResponsive()

// Tab state
const activeTab = ref('team')
const systemTabs = ['profile', 'enterprise', 'api', 'agent', 'storage', 'weknora', 'customField']
const isSystemTab = computed(() => systemTabs.includes(activeTab.value))
const systemSubTabs = [
  { value: 'profile', label: '个人资料' },
  { value: 'enterprise', label: '企业信息' },
  { value: 'api', label: 'AI/API 配置' },
  { value: 'agent', label: '智能体' },
  { value: 'storage', label: '对象存储' },
  { value: 'weknora', label: '知识库服务' },
  { value: 'customField', label: '自定义字段' }
]

// Profile form
const savingProfile = ref(false)
const profileForm = reactive({
  img: '',
  imgUrl: '',
  realname: '',
  email: '',
  phone: '',
  department: '',
  position: ''
})
const avatarUploading = ref(false)
const avatarInputRef = ref<HTMLInputElement | null>(null)
const avatarPreviewUrl = ref('')

// Password form
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// Agent state
const showAgentDialog = ref(false)
const submitting = ref(false)
const editingAgent = ref<AiAgent | null>(null)
const agentForm = reactive({
  label: '',
  prompt: '',
  iconName: 'Promotion',
  enabled: true
})

// Custom field state
const showFieldDialog = ref(false)
const activeEntityType = ref<EntityType>('customer')
const customFields = ref<CustomField[]>([])
const loadingFields = ref(false)
const editingField = ref<CustomField | null>(null)
const fieldForm = reactive({
  fieldLabel: '',
  fieldName: '',
  fieldType: 'text' as FieldType,
  placeholder: '',
  defaultValue: '',
  isRequired: false,
  isSearchable: false,
  isShowInList: true,
  options: [] as FieldOption[]
})

// Enterprise config state
const savingEnterprise = ref(false)
const enterpriseConfigLoaded = ref(false)
const logoInputRef = ref<HTMLInputElement | null>(null)
const enterpriseForm = reactive({
  name: '',
  logo: '',
  logoUrl: '',
  updateTime: ''
})

// Field type labels
const FIELD_TYPE_LABELS: Record<FieldType, string> = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  date: '日期',
  datetime: '日期时间',
  select: '单选下拉',
  multiselect: '多选下拉',
  checkbox: '开关'
}

// AI Config state
const showApiKey = ref(false)
const savingAiConfig = ref(false)
const testingConnection = ref(false)
const connectionTestResult = ref<AiConnectionTestResult | null>(null)
const aiConfigLoaded = ref(false)

const aiConfigForm = reactive<AiConfigUpdateBO & { updateTime?: string }>({
  provider: 'dashscope',
  apiUrl: '',
  apiKey: '',
  model: '',
  temperature: 0.7,
  maxTokens: 2048,
  updateTime: undefined
})

// MinIO Config state
const loadingMinioConfig = ref(false)
const minioConfig = reactive<MinioConsoleConfig>({
  enabled: false,
  consoleUrl: ''
})

// WeKnora Config state
const showWeknoraApiKey = ref(false)
const savingWeknoraConfig = ref(false)
const testingWeknoraConnection = ref(false)
const loadingWeknoraConfig = ref(false)
const weknoraConfigLoaded = ref(false)
const weknoraConnectionTestResult = ref<WeKnoraConnectionTestResult | null>(null)

const weknoraConfigForm = reactive<WeKnoraConfigUpdateBO & { updateTime?: string }>({
  enabled: false,
  baseUrl: '',
  apiKey: '',
  knowledgeBaseId: '',
  matchCount: 5,
  vectorThreshold: 0.5,
  autoRagEnabled: true,
  updateTime: undefined
})

// Team management state
const deptTreeRef = ref()
const deptTree = ref<DeptVO[]>([])
const selectedDept = ref<DeptVO | null>(null)
const memberList = ref<any[]>([])
const loadingDeptTree = ref(false)
const loadingMembers = ref(false)
const showDeptDrawer = ref(false)
const memberSearch = ref('')
const memberRoleId = ref<number>(0)

const filteredMembers = computed(() => {
  const list = memberList.value || []
  const s = memberSearch.value.trim().toLowerCase()
  const roleId = Number(memberRoleId.value || 0)
  return list.filter((m: any) => {
    const okSearch = !s || [m.realname, m.username, m.email, m.mobile]
      .filter(Boolean)
      .some((v: string) => String(v).toLowerCase().includes(s))
    const okRole = roleId === 0 || (Array.isArray(m.roleIds) && m.roleIds.map(String).includes(String(roleId))) ||
      (Array.isArray(m.roles) && m.roles.map(String).includes(String(roleId)))
    return okSearch && okRole
  })
})

// Dept dialog
const showDeptDialog = ref(false)
const submittingDept = ref(false)
const editingDept = ref<DeptVO | null>(null)
const deptFormParentId = ref<number | string>(0)
const deptForm = reactive({
  deptName: '',
  sortOrder: 0
})

// Member dialog
const showAddMemberDialog = ref(false)
const submittingMember = ref(false)
const editingMember = ref<any>(null)
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
  roleIds: [] as string[]
})
const allRoleOptions = ref<RoleVO[]>([])
const allUserListForParent = ref<any[]>([])

// Employee detail drawer
const showMemberDetailDrawer = ref(false)
const detailMember = ref<any>(null)

// Role management state
const roleList = ref<RoleVO[]>([])
const loadingRoles = ref(false)
const showRoleDialog = ref(false)
const savingRole = ref(false)
const editingRole = ref<RoleVO | null>(null)
const roleForm = reactive({ roleName: '', description: '' })

// Combined role drawer state
const showRoleDrawer = ref(false)
const roleDrawerRole = ref<RoleVO | null>(null)
const roleDrawerTab = ref<'members' | 'permissions'>('members')

// Permission state (used inside drawer)
const permissionRole = ref<RoleVO | null>(null)
const permissionList = ref<RolePermissionVO[]>([])
const loadingPermissions = ref(false)
const savingPermissions = ref(false)

// Role users state (used inside drawer)
const roleUsersRole = ref<RoleVO | null>(null)
const roleUsers = ref<any[]>([])
const loadingRoleUsers = ref(false)
const roleUserSearch = ref('')

// Add user to role dialog state
const showAddRoleUserDialog = ref(false)
const allUsers = ref<any[]>([])
const allUserSearch = ref('')
const selectedUserIds = ref<string[]>([])
const addingRoleUsers = ref(false)

const dataScopeOptions = [
  { value: 1, label: '本人' },
  { value: 2, label: '本人及下属' },
  { value: 3, label: '本部门' },
  { value: 4, label: '本部门及下属部门' },
  { value: 5, label: '全部' }
]

const filteredRoleUsers = computed(() => {
  if (!roleUserSearch.value) return roleUsers.value
  const s = roleUserSearch.value.toLowerCase()
  return roleUsers.value.filter((u: any) =>
    (u.realname || '').toLowerCase().includes(s) ||
    (u.username || '').toLowerCase().includes(s) ||
    (u.email || '').toLowerCase().includes(s)
  )
})

const availableUsersForRole = computed(() => {
  const existingIds = new Set(roleUsers.value.map((u: any) => String(u.userId)))
  let users = allUsers.value.filter((u: any) => !existingIds.has(String(u.userId)))
  if (allUserSearch.value) {
    const s = allUserSearch.value.toLowerCase()
    users = users.filter((u: any) =>
      (u.realname || '').toLowerCase().includes(s) ||
      (u.username || '').toLowerCase().includes(s)
    )
  }
  return users
})

// Avatar color palette
const avatarColors = ['bg-purple-500', 'bg-blue-500', 'bg-green-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500']
function getAvatarColor(name: string): string {
  if (!name) return avatarColors[0]
  const index = name.charCodeAt(0) % avatarColors.length
  return avatarColors[index]
}

function countDepts(tree: any[]): number {
  let count = 0
  for (const node of tree) {
    count++
    if (node.children) count += countDepts(node.children)
  }
  return count
}

// 预设的 AI 服务提供商
const providerPresets: AiProviderPreset[] = [
  {
    label: 'OpenAI',
    value: 'openai',
    baseUrl: 'https://api.openai.com/v1',
    models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-4', 'gpt-3.5-turbo']
  },
  {
    label: '阿里云 DashScope (通义千问)',
    value: 'dashscope',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/',
    models: ['qwen-max', 'qwen-plus', 'qwen-turbo', 'qwen-long']
  },
  {
    label: '自定义 OpenAI 兼容服务',
    value: 'custom',
    baseUrl: '',
    models: []
  }
]

// 当前提供商的模型列表
const currentProviderModels = computed(() => {
  const preset = providerPresets.find(p => p.value === aiConfigForm.provider)
  return preset?.models || []
})

function getFieldTypeLabel(type: FieldType): string {
  return FIELD_TYPE_LABELS[type] || type
}

onMounted(async () => {
  // Load user detail info from API
  try {
    const detail = await getLoginUserDetail()
    profileForm.img = detail.img || ''
    profileForm.imgUrl = (detail as any).imgUrl || ''
    profileForm.realname = detail.realname || ''
    profileForm.email = detail.email || ''
    profileForm.phone = detail.mobile || ''
    profileForm.department = detail.deptName || ''
    profileForm.position = detail.post || ''
  } catch {
    // Fallback to store data
    const info = userStore.userInfo as any
    profileForm.img = info?.img || ''
    profileForm.imgUrl = info?.imgUrl || ''
    profileForm.realname = userStore.realname || ''
    profileForm.email = info?.email || ''
    profileForm.phone = info?.mobile || ''
    profileForm.department = info?.deptName || ''
    profileForm.position = info?.post || ''
  }

  agentStore.fetchAllAgents()
  loadCustomFields()

  // 预加载 MinIO 配置，确保切换到对象存储 Tab 时数据已就绪
  loadMinioConfig()
})

// Profile methods
async function handleAvatarChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
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
    ElMessage.success('头像上传成功，点击保存更改生效')
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
    if (avatarInputRef.value) avatarInputRef.value.value = ''
  }
}

function resetProfileForm() {
  const info = userStore.userInfo as any
  profileForm.img = info?.img || ''
  profileForm.imgUrl = info?.imgUrl || ''
  avatarPreviewUrl.value = ''
  profileForm.realname = userStore.realname || ''
  profileForm.email = info?.email || ''
  profileForm.phone = info?.mobile || ''
  profileForm.department = info?.deptName || ''
  profileForm.position = info?.post || ''
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
    ElMessage.success('个人资料保存成功')
  } catch {
    // Error handled by interceptor
  } finally {
    savingProfile.value = false
  }
}

// Password methods
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

  submitting.value = true
  try {
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    ElMessage.success('密码修改成功')
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

// Custom field methods
async function loadCustomFields() {
  loadingFields.value = true
  try {
    customFields.value = await getFieldsByEntity(activeEntityType.value)
  } catch {
    // Error handled by interceptor
  } finally {
    loadingFields.value = false
  }
}

function handleOpenFieldDialog() {
  editingField.value = null
  resetFieldForm()
  showFieldDialog.value = true
}

function handleEditField(field: CustomField) {
  editingField.value = field
  Object.assign(fieldForm, {
    fieldLabel: field.fieldLabel,
    fieldName: field.fieldName,
    fieldType: field.fieldType,
    placeholder: field.placeholder || '',
    defaultValue: field.defaultValue || '',
    isRequired: field.isRequired,
    isSearchable: field.isSearchable,
    isShowInList: field.isShowInList,
    options: field.options ? [...field.options] : []
  })
  showFieldDialog.value = true
}

function handleFieldTypeChange(newType: FieldType) {
  if (!['select', 'multiselect'].includes(newType)) {
    fieldForm.options = []
  } else if (fieldForm.options.length === 0) {
    fieldForm.options = [{ value: '', label: '' }]
  }
}

async function handleSaveField() {
  if (!fieldForm.fieldLabel.trim()) {
    ElMessage.warning('请输入字段标签')
    return
  }
  if (!editingField.value && !fieldForm.fieldName.trim()) {
    ElMessage.warning('请输入字段标识')
    return
  }
  if (!editingField.value && !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(fieldForm.fieldName)) {
    ElMessage.warning('字段标识只能包含字母数字下划线，且以字母开头')
    return
  }

  if (['select', 'multiselect'].includes(fieldForm.fieldType)) {
    const validOptions = fieldForm.options.filter(o => o.value.trim() && o.label.trim())
    if (validOptions.length === 0) {
      ElMessage.warning('请至少添加一个有效选项')
      return
    }
    fieldForm.options = validOptions
  }

  submitting.value = true
  try {
    if (editingField.value) {
      await updateCustomField({
        fieldId: editingField.value.fieldId,
        fieldLabel: fieldForm.fieldLabel,
        placeholder: fieldForm.placeholder || undefined,
        defaultValue: fieldForm.defaultValue || undefined,
        isRequired: fieldForm.isRequired,
        isSearchable: fieldForm.isSearchable,
        isShowInList: fieldForm.isShowInList,
        options: fieldForm.options.length > 0 ? fieldForm.options : undefined
      })
      ElMessage.success('字段更新成功')
    } else {
      await addCustomField({
        entityType: activeEntityType.value,
        fieldName: fieldForm.fieldName,
        fieldLabel: fieldForm.fieldLabel,
        fieldType: fieldForm.fieldType,
        placeholder: fieldForm.placeholder || undefined,
        defaultValue: fieldForm.defaultValue || undefined,
        isRequired: fieldForm.isRequired,
        isSearchable: fieldForm.isSearchable,
        isShowInList: fieldForm.isShowInList,
        options: fieldForm.options.length > 0 ? fieldForm.options : undefined
      })
      ElMessage.success('字段添加成功')
    }
    showFieldDialog.value = false
    resetFieldForm()
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleToggleFieldStatus(field: CustomField, enabled: boolean) {
  try {
    if (enabled) {
      await enableCustomField(field.fieldId)
    } else {
      await disableCustomField(field.fieldId)
    }
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  }
}

async function handleDeleteField(field: CustomField) {
  try {
    await deleteCustomField(field.fieldId)
    ElMessage.success('字段删除成功')
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  }
}

function resetFieldForm() {
  editingField.value = null
  Object.assign(fieldForm, {
    fieldLabel: '',
    fieldName: '',
    fieldType: 'text',
    placeholder: '',
    defaultValue: '',
    isRequired: false,
    isSearchable: false,
    isShowInList: true,
    options: []
  })
}

// Team management methods
async function loadDeptTree() {
  loadingDeptTree.value = true
  try {
    deptTree.value = await fetchDeptTree()
    // 默认选中第一个顶级部门
    if (deptTree.value.length > 0 && !selectedDept.value) {
      selectedDept.value = deptTree.value[0]
      nextTick(() => {
        deptTreeRef.value?.setCurrentKey(deptTree.value[0].deptId)
      })
      loadMembers()
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loadingDeptTree.value = false
  }
}

async function loadMembers() {
  if (!selectedDept.value) {
    memberList.value = []
    return
  }
  loadingMembers.value = true
  try {
    const res = await queryUserList({ deptId: selectedDept.value.deptId, limit: 200 })
    memberList.value = res?.list || res?.records || res || []
  } catch {
    // Error handled by interceptor
  } finally {
    loadingMembers.value = false
  }
}

function handleDeptClick(data: DeptVO) {
  selectedDept.value = data
  loadMembers()
}

function handleDeptCommand(command: string, data: DeptVO) {
  if (command === 'edit') {
    handleEditDept(data)
  } else if (command === 'addChild') {
    handleAddDept(data.deptId)
  } else if (command === 'delete') {
    handleDeleteDept(data)
  }
}

function handleAddDept(parentId: string | number) {
  editingDept.value = null
  deptFormParentId.value = parentId
  Object.assign(deptForm, { deptName: '', sortOrder: 0 })
  showDeptDialog.value = true
}

function handleEditDept(dept: DeptVO) {
  editingDept.value = dept
  deptFormParentId.value = dept.parentId
  Object.assign(deptForm, { deptName: dept.deptName, sortOrder: dept.sortOrder || 0 })
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
    // Cancelled or error
  }
}

async function handleSaveDept() {
  if (!deptForm.deptName.trim()) {
    ElMessage.warning('请输入部门名称')
    return
  }
  submittingDept.value = true
  try {
    if (editingDept.value) {
      await updateDept({
        deptId: editingDept.value.deptId,
        deptName: deptForm.deptName,
        parentId: deptFormParentId.value,
        sortOrder: deptForm.sortOrder
      })
      ElMessage.success('部门更新成功')
    } else {
      await addDept({
        deptName: deptForm.deptName,
        parentId: deptFormParentId.value,
        sortOrder: deptForm.sortOrder
      })
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
    roleIds: (member.roleIds || []).map(String)
  })
  showAddMemberDialog.value = true
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
    // Cancelled or error
  }
}

async function handleSaveMember() {
  if (editingMember.value) {
    if (!memberForm.realname.trim()) {
      ElMessage.warning('请输入姓名')
      return
    }
    submittingMember.value = true
    try {
      await updateUserInfo({
        userId: editingMember.value.userId,
        realname: memberForm.realname,
        mobile: memberForm.mobile || undefined,
        email: memberForm.email || undefined,
        post: memberForm.post || undefined,
        deptId: memberForm.deptId || undefined,
        parentId: memberForm.parentId || 0,
        status: memberForm.status,
        password: memberForm.password || undefined,
        roleIds: memberForm.roleIds
      })
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
  } else {
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
}

function resetMemberForm() {
  editingMember.value = null
  Object.assign(memberForm, { username: '', realname: '', password: '', mobile: '', email: '', post: '', deptId: selectedDept.value ? selectedDept.value.deptId : null, parentId: null, status: 1, roleIds: [] })
}

async function loadAllUsersForParent() {
  try {
    const res = await queryUserList({ limit: 500 })
    allUserListForParent.value = res?.list || res?.records || []
  } catch { /* ignore */ }
}

const parentOptions = computed(() => {
  const currentId = editingMember.value?.userId
  return allUserListForParent.value.filter((u: any) =>
    u.status === 1 && (!currentId || String(u.userId) !== String(currentId))
  )
})

function handleMemberRowClick(member: any) {
  detailMember.value = member
  showMemberDetailDrawer.value = true
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
  } catch { /* cancelled */ }
}

async function loadRoleOptions() {
  try { allRoleOptions.value = await queryRoleList() } catch { /* ignore */ }
}

function handleAddMember() {
  resetMemberForm()
  loadAllUsersForParent()
  loadRoleOptions()
  showAddMemberDialog.value = true
}

// Agent methods
function handleEditAgent(agent: AiAgent) {
  editingAgent.value = agent
  Object.assign(agentForm, {
    label: agent.label,
    prompt: agent.prompt,
    iconName: agent.iconName,
    enabled: !!agent.enabled
  })
  showAgentDialog.value = true
}

async function handleDeleteAgent(agent: AiAgent) {
  try {
    await ElMessageBox.confirm(`确定要删除智能体「${agent.label}」吗？`, '提示', { type: 'warning' })
    await agentStore.removeAgent(agent.agentId)
    ElMessage.success('删除成功')
  } catch {
    // Cancelled
  }
}

async function handleToggleAgent(agentId: string, newEnabled: boolean) {
  await agentStore.toggleAgentEnabled(agentId, newEnabled)
}

async function handleSaveAgent() {
  if (!agentForm.label || !agentForm.prompt) {
    ElMessage.warning('请填写名称和提示词')
    return
  }

  submitting.value = true
  try {
    if (editingAgent.value) {
      await agentStore.editAgent({
        ...agentForm,
        agentId: editingAgent.value.agentId
      } as any)
      ElMessage.success('更新成功')
    } else {
      await agentStore.createAgent(agentForm as any)
      ElMessage.success('创建成功')
    }
    showAgentDialog.value = false
    resetAgentForm()
  } finally {
    submitting.value = false
  }
}

function resetAgentForm() {
  editingAgent.value = null
  Object.assign(agentForm, { label: '', prompt: '', iconName: 'Promotion', enabled: true })
}

// AI Config methods
function handleProviderChange(provider: AiProvider) {
  const preset = providerPresets.find(p => p.value === provider)
  if (preset) {
    aiConfigForm.apiUrl = preset.baseUrl
    if (preset.models.length > 0) {
      aiConfigForm.model = preset.models[0]
    }
  }
  connectionTestResult.value = null
}

async function loadAiConfig() {
  try {
    const config = await getAiConfig()
    Object.assign(aiConfigForm, {
      provider: (config.provider || 'dashscope') as AiProvider,
      apiUrl: config.apiUrl || '',
      apiKey: '', // API Key 不回显完整值，需要用户重新输入修改
      model: config.model || '',
      temperature: config.temperature ?? 0.7,
      maxTokens: config.maxTokens ?? 2048,
      updateTime: config.updateTime
    })
    connectionTestResult.value = null
    aiConfigLoaded.value = true
  } catch {
    // Error handled by interceptor
  }
}

async function handleTestConnection() {
  if (!aiConfigForm.apiUrl || !aiConfigForm.apiKey) {
    ElMessage.warning('请先填写 API 地址和密钥')
    return
  }

  testingConnection.value = true
  connectionTestResult.value = null

  try {
    const result = await testAiConnection({
      provider: aiConfigForm.provider,
      apiUrl: aiConfigForm.apiUrl,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens
    })
    connectionTestResult.value = result
  } catch (error: any) {
    connectionTestResult.value = {
      success: false,
      responseTime: 0,
      message: error.message || '连接测试失败'
    }
  } finally {
    testingConnection.value = false
  }
}

async function handleSaveAiConfig() {
  if (!aiConfigForm.apiUrl) {
    ElMessage.warning('请填写 API 地址')
    return
  }
  if (!aiConfigForm.apiKey) {
    ElMessage.warning('请填写 API 密钥')
    return
  }
  if (!aiConfigForm.model) {
    ElMessage.warning('请选择或输入模型名称')
    return
  }

  savingAiConfig.value = true
  try {
    await updateAiConfig({
      provider: aiConfigForm.provider,
      apiUrl: aiConfigForm.apiUrl,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens
    })
    ElMessage.success('AI 配置保存成功，已立即生效')
    await loadAiConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    savingAiConfig.value = false
  }
}

function formatTime(time: string | undefined): string {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

// 监听 tab 切换，懒加载配置
// Watch drawer tab switching to load data
watch(roleDrawerTab, async (newTab) => {
  if (!showRoleDrawer.value || !roleDrawerRole.value) return
  if (newTab === 'members') {
    roleUsersRole.value = roleDrawerRole.value
    roleUserSearch.value = ''
    await loadRoleUsers()
  } else if (newTab === 'permissions') {
    permissionRole.value = roleDrawerRole.value
    loadingPermissions.value = true
    try {
      permissionList.value = await getRolePermissions(roleDrawerRole.value.roleId)
    } catch {
      // handled
    } finally {
      loadingPermissions.value = false
    }
  }
})

watch(activeTab, async (newTab) => {
  if (newTab === 'team') {
    await loadDeptTree()
    loadMembers()
    // 预加载角色列表，供编辑成员时选择
    if (allRoleOptions.value.length === 0) {
      try { allRoleOptions.value = await queryRoleList() } catch { /* ignore */ }
    }
  }
  if (newTab === 'role') {
    await loadRoleList()
  }
  if (newTab === 'api' && !aiConfigLoaded.value) {
    await loadAiConfig()
  }
  if (newTab === 'storage') {
    await loadMinioConfig()
  }
  if (newTab === 'weknora' && !weknoraConfigLoaded.value) {
    await loadWeknoraConfig()
  }
  if (newTab === 'enterprise' && !enterpriseConfigLoaded.value) {
    await loadEnterpriseConfig()
  }
}, { immediate: true })

// MinIO methods
async function loadMinioConfig() {
  loadingMinioConfig.value = true
  try {
    const config = await getMinioConsoleUrl()
    console.log('MinIO config loaded:', config)
    Object.assign(minioConfig, config)
  } catch (error) {
    console.error('Failed to load MinIO config:', error)
  } finally {
    loadingMinioConfig.value = false
  }
}

async function handleOpenMinioConsole() {
  if (!minioConfig.enabled) {
    ElMessage.warning('MinIO 未启用')
    return
  }

  try {
    // 获取带 session_token 的 SSO URL
    const { ssoUrl } = await getMinioSsoUrl()
    if (!ssoUrl) {
      ElMessage.warning('无法获取 SSO 登录地址')
      return
    }
    const win = window.open(ssoUrl, '_blank')
    if (!win) {
      ElMessage.warning('浏览器阻止了弹窗，请允许弹窗后重试')
    }
  } catch (error) {
    console.error('Failed to get SSO URL:', error)
    ElMessage.error('获取 SSO 登录地址失败')
  }
}

// WeKnora methods
async function loadWeknoraConfig() {
  loadingWeknoraConfig.value = true
  try {
    const config = await getWeKnoraConfig()
    Object.assign(weknoraConfigForm, {
      enabled: config.enabled ?? false,
      baseUrl: config.baseUrl || '',
      apiKey: '', // API Key 不回显完整值，需要用户重新输入修改
      knowledgeBaseId: config.knowledgeBaseId || '',
      matchCount: config.matchCount ?? 5,
      vectorThreshold: config.vectorThreshold ?? 0.5,
      autoRagEnabled: config.autoRagEnabled ?? true,
      updateTime: config.updateTime
    })
    weknoraConnectionTestResult.value = null
    weknoraConfigLoaded.value = true
  } catch {
    // Error handled by interceptor
  } finally {
    loadingWeknoraConfig.value = false
  }
}

async function handleTestWeknoraConnection() {
  if (!weknoraConfigForm.baseUrl || !weknoraConfigForm.apiKey) {
    ElMessage.warning('请先填写 API 地址和密钥')
    return
  }

  testingWeknoraConnection.value = true
  weknoraConnectionTestResult.value = null

  try {
    const result = await testWeKnoraConnection({
      enabled: weknoraConfigForm.enabled,
      baseUrl: weknoraConfigForm.baseUrl,
      apiKey: weknoraConfigForm.apiKey,
      knowledgeBaseId: weknoraConfigForm.knowledgeBaseId,
      matchCount: weknoraConfigForm.matchCount,
      vectorThreshold: weknoraConfigForm.vectorThreshold,
      autoRagEnabled: weknoraConfigForm.autoRagEnabled
    })
    weknoraConnectionTestResult.value = result
  } catch (error: any) {
    weknoraConnectionTestResult.value = {
      success: false,
      responseTime: 0,
      message: error.message || '连接测试失败'
    }
  } finally {
    testingWeknoraConnection.value = false
  }
}

async function handleSaveWeknoraConfig() {
  savingWeknoraConfig.value = true
  try {
    await updateWeKnoraConfig({
      enabled: weknoraConfigForm.enabled,
      baseUrl: weknoraConfigForm.baseUrl,
      apiKey: weknoraConfigForm.apiKey || undefined,
      knowledgeBaseId: weknoraConfigForm.knowledgeBaseId,
      matchCount: weknoraConfigForm.matchCount,
      vectorThreshold: weknoraConfigForm.vectorThreshold,
      autoRagEnabled: weknoraConfigForm.autoRagEnabled
    })
    ElMessage.success('WeKnora 配置保存成功')
    await loadWeknoraConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    savingWeknoraConfig.value = false
  }
}

// ========== Enterprise Config Methods ==========

async function loadEnterpriseConfig() {
  try {
    const config = await getEnterpriseConfig()
    Object.assign(enterpriseForm, {
      name: config.name || '',
      logo: config.logo || '',
      logoUrl: config.logoUrl || '',
      updateTime: config.updateTime || ''
    })
    enterpriseConfigLoaded.value = true
  } catch {
    // Error handled by interceptor
  }
}

function triggerLogoUpload() {
  logoInputRef.value?.click()
}

async function handleLogoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 验证文件类型和大小
  if (!['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG 格式')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  try {
    const presigned = await getPresignedUploadUrl(file.name, file.type)
    await uploadToMinIO(file, presigned.uploadUrl)
    enterpriseForm.logo = presigned.objectKey
    enterpriseForm.logoUrl = presigned.accessUrl
    ElMessage.success('Logo 上传成功')
  } catch {
    ElMessage.error('Logo 上传失败')
  } finally {
    // 重置 input 以允许重复选择同一文件
    input.value = ''
  }
}

function removeLogo() {
  enterpriseForm.logo = ''
  enterpriseForm.logoUrl = ''
}

async function saveEnterpriseConfig() {
  savingEnterprise.value = true
  try {
    await updateEnterpriseConfig({
      name: enterpriseForm.name,
      logo: enterpriseForm.logo
    })
    ElMessage.success('企业信息保存成功')
    // 更新全局 store
    enterpriseStore.updateLocal({
      name: enterpriseForm.name || null,
      logo: enterpriseForm.logo || null,
      logoUrl: enterpriseForm.logoUrl || null
    })
    await loadEnterpriseConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    savingEnterprise.value = false
  }
}

// ========== Role Management Methods ==========

async function loadRoleList() {
  loadingRoles.value = true
  try {
    roleList.value = await queryRoleList()
  } catch {
    // handled
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
      await updateRole({ roleId: editingRole.value.roleId, roleName: roleForm.roleName, description: roleForm.description })
      ElMessage.success('角色更新成功')
    } else {
      await addRole({ roleName: roleForm.roleName, description: roleForm.description })
      ElMessage.success('角色创建成功')
    }
    showRoleDialog.value = false
    await loadRoleList()
  } catch {
    // handled
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

// Combined drawer opener
async function openRoleDrawer(role: RoleVO, tab: 'members' | 'permissions') {
  roleDrawerRole.value = role
  roleDrawerTab.value = tab
  showRoleDrawer.value = true
  if (tab === 'members') {
    roleUsersRole.value = role
    roleUserSearch.value = ''
    await loadRoleUsers()
  } else {
    permissionRole.value = role
    loadingPermissions.value = true
    try {
      permissionList.value = await getRolePermissions(role.roleId)
    } catch {
      // handled
    } finally {
      loadingPermissions.value = false
    }
  }
}

async function handleSavePermissions() {
  if (!permissionRole.value) return
  savingPermissions.value = true
  const permissions: PermItem[] = []
  for (const mod of permissionList.value) {
    for (const act of mod.actions) {
      if (act.enabled) {
        permissions.push({
          menuId: act.menuId,
          dataScope: act.hasScopeOption ? act.dataScope : null
        })
      }
    }
  }
  try {
    await saveRolePermissions({ roleId: permissionRole.value.roleId, permissions })
    ElMessage.success('权限配置保存成功')
    showRoleDrawer.value = false
  } catch {
    // handled
  } finally {
    savingPermissions.value = false
  }
}

// Role users methods
async function loadRoleUsers() {
  if (!roleUsersRole.value) return
  loadingRoleUsers.value = true
  try {
    const res = await queryUserList({ roleId: roleUsersRole.value.roleId, limit: 200 })
    roleUsers.value = res?.list || res?.records || (Array.isArray(res) ? res : [])
  } catch {
    // handled
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
  const idx = selectedUserIds.value.indexOf(id)
  if (idx >= 0) selectedUserIds.value.splice(idx, 1)
  else selectedUserIds.value.push(id)
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
    // handled
  } finally {
    addingRoleUsers.value = false
  }
}

watch(showAddRoleUserDialog, async (val) => {
  if (val) {
    allUserSearch.value = ''
    selectedUserIds.value = []
    try {
      const res = await queryUserList({ limit: 200 })
      allUsers.value = res?.list || res?.records || (Array.isArray(res) ? res : [])
    } catch {
      // handled
    }
  }
})
</script>

<style scoped>
/* Department tree styles */
:deep(.el-tree-node__content) {
  height: 36px;
  padding-right: 4px;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: transparent;
}

/* Dept tree: let custom row handle hover/selected */
:deep(.el-tree-node__content:hover) {
  background-color: transparent;
}

/* Employee detail drawer: remove default body padding */
:deep(.employee-detail-drawer .el-drawer__body) {
  padding: 0 !important;
}
</style>
