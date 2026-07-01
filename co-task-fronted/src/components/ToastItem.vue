<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { CheckCircle, XCircle, AlertTriangle, X } from 'lucide-vue-next'
import type { Component } from 'vue'

export interface ToastMessage {
  id: string
  type: 'success' | 'error' | 'warning'
  message: string
}

const props = defineProps<{ toast: ToastMessage }>()
const emit = defineEmits<{ remove: [] }>()

const visible = ref(false)
let timer: ReturnType<typeof setTimeout>

onMounted(() => {
  visible.value = true
  timer = setTimeout(() => {
    visible.value = false
    setTimeout(() => emit('remove'), 300)
  }, 4000)
})

onUnmounted(() => clearTimeout(timer))

const bgColor: Record<string, string> = {
  success: 'bg-emerald-50 border-emerald-200 text-emerald-800',
  error: 'bg-red-50 border-red-200 text-red-800',
  warning: 'bg-yellow-50 border-yellow-200 text-yellow-800',
}

const icons: Record<string, Component> = {
  success: CheckCircle,
  error: XCircle,
  warning: AlertTriangle,
}
</script>

<template>
  <div
    :class="[
      'flex items-center space-x-3 px-4 py-3 rounded-xl border shadow-lg transition-all duration-300 max-w-sm',
      bgColor[toast.type] || bgColor.success,
      visible ? 'opacity-100 translate-x-0' : 'opacity-0 translate-x-4',
    ]"
  >
    <component :is="icons[toast.type]" class="h-5 w-5 shrink-0" />
    <p class="text-sm flex-1">{{ toast.message }}</p>
    <button @click="emit('remove')" class="p-0.5 hover:bg-black/5 rounded">
      <X class="h-3.5 w-3.5" />
    </button>
  </div>
</template>
