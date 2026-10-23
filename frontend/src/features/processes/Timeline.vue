<script setup lang="ts">
import { formatDate } from '@/shared/lib/format'
import type { Movement } from '@/shared/api/types'

defineProps<{ movements: Movement[]; unreadPublicationIds?: string[] }>()
</script>

<template>
  <ol class="flex flex-col">
    <li v-for="m in movements" :key="m.id" class="flex gap-2 border-t border-border py-2">
      <span
        :class="[
          'h-5 w-11 flex-none rounded-sm border border-primary text-center text-[12px] leading-[18px] font-semibold',
          m.source === 'DJEN' ? 'bg-primary text-primary-foreground' : 'bg-card text-primary',
        ]"
      >
        {{ m.source }}
      </span>
      <div class="flex min-w-0 flex-col gap-0.5">
        <span class="text-caption">
          {{ formatDate(m.movementDate) }}
          <template v-if="m.publicationId && unreadPublicationIds?.includes(m.publicationId)">
            · <strong class="text-novelty-text">NOVA</strong>
          </template>
        </span>
        <span>{{ m.description }}</span>
      </div>
    </li>
    <li v-if="movements.length === 0" class="border-t border-border py-2 text-muted-foreground">
      Nenhuma movimentação registrada ainda.
    </li>
  </ol>
</template>
