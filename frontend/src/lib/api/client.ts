// @path frontend/src/lib/api/client.ts
// @owner frontend
// @responsibility Axios client configurado — interceptors para JWT + refresh automático
// @see docs/SECURITY_LGPD.md#autenticacao | docs/API_CONTRACTS.md#convencoes
import axios, { AxiosError, AxiosInstance } from 'axios';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8000';

export const apiClient: AxiosInstance = axios.create({
  baseURL: `${BASE_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,  // envia cookies HttpOnly automaticamente
});

// Injeta o access token do localStorage (fallback enquanto HttpOnly cookies não estão configurados no dev)
apiClient.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = sessionStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Interceptor de resposta — trata 401 e tenta refresh automático
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as typeof error.config & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = sessionStorage.getItem('refresh_token');
        if (!refreshToken) throw new Error('No refresh token');

        const { data } = await axios.post(`${BASE_URL}/api/v1/auth/refresh`, {
          refresh_token: refreshToken,
        });

        sessionStorage.setItem('access_token', data.access_token);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${data.access_token}`;
        }

        return apiClient(originalRequest);
      } catch {
        sessionStorage.clear();
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);
