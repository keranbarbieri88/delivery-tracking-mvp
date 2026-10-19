import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { loginRequest, registerRequest } from '../api/authApi';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '../api/client';
import { JwtClaims, Role, User } from '../types/auth';

export interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, senha: string) => Promise<User>;
  register: (nome: string, email: string, senha: string, role?: Role) => Promise<User>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function decodeJwtClaims(token: string): JwtClaims | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const jsonPayload = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload) as JwtClaims;
  } catch {
    return null;
  }
}

function isTokenExpired(claims: JwtClaims | null): boolean {
  if (!claims || typeof claims.exp !== 'number') {
    return false;
  }
  const nowInSeconds = Math.floor(Date.now() / 1000);
  return claims.exp <= nowInSeconds;
}

function normalizeRole(role?: string): Role {
  if (role === 'LOJISTA' || role === 'ADMIN' || role === 'CLIENTE') {
    return role;
  }
  return 'CLIENTE';
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
    const storedUserJson = localStorage.getItem(USER_STORAGE_KEY);

    if (!storedToken) {
      setLoading(false);
      return;
    }

    const claims = decodeJwtClaims(storedToken);
    if (!claims || isTokenExpired(claims)) {
      clearSession();
      setLoading(false);
      return;
    }

    if (storedUserJson) {
      try {
        const parsedUser = JSON.parse(storedUserJson) as User;
        setUser({
          ...parsedUser,
          email: parsedUser.email || claims.sub || '',
          role: normalizeRole(parsedUser.role ?? claims.role),
        });
        setLoading(false);
        return;
      } catch {
        // Fallback para hidratação a partir das claims do JWT
      }
    }

    setUser({
      id: claims.userId || '',
      nome: claims.nome || claims.sub || 'Usuário',
      email: claims.sub || '',
      role: normalizeRole(claims.role),
    });
    setLoading(false);
  }, [clearSession]);

  const login = useCallback(async (email: string, senha: string): Promise<User> => {
    const authResponse = await loginRequest({ email: email.trim(), senha });
    const claims = decodeJwtClaims(authResponse.token);

    const authenticatedUser: User = {
      id: authResponse.user?.id || claims?.userId || '',
      nome: authResponse.user?.nome || claims?.nome || email.trim(),
      email: authResponse.user?.email || claims?.sub || email.trim(),
      role: normalizeRole(authResponse.user?.role ?? claims?.role),
      createdAt: authResponse.user?.createdAt,
    };

    localStorage.setItem(TOKEN_STORAGE_KEY, authResponse.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(authenticatedUser));
    setUser(authenticatedUser);
    return authenticatedUser;
  }, []);

  const register = useCallback(
    async (
      nome: string,
      email: string,
      senha: string,
      role: Role = 'CLIENTE'
    ): Promise<User> => {
      return registerRequest({
        nome: nome.trim(),
        email: email.trim(),
        senha,
        role,
      });
    },
    []
  );

  const logout = useCallback(() => {
    clearSession();
  }, [clearSession]);

  const contextValue = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      loading,
      login,
      register,
      logout,
    }),
    [user, loading, login, register, logout]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
};

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser utilizado dentro de um AuthProvider');
  }
  return context;
}

