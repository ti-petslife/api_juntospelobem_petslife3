package com.juntospelobem.pets.security;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final String secret;
    private final int expirationMinutes;

  public JwtService(
            @Value("${api.security.token.secret}") String secret,
            @Value("${api.security.token.expiration-minutes}") int expirationMinutes) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

 public String gerarToken(String documento) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("Nossa_API_BFF")
                    .withSubject(documento)
                    .withIssuedAt(java.util.Date.from(Instant.now()))
                    .withExpiresAt(java.util.Date.from(gerarDataExpiracao()))
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro interno ao gerar o passaporte digital.", exception);
        }
    }

    private Instant gerarDataExpiracao() {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }
    public String validarTokenEExtrairDocumento(String token) {
        System.out.println("DEBUG: Chave secreta sendo usada: " + this.secret);
        Algorithm algorithm = Algorithm.HMAC256(secret);
        
        return JWT.require(algorithm)
                .withIssuer("Nossa_API_BFF")
                .build()
                .verify(token) 
                .getSubject(); 
    }
}