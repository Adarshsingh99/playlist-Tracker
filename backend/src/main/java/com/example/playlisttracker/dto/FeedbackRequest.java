package com.example.playlisttracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Feedback cannot be empty")
    private String feedback;
}
