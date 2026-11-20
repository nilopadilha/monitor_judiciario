import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/shared/api/types'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    roles?: Role[]
    title?: string
  }
}

const staff: Role[] = ['ADMIN', 'MANAGER']
const adminOnly: Role[] = ['ADMIN']

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/features/auth/LoginView.vue'),
    meta: { public: true, title: 'Entrar' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'dashboard', component: () => import('@/features/dashboard/DashboardView.vue'), meta: { title: 'Painel' } },
      { path: 'processos', name: 'processes', component: () => import('@/features/processes/ProcessesView.vue'), meta: { title: 'Processos' } },
      { path: 'processos/:id', name: 'process', component: () => import('@/features/processes/ProcessDetailView.vue'), meta: { title: 'Processo' } },
      { path: 'publicacoes', name: 'publications', component: () => import('@/features/publications/PublicationsView.vue'), meta: { title: 'Publicações' } },
      { path: 'prazos', name: 'deadlines', component: () => import('@/features/deadlines/DeadlinesView.vue'), meta: { title: 'Prazos', roles: staff } },
      { path: 'clientes', name: 'clients', component: () => import('@/features/clients/ClientsView.vue'), meta: { title: 'Clientes', roles: staff } },
      { path: 'advogados', name: 'lawyers', component: () => import('@/features/lawyers/LawyersView.vue'), meta: { title: 'Advogados', roles: staff } },
      { path: 'notificacoes', name: 'notifications', component: () => import('@/features/notifications/NotificationsView.vue'), meta: { title: 'Notificações' } },
      { path: 'monitoramento', name: 'monitoring', component: () => import('@/features/monitoring/MonitoringView.vue'), meta: { title: 'Monitoramento', roles: staff } },
      { path: 'tribunais', name: 'tribunals', component: () => import('@/features/tribunals/TribunalsView.vue'), meta: { title: 'Tribunais', roles: adminOnly } },
      { path: 'usuarios', name: 'users', component: () => import('@/features/users/UsersView.vue'), meta: { title: 'Usuários', roles: adminOnly } },
      { path: 'auditoria', name: 'audit', component: () => import('@/features/audit/AuditView.vue'), meta: { title: 'Auditoria', roles: adminOnly } },
      { path: 'conta', name: 'account', component: () => import('@/features/account/AccountView.vue'), meta: { title: 'Minha Conta' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.init()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  if (to.meta.roles && auth.role && !to.meta.roles.includes(auth.role)) {
    return { name: 'dashboard' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Monitor Judicial` : 'Monitor Judicial'
})
