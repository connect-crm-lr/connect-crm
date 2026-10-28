import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import Login from '../pages/Login';
import Dashboard from '../pages/Dashboard';
import Clientes from '../pages/Clientes';
import Funil from '../pages/Funil';
import Estoque from '../pages/Estoque';
import Catalogo from '../pages/Catalogo';
import NovoPedido from '../pages/NovoPedido';
import HistoricoVendas from '../pages/HistoricoVendas'; // <-- Importação do Histórico de Vendas

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />

        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/login" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="clientes" element={<Clientes />} />
          <Route path="funil" element={<Funil />} />
          <Route path="catalogo" element={<Catalogo />} />
          <Route path="estoque" element={<Estoque />} />
          <Route path="novo-pedido" element={<NovoPedido />} />
          
          {/* Rota do Histórico de Vendas atualizada */}
          <Route path="historico-vendas" element={<HistoricoVendas />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}