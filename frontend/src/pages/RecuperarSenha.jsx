import { useState } from 'react';
import { Link } from 'react-router-dom';

// Mensagem padrão (AGENTS 6.7): a mesma para qualquer e-mail informado, cadastrado ou não,
// para a tela não revelar quais e-mails existem.
const MENSAGEM_PADRAO = 'Se o e-mail estiver cadastrado, enviaremos o link';
const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Plano B do RF-002 (decisão de 03/11): só a tela e a mensagem padrão. Não há envio real de e-mail
// nem chamada à API; o envio do link e a criação de senha nova ficam para versões futuras.
export default function RecuperarSenha() {
  const [email, setEmail] = useState('');
  const [erroEmail, setErroEmail] = useState('');
  const [mensagem, setMensagem] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    setMensagem('');
    const valor = email.trim();
    if (!valor) {
      setErroEmail('Informe o e-mail');
      return;
    }
    if (!FORMATO_EMAIL.test(valor)) {
      setErroEmail('Informe um e-mail válido');
      return;
    }
    setErroEmail('');
    setMensagem(MENSAGEM_PADRAO);
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.logoBox}>
          <span style={{ color: 'white', fontWeight: 'bold', fontSize: '1.2rem' }}>C</span>
        </div>
        <h1 style={styles.title}>Connect CRM</h1>
        <p style={styles.subtitle}>LR Representações</p>
      </div>

      <div style={styles.card}>
        <h2 style={styles.cardTitle}>Esqueci minha senha</h2>
        <p style={styles.cardSubtitle}>Informe o e-mail da sua conta</p>

        <form onSubmit={handleSubmit} noValidate style={styles.form}>
          <div style={styles.inputGroup}>
            <label htmlFor="recuperar-email" style={styles.label}>E-MAIL</label>
            <input
              id="recuperar-email"
              type="email"
              placeholder="seu@email.com"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setErroEmail(''); setMensagem(''); }}
              style={{ ...styles.input, ...(erroEmail ? styles.inputErro : {}) }}
              aria-invalid={Boolean(erroEmail)}
              aria-describedby={erroEmail ? 'recuperar-email-erro' : undefined}
            />
            {erroEmail && <span id="recuperar-email-erro" role="alert" style={styles.erroCampo}>{erroEmail}</span>}
          </div>

          {mensagem && <p role="status" style={styles.aviso}>{mensagem}</p>}

          <button type="submit" style={styles.submitButton}>Enviar link</button>
        </form>

        <div style={styles.voltar}>
          <Link to="/login" style={styles.link}>Voltar para o login</Link>
        </div>
      </div>

      <p style={styles.footer}>© {new Date().getFullYear()} Connect CRM · LR Representações</p>
    </div>
  );
}

// Mesmo visual da tela de login.
const styles = {
  container: { display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', backgroundColor: '#F9FAFB', fontFamily: 'sans-serif' },
  header: { textAlign: 'center', marginBottom: '2rem' },
  logoBox: { width: '48px', height: '48px', backgroundColor: '#4F46E5', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1rem auto' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  card: { backgroundColor: 'white', padding: '2.5rem', borderRadius: '8px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)', width: '100%', maxWidth: '400px', boxSizing: 'border-box' },
  cardTitle: { fontSize: '1.25rem', fontWeight: '600', color: '#111827', margin: '0 0 0.5rem 0' },
  cardSubtitle: { fontSize: '0.875rem', color: '#6B7280', marginBottom: '1.5rem' },
  form: { display: 'flex', flexDirection: 'column', gap: '1.25rem' },
  inputGroup: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  input: { padding: '0.75rem 1rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '1rem', outline: 'none' },
  inputErro: { borderColor: '#DC2626' },
  erroCampo: { color: '#DC2626', fontSize: '0.75rem' },
  aviso: { backgroundColor: '#ECFDF5', border: '1px solid #A7F3D0', color: '#047857', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.875rem', margin: 0 },
  submitButton: { backgroundColor: '#4F46E5', color: 'white', padding: '0.75rem', borderRadius: '6px', border: 'none', fontSize: '1rem', fontWeight: '500', cursor: 'pointer' },
  voltar: { marginTop: '1.25rem', textAlign: 'center' },
  link: { color: '#4F46E5', fontSize: '0.875rem', textDecoration: 'none' },
  footer: { marginTop: '2rem', fontSize: '0.875rem', color: '#9CA3AF' },
};
