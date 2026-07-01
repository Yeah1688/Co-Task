<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { CheckCircle, AlertCircle, Loader2 } from 'lucide-vue-next'
import api from '../api/axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const inviteCode = route.query.code as string | null
const status = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('正在验证邀请密钥，加入企业隔离工作区...')

onMounted(() => {
  if (!authStore.isAuthenticated) {
    localStorage.setItem('redirect_after_auth', window.location.pathname + window.location.search)
    router.push('/')
    return
  }

  if (!inviteCode) {
    status.value = 'error'
    message.value = '无效的邀请链接，缺少安全验证特征码。'
    return
  }

  processJoin()
})

const processJoin = async () => {
  try {
    const response = await api.post('/workspaces/join', { inviteCode })
    const { workspaceId, message: serverMsg } = response.data

    await authStore.fetchUserWorkspaces()

    status.value = 'success'
    message.value = serverMsg || '成功加入团队工作区！'

    setTimeout(() => {
      const targetWs = authStore.workspaces.find((w) => w.id === workspaceId)
      if (targetWs) {
        authStore.setActiveWorkspace(targetWs)
      }
      router.push('/')
    }, 2000)
  } catch (err: any) {
    status.value = 'error'
    message.value = err.response?.data?.message || '接受邀请失败，可能您已经是该成员或链接已失效。'
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 px-4">
    <div class="max-w-md w-full p-8 bg-white rounded-2xl shadow-xl border border-slate-100 text-center space-y-6">
      <div class="mx-auto h-16 w-16 rounded-2xl flex items-center justify-center shadow-lg transition-transform animate-pulse">
        <Loader2 v-if="status === 'loading'" class="h-8 w-8 text-blue-600 animate-spin" />
        <CheckCircle v-if="status === 'success'" class="h-8 w-8 text-emerald-500 bg-emerald-50 rounded-xl p-1" />
        <AlertCircle v-if="status === 'error'" class="h-8 w-8 text-rose-500 bg-rose-50 rounded-xl p-1" />
      </div>

      <h2 class="text-xl font-bold text-slate-800">
        <template v-if="status === 'loading'">协同邀请同步中</template>
        <template v-if="status === 'success'">加入成功</template>
        <template v-if="status === 'error'">安全认证失败</template>
      </h2>

      <p class="text-slate-500 text-sm">{{ message }}</p>

      <button
        v-if="status === 'error'"
        @click="router.push('/')"
        class="w-full py-2.5 bg-slate-800 hover:bg-slate-900 text-white font-medium rounded-xl text-sm transition-colors"
      >
        返回工作台
      </button>
    </div>
  </div>
</template>
