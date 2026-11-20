import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

vi.mock('@/shared/api/endpoints', () => ({
  authApi: {
    login: vi.fn(),
    refresh: vi.fn(),
    revoke: vi.fn().mockResolvedValue(undefined),
    me: vi.fn(),
    changePassword: vi.fn(),
  },
  lgpdApi: {
    updateMyPreferences: vi.fn(),
    exportMine: vi.fn(),
  },
}))

import { authApi, lgpdApi } from '@/shared/api/endpoints'
import { useAuthStore } from '@/stores/auth'
import AccountView from './AccountView.vue'

describe('AccountView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setActivePinia(createPinia())
  })

  it('shows LGPD panel for CUSTOMER and updates notification preferences', async () => {
    const auth = useAuthStore()
    auth.user = { id: 'u1', name: 'Maria', email: 'm@x.com', role: 'CUSTOMER', clientId: 'c1', active: true }
    vi.mocked(lgpdApi.updateMyPreferences).mockResolvedValue({
      id: 'c1',
      name: 'Maria',
      document: '***.123.***-**',
      email: 'm@x.com',
      status: 'ACTIVE',
      notifyEmail: true,
      notifyWhatsapp: false,
      createdAt: '2026-10-23T00:00:00Z',
    })

    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/login', component: { template: '<div/>' } }, { path: '/conta', component: AccountView }] })
    await router.push('/conta')
    const wrapper = mount(AccountView, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('Privacidade e LGPD')
    await wrapper.findAll('button').find((b) => b.text().includes('Salvar preferências'))!.trigger('click')
    await flushPromises()

    expect(lgpdApi.updateMyPreferences).toHaveBeenCalledWith(true, false)
    expect(wrapper.text()).toContain('Preferências de notificação atualizadas')
  })

  it('changes password and redirects to login', async () => {
    const auth = useAuthStore()
    auth.user = { id: 'u1', name: 'Admin', email: 'a@x.com', role: 'ADMIN', active: true }
    vi.mocked(authApi.changePassword).mockResolvedValue({} as never)
    vi.mocked(authApi.revoke).mockResolvedValue({} as never)

    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/login', component: { template: '<div/>' } }, { path: '/conta', component: AccountView }] })
    await router.push('/conta')
    const wrapper = mount(AccountView, { global: { plugins: [router] } })

    await wrapper.find('#cur-pass').setValue('atual-forte-123')
    await wrapper.find('#new-pass').setValue('nova-senha-forte-456')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authApi.changePassword).toHaveBeenCalledWith('atual-forte-123', 'nova-senha-forte-456')
    expect(router.currentRoute.value.path).toBe('/login')
  })
})
