<script setup lang="ts">
import type { Problem } from '@/shared/api/types'

defineProps<{ problem: Problem | null }>()
</script>

<template>
  <div v-if="problem" role="alert" class="rounded-md border border-destructive bg-card p-3 text-[13px] leading-[18px]">
    <p class="font-semibold text-destructive">{{ problem.title ?? 'Erro' }}</p>
    <p v-if="problem.detail" class="text-foreground">{{ problem.detail }}</p>
    <ul v-if="problem.errors" class="mt-1 list-disc pl-5 text-foreground">
      <li v-for="(message, field) in problem.errors" :key="field">{{ field }}: {{ message }}</li>
    </ul>
    <p v-if="problem.correlationId" class="mt-1 text-caption">Código para o suporte: {{ problem.correlationId }}</p>
  </div>
</template>
