import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios'
import { configureAuth, http } from './http'

function respond(config: InternalAxiosRequestConfig, status: number, data: unknown = {}) {
  const response = { data, status, statusText: String(status), headers: {}, config }
  if (status >= 400) {
    return Promise.reject(Object.assign(new Error(`HTTP ${status}`), { isAxiosError: true, config, response }))
  }
  return Promise.resolve(response)
}

describe('http client', () => {
  let validToken = 'novo'
  const seen: InternalAxiosRequestConfig[] = []
  const adapter: AxiosAdapter = (config) => {
    seen.push(config)
    const auth = config.headers.Authorization
    return auth === `Bearer ${validToken}` ? respond(config, 200, { ok: true }) : respond(config, 401)
  }

  beforeEach(() => {
    seen.length = 0
    validToken = 'novo'
    http.defaults.adapter = adapter
  })

  afterEach(() => configureAuth(null))

  it('adds bearer token and a correlation id', async () => {
    configureAuth({ accessToken: () => 'novo', refresh: vi.fn(), onAuthFailure: vi.fn() })
    await http.get('/clients')
    expect(seen[0]?.headers.Authorization).toBe('Bearer novo')
    expect(seen[0]?.headers['X-Correlation-Id']).toMatch(/[0-9a-f-]{8,}/)
  })

  it('refreshes once for concurrent 401s and retries', async () => {
    let token = 'expirado'
    const refresh = vi.fn(async () => {
      token = 'novo'
      return 'novo'
    })
    configureAuth({ accessToken: () => token, refresh, onAuthFailure: vi.fn() })

    const results = await Promise.all([http.get('/a'), http.get('/b'), http.get('/c')])

    expect(results.map((r) => r.status)).toEqual([200, 200, 200])
    expect(refresh).toHaveBeenCalledTimes(1)
  })

  it('logs out when refresh fails and does not loop', async () => {
    const onAuthFailure = vi.fn()
    configureAuth({ accessToken: () => 'velho', refresh: vi.fn().mockRejectedValue(new Error('x')), onAuthFailure })
    await expect(http.get('/a')).rejects.toThrow('HTTP 401')
    expect(onAuthFailure).toHaveBeenCalledTimes(1)
  })

  it('never refreshes for the token endpoint itself', async () => {
    const refresh = vi.fn()
    configureAuth({ accessToken: () => null, refresh, onAuthFailure: vi.fn() })
    await expect(http.post('/tokens', {})).rejects.toThrow('HTTP 401')
    expect(refresh).not.toHaveBeenCalled()
  })
})
