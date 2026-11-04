// Endereço da API. No build de produção ele vem da variável VITE_API_URL,
// definida no serviço que publica o front (ver docs/deploy.md).
// O Vite grava o valor no código na hora do build: mudar a variável exige um build novo.
// Em desenvolvimento, sem a variável, usa o back-end local.
export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '');

/**
 * Chama a API e devolve o JSON da resposta.
 * Em erro, lança um Error com a "mensagem" do formato padrão da API (ErroResponse)
 * e anexa o corpo completo em "erro.detalhes" (ex.: lista de campos inválidos).
 */
export async function chamarApi(caminho, opcoes = {}) {
  const resposta = await fetch(`${API_URL}${caminho}`, {
    ...opcoes,
    headers: {
      'Content-Type': 'application/json',
      ...opcoes.headers,
    },
  });

  if (resposta.status === 204) {
    return null;
  }

  const corpo = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    const erro = new Error(corpo?.mensagem || 'Não foi possível falar com o servidor');
    erro.detalhes = corpo;
    throw erro;
  }
  return corpo;
}
