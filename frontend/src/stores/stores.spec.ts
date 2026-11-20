import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/shared/api/endpoints', () => ({
  clientsApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    deactivate: vi.fn(),
  },
  processesApi: {
    list: vi.fn(),
    get: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    archive: vi.fn(),
    movements: vi.fn(),
  },
  monitoringApi: {
    list: vi.fn(),
    get: vi.fn(),
    start: vi.fn(),
  },
  publicationsApi: {
    list: vi.fn(),
    markRead: vi.fn(),
  },
  notificationsApi: {
    list: vi.fn(),
    send: vi.fn(),
  },
  dashboardApi: {
    summary: vi.fn(),
  },
}))

import { clientsApi, dashboardApi, monitoringApi, notificationsApi, processesApi, publicationsApi } from '@/shared/api/endpoints'
import { useClientsStore } from './clients'
import { useDashboardStore } from './dashboard'
import { useMonitoringStore } from './monitoring'
import { useNotificationsStore } from './notifications'
import { useProcessesStore } from './processes'

describe('domain stores', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setActivePinia(createPinia())
  })

  it('manages clients state and CRUD actions', async () => {
    const client = {
      id: 'c1',
      name: 'Horizonte Ltda',
      document: '11.222.333/0001-81',
      email: 'contato@horizonte.com',
      status: 'ACTIVE' as const,
      notifyEmail: true,
      notifyWhatsapp: false,
      createdAt: '2026-10-01T00:00:00Z',
    }
    vi.mocked(clientsApi.list).mockResolvedValue({ content: [client], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    vi.mocked(clientsApi.create).mockResolvedValue(client)
    vi.mocked(clientsApi.update).mockResolvedValue({ ...client, name: 'Horizonte S.A.' })
    vi.mocked(clientsApi.deactivate).mockResolvedValue({} as never)

    const store = useClientsStore()
    await store.fetchClients({ q: 'Horizonte', status: 'ACTIVE' })
    expect(store.page?.content).toHaveLength(1)

    const created = await store.createClient({ name: client.name, document: client.document, email: client.email, notifyEmail: true, notifyWhatsapp: false })
    expect(created.id).toBe('c1')

    const updated = await store.updateClient('c1', { name: 'Horizonte S.A.', document: client.document, email: client.email, notifyEmail: true, notifyWhatsapp: false })
    expect(updated.name).toBe('Horizonte S.A.')

    await store.deactivateClient('c1')
    expect(clientsApi.deactivate).toHaveBeenCalledWith('c1')
  })

  it('manages processes state, timeline movements and status changes', async () => {
    const proc = {
      id: 'p1',
      clientId: 'c1',
      number: '1001234-83.2026.8.26.0100',
      courtCode: 'TJSP',
      status: 'ACTIVE' as const,
      situation: 'MONITORED' as const,
      unreadPublications: 0,
      createdAt: '2026-10-01T00:00:00Z',
    }
    const mov = { id: 'm1', type: 'SENTENCA', typeLabel: 'Sentença', description: 'Sentença proferida', movementDate: '2026-10-20', source: 'DJEN' as const }
    vi.mocked(processesApi.list).mockResolvedValue({ content: [proc], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    vi.mocked(processesApi.get).mockResolvedValue(proc)
    vi.mocked(processesApi.movements).mockResolvedValue({ content: [mov], page: 0, size: 50, totalElements: 1, totalPages: 1 })
    vi.mocked(processesApi.create).mockResolvedValue(proc)
    vi.mocked(processesApi.update).mockResolvedValue({ ...proc, status: 'PAUSED', situation: 'PAUSED' })
    vi.mocked(processesApi.archive).mockResolvedValue({} as never)

    const store = useProcessesStore()
    await store.fetchProcesses({ situation: 'MONITORED' })
    expect(store.page?.content).toHaveLength(1)

    await store.fetchProcess('p1')
    expect(store.currentProcess?.number).toBe('1001234-83.2026.8.26.0100')
    expect(store.movements).toHaveLength(1)

    await store.createProcess({ clientId: 'c1', number: proc.number })
    await store.updateProcess('p1', { status: 'PAUSED' })
    expect(store.currentProcess?.status).toBe('PAUSED')

    await store.archiveProcess('p1')
    expect(processesApi.archive).toHaveBeenCalledWith('p1')
  })

  it('manages monitoring executions and publications', async () => {
    const exec = {
      id: 'e1',
      trigger: 'MANUAL' as const,
      triggeredBy: 'admin@x.com',
      status: 'COMPLETED' as const,
      startedAt: '2026-10-23T09:00:00Z',
      processesChecked: 2,
      newPublications: 1,
      failedJobs: 0,
      jobs: [],
    }
    const pub = { id: 'pub1', processId: 'p1', publicationDate: '2026-10-23', content: 'Intimação', source: 'DJEN' as const, read: false }
    vi.mocked(monitoringApi.list).mockResolvedValue({ content: [exec], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    vi.mocked(monitoringApi.get).mockResolvedValue(exec)
    vi.mocked(monitoringApi.start).mockResolvedValue({ ...exec, status: 'RUNNING' })
    vi.mocked(publicationsApi.list).mockResolvedValue({ content: [pub], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    vi.mocked(publicationsApi.markRead).mockResolvedValue({ ...pub, read: true })

    const store = useMonitoringStore()
    await store.fetchExecutions()
    expect(store.executions).toHaveLength(1)

    await store.startExecution()
    expect(store.selectedExecution?.status).toBe('RUNNING')

    await store.fetchExecution('e1')
    expect(store.selectedExecution?.status).toBe('COMPLETED')

    await store.fetchPublications({ processId: 'p1' })
    expect(store.publications).toHaveLength(1)

    const marked = await store.markPublicationRead('pub1')
    expect(marked.read).toBe(true)
  })

  it('manages notifications and dashboard summary', async () => {
    const notif = {
      id: 'n1',
      processId: 'p1',
      channel: 'EMAIL' as const,
      recipient: 'c@x.com',
      status: 'SENT' as const,
      attempts: 1,
      createdAt: '2026-10-23T09:00:00Z',
    }
    const summary = {
      monitoredProcesses: 5,
      pausedProcesses: 1,
      processesWithNews: 2,
      processesWithFailedCheck: 0,
      unreadPublications: 3,
      notificationsToday: 4,
      failedNotificationsToday: 0,
      publicationsPerDay: Array.from({ length: 30 }, (_, i) => ({ date: `2026-10-${String(i + 1).padStart(2, '0')}`, count: 0 })),
      publicationsByTribunal: [{ courtCode: 'TJSP', count: 3 }],
    }
    vi.mocked(notificationsApi.list).mockResolvedValue({ content: [notif], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    vi.mocked(notificationsApi.send).mockResolvedValue(notif)
    vi.mocked(dashboardApi.summary).mockResolvedValue(summary)

    const notifStore = useNotificationsStore()
    await notifStore.fetchNotifications({ channel: 'EMAIL' })
    expect(notifStore.page?.content).toHaveLength(1)
    const sent = await notifStore.sendManual({ processId: 'p1', channel: 'EMAIL', message: 'Aviso' })
    expect(sent.status).toBe('SENT')

    const dashStore = useDashboardStore()
    await dashStore.fetchSummary()
    expect(dashStore.summary?.monitoredProcesses).toBe(5)
    expect(dashStore.summary?.publicationsPerDay).toHaveLength(30)
  })
})
