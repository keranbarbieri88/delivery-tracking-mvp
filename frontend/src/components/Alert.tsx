import React from 'react';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';

export interface AlertProps {
  variant?: 'error' | 'success' | 'info';
  title?: string;
  message: string;
  validationErrors?: Record<string, string>;
  onClose?: () => void;
}

export const Alert: React.FC<AlertProps> = ({
  variant = 'error',
  title,
  message,
  validationErrors,
  onClose,
}) => {
  const styles: Record<NonNullable<AlertProps['variant']>, {
    container: string;
    icon: React.ReactNode;
  }> = {
    error: {
      container: 'border-red-200 bg-red-50 text-red-800',
      icon: <AlertCircle className="h-5 w-5 shrink-0 text-red-600" aria-hidden="true" />,
    },
    success: {
      container: 'border-emerald-200 bg-emerald-50 text-emerald-800',
      icon: <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" aria-hidden="true" />,
    },
    info: {
      container: 'border-blue-200 bg-blue-50 text-blue-800',
      icon: <Info className="h-5 w-5 shrink-0 text-blue-600" aria-hidden="true" />,
    },
  };

  const errorEntries = validationErrors ? Object.entries(validationErrors) : [];

  return (
    <div
      role="alert"
      className={`flex items-start gap-3 rounded-xl border p-3.5 text-sm shadow-sm ${styles[variant].container}`}
    >
      <div className="mt-0.5">{styles[variant].icon}</div>
      <div className="flex-1 space-y-1">
        {title && <p className="font-semibold">{title}</p>}
        <p className="leading-relaxed">{message}</p>
        {errorEntries.length > 0 && (
          <ul className="mt-2 list-inside list-disc space-y-0.5 text-xs font-medium">
            {errorEntries.map(([field, fieldMsg]) => (
              <li key={field}>
                <span className="font-semibold">{field}:</span> {fieldMsg}
              </li>
            ))}
          </ul>
        )}
      </div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          aria-label="Fechar alerta"
          className="rounded-lg p-1 opacity-70 transition-opacity hover:opacity-100 focus:outline-none"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      )}
    </div>
  );
};

