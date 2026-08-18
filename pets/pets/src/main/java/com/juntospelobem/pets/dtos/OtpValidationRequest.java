package com.juntospelobem.pets.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpValidationRequest(
    @NotBlank(message = "O documento não pode ser vazio")
    @Pattern(regexp = "^\\d{11}$|^\\d{14}$", message = "O documento deve conter 11 (CPF) ou 14 (CNPJ) dígitos numéricos")
    String documento,

    @NotBlank(message = "O código OTP é obrigatório")
    @Size(min = 6, max = 6, message = "O código deve ter exatamente 6 dígitos")
    String codigo
) {}