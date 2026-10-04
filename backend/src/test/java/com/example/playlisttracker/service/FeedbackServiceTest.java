package com.example.playlisttracker.service;

import com.example.playlisttracker.dto.FeedbackRequest;
import com.example.playlisttracker.model.Feedback;
import com.example.playlisttracker.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private FeedbackRepository feedbackRepository;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(mailSender, feedbackRepository);
        ReflectionTestUtils.setField(feedbackService, "receiverEmail", "owner@example.com");
        ReflectionTestUtils.setField(feedbackService, "mailUsername", "sender@example.com");

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId("fb-123");
            return f;
        });
    }

    @Test
    void testSendFeedback_Success() {
        FeedbackRequest request = FeedbackRequest.builder()
                .name("Adarsh")
                .feedback("Great application!")
                .build();

        Feedback saved = feedbackService.sendFeedback(request);

        assertNotNull(saved);
        assertEquals("fb-123", saved.getId());
        verify(feedbackRepository, times(1)).save(any(Feedback.class));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("owner@example.com", sent.getTo()[0]);
        assertEquals("New Feedback — Playlist Tracker", sent.getSubject());
        assertTrue(sent.getText().contains("Name: Adarsh"));
        assertTrue(sent.getText().contains("Feedback:\nGreat application!"));
    }

    @Test
    void testSendFeedback_MailException_GracefulFallbackToDb() {
        FeedbackRequest request = FeedbackRequest.builder()
                .name("Adarsh")
                .feedback("Great application!")
                .build();

        doThrow(new MailSendException("SMTP connection blocked by hosting")).when(mailSender).send(any(SimpleMailMessage.class));

        // When mail fails (e.g. Render free tier blocks port 587), it still saves to DB and returns successfully
        Feedback saved = feedbackService.sendFeedback(request);
        assertNotNull(saved);
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }
}
