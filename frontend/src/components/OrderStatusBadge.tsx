import React from 'react';
import {
  CheckCircle2,
  ChefHat,
  Clock,
  Truck,
  XCircle,
} from 'lucide-react';
import { OrderStatus } from '../types/order';

interface StatusStyleConfig {
  label: string;
  classes: string;
  icon: React.ReactNode;
}

const STATUS_CONFIG: Record<OrderStatus, StatusStyleConfig> = {
  RECEBIDO: {
    label: 'Recebido',
    classes: 'bg-blue-100 text-blue-800 ring-1 ring-inset ring-blue-600/20',
    icon: <Clock className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />,
  },
  EM_PREPARO: {
    label: 'Em Preparo',
    classes: 'bg-amber-100 text-amber-800 ring-1 ring-inset ring-amber-600/20',
    icon: <ChefHat className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />,
  },
  SAIU_PARA_ENTREGA: {
    label: 'Saiu Para Entrega',
    classes: 'bg-purple-100 text-purple-800 ring-1 ring-inset ring-purple-600/20',
    icon: <Truck className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />,
  },
  ENTREGUE: {
    label: 'Entregue',
    classes: 'bg-emerald-100 text-emerald-800 ring-1 ring-inset ring-emerald-600/20',
    icon: <CheckCircle2 className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />,
  },
  CANCELADO: {
    label: 'Cancelado',
    classes: 'bg-rose-100 text-rose-800 ring-1 ring-inset ring-rose-600/20',
    icon: <XCircle className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />,
  },
};

export interface OrderStatusBadgeProps {
  status: OrderStatus;
  className?: string;
}

export const OrderStatusBadge: React.FC<OrderStatusBadgeProps> = ({
  status,
  className = '',
}) => {
  const config = STATUS_CONFIG[status] ?? STATUS_CONFIG.RECEBIDO;

  return (
    <span
      role="status"
      aria-label={`Status do pedido: ${config.label}`}
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold tracking-wide ${config.classes} ${className}`}
    >
      {config.icon}
      <span>{config.label}</span>
    </span>
  );
};
