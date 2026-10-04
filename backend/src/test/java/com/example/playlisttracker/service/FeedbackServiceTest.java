package com.example.playlisttracker.service;

import com.example.playlisttracker.dto.FeedbackRequest;
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
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(mailSender);
        ReflectionTestUtils.setField(feedbackService, "receiverEmail", "owner@example.com");
        ReflectionTestUtils.setField(feedbackService, "mailUsername", "sender@example.com");
    }

    @Test
    void testSendFeedback_Success() {
        FeedbackRequest request = FeedbackRequest.builder()
                .name("Adarsh")
                .feedback("Great application!")
                .build();

        feedbackService.sendFeedback(request);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("owner@example.com", sent.getTo()[0]);
        assertEquals("New Feedback — Playlist Tracker", sent.getSubject());
        assertTrue(sent.getText().contains("Name: Adarsh"));
        assertTrue(sent.getText().contains("Feedback:\nGreat application!"));
    }

    @Test
    void testSendFeedback_MailException() {
        FeedbackRequest request = FeedbackRequest.builder()
                .name("Adarsh")
                .feedback("Great application!")
                .build();

        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                feedbackService.sendFeedback(request));

        assertTrue(ex.getReason().contains("Unable to send feedback. Please try again."));
    }
}
