package com.juntospelobem.pets.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequest(
    @NotBlank(message = "O documento não pode ser vazio")
    @Pattern(regexp = "^\\d{11}$|^\\d{14}$", message = "O documento deve conter 11 (CPF) ou 14 (CNPJ) dígitos numéricos")
    String documento
) {}
