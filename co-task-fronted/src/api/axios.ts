import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:5000/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：自动在每个请求头中注入 JWT Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('cotask_token');
    const isAuthUrl = config.url?.includes('/auth/login') || config.url?.includes('/auth/register');

    if (token && config.headers && !isAuthUrl) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：统一处理错误（如 401 未授权自动跳回登录页）
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      console.error('登录过期或未授权');
      localStorage.removeItem('cotask_token');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export default api;
