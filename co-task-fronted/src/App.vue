<script setup lang="ts">
import { onMounted, computed } from "vue";
import { useAuthStore } from "./stores/authStore";
import AuthPage from "./components/AuthPage.vue";
import WorkspaceSelector from "./components/WorkspaceSelector.vue";
import KanbanBoard from "./components/KanbanBoard.vue";

const authStore = useAuthStore();

const activeWorkspaceId = computed(() => authStore.activeWorkspace?.id);

onMounted(() => {
  if (authStore.isAuthenticated) {
    authStore.fetchUserWorkspaces();
  }
});
</script>

<template>
  <AuthPage v-if="!authStore.isAuthenticated" />

  <div v-else-if="!authStore.activeWorkspace" class="flex items-center justify-center h-screen">
    正在加载工作区...
  </div>

  <div v-else class="flex min-h-screen bg-slate-50">
    <WorkspaceSelector />
    <div class="flex-1 overflow-hidden">
      <KanbanBoard :workspace-id="activeWorkspaceId!" />
    </div>
  </div>
</template>
