import { isAxiosError } from 'axios'
import type { Problem } from './types'

/** Converte qualquer erro em Problem Details com mensagem legível para o usuário. */
export function toProblem(error: unknown): Problem {
  if (isAxiosError(error)) {
    const data = error.response?.data as Problem | undefined
    if (data && (data.detail || data.title)) {
      return { ...data, status: data.status ?? error.response?.status }
    }
    if (!error.response) {
      return { title: 'Sem conexão', detail: 'Não foi possível falar com o servidor. Verifique sua conexão.' }
    }
    return { status: error.response.status, title: 'Erro', detail: `O servidor respondeu ${error.response.status}.` }
  }
  return { title: 'Erro', detail: error instanceof Error ? error.message : 'Erro inesperado.' }
}

export function problemCode(problem: Problem): string | undefined {
  return problem.type?.split('/').pop()
}
