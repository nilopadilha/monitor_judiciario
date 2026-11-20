import { defineStore } from 'pinia'
import { ref } from 'vue'
import { monitoringApi, publicationsApi } from '@/shared/api/endpoints'
import type { MonitoringExecution, Publication } from '@/shared/api/types'

/** Store Pinia para execuções de monitoramento e publicações do DJEN (RF-019, RF-024, RF-025). */
export const useMonitoringStore = defineStore('monitoring', () => {
  const executions = ref<MonitoringExecution[]>([])
  const selectedExecution = ref<MonitoringExecution | null>(null)
  const publications = ref<Publication[]>([])
  const loading = ref(false)

  async function fetchExecutions(page = 0) {
    loading.value = true
    try {
      const result = await monitoringApi.list(page)
      executions.value = result.content
      return result
    } finally {
      loading.value = false
    }
  }

  async function fetchExecution(id: string) {
    const execution = await monitoringApi.get(id)
    selectedExecution.value = execution
    return execution
  }

  async function startExecution() {
    const execution = await monitoringApi.start()
    selectedExecution.value = execution
    return execution
  }

  async function fetchPublications(params: { processId?: string; unread?: boolean; page?: number; size?: number } = {}) {
    const result = await publicationsApi.list(params)
    publications.value = result.content
    return result
  }

  async function markPublicationRead(id: string) {
    return publicationsApi.markRead(id)
  }

  return {
    executions,
    selectedExecution,
    publications,
    loading,
    fetchExecutions,
    fetchExecution,
    startExecution,
    fetchPublications,
    markPublicationRead,
  }
})
