import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar'; // <-- Importa a nova Sidebar

export default function MainLayout() {
  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#F3F4F6' }}>
      {/* Insere o componente Sidebar real */}
      <Sidebar />
      
      {/* Área principal */}
      <main style={{ flex: 1, padding: '2rem', overflowY: 'auto' }}>
        <Outlet />
      </main>
    </div>
  );
}