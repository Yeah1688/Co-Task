<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { KanbanSquare, Mail, Lock, User, ArrowRight } from 'lucide-vue-next'
import api from '../api/axios'

const authStore = useAuthStore()

const isLogin = ref(true)
const email = ref('')
const name = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const handleSubmit = async () => {
  error.value = ''
  loading.value = true

  try {
    if (isLogin.value) {
      await authStore.login(email.value, password.value)
      const redirectUrl = localStorage.getItem('redirect_after_auth')
      if (redirectUrl) {
        localStorage.removeItem('redirect_after_auth')
        window.location.href = redirectUrl
      }
    } else {
      await api.post('/auth/register', { email: email.value, name: name.value, password: password.value })
      isLogin.value = true
      alert('注册成功，请登录！')
    }
  } catch (err: any) {
    error.value = err.message || '网络请求失败，请检查后端服务是否开启'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 px-4">
    <div class="max-w-md w-full space-y-8 p-8 bg-white rounded-2xl shadow-xl border border-slate-100">
      <div class="text-center">
        <div class="mx-auto h-12 w-12 rounded-xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/20">
          <KanbanSquare class="h-6 w-6" />
        </div>
        <h2 class="mt-4 text-3xl font-extrabold text-slate-900">
          {{ isLogin ? '欢迎回来 CoTask' : '创建您的智协账户' }}
        </h2>
        <p class="mt-2 text-sm text-slate-500">
          {{ isLogin ? '多维协作研发看板系统' : '开启团队高效敏捷迭代之旅' }}
        </p>
      </div>

      <div v-if="error" class="bg-red-50 text-red-600 p-3 rounded-lg text-sm font-medium border border-red-100">
        {{ error }}
      </div>

      <form class="mt-8 space-y-4" @submit.prevent="handleSubmit">
        <div v-if="!isLogin" class="relative">
          <User class="absolute left-3 top-3.5 h-5 w-5 text-slate-400" />
          <input
            type="text"
            required
            placeholder="您的姓名/昵称"
            class="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-colors"
            v-model="name"
          />
        </div>

        <div class="relative">
          <Mail class="absolute left-3 top-3.5 h-5 w-5 text-slate-400" />
          <input
            type="email"
            required
            placeholder="注册电子邮箱"
            class="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-colors"
            v-model="email"
          />
        </div>

        <div class="relative">
          <Lock class="absolute left-3 top-3.5 h-5 w-5 text-slate-400" />
          <input
            type="password"
            required
            placeholder="请输入密码"
            class="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-colors"
            v-model="password"
          />
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full mt-6 py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl shadow-lg shadow-blue-500/10 flex items-center justify-center space-x-2 transition-all transform active:scale-[0.98] disabled:opacity-50"
        >
          <span>{{ loading ? '正在处理...' : isLogin ? '立即登录' : '立即注册' }}</span>
          <ArrowRight v-if="!loading" class="h-4 w-4" />
        </button>
      </form>

      <div class="text-center mt-4">
        <button
          @click="isLogin = !isLogin; error = ''"
          class="text-sm font-medium text-blue-600 hover:text-blue-700 transition-colors"
        >
          {{ isLogin ? '还没有账号？立即创建' : '已有账号？返回登录' }}
        </button>
      </div>
    </div>
  </div>
</template>
