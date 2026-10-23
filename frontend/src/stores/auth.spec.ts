import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/shared/api/endpoints', () => ({
  authApi: {
    login: vi.fn(),
    refresh: vi.fn(),
    revoke: vi.fn(),
    me: vi.fn(),
  },
}))

import { authApi } from '@/shared/api/endpoints'
import { useAuthStore } from './auth'

const pair = (n: number) => ({ accessToken: `a${n}`, refreshToken: `r${n}`, expiresInSeconds: 900, tokenType: 'Bearer' as const })
const admin = { id: '1', name: 'Admin', email: 'a@x.com', role: 'ADMIN' as const, active: true }

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.resetAllMocks()
    setActivePinia(createPinia())
  })

  it('logs in keeping access token in memory and refresh token in storage', async () => {
    vi.mocked(authApi.login).mockResolvedValue(pair(1))
    vi.mocked(authApi.me).mockResolvedValue(admin)
    const auth = useAuthStore()

    await auth.login('a@x.com', 'senha')

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.isStaff).toBe(true)
    expect(auth.accessToken).toBe('a1')
    expect(localStorage.getItem('mj.refresh')).toBe('r1')
  })

  it('restores the session once from the refresh token', async () => {
    localStorage.setItem('mj.refresh', 'r0')
    vi.mocked(authApi.refresh).mockResolvedValue(pair(2))
    vi.mocked(authApi.me).mockResolvedValue({ ...admin, role: 'CUSTOMER', clientId: 'c' })
    const auth = useAuthStore()

    await Promise.all([auth.init(), auth.init()])

    expect(authApi.refresh).toHaveBeenCalledTimes(1)
    expect(authApi.refresh).toHaveBeenCalledWith('r0')
    expect(auth.isStaff).toBe(false)
    expect(localStorage.getItem('mj.refresh')).toBe('r2')
  })

  it('clears an invalid stored session', async () => {
    localStorage.setItem('mj.refresh', 'revogado')
    vi.mocked(authApi.refresh).mockRejectedValue(new Error('401'))
    const auth = useAuthStore()
    await auth.init()
    expect(auth.isAuthenticated).toBe(false)
    expect(localStorage.getItem('mj.refresh')).toBeNull()
  })

  it('logs out revoking the refresh token', async () => {
    vi.mocked(authApi.login).mockResolvedValue(pair(3))
    vi.mocked(authApi.me).mockResolvedValue(admin)
    vi.mocked(authApi.revoke).mockRejectedValue(new Error('offline'))
    const auth = useAuthStore()
    await auth.login('a@x.com', 'senha')

    await auth.logout()

    expect(authApi.revoke).toHaveBeenCalledWith('r3')
    expect(auth.isAuthenticated).toBe(false)
    expect(localStorage.getItem('mj.refresh')).toBeNull()
  })
})
