package com.example.playlisttracker.repository;

import com.example.playlisttracker.model.CustomPlaylist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomPlaylistRepository extends MongoRepository<CustomPlaylist, String> {

    /**
     * Find all custom playlists belonging to the current anonymous user, ordered by creation date descending.
     */
    List<CustomPlaylist> findByAnonymousUserIdOrderByCreatedAtDesc(String anonymousUserId);

    /**
     * Find a custom playlist by its MongoDB _id AND anonymousUserId.
     * Guarantees that User A cannot read or mutate User B's custom playlist.
     */
    Optional<CustomPlaylist> findByIdAndAnonymousUserId(String id, String anonymousUserId);
}
