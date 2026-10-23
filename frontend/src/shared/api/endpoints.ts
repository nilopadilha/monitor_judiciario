import { http } from './http'
import type {
  Channel,
  Client,
  ClientInput,
  DashboardSummary,
  MonitoringExecution,
  Movement,
  Notification,
  Page,
  Process,
  ProcessStatus,
  Publication,
  Situation,
  TokenPair,
  Tribunal,
  User,
} from './types'

type Query = Record<string, string | number | boolean | undefined>

const clean = (params: Query) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== ''))

export const authApi = {
  login: (email: string, password: string) =>
    http.post<TokenPair>('/tokens', { grantType: 'PASSWORD', email, password }).then((r) => r.data),
  refresh: (refreshToken: string) =>
    http.post<TokenPair>('/tokens', { grantType: 'REFRESH_TOKEN', refreshToken }).then((r) => r.data),
  revoke: (refreshToken: string) => http.post('/token-revocations', { refreshToken }),
  me: () => http.get<User>('/users/me').then((r) => r.data),
}

export const dashboardApi = {
  summary: () => http.get<DashboardSummary>('/dashboard-summary').then((r) => r.data),
}

export const processesApi = {
  list: (params: { situation?: Situation; clientId?: string; number?: string; page?: number; size?: number } = {}) =>
    http.get<Page<Process>>('/processes', { params: clean(params) }).then((r) => r.data),
  get: (id: string) => http.get<Process>(`/processes/${id}`).then((r) => r.data),
  create: (input: { clientId: string; number: string; subject?: string }) =>
    http.post<Process>('/processes', input).then((r) => r.data),
  update: (id: string, input: { subject?: string; status: ProcessStatus }) =>
    http.put<Process>(`/processes/${id}`, input).then((r) => r.data),
  archive: (id: string) => http.delete(`/processes/${id}`),
  movements: (id: string) => http.get<Page<Movement>>(`/processes/${id}/movements`).then((r) => r.data),
}

export const publicationsApi = {
  list: (params: { processId?: string; unread?: boolean; page?: number; size?: number } = {}) =>
    http.get<Page<Publication>>('/publications', { params: clean(params) }).then((r) => r.data),
  markRead: (id: string) => http.patch<Publication>(`/publications/${id}`, { read: true }).then((r) => r.data),
}

export const clientsApi = {
  list: (params: { q?: string; status?: string; page?: number; size?: number } = {}) =>
    http.get<Page<Client>>('/clients', { params: clean(params) }).then((r) => r.data),
  create: (input: ClientInput) => http.post<Client>('/clients', input).then((r) => r.data),
  update: (id: string, input: ClientInput) => http.put<Client>(`/clients/${id}`, input).then((r) => r.data),
  deactivate: (id: string) => http.delete(`/clients/${id}`),
}

export const notificationsApi = {
  list: (params: { processId?: string; channel?: Channel; status?: string; page?: number; size?: number } = {}) =>
    http.get<Page<Notification>>('/notifications', { params: clean(params) }).then((r) => r.data),
  send: (input: { processId: string; channel: Channel; message: string }) =>
    http.post<Notification>('/notifications', input).then((r) => r.data),
}

export const monitoringApi = {
  list: (page = 0) =>
    http.get<Page<MonitoringExecution>>('/monitoring-executions', { params: { page } }).then((r) => r.data),
  get: (id: string) => http.get<MonitoringExecution>(`/monitoring-executions/${id}`).then((r) => r.data),
  start: () => http.post<MonitoringExecution>('/monitoring-executions').then((r) => r.data),
}

export const tribunalsApi = {
  list: () => http.get<Tribunal[]>('/tribunals', { params: { active: true } }).then((r) => r.data),
}
