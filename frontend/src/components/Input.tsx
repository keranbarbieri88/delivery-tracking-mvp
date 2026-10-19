import React, { useState } from 'react';
import { AlertCircle, Eye, EyeOff } from 'lucide-react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  id: string;
  label: string;
  hint?: string;
  error?: string;
  leftIcon?: React.ReactNode;
  enablePasswordToggle?: boolean;
}

export const Input: React.FC<InputProps> = ({
  id,
  label,
  hint,
  error,
  leftIcon,
  enablePasswordToggle = false,
  type = 'text',
  className = '',
  onBlur,
  onInput,
  ...rest
}) => {
  const [showPassword, setShowPassword] = useState(false);
  const [hasInteracted, setHasInteracted] = useState(false);

  const isPasswordField = type === 'password' && enablePasswordToggle;
  const resolvedType = isPasswordField ? (showPassword ? 'text' : 'password') : type;

  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = `${id}-error`;
  const describedBy = [hintId, error ? errorId : undefined].filter(Boolean).join(' ') || undefined;

  const handleBlur = (event: React.FocusEvent<HTMLInputElement>) => {
    setHasInteracted(true);
    const input = event.currentTarget;
    const isNativeInvalid =
      typeof input.matches === 'function' && input.matches(':user-invalid');
    const isInvalid = Boolean(error) || isNativeInvalid || !input.checkValidity();
    input.setAttribute('aria-invalid', isInvalid ? 'true' : 'false');
    input.classList.toggle('user-invalid-fallback', isInvalid);
    input.classList.toggle('user-valid-fallback', !isInvalid && Boolean(input.value));
    onBlur?.(event);
  };

  const handleInput = (event: React.FormEvent<HTMLInputElement>) => {
    const input = event.currentTarget;
    if (hasInteracted || input.hasAttribute('aria-invalid')) {
      const isInvalid = Boolean(error) || !input.checkValidity();
      input.setAttribute('aria-invalid', isInvalid ? 'true' : 'false');
      input.classList.toggle('user-invalid-fallback', isInvalid);
      input.classList.toggle('user-valid-fallback', !isInvalid && Boolean(input.value));
    }
    onInput?.(event);
  };

  return (
    <div className="field w-full space-y-1.5">
      <label
        htmlFor={id}
        className="block text-sm font-medium text-slate-700"
      >
        {label}
      </label>

      {hint && (
        <span id={hintId} className="hint block text-xs text-slate-500">
          {hint}
        </span>
      )}

      <div className="relative">
        {leftIcon && (
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">
            {leftIcon}
          </div>
        )}

        <input
          id={id}
          type={resolvedType}
          aria-describedby={describedBy}
          aria-errormessage={error ? errorId : undefined}
          aria-invalid={error ? 'true' : undefined}
          onBlur={handleBlur}
          onInput={handleInput}
          className={`block w-full rounded-xl border bg-white py-2.5 text-sm text-slate-900 shadow-sm transition-colors placeholder:text-slate-400 focus:outline-none focus:ring-2 ${
            leftIcon ? 'pl-10' : 'pl-3.5'
          } ${isPasswordField ? 'pr-11' : 'pr-3.5'} ${
            error
              ? 'border-red-500 bg-red-50/40 focus:border-red-500 focus:ring-red-500/20'
              : 'border-slate-300 focus:border-orange-500 focus:ring-orange-500/20'
          } ${className}`}
          {...rest}
        />

        {isPasswordField && (
          <button
            type="button"
            onClick={() => setShowPassword((prev) => !prev)}
            aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
            className="absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 hover:text-slate-600 focus:outline-none"
          >
            {showPassword ? (
              <EyeOff className="h-4 w-4" aria-hidden="true" />
            ) : (
              <Eye className="h-4 w-4" aria-hidden="true" />
            )}
          </button>
        )}
      </div>

      {error && (
        <div
          id={errorId}
          role="alert"
          className="flex items-center gap-1.5 text-xs font-medium text-red-600"
        >
          <AlertCircle className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
};

