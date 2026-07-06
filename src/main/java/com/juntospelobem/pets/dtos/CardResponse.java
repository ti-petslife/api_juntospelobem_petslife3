package com.juntospelobem.pets.dtos;

import java.math.BigDecimal;

public record CardResponse(
    String id,
    String status,
    String dataCriacao,
    int numeroNotaFiscal,
    BigDecimal valorTotal,
    String link,
    int qtCupons
) {}