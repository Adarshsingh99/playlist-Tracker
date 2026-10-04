package com.example.playlisttracker.controller;

import com.example.playlisttracker.dto.PlaylistRequest;
import com.example.playlisttracker.dto.VideoProgressRequest;
import com.example.playlisttracker.filter.AnonymousUserFilter;
import com.example.playlisttracker.model.Playlist;
import com.example.playlisttracker.service.PlaylistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    /**
     * POST /api/playlists/import
     * Imports a YouTube playlist for the current anonymous user.
     * The anonymousUserId is read from the request attribute set by {@link com.example.playlisttracker.filter.AnonymousUserFilter}.
     * It is NEVER read from the request body.
     */
    @PostMapping("/import")
    public ResponseEntity<Playlist> importPlaylist(
            @Valid @RequestBody PlaylistRequest request,
            HttpServletRequest httpRequest) {

        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        Playlist playlist = playlistService.importPlaylist(anonymousUserId, request.getPlaylistUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    /**
     * GET /api/playlists
     * Returns only playlists belonging to the current anonymous user.
     */
    @GetMapping
    public ResponseEntity<List<Playlist>> getUserPlaylists(HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        List<Playlist> playlists = playlistService.getUserPlaylists(anonymousUserId);
        return ResponseEntity.ok(playlists);
    }

    /**
     * GET /api/playlists/{playlistId}
     * Returns a specific playlist. Returns 404 if it doesn't belong to the current user.
     * This prevents exposing whether another user owns the playlist.
     */
    @GetMapping("/{playlistId}")
    public ResponseEntity<Playlist> getPlaylist(
            @PathVariable String playlistId,
            HttpServletRequest httpRequest) {

        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        Playlist playlist = playlistService.getPlaylist(anonymousUserId, playlistId);
        return ResponseEntity.ok(playlist);
    }

    /**
     * PATCH /api/playlists/{playlistId}/videos/{videoId}
     * Updates completion status of a video. Verifies the playlist belongs to the current user.
     */
    @PatchMapping("/{playlistId}/videos/{videoId}")
    public ResponseEntity<Playlist> updateVideoProgress(
            @PathVariable String playlistId,
            @PathVariable String videoId,
            @RequestBody VideoProgressRequest request,
            HttpServletRequest httpRequest) {

        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        Playlist updated = playlistService.updateVideoProgress(
                anonymousUserId, playlistId, videoId, request.isCompleted());
        return ResponseEntity.ok(updated);
    }

    /**
     * Reads the anonymous user ID from the request attribute populated by the filter.
     * If somehow missing (shouldn't happen), throws an internal error.
     */
    private String resolveAnonymousUserId(HttpServletRequest request) {
        Object attr = request.getAttribute(AnonymousUserFilter.ANONYMOUS_USER_ID_ATTRIBUTE);
        if (attr == null || attr.toString().isBlank()) {
            throw new IllegalStateException("Anonymous user ID not resolved by filter");
        }
        return attr.toString();
    }
}
