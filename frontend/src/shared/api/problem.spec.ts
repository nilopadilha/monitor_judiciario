import { describe, expect, it } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { problemCode, toProblem } from './problem'

const config = { headers: new AxiosHeaders() }

describe('toProblem', () => {
  it('uses RFC 9457 body when present', () => {
    const error = new AxiosError('x', 'ERR', config, null, {
      status: 422,
      statusText: '',
      headers: {},
      config,
      data: { type: 'https://monitorjudicial.app/problems/invalid-process-number', title: 'Regra', detail: 'DV errado' },
    })
    const problem = toProblem(error)
    expect(problem.detail).toBe('DV errado')
    expect(problem.status).toBe(422)
    expect(problemCode(problem)).toBe('invalid-process-number')
  })

  it('handles network errors, bare HTTP errors and plain errors', () => {
    expect(toProblem(new AxiosError('x', 'ERR_NETWORK', config)).title).toBe('Sem conexão')
    const bare = new AxiosError('x', 'ERR', config, null, { status: 502, statusText: '', headers: {}, config, data: '' })
    expect(toProblem(bare).detail).toBe('O servidor respondeu 502.')
    expect(toProblem(new Error('falhou')).detail).toBe('falhou')
    expect(toProblem('???').detail).toBe('Erro inesperado.')
  })
})
