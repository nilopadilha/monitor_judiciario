<script setup lang="ts">
import { cn } from '@/shared/lib/utils'

const props = withDefaults(defineProps<{ class?: string; as?: string }>(), { class: '', as: 'section' })
</script>

<template>
  <component :is="props.as" :class="cn('rounded-lg border border-border bg-card p-4', props.class)">
    <slot />
  </component>
</template>
