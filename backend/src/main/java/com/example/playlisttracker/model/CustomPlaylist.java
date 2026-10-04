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
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "custom_playlists")
@CompoundIndex(name = "user_custom_playlist_idx", def = "{'anonymousUserId': 1, 'createdAt': -1}")
public class CustomPlaylist {

    @Id
    private String id;

    private String anonymousUserId;
    private String name;

    @Builder.Default
    private List<CustomVideo> videos = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
