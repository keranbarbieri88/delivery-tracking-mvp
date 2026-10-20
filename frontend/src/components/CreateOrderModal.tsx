import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  MapPin,
  Plus,
  ShoppingBag,
  User as UserIcon,
  X,
} from 'lucide-react';
import { extractApiError } from '../api/client';
import { createOrder } from '../api/orderApi';
import { CreateOrderItemRequest, Order } from '../types/order';
import { Alert } from './Alert';
import { Button } from './Button';
import { Input } from './Input';
import {
  calculateItemSubtotal,
  OrderItemDraft,
  OrderItemInputRow,
  OrderItemRowErrors,
} from './OrderItemInputRow';

export interface CreateOrderModalProps {
  isOpen: boolean;
  defaultClienteNome?: string;
  onClose: () => void;
  onOrderCreated: (createdOrder: Order) => Promise<void> | void;
}

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

let nextDraftId = 1;
function createEmptyItemDraft(): OrderItemDraft {
  const uid = `draft-${Date.now()}-${nextDraftId++}`;
  return {
    uid,
    nome: '',
    quantidade: '1',
    precoUnitario: '',
  };
}

export const CreateOrderModal: React.FC<CreateOrderModalProps> = ({
  isOpen,
  defaultClienteNome = '',
  onClose,
  onOrderCreated,
}) => {
  const dialogRef = useRef<HTMLDialogElement | null>(null);

  const [clienteNome, setClienteNome] = useState(defaultClienteNome);
  const [enderecoEntrega, setEnderecoEntrega] = useState('');
  const [items, setItems] = useState<OrderItemDraft[]>(() => [
    createEmptyItemDraft(),
  ]);

  const [clienteError, setClienteError] = useState<string | undefined>();
  const [enderecoError, setEnderecoError] = useState<string | undefined>();
  const [itemErrors, setItemErrors] = useState<Record<string, OrderItemRowErrors>>(
    {}
  );
  const [apiError, setApiError] = useState<string | null>(null);
  const [apiValidationMap, setApiValidationMap] = useState<Record<string, string>>(
    {}
  );
  const [submitting, setSubmitting] = useState(false);

  const resetForm = useCallback(() => {
    setClienteNome(defaultClienteNome);
    setEnderecoEntrega('');
    setItems([createEmptyItemDraft()]);
    setClienteError(undefined);
    setEnderecoError(undefined);
    setItemErrors({});
    setApiError(null);
    setApiValidationMap({});
    setSubmitting(false);
  }, [defaultClienteNome]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }

    if (isOpen) {
      resetForm();
      if (!dialog.open) {
        dialog.showModal();
      }
    } else if (dialog.open) {
      dialog.close();
    }
  }, [isOpen, resetForm]);

  // Fallback para light-dismiss em navegadores sem suporte nativo a <dialog closedby="any">
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }

    const supportsClosedBy =
      typeof HTMLDialogElement !== 'undefined' &&
      'closedBy' in HTMLDialogElement.prototype;

    if (supportsClosedBy) {
      return;
    }

    const handleBackdropClick = (event: MouseEvent) => {
      if (submitting) {
        return;
      }
      if (event.target !== dialog) {
        return;
      }
      const rect = dialog.getBoundingClientRect();
      const isDialogContent =
        rect.top <= event.clientY &&
        event.clientY <= rect.top + rect.height &&
        rect.left <= event.clientX &&
        event.clientX <= rect.left + rect.width;

      if (!isDialogContent) {
        dialog.close();
      }
    };

    dialog.addEventListener('click', handleBackdropClick);
    return () => {
      dialog.removeEventListener('click', handleBackdropClick);
    };
  }, [submitting]);

  const handleDialogCancel = (event: React.SyntheticEvent<HTMLDialogElement>) => {
    if (submitting) {
      event.preventDefault();
      return;
    }
    onClose();
  };

  const handleDialogClose = () => {
    if (isOpen && !submitting) {
      onClose();
    }
  };

  const handleItemChange = useCallback(
    (uid: string, field: keyof Omit<OrderItemDraft, 'uid'>, value: string) => {
      setItems((prev) =>
        prev.map((item) =>
          item.uid === uid ? { ...item, [field]: value } : item
        )
      );

      setItemErrors((prev) => {
        const existing = prev[uid];
        if (!existing || !existing[field]) {
          return prev;
        }
        return {
          ...prev,
          [uid]: {
            ...existing,
            [field]: undefined,
          },
        };
      });
    },
    []
  );

  const handleAddItem = () => {
    setItems((prev) => [...prev, createEmptyItemDraft()]);
  };

  const handleRemoveItem = useCallback((uid: string) => {
    setItems((prev) => (prev.length > 1 ? prev.filter((i) => i.uid !== uid) : prev));
    setItemErrors((prev) => {
      const next = { ...prev };
      delete next[uid];
      return next;
    });
  }, []);

  const orderTotal = useMemo(
    () => items.reduce((sum, item) => sum + calculateItemSubtotal(item), 0),
    [items]
  );

  const validateForm = (): CreateOrderItemRequest[] | null => {
    let isValid = true;

    if (!clienteNome.trim()) {
      setClienteError('Informe o nome do cliente.');
      isValid = false;
    } else {
      setClienteError(undefined);
    }

    if (!enderecoEntrega.trim()) {
      setEnderecoError('O endereço de entrega é obrigatório.');
      isValid = false;
    } else {
      setEnderecoError(undefined);
    }

    const nextItemErrors: Record<string, OrderItemRowErrors> = {};
    const parsedItems: CreateOrderItemRequest[] = [];

    for (const item of items) {
      const rowErr: OrderItemRowErrors = {};
      const trimmedNome = item.nome.trim();
      const parsedQtd = Number.parseInt(item.quantidade, 10);
      const parsedPrice = Number.parseFloat(item.precoUnitario.replace(',', '.'));

      if (!trimmedNome) {
        rowErr.nome = 'Obrigatório';
        isValid = false;
      }

      if (
        Number.isNaN(parsedQtd) ||
        parsedQtd < 1 ||
        String(parsedQtd) !== item.quantidade.trim()
      ) {
        rowErr.quantidade = 'Mínimo 1 (inteiro)';
        isValid = false;
      }

      if (Number.isNaN(parsedPrice) || parsedPrice < 0.01) {
        rowErr.precoUnitario = 'Maior que R$ 0,00';
        isValid = false;
      }

      if (Object.keys(rowErr).length > 0) {
        nextItemErrors[item.uid] = rowErr;
      } else {
        parsedItems.push({
          nome: trimmedNome,
          quantidade: parsedQtd,
          precoUnitario: Number(parsedPrice.toFixed(2)),
        });
      }
    }

    setItemErrors(nextItemErrors);
    return isValid ? parsedItems : null;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setApiError(null);
    setApiValidationMap({});

    const validItems = validateForm();
    if (!validItems) {
      return;
    }

    setSubmitting(true);
    try {
      const created = await createOrder({
        clienteNome: clienteNome.trim(),
        enderecoEntrega: enderecoEntrega.trim(),
        itens: validItems,
      });

      resetForm();
      onClose();
      await onOrderCreated(created);
    } catch (error: unknown) {
      const parsed = extractApiError(error);
      setApiError(parsed.message);
      setApiValidationMap(parsed.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  const dialogExtraProps: Record<string, string> = {
    closedby: submitting ? 'none' : 'any',
  };

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="create-order-modal-title"
      onCancel={handleDialogCancel}
      onClose={handleDialogClose}
      {...dialogExtraProps}
      className="m-auto w-[calc(100%-1.5rem)] max-w-2xl rounded-2xl border border-slate-200 bg-white p-0 text-slate-900 shadow-2xl open:flex open:max-h-[92dvh] open:flex-col sm:w-full"
    >
      {/* Cabeçalho do Modal */}
      <div className="flex items-center justify-between border-b border-slate-100 px-4 py-4 sm:px-6">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-orange-100 text-orange-600">
            <ShoppingBag className="h-5 w-5" aria-hidden="true" />
          </div>
          <div>
            <h2
              id="create-order-modal-title"
              className="text-base font-bold text-slate-900 sm:text-lg"
            >
              Novo Pedido de Entrega
            </h2>
            <p className="text-xs text-slate-500">
              Preencha o endereço e os itens para registrar o pedido
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          disabled={submitting}
          aria-label="Fechar modal"
          className="rounded-xl p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 focus:outline-none focus:ring-2 focus:ring-orange-500/20 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <X className="h-5 w-5" aria-hidden="true" />
        </button>
      </div>

      {/* Corpo Rolável do Formulário */}
      <form
        onSubmit={handleSubmit}
        noValidate
        className="flex min-h-0 flex-1 flex-col overflow-hidden"
      >
        <div className="flex-1 space-y-5 overflow-y-auto px-4 py-4 sm:px-6 sm:py-5">
          {apiError && (
            <Alert
              variant="error"
              title="Erro ao registrar pedido"
              message={apiError}
              validationErrors={apiValidationMap}
              onClose={() => {
                setApiError(null);
                setApiValidationMap({});
              }}
            />
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              id="order-cliente-nome"
              name="clienteNome"
              label="Nome do Cliente *"
              placeholder="Ex.: Maria Silva"
              required
              disabled={submitting}
              value={clienteNome}
              onChange={(e) => {
                setClienteNome(e.target.value);
                if (clienteError) setClienteError(undefined);
              }}
              error={clienteError}
              leftIcon={<UserIcon className="h-4 w-4" aria-hidden="true" />}
            />

            <Input
              id="order-endereco-entrega"
              name="enderecoEntrega"
              label="Endereço de Entrega *"
              placeholder="Ex.: Av. Paulista, 1000 - Apto 42"
              required
              disabled={submitting}
              value={enderecoEntrega}
              onChange={(e) => {
                setEnderecoEntrega(e.target.value);
                if (enderecoError) setEnderecoError(undefined);
              }}
              error={enderecoError}
              leftIcon={<MapPin className="h-4 w-4" aria-hidden="true" />}
            />
          </div>

          {/* Seção Dinâmica de Itens */}
          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="text-sm font-bold text-slate-800">
                  Itens do Pedido
                </h3>
                <p className="text-xs text-slate-500">
                  Adicione um ou mais produtos com quantidade e preço unitário
                </p>
              </div>

              <Button
                type="button"
                variant="secondary"
                disabled={submitting}
                onClick={handleAddItem}
                leftIcon={<Plus className="h-4 w-4 text-orange-600" aria-hidden="true" />}
                className="!px-3 !py-1.5 text-xs"
              >
                + Adicionar Item
              </Button>
            </div>

            <div className="space-y-3">
              {items.map((item, index) => (
                <OrderItemInputRow
                  key={item.uid}
                  index={index}
                  item={item}
                  errors={itemErrors[item.uid]}
                  canRemove={items.length > 1}
                  disabled={submitting}
                  onChange={handleItemChange}
                  onRemove={handleRemoveItem}
                />
              ))}
            </div>
          </div>
        </div>

        {/* Rodapé Fixo com Resumo Financeiro e Ações */}
        <div className="border-t border-slate-200 bg-slate-50 px-4 py-3.5 sm:px-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-baseline justify-between sm:block">
              <span className=" block text-xs font-medium uppercase tracking-wider text-slate-500">
                Valor Total do Pedido
              </span>
              <span className="text-lg font-extrabold text-orange-600 sm:text-xl">
                {currencyFormatter.format(orderTotal)}
              </span>
            </div>

            <div className="flex items-center justify-end gap-2.5">
              <Button
                type="button"
                variant="secondary"
                disabled={submitting}
                onClick={onClose}
                className="flex-1 sm:flex-initial"
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                variant="primary"
                loading={submitting}
                disabled={submitting}
                className="flex-1 sm:flex-initial"
              >
                {submitting ? 'Salvando...' : 'Salvar Pedido'}
              </Button>
            </div>
          </div>
        </div>
      </form>
    </dialog>
  );
};

