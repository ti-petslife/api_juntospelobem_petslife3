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

    public String gerarEEnviarCodigo(String documento) {
        ClienteDados dadosCliente = bitrixClient.buscarDadosClientePorDocumento(documento);
        String codigoOtp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        
        Cache cache = Objects.requireNonNull(cacheManager.getCache("otpCache"));
        
        // O tempo de vida é delegado ao Caffeine configurado no CacheManager
        cache.put(documento, codigoOtp);
        
        emailService.enviarCodigo(dadosCliente.email(), codigoOtp, dadosCliente.id());
        return mascararEmail(dadosCliente.email());
    }

    public AuthTokenResponse validarCodigoEGerarToken(String documento, String codigoDigitado) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache("otpCache"));
        
        // Recupera a String diretamente. Se expirou no cache, retorna null automaticamente.
        String codigoSalvo = cache.get(documento, String.class);

        if (codigoSalvo == null || !codigoSalvo.equals(codigoDigitado)) {
            cache.evict(documento); 
            throw new OtpInvalidoException("Código inválido ou expirado.");
        }

        cache.evict(documento); 
        String tokenJwt = jwtService.gerarToken(documento);
        
        return new AuthTokenResponse(tokenJwt, "Bearer");
    }

    private String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] partes = email.split("@");
        String nome = partes[0];
        if (nome.length() <= 1) return email;
        return nome.charAt(0) + "*".repeat(nome.length() - 1) + "@" + partes[1];
    }
}