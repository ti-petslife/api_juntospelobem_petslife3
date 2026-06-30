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
            
            String htmlMsg = "<div style='font-family: Arial, sans-serif; text-align: center; color: #333; padding: 20px;'>"
                    + "<h2>Olá!</h2>"
                    + "<p>O seu código de cliente é: <strong>" + codcli + "</strong></p>"
                    + "<p>Aqui está o seu código de acesso para o portal:</p>"
                    + "<h1 style='color: #4CAF50; font-size: 40px; letter-spacing: 5px; background: #f4f4f4; padding: 15px; border-radius: 8px; display: inline-block;'>" + codigo + "</h1>"
                    + "<p>Este código é válido por alguns minutos. Não o partilhe com ninguém.</p>"
                    + "<br><p>Equipa Juntos Pelo Bem</p>"
                    + "</div>";

            helper.setText(htmlMsg, true); 

            mailSender.send(message);
            log.info("✅ E-mail enviado com sucesso para: {}", emailDestino);

        } catch (MessagingException e) {
            log.error("❌ Erro ao enviar e-mail para {}: {}", emailDestino, e.getMessage());
        }
    }
}