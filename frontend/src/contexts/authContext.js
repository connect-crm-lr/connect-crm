import { createContext } from 'react';

// Contexto separado do AuthProvider para o lint (react-refresh) aceitar: componente e contexto em arquivos distintos.
export const AuthContext = createContext(null);
