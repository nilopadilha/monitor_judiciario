<script setup lang="ts">
import { computed } from 'vue'
import type { Situation } from '@/shared/api/types'

/** Status do domínio conforme a tabela do design system: sempre palavra, nunca só cor. */
const props = defineProps<{ situation: Situation | 'SENT' | 'FAILED_DELIVERY' | 'PENDING' }>()

const map: Record<string, { label: string; class: string }> = {
  NEW_PUBLICATION: { label: 'Nova publicação', class: 'bg-novelty text-foreground border-novelty' },
  MONITORED: { label: 'Monitorado', class: 'bg-card text-ok border-ok' },
  FAILED: { label: 'Falhou', class: 'bg-card text-destructive border-destructive' },
  PAUSED: { label: 'Pausado', class: 'bg-accent text-muted-foreground border-accent' },
  ARCHIVED: { label: 'Arquivado', class: 'bg-accent text-muted-foreground border-accent' },
  SENT: { label: 'Entregue', class: 'bg-card text-ok border-ok' },
  FAILED_DELIVERY: { label: 'Falhou', class: 'bg-card text-destructive border-destructive' },
  PENDING: { label: 'Pendente', class: 'bg-accent text-muted-foreground border-accent' },
}

const config = computed(() => map[props.situation] ?? { label: props.situation, class: 'bg-accent' })
</script>

<template>
  <span :class="['inline-block whitespace-nowrap rounded-sm border px-1.5 py-0.5 text-label', config.class]">
    {{ config.label }}
  </span>
</template>
