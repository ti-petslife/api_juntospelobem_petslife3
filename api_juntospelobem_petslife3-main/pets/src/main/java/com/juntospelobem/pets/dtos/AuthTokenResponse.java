package com.juntospelobem.pets.dtos;

public record AuthTokenResponse(
    String token,
    String tipo
) {}