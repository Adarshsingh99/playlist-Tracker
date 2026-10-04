package com.example.playlisttracker.service;

import com.example.playlisttracker.dto.AddContentResponse;
import com.example.playlisttracker.model.CustomPlaylist;
import com.example.playlisttracker.model.CustomVideo;
import com.example.playlisttracker.model.Video;
import com.example.playlisttracker.repository.CustomPlaylistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CustomPlaylistService {

    private static final Logger log = LoggerFactory.getLogger(CustomPlaylistService.class);

    private static final Pattern PLAYLIST_EXPLICIT_PATTERN = Pattern.compile(
            "(?:youtube\\.com/playlist.*[?&]list=)([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VIDEO_WATCH_PATTERN = Pattern.compile(
            "(?:youtube\\.com/watch.*[?&]v=)([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern YOUTU_BE_PATTERN = Pattern.compile(
            "(?:youtu\\.be/)([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SHORTS_PATTERN = Pattern.compile(
            "(?:youtube\\.com/shorts/)([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EMBED_PATTERN = Pattern.compile(
            "(?:youtube\\.com/embed/)([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LIVE_PATTERN = Pattern.compile(
            "(?:youtube\\.com/live/)([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GENERIC_LIST_PATTERN = Pattern.compile(
            "(?:[?&]list=)([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE
    );

    private final CustomPlaylistRepository customPlaylistRepository;
    private final YouTubeService youTubeService;

    public CustomPlaylistService(CustomPlaylistRepository customPlaylistRepository, YouTubeService youTubeService) {
        this.customPlaylistRepository = customPlaylistRepository;
        this.youTubeService = youTubeService;
    }

    /**
     * Create a new custom playlist for the current anonymous user.
     */
    public CustomPlaylist createPlaylist(String anonymousUserId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Playlist name is required.");
        }

        CustomPlaylist playlist = CustomPlaylist.builder()
                .anonymousUserId(anonymousUserId)
                .name(name.trim())
                .videos(new ArrayList<>())
                .build();

        CustomPlaylist saved = customPlaylistRepository.save(playlist);
        log.info("Created custom playlist '{}' with ID {} for user {}", saved.getName(), saved.getId(), anonymousUserId);
        return saved;
    }

    /**
     * Get all custom playlists for the current anonymous user.
     */
    public List<CustomPlaylist> getUserPlaylists(String anonymousUserId) {
        return customPlaylistRepository.findByAnonymousUserIdOrderByCreatedAtDesc(anonymousUserId);
    }

    /**
     * Get a specific custom playlist by id. Returns 404 if not found or not owned by this user.
     */
    public CustomPlaylist getPlaylist(String anonymousUserId, String playlistId) {
        return customPlaylistRepository.findByIdAndAnonymousUserId(playlistId, anonymousUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom playlist not found"));
    }

    /**
     * Adds content (single video or entire YouTube playlist) to an existing custom playlist.
     */
    public AddContentResponse addContent(String anonymousUserId, String playlistId, String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube URL is required.");
        }

        CustomPlaylist playlist = getPlaylist(anonymousUserId, playlistId);
        if (playlist.getVideos() == null) {
            playlist.setVideos(new ArrayList<>());
        }

        String url = rawUrl.trim();
        ContentTarget target = identifyContentTarget(url);

        if (target.type == TargetType.VIDEO) {
            return addSingleVideo(playlist, target.id);
        } else if (target.type == TargetType.PLAYLIST) {
            return addEntirePlaylist(playlist, target.id);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please enter a valid YouTube video or playlist URL.");
        }
    }

    /**
     * Updates completion status of a video inside a custom playlist.
     */
    public CustomPlaylist updateVideoProgress(String anonymousUserId,
                                              String playlistId,
                                              String videoId,
                                              boolean completed) {
        CustomPlaylist playlist = getPlaylist(anonymousUserId, playlistId);

        boolean found = false;
        if (playlist.getVideos() != null) {
            for (CustomVideo video : playlist.getVideos()) {
                if (video.getVideoId().equals(videoId)) {
                    video.setCompleted(completed);
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found in custom playlist");
        }

        return customPlaylistRepository.save(playlist);
    }

    /**
     * Deletes a video from a custom playlist and recalculates video positions.
     */
    public CustomPlaylist deleteVideo(String anonymousUserId, String playlistId, String videoId) {
        CustomPlaylist playlist = getPlaylist(anonymousUserId, playlistId);

        if (playlist.getVideos() == null || playlist.getVideos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found in custom playlist");
        }

        boolean removed = playlist.getVideos().removeIf(v -> v.getVideoId().equals(videoId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found in custom playlist");
        }

        // Re-index positions
        for (int i = 0; i < playlist.getVideos().size(); i++) {
            playlist.getVideos().get(i).setPosition(i + 1);
        }

        return customPlaylistRepository.save(playlist);
    }

    /**
     * Deletes an entire custom playlist.
     */
    public void deletePlaylist(String anonymousUserId, String playlistId) {
        CustomPlaylist playlist = getPlaylist(anonymousUserId, playlistId);
        customPlaylistRepository.delete(playlist);
        log.info("Deleted custom playlist {} for user {}", playlistId, anonymousUserId);
    }

    // Helper: add single video with duplicate prevention
    private AddContentResponse addSingleVideo(CustomPlaylist playlist, String videoId) {
        boolean exists = playlist.getVideos().stream()
                .anyMatch(v -> v.getVideoId().equals(videoId));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video already exists in this playlist.");
        }

        Video fetched = youTubeService.fetchSingleVideo(videoId);

        CustomVideo customVideo = CustomVideo.builder()
                .videoId(fetched.getVideoId())
                .title(fetched.getTitle())
                .thumbnail(fetched.getThumbnail())
                .duration(fetched.getDuration())
                .youtubeUrl("https://www.youtube.com/watch?v=" + fetched.getVideoId())
                .position(playlist.getVideos().size() + 1)
                .completed(false)
                .build();

        playlist.getVideos().add(customVideo);
        CustomPlaylist saved = customPlaylistRepository.save(playlist);

        return AddContentResponse.builder()
                .playlist(saved)
                .contentType("VIDEO")
                .totalFound(1)
                .addedCount(1)
                .alreadyExistedCount(0)
                .message("Video added successfully.")
                .build();
    }

    // Helper: add entire playlist with duplicate skipping
    private AddContentResponse addEntirePlaylist(CustomPlaylist playlist, String youtubePlaylistId) {
        List<Video> fetchedVideos = youTubeService.fetchAllPlaylistVideos(youtubePlaylistId);

        if (fetchedVideos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Playlist has no public videos or could not be found.");
        }

        Set<String> existingVideoIds = playlist.getVideos().stream()
                .map(CustomVideo::getVideoId)
                .collect(Collectors.toSet());

        List<CustomVideo> toAdd = new ArrayList<>();
        int nextPosition = playlist.getVideos().size() + 1;
        int alreadyExisted = 0;

        for (Video v : fetchedVideos) {
            if (existingVideoIds.contains(v.getVideoId())) {
                alreadyExisted++;
            } else {
                existingVideoIds.add(v.getVideoId());
                toAdd.add(CustomVideo.builder()
                        .videoId(v.getVideoId())
                        .title(v.getTitle())
                        .thumbnail(v.getThumbnail())
                        .duration(v.getDuration())
                        .youtubeUrl("https://www.youtube.com/watch?v=" + v.getVideoId())
                        .position(nextPosition++)
                        .completed(false)
                        .build());
            }
        }

        if (!toAdd.isEmpty()) {
            playlist.getVideos().addAll(toAdd);
        }

        CustomPlaylist saved = customPlaylistRepository.save(playlist);

        String message = String.format("%d videos found: %d added, %d already existed.",
                fetchedVideos.size(), toAdd.size(), alreadyExisted);

        return AddContentResponse.builder()
                .playlist(saved)
                .contentType("PLAYLIST")
                .totalFound(fetchedVideos.size())
                .addedCount(toAdd.size())
                .alreadyExistedCount(alreadyExisted)
                .message(message)
                .build();
    }

    /**
     * Determines whether a given URL is a single video or a playlist.
     */
    private ContentTarget identifyContentTarget(String url) {
        // 1. Explicit playlist URL e.g. youtube.com/playlist?list=...
        Matcher playlistMatcher = PLAYLIST_EXPLICIT_PATTERN.matcher(url);
        if (playlistMatcher.find()) {
            return new ContentTarget(TargetType.PLAYLIST, playlistMatcher.group(1));
        }

        // 2. Video watch URL e.g. youtube.com/watch?v=...
        Matcher watchMatcher = VIDEO_WATCH_PATTERN.matcher(url);
        if (watchMatcher.find()) {
            return new ContentTarget(TargetType.VIDEO, watchMatcher.group(1));
        }

        // 3. Shortened youtu.be URL
        Matcher youtuMatcher = YOUTU_BE_PATTERN.matcher(url);
        if (youtuMatcher.find()) {
            return new ContentTarget(TargetType.VIDEO, youtuMatcher.group(1));
        }

        // 4. Shorts URL
        Matcher shortsMatcher = SHORTS_PATTERN.matcher(url);
        if (shortsMatcher.find()) {
            return new ContentTarget(TargetType.VIDEO, shortsMatcher.group(1));
        }

        // 5. Embed URL
        Matcher embedMatcher = EMBED_PATTERN.matcher(url);
        if (embedMatcher.find()) {
            return new ContentTarget(TargetType.VIDEO, embedMatcher.group(1));
        }

        // 6. Live URL e.g. youtube.com/live/VIDEO_ID
        Matcher liveMatcher = LIVE_PATTERN.matcher(url);
        if (liveMatcher.find()) {
            return new ContentTarget(TargetType.VIDEO, liveMatcher.group(1));
        }

        // 7. Generic list param fallback (if someone pasted a URL that only has list=...)
        Matcher genericListMatcher = GENERIC_LIST_PATTERN.matcher(url);
        if (genericListMatcher.find()) {
            return new ContentTarget(TargetType.PLAYLIST, genericListMatcher.group(1));
        }

        return new ContentTarget(TargetType.INVALID, null);
    }

    private enum TargetType {
        VIDEO,
        PLAYLIST,
        INVALID
    }

    private record ContentTarget(TargetType type, String id) {}
}
