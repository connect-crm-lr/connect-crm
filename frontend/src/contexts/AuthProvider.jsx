import { useCallback, useEffect, useMemo, useState } from 'react';
import { AuthContext } from './authContext';
import { login } from '../services/auth';
import { EVENTO_SESSAO_EXPIRADA } from '../services/api';
import { lerSessao, limparSessao, salvarSessao } from '../services/sessao';

// Fonte única de "quem está logado" para o sistema todo (menu, rotas protegidas, telas).
export default function AuthProvider({ children }) {
  // Função no useState: lê o localStorage uma vez, na primeira renderização.
  const [sessao, setSessao] = useState(() => lerSessao());

  const entrar = useCallback(async (email, senha) => {
    const resposta = await login(email, senha);
    setSessao(salvarSessao(resposta));
  }, []);

  const sair = useCallback(() => {
    limparSessao();
    setSessao(null);
  }, []);

  // A API recusou o token (401): derruba a sessão e a RotaProtegida leva ao login.
  useEffect(() => {
    const aoExpirar = () => setSessao(null);
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    return () => window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
  }, []);

  // Sem isso, o objeto mudaria a cada renderização e re-renderizaria todos os consumidores à toa.
  const valor = useMemo(
    () => ({ usuario: sessao ? { nome: sessao.nome } : null, autenticado: Boolean(sessao), entrar, sair }),
    [sessao, entrar, sair],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}
