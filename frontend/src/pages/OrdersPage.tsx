import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  ChefHat,
  Clock,
  Inbox,
  Package,
  RefreshCw,
  Truck,
} from 'lucide-react';
import { extractApiError } from '../api/client';
import { listOrders, updateOrderStatus } from '../api/orderApi';
import { Button } from '../components/Button';
import { Layout } from '../components/Layout';
import { OrderCard } from '../components/OrderCard';
import { useAuth } from '../contexts/AuthContext';
import { Order, OrderStatus } from '../types/order';

type StatusFilter = 'TODOS' | OrderStatus;

const FILTER_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'RECEBIDO', label: 'Recebidos' },
  { value: 'EM_PREPARO', label: 'Em Preparo' },
  { value: 'DESPACHADO', label: 'Despachados' },
  { value: 'ENTREGUE', label: 'Entregues' },
  { value: 'CANCELADO', label: 'Cancelados' },
];

function matchesFilter(orderStatus: OrderStatus, filter: StatusFilter): boolean {
  if (filter === 'TODOS') {
    return true;
  }
  if (filter === 'DESPACHADO') {
    return orderStatus === 'DESPACHADO' || orderStatus === 'ENVIADO';
  }
  return orderStatus === filter;
}

export const OrdersPage: React.FC = () => {
  const { user } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('TODOS');

  const fetchOrders = useCallback(async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setErrorMessage(null);

    try {
      const data = await listOrders();
      setOrders(data);
    } catch (error: unknown) {
      const parsed = extractApiError(error);
      setErrorMessage(parsed.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void fetchOrders(false);
  }, [fetchOrders]);

  const handleStatusChange = useCallback(
    async (orderId: string, newStatus: OrderStatus) => {
      const updatedOrder = await updateOrderStatus(orderId, newStatus);
      setOrders((prevOrders) =>
        prevOrders.map((order) => (order.id === orderId ? updatedOrder : order))
      );
    },
    []
  );

  const filteredOrders = useMemo(
    () => orders.filter((order) => matchesFilter(order.status, statusFilter)),
    [orders, statusFilter]
  );

  const counters = useMemo(() => {
    let recebidos = 0;
    let emPreparo = 0;
    let despachados = 0;
    let entregues = 0;

    for (const order of orders) {
      if (order.status === 'RECEBIDO') recebidos += 1;
      else if (order.status === 'EM_PREPARO') emPreparo += 1;
      else if (order.status === 'DESPACHADO' || order.status === 'ENVIADO')
        despachados += 1;
      else if (order.status === 'ENTREGUE') entregues += 1;
    }

    return { recebidos, emPreparo, despachados, entregues };
  }, [orders]);

  const userRole = user?.role ?? 'CLIENTE';

  return (
    <Layout>
      <div className="space-y-6">
        {/* Cabeçalho da Página e Ação de Refresh */}
        <section className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:p-6">
          <div className="min-w-0">
            <h1 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">
              Acompanhamento de Pedidos
            </h1>
            <p className="mt-1 text-xs text-slate-600 sm:text-sm">
              {userRole === 'LOJISTA' || userRole === 'ADMIN'
                ? 'Visualize e atualize o status operacional de todos os pedidos em tempo real.'
                : 'Consulte o histórico e o andamento das suas entregas.'}
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-2.5">
            <Button
              variant="secondary"
              onClick={() => void fetchOrders(true)}
              loading={refreshing}
              disabled={loading}
              leftIcon={<RefreshCw className="h-4 w-4" aria-hidden="true" />}
              className="w-full sm:w-auto"
            >
              Atualizar lista
            </Button>
          </div>
        </section>

        {/* Resumo Operacional de Status */}
        <section
          aria-label="Resumo de pedidos por etapa"
          className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4"
        >
          <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-3.5 shadow-sm">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
              <Clock className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-xs font-medium text-slate-500">Recebidos</p>
              <p className="text-lg font-bold text-slate-900">{counters.recebidos}</p>
            </div>
          </div>

          <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-3.5 shadow-sm">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
              <ChefHat className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-xs font-medium text-slate-500">Em Preparo</p>
              <p className="text-lg font-bold text-slate-900">{counters.emPreparo}</p>
            </div>
          </div>

          <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-3.5 shadow-sm">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-purple-50 text-purple-600">
              <Truck className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-xs font-medium text-slate-500">Despachados</p>
              <p className="text-lg font-bold text-slate-900">{counters.despachados}</p>
            </div>
          </div>

          <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-3.5 shadow-sm">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
              <CheckCircle2 className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-xs font-medium text-slate-500">Entregues</p>
              <p className="text-lg font-bold text-slate-900">{counters.entregues}</p>
            </div>
          </div>
        </section>

        {/* Barra de Filtros Responsiva */}
        <section
          aria-label="Filtrar pedidos por status"
          className="flex items-center gap-2 overflow-x-auto pb-1"
        >
          {FILTER_OPTIONS.map((option) => {
            const active = statusFilter === option.value;
            return (
              <button
                key={option.value}
                type="button"
                onClick={() => setStatusFilter(option.value)}
                aria-pressed={active}
                className={`shrink-0 rounded-xl px-3.5 py-2 text-xs font-semibold transition-all sm:text-sm ${
                  active
                    ? 'bg-orange-600 text-white shadow-sm'
                    : 'border border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50'
                }`}
              >
                {option.label}
              </button>
            );
          })}
        </section>

        {/* Estado de Erro com opção Tentar novamente */}
        {errorMessage && !loading && (
          <div
            role="alert"
            className="flex flex-col items-start justify-between gap-4 rounded-2xl border border-red-200 bg-red-50 p-4 text-red-900 shadow-sm sm:flex-row sm:items-center sm:p-5"
          >
            <div className="flex items-start gap-3">
              <AlertTriangle
                className="mt-0.5 h-5 w-5 shrink-0 text-red-600"
                aria-hidden="true"
              />
              <div>
                <h2 className="text-sm font-bold">
                  Não foi possível carregar os pedidos
                </h2>
                <p className="mt-0.5 text-xs text-red-700 sm:text-sm">
                  {errorMessage}
                </p>
              </div>
            </div>

            <Button
              variant="danger"
              onClick={() => void fetchOrders(false)}
              leftIcon={<RefreshCw className="h-4 w-4" aria-hidden="true" />}
              className="w-full shrink-0 sm:w-auto"
            >
              Tentar novamente
            </Button>
          </div>
        )}

        {/* Estado de Loading (Skeleton) */}
        {loading && (
          <div
            aria-busy="true"
            aria-label="Carregando lista de pedidos"
            className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3"
          >
            {[1, 2, 3, 4, 5, 6].map((skeletonId) => (
              <div
                key={skeletonId}
                className="flex flex-col justify-between rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
              >
                <div className="animate-pulse space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <div className="space-y-2">
                      <div className="h-4 w-24 rounded bg-slate-200" />
                      <div className="h-3 w-32 rounded bg-slate-100" />
                    </div>
                    <div className="h-6 w-24 rounded-full bg-slate-200" />
                  </div>
                  <div className="h-4 w-3/4 rounded bg-slate-200" />
                  <div className="space-y-2 rounded-xl bg-slate-50 p-3">
                    <div className="h-3 w-1/2 rounded bg-slate-200" />
                    <div className="h-3 w-full rounded bg-slate-100" />
                    <div className="h-3 w-5/6 rounded bg-slate-100" />
                  </div>
                  <div className="border-t border-slate-100 pt-3">
                    <div className="flex items-center justify-between">
                      <div className="h-3 w-20 rounded bg-slate-200" />
                      <div className="h-5 w-24 rounded bg-slate-200" />
                    </div>
                    <div className="mt-3 h-9 w-full rounded-xl bg-slate-100" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!loading && !errorMessage && filteredOrders.length === 0 && (
          <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-12 text-center shadow-sm sm:py-16">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-50 text-orange-600">
              {orders.length === 0 ? (
                <Package className="h-7 w-7" aria-hidden="true" />
              ) : (
                <Inbox className="h-7 w-7" aria-hidden="true" />
              )}
            </div>
            <h2 className="mt-4 text-base font-bold text-slate-900 sm:text-lg">
              {orders.length === 0
                ? 'Nenhum pedido cadastrado no momento'
                : 'Nenhum pedido encontrado para este filtro'}
            </h2>
            <p className="mt-1.5 max-w-md text-xs text-slate-500 sm:text-sm">
              {orders.length === 0
                ? 'Assim que novos pedidos forem realizados, eles aparecerão automaticamente nesta listagem para acompanhamento.'
                : 'Experimente selecionar outro filtro de status ou atualizar a listagem.'}
            </p>
            <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
              {statusFilter !== 'TODOS' && (
                <Button
                  variant="secondary"
                  onClick={() => setStatusFilter('TODOS')}
                >
                  Mostrar todos os status
                </Button>
              )}
              <Button
                variant="primary"
                onClick={() => void fetchOrders(true)}
                loading={refreshing}
                leftIcon={<RefreshCw className="h-4 w-4" aria-hidden="true" />}
              >
                Atualizar pedidos
              </Button>
            </div>
          </div>
        )}

        {/* Grade Responsiva de Cards de Pedidos */}
        {!loading && filteredOrders.length > 0 && (
          <section
            aria-label="Lista de pedidos"
            className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3"
          >
            {filteredOrders.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                userRole={userRole}
                onStatusChange={handleStatusChange}
              />
            ))}
          </section>
        )}
      </div>
    </Layout>
  );
};
