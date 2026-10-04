package com.example.playlisttracker.service;

import com.example.playlisttracker.model.Playlist;
import com.example.playlisttracker.model.Video;
import com.example.playlisttracker.repository.PlaylistRepository;
import com.example.playlisttracker.service.YouTubeService.PlaylistInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    // Matches both long and short YouTube playlist URL formats
    private static final Pattern PLAYLIST_ID_PATTERN = Pattern.compile(
            "(?:list=)([a-zA-Z0-9_-]+)"
    );

    private final PlaylistRepository playlistRepository;
    private final YouTubeService youTubeService;

    public PlaylistService(PlaylistRepository playlistRepository, YouTubeService youTubeService) {
        this.playlistRepository = playlistRepository;
        this.youTubeService = youTubeService;
    }

    /**
     * Imports a YouTube playlist for the given anonymous user.
     * If the user already imported this playlist, returns the existing record.
     */
    public Playlist importPlaylist(String anonymousUserId, String playlistUrl) {
        String playlistId = extractPlaylistId(playlistUrl);

        // Check if already imported by this user
        Optional<Playlist> existing = playlistRepository
                .findByAnonymousUserIdAndPlaylistId(anonymousUserId, playlistId);
        if (existing.isPresent()) {
            log.info("Playlist {} already imported for user {}", playlistId, anonymousUserId);
            return existing.get();
        }

        // Fetch from YouTube
        PlaylistInfo info = youTubeService.fetchPlaylistInfo(playlistId);
        List<Video> videos = youTubeService.fetchAllPlaylistVideos(playlistId);

        if (videos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Playlist has no public videos. Please check the URL.");
        }

        Playlist playlist = Playlist.builder()
                .anonymousUserId(anonymousUserId)
                .playlistId(playlistId)
                .title(info.title())
                .thumbnail(info.thumbnail())
                .totalVideos(videos.size())
                .videos(videos)
                .build();

        Playlist saved = playlistRepository.save(playlist);
        log.info("Imported playlist {} for user {}", playlistId, anonymousUserId);
        return saved;
    }

    /**
     * Returns all playlists belonging to the current anonymous user.
     */
    public List<Playlist> getUserPlaylists(String anonymousUserId) {
        return playlistRepository.findByAnonymousUserIdOrderByCreatedAtDesc(anonymousUserId);
    }

    /**
     * Returns a specific playlist. Returns 404 if it doesn't belong to the current user.
     */
    public Playlist getPlaylist(String anonymousUserId, String playlistDbId) {
        return playlistRepository.findByIdAndAnonymousUserId(playlistDbId, anonymousUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
    }

    /**
     * Updates the completion status of a single video. Scopes the update to the
     * current anonymous user — never updates by playlistId alone.
     */
    public Playlist updateVideoProgress(String anonymousUserId,
                                         String playlistDbId,
                                         String videoId,
                                         boolean completed) {
        Playlist playlist = playlistRepository
                .findByIdAndAnonymousUserId(playlistDbId, anonymousUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));

        boolean found = false;
        for (Video video : playlist.getVideos()) {
            if (video.getVideoId().equals(videoId)) {
                video.setCompleted(completed);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found in playlist");
        }

        return playlistRepository.save(playlist);
    }

    /**
     * Extracts the YouTube playlist ID from a URL.
     * Supports formats:
     *   https://www.youtube.com/playlist?list=PLxxxxxx
     *   https://youtube.com/playlist?list=PLxxxxxx
     *   https://www.youtube.com/watch?v=xxx&list=PLxxxxxx
     */
    private String extractPlaylistId(String url) {
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please enter a valid YouTube playlist URL.");
        }

        Matcher matcher = PLAYLIST_ID_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please enter a valid YouTube playlist URL.");
        }

        String id = matcher.group(1);
        if (id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please enter a valid YouTube playlist URL.");
        }

        return id;
    }
}
