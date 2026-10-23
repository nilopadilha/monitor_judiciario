<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Card from '@/shared/ui/Card.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { monitoringApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { formatDateTime } from '@/shared/lib/format'
import type { ExecutionStatus, MonitoringExecution, Problem } from '@/shared/api/types'

const executions = ref<MonitoringExecution[]>([])
const selected = ref<MonitoringExecution | null>(null)
const problem = ref<Problem | null>(null)

const statusLabel: Record<ExecutionStatus, string> = {
  RUNNING: 'Em andamento',
  COMPLETED: 'Concluída',
  COMPLETED_WITH_FAILURES: 'Concluída com falhas',
  FAILED: 'Falhou',
  ABORTED: 'Interrompida',
}

async function open(id: string) {
  try {
    selected.value = await monitoringApi.get(id)
  } catch (error) {
    problem.value = toProblem(error)
  }
}

onMounted(async () => {
  try {
    executions.value = (await monitoringApi.list()).content
    if (executions.value[0]) await open(executions.value[0].id)
  } catch (error) {
    problem.value = toProblem(error)
  }
})
</script>

<template>
  <div class="flex flex-col gap-8">
    <div class="flex flex-col gap-1">
      <h1 class="text-title">Monitoramento</h1>
      <p class="text-caption">Execução diária às 06:00 (horário de Brasília) e execuções manuais.</p>
    </div>
    <ProblemAlert :problem="problem" />
    <div class="flex flex-wrap items-start gap-4">
      <Card class="min-w-0 flex-[1_1_320px]">
        <h2 class="text-heading mb-2">Execuções</h2>
        <ul>
          <li v-for="e in executions" :key="e.id" class="border-t border-border">
            <button
              type="button"
              :class="['flex min-h-11 w-full flex-col items-start py-2 text-left', selected?.id === e.id && 'font-semibold']"
              @click="open(e.id)"
            >
              <span>{{ formatDateTime(e.startedAt) }} · {{ e.trigger === 'MANUAL' ? 'Manual' : 'Agendada' }}</span>
              <span class="text-caption">{{ statusLabel[e.status] }} · {{ e.newPublications }} novidade(s) · {{ e.failedJobs }} falha(s)</span>
            </button>
          </li>
          <li v-if="executions.length === 0" class="border-t border-border py-2 text-muted-foreground">Nenhuma execução ainda.</li>
        </ul>
      </Card>
      <Card v-if="selected" class="min-w-0 flex-[2_1_480px]">
        <h2 class="text-heading">{{ statusLabel[selected.status] }}</h2>
        <p class="text-caption mb-2">
          Iniciada por {{ selected.triggeredBy }} em {{ formatDateTime(selected.startedAt) }} · {{ selected.processesChecked }} processo(s)
        </p>
        <div class="overflow-x-auto">
          <table class="w-full min-w-[560px] border-collapse">
            <thead>
              <tr class="text-left text-label text-muted-foreground">
                <th scope="col" class="border-b border-border p-2">Processo</th>
                <th scope="col" class="border-b border-border p-2">Resultado</th>
                <th scope="col" class="border-b border-border p-2">Novidades</th>
                <th scope="col" class="border-b border-border p-2">Tempo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="j in selected.jobs" :key="j.processId">
                <td class="border-b border-border p-2 font-mono text-[14px]">{{ j.processNumber }}</td>
                <td class="border-b border-border p-2">
                  <span :class="j.status === 'SUCCESS' ? 'text-ok' : 'text-destructive'">{{ j.status === 'SUCCESS' ? 'Sucesso' : 'Falhou' }}</span>
                  <span v-if="j.errorMessage" class="block text-[13px] text-destructive">{{ j.errorMessage }}</span>
                </td>
                <td class="border-b border-border p-2">{{ j.newPublications }}</td>
                <td class="border-b border-border p-2 text-caption">{{ j.executionTimeMs }} ms</td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  </div>
</template>
