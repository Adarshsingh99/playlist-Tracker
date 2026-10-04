package com.example.playlisttracker.dto;

import com.example.playlisttracker.model.CustomPlaylist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddContentResponse {
    private CustomPlaylist playlist;
    private String contentType; // "VIDEO" or "PLAYLIST"
    private int totalFound;
    private int addedCount;
    private int alreadyExistedCount;
    private String message;
}
