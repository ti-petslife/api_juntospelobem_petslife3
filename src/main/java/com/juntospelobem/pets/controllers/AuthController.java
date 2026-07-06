package com.juntospelobem.pets.controllers;


import com.juntospelobem.pets.dtos.OtpRequest;
import com.juntospelobem.pets.dtos.OtpValidationRequest;
import com.juntospelobem.pets.dtos.AuthTokenResponse;
import com.juntospelobem.pets.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "https://juntospelobem.petslife.vet.br"}, allowCredentials = "true")
public class AuthController {

    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/solicitar-codigo")
    public ResponseEntity<Void> solicitarCodigo(@RequestBody @Valid OtpRequest request) {
        
        authService.gerarEEnviarCodigo(request.documento());
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/validar-codigo")
    public ResponseEntity<AuthTokenResponse> validarCodigo(@RequestBody @Valid OtpValidationRequest request) {
        
        AuthTokenResponse tokenResponse = authService.validarCodigoEGerarToken(
            request.documento(), 
            request.codigo()
        );
        
        return ResponseEntity.ok(tokenResponse);
    }
}