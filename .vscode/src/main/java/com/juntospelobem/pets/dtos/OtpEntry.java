package com.juntospelobem.pets.dtos;

import java.time.Duration;
import java.time.Instant;

public record OtpEntry(String codigo, Instant criadoEm) {

    public static OtpEntry criar(String codigo) {
        return new OtpEntry(codigo, Instant.now());
    }

    public boolean estaValido(Duration tempoLimite) {
        return Instant.now().isBefore(criadoEm.plus(tempoLimite));
    }
}