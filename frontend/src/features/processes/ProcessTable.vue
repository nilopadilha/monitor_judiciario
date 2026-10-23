<script setup lang="ts">
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { formatRelative } from '@/shared/lib/format'
import type { Process } from '@/shared/api/types'

defineProps<{ processes: Process[]; selectedId?: string | null; loading?: boolean }>()
const emit = defineEmits<{ select: [process: Process] }>()
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full min-w-[640px] border-collapse">
      <thead>
        <tr class="text-left text-label text-muted-foreground">
          <th scope="col" class="border-b border-border p-2">Número CNJ</th>
          <th scope="col" class="border-b border-border p-2">Cliente</th>
          <th scope="col" class="border-b border-border p-2">Tribunal</th>
          <th scope="col" class="border-b border-border p-2">Status</th>
          <th scope="col" class="border-b border-border p-2">Verificado</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in processes" :key="p.id" :class="p.id === selectedId ? 'bg-accent' : 'bg-card'">
          <td class="border-b border-border p-2">
            <button
              type="button"
              class="text-process-number min-h-11 cursor-pointer whitespace-nowrap text-primary underline underline-offset-[3px]"
              :aria-label="`Abrir processo ${p.number}`"
              @click="emit('select', p)"
            >
              {{ p.number }}
            </button>
          </td>
          <td class="border-b border-border p-2">{{ p.clientName }}</td>
          <td class="border-b border-border p-2">{{ p.courtCode }}</td>
          <td class="border-b border-border p-2"><StatusBadge :situation="p.situation" /></td>
          <td class="border-b border-border p-2 text-caption">{{ formatRelative(p.lastCheckedAt) }}</td>
        </tr>
      </tbody>
    </table>
    <p v-if="!loading && processes.length === 0" class="px-2 py-4 text-muted-foreground">Nenhum processo neste filtro.</p>
    <p v-if="loading" class="px-2 py-4 text-muted-foreground" role="status">Carregando processos…</p>
  </div>
</template>
