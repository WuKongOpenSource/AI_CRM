import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/request'
import { getOidcSessionToken } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const settingsComponent = () => import('@/views/settings/SettingsView.vue')
const settingsMeta = {
  title: '系统设置',
  icon: 'set',
  permission: ['user', 'role', 'config', 'dept', 'customField']
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/legal/user-agreement',
    name: 'UserAgreement',
    component: () => import('@/views/legal/LegalDocumentView.vue'),
    props: { documentType: 'agreement' },
    meta: { requiresAuth: false, title: '用户协议' }
  },
  {
    path: '/legal/privacy-policy',
    name: 'PrivacyPolicy',
    component: () => import('@/views/legal/LegalDocumentView.vue'),
    props: { documentType: 'privacy' },
    meta: { requiresAuth: false, title: '隐私声明' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/chat'
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { title: 'AI 助手', icon: 'ai', permission: 'chat' }
      },
      {
        path: 'customer',
        name: 'CustomerList',
        component: () => import('@/views/customer/CustomerListView.vue'),
        meta: { title: '客户管理', icon: 'group', permission: 'customer:view' }
      },
      {
        path: 'address-book',
        name: 'AddressBook',
        component: () => import('@/views/addressBook/AddressBookListView.vue'),
        meta: { title: '通讯录', icon: 'contacts', permission: 'addressBook:list' }
      },
      {
        path: 'addressBook',
        redirect: '/address-book'
      },
      {
        path: 'customer/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/CustomerDetailView.vue'),
        meta: { title: '客户详情', hidden: true, permission: 'customer:view' }
      },
      {
        path: 'relation',
        name: 'RelationList',
        component: () => import('@/views/relation/RelationListView.vue'),
        meta: { title: '关系', icon: 'contacts' }
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/product/ProductListView.vue'),
        meta: { title: '产品管理', icon: 'inventory_2', permission: 'product:view' }
      },
      {
        path: 'project',
        name: 'ProjectList',
        component: () => import('@/views/project/ProjectListView.vue'),
        meta: { title: '项目管理', icon: 'task_alt', permission: 'task' }
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/project/ProjectDetailView.vue'),
        meta: { title: '项目详情', hidden: true, permission: 'task' }
      },
      {
        path: 'mail',
        name: 'Mail',
        component: () => import('@/views/mail/MailView.vue'),
        meta: { title: '邮箱', icon: 'mail', permission: 'mail:view' }
      },
      {
        path: 'task',
        name: 'TaskList',
        component: () => import('@/views/task/TaskListView.vue'),
        meta: { title: '任务管理', icon: 'task_alt', permission: 'task:view' }
      },
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('@/views/calendar/CalendarView.vue'),
        meta: { title: '日程安排', icon: 'calendar_today', permission: 'schedule' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgeView.vue'),
        meta: { title: '知识库', icon: 'menu_book', permission: 'knowledge' }
      },
      {
        path: 'settings',
        redirect: '/settings/team'
      },
      {
        path: 'settings/team',
        name: 'SettingsTeam',
        component: settingsComponent,
        meta: settingsMeta
      },
      {
        path: 'settings/role',
        name: 'SettingsRole',
        component: settingsComponent,
        meta: settingsMeta
      },
      {
        path: 'settings/system',
        redirect: '/settings/system/enterprise'
      },
      {
        path: 'settings/system/profile',
        redirect: '/settings/system/enterprise'
      },
      {
        path: 'settings/system/:systemTab(enterprise|api|auth|agent|storage|customField)',
        name: 'SettingsSystem',
        component: settingsComponent,
        meta: settingsMeta
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// Navigation guard
router.beforeEach(async (to, _from, next) => {
  const token = getToken()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && token) {
    // 用户已登录访问登录页，检查是否有 redirect 参数
    let redirect = to.query.redirect as string
    if (redirect) {
      // 如果是 OIDC 授权 URL，需要先获取 session_token
      if (redirect.includes('/oauth2/authorize')) {
        try {
          const { sessionToken } = await getOidcSessionToken()
          const url = new URL(redirect)
          url.searchParams.set('session_token', sessionToken)
          redirect = url.toString()
        } catch (error) {
          console.error('Failed to get OIDC session token:', error)
        }
      }
      // 如果是外部 URL，直接跳转
      if (redirect.startsWith('http://') || redirect.startsWith('https://')) {
        window.location.href = redirect
        return
      }
      next(redirect)
    } else {
      next({ name: 'Chat' })
    }
  } else if (requiresAuth && token) {
    // 有 token 但可能没有用户信息，需要获取
    const userStore = useUserStore()
    if (!userStore.userInfo || !userStore.permissionsLoaded) {
      try {
        await userStore.fetchUserInfo()
      } catch (error) {
        // token 失效，跳转登录页
        console.error('Failed to fetch user info:', error)
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
      }
    }
    // 权限检查
    const permission = to.meta.permission as string | string[] | undefined
    if (permission) {
      const hasAccess = Array.isArray(permission)
        ? permission.some(p => userStore.hasPermission(p))
        : (permission === 'chat' || userStore.hasPermission(permission))
      if (!hasAccess) {
        // 无权限，重定向到 AI 助手
        next({ name: 'Chat' })
        return
      }
    }
    next()
  } else {
    next()
  }
})

export default router
