package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Gera o token JWT do usuário logado. O token leva o e-mail (subject), o nome e a
 * data de expiração, assinados com a chave secreta (HS256). Nunca leva a senha.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final Duration validade;

    public TokenService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiracao-horas}") long expiracaoHoras) {
        this.jwtEncoder = jwtEncoder;
        this.validade = Duration.ofHours(expiracaoHoras);
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("connect-crm")
                .subject(usuario.getEmail())
                .claim("nome", usuario.getNome())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }

    public long validadeEmSegundos() {
        return validade.toSeconds();
    }
}
