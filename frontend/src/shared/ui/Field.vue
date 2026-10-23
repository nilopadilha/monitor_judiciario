<script setup lang="ts">
import Label from './Label.vue'

defineProps<{ id: string; label: string; error?: string; hint?: string }>()
</script>

<template>
  <div class="flex flex-col gap-1">
    <Label :for="id">{{ label }}</Label>
    <slot />
    <p v-if="error" :id="`${id}-error`" class="text-[13px] leading-[18px] text-destructive">{{ error }}</p>
    <p v-else-if="hint" class="text-caption">{{ hint }}</p>
  </div>
</template>
