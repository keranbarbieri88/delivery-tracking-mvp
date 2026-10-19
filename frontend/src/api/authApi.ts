import { apiClient } from './client';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../types/auth';

export async function loginRequest(payload: LoginRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/auth/login', payload);
  return response.data;
}

export async function registerRequest(payload: RegisterRequest): Promise<User> {
  const response = await apiClient.post<User>('/auth/register', payload);
  return response.data;
}

