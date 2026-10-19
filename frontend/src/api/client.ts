import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ErrorResponse, ParsedApiError } from '../types/auth';

export const TOKEN_STORAGE_KEY = 'delivery_tracker_token';
export const USER_STORAGE_KEY = 'delivery_tracker_user';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token =
      localStorage.getItem(TOKEN_STORAGE_KEY) ||
      sessionStorage.getItem(TOKEN_STORAGE_KEY);

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: unknown) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorResponse>) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || '';
    const isAuthEndpoint =
      requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register');

    if (status === 401 && !isAuthEndpoint) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
      sessionStorage.removeItem(USER_STORAGE_KEY);

      if (window.location.pathname !== '/login') {
        window.location.replace('/login');
      }
    }

    return Promise.reject(error);
  }
);

export function extractApiError(error: unknown): ParsedApiError {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    const status = error.response?.status ?? 0;
    const data = error.response?.data;

    if (data && typeof data === 'object') {
      return {
        status: data.status ?? status,
        message:
          data.message ||
          'Não foi possível concluir a operação. Verifique os dados informados.',
        validationErrors: data.validationErrors ?? {},
      };
    }

    if (status === 401) {
      return {
        status: 401,
        message: 'Credenciais inválidas: e-mail ou senha incorretos.',
        validationErrors: {},
      };
    }

    if (!error.response) {
      return {
        status: 0,
        message:
          'Não foi possível conectar ao servidor da API. Verifique se o back-end está em execução.',
        validationErrors: {},
      };
    }
  }

  if (error instanceof Error) {
    return {
      status: 0,
      message: error.message,
      validationErrors: {},
    };
  }

  return {
    status: 0,
    message: 'Ocorreu um erro inesperado. Tente novamente.',
    validationErrors: {},
  };
}

