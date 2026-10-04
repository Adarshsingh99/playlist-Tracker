package com.example.playlisttracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaylistRequest {

    @NotBlank(message = "Playlist URL is required")
    private String playlistUrl;
}
