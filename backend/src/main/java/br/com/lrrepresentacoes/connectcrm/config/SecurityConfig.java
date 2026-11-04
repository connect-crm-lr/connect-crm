package br.com.lrrepresentacoes.connectcrm.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Segurança da API com JWT (decisão da equipe em 03/11):
 * - /auth/** é liberado (é onde se faz o login);
 * - todo o resto exige o cabeçalho "Authorization: Bearer <token>";
 * - sem sessão no servidor (stateless): cada requisição se identifica pelo token.
 */
@Configuration
public class SecurityConfig {

    private static final int TAMANHO_MINIMO_CHAVE = 32; // 256 bits, exigência do HS256

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RespostaNaoAutenticado respostaNaoAutenticado)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sem cookie de sessão: CSRF não se aplica
                .cors(Customizer.withDefaults()) // usa o CorsConfigurationSource do CorsConfig
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(jwt -> jwt
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(respostaNaoAutenticado))
                .exceptionHandling(erros -> erros.authenticationEntryPoint(respostaNaoAutenticado));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey chaveJwt(@Value("${app.jwt.secret}") String segredo) {
        if (segredo == null || segredo.length() < TAMANHO_MINIMO_CHAVE) {
            throw new IllegalStateException(
                    "JWT_SECRET precisa ter pelo menos " + TAMANHO_MINIMO_CHAVE + " caracteres (database/.env)");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
