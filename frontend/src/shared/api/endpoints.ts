import { http } from './http'
import type {
  AuditLog,
  Channel,
  Client,
  ClientDataExport,
  ClientInput,
  CreateDeadlineInput,
  CreateUserInput,
  DashboardSummary,
  Deadline,
  DeadlineStatus,
  DeadlineSummary,
  DiscoveryResult,
  Lawyer,
  LawyerInput,
  MonitoringExecution,
  Movement,
  Notification,
  Page,
  PracticeArea,
  Process,
  ProcessStatus,
  Publication,
  Situation,
  SynchronizationResult,
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
  refresh: () =>
    http.post<TokenPair>('/tokens', { grantType: 'REFRESH_TOKEN' }).then((r) => r.data),
  revoke: () => http.post('/token-revocations', {}),
  me: () => http.get<User>('/users/me').then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    http.put('/users/me/password', { currentPassword, newPassword }),
}

export const dashboardApi = {
  summary: () => http.get<DashboardSummary>('/dashboard-summary').then((r) => r.data),
}

export const processesApi = {
  list: (params: { situation?: Situation; clientId?: string; lawyerId?: string; number?: string; page?: number; size?: number } = {}) =>
    http.get<Page<Process>>('/processes', { params: clean(params) }).then((r) => r.data),
  get: (id: string) => http.get<Process>(`/processes/${id}`).then((r) => r.data),
  create: (input: { clientId: string; lawyerId?: string; practiceArea?: string; number: string; subject?: string; confidential?: boolean }) =>
    http.post<Process>('/processes', input).then((r) => r.data),
  update: (id: string, input: { subject?: string; status: ProcessStatus; confidential?: boolean; lawyerId?: string; practiceArea?: string }) =>
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
  get: (id: string) => http.get<Client>(`/clients/${id}`).then((r) => r.data),
  create: (input: ClientInput) => http.post<Client>('/clients', input).then((r) => r.data),
  update: (id: string, input: ClientInput) => http.put<Client>(`/clients/${id}`, input).then((r) => r.data),
  deactivate: (id: string) => http.delete(`/clients/${id}`),
  anonymize: (id: string) => http.post<Client>(`/clients/${id}/anonymizations`).then((r) => r.data),
  discoverProcesses: (id: string) =>
    http.post<DiscoveryResult>(`/clients/${id}/process-discoveries`).then((r) => r.data),
}

export const lawyersApi = {
  list: (params: { q?: string; practiceArea?: PracticeArea; active?: boolean; page?: number; size?: number } = {}) =>
    http.get<Page<Lawyer>>('/lawyers', { params: clean(params) }).then((r) => r.data),
  create: (input: LawyerInput) => http.post<Lawyer>('/lawyers', input).then((r) => r.data),
  update: (id: string, input: LawyerInput) => http.put<Lawyer>(`/lawyers/${id}`, input).then((r) => r.data),
  deactivate: (id: string) => http.delete(`/lawyers/${id}`),
  discoverProcesses: (id: string) =>
    http.post<DiscoveryResult>(`/lawyers/${id}/process-discoveries`).then((r) => r.data),
}

export const usersApi = {
  list: (page = 0, size = 20) =>
    http.get<Page<User>>('/users', { params: { page, size } }).then((r) => r.data),
  create: (input: CreateUserInput) => http.post<User>('/users', input).then((r) => r.data),
  setActive: (id: string, active: boolean) =>
    http.patch<User>(`/users/${id}`, { active }).then((r) => r.data),
}

export const auditApi = {
  list: (params: { resourceType?: string; resourceId?: string; page?: number; size?: number } = {}) =>
    http.get<Page<AuditLog>>('/audit-logs', { params: clean(params) }).then((r) => r.data),
}

export const lgpdApi = {
  exportClient: (id: string) => http.get<ClientDataExport>(`/clients/${id}/data-export`).then((r) => r.data),
  exportMine: () => http.get<ClientDataExport>('/users/me/data-export').then((r) => r.data),
  updateMyPreferences: (notifyEmail: boolean, notifyWhatsapp: boolean) =>
    http.patch<Client>('/clients/me/notification-preferences', { notifyEmail, notifyWhatsapp }).then((r) => r.data),
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
  list: (active?: boolean) =>
    http.get<Tribunal[]>('/tribunals', { params: clean({ active }) }).then((r) => r.data),
  setActive: (code: string, active: boolean) =>
    http.patch<Tribunal>(`/tribunals/${code}`, { active }).then((r) => r.data),
  synchronize: () => http.post<SynchronizationResult>('/tribunal-synchronizations').then((r) => r.data),
}

export const deadlinesApi = {
  list: (params: { lawyerId?: string; processId?: string; status?: DeadlineStatus; page?: number; size?: number } = {}) =>
    http.get<Page<Deadline>>('/deadlines', { params: clean(params) }).then((r) => r.data),
  summary: () => http.get<DeadlineSummary>('/deadlines/summary').then((r) => r.data),
  create: (input: CreateDeadlineInput) => http.post<Deadline>('/deadlines', input).then((r) => r.data),
  updateStatus: (id: string, status: DeadlineStatus) =>
    http.patch<Deadline>(`/deadlines/${id}`, { status }).then((r) => r.data),
  triggerAlerts: () => http.post<{ alertsSent: number }>('/deadlines/alerts').then((r) => r.data),
}
