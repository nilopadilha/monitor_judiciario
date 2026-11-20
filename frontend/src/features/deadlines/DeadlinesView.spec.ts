import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

vi.mock('@/shared/api/endpoints', () => ({
  deadlinesApi: {
    summary: vi.fn(),
    list: vi.fn(),
    create: vi.fn(),
    updateStatus: vi.fn(),
    triggerAlerts: vi.fn(),
  },
  lawyersApi: {
    list: vi.fn().mockResolvedValue({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }),
  },
  processesApi: {
    list: vi.fn().mockResolvedValue({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }),
  },
}))

import { deadlinesApi } from '@/shared/api/endpoints'
import DeadlinesView from './DeadlinesView.vue'

describe('DeadlinesView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setActivePinia(createPinia())
  })

  it('renders urgency summary, lists deadlines, and marks one as completed', async () => {
    vi.mocked(deadlinesApi.summary).mockResolvedValue({
      overdue: 1,
      dueToday: 0,
      dueIn3Days: 2,
      dueIn7Days: 3,
      openTotal: 4,
    })
    vi.mocked(deadlinesApi.list).mockResolvedValue({
      content: [
        {
          id: 'd1',
          processId: 'p1',
          processNumber: '1001234-83.2026.8.26.0100',
          courtCode: 'TJSP',
          clientName: 'Maria Souza',
          lawyerName: 'Dra. Beatriz',
          title: 'Prazo recursal (Sentença — 15 dias úteis)',
          startDate: '2026-11-10',
          dueDate: '2026-11-22',
          businessDays: true,
          status: 'OPEN',
          urgency: 'SOON',
          daysRemaining: 2,
          autoDetected: true,
          createdAt: '2026-11-10T00:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(deadlinesApi.updateStatus).mockResolvedValue({} as never)

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/prazos', component: DeadlinesView },
        { path: '/processos/:id', component: { template: '<div/>' } },
      ],
    })
    await router.push('/prazos')
    const wrapper = mount(DeadlinesView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Prazo recursal')
    expect(wrapper.text()).toContain('Automático')
    expect(wrapper.text()).toContain('Dra. Beatriz')

    await wrapper.findAll('button').find((b) => b.text().includes('Cumprido'))!.trigger('click')
    await flushPromises()
    expect(deadlinesApi.updateStatus).toHaveBeenCalledWith('d1', 'COMPLETED')
  })
})
