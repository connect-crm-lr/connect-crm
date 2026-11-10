import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import RotaProtegida from '../components/RotaProtegida';
import Login from '../pages/Login';
import Dashboard from '../pages/Dashboard';
import Clientes from '../pages/Clientes';
import Funil from '../pages/Funil';
import Estoque from '../pages/Estoque';
import Catalogo from '../pages/Catalogo';
import NovoPedido from '../pages/NovoPedido';
import HistoricoVendas from '../pages/HistoricoVendas';
import { useAuth } from '../hooks/useAuth';

// Quem já está logado e abre "/" ou "/login" vai direto ao Dashboard; os demais, ao login.
function RedirecionarInicio() {
  const { autenticado } = useAuth();
  return <Navigate to={autenticado ? '/dashboard' : '/login'} replace />;
}

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />

        {/* Tudo dentro de RotaProtegida exige login; sem ele, volta para /login. */}
        <Route element={<RotaProtegida />}>
          <Route path="/" element={<MainLayout />}>
            <Route index element={<RedirecionarInicio />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="clientes" element={<Clientes />} />
            <Route path="funil" element={<Funil />} />
            <Route path="catalogo" element={<Catalogo />} />
            <Route path="estoque" element={<Estoque />} />
            <Route path="novo-pedido" element={<NovoPedido />} />
            <Route path="historico-vendas" element={<HistoricoVendas />} />
          </Route>
        </Route>

        <Route path="*" element={<RedirecionarInicio />} />
      </Routes>
    </BrowserRouter>
  );
}
