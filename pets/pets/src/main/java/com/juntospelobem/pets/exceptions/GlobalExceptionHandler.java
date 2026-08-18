package com.juntospelobem.pets.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ProblemDetail handleClienteNaoEncontrado(ClienteNaoEncontradoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Cliente não encontrado");
        problemDetail.setType(URI.create("https://suaempresa.com.br/erros/cliente-nao-encontrado"));
        return problemDetail;
    }

    @ExceptionHandler(OtpInvalidoException.class)
    public ProblemDetail handleOtpInvalido(OtpInvalidoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Falha de Autenticação");
        problemDetail.setType(URI.create("https://suaempresa.com.br/erros/otp-invalido"));
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Os dados enviados são inválidos.");
        problemDetail.setTitle("Erro de Validação");
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            problemDetail.setProperty(error.getField(), error.getDefaultMessage())
        );

        return problemDetail;
    }
}