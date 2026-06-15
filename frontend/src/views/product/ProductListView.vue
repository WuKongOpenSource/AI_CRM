<template>
  <div class="product-page flex h-full min-h-0 flex-col bg-slate-50">
    <div class="flex shrink-0 flex-col gap-3 border-b border-slate-200 bg-white px-4 py-4 md:px-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="min-w-0">
          <h1 class="text-[22px] font-bold text-slate-900">产品管理</h1>
          <p class="mt-1 text-[13px] text-slate-500">维护产品资料、类目、负责人和业务启停状态</p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openCreateDialog">新建产品</el-button>
          <el-dropdown v-if="showHeaderMoreActions" trigger="click" @command="handleHeaderMoreCommand">
            <el-button :icon="MoreFilled" aria-label="更多操作" title="更多操作" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="canImport" command="import" :icon="Upload">导入</el-dropdown-item>
                <el-dropdown-item v-if="canExport" command="export" :icon="Download">导出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="grid gap-3 md:grid-cols-[minmax(220px,1fr)_160px_160px] xl:grid-cols-[minmax(220px,373px)_160px_160px]">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索产品名称、编码、类型、单位或描述"
          :prefix-icon="Search"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <el-select v-model="query.status" clearable placeholder="状态" @change="applyFilters">
          <el-option label="启用" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
        <el-select v-model="query.productType" clearable placeholder="产品类型" @change="applyFilters">
          <el-option v-for="type in productTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
      </div>
    </div>

    <div
      class="grid min-h-0 flex-1 grid-cols-1 overflow-hidden"
      :class="categoryPanelCollapsed ? 'lg:grid-cols-[48px_minmax(0,1fr)]' : 'lg:grid-cols-[280px_minmax(0,1fr)]'"
    >
      <aside
        class="relative min-h-0 border-b border-slate-200 bg-white lg:border-b-0 lg:border-r"
        :class="categoryPanelCollapsed ? 'product-category-panel--collapsed' : ''"
      >
        <button
          type="button"
          class="product-category-panel-toggle"
          :title="categoryPanelCollapsed ? '展开产品类目' : '收起产品类目'"
          :aria-label="categoryPanelCollapsed ? '展开产品类目' : '收起产品类目'"
          @click="categoryPanelCollapsed = !categoryPanelCollapsed"
        >
          <span class="material-symbols-outlined text-[18px]">
            {{ categoryPanelCollapsed ? 'keyboard_double_arrow_right' : 'keyboard_double_arrow_left' }}
          </span>
        </button>
        <div v-if="categoryPanelCollapsed" class="h-12 lg:h-full"></div>
        <template v-else>
          <div class="flex items-center border-b border-slate-200 py-3 pl-4 pr-9">
            <div class="min-w-0 flex-1 text-sm font-bold text-slate-900">产品类目</div>
            <div class="ml-auto flex shrink-0 items-center justify-end gap-1">
              <el-button v-if="canManageCategory" text :icon="FolderAdd" @click="openCategoryDialog()">新增</el-button>
            </div>
          </div>
          <div class="h-[240px] overflow-y-auto px-3 py-3 lg:h-full">
            <button
              type="button"
              class="mb-2 flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm"
              :class="!query.categoryId ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-100'"
              @click="selectCategory(undefined)"
            >
              <span>全部产品</span>
              <span class="material-symbols-outlined text-[18px]">inventory_2</span>
            </button>
            <el-tree
              :data="categories"
              node-key="categoryId"
              default-expand-all
              :props="{ label: 'categoryName', children: 'children' }"
              :expand-on-click-node="false"
              class="product-category-tree"
              @node-click="node => selectCategory(String(node.categoryId))"
            >
              <template #default="{ data }">
                <div
                  class="product-category-node flex min-w-0 flex-1 items-center rounded-md px-2 py-1"
                  :class="String(query.categoryId || '') === String(data.categoryId) ? 'bg-slate-100 text-slate-950' : ''"
                >
                  <span class="product-category-node__name truncate text-sm">{{ data.categoryName }}</span>
                  <span v-if="canManageCategory" class="product-category-node__actions flex items-center gap-1" @click.stop>
                    <button class="product-icon-btn" title="新增子类目" @click="openCategoryDialog(data)">
                      <span class="material-symbols-outlined text-[16px]">add</span>
                    </button>
                    <button class="product-icon-btn" title="编辑类目" @click="openCategoryDialog(data, true)">
                      <span class="material-symbols-outlined text-[16px]">edit</span>
                    </button>
                    <button class="product-icon-btn" title="上移" :disabled="!canMoveCategory(data, 'up')" @click="handleMoveCategory(data, 'up')">
                      <span class="material-symbols-outlined text-[16px]">keyboard_arrow_up</span>
                    </button>
                    <button class="product-icon-btn" title="下移" :disabled="!canMoveCategory(data, 'down')" @click="handleMoveCategory(data, 'down')">
                      <span class="material-symbols-outlined text-[16px]">keyboard_arrow_down</span>
                    </button>
                    <button class="product-icon-btn text-rose-500" title="删除类目" @click="handleDeleteCategory(data)">
                      <span class="material-symbols-outlined text-[16px]">delete</span>
                    </button>
                  </span>
                </div>
              </template>
            </el-tree>
          </div>
        </template>
      </aside>

      <main class="min-h-0 overflow-hidden">
        <div class="flex h-full min-h-0 flex-col bg-white">
          <div class="flex shrink-0 items-center justify-between border-b border-slate-200 px-4 py-3 text-sm text-slate-500">
            <span>共 {{ total }} 个产品</span>
            <el-checkbox v-model="query.includeChildCategory" :disabled="!query.categoryId" @change="loadProducts">
              包含子类目
            </el-checkbox>
          </div>

          <div class="min-h-0 flex-1">
            <el-table
              v-loading="loading"
              :data="products"
              height="100%"
              row-key="productId"
              table-layout="fixed"
              empty-text="暂无产品"
              @row-click="openDetail"
            >
              <el-table-column label="产品" min-width="240">
                <template #default="{ row }">
                  <div class="flex min-w-0 items-center gap-3">
                    <div class="product-main-image-thumb">
                      <img v-if="row.mainImageUrl" :src="row.mainImageUrl" :alt="row.productName" class="size-full object-cover" />
                      <span v-else class="material-symbols-outlined text-[18px]">inventory_2</span>
                    </div>
                    <div class="min-w-0">
                      <div class="flex min-w-0 items-center gap-2">
                        <span class="truncate text-sm font-semibold text-slate-900">{{ row.productName }}</span>
                        <el-tag size="small" :type="row.status === 'active' ? 'success' : 'info'">
                          {{ statusLabel(row.status) }}
                        </el-tag>
                      </div>
                      <p class="mt-1 truncate text-xs text-slate-400">{{ row.productCode || '无编码' }}</p>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="类目" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.categoryPath || row.categoryName || '-' }}</template>
              </el-table-column>
              <el-table-column label="类型/单位" width="140">
                <template #default="{ row }">
                  <div class="text-sm text-slate-700">{{ productTypeLabel(row.productType) }}</div>
                  <div class="text-xs text-slate-400">{{ unitLabel(row.unit) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="标准价" width="120" align="right">
                <template #default="{ row }">{{ money(row.standardPrice) }}</template>
              </el-table-column>
              <el-table-column label="负责人" width="130" show-overflow-tooltip>
                <template #default="{ row }">{{ row.ownerName || '-' }}</template>
              </el-table-column>
              <el-table-column label="更新时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.updateTime || row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="152" fixed="right">
                <template #default="{ row }">
                  <div class="flex items-center justify-end gap-1" @click.stop>
                    <button v-if="canEdit" class="product-icon-btn" title="编辑" @click="openEditDialog(row)">
                      <span class="material-symbols-outlined text-[18px]">edit</span>
                    </button>
                    <button v-if="canUpdateStatus" class="product-icon-btn" :title="row.status === 'active' ? '停用' : '启用'" @click="toggleStatus(row)">
                      <span class="material-symbols-outlined text-[18px]">{{ row.status === 'active' ? 'toggle_off' : 'toggle_on' }}</span>
                    </button>
                    <button v-if="canTransfer" class="product-icon-btn" title="转移负责人" @click="openTransferDialog([row])">
                      <span class="material-symbols-outlined text-[18px]">switch_account</span>
                    </button>
                    <button v-if="canDelete" class="product-icon-btn text-rose-500" title="删除" @click="handleDelete(row)">
                      <span class="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <footer class="product-list-pager">
            <span class="product-list-pager__total">共 {{ total }} 个产品</span>
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="limit"
              small
              background
              :hide-on-single-page="false"
              :layout="isMobile ? 'prev, pager, next' : 'sizes, prev, pager, next, jumper'"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="loadProducts"
              @size-change="handleSizeChange"
            />
          </footer>
        </div>
      </main>
    </div>

    <el-drawer v-model="detailVisible" :size="isMobile ? '100%' : '520px'" title="产品详情" destroy-on-close>
      <div v-if="detailLoading" class="flex h-60 items-center justify-center">
        <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
      </div>
      <div v-else-if="currentProduct" class="space-y-5">
        <section>
          <div class="flex items-start gap-3">
            <div class="flex size-12 shrink-0 items-center justify-center rounded-lg bg-slate-900 text-white">
              <img v-if="currentProduct.mainImageUrl" :src="currentProduct.mainImageUrl" :alt="currentProduct.productName" class="size-full rounded-lg object-cover" />
              <span v-else class="material-symbols-outlined">inventory_2</span>
            </div>
            <div class="min-w-0 flex-1">
              <h2 class="truncate text-lg font-bold text-slate-900">{{ currentProduct.productName }}</h2>
              <p class="mt-1 text-sm text-slate-500">{{ currentProduct.productCode || '无编码' }}</p>
            </div>
            <el-tag :type="currentProduct.status === 'active' ? 'success' : 'info'">
              {{ statusLabel(currentProduct.status) }}
            </el-tag>
          </div>
        </section>

        <section class="grid grid-cols-2 gap-3 text-sm">
          <InfoItem label="类目" :value="currentProduct.categoryPath || currentProduct.categoryName || '-'" />
          <InfoItem label="类型" :value="productTypeLabel(currentProduct.productType)" />
          <InfoItem label="单位" :value="unitLabel(currentProduct.unit)" />
          <InfoItem label="负责人" :value="currentProduct.ownerName || '-'" />
          <InfoItem label="标准价" :value="money(currentProduct.standardPrice)" />
          <InfoItem label="成本价" :value="money(currentProduct.costPrice)" />
          <InfoItem label="创建人" :value="formatProductCreatorName(currentProduct)" />
          <InfoItem label="创建时间" :value="formatDateTime(currentProduct.createTime)" />
          <InfoItem label="更新时间" :value="formatDateTime(currentProduct.updateTime || currentProduct.createTime)" class="col-span-2" />
        </section>

        <section>
          <h3 class="mb-2 text-sm font-bold text-slate-900">描述</h3>
          <p class="min-h-20 whitespace-pre-wrap rounded-md bg-slate-50 p-3 text-sm leading-6 text-slate-600">
            {{ currentProduct.description || '暂无描述' }}
          </p>
        </section>

        <section v-if="customFieldEntries.length">
          <h3 class="mb-2 text-sm font-bold text-slate-900">自定义字段</h3>
          <div class="grid grid-cols-1 gap-2">
            <InfoItem
              v-for="item in customFieldEntries"
              :key="item.key"
              :label="item.key"
              :value="String(item.value ?? '-')"
            />
          </div>
        </section>

        <div class="flex flex-wrap gap-2">
          <el-button type="primary" :icon="ChatDotRound" @click="openProductChat(currentProduct)">产品对话</el-button>
          <el-button v-if="canEdit" :icon="Edit" @click="openEditDialog(currentProduct)">编辑</el-button>
          <el-button v-if="canUpdateStatus" :icon="Switch" @click="toggleStatus(currentProduct)">
            {{ currentProduct.status === 'active' ? '停用' : '启用' }}
          </el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog
      v-model="formVisible"
      :width="isMobile ? 'calc(100% - 24px)' : '680px'"
      :show-close="false"
      destroy-on-close
      :class="[
        'wk-dialog--flush wk-product-dialog wk-crm-el-field-scope',
        isMobile ? 'wk-product-dialog--mobile' : 'wk-product-dialog--desktop'
      ]"
    >
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <span class="material-symbols-outlined text-[22px]">inventory_2</span>
            </div>
            <div class="min-w-0">
              <h2 class="truncate text-lg font-bold text-slate-900">{{ editingProduct ? '编辑产品' : '新建产品' }}</h2>
              <p class="mt-0.5 text-xs text-slate-500">维护产品资料、类目、价格和负责人。</p>
            </div>
          </div>
          <button
            type="button"
            class="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
            aria-label="关闭"
            @click="formVisible = false"
          >
            <span class="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>
      </template>

      <div class="bg-white px-5 pb-6 pt-5 md:px-6 md:pb-7">
        <el-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-position="top"
          class="wk-product-form grid grid-cols-1 gap-4 md:grid-cols-2"
        >
          <el-form-item label="产品名称" prop="productName" class="md:col-span-2">
            <el-input v-model="form.productName" maxlength="100" show-word-limit size="large" class="wk-crm-el-field-input" placeholder="请输入产品名称" />
          </el-form-item>
          <el-form-item label="产品编码" prop="productCode" class="md:col-span-2">
            <el-input v-model="form.productCode" maxlength="100" placeholder="租户内唯一" size="large" class="wk-crm-el-field-input" />
          </el-form-item>
          <el-form-item label="产品主图" class="md:col-span-2">
            <div class="product-main-image-field">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/*"
                :on-change="handleMainImageChange"
                :disabled="mainImageUploading"
              >
                <button type="button" class="product-main-image-uploader">
                  <img v-if="form.mainImageUrl" :src="form.mainImageUrl" alt="产品主图" class="size-full object-cover" />
                  <span v-else-if="mainImageUploading" class="material-symbols-outlined animate-spin text-[24px]">progress_activity</span>
                  <span v-else class="material-symbols-outlined text-[24px]">add_photo_alternate</span>
                </button>
              </el-upload>
              <div class="min-w-0">
                <p class="text-sm font-semibold text-slate-700">{{ form.mainImage ? '已上传产品主图' : '上传产品主图' }}</p>
                <p class="mt-1 text-xs leading-5 text-slate-400">仅支持一张图片，重新上传会替换当前主图。</p>
                <button
                  v-if="form.mainImage"
                  type="button"
                  class="mt-2 text-xs font-semibold text-rose-500 transition-colors hover:text-rose-600"
                  @click="removeMainImage"
                >
                  移除图片
                </button>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="产品类目" class="md:col-span-2">
            <el-tree-select
              v-model="form.categoryId"
              :data="categories"
              node-key="categoryId"
              check-strictly
              clearable
              default-expand-all
              :props="{ label: 'categoryName', children: 'children', value: 'categoryId' }"
              placeholder="请选择产品类目"
              size="large"
              class="wk-crm-el-field-select w-full"
            />
          </el-form-item>
          <el-form-item label="产品类型">
            <el-select v-model="form.productType" clearable placeholder="请选择产品类型" size="large" class="wk-crm-el-field-select w-full">
              <el-option v-for="type in productTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
          </el-form-item>
        <el-form-item label="单位">
            <el-select v-model="form.unit" clearable placeholder="请选择单位" size="large" class="wk-crm-el-field-select w-full">
              <el-option v-for="unit in unitOptions" :key="unit.value" :label="unit.label" :value="unit.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="标准价">
            <el-input-number v-model="form.standardPrice" :min="0" :precision="2" size="large" class="wk-crm-el-field-input w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="成本价">
            <el-input-number v-model="form.costPrice" :min="0" :precision="2" size="large" class="wk-crm-el-field-input w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="负责人" class="md:col-span-2">
            <el-select
              v-model="form.ownerId"
              filterable
              remote
              clearable
              reserve-keyword
              placeholder="默认当前用户"
              :remote-method="loadUserOptions"
              :loading="userLoading"
              size="large"
              class="wk-crm-el-field-select w-full"
            >
              <el-option v-for="user in userOptions" :key="user.userId" :label="userLabel(user)" :value="String(user.userId)" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述" class="md:col-span-2">
            <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit resize="none" class="wk-crm-el-field-input" placeholder="补充产品说明" />
          </el-form-item>
          <DynamicFieldForm
            v-model="customFieldValues"
            entity-type="product"
            mode="custom"
            :entity-id="editingProduct?.productId || null"
            class="grid grid-cols-1 gap-4 md:col-span-2 md:grid-cols-2"
          />
        </el-form>
      </div>

      <template #footer>
        <div class="flex gap-3">
          <button
            type="button"
            class="flex-1 rounded-xl bg-slate-100 py-2.5 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-200"
            @click="formVisible = false"
          >
            取消
          </button>
          <button
            type="button"
            class="flex-1 rounded-xl bg-primary py-2.5 text-sm font-bold text-white shadow-sm transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="saving || !form.productName.trim() || (settingsForm.codeRequired && !form.productCode.trim())"
            @click="submitForm"
          >
            {{ saving ? '提交中...' : (editingProduct ? '保存修改' : '创建产品') }}
          </button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="转移负责人" :width="isMobile ? '92%' : '420px'">
      <el-form label-width="96px">
        <el-form-item label="新负责人">
          <el-select
            v-model="transferOwnerId"
            filterable
            remote
            reserve-keyword
            :remote-method="loadUserOptions"
            :loading="userLoading"
            placeholder="搜索员工"
          >
            <el-option v-for="user in userOptions" :key="user.userId" :label="userLabel(user)" :value="String(user.userId)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTransfer">确认转移</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="settingsVisible" title="产品设置" :width="isMobile ? '92%' : '420px'">
      <el-form label-width="140px">
        <el-form-item label="产品编码必填">
          <el-switch v-model="settingsForm.codeRequired" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSettings">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryVisible" :title="editingCategory ? '编辑类目' : '新增类目'" :width="isMobile ? '92%' : '420px'">
      <el-form label-width="96px">
        <el-form-item label="上级类目">
          <el-tree-select
            v-model="categoryForm.parentId"
            :data="categories"
            node-key="categoryId"
            check-strictly
            clearable
            default-expand-all
            :props="{ label: 'categoryName', children: 'children', value: 'categoryId' }"
          />
        </el-form-item>
        <el-form-item label="类目名称" required>
          <el-input v-model="categoryForm.categoryName" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入产品" :width="isMobile ? '94%' : '760px'">
      <div class="space-y-4">
        <div class="flex flex-wrap items-center gap-2">
          <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleImportFileChange">
            <el-button :icon="Upload">选择 Excel</el-button>
          </el-upload>
          <el-button text :icon="Download" @click="handleDownloadTemplate">下载模板</el-button>
          <span class="text-sm text-slate-500">{{ importFile?.name || '未选择文件' }}</span>
        </div>
        <div v-if="importPreview" class="rounded-md border border-slate-200 p-3 text-sm text-slate-600">
          共 {{ importPreview.totalRows }} 行，有效 {{ importPreview.validRows }} 行，错误 {{ importPreview.errorRows }} 行，重复编码 {{ importPreview.duplicateRows }} 行
        </div>
        <el-table v-if="importPreview" :data="importPreview.rows" max-height="300" size="small">
          <el-table-column prop="rowNum" label="行" width="64" />
          <el-table-column prop="productName" label="产品名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="productCode" label="编码" width="140" show-overflow-tooltip />
          <el-table-column prop="categoryPath" label="类目" min-width="160" show-overflow-tooltip />
          <el-table-column prop="ownerName" label="负责人" width="110" />
          <el-table-column label="结果" min-width="180">
            <template #default="{ row }">
              <el-tag v-if="row.errors?.length" type="danger" size="small">错误</el-tag>
              <el-tag v-else-if="row.duplicate" type="warning" size="small">确认后更新</el-tag>
              <el-tag v-else type="success" size="small">新增</el-tag>
              <span class="ml-2 text-xs text-slate-500">{{ row.errors?.join('；') }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="importPreview?.errors?.length" class="rounded-md bg-rose-50 p-3 text-sm text-rose-600">
          <p v-for="error in importPreview.errors" :key="error">{{ error }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!importPreview || importPreview.validRows === 0" :loading="saving" @click="submitImport">
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { ChatDotRound, Download, Edit, FolderAdd, MoreFilled, Plus, Search, Switch, Upload } from '@element-plus/icons-vue'
import { queryUserList } from '@/api/auth'
import { getFormFieldsByEntity } from '@/api/customField'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import {
  addProduct,
  addProductCategory,
  confirmProductImport,
  deleteProduct,
  deleteProductCategory,
  downloadProductImportTemplate,
  exportProducts,
  getProductCategoryTree,
  getProductDetail,
  getProductSettings,
  importProductPreview,
  moveProductCategory,
  queryProductList,
  transferProducts,
  updateProduct,
  updateProductCategory,
  updateProductSettings,
  updateProductStatus
} from '@/api/product'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'
import { appEvents, APP_EVENT } from '@/utils/events'
import { formatProductCreatorName, formatProductTypeLabel } from '@/utils/productDisplay'
import type { FieldOption } from '@/types/customField'
import type { ProductCategoryVO, ProductImportPreviewVO, ProductQueryBO, ProductStatus, ProductVO } from '@/types/product'

const InfoItem = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: String, required: true }
  },
  setup(props) {
    return () => h('div', { class: 'rounded-md border border-slate-200 bg-white px-3 py-2' }, [
      h('div', { class: 'text-[12px] text-slate-400' }, props.label),
      h('div', { class: 'mt-1 break-words text-sm font-medium text-slate-800' }, props.value)
    ])
  }
})

interface UserOption {
  userId: string | number
  realname?: string
  username?: string
  status?: number
}

interface ProductTypeOption {
  value: string
  label: string
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const products = ref<ProductVO[]>([])
const categories = ref<ProductCategoryVO[]>([])
const categoryPanelCollapsed = ref(false)
const total = ref(0)
const page = ref(1)
const limit = ref(20)
const DEFAULT_PRODUCT_TYPE_OPTIONS: ProductTypeOption[] = [
  { value: 'goods', label: '商品' },
  { value: 'service', label: '服务' }
]
const productTypeOptions = ref<ProductTypeOption[]>([...DEFAULT_PRODUCT_TYPE_OPTIONS])
const DEFAULT_UNIT_OPTIONS: ProductTypeOption[] = [
  { value: '个', label: '个' },
  { value: '套', label: '套' },
  { value: '台', label: '台' },
  { value: '件', label: '件' },
  { value: '年', label: '年' },
  { value: '月', label: '月' },
  { value: '次', label: '次' }
]
const unitOptions = ref<ProductTypeOption[]>([...DEFAULT_UNIT_OPTIONS])

const query = reactive<ProductQueryBO>({
  keyword: '',
  categoryId: undefined,
  includeChildCategory: true,
  productType: '',
  status: ''
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentProduct = ref<ProductVO | null>(null)

const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingProduct = ref<ProductVO | null>(null)
const customFieldValues = ref<Record<string, unknown>>({})
const form = reactive({
  productName: '',
  productCode: '',
  mainImage: '',
  mainImageUrl: '',
  categoryId: '',
  productType: '',
  unit: '',
  standardPrice: undefined as number | undefined,
  costPrice: undefined as number | undefined,
  ownerId: '',
  description: ''
})

const transferVisible = ref(false)
const transferOwnerId = ref('')
const transferTargetIds = ref<string[]>([])

const settingsVisible = ref(false)
const settingsForm = reactive({ codeRequired: true })

const categoryVisible = ref(false)
const editingCategory = ref<ProductCategoryVO | null>(null)
const categoryForm = reactive({ categoryId: '', parentId: '', categoryName: '' })

const importVisible = ref(false)
const importFile = ref<File | null>(null)
const importPreview = ref<ProductImportPreviewVO | null>(null)
const mainImageUploading = ref(false)

const userLoading = ref(false)
const userOptions = ref<UserOption[]>([])

const canCreate = computed(() => userStore.hasPermission('product:create'))
const canEdit = computed(() => userStore.hasPermission('product:edit'))
const canDelete = computed(() => userStore.hasPermission('product:delete'))
const canUpdateStatus = computed(() => userStore.hasPermission('product:update_status'))
const canTransfer = computed(() => userStore.hasPermission('product:transfer'))
const canImport = computed(() => userStore.hasPermission('product:import'))
const canExport = computed(() => userStore.hasPermission('product:export'))
const canSettings = computed(() => userStore.hasPermission('product:settings'))
const canManageCategory = computed(() => userStore.hasPermission('product:category_manage'))
const showHeaderMoreActions = computed(() => canImport.value || canExport.value)

const formRules = computed<FormRules>(() => ({
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productCode: settingsForm.codeRequired
    ? [{ required: true, message: '请输入产品编码', trigger: 'blur' }]
    : []
}))

const customFieldEntries = computed(() => {
  const fields = currentProduct.value?.customFields || {}
  return Object.entries(fields)
    .filter(([, value]) => value !== null && value !== undefined && value !== '')
    .map(([key, value]) => ({ key, value }))
})

function normalizeId(value: unknown): string | undefined {
  if (value === null || value === undefined || value === '') return undefined
  return String(value)
}

function userLabel(user: UserOption): string {
  return user.realname || user.username || String(user.userId)
}

function statusLabel(status?: ProductStatus | string): string {
  return status === 'inactive' ? '停用' : '启用'
}

function normalizeProductTypeOption(option: Partial<FieldOption> | ProductTypeOption | null | undefined): ProductTypeOption | null {
  const value = String(option?.value ?? '').trim()
  if (!value) return null
  return {
    value,
    label: String(option?.label || formatProductTypeLabel(value)).trim()
  }
}

function mergeProductTypeOptions(options: ProductTypeOption[]) {
  const merged = new Map<string, ProductTypeOption>()
  productTypeOptions.value.forEach(option => {
    merged.set(option.value, option)
  })
  options.forEach(option => {
    const normalized = normalizeProductTypeOption(option)
    if (normalized) {
      merged.set(normalized.value, normalized)
    }
  })
  productTypeOptions.value = Array.from(merged.values())
}

function normalizeUnitOption(option: Partial<FieldOption> | ProductTypeOption | null | undefined): ProductTypeOption | null {
  const value = String(option?.value ?? '').trim()
  if (!value) return null
  return {
    value,
    label: String(option?.label || value).trim()
  }
}

function mergeUnitOptions(options: ProductTypeOption[]) {
  const merged = new Map<string, ProductTypeOption>()
  unitOptions.value.forEach(option => {
    merged.set(option.value, option)
  })
  options.forEach(option => {
    const normalized = normalizeUnitOption(option)
    if (normalized) {
      merged.set(normalized.value, normalized)
    }
  })
  unitOptions.value = Array.from(merged.values())
}

function productTypeLabel(value?: string | null): string {
  const raw = String(value ?? '').trim()
  if (!raw) return '-'
  return productTypeOptions.value.find(option => option.value === raw)?.label || formatProductTypeLabel(raw)
}

function unitLabel(value?: string | null): string {
  const raw = String(value ?? '').trim()
  if (!raw) return '-'
  return unitOptions.value.find(option => option.value === raw)?.label || raw
}

function money(value?: number | string): string {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  if (Number.isNaN(n)) return String(value)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDateTime(value?: string): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

function buildQuery(): ProductQueryBO {
  return {
    ...query,
    categoryId: normalizeId(query.categoryId),
    includeChildCategory: Boolean(query.includeChildCategory),
    productType: query.productType || undefined,
    status: query.status || undefined,
    page: page.value,
    limit: limit.value
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const result = await queryProductList(buildQuery())
    products.value = result.list || []
    total.value = result.totalRow || 0
    mergeProductTypeOptions(
      products.value
        .map(item => normalizeProductTypeOption({ value: item.productType || '', label: productTypeLabel(item.productType) }))
        .filter((item): item is ProductTypeOption => Boolean(item))
    )
  } finally {
    loading.value = false
  }
}

async function loadProductTypeOptions() {
  try {
    const fields = await getFormFieldsByEntity('product')
    const productTypeField = fields.find(field => field.fieldName === 'productType' || field.fieldLabel === '产品类型')
    const unitField = fields.find(field => field.fieldName === 'unit' || field.fieldLabel === '单位')
    mergeProductTypeOptions((productTypeField?.options || [])
      .map(option => normalizeProductTypeOption(option))
      .filter((item): item is ProductTypeOption => Boolean(item)))
    mergeUnitOptions((unitField?.options || [])
      .map(option => normalizeUnitOption(option))
      .filter((item): item is ProductTypeOption => Boolean(item)))
  } catch {
    // The default options keep the form usable if field config is unavailable.
  }
}

function refreshProductSidebar() {
  appEvents.emit(APP_EVENT.PRODUCT_SIDEBAR_REFRESH, { preserveScroll: true })
}

async function loadCategories() {
  categories.value = await getProductCategoryTree()
}

async function loadSettingsSafe() {
  if (!canSettings.value) return
  try {
    const settings = await getProductSettings()
    settingsForm.codeRequired = settings.codeRequired
  } catch {
    // permission may be absent on some roles; defaults still work for validation hints
  }
}

function selectCategory(categoryId?: string) {
  query.categoryId = categoryId
  page.value = 1
  void loadProducts()
}

function applyFilters() {
  page.value = 1
  void loadProducts()
}

function handleSizeChange() {
  page.value = 1
  void loadProducts()
}

async function openDetail(row: ProductVO) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    currentProduct.value = await getProductDetail(String(row.productId))
  } finally {
    detailLoading.value = false
  }
}

function resetForm(product?: ProductVO | null) {
  editingProduct.value = product || null
  form.productName = product?.productName || ''
  form.productCode = product?.productCode || ''
  form.mainImage = product?.mainImage || ''
  form.mainImageUrl = product?.mainImageUrl || ''
  form.categoryId = product?.categoryId ? String(product.categoryId) : ''
  form.productType = product?.productType || ''
  form.unit = product?.unit || ''
  form.standardPrice = product?.standardPrice === undefined || product?.standardPrice === '' ? undefined : Number(product.standardPrice)
  form.costPrice = product?.costPrice === undefined || product?.costPrice === '' ? undefined : Number(product.costPrice)
  form.ownerId = product?.ownerId ? String(product.ownerId) : ''
  form.description = product?.description || ''
  customFieldValues.value = { ...(product?.customFields || {}) }
  if (product?.unit) {
    mergeUnitOptions([{ value: product.unit, label: product.unit }])
  }
}

async function handleMainImageChange(uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }
  mainImageUploading.value = true
  try {
    const presigned = await getPresignedUploadUrl(raw.name, raw.type, 'product/main-images')
    await uploadToMinIO(raw, presigned.uploadUrl)
    form.mainImage = presigned.objectKey
    form.mainImageUrl = presigned.accessUrl
  } finally {
    mainImageUploading.value = false
  }
}

function removeMainImage() {
  form.mainImage = ''
  form.mainImageUrl = ''
}

function openCreateDialog() {
  resetForm()
  form.categoryId = query.categoryId || ''
  formVisible.value = true
}

function openEditDialog(product: ProductVO) {
  resetForm(product)
  formVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      productName: form.productName.trim(),
      productCode: form.productCode.trim() || undefined,
      mainImage: form.mainImage,
      categoryId: form.categoryId || undefined,
      productType: form.productType.trim() || undefined,
      unit: form.unit.trim() || undefined,
      standardPrice: form.standardPrice,
      costPrice: form.costPrice,
      ownerId: form.ownerId || undefined,
      description: form.description.trim() || undefined,
      customFields: customFieldValues.value
    }
    if (editingProduct.value) {
      await updateProduct({ ...payload, productId: String(editingProduct.value.productId) })
      ElMessage.success('产品已更新')
    } else {
      await addProduct(payload)
      ElMessage.success('产品已创建')
    }
    formVisible.value = false
    await Promise.all([loadProducts(), loadCategories()])
    refreshProductSidebar()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(product: ProductVO) {
  const nextStatus: ProductStatus = product.status === 'active' ? 'inactive' : 'active'
  await updateProductStatus({ productId: String(product.productId), status: nextStatus })
  ElMessage.success(nextStatus === 'active' ? '产品已启用' : '产品已停用')
  await loadProducts()
  refreshProductSidebar()
  if (currentProduct.value?.productId === product.productId) {
    currentProduct.value = await getProductDetail(String(product.productId))
  }
}

async function handleDelete(product: ProductVO) {
  await ElMessageBox.confirm(`确认删除产品「${product.productName}」？删除后默认列表不再显示。`, '删除产品', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteProduct(String(product.productId))
  ElMessage.success('产品已删除')
  detailVisible.value = false
  await loadProducts()
  refreshProductSidebar()
}

function openTransferDialog(rows: ProductVO[]) {
  transferTargetIds.value = rows.map(row => String(row.productId))
  transferOwnerId.value = ''
  transferVisible.value = true
  void loadUserOptions('')
}

async function submitTransfer() {
  if (!transferOwnerId.value) {
    ElMessage.warning('请选择新负责人')
    return
  }
  saving.value = true
  try {
    await transferProducts({ productIds: transferTargetIds.value, ownerId: transferOwnerId.value })
    ElMessage.success('负责人已转移')
    transferVisible.value = false
    await loadProducts()
    refreshProductSidebar()
  } finally {
    saving.value = false
  }
}

async function openSettingsDialog() {
  await loadSettingsSafe()
  settingsVisible.value = true
}

async function submitSettings() {
  saving.value = true
  try {
    await updateProductSettings({ codeRequired: settingsForm.codeRequired })
    ElMessage.success('产品设置已保存')
    settingsVisible.value = false
  } finally {
    saving.value = false
  }
}

function openCategoryDialog(category?: ProductCategoryVO, edit = false) {
  editingCategory.value = edit && category ? category : null
  categoryForm.categoryId = edit && category ? String(category.categoryId) : ''
  categoryForm.parentId = edit ? (category?.parentId ? String(category.parentId) : '') : (category?.categoryId ? String(category.categoryId) : '')
  categoryForm.categoryName = edit && category ? category.categoryName : ''
  categoryVisible.value = true
}

function findCategoryContext(
  categoryId: string,
  nodes: ProductCategoryVO[] = categories.value,
  parent: ProductCategoryVO | null = null
): { category: ProductCategoryVO; siblings: ProductCategoryVO[]; index: number; parent: ProductCategoryVO | null } | null {
  for (let index = 0; index < nodes.length; index += 1) {
    const category = nodes[index]
    if (String(category.categoryId) === categoryId) {
      return { category, siblings: nodes, index, parent }
    }
    const childResult = findCategoryContext(categoryId, category.children || [], category)
    if (childResult) return childResult
  }
  return null
}

function canMoveCategory(category: ProductCategoryVO, direction: 'up' | 'down') {
  const context = findCategoryContext(String(category.categoryId))
  if (!context) return false
  return direction === 'up'
    ? context.index > 0
    : context.index < context.siblings.length - 1
}

async function handleMoveCategory(category: ProductCategoryVO, direction: 'up' | 'down') {
  const context = findCategoryContext(String(category.categoryId))
  if (!context || !canMoveCategory(category, direction)) return
  const targetIndex = direction === 'up' ? context.index - 1 : context.index + 1
  const target = context.siblings[targetIndex]
  const targetSortOrder = Number(target.sortOrder ?? targetIndex * 10)
  await moveProductCategory({
    categoryId: String(category.categoryId),
    parentId: context.parent?.categoryId ? String(context.parent.categoryId) : undefined,
    sortOrder: direction === 'up' ? targetSortOrder - 1 : targetSortOrder + 1
  })
  await loadCategories()
}

async function submitCategory() {
  if (!categoryForm.categoryName.trim()) {
    ElMessage.warning('请输入类目名称')
    return
  }
  saving.value = true
  try {
    if (editingCategory.value) {
      await updateProductCategory({
        categoryId: categoryForm.categoryId,
        parentId: categoryForm.parentId || undefined,
        categoryName: categoryForm.categoryName.trim()
      })
      ElMessage.success('类目已更新')
    } else {
      await addProductCategory({
        parentId: categoryForm.parentId || undefined,
        categoryName: categoryForm.categoryName.trim()
      })
      ElMessage.success('类目已创建')
    }
    categoryVisible.value = false
    await loadCategories()
  } finally {
    saving.value = false
  }
}

async function handleDeleteCategory(category: ProductCategoryVO) {
  await ElMessageBox.confirm(`确认删除类目「${category.categoryName}」？有子类目或绑定产品时系统会阻止删除。`, '删除类目', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteProductCategory(String(category.categoryId))
  ElMessage.success('类目已删除')
  if (String(query.categoryId || '') === String(category.categoryId)) {
    query.categoryId = undefined
  }
  await Promise.all([loadCategories(), loadProducts()])
}

function saveBlob(blob: Blob, filename: string) {
  const href = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = href
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(href)
}

async function handleExport() {
  const blob = await exportProducts(buildQuery())
  saveBlob(blob, `products-${Date.now()}.xlsx`)
}

function handleHeaderMoreCommand(command: 'import' | 'export' | 'settings') {
  if (command === 'import') {
    openImportDialog()
    return
  }
  if (command === 'export') {
    void handleExport()
    return
  }
  if (command === 'settings') {
    void openSettingsDialog()
  }
}

function openImportDialog() {
  importVisible.value = true
  importFile.value = null
  importPreview.value = null
}

async function handleDownloadTemplate() {
  const blob = await downloadProductImportTemplate()
  saveBlob(blob, 'product-import-template.xlsx')
}

async function handleImportFileChange(uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  importFile.value = raw
  importPreview.value = await importProductPreview(raw)
}

async function submitImport() {
  if (!importPreview.value) return
  saving.value = true
  try {
    const result = await confirmProductImport(importPreview.value.rows)
    ElMessage.success(`导入完成，新增 ${result.imported} 条，更新 ${result.updated} 条，跳过 ${result.skipped} 条`)
    importVisible.value = false
    await Promise.all([loadProducts(), loadCategories()])
    refreshProductSidebar()
  } finally {
    saving.value = false
  }
}

async function loadUserOptions(search = '') {
  userLoading.value = true
  try {
    const result = await queryUserList({ search, page: 1, limit: 30 })
    userOptions.value = (result.list || []).filter((user: UserOption) => user.status === undefined || user.status === 1)
  } finally {
    userLoading.value = false
  }
}

async function openProductChat(product: ProductVO) {
  await chatStore.openProductChat({
    productId: String(product.productId),
    productName: product.productName
  })
  await router.push({ path: '/chat', query: { productId: String(product.productId) } })
}

async function maybeOpenProductFromQuery() {
  const productId = route.query.openProductId || route.query.productId
  if (!productId || Array.isArray(productId)) return
  const detail = await getProductDetail(String(productId))
  await openDetail(detail)
}

async function maybeOpenProductEditFromQuery() {
  const productId = route.query.editProductId
  if (!productId || Array.isArray(productId)) return
  if (!canEdit.value) {
    ElMessage.warning('暂无编辑产品权限')
    return
  }
  const detail = await getProductDetail(String(productId))
  openEditDialog(detail)
}

watch(
  () => route.query.openProductId,
  () => {
    if (route.name === 'ProductList') void maybeOpenProductFromQuery()
  }
)

watch(
  () => route.query.editProductId,
  () => {
    if (route.name === 'ProductList') void maybeOpenProductEditFromQuery()
  }
)

onMounted(async () => {
  await Promise.all([loadCategories(), loadSettingsSafe(), loadProductTypeOptions()])
  await loadProducts()
  await maybeOpenProductEditFromQuery()
  await maybeOpenProductFromQuery()
})
</script>

<style scoped>
.product-page :deep(.el-button) {
  border-radius: 6px;
}

.product-page :deep(.el-table__body .el-table__row) {
  cursor: pointer;
}

.product-main-image-thumb {
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 8px;
  background: rgb(245 245 245);
  color: rgb(143 143 143);
}

.product-main-image-field {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 14px;
}

.product-main-image-uploader {
  display: inline-flex;
  width: 88px;
  height: 88px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed rgb(203 213 225);
  border-radius: 12px;
  background: rgb(248 250 252);
  color: rgb(100 116 139);
  transition: border-color 0.15s ease, background-color 0.15s ease, color 0.15s ease;
}

.product-main-image-uploader:hover {
  border-color: rgb(15 23 42);
  background: #fff;
  color: rgb(15 23 42);
}

.product-list-pager {
  position: sticky;
  bottom: 0;
  z-index: 5;
  display: flex;
  min-height: 56px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid rgb(226 232 240);
  background: #fff;
  padding: 10px 16px;
}

.product-list-pager__total {
  flex-shrink: 0;
  color: rgb(100 116 139);
  font-size: 13px;
}

.product-list-pager :deep(.el-pagination) {
  min-width: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.product-category-panel--collapsed {
  min-width: 48px;
}

.product-category-panel-toggle {
  display: inline-flex;
  position: absolute;
  top: 14px;
  right: -15px;
  z-index: 10;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(226 232 240);
  border-radius: 999px;
  background: #fff;
  color: rgb(100 116 139);
  box-shadow: 0 8px 20px rgb(15 23 42 / 8%);
  transition: border-color 0.15s ease, background-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.product-category-panel-toggle:hover {
  border-color: rgb(203 213 225);
  background: rgb(241 245 249);
  color: rgb(15 23 42);
  box-shadow: 0 10px 24px rgb(15 23 42 / 12%);
}

.product-category-tree :deep(.el-tree-node__content) {
  height: 34px;
  border-radius: 6px;
}

.product-category-tree :deep(.el-tree-node__content:hover) {
  background: rgb(248 250 252);
}

.product-category-node {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.product-category-node__name {
  min-width: 0;
  max-width: 100%;
  transition: max-width 0.15s ease;
}

.product-category-node__actions {
  position: absolute;
  top: 50%;
  right: 4px;
  max-width: calc(100% - 12px);
  overflow: hidden;
  padding-left: 8px;
  background: linear-gradient(90deg, transparent 0, var(--wk-bg-surface) 12px, var(--wk-bg-surface) 100%);
  opacity: 0;
  pointer-events: none;
  transform: translateY(-50%);
  transition: opacity 0.15s ease;
}

.product-category-node:hover .product-category-node__name,
.product-category-tree :deep(.el-tree-node__content:hover) .product-category-node__name {
  max-width: calc(100% - 144px);
}

.product-category-node:hover .product-category-node__actions,
.product-category-tree :deep(.el-tree-node__content:hover) .product-category-node__actions {
  opacity: 1;
  pointer-events: auto;
}

.product-icon-btn {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: inherit;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.product-icon-btn:hover {
  background: rgb(241 245 249);
  color: rgb(15 23 42);
}

.product-icon-btn:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

:global(.wk-product-dialog.el-dialog) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:global(.wk-product-dialog.el-dialog .el-dialog__header) {
  flex-shrink: 0;
  margin-right: 0;
  padding: 22px 24px 14px !important;
}

:global(.wk-product-dialog.el-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 !important;
}

:global(.wk-product-dialog.el-dialog .el-dialog__footer) {
  flex-shrink: 0;
  padding: 14px 24px 22px !important;
}

:global(.wk-product-dialog--desktop.el-dialog) {
  max-height: calc(100vh - 20vh);
  margin-bottom: 10vh;
}

:global(.wk-product-dialog--mobile.el-dialog) {
  height: calc(100dvh - 32px);
  max-height: calc(100dvh - 32px);
  margin: 16px auto !important;
  border-radius: 1rem !important;
}

:global(.wk-product-dialog .wk-product-form .el-form-item) {
  margin-bottom: 0;
}

:global(.wk-product-dialog .wk-product-form .el-form-item__label) {
  margin-bottom: 6px;
  color: rgb(100 116 139);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.25;
}

:global(.wk-product-dialog .el-input-number .el-input__wrapper) {
  width: 100%;
}

:global(.el-overlay:has(.wk-product-dialog)),
:global(.el-overlay-dialog:has(.wk-product-dialog)) {
  overflow: hidden;
}
</style>
