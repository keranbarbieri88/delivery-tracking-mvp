export type Role = 'CLIENTE' | 'LOJISTA' | 'ADMIN';

export interface User {
  id: string;
  nome: string;
  email: string;
  role: Role;
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  type: string;
  user: User;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}

export interface JwtClaims {
  sub?: string;
  userId?: string;
  role?: Role;
  nome?: string;
  iat?: number;
  exp?: number;
}

export interface ParsedApiError {
  status: number;
  message: string;
  validationErrors: Record<string, string>;
}

