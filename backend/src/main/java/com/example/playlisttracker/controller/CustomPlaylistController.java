package com.example.playlisttracker.controller;

import com.example.playlisttracker.dto.AddContentRequest;
import com.example.playlisttracker.dto.AddContentResponse;
import com.example.playlisttracker.dto.CreateCustomPlaylistRequest;
import com.example.playlisttracker.dto.VideoProgressRequest;
import com.example.playlisttracker.filter.AnonymousUserFilter;
import com.example.playlisttracker.model.CustomPlaylist;
import com.example.playlisttracker.service.CustomPlaylistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/custom-playlists")
public class CustomPlaylistController {

    private final CustomPlaylistService customPlaylistService;

    public CustomPlaylistController(CustomPlaylistService customPlaylistService) {
        this.customPlaylistService = customPlaylistService;
    }

    /**
     * POST /api/custom-playlists
     * Create a new custom playlist for the current anonymous user.
     */
    @PostMapping
    public ResponseEntity<CustomPlaylist> createPlaylist(
            @Valid @RequestBody CreateCustomPlaylistRequest request,
            HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        CustomPlaylist playlist = customPlaylistService.createPlaylist(anonymousUserId, request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    /**
     * GET /api/custom-playlists
     * Retrieve all custom playlists for the current anonymous user.
     */
    @GetMapping
    public ResponseEntity<List<CustomPlaylist>> getUserPlaylists(HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        List<CustomPlaylist> playlists = customPlaylistService.getUserPlaylists(anonymousUserId);
        return ResponseEntity.ok(playlists);
    }

    /**
     * GET /api/custom-playlists/{id}
     * Retrieve a specific custom playlist. Returns 404 if not found or belongs to another user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomPlaylist> getPlaylist(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        CustomPlaylist playlist = customPlaylistService.getPlaylist(anonymousUserId, id);
        return ResponseEntity.ok(playlist);
    }

    /**
     * POST /api/custom-playlists/{id}/content
     * Add a single YouTube video OR an entire YouTube playlist into this custom playlist.
     */
    @PostMapping("/{id}/content")
    public ResponseEntity<AddContentResponse> addContent(
            @PathVariable String id,
            @Valid @RequestBody AddContentRequest request,
            HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        AddContentResponse response = customPlaylistService.addContent(anonymousUserId, id, request.getUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/custom-playlists/{id}/videos/{videoId}
     * Update completion status of a video in this custom playlist.
     */
    @PatchMapping("/{id}/videos/{videoId}")
    public ResponseEntity<CustomPlaylist> updateVideoProgress(
            @PathVariable String id,
            @PathVariable String videoId,
            @RequestBody VideoProgressRequest request,
            HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        CustomPlaylist updated = customPlaylistService.updateVideoProgress(
                anonymousUserId, id, videoId, request.isCompleted());
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/custom-playlists/{id}/videos/{videoId}
     * Remove a video from the custom playlist.
     */
    @DeleteMapping("/{id}/videos/{videoId}")
    public ResponseEntity<CustomPlaylist> deleteVideo(
            @PathVariable String id,
            @PathVariable String videoId,
            HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        CustomPlaylist updated = customPlaylistService.deleteVideo(anonymousUserId, id, videoId);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/custom-playlists/{id}
     * Delete an entire custom playlist.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        String anonymousUserId = resolveAnonymousUserId(httpRequest);
        customPlaylistService.deletePlaylist(anonymousUserId, id);
        return ResponseEntity.noContent().build();
    }

    private String resolveAnonymousUserId(HttpServletRequest request) {
        Object attr = request.getAttribute(AnonymousUserFilter.ANONYMOUS_USER_ID_ATTRIBUTE);
        if (attr == null || attr.toString().isBlank()) {
            throw new IllegalStateException("Anonymous user ID not resolved by filter");
        }
        return attr.toString();
    }
}
