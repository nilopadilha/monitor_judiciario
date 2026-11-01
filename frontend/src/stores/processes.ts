import { defineStore } from 'pinia'
import { ref } from 'vue'
import { processesApi } from '@/shared/api/endpoints'
import type { Movement, Page, Process, ProcessStatus, Situation } from '@/shared/api/types'

/** Store Pinia para gestão e consulta de processos judiciais (RF-015 a RF-018). */
export const useProcessesStore = defineStore('processes', () => {
  const page = ref<Page<Process> | null>(null)
  const currentProcess = ref<Process | null>(null)
  const movements = ref<Movement[]>([])
  const loading = ref(false)

  async function fetchProcesses(params: { situation?: Situation; clientId?: string; number?: string; page?: number; size?: number } = {}) {
    loading.value = true
    try {
      const result = await processesApi.list(params)
      page.value = result
      return result
    } finally {
      loading.value = false
    }
  }

  async function fetchProcess(id: string) {
    loading.value = true
    try {
      const [proc, movs] = await Promise.all([processesApi.get(id), processesApi.movements(id)])
      currentProcess.value = proc
      movements.value = movs.content
      return proc
    } finally {
      loading.value = false
    }
  }

  async function createProcess(input: { clientId: string; number: string; subject?: string }) {
    return processesApi.create(input)
  }

  async function updateProcess(id: string, input: { subject?: string; status: ProcessStatus }) {
    const updated = await processesApi.update(id, input)
    currentProcess.value = updated
    return updated
  }

  async function archiveProcess(id: string) {
    await processesApi.archive(id)
  }

  return { page, currentProcess, movements, loading, fetchProcesses, fetchProcess, createProcess, updateProcess, archiveProcess }
})
