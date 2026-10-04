package com.example.playlisttracker.repository;

import com.example.playlisttracker.model.Playlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends MongoRepository<Playlist, String> {

    /**
     * Find all playlists belonging to the current anonymous user.
     */
    List<Playlist> findByAnonymousUserIdOrderByCreatedAtDesc(String anonymousUserId);

    /**
     * Find a playlist by its MongoDB _id AND anonymousUserId.
     * This prevents one user from accessing another user's playlist.
     */
    Optional<Playlist> findByIdAndAnonymousUserId(String id, String anonymousUserId);

    /**
     * Check if a playlist already exists for a user (compound unique index enforcement).
     */
    Optional<Playlist> findByAnonymousUserIdAndPlaylistId(String anonymousUserId, String playlistId);
}
