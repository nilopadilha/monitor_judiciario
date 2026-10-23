<script setup lang="ts">
import Button from './Button.vue'

const props = defineProps<{ page: number; totalPages: number; totalElements: number }>()
const emit = defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="props.totalPages > 1" aria-label="Paginação" class="flex flex-wrap items-center justify-between gap-2 pt-2">
    <span class="text-caption">Página {{ props.page + 1 }} de {{ props.totalPages }} · {{ props.totalElements }} itens</span>
    <div class="flex gap-2">
      <Button variant="outline" size="sm" :disabled="props.page === 0" @click="emit('change', props.page - 1)">Anterior</Button>
      <Button variant="outline" size="sm" :disabled="props.page + 1 >= props.totalPages" @click="emit('change', props.page + 1)">Próxima</Button>
    </div>
  </nav>
</template>
