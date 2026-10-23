<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { watchDebounced } from '@vueuse/core'
import { Plus } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Input from '@/shared/ui/Input.vue'
import Modal from '@/shared/ui/Modal.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import ClientForm from './ClientForm.vue'
import { clientsApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import type { Client, Page, Problem } from '@/shared/api/types'

const page = ref<Page<Client> | null>(null)
const query = ref('')
const status = ref<'ACTIVE' | 'INACTIVE' | ''>('ACTIVE')
const current = ref(0)
const editing = ref<Client | null>(null)
const open = ref(false)
const problem = ref<Problem | null>(null)

async function load() {
  problem.value = null
  try {
    page.value = await clientsApi.list({ q: query.value, status: status.value || undefined, page: current.value })
  } catch (error) {
    problem.value = toProblem(error)
  }
}

function edit(client: Client | null) {
  editing.value = client
  open.value = true
}

async function onSaved() {
  open.value = false
  await load()
}

async function deactivate(client: Client) {
  if (!window.confirm(`Inativar ${client.name}? Os processos e o histórico são mantidos.`)) return
  try {
    await clientsApi.deactivate(client.id)
    await load()
  } catch (error) {
    problem.value = toProblem(error)
  }
}

watchDebounced(query, () => { current.value = 0; load() }, { debounce: 300 })
watch(status, () => { current.value = 0; load() })
watch(current, load)
onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <h1 class="text-title">Clientes</h1>
      <Button @click="edit(null)"><Plus aria-hidden="true" /> Cadastrar cliente</Button>
    </header>
    <Card class="flex flex-col gap-4">
      <div class="flex flex-wrap gap-2">
        <label class="sr-only" for="q">Buscar cliente</label>
        <Input id="q" v-model="query" class="max-w-[320px]" placeholder="Buscar por nome ou documento" />
        <label class="sr-only" for="status">Status</label>
        <select id="status" v-model="status" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="ACTIVE">Ativos</option>
          <option value="INACTIVE">Inativos</option>
          <option value="">Todos</option>
        </select>
      </div>
      <ProblemAlert :problem="problem" />
      <div class="overflow-x-auto">
        <table class="w-full min-w-[720px] border-collapse">
          <thead>
            <tr class="text-left text-label text-muted-foreground">
              <th scope="col" class="border-b border-border p-2">Nome</th>
              <th scope="col" class="border-b border-border p-2">Documento</th>
              <th scope="col" class="border-b border-border p-2">Contato</th>
              <th scope="col" class="border-b border-border p-2">Canais</th>
              <th scope="col" class="border-b border-border p-2"><span class="sr-only">Ações</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in page?.content ?? []" :key="c.id">
              <td class="border-b border-border p-2 font-medium">{{ c.name }}</td>
              <td class="border-b border-border p-2 font-mono text-[14px]">{{ c.document }}</td>
              <td class="border-b border-border p-2 text-caption">{{ c.email }}<br />{{ c.phone }}</td>
              <td class="border-b border-border p-2">
                {{ [c.notifyEmail && 'E-mail', c.notifyWhatsapp && 'WhatsApp'].filter(Boolean).join(' · ') }}
              </td>
              <td class="border-b border-border p-2 text-right">
                <Button variant="ghost" size="sm" @click="edit(c)">Editar</Button>
                <Button v-if="c.status === 'ACTIVE'" variant="ghost" size="sm" class="text-destructive" @click="deactivate(c)">Inativar</Button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="page && page.content.length === 0" class="px-2 py-4 text-muted-foreground">Nenhum cliente encontrado.</p>
      </div>
      <Pagination v-if="page" :page="page.page" :total-pages="page.totalPages" :total-elements="page.totalElements" @change="current = $event" />
    </Card>
    <Modal v-model:open="open" :title="editing ? 'Editar cliente' : 'Cadastrar cliente'">
      <ClientForm :key="editing?.id ?? 'new'" :client="editing" @saved="onSaved" />
    </Modal>
  </div>
</template>
