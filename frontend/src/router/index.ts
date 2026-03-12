import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/request'
import { getOidcSessionToken } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/RegisterView.vue'),
    meta: { requiresAuth: false }
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
        meta: { title: 'AI 助手', icon: 'auto_awesome' }
      },
      {
        path: 'customer',
        name: 'CustomerList',
        component: () => import('@/views/customer/CustomerListView.vue'),
        meta: { title: '客户管理', icon: 'group' }
      },
      {
        path: 'customer/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/CustomerDetailView.vue'),
        meta: { title: '客户详情', hidden: true }
      },
      {
        path: 'task',
        name: 'TaskList',
        component: () => import('@/views/task/TaskListView.vue'),
        meta: { title: '任务管理', icon: 'task_alt' }
      },
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('@/views/calendar/CalendarView.vue'),
        meta: { title: '日程安排', icon: 'calendar_today' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgeView.vue'),
        meta: { title: '知识库', icon: 'menu_book' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '系统设置', icon: 'settings' }
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
  } else if ((to.name === 'Login' || to.name === 'Register') && token) {
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
        } catch (e) {
          console.error('Failed to get OIDC session token:', e)
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
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch (e) {
        // token 失效，跳转登录页
        console.error('Failed to fetch user info:', e)
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
      }
    }
    next()
  } else {
    next()
  }
})

export default router
