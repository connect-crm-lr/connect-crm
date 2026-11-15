import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Login() {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [errosCampos, setErrosCampos] = useState({});
  const [aviso, setAviso] = useState('');
  const { autenticado, entrar } = useAuth();
  const local = useLocation();

  // Já logado (ou login recém-feito): vai para a página que o usuário tentou abrir, ou para o Dashboard.
  if (autenticado) {
    return <Navigate to={local.state?.de || '/dashboard'} replace />;
  }

  const handleLogin = async (e) => {
    e.preventDefault();
    setErro('');
    setErrosCampos({});
    setAviso('');
    setEnviando(true);
    try {
      await entrar(email, password);
      // Sem navigate aqui: ao logar, "autenticado" vira true e o redirecionamento acima acontece.
    } catch (falha) {
      if (falha.detalhes?.campos?.length) {
        // Erro de validação (400): cada mensagem aparece abaixo do campo correspondente.
        setErrosCampos(Object.fromEntries(falha.detalhes.campos.map((c) => [c.campo, c.mensagem])));
      } else if (falha.status) {
        // 401 traz "e-mail ou senha incorretos"; mostramos a mensagem da API sem dizer qual campo errou.
        setErro(falha.message);
      } else {
        setErro('Não foi possível falar com o servidor. Verifique sua conexão e tente de novo.');
      }
      setEnviando(false);
    }
  };

  const handleRecuperarSenha = () => {
    // Escopo do RF-002 ainda não decidido: por ora, só a mensagem padrão (não envia e-mail).
    setErro('');
    setAviso('Se o e-mail estiver cadastrado, enviaremos o link');
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

        <form onSubmit={handleLogin} style={styles.form} noValidate>
          <div style={styles.inputGroup}>
            <label htmlFor="login-email" style={styles.label}>E-MAIL</label>
            <input
              id="login-email"
              type="email"
              autoComplete="username"
              placeholder="seu@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={{ ...styles.input, ...(errosCampos.email ? styles.inputErro : {}) }}
              aria-invalid={Boolean(errosCampos.email)}
              aria-describedby={errosCampos.email ? 'login-email-erro' : undefined}
              required
            />
            {errosCampos.email && <span id="login-email-erro" style={styles.erroCampo}>{errosCampos.email}</span>}
          </div>

          <div style={styles.inputGroup}>
            <label htmlFor="login-senha" style={styles.label}>SENHA</label>
            <div style={styles.passwordWrapper}>
              <input
                id="login-senha"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{ ...styles.inputPassword, ...(errosCampos.senha ? styles.inputErro : {}) }}
                aria-invalid={Boolean(errosCampos.senha)}
                aria-describedby={errosCampos.senha ? 'login-senha-erro' : undefined}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={styles.eyeButton}
                aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
              >
                {showPassword ? <EyeOff size={20} color="#9CA3AF" /> : <Eye size={20} color="#9CA3AF" />}
              </button>
            </div>
            {errosCampos.senha && <span id="login-senha-erro" style={styles.erroCampo}>{errosCampos.senha}</span>}
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

          {/* Mensagem geral (credenciais incorretas, API fora do ar) ou aviso da recuperação de senha */}
          {erro && <p role="alert" style={styles.erroGeral}>{erro}</p>}
          {aviso && <p role="status" style={styles.aviso}>{aviso}</p>}

          <button
            type="submit"
            disabled={enviando}
            style={{ ...styles.submitButton, ...(enviando ? styles.submitButtonDesabilitado : {}) }}
          >
            {enviando ? 'Entrando...' : 'Entrar'}
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
  // Mesma propriedade "border" dos campos (e não borderColor), para o React não misturar atalho e propriedade solta.
  inputErro: {
    border: '1px solid #DC2626',
  },
  erroCampo: {
    fontSize: '0.8125rem',
    color: '#B91C1C',
  },
  erroGeral: {
    margin: 0,
    padding: '0.75rem 1rem',
    backgroundColor: '#FEF2F2',
    border: '1px solid #FECACA',
    borderRadius: '6px',
    color: '#B91C1C',
    fontSize: '0.875rem',
  },
  aviso: {
    margin: 0,
    padding: '0.75rem 1rem',
    backgroundColor: '#EEF2FF',
    border: '1px solid #C7D2FE',
    borderRadius: '6px',
    color: '#3730A3',
    fontSize: '0.875rem',
  },
  submitButtonDesabilitado: {
    opacity: 0.6,
    cursor: 'not-allowed',
  },
};