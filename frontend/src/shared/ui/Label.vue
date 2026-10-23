<script setup lang="ts">
defineProps<{ for?: string }>()
</script>

<template>
  <label :for="$props.for" class="text-[13px] leading-[18px] font-semibold text-foreground"><slot /></label>
</template>
