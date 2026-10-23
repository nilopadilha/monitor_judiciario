<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardSummary } from '@/shared/api/types'

const props = defineProps<{ summary: DashboardSummary | null }>()

const kpis = computed(() => {
  const s = props.summary
  if (!s) return []
  return [
    { label: 'Processos monitorados', value: s.monitoredProcesses, note: `${s.pausedProcesses} pausado(s)`, tone: 'text-foreground' },
    { label: 'Com novidade', value: s.processesWithNews, note: `${s.unreadPublications} publicação(ões) não lida(s)`, tone: s.processesWithNews ? 'text-novelty-text' : 'text-foreground' },
    { label: 'Notificações hoje', value: s.notificationsToday, note: `${s.failedNotificationsToday} não entregue(s)`, tone: 'text-foreground' },
    { label: 'Falhas de consulta', value: s.processesWithFailedCheck, note: s.processesWithFailedCheck ? 'DJEN sem resposta na última verificação' : 'nenhuma na última verificação', tone: s.processesWithFailedCheck ? 'text-destructive' : 'text-ok' },
  ]
})
</script>

<template>
  <section aria-label="Resumo" class="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-4">
    <div v-for="kpi in kpis" :key="kpi.label" class="flex flex-col gap-1 rounded-lg border border-border bg-card p-4">
      <span class="text-label text-muted-foreground">{{ kpi.label }}</span>
      <span :class="['text-display', kpi.tone]">{{ kpi.value }}</span>
      <span class="text-caption">{{ kpi.note }}</span>
    </div>
  </section>
</template>
