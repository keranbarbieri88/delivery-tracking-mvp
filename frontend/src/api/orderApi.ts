import { apiClient } from './client';
import {
  Order,
  OrderItem,
  OrderStatus,
  RawOrderResponse,
  UpdateOrderStatusRequest,
} from '../types/order';

function normalizeOrderItem(item: OrderItem): OrderItem {
  const quantidade = Number(item.quantidade) || 0;
  const precoUnitario = Number(item.precoUnitario) || 0;
  const subtotal =
    item.subtotal !== undefined
      ? Number(item.subtotal)
      : quantidade * precoUnitario;

  return {
    id: item.id,
    nome: item.nome,
    quantidade,
    precoUnitario,
    subtotal,
  };
}

export function normalizeOrder(raw: RawOrderResponse): Order {
  const itens = Array.isArray(raw.itens) ? raw.itens.map(normalizeOrderItem) : [];
  const calculatedTotal = itens.reduce(
    (sum, item) => sum + (item.subtotal ?? item.quantidade * item.precoUnitario),
    0
  );

  const valorTotal =
    raw.valorTotal !== undefined
      ? Number(raw.valorTotal)
      : raw.total !== undefined
      ? Number(raw.total)
      : calculatedTotal;

  const criadoEm = raw.criadoEm || raw.createdAt || new Date().toISOString();

  return {
    id: raw.id,
    clienteNome: raw.clienteNome,
    enderecoEntrega: raw.enderecoEntrega,
    status: raw.status,
    valorTotal,
    criadoEm,
    itens,
  };
}

function toBackendStatus(status: OrderStatus): OrderStatus {
  if (status === 'DESPACHADO') {
    return 'ENVIADO';
  }
  return status;
}

export async function listOrders(): Promise<Order[]> {
  const response = await apiClient.get<RawOrderResponse[]>('/pedidos');
  return Array.isArray(response.data) ? response.data.map(normalizeOrder) : [];
}

export async function getOrderById(id: string): Promise<Order> {
  const response = await apiClient.get<RawOrderResponse>(`/pedidos/${id}`);
  return normalizeOrder(response.data);
}

export async function updateOrderStatus(
  id: string,
  status: OrderStatus
): Promise<Order> {
  const payload: UpdateOrderStatusRequest = {
    status: toBackendStatus(status),
  };
  const response = await apiClient.patch<RawOrderResponse>(
    `/pedidos/${id}/status`,
    payload
  );
  return normalizeOrder(response.data);
}

