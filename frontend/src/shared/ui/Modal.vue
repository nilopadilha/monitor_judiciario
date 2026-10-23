<script setup lang="ts">
import { DialogClose, DialogContent, DialogDescription, DialogOverlay, DialogPortal, DialogRoot, DialogTitle } from 'reka-ui'
import { X } from 'lucide-vue-next'

const open = defineModel<boolean>('open', { default: false })
defineProps<{ title: string; description?: string }>()
</script>

<template>
  <DialogRoot v-model:open="open">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-40 bg-foreground/40" />
      <DialogContent
        class="fixed left-1/2 top-1/2 z-50 max-h-[90vh] w-[min(560px,calc(100vw-32px))] -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-lg border border-border bg-card p-6"
      >
        <div class="mb-4 flex items-start justify-between gap-4">
          <div>
            <DialogTitle class="text-title">{{ title }}</DialogTitle>
            <DialogDescription v-if="description" class="text-caption mt-1">{{ description }}</DialogDescription>
          </div>
          <DialogClose aria-label="Fechar" class="flex h-11 w-11 items-center justify-center rounded-md hover:bg-accent">
            <X class="size-5" aria-hidden="true" />
          </DialogClose>
        </div>
        <slot />
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
