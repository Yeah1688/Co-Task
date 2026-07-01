import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import api from '../api/axios';
import type { Workspace } from '../types';

interface User {
  id: string;
  name: string;
  email: string;
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null);
  const token = ref<string | null>(localStorage.getItem('cotask_token'));
  const workspaces = ref<Workspace[]>([]);
  const activeWorkspace = ref<Workspace | null>(null);

  const isAuthenticated = computed(() => !!token.value);

  async function login(email: string, password: string) {
    const response = await api.post('/auth/login', { email, password });
    const { token: newToken, user: newUser } = response.data;

    localStorage.setItem('cotask_token', newToken);
    token.value = newToken;
    user.value = newUser;

    await fetchUserWorkspaces();
  }

  function logout() {
    localStorage.removeItem('cotask_token');
    localStorage.removeItem('active_workspace_id');
    token.value = null;
    user.value = null;
    workspaces.value = [];
    activeWorkspace.value = null;
  }

  async function fetchUserWorkspaces() {
    try {
      const response = await api.get('/workspaces/my');
      workspaces.value = response.data;

      if (workspaces.value.length > 0) {
        const savedId = localStorage.getItem('active_workspace_id');
        const matched = workspaces.value.find((w: Workspace) => w.id === savedId);
        const defaultActive = matched || workspaces.value[0];

        activeWorkspace.value = defaultActive;
        localStorage.setItem('active_workspace_id', defaultActive.id);
      }
    } catch (err) {
      console.error('加载工作区失败', err);
    }
  }

  function setActiveWorkspace(workspace: Workspace) {
    activeWorkspace.value = workspace;
    localStorage.setItem('active_workspace_id', workspace.id);
  }

  return {
    user,
    token,
    workspaces,
    activeWorkspace,
    isAuthenticated,
    login,
    logout,
    fetchUserWorkspaces,
    setActiveWorkspace,
  };
});
