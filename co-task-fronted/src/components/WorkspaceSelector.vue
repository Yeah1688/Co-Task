<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { Plus, Building2, ShieldCheck, Share2, Copy, Check, Link2 } from 'lucide-vue-next'
import api from '../api/axios'

const authStore = useAuthStore()

const showModal = ref(false)
const newName = ref('')
const newDesc = ref('')

const handleCreate = async () => {
  if (!newName.value.trim()) return

  try {
    await api.post('/workspaces', { name: newName.value, description: newDesc.value })
    newName.value = ''
    newDesc.value = ''
    showModal.value = false
    await authStore.fetchUserWorkspaces()
  } catch (err) {
    alert('创建工作区失败')
  }
}

// ---- InviteButton logic ----
const inviteUrl = ref('')
const copied = ref(false)
const loading = ref(false)
const showDropdown = ref(false)

const handleGenerateLink = async () => {
  if (!authStore.activeWorkspace) return
  loading.value = true
  try {
    const response = await api.get(`/workspaces/${authStore.activeWorkspace.id}/invite-link`)
    inviteUrl.value = response.data.inviteUrl
  } catch (err) {
    alert('无法生成邀请链接，权限校验失败')
  } finally {
    loading.value = false
  }
}

const copyToClipboard = () => {
  navigator.clipboard.writeText(inviteUrl.value)
  copied.value = true
  setTimeout(() => (copied.value = false), 2000)
}
</script>

<template>
  <div class="w-64 bg-slate-900 text-slate-300 h-screen flex flex-col border-r border-slate-800">
    <!-- 侧边栏头部 -->
    <div class="p-4 border-b border-slate-800 flex items-center justify-between">
      <div class="flex items-center space-x-2 overflow-hidden">
        <Building2 class="h-5 w-5 text-blue-400 shrink-0" />
        <span class="font-bold text-white truncate">
          {{ authStore.activeWorkspace ? authStore.activeWorkspace.name : '暂无激活工作区' }}
        </span>
      </div>
      <div class="flex items-center space-x-2">
        <span
          v-if="authStore.activeWorkspace"
          class="text-[10px] bg-blue-500/20 text-blue-400 px-1.5 py-0.5 rounded border border-blue-500/30 flex items-center shrink-0"
        >
          <ShieldCheck class="h-3 w-3 mr-0.5" />
          {{ authStore.activeWorkspace.role }}
        </span>

        <!-- Invite Button -->
        <div v-if="authStore.activeWorkspace && authStore.activeWorkspace.role !== 'MEMBER'" class="relative inline-block text-left">
          <button
            @click="showDropdown = !showDropdown; if (!inviteUrl) handleGenerateLink()"
            class="flex items-center space-x-1.5 px-3 py-1.5 bg-blue-50 hover:bg-blue-100 text-blue-600 rounded-xl text-xs font-semibold transition-colors shadow-sm"
          >
            <Share2 class="h-3.5 w-3.5" />
            <span>邀请成员</span>
          </button>

          <div
            v-if="showDropdown"
            class="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-100 p-4 z-50 space-y-3"
          >
            <div>
              <h4 class="text-sm font-bold text-slate-800 flex items-center">
                <Link2 class="h-4 w-4 mr-1 text-blue-500" />
                团队隔离邀请链接
              </h4>
              <p class="text-[11px] text-slate-400 mt-0.5">
                任何获得此链接的用户均可以普通成员权限加入当前多租户沙箱。
              </p>
            </div>

            <div class="flex items-center space-x-2">
              <input
                type="text"
                readonly
                :value="loading ? '正在从云端刷新特征密钥...' : inviteUrl"
                class="flex-1 bg-slate-50 border border-slate-200 rounded-lg p-2 text-xs text-slate-600 focus:outline-none truncate"
              />
              <button
                :disabled="loading || !inviteUrl"
                @click="copyToClipboard"
                :class="[
                  'p-2 rounded-lg transition-colors border',
                  copied
                    ? 'bg-emerald-50 text-emerald-600 border-emerald-200'
                    : 'bg-blue-600 text-white hover:bg-blue-700 border-blue-600',
                ]"
              >
                <Check v-if="copied" class="h-4 w-4" />
                <Copy v-else class="h-4 w-4" />
              </button>
            </div>

            <div class="flex justify-between items-center pt-1 border-t border-slate-50">
              <span class="text-[10px] text-slate-400">安全性：代码24小时内有效</span>
              <button @click="handleGenerateLink" class="text-[10px] text-blue-600 hover:underline font-medium">
                重新生成密钥
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 多租户工作区切换器 -->
    <div class="flex-1 overflow-y-auto p-2 space-y-1">
      <div class="text-xs font-semibold text-slate-500 px-2 py-1 uppercase tracking-wider">
        切换多租户工作区
      </div>
      <button
        v-for="ws in authStore.workspaces"
        :key="ws.id"
        @click="authStore.setActiveWorkspace(ws)"
        :class="[
          'w-full text-left px-3 py-2.5 rounded-xl flex items-center justify-between transition-colors',
          authStore.activeWorkspace?.id === ws.id
            ? 'bg-blue-600 text-white font-medium shadow-md shadow-blue-600/10'
            : 'hover:bg-slate-800 text-slate-400 hover:text-slate-200',
        ]"
      >
        <span class="truncate">{{ ws.name }}</span>
      </button>
    </div>

    <!-- 底部：新建工作区 -->
    <div class="p-2 border-t border-slate-800">
      <button
        @click="showModal = true"
        class="w-full py-2 px-3 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm rounded-xl flex items-center justify-center space-x-1 transition-colors"
      >
        <Plus class="h-4 w-4" />
        <span>新建多租户工作区</span>
      </button>
    </div>

    <!-- 弹窗 -->
    <div
      v-if="showModal"
      class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4"
    >
      <form
        @submit.prevent="handleCreate"
        class="bg-white rounded-2xl p-6 max-w-sm w-full space-y-4 shadow-2xl text-slate-800"
      >
        <h3 class="text-lg font-bold text-slate-900">建立企业隔离工作区</h3>
        <div class="space-y-1">
          <label class="text-xs font-semibold text-slate-500">工作区名称</label>
          <input
            type="text"
            required
            placeholder="如：山东海洋局研发一期"
            class="w-full border border-slate-200 rounded-xl p-2.5 text-sm"
            v-model="newName"
          />
        </div>
        <div class="space-y-1">
          <label class="text-xs font-semibold text-slate-500">业务描述 (选填)</label>
          <textarea
            placeholder="多租户沙箱隔离环境..."
            class="w-full border border-slate-200 rounded-xl p-2.5 text-sm h-20"
            v-model="newDesc"
          ></textarea>
        </div>
        <div class="flex space-x-2 justify-end pt-2">
          <button
            type="button"
            @click="showModal = false"
            class="px-4 py-2 text-sm font-medium text-slate-500 hover:bg-slate-100 rounded-xl"
          >
            取消
          </button>
          <button
            type="submit"
            class="px-4 py-2 text-sm font-medium bg-blue-600 hover:bg-blue-700 text-white rounded-xl"
          >
            确认创建
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
