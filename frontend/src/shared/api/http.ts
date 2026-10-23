import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

/**
 * Cliente HTTP da API. O módulo de autenticação registra os ganchos via {@link configureAuth}, evitando dependência
 * circular entre o cliente e a store.
 */
export const http = axios.create({ baseURL: '/api/v1', timeout: 30_000 })

interface AuthHooks {
  accessToken: () => string | null
  refresh: () => Promise<string>
  onAuthFailure: () => void
}

let hooks: AuthHooks | null = null
let refreshing: Promise<string> | null = null

export function configureAuth(next: AuthHooks | null) {
  hooks = next
  refreshing = null
}

function newCorrelationId(): string {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 10)}`
}

http.interceptors.request.use((config) => {
  const token = hooks?.accessToken()
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Correlation-Id'] = newCorrelationId()
  return config
})

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

/** 401 → renova o access token uma única vez (requisições concorrentes aguardam a mesma renovação) e repete. */
http.interceptors.response.use(undefined, async (error: AxiosError) => {
  const config = error.config as RetriableConfig | undefined
  const isTokenEndpoint = config?.url?.startsWith('/tokens') || config?.url?.startsWith('/token-revocations')
  if (error.response?.status !== 401 || !config || config._retried || isTokenEndpoint || !hooks) {
    throw error
  }
  config._retried = true
  try {
    refreshing ??= hooks.refresh().finally(() => {
      refreshing = null
    })
    const token = await refreshing
    config.headers.Authorization = `Bearer ${token}`
    return http(config)
  } catch {
    hooks.onAuthFailure()
    throw error
  }
})
