package com.juntospelobem.pets.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtSecurityFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("METODO: " + request.getMethod() + " | URI: " + request.getRequestURI());

        // 💡 Jeito Sênior: Deixa o Preflight passar sem exigir Token
        // O CorsConfig interceptará isso na sequência e devolverá os cabeçalhos corretos.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 💡 Uso do Switch do Java 25 para rotas públicas
        String uri = request.getRequestURI();
        boolean rotaLiberada = switch (uri) {
            case String s when s.startsWith("/api/auth") -> true;
            default -> false;
        };

        if (rotaLiberada) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Acesso negado. Token ausente ou mal formatado.\"}");
            return;
        }

        String token = authHeader.replace("Bearer ", "");

        try {
            if (token != null) {
                String documento = jwtService.validarTokenEExtrairDocumento(token);
                request.setAttribute("documentoCliente", documento);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Acesso negado. Token invalido ou expirado.\"}");
            return; 
        }

        filterChain.doFilter(request, response);
    }
}