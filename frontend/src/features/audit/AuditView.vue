<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Card from '@/shared/ui/Card.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { auditApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { formatDateTime } from '@/shared/lib/format'
import type { AuditLog, Page, Problem } from '@/shared/api/types'

const page = ref<Page<AuditLog> | null>(null)
const resourceType = ref('')
const current = ref(0)
const problem = ref<Problem | null>(null)

async function load() {
  problem.value = null
  try {
    page.value = await auditApi.list({
      resourceType: resourceType.value || undefined,
      page: current.value,
      size: 30,
    })
  } catch (error) {
    problem.value = toProblem(error)
  }
}

watch(resourceType, () => {
  current.value = 0
  load()
})
watch(current, load)
onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-title">Auditoria</h1>
        <p class="text-caption">Trilha imutável de acessos, alterações e operações LGPD.</p>
      </div>
      <div>
        <label class="sr-only" for="audit-resource">Recurso</label>
        <select id="audit-resource" v-model="resourceType" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="">Todos os recursos</option>
          <option value="client">Clientes</option>
          <option value="process">Processos</option>
          <option value="user">Usuários</option>
          <option value="notification">Notificações</option>
          <option value="monitoring-execution">Monitoramento</option>
          <option value="tribunal">Tribunais</option>
        </select>
      </div>
    </header>

    <Card class="flex flex-col gap-4">
      <ProblemAlert :problem="problem" />
      <div class="overflow-x-auto">
        <table class="w-full min-w-[760px] border-collapse">
          <thead>
            <tr class="text-left text-label text-muted-foreground">
              <th scope="col" class="border-b border-border p-2">Data/Hora</th>
              <th scope="col" class="border-b border-border p-2">Ator</th>
              <th scope="col" class="border-b border-border p-2">Ação</th>
              <th scope="col" class="border-b border-border p-2">Recurso</th>
              <th scope="col" class="border-b border-border p-2">Detalhes</th>
              <th scope="col" class="border-b border-border p-2">Correlation ID</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in page?.content ?? []" :key="log.id">
              <td class="border-b border-border p-2 text-caption whitespace-nowrap">{{ formatDateTime(log.occurredAt) }}</td>
              <td class="border-b border-border p-2 text-caption">{{ log.actorEmail ?? 'sistema' }}</td>
              <td class="border-b border-border p-2 font-mono text-[13px]">{{ log.action }}</td>
              <td class="border-b border-border p-2 text-caption">{{ log.resourceType }} · {{ log.resourceId.slice(0, 8) }}</td>
              <td class="border-b border-border p-2 font-mono text-[12px] text-muted-foreground">
                {{ log.details && Object.keys(log.details).length ? JSON.stringify(log.details) : '—' }}
              </td>
              <td class="border-b border-border p-2 font-mono text-[12px] text-muted-foreground">
                {{ log.correlationId ? log.correlationId.slice(0, 8) : '—' }}
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="page && page.content.length === 0" class="px-2 py-4 text-muted-foreground">Nenhum evento de auditoria.</p>
      </div>
      <Pagination
        v-if="page"
        :page="page.page"
        :total-pages="page.totalPages"
        :total-elements="page.totalElements"
        @change="current = $event"
      />
    </Card>
  </div>
</template>
