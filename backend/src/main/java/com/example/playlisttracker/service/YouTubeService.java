package com.example.playlisttracker.service;

import com.example.playlisttracker.model.Video;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls the YouTube Data API v3 to retrieve playlist metadata and all videos.
 */
@Service
public class YouTubeService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeService.class);
    private static final String YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${youtube.api.key}")
    private String apiKey;

    public YouTubeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches basic playlist information (title, thumbnail).
     *
     * @param playlistId the YouTube playlist ID
     * @return JsonNode of the playlist resource
     * @throws IllegalArgumentException if the playlist doesn't exist or is private
     * @throws RuntimeException         on API or network errors
     */
    public PlaylistInfo fetchPlaylistInfo(String playlistId) {
        String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/playlists")
                .queryParam("part", "snippet")
                .queryParam("id", playlistId)
                .queryParam("key", apiKey)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");

            if (items.isEmpty()) {
                throw new IllegalArgumentException("Playlist not found or is private: " + playlistId);
            }

            JsonNode snippet = items.get(0).path("snippet");
            String title = snippet.path("title").asText("Untitled Playlist");
            String thumbnail = extractBestThumbnail(snippet.path("thumbnails"));

            return new PlaylistInfo(title, thumbnail);

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("YouTube API quota exceeded or access denied", e);
            throw new RuntimeException("YouTube API quota exceeded. Please try again later.");
        } catch (HttpClientErrorException e) {
            log.error("YouTube API client error: {}", e.getMessage());
            throw new RuntimeException("Unable to fetch playlist from YouTube.");
        } catch (RestClientException e) {
            log.error("Network error calling YouTube API", e);
            throw new RuntimeException("Network error. Please check your connection and try again.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching playlist info", e);
            throw new RuntimeException("Unable to fetch playlist right now. Please try again later.");
        }
    }

    /**
     * Fetches all videos in a playlist, handling pagination automatically.
     *
     * @param playlistId the YouTube playlist ID
     * @return list of Video objects
     */
    public List<Video> fetchAllPlaylistVideos(String playlistId) {
        List<Video> videos = new ArrayList<>();
        String pageToken = null;
        int position = 1;

        do {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/playlistItems")
                    .queryParam("part", "snippet,contentDetails")
                    .queryParam("playlistId", playlistId)
                    .queryParam("maxResults", 50)
                    .queryParam("key", apiKey);

            if (pageToken != null) {
                builder.queryParam("pageToken", pageToken);
            }

            String url = builder.toUriString();

            try {
                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("items");

                for (JsonNode item : items) {
                    JsonNode snippet = item.path("snippet");
                    String videoId = snippet.path("resourceId").path("videoId").asText();
                    String status = item.path("status").path("privacyStatus").asText();

                    // Skip deleted or private videos
                    if (videoId.isEmpty() || videoId.equals("null")) {
                        log.debug("Skipping item with missing videoId at position {}", position);
                        position++;
                        continue;
                    }

                    String title = snippet.path("title").asText("");
                    // Skip private/deleted video placeholders
                    if (title.equals("Private video") || title.equals("Deleted video")) {
                        log.debug("Skipping {} video at position {}", title, position);
                        position++;
                        continue;
                    }

                    String thumbnail = extractBestThumbnail(snippet.path("thumbnails"));

                    Video video = Video.builder()
                            .videoId(videoId)
                            .title(title)
                            .thumbnail(thumbnail)
                            .duration("") // will be enriched below
                            .position(position)
                            .completed(false)
                            .build();

                    videos.add(video);
                    position++;
                }

                JsonNode nextPageTokenNode = root.path("nextPageToken");
                pageToken = nextPageTokenNode.isMissingNode() ? null : nextPageTokenNode.asText(null);

            } catch (HttpClientErrorException.Forbidden e) {
                log.error("YouTube API quota exceeded", e);
                throw new RuntimeException("YouTube API quota exceeded. Please try again later.");
            } catch (HttpClientErrorException e) {
                log.error("YouTube API client error fetching playlist items: {}", e.getMessage());
                throw new RuntimeException("Unable to fetch playlist videos from YouTube.");
            } catch (RestClientException e) {
                log.error("Network error fetching playlist items", e);
                throw new RuntimeException("Network error. Please check your connection.");
            } catch (Exception e) {
                log.error("Unexpected error fetching playlist items", e);
                throw new RuntimeException("Unable to fetch playlist right now. Please try again later.");
            }

        } while (pageToken != null);

        // Enrich videos with duration
        enrichVideosWithDuration(videos);

        return videos;
    }

    /**
     * Calls the videos.list API to fetch duration for each video in batches of 50.
     */
    private void enrichVideosWithDuration(List<Video> videos) {
        if (videos.isEmpty()) return;

        // Process in batches of 50 (API limit)
        int batchSize = 50;
        for (int i = 0; i < videos.size(); i += batchSize) {
            List<Video> batch = videos.subList(i, Math.min(i + batchSize, videos.size()));

            String ids = batch.stream()
                    .map(Video::getVideoId)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/videos")
                    .queryParam("part", "contentDetails")
                    .queryParam("id", ids)
                    .queryParam("key", apiKey)
                    .toUriString();

            try {
                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);

                // Map videoId -> duration
                for (JsonNode item : root.path("items")) {
                    String videoId = item.path("id").asText();
                    String isoDuration = item.path("contentDetails").path("duration").asText();
                    String formatted = formatDuration(isoDuration);

                    batch.stream()
                            .filter(v -> v.getVideoId().equals(videoId))
                            .findFirst()
                            .ifPresent(v -> v.setDuration(formatted));
                }

            } catch (Exception e) {
                log.warn("Failed to enrich batch with duration, continuing without duration: {}", e.getMessage());
                // Non-fatal — videos will have empty duration
            }
        }
    }

    /**
     * Converts ISO 8601 duration (e.g. PT18M32S) to human-readable format (18:32).
     */
    private String formatDuration(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            Duration d = Duration.parse(iso);
            long hours = d.toHours();
            long minutes = d.toMinutesPart();
            long seconds = d.toSecondsPart();
            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%d:%02d", minutes, seconds);
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Picks the best available thumbnail (maxres → high → medium → default).
     */
    private String extractBestThumbnail(JsonNode thumbnails) {
        for (String quality : new String[]{"maxres", "high", "medium", "default"}) {
            JsonNode node = thumbnails.path(quality).path("url");
            if (!node.isMissingNode() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return "";
    }

    /**
     * Fetches details for a single video by videoId.
     *
     * @param videoId the YouTube video ID
     * @return Video object with metadata and formatted duration
     */
    public Video fetchSingleVideo(String videoId) {
        String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_BASE + "/videos")
                .queryParam("part", "snippet,contentDetails")
                .queryParam("id", videoId)
                .queryParam("key", apiKey)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");

            if (items.isEmpty()) {
                throw new IllegalArgumentException("Video not found or is private: " + videoId);
            }

            JsonNode item = items.get(0);
            JsonNode snippet = item.path("snippet");
            String title = snippet.path("title").asText("Untitled Video");
            String thumbnail = extractBestThumbnail(snippet.path("thumbnails"));
            String isoDuration = item.path("contentDetails").path("duration").asText("");
            String duration = formatDuration(isoDuration);

            return Video.builder()
                    .videoId(videoId)
                    .title(title)
                    .thumbnail(thumbnail)
                    .duration(duration)
                    .position(1)
                    .completed(false)
                    .build();

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("YouTube API quota exceeded or access denied", e);
            throw new RuntimeException("YouTube API quota exceeded. Please try again later.");
        } catch (HttpClientErrorException e) {
            log.error("YouTube API client error fetching video: {}", e.getMessage());
            throw new RuntimeException("Unable to fetch video from YouTube.");
        } catch (RestClientException e) {
            log.error("Network error calling YouTube API", e);
            throw new RuntimeException("Network error. Please check your connection.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching video", e);
            throw new RuntimeException("Unable to fetch video right now. Please try again later.");
        }
    }

    /**
     * Simple value holder for playlist metadata.
     */
    public record PlaylistInfo(String title, String thumbnail) {}
}
