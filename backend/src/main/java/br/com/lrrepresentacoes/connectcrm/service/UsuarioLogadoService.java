package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.exception.CredenciaisInvalidasException;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Identifica quem está fazendo a requisição. O token JWT leva o e-mail do usuário no
 * "subject" (TokenService), e o Spring Security o expõe como nome da autenticação.
 * Usado para gravar o id_usuario nas tabelas de negócio (AGENTS 5.2).
 */
// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class UsuarioLogadoService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioLogadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Token válido de um usuário que não existe mais é tratado como não autenticado (401).
    public Usuario obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || autenticacao.getName() == null) {
            throw new CredenciaisInvalidasException();
        }
        return usuarioRepository.findByEmailIgnoreCase(autenticacao.getName())
                .orElseThrow(CredenciaisInvalidasException::new);
    }
}
