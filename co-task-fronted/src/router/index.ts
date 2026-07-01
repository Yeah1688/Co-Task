import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../App.vue'),
    },
    {
      path: '/join',
      name: 'join',
      component: () => import('../components/JoinWorkspace.vue'),
    },
  ],
});

export default router;
