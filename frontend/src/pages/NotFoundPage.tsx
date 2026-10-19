import React from 'react';
import { ArrowLeft, Compass } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export const NotFoundPage: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-4 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-100 text-orange-600">
        <Compass className="h-7 w-7" aria-hidden="true" />
      </div>
      <p className="mt-4 text-sm font-bold uppercase tracking-wider text-orange-600">
        Erro 404
      </p>
      <h1 className="mt-1 text-2xl font-bold text-slate-900 sm:text-3xl">
        Página não encontrada
      </h1>
      <p className="mt-2 max-w-md text-sm text-slate-600">
        O endereço que você tentou acessar não existe ou foi movido.
      </p>
      <Link
        to={isAuthenticated ? '/pedidos' : '/login'}
        className="mt-6 inline-flex items-center gap-2 rounded-xl bg-orange-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-orange-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        <span>{isAuthenticated ? 'Voltar para Pedidos' : 'Ir para o Login'}</span>
      </Link>
    </div>
  );
};

