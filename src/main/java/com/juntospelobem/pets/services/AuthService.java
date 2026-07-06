package com.juntospelobem.pets.services;

import com.juntospelobem.pets.dtos.AuthTokenResponse;
import com.juntospelobem.pets.dtos.ClienteDados;
import com.juntospelobem.pets.exceptions.OtpInvalidoException;
import com.juntospelobem.pets.clients.BitrixClient;
import com.juntospelobem.pets.security.JwtService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final BitrixClient bitrixClient;
    private final CacheManager cacheManager;
    private final EmailService emailService;
    private final JwtService jwtService;

    public AuthService(BitrixClient bitrixClient, CacheManager cacheManager, 
                       EmailService emailService, JwtService jwtService) {
        this.bitrixClient = bitrixClient;
        this.cacheManager = cacheManager;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

   public void gerarEEnviarCodigo(String documento) {
        
        ClienteDados dadosCliente = bitrixClient.buscarDadosClientePorDocumento(documento);

        String codigoOtp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        Cache cache = Objects.requireNonNull(cacheManager.getCache("otpCache"));
        cache.put(documento, codigoOtp);

        // 2. Passamos os dados extraídos do pacote para o EmailService
        emailService.enviarCodigo(dadosCliente.email(), codigoOtp, dadosCliente.id());
    }

    public AuthTokenResponse validarCodigoEGerarToken(String documento, String codigoDigitado) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache("otpCache"));
        
        String codigoSalvo = cache.get(documento, String.class);

        if (codigoSalvo == null || !codigoSalvo.equals(codigoDigitado)) {
            throw new OtpInvalidoException("Código inválido ou expirado.");
        }

        cache.evict(documento);

        String tokenJwt = jwtService.gerarToken(documento);
        return new AuthTokenResponse(tokenJwt, "Bearer");
    }
}