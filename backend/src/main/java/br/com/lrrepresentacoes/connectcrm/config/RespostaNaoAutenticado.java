package br.com.lrrepresentacoes.connectcrm.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Resposta 401 quando a requisição chega sem token ou com token inválido/expirado.
 * Esse erro acontece no filtro do Spring Security, antes do controller, por isso não passa
 * pelo GlobalExceptionHandler: o JSON é montado aqui, no mesmo formato do ErroResponse.
 */
@Component
public class RespostaNaoAutenticado implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excecao) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String caminho = request.getRequestURI().replace("\"", "");
        response.getWriter().write("""
                {"status":401,"erro":"Unauthorized","mensagem":"Faça login para continuar",\
                "caminho":"%s","dataHora":"%s","campos":[]}""".formatted(caminho, LocalDateTime.now()));
    }
}
