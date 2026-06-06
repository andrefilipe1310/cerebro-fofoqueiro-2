package com.fofoqueiro.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public EmailService(JavaMailSender mailSender,
                        @Value("${email.from}") String from,
                        @Value("${email.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    public void sendCameraOffline(String to, String cameraName, String tenantName) {
        String subject = "[" + tenantName + "] Camera Offline: " + cameraName;
        String body = "<h2>Camera Offline Alert</h2>" +
                "<p>Camera <strong>" + cameraName + "</strong> in tenant <strong>" + tenantName +
                "</strong> is currently offline.</p>" +
                "<p>Please check the connection and verify the camera is powered on.</p>";
        sendHtml(to, subject, body);
    }

    public void sendWelcome(String to, String name) {
        String subject = "Welcome to Fofoqueiro!";
        String body = "<h2>Welcome, " + name + "!</h2>" +
                "<p>Your account has been created. You can now log in and start managing your cameras.</p>";
        sendHtml(to, subject, body);
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
