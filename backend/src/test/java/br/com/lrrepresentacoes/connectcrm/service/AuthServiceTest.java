package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.LoginRequest;
import br.com.lrrepresentacoes.connectcrm.dto.LoginResponse;
import br.com.lrrepresentacoes.connectcrm.exception.CredenciaisInvalidasException;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenService tokenService;

    // BCrypt de verdade: o teste confere a senha do mesmo jeito que a aplicação.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;
    private Usuario usuario;

    @BeforeEach
    void configurar() {
        authService = new AuthService(usuarioRepository, passwordEncoder, tokenService);
        usuario = new Usuario("Revendedor LR", "revendedor@lr.com.br", passwordEncoder.encode("senha-correta"));
    }

    @Test
    void login_comSenhaCorreta_devolveOTokenBearer() {
        when(usuarioRepository.findByEmailIgnoreCase("revendedor@lr.com.br")).thenReturn(Optional.of(usuario));
        when(tokenService.gerar(usuario)).thenReturn("token-gerado");
        when(tokenService.validadeEmSegundos()).thenReturn(28800L);

        LoginResponse resposta = authService.login(new LoginRequest("revendedor@lr.com.br", "senha-correta"));

        assertThat(resposta.token()).isEqualTo("token-gerado");
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.nome()).isEqualTo("Revendedor LR");
    }

    @Test
    void login_comSenhaErrada_lancaCredenciaisInvalidasSemGerarToken() {
        when(usuarioRepository.findByEmailIgnoreCase("revendedor@lr.com.br")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("revendedor@lr.com.br", "senha-errada")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos");
        verify(tokenService, never()).gerar(any());
    }

    @Test
    void login_comEmailInexistente_daAMesmaMensagemDaSenhaErrada() {
        when(usuarioRepository.findByEmailIgnoreCase("ninguem@lr.com.br")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@lr.com.br", "qualquer")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos");
    }

    @Test
    void login_removeEspacosDoEmail() {
        when(usuarioRepository.findByEmailIgnoreCase("revendedor@lr.com.br")).thenReturn(Optional.of(usuario));
        when(tokenService.gerar(usuario)).thenReturn("token-gerado");

        authService.login(new LoginRequest("  revendedor@lr.com.br  ", "senha-correta"));

        verify(usuarioRepository).findByEmailIgnoreCase("revendedor@lr.com.br");
    }
}
