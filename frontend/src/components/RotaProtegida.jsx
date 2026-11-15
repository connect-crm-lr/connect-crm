import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

// Barra o acesso às telas internas sem login. Guarda a página pedida em "de" para voltar a ela depois de entrar.
export default function RotaProtegida() {
  const { autenticado } = useAuth();
  const local = useLocation();

  if (!autenticado) {
    return <Navigate to="/login" replace state={{ de: local.pathname }} />;
  }
  return <Outlet />;
}
