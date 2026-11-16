import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import FundoParticulas from '../components/FundoParticulas';
import LogoConnect from '../components/LogoConnect';
import './Login.css';

export default function Login() {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [errosCampos, setErrosCampos] = useState({});
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

  return (
    <div className="login-pagina">
      <FundoParticulas className="login-fundo" />

      <main className="login-conteudo">
        {/* Cabeçalho */}
        <header className="login-cabecalho">
          <div className="login-logo">
            <LogoConnect tamanho={42} />
          </div>
          <h1 className="login-titulo">Connect CRM</h1>
          <p className="login-subtitulo">LR Representações</p>
        </header>

        {/* Cartão de login */}
        <section className="login-card">
          <h2 className="login-card-titulo">Bem-vindo de volta</h2>
          <p className="login-card-subtitulo">Acesse sua conta para continuar</p>

        <form onSubmit={handleLogin} className="login-form" noValidate>
          <div className="login-campo">
            <label htmlFor="login-email" className="login-label">E-MAIL</label>
            <input
              id="login-email"
              type="email"
              autoComplete="username"
              placeholder="seu@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={`login-input${errosCampos.email ? ' login-input--erro' : ''}`}
              aria-invalid={Boolean(errosCampos.email)}
              aria-describedby={errosCampos.email ? 'login-email-erro' : undefined}
              required
            />
            {errosCampos.email && <span id="login-email-erro" className="login-erro-campo">{errosCampos.email}</span>}
          </div>

          <div className="login-campo">
            <label htmlFor="login-senha" className="login-label">SENHA</label>
            <div className="login-senha">
              <input
                id="login-senha"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={`login-input${errosCampos.senha ? ' login-input--erro' : ''}`}
                aria-invalid={Boolean(errosCampos.senha)}
                aria-describedby={errosCampos.senha ? 'login-senha-erro' : undefined}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="login-olho"
                aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {errosCampos.senha && <span id="login-senha-erro" className="login-erro-campo">{errosCampos.senha}</span>}
          </div>

          <Link to="/recuperar-senha" className="login-esqueci">Esqueci minha senha</Link>

          {/* Mensagem geral (credenciais incorretas, API fora do ar) */}
          {erro && <p role="alert" className="login-alerta">{erro}</p>}

          <button type="submit" disabled={enviando} className="login-botao">
            {enviando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
        </section>

        {/* Rodapé */}
        <p className="login-rodape">© {new Date().getFullYear()} Connect CRM · LR Representações</p>
      </main>
    </div>
  );
}
