import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Filter,
  BookOpen,
  Package,
  FilePlus,
  History,
  LogOut,
} from 'lucide-react';

import logoImg from '../assets/logo-nova.png';

// Mesma cor de fundo da logo, para ela se fundir com a sidebar
const SIDEBAR_BG = '#01061C';

export default function Sidebar() {
  const navigate = useNavigate();

  const handleLogout = () => {
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
  ];

  return (
    <aside style={styles.sidebar}>
      <div style={styles.header}>
        <img src={logoImg} alt="Connect CRM Logo" style={styles.logoImage} />
      </div>

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

const styles = {
  sidebar: {
    width: '250px',
    backgroundColor: SIDEBAR_BG,
    color: '#D1D5DB',
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    position: 'sticky',
    top: 0,
  },
  header: {
  width: '100%',
  height: '190px',          // altura da área marcada no print
  padding: 0,               // sem espaçamento, a logo ocupa tudo
  overflow: 'hidden',
  backgroundColor: SIDEBAR_BG,
  borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
},
  logoImage: {
  display: 'block',
  width: '100%',
  height: '100%',
  objectFit: 'cover',       // preenche todo o espaço sem distorcer
  objectPosition: 'center', // mantém o centro (onde fica a logo)
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
    color: '#D1D5DB',
    fontSize: '0.875rem',
    fontWeight: '500',
    borderLeft: '4px solid transparent', // evita "pulo" do texto ao ativar o item
    transition: 'all 0.2s',
  },
  navItemActive: {
  backgroundColor: '#1E3A8A', // azul que combina com o #01061C
  color: 'white',
  borderLeft: '4px solid #60A5FA', // detalhe azul claro no lugar do branco
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