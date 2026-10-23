<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/shared/lib/utils'
import { buttonVariants, type ButtonVariants } from './button'

const props = withDefaults(
  defineProps<{
    variant?: ButtonVariants['variant']
    size?: ButtonVariants['size']
    type?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
    class?: string
  }>(),
  { type: 'button', variant: 'default', size: 'default', loading: false, disabled: false, class: '' },
)

const classes = computed(() => cn(buttonVariants({ variant: props.variant, size: props.size }), props.class))
</script>

<template>
  <button :type="type" :class="classes" :disabled="disabled || loading" :aria-busy="loading || undefined">
    <svg v-if="loading" class="animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-opacity="0.3" stroke-width="2" />
      <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" stroke-width="2" />
    </svg>
    <slot />
  </button>
</template>
