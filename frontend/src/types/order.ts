export type OrderStatus =
  | 'RECEBIDO'
  | 'EM_PREPARO'
  | 'SAIU_PARA_ENTREGA'
  | 'ENTREGUE'
  | 'CANCELADO';

export interface OrderItem {
  id?: string;
  nome: string;
  quantidade: number;
  precoUnitario: number;
  subtotal?: number;
}

export interface Order {
  id: string;
  clienteNome?: string;
  enderecoEntrega: string;
  status: OrderStatus;
  valorTotal: number;
  criadoEm: string;
  itens: OrderItem[];
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
}

export interface CreateOrderItemRequest {
  nome: string;
  quantidade: number;
  precoUnitario: number;
}

export interface CreateOrderRequest {
  clienteNome: string;
  enderecoEntrega: string;
  itens: CreateOrderItemRequest[];
}

export interface RawOrderResponse {
  id: string;
  clienteNome?: string;
  enderecoEntrega: string;
  status: OrderStatus;
  valorTotal?: number;
  total?: number;
  criadoEm?: string;
  createdAt?: string;
  itens?: OrderItem[];
}
