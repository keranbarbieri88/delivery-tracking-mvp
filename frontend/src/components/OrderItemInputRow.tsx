import React from 'react';
import { AlertCircle, Trash2 } from 'lucide-react';

export interface OrderItemDraft {
  uid: string;
  nome: string;
  quantidade: string;
  precoUnitario: string;
}

export interface OrderItemRowErrors {
  nome?: string;
  quantidade?: string;
  precoUnitario?: string;
}

export interface OrderItemInputRowProps {
  index: number;
  item: OrderItemDraft;
  errors?: OrderItemRowErrors;
  canRemove: boolean;
  disabled?: boolean;
  onChange: (uid: string, field: keyof Omit<OrderItemDraft, 'uid'>, value: string) => void;
  onRemove: (uid: string) => void;
}

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

export function calculateItemSubtotal(item: OrderItemDraft): number {
  const qty = Number.parseInt(item.quantidade, 10);
  const price = Number.parseFloat(item.precoUnitario.replace(',', '.'));
  if (Number.isNaN(qty) || qty <= 0 || Number.isNaN(price) || price <= 0) {
    return 0;
  }
  return qty * price;
}

export const OrderItemInputRow: React.FC<OrderItemInputRowProps> = ({
  index,
  item,
  errors,
  canRemove,
  disabled = false,
  onChange,
  onRemove,
}) => {
  const subtotal = calculateItemSubtotal(item);
  const nomeId = `item-nome-${item.uid}`;
  const qtdId = `item-qtd-${item.uid}`;
  const precoId = `item-preco-${item.uid}`;

  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3.5 transition-colors hover:border-slate-300">
      <div className="mb-2.5 flex items-center justify-between gap-2">
        <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
          Item #{index + 1}
        </span>
        <div className="flex items-center gap-3">
          <span className="text-xs font-semibold text-slate-600">
            Subtotal:{' '}
            <strong className="font-bold text-slate-900">
              {currencyFormatter.format(subtotal)}
            </strong>
          </span>
          <button
            type="button"
            onClick={() => onRemove(item.uid)}
            disabled={!canRemove || disabled}
            title={
              canRemove
                ? `Remover item #${index + 1}`
                : 'O pedido deve possuir ao menos 1 item'
            }
            aria-label={`Remover item ${index + 1}`}
            className="inline-flex items-center justify-center rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-600 focus:outline-none focus:ring-2 focus:ring-red-500/20 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-slate-400"
          >
            <Trash2 className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-12">
        {/* Nome do Item */}
        <div className="sm:col-span-6">
          <label
            htmlFor={nomeId}
            className="mb-1 block text-xs font-medium text-slate-700"
          >
            Descrição do Item *
          </label>
          <input
            id={nomeId}
            type="text"
            required
            disabled={disabled}
            placeholder="Ex.: Pizza Calabresa Grande"
            value={item.nome}
            onChange={(e) => onChange(item.uid, 'nome', e.target.value)}
            aria-invalid={errors?.nome ? 'true' : undefined}
            className={`block w-full rounded-xl border bg-white px-3 py-2 text-xs text-slate-900 shadow-sm transition-colors placeholder:text-slate-400 focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:bg-slate-100 sm:text-sm ${
              errors?.nome
                ? 'border-red-500 bg-red-50/40 focus:border-red-500 focus:ring-red-500/20'
                : 'border-slate-300 focus:border-orange-500 focus:ring-orange-500/20'
            }`}
          />
          {errors?.nome && (
            <p className="mt-1 flex items-center gap-1 text-xs font-medium text-red-600">
              <AlertCircle className="h-3 w-3 shrink-0" aria-hidden="true" />
              <span>{errors.nome}</span>
            </p>
          )}
        </div>

        {/* Quantidade */}
        <div className="sm:col-span-3">
          <label
            htmlFor={qtdId}
            className="mb-1 block text-xs font-medium text-slate-700"
          >
            Qtd. *
          </label>
          <input
            id={qtdId}
            type="number"
            inputMode="numeric"
            min={1}
            step={1}
            required
            disabled={disabled}
            placeholder="1"
            value={item.quantidade}
            onChange={(e) => onChange(item.uid, 'quantidade', e.target.value)}
            aria-invalid={errors?.quantidade ? 'true' : undefined}
            className={`block w-full rounded-xl border bg-white px-3 py-2 text-xs text-slate-900 shadow-sm transition-colors placeholder:text-slate-400 focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:bg-slate-100 sm:text-sm ${
              errors?.quantidade
                ? 'border-red-500 bg-red-50/40 focus:border-red-500 focus:ring-red-500/20'
                : 'border-slate-300 focus:border-orange-500 focus:ring-orange-500/20'
            }`}
          />
          {errors?.quantidade && (
            <p className="mt-1 flex items-center gap-1 text-xs font-medium text-red-600">
              <AlertCircle className="h-3 w-3 shrink-0" aria-hidden="true" />
              <span>{errors.quantidade}</span>
            </p>
          )}
        </div>

        {/* Preço Unitário */}
        <div className="sm:col-span-3">
          <label
            htmlFor={precoId}
            className="mb-1 block text-xs font-medium text-slate-700"
          >
            Preço Unit. (R$) *
          </label>
          <input
            id={precoId}
            type="number"
            inputMode="decimal"
            min="0.01"
            step="0.01"
            required
            disabled={disabled}
            placeholder="0,00"
            value={item.precoUnitario}
            onChange={(e) => onChange(item.uid, 'precoUnitario', e.target.value)}
            aria-invalid={errors?.precoUnitario ? 'true' : undefined}
            className={`block w-full rounded-xl border bg-white px-3 py-2 text-xs text-slate-900 shadow-sm transition-colors placeholder:text-slate-400 focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:bg-slate-100 sm:text-sm ${
              errors?.precoUnitario
                ? 'border-red-500 bg-red-50/40 focus:border-red-500 focus:ring-red-500/20'
                : 'border-slate-300 focus:border-orange-500 focus:ring-orange-500/20'
            }`}
          />
          {errors?.precoUnitario && (
            <p className="mt-1 flex items-center gap-1 text-xs font-medium text-red-600">
              <AlertCircle className="h-3 w-3 shrink-0" aria-hidden="true" />
              <span>{errors.precoUnitario}</span>
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

