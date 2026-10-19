import React from 'react';
import { CheckCircle2, Clock, PackageCheck, Truck } from 'lucide-react';
import { Layout } from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';

export const OrdersPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:p-6">
          <div>
            <h1 className="text-xl font-bold text-slate-900 sm:text-2xl">
              Painel de Pedidos
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              Olá, <span className="font-semibold text-slate-800">{user?.nome}</span>!{' '}
              {user?.role === 'LOJISTA' || user?.role === 'ADMIN'
                ? 'Gerencie o ciclo de vida e as entregas de todos os pedidos da loja.'
                : 'Acompanhe o status das suas entregas em tempo real.'}
            </p>
          </div>

          <div className="inline-flex items-center gap-2 self-start rounded-xl bg-emerald-50 px-3.5 py-2 text-xs font-semibold text-emerald-700 sm:self-auto">
            <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-hidden="true" />
            <span>Sessão JWT autenticada ({user?.role})</span>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="flex items-center gap-3.5 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
              <Clock className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Recebidos &amp; Preparo</p>
              <p className="text-sm font-semibold text-slate-900">
                Acompanhamento instantâneo
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3.5 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
              <Truck className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Em Rota de Entrega</p>
              <p className="text-sm font-semibold text-slate-900">
                Status atualizado pelo lojista
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3.5 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
              <PackageCheck className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Concluídos</p>
              <p className="text-sm font-semibold text-slate-900">
                Histórico seguro por perfil
              </p>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

