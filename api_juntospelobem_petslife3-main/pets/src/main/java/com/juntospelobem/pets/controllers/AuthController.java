package com.juntospelobem.pets.controllers;

import com.juntospelobem.pets.dtos.OtpRequest;
import com.juntospelobem.pets.dtos.OtpResponse; 
import com.juntospelobem.pets.dtos.OtpValidationRequest;
import com.juntospelobem.pets.dtos.AuthTokenResponse;
import com.juntospelobem.pets.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/solicitar-codigo")
    public ResponseEntity<OtpResponse> solicitarCodigo(@RequestBody @Valid OtpRequest request) {
        
        String emailMascarado = authService.gerarEEnviarCodigo(request.documento());
        
        return ResponseEntity.ok(new OtpResponse(emailMascarado));
    }
    /*sera que vai assim */

    @PostMapping("/validar-codigo")
    public ResponseEntity<AuthTokenResponse> validarCodigo(@RequestBody @Valid OtpValidationRequest request) {
        
        AuthTokenResponse tokenResponse = authService.validarCodigoEGerarToken(
            request.documento(), 
            request.codigo()
        );
        
        return ResponseEntity.ok(tokenResponse);
    }
}