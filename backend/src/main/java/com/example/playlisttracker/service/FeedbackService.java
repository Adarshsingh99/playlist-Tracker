package com.example.playlisttracker.service;

import com.example.playlisttracker.dto.FeedbackRequest;
import com.example.playlisttracker.model.Feedback;
import com.example.playlisttracker.repository.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final JavaMailSender mailSender;
    private final FeedbackRepository feedbackRepository;

    @Value("${app.feedback.receiver-email:}")
    private String receiverEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Autowired
    public FeedbackService(@Autowired(required = false) JavaMailSender mailSender,
                           FeedbackRepository feedbackRepository) {
        this.mailSender = mailSender;
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback sendFeedback(FeedbackRequest request) {
        String cleanName = request.getName() != null ? request.getName().trim() : "Anonymous";
        String cleanFeedback = request.getFeedback() != null ? request.getFeedback().trim() : "";

        // 1. Always save feedback to MongoDB first — guarantees 100% data safety
        Feedback feedbackEntity = Feedback.builder()
                .name(cleanName)
                .feedback(cleanFeedback)
                .build();
        Feedback saved = feedbackRepository.save(feedbackEntity);
        log.info("Feedback saved to MongoDB with ID: {}", saved.getId());

        // 2. Try to dispatch email notification if mailSender is available
        String targetEmail = (receiverEmail != null && !receiverEmail.isBlank())
                ? receiverEmail.trim()
                : (mailUsername != null && !mailUsername.isBlank()) ? mailUsername.trim() : null;

        if (mailSender != null && targetEmail != null && !targetEmail.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (mailUsername != null && !mailUsername.isBlank()) {
                    message.setFrom(mailUsername.trim());
                }
                message.setTo(targetEmail);
                message.setSubject("New Feedback — Playlist Tracker");
                message.setText(String.format("New Feedback — Playlist Tracker\n\nName: %s\n\nFeedback:\n%s",
                        cleanName, cleanFeedback));

                mailSender.send(message);
                log.info("Feedback email sent successfully to {}", targetEmail);
            } catch (MailException ex) {
                // Render free tier blocks outbound SMTP port 587 to prevent spam
                log.warn("SMTP email notification could not be delivered (e.g. cloud host blocking outbound SMTP), but feedback was safely saved to MongoDB: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Unexpected error sending feedback email, feedback preserved in DB: {}", ex.getMessage());
            }
        }

        return saved;
    }

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }
}
