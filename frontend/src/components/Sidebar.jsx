import { NavLink, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Users, 
  Filter, 
  BookOpen, 
  Package, 
  FilePlus, 
  History, 
  LogOut 
} from 'lucide-react';

export default function Sidebar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    // Futuramente, aqui será limpo o token de sessão
    navigate('/login');
  };

  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/clientes', label: 'Clientes', icon: Users },
    { path: '/funil', label: 'Funil', icon: Filter },
    { path: '/catalogo', label: 'Catálogo', icon: BookOpen },
    { path: '/estoque', label: 'Estoque', icon: Package },
    { path: '/novo-pedido', label: 'Novo Pedido', icon: FilePlus },
    { path: '/historico-vendas', label: 'Histórico de Vendas', icon: History },
    // A tela "Usuários" foi omitida conforme a regra do MVP
  ];

  return (
    <aside style={styles.sidebar}>
      {/* Cabeçalho da Sidebar */}
      <div style={styles.header}>
        <div style={styles.logoBox}>
          <span style={styles.logoText}>C</span>
        </div>
        <span style={styles.brandName}>Connect CRM</span>
      </div>

      {/* Navegação */}
      <nav style={styles.nav}>
        {menuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            style={({ isActive }) => ({
              ...styles.navItem,
              ...(isActive ? styles.navItemActive : {}),
            })}
          >
            <item.icon size={20} style={styles.icon} />
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* Rodapé da Sidebar */}
      <div style={styles.footer}>
        <p style={styles.companyName}>LR REPRESENTAÇÕES</p>
        <button onClick={handleLogout} style={styles.logoutButton}>
          <LogOut size={18} style={styles.icon} />
          Sair
        </button>
      </div>
    </aside>
  );
}

// Estilos in-line da Sidebar
const styles = {
  sidebar: {
    width: '250px',
    backgroundColor: '#312E81', // Azul escuro do protótipo
    color: '#D1D5DB', // Cor do texto inativo (cinzento claro)
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    position: 'sticky',
    top: 0,
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    padding: '1.5rem',
    borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
  },
  logoBox: {
    width: '32px',
    height: '32px',
    backgroundColor: '#4F46E5', // Azul principal mais claro
    borderRadius: '8px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoText: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: '1rem',
  },
  brandName: {
    color: 'white',
    fontWeight: '600',
    fontSize: '1.125rem',
  },
  nav: {
    flex: 1,
    padding: '1rem 0',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.25rem',
  },
  navItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    padding: '0.75rem 1.5rem',
    textDecoration: 'none',
    color: '#D1D5DB', // Cinzento claro
    fontSize: '0.875rem',
    fontWeight: '500',
    transition: 'all 0.2s',
  },
  navItemActive: {
    backgroundColor: '#4F46E5', // Fundo azul quando ativo
    color: 'white',
    borderLeft: '4px solid white', // Destaque na borda esquerda
  },
  icon: {
    minWidth: '20px',
  },
  footer: {
    padding: '1.5rem',
    borderTop: '1px solid rgba(255, 255, 255, 0.1)',
  },
  companyName: {
    fontSize: '0.75rem',
    color: '#9CA3AF',
    marginBottom: '0.75rem',
    letterSpacing: '0.05em',
  },
  logoutButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    background: 'none',
    border: 'none',
    color: '#D1D5DB',
    cursor: 'pointer',
    padding: 0,
    fontSize: '0.875rem',
    fontWeight: '500',
  },
};  