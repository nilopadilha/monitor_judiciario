<script setup lang="ts">
import { cn } from '@/shared/lib/utils'

defineOptions({ inheritAttrs: false })
const model = defineModel<string>({ default: '' })
const props = withDefaults(defineProps<{ class?: string; invalid?: boolean }>(), { class: '', invalid: false })
</script>

<template>
  <input
    v-model="model"
    v-bind="$attrs"
    :aria-invalid="props.invalid || undefined"
    :class="
      cn(
        'h-11 w-full rounded-md border border-input bg-card px-3 text-[15px] placeholder:text-muted-foreground',
        props.invalid && 'border-destructive',
        props.class,
      )
    "
  />
</template>
