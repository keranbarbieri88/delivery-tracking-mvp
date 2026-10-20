import React, { useState } from 'react';
import {
  AlertCircle,
  Calendar,
  Hash,
  Loader2,
  MapPin,
  ShoppingBag,
  User as UserIcon,
} from 'lucide-react';
import { extractApiError } from '../api/client';
import { Role } from '../types/auth';
import { Order, OrderStatus } from '../types/order';
import { OrderStatusBadge } from './OrderStatusBadge';

export interface OrderCardProps {
  order: Order;
  userRole: Role;
  onStatusChange: (orderId: string, newStatus: OrderStatus) => Promise<void>;
}

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

const dateTimeFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

interface StatusOption {
  value: OrderStatus;
  label: string;
}

const ALL_STATUS_OPTIONS: StatusOption[] = [
  { value: 'RECEBIDO', label: 'Recebido' },
  { value: 'EM_PREPARO', label: 'Em Preparo' },
  { value: 'SAIU_PARA_ENTREGA', label: 'Saiu Para Entrega' },
  { value: 'ENTREGUE', label: 'Entregue' },
  { value: 'CANCELADO', label: 'Cancelado' },
];

function getAllowedNextStatuses(current: OrderStatus, role: Role): OrderStatus[] {
  if (current === 'ENTREGUE' || current === 'CANCELADO') {
    return [];
  }

  if (role === 'CLIENTE') {
    return current === 'RECEBIDO' ? ['CANCELADO'] : [];
  }

  switch (current) {
    case 'RECEBIDO':
      return ['EM_PREPARO', 'CANCELADO'];
    case 'EM_PREPARO':
      return ['SAIU_PARA_ENTREGA', 'CANCELADO'];
    case 'SAIU_PARA_ENTREGA':
      return ['ENTREGUE', 'CANCELADO'];
    default:
      return [];
  }
}

function formatDateSafe(isoString: string): string {
  try {
    const parsed = new Date(isoString);
    if (Number.isNaN(parsed.getTime())) {
      return isoString;
    }
    return dateTimeFormatter.format(parsed);
  } catch {
    return isoString;
  }
}

export const OrderCard: React.FC<OrderCardProps> = ({
  order,
  userRole,
  onStatusChange,
}) => {
  const [updating, setUpdating] = useState(false);
  const [cardError, setCardError] = useState<string | null>(null);

  const currentDisplayStatus = order.status;
  const isTerminal =
    currentDisplayStatus === 'ENTREGUE' || currentDisplayStatus === 'CANCELADO';
  const allowedNext = getAllowedNextStatuses(order.status, userRole);
  const canInteract = !isTerminal && allowedNext.length > 0 && !updating;

  const shortId = order.id ? order.id.slice(0, 8).toUpperCase() : '---';
  const totalItemsCount = order.itens.reduce(
    (acc, item) => acc + (Number(item.quantidade) || 0),
    0,
  );

  const handleSelectChange = async (event: React.ChangeEvent<HTMLSelectElement>) => {
    const nextStatus = event.target.value as OrderStatus;
    if (!nextStatus || nextStatus === currentDisplayStatus) {
      return;
    }

    setCardError(null);
    setUpdating(true);
    try {
      await onStatusChange(order.id, nextStatus);
    } catch (error: unknown) {
      const parsed = extractApiError(error);
      setCardError(parsed.message);
    } finally {
      setUpdating(false);
    }
  };

  return (
    <article
      aria-label={`Pedido ${shortId}`}
      className="flex flex-col justify-between rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm transition-shadow hover:shadow-md sm:p-5"
    >
      <div className="space-y-4">
        {/* Cabeçalho do Card: ID, Data e Badge de Status */}
        <div className="flex flex-wrap items-start justify-between gap-2 border-b border-slate-100 pb-3">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1.5 text-slate-900">
              <Hash className="h-4 w-4 shrink-0 text-orange-600" aria-hidden="true" />
              <span
                title={`ID completo: ${order.id}`}
                className="font-mono text-sm font-bold tracking-tight"
              >
                {shortId}
              </span>
            </div>
            <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
              <Calendar className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
              <time dateTime={order.criadoEm}>{formatDateSafe(order.criadoEm)}</time>
            </div>
          </div>

          <OrderStatusBadge status={order.status} />
        </div>

        {/* Cliente (quando disponível) e Endereço de Entrega */}
        <div className="space-y-2 text-xs sm:text-sm">
          {order.clienteNome && (
            <div className="flex items-center gap-2 text-slate-700">
              <UserIcon className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
              <span className="truncate font-medium">{order.clienteNome}</span>
            </div>
          )}

          <div className="flex items-start gap-2 text-slate-700">
            <MapPin
              className="mt-0.5 h-4 w-4 shrink-0 text-orange-600"
              aria-hidden="true"
            />
            <span className="break-words leading-snug text-slate-700">
              {order.enderecoEntrega}
            </span>
          </div>
        </div>

        {/* Lista resumida de itens do pedido */}
        <div className="rounded-xl bg-slate-50 p-3">
          <div className="mb-2 flex items-center justify-between text-xs font-semibold text-slate-600">
            <span className="inline-flex items-center gap-1.5">
              <ShoppingBag className="h-3.5 w-3.5 text-slate-400" aria-hidden="true" />
              Itens do pedido
            </span>
            <span>
              {totalItemsCount} {totalItemsCount === 1 ? 'unidade' : 'unidades'}
            </span>
          </div>

          <ul className="max-h-36 space-y-1.5 overflow-y-auto pr-1 text-xs sm:text-sm">
            {order.itens.map((item, index) => {
              const subtotal = item.subtotal ?? item.quantidade * item.precoUnitario;
              return (
                <li
                  key={item.id ?? `${order.id}-item-${index}`}
                  className="flex items-center justify-between gap-2 text-slate-700"
                >
                  <span className="min-w-0 flex-1 truncate">
                    <strong className="font-semibold text-slate-900">
                      {item.quantidade}x
                    </strong>{' '}
                    {item.nome}
                  </span>
                  <span className="shrink-0 font-medium text-slate-600">
                    {currencyFormatter.format(subtotal)}
                  </span>
                </li>
              );
            })}
          </ul>
        </div>
      </div>

      {/* Rodapé do Card: Total em BRL e Ação Rápida de Atualização de Status */}
      <div className="mt-4 space-y-3 border-t border-slate-100 pt-3.5">
        <div className="flex items-baseline justify-between">
          <span className="text-xs font-medium uppercase tracking-wider text-slate-500">
            Valor Total
          </span>
          <span className="text-base font-extrabold text-slate-900 sm:text-lg">
            {currencyFormatter.format(order.valorTotal)}
          </span>
        </div>

        {cardError && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-2.5 text-xs text-red-700"
          >
            <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0 text-red-600" />
            <span className="flex-1 leading-relaxed">{cardError}</span>
          </div>
        )}

        <div className="space-y-1.5">
          <label
            htmlFor={`status-select-${order.id}`}
            className="block text-xs font-medium text-slate-600"
          >
            {isTerminal
              ? 'Pedido finalizado (status definitivo)'
              : userRole === 'CLIENTE'
                ? allowedNext.includes('CANCELADO')
                  ? 'Ação disponível (cancelamento permitido enquanto Recebido)'
                  : 'Pedido em andamento pela loja'
                : 'Atualizar etapa do pedido'}
          </label>

          <div className="relative">
            <select
              id={`status-select-${order.id}`}
              value={currentDisplayStatus}
              disabled={!canInteract}
              onChange={handleSelectChange}
              aria-label={`Atualizar status do pedido ${shortId}`}
              className="block w-full rounded-xl border border-slate-300 bg-white px-3 py-2 pr-9 text-xs font-semibold text-slate-800 shadow-sm transition-colors focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500 sm:text-sm"
            >
              {ALL_STATUS_OPTIONS.map((option) => {
                const isCurrent = option.value === currentDisplayStatus;
                const isAllowed = allowedNext.includes(option.value);
                return (
                  <option
                    key={option.value}
                    value={option.value}
                    disabled={!isCurrent && !isAllowed}
                  >
                    {option.label}
                    {isCurrent ? ' (Atual)' : !isAllowed ? ' (Indisponível)' : ''}
                  </option>
                );
              })}
            </select>

            {updating && (
              <div className="pointer-events-none absolute inset-y-0 right-2.5 flex items-center">
                <Loader2
                  className="h-4 w-4 animate-spin text-orange-600"
                  aria-hidden="true"
                />
              </div>
            )}
          </div>
        </div>
      </div>
    </article>
  );
};
