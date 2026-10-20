export type OrderStatus =
  | 'RECEBIDO'
  | 'EM_PREPARO'
  | 'DESPACHADO'
  | 'ENVIADO'
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

