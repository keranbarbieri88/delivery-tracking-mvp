import React, { useEffect, useState } from 'react';
import { Lock, LogIn, Mail, Package } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { extractApiError } from '../api/client';
import { Alert } from '../components/Alert';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { useAuth } from '../contexts/AuthContext';

interface LocationState {
  registeredEmail?: string;
  welcomeMessage?: string;
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const LoginPage: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state as LocationState | null) ?? null;

  const [email, setEmail] = useState(state?.registeredEmail ?? '');
  const [senha, setSenha] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; senha?: string }>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(
    state?.welcomeMessage ?? null
  );
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/pedidos', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const validateForm = (): boolean => {
    const errors: { email?: string; senha?: string } = {};
    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      errors.email = 'O e-mail é obrigatório.';
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      errors.email = 'Informe um endereço de e-mail válido (ex.: voce@exemplo.com).';
    }

    if (!senha) {
      errors.senha = 'A senha é obrigatória.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setApiError(null);

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      await login(email, senha);
      navigate('/pedidos', { replace: true });
    } catch (error: unknown) {
      const parsed = extractApiError(error);
      setApiError(parsed.message);
      if (parsed.validationErrors.email || parsed.validationErrors.senha) {
        setFieldErrors({
          email: parsed.validationErrors.email,
          senha: parsed.validationErrors.senha,
        });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col justify-center bg-gradient-to-br from-slate-50 via-orange-50/30 to-slate-100 px-4 py-8 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex flex-col items-center text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-orange-600 text-white shadow-md shadow-orange-600/25">
            <Package className="h-6 w-6" aria-hidden="true" />
          </div>
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Entrar no Delivery<span className="text-orange-600">Tracker</span>
          </h1>
          <p className="mt-1.5 text-sm text-slate-600">
            Acompanhe seus pedidos e entregas em tempo real
          </p>
        </div>

        <div className="mt-6 rounded-2xl border border-slate-200/80 bg-white p-6 shadow-xl shadow-slate-200/50 sm:p-8">
          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            {successBanner && (
              <Alert
                variant="success"
                title="Cadastro realizado!"
                message={successBanner}
                onClose={() => setSuccessBanner(null)}
              />
            )}

            {apiError && (
              <Alert
                variant="error"
                title="Falha na autenticação"
                message={apiError}
                onClose={() => setApiError(null)}
              />
            )}

            <Input
              id="email"
              name="email"
              type="email"
              label="E-mail"
              hint="Formato esperado: voce@exemplo.com"
              placeholder="seu@email.com"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (fieldErrors.email) {
                  setFieldErrors((prev) => ({ ...prev, email: undefined }));
                }
              }}
              error={fieldErrors.email}
              leftIcon={<Mail className="h-4 w-4" aria-hidden="true" />}
            />

            <Input
              id="current-password"
              name="password"
              type="password"
              label="Senha"
              placeholder="Digite sua senha"
              autoComplete="current-password"
              required
              enablePasswordToggle
              value={senha}
              onChange={(e) => {
                setSenha(e.target.value);
                if (fieldErrors.senha) {
                  setFieldErrors((prev) => ({ ...prev, senha: undefined }));
                }
              }}
              error={fieldErrors.senha}
              leftIcon={<Lock className="h-4 w-4" aria-hidden="true" />}
            />

            <Button
              type="submit"
              fullWidth
              loading={submitting}
              leftIcon={<LogIn className="h-4 w-4" aria-hidden="true" />}
            >
              {submitting ? 'Autenticando...' : 'Entrar na plataforma'}
            </Button>
          </form>

          <div className="mt-6 border-t border-slate-100 pt-5 text-center">
            <p className="text-sm text-slate-600">
              Ainda não possui uma conta?{' '}
              <Link
                to="/register"
                className="font-semibold text-orange-600 transition-colors hover:text-orange-700 hover:underline"
              >
                Criar conta gratuitamente
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

