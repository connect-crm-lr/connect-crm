// Guarda o login no navegador (token JWT, nome e quando expira).
// localStorage mantém o usuário logado ao recarregar a página; a validade vem da API (8 h por padrão).
const CHAVE = 'connect-crm:sessao';

export function salvarSessao({ token, tipo, expiraEmSegundos, nome }) {
  const sessao = {
    token,
    tipo: tipo || 'Bearer',
    nome,
    // Guardamos o instante de expiração, e não a duração, para conferir depois de recarregar a página.
    expiraEm: Date.now() + expiraEmSegundos * 1000,
  };
  try {
    localStorage.setItem(CHAVE, JSON.stringify(sessao));
  } catch {
    // Navegador sem armazenamento (ex.: modo restrito): o login vale só até recarregar a página.
  }
  return sessao;
}

export function lerSessao() {
  try {
    const sessao = JSON.parse(localStorage.getItem(CHAVE));
    if (!sessao?.token || !(sessao.expiraEm > Date.now())) {
      limparSessao();
      return null;
    }
    return sessao;
  } catch {
    return null;
  }
}

export function limparSessao() {
  try {
    localStorage.removeItem(CHAVE);
  } catch {
    // Sem armazenamento, não há o que limpar.
  }
}
