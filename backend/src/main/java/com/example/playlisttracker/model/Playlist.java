package com.example.playlisttracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "playlists")
@CompoundIndex(name = "user_playlist_unique", def = "{'anonymousUserId': 1, 'playlistId': 1}", unique = true)
public class Playlist {

    @Id
    private String id;

    private String anonymousUserId;
    private String playlistId;
    private String title;
    private String thumbnail;
    private int totalVideos;
    private List<Video> videos;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
