package com.example.playlisttracker.service;

import com.example.playlisttracker.dto.FeedbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final JavaMailSender mailSender;

    @Value("${app.feedback.receiver-email:}")
    private String receiverEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Autowired
    public FeedbackService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendFeedback(FeedbackRequest request) {
        String targetEmail = (receiverEmail != null && !receiverEmail.isBlank())
                ? receiverEmail.trim()
                : (mailUsername != null && !mailUsername.isBlank()) ? mailUsername.trim() : null;

        String emailSubject = "New Feedback — Playlist Tracker";
        String emailBody = String.format("New Feedback — Playlist Tracker\n\nName: %s\n\nFeedback:\n%s",
                request.getName().trim(),
                request.getFeedback().trim());

        // If email sending is not configured in local environment, log gracefully
        if (mailSender == null || targetEmail == null || targetEmail.isBlank()) {
            log.warn("Mail service not fully configured (target email or mailSender missing). Feedback received:\n{}", emailBody);
            // In local/dev without SMTP credentials, treat as received and logged
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailUsername != null && !mailUsername.isBlank()) {
                message.setFrom(mailUsername.trim());
            }
            message.setTo(targetEmail);
            message.setSubject(emailSubject);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Feedback email sent successfully to {}", targetEmail);
        } catch (MailException ex) {
            log.error("Failed to send feedback email: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to send feedback. Please try again.");
        }
    }
}
