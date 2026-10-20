import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function Login() {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();

  const handleLogin = (e) => {
    e.preventDefault();
    // No futuro, aqui entra a chamada à API do Back-end Java
    // Simulando o login para avançar:
    if (email && password) {
      navigate('/dashboard');
    }
  };

  const handleRecuperarSenha = () => {
    // Regra: "Se o e-mail estiver cadastrado, enviaremos o link" (Mockup)
    alert("Se o e-mail estiver cadastrado, enviaremos o link");
  };

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <div style={styles.logoBox}>
          {/* Substituir pelo logotipo real depois */}
          <span style={{ color: 'white', fontWeight: 'bold', fontSize: '1.2rem' }}>C</span>
        </div>
        <h1 style={styles.title}>Connect CRM</h1>
        <p style={styles.subtitle}>LR Representações</p>
      </div>

      {/* Cartão de Login */}
      <div style={styles.card}>
        <h2 style={styles.cardTitle}>Bem-vindo de volta</h2>
        <p style={styles.cardSubtitle}>Acesse sua conta para continuar</p>

        <form onSubmit={handleLogin} style={styles.form}>
          <div style={styles.inputGroup}>
            <label style={styles.label}>E-MAIL</label>
            <input
              type="email"
              placeholder="seu@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={styles.input}
              required
            />
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>SENHA</label>
            <div style={styles.passwordWrapper}>
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={styles.inputPassword}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={styles.eyeButton}
              >
                {showPassword ? <EyeOff size={20} color="#9CA3AF" /> : <Eye size={20} color="#9CA3AF" />}
              </button>
            </div>
          </div>

          <div style={styles.forgotPasswordContainer}>
            <button
              type="button"
              onClick={handleRecuperarSenha}
              style={styles.forgotPasswordLink}
            >
              Esqueci minha senha
            </button>
          </div>

          <button type="submit" style={styles.submitButton}>
            Entrar
          </button>
        </form>
      </div>

      {/* Rodapé */}
      <p style={styles.footer}>© {new Date().getFullYear()} Connect CRM · LR Representações</p>
    </div>
  );
}

// Estilos in-line (Pode ser passado para CSS ou Tailwind posteriormente)
const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    backgroundColor: '#F9FAFB', // Fundo claro do protótipo
    fontFamily: 'sans-serif',
  },
  header: {
    textAlign: 'center',
    marginBottom: '2rem',
  },
  logoBox: {
    width: '48px',
    height: '48px',
    backgroundColor: '#4F46E5', // Azul principal
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 1rem auto',
  },
  title: {
    fontSize: '1.5rem',
    fontWeight: '600',
    color: '#111827',
    margin: '0 0 0.25rem 0',
  },
  subtitle: {
    fontSize: '0.875rem',
    color: '#6B7280',
    margin: 0,
  },
  card: {
    backgroundColor: 'white',
    padding: '2.5rem',
    borderRadius: '8px',
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
    width: '100%',
    maxWidth: '400px',
  },
  cardTitle: {
    fontSize: '1.25rem',
    fontWeight: '600',
    color: '#111827',
    margin: '0 0 0.5rem 0',
  },
  cardSubtitle: {
    fontSize: '0.875rem',
    color: '#6B7280',
    marginBottom: '1.5rem',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
  },
  inputGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
  },
  label: {
    fontSize: '0.75rem',
    fontWeight: '600',
    color: '#6B7280',
    letterSpacing: '0.05em',
  },
  input: {
    padding: '0.75rem 1rem',
    border: '1px solid #D1D5DB',
    borderRadius: '6px',
    fontSize: '1rem',
    outline: 'none',
  },
  passwordWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
  },
  inputPassword: {
    width: '100%',
    padding: '0.75rem 2.5rem 0.75rem 1rem',
    border: '1px solid #D1D5DB',
    borderRadius: '6px',
    fontSize: '1rem',
    outline: 'none',
  },
  eyeButton: {
    position: 'absolute',
    right: '0.75rem',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    padding: 0,
  },
  forgotPasswordContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: '-0.5rem',
  },
  forgotPasswordLink: {
    background: 'none',
    border: 'none',
    color: '#4F46E5',
    fontSize: '0.875rem',
    cursor: 'pointer',
    padding: 0,
  },
  submitButton: {
    backgroundColor: '#4F46E5',
    color: 'white',
    padding: '0.75rem',
    borderRadius: '6px',
    border: 'none',
    fontSize: '1rem',
    fontWeight: '500',
    cursor: 'pointer',
    marginTop: '0.5rem',
  },
  footer: {
    marginTop: '2rem',
    fontSize: '0.875rem',
    color: '#9CA3AF',
  },
};