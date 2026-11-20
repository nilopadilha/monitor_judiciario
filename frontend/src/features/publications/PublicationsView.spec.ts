import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

vi.mock('@/shared/api/endpoints', () => ({
  publicationsApi: {
    list: vi.fn(),
    markRead: vi.fn(),
  },
}))

import { publicationsApi } from '@/shared/api/endpoints'
import { useAuthStore } from '@/stores/auth'
import PublicationsView from './PublicationsView.vue'

describe('PublicationsView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setActivePinia(createPinia())
  })

  it('lists unread publications with source badge and allows staff to mark as read', async () => {
    const auth = useAuthStore()
    auth.user = { id: 'u1', name: 'Admin', email: 'a@x.com', role: 'ADMIN', active: true }
    vi.mocked(publicationsApi.list).mockResolvedValue({
      content: [
        {
          id: 'p1',
          processId: 'proc1',
          processNumber: '1001234-83.2026.8.26.0100',
          courtCode: 'TJSP',
          publicationDate: '2026-10-22',
          documentType: 'Sentença',
          content: 'Julgo procedente o pedido.',
          source: 'DATAJUD',
          read: false,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(publicationsApi.markRead).mockResolvedValue({} as never)

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/publicacoes', component: PublicationsView },
        { path: '/processos/:id', component: { template: '<div/>' } },
      ],
    })
    await router.push('/publicacoes')
    const wrapper = mount(PublicationsView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('1001234-83.2026.8.26.0100')
    expect(wrapper.text()).toContain('DATAJUD')

    await wrapper.findAll('button').find((b) => b.text().includes('Marcar como lida'))!.trigger('click')
    await flushPromises()
    expect(publicationsApi.markRead).toHaveBeenCalledWith('p1')
  })
})
