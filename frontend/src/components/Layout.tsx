import React from 'react';
import { LogOut, Package, Shield, Store, User as UserIcon } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Role } from '../types/auth';
import { Button } from './Button';

const roleBadgeConfig: Record<
  Role,
  { label: string; className: string; icon: React.ReactNode }
> = {
  CLIENTE: {
    label: 'Cliente',
    className: 'bg-blue-50 text-blue-700 border-blue-200',
    icon: <UserIcon className="h-3.5 w-3.5" aria-hidden="true" />,
  },
  LOJISTA: {
    label: 'Lojista',
    className: 'bg-orange-50 text-orange-700 border-orange-200',
    icon: <Store className="h-3.5 w-3.5" aria-hidden="true" />,
  },
  ADMIN: {
    label: 'Admin',
    className: 'bg-purple-50 text-purple-700 border-purple-200',
    icon: <Shield className="h-3.5 w-3.5" aria-hidden="true" />,
  },
};

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const roleBadge = user ? roleBadgeConfig[user.role] : roleBadgeConfig.CLIENTE;

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
          <Link
            to="/pedidos"
            className="flex items-center gap-2.5 text-slate-900 hover:opacity-90"
          >
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-600 text-white shadow-sm">
              <Package className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <span className="block text-base font-bold tracking-tight sm:text-lg">
                Delivery<span className="text-orange-600">Tracker</span>
              </span>
              <span className="hidden text-xs text-slate-500 sm:block">
                Gestão e rastreamento de entregas em tempo real
              </span>
            </div>
          </Link>

          {user && (
            <div className="flex items-center gap-3">
              <div className="flex flex-col items-end">
                <div className="flex items-center gap-2">
                  <span className="max-w-[140px] truncate text-sm font-semibold text-slate-800 sm:max-w-[220px]">
                    {user.nome}
                  </span>
                  <span
                    className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${roleBadge.className}`}
                  >
                    {roleBadge.icon}
                    {roleBadge.label}
                  </span>
                </div>
                <span className="hidden text-xs text-slate-500 sm:block">
                  {user.email}
                </span>
              </div>

              <Button
                variant="secondary"
                onClick={handleLogout}
                leftIcon={<LogOut className="h-4 w-4" aria-hidden="true" />}
                aria-label="Sair da conta"
                className="!px-3 !py-2 text-xs sm:text-sm"
              >
                <span className="hidden sm:inline">Sair</span>
              </Button>
            </div>
          )}
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 sm:py-8">
        {children}
      </main>

      <footer className="border-t border-slate-200 bg-white py-4 text-center text-xs text-slate-500">
        DeliveryTracker MVP • Spring Boot 3 &amp; React + TypeScript
      </footer>
    </div>
  );
};

