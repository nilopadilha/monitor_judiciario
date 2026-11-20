import { defineStore } from 'pinia'
import { ref } from 'vue'
import { clientsApi } from '@/shared/api/endpoints'
import type { Client, ClientInput, ClientStatus, Page } from '@/shared/api/types'

/** Store Pinia para gestão de clientes (RF-008 a RF-011). */
export const useClientsStore = defineStore('clients', () => {
  const page = ref<Page<Client> | null>(null)
  const loading = ref(false)

  async function fetchClients(params: { q?: string; status?: ClientStatus | ''; page?: number; size?: number } = {}) {
    loading.value = true
    try {
      const result = await clientsApi.list({
        q: params.q,
        status: params.status || undefined,
        page: params.page,
        size: params.size,
      })
      page.value = result
      return result
    } finally {
      loading.value = false
    }
  }

  async function createClient(input: ClientInput) {
    return clientsApi.create(input)
  }

  async function updateClient(id: string, input: ClientInput) {
    return clientsApi.update(id, input)
  }

  async function deactivateClient(id: string) {
    await clientsApi.deactivate(id)
  }

  return { page, loading, fetchClients, createClient, updateClient, deactivateClient }
})
