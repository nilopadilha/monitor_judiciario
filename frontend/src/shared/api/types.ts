/** Contratos da API (espelham os records do backend). */

export type Role = 'ADMIN' | 'MANAGER' | 'CUSTOMER'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  tokenType: 'Bearer'
}

export interface User {
  id: string
  name: string
  email: string
  role: Role
  clientId?: string
  active: boolean
}

export type ClientStatus = 'ACTIVE' | 'INACTIVE'

export interface Client {
  id: string
  name: string
  document: string
  email: string
  phone?: string
  status: ClientStatus
  notifyEmail: boolean
  notifyWhatsapp: boolean
  createdAt: string
}

export interface ClientInput {
  name: string
  document: string
  email: string
  phone?: string
  notifyEmail: boolean
  notifyWhatsapp: boolean
}

export type ProcessStatus = 'ACTIVE' | 'PAUSED' | 'ARCHIVED'
export type Situation = 'ARCHIVED' | 'PAUSED' | 'FAILED' | 'NEW_PUBLICATION' | 'MONITORED'

export interface Process {
  id: string
  clientId: string
  clientName?: string
  number: string
  courtCode: string
  subject?: string
  status: ProcessStatus
  situation: Situation
  unreadPublications: number
  lastError?: string
  lastCheckedAt?: string
  createdAt: string
}

export interface Publication {
  id: string
  processId: string
  processNumber?: string
  courtCode?: string
  publicationDate: string
  communicationType?: string
  documentType?: string
  organName?: string
  content: string
  link?: string
  source: 'DJEN' | 'PJE'
  read: boolean
  readAt?: string
}

export interface Movement {
  id: string
  publicationId?: string
  type: string
  typeLabel: string
  description: string
  movementDate: string
  source: 'DJEN' | 'PJE'
}

export type Channel = 'EMAIL' | 'WHATSAPP'
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED'

export interface Notification {
  id: string
  processId: string
  processNumber?: string
  clientName?: string
  channel: Channel
  recipient: string
  subject?: string
  status: NotificationStatus
  attempts: number
  errorMessage?: string
  createdAt: string
  sentAt?: string
}

export type ExecutionStatus = 'RUNNING' | 'COMPLETED' | 'COMPLETED_WITH_FAILURES' | 'FAILED' | 'ABORTED'

export interface MonitoringExecution {
  id: string
  trigger: 'SCHEDULED' | 'MANUAL'
  triggeredBy: string
  status: ExecutionStatus
  startedAt: string
  finishedAt?: string
  processesChecked: number
  newPublications: number
  failedJobs: number
  errorMessage?: string
  jobs: {
    processId: string
    processNumber?: string
    status: 'SUCCESS' | 'FAILED'
    executionTimeMs: number
    newPublications: number
    errorMessage?: string
  }[]
}

export interface DashboardSummary {
  monitoredProcesses: number
  pausedProcesses: number
  processesWithNews: number
  processesWithFailedCheck: number
  unreadPublications: number
  notificationsToday: number
  failedNotificationsToday: number
  lastExecution?: {
    id: string
    status: ExecutionStatus
    startedAt: string
    finishedAt?: string
    newPublications: number
    failedJobs: number
  }
  publicationsPerDay: { date: string; count: number }[]
  publicationsByTribunal: { courtCode: string; count: number }[]
}

export interface Tribunal {
  code: string
  name: string
  state: string
  active: boolean
}

/** RFC 9457 Problem Details */
export interface Problem {
  type?: string
  title?: string
  status?: number
  detail?: string
  correlationId?: string
  errors?: Record<string, string>
}
