package com.juntospelobem.pets.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarCodigo(String emailDestino, String codigo, String codcli) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                 helper.setFrom("noreply@grupoagrofarm.com.br"); 
            
            helper.setTo(emailDestino);
            helper.setSubject("Seu Código de Acesso - Juntos Pelo Bem");
            
           String htmlMsg = "<div style=\"font-family: 'Inter', 'Segoe UI', Arial, sans-serif; background-color: #f8f9fa; padding: 40px 20px;\">"
               + "<div style=\"max-width: 500px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); text-align: center;\">"
               + "<h2 style=\"color: #1a0b2e; font-size: 26px; margin-top: 0;\">Olá!</h2>"
               + "<p style=\"color: #4a4a4a; font-size: 16px; line-height: 1.6; margin-bottom: 30px;\">Aqui está o seu código de acesso para o portal:</p>"
               + "<div style=\"background-color: #2b124c; padding: 20px 30px; border-radius: 8px; margin: 0 auto 30px auto; display: inline-block;\">"
               + "<h1 style=\"color: #ffffff; font-size: 36px; letter-spacing: 8px; margin: 0; font-family: monospace;\">" + codigo + "</h1>"
               + "</div>"
               + "<p style=\"color: #888888; font-size: 14px; line-height: 1.5;\">Este código é válido por 5 minutos!!.<br>Não o compartilhe com ninguém.</p>"
               + "<hr style=\"border: 0; border-top: 1px solid #eef0f2; margin: 30px 0;\">"
               + "<p style=\"color: #00a0e3; font-weight: bold; font-size: 16px; margin: 0;\">Equipe Juntos Pelo Bem</p>"
               + "<p style=\"color: #a0a0a0; font-size: 12px; margin-top: 5px;\">Petslife</p>"
               + "</div>"
               + "</div>";

            helper.setText(htmlMsg, true); 

            mailSender.send(message);
            log.info("✅ E-mail enviado com sucesso para: {}", emailDestino);

        } catch (MessagingException e) {
            log.error("❌ Erro ao enviar e-mail para {}: {}", emailDestino, e.getMessage());
        }
    }
}