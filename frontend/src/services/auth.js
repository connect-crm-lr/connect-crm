import { chamarApi } from './api';

// POST /auth/login. A API devolve { token, tipo, expiraEmSegundos, nome }.
// E-mail inexistente e senha errada voltam como o mesmo 401, de propósito (não revela qual campo errou).
export function login(email, senha) {
  return chamarApi('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email: email.trim(), senha }),
  });
}
