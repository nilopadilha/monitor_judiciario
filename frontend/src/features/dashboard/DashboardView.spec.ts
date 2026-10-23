import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { createTestingPinia } from '@/features/auth/testing'

vi.mock('@/shared/api/endpoints', () => ({
  dashboardApi: { summary: vi.fn() },
  processesApi: { list: vi.fn(), get: vi.fn(), movements: vi.fn() },
  publicationsApi: { list: vi.fn(), markRead: vi.fn() },
  notificationsApi: { list: vi.fn() },
  monitoringApi: { start: vi.fn(), get: vi.fn() },
}))

import DashboardView from './DashboardView.vue'
import { dashboardApi, monitoringApi, notificationsApi, processesApi, publicationsApi } from '@/shared/api/endpoints'
import { useAuthStore } from '@/stores/auth'
import type { Process } from '@/shared/api/types'

const page = <T>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
const process: Process = {
  id: 'p1', clientId: 'c1', clientName: 'Horizonte', number: '1001234-83.2026.8.26.0100', courtCode: 'TJSP',
  status: 'ACTIVE', situation: 'NEW_PUBLICATION', unreadPublications: 1, createdAt: '2026-10-01T00:00:00Z',
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.mocked(dashboardApi.summary).mockResolvedValue({
      monitoredProcesses: 6, pausedProcesses: 1, processesWithNews: 2, processesWithFailedCheck: 1, unreadPublications: 3,
      notificationsToday: 4, failedNotificationsToday: 1, publicationsPerDay: [], publicationsByTribunal: [],
    })
    vi.mocked(processesApi.list).mockResolvedValue(page([process]))
    vi.mocked(processesApi.get).mockResolvedValue(process)
    vi.mocked(processesApi.movements).mockResolvedValue(page([]))
    vi.mocked(publicationsApi.list).mockResolvedValue(page([]))
    vi.mocked(notificationsApi.list).mockResolvedValue(page([]))
  })

  function render(role: 'ADMIN' | 'CUSTOMER') {
    const pinia = createTestingPinia()
    const auth = useAuthStore()
    auth.user = { id: 'u', name: 'U', email: 'u@x.com', role, active: true }
    return mount(DashboardView, {
      global: { plugins: [pinia], stubs: { RouterLink: RouterLinkStub, PublicationsChart: true } },
    })
  }

  it('shows KPIs, filters and selects the first process', async () => {
    const wrapper = render('ADMIN')
    await flushPromises()

    expect(wrapper.text()).toContain('Processos monitorados')
    expect(wrapper.text()).toContain('Com novidade · 2')
    expect(wrapper.text()).toContain('Com falha · 1')
    expect(processesApi.get).toHaveBeenCalledWith('p1')

    const failFilter = wrapper.findAll('button').find((b) => b.text().startsWith('Com falha'))
    await failFilter?.trigger('click')
    await flushPromises()
    expect(processesApi.list).toHaveBeenLastCalledWith({ situation: 'FAILED', size: 20 })
  })

  it('starts monitoring for staff and hides it for customers', async () => {
    vi.mocked(monitoringApi.start).mockResolvedValue({ id: 'e1' } as never)
    const staff = render('ADMIN')
    await flushPromises()
    const run = staff.findAll('button').find((b) => b.text().includes('Executar monitoramento'))
    await run?.trigger('click')
    await flushPromises()
    expect(monitoringApi.start).toHaveBeenCalled()
    expect(staff.text()).toContain('Consultando DJEN…')
    staff.unmount()

    const customer = render('CUSTOMER')
    await flushPromises()
    expect(customer.text()).not.toContain('Executar monitoramento')
  })
})
