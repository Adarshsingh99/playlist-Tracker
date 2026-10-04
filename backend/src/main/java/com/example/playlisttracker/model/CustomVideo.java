package com.example.playlisttracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomVideo {
    private String videoId;
    private String title;
    private String thumbnail;
    private String duration;
    private String youtubeUrl;
    private int position;
    private boolean completed;
}
