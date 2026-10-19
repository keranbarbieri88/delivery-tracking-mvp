import React, { useEffect, useState } from 'react';
import { Lock, Mail, Package, Store, User as UserIcon, UserPlus } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { extractApiError } from '../api/client';
import { Alert } from '../components/Alert';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { useAuth } from '../contexts/AuthContext';
import { Role } from '../types/auth';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface RegisterFormErrors {
  nome?: string;
  email?: string;
  senha?: string;
}

export const RegisterPage: React.FC = () => {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [role, setRole] = useState<Role>('CLIENTE');
  const [fieldErrors, setFieldErrors] = useState<RegisterFormErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [apiValidationMap, setApiValidationMap] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/pedidos', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const validateForm = (): boolean => {
    const errors: RegisterFormErrors = {};
    const trimmedNome = nome.trim();
    const trimmedEmail = email.trim();

    if (!trimmedNome) {
      errors.nome = 'O nome completo é obrigatório.';
    } else if (trimmedNome.length < 2) {
      errors.nome = 'Informe seu nome completo.';
    }

    if (!trimmedEmail) {
      errors.email = 'O e-mail é obrigatório.';
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      errors.email = 'Informe um endereço de e-mail válido (ex.: voce@exemplo.com).';
    }

    if (!senha) {
      errors.senha = 'A senha é obrigatória.';
    } else if (senha.length < 6) {
      errors.senha = 'A senha deve conter no mínimo 6 caracteres.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setApiError(null);
    setApiValidationMap({});

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      const createdUser = await register(nome, email, senha, role);
      navigate('/login', {
        replace: true,
        state: {
          registeredEmail: createdUser.email,
          welcomeMessage: `Bem-vindo(a), ${createdUser.nome}! Sua conta foi criada com sucesso. Faça login para continuar.`,
        },
      });
    } catch (error: unknown) {
      const parsed = extractApiError(error);
      setApiError(parsed.message);
      setApiValidationMap(parsed.validationErrors);

      if (Object.keys(parsed.validationErrors).length > 0) {
        setFieldErrors({
          nome: parsed.validationErrors.nome,
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
            Criar sua conta
          </h1>
          <p className="mt-1.5 text-sm text-slate-600">
            Cadastre-se como Cliente ou Lojista no DeliveryTracker
          </p>
        </div>

        <div className="mt-6 rounded-2xl border border-slate-200/80 bg-white p-6 shadow-xl shadow-slate-200/50 sm:p-8">
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {apiError && (
              <Alert
                variant="error"
                title="Não foi possível concluir o cadastro"
                message={apiError}
                validationErrors={apiValidationMap}
                onClose={() => {
                  setApiError(null);
                  setApiValidationMap({});
                }}
              />
            )}

            <Input
              id="nome"
              name="name"
              type="text"
              label="Nome completo"
              placeholder="Ex.: Maria Silva"
              autoComplete="name"
              required
              value={nome}
              onChange={(e) => {
                setNome(e.target.value);
                if (fieldErrors.nome) {
                  setFieldErrors((prev) => ({ ...prev, nome: undefined }));
                }
              }}
              error={fieldErrors.nome}
              leftIcon={<UserIcon className="h-4 w-4" aria-hidden="true" />}
            />

            <Input
              id="register-email"
              name="email"
              type="email"
              label="E-mail"
              hint="Utilize seu melhor e-mail (ex.: voce@exemplo.com)"
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
              id="new-password"
              name="new-password"
              type="password"
              label="Senha"
              hint="Mínimo de 6 caracteres"
              placeholder="Crie uma senha segura"
              autoComplete="new-password"
              minLength={6}
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

            <fieldset className="space-y-2 pt-1">
              <legend className="block text-sm font-medium text-slate-700">
                Tipo de Perfil
              </legend>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setRole('CLIENTE')}
                  aria-pressed={role === 'CLIENTE'}
                  className={`flex flex-col items-start gap-1 rounded-xl border p-3 text-left transition-all ${
                    role === 'CLIENTE'
                      ? 'border-orange-600 bg-orange-50/70 text-orange-950 ring-2 ring-orange-500/20'
                      : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-2 font-semibold text-sm">
                    <UserIcon
                      className={`h-4 w-4 ${
                        role === 'CLIENTE' ? 'text-orange-600' : 'text-slate-400'
                      }`}
                      aria-hidden="true"
                    />
                    <span>Cliente</span>
                  </div>
                  <span className="text-xs text-slate-500">
                    Realiza e acompanha seus próprios pedidos
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => setRole('LOJISTA')}
                  aria-pressed={role === 'LOJISTA'}
                  className={`flex flex-col items-start gap-1 rounded-xl border p-3 text-left transition-all ${
                    role === 'LOJISTA'
                      ? 'border-orange-600 bg-orange-50/70 text-orange-950 ring-2 ring-orange-500/20'
                      : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-2 font-semibold text-sm">
                    <Store
                      className={`h-4 w-4 ${
                        role === 'LOJISTA' ? 'text-orange-600' : 'text-slate-400'
                      }`}
                      aria-hidden="true"
                    />
                    <span>Lojista</span>
                  </div>
                  <span className="text-xs text-slate-500">
                    Gerencia preparo e envio de todos os pedidos
                  </span>
                </button>
              </div>
            </fieldset>

            <div className="pt-2">
              <Button
                type="submit"
                fullWidth
                loading={submitting}
                leftIcon={<UserPlus className="h-4 w-4" aria-hidden="true" />}
              >
                {submitting ? 'Cadastrando...' : 'Concluir cadastro'}
              </Button>
            </div>
          </form>

          <div className="mt-6 border-t border-slate-100 pt-5 text-center">
            <p className="text-sm text-slate-600">
              Já possui uma conta?{' '}
              <Link
                to="/login"
                className="font-semibold text-orange-600 transition-colors hover:text-orange-700 hover:underline"
              >
                Fazer login
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

