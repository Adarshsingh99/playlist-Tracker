package com.example.playlisttracker.service;

import com.example.playlisttracker.dto.AddContentResponse;
import com.example.playlisttracker.model.CustomPlaylist;
import com.example.playlisttracker.model.CustomVideo;
import com.example.playlisttracker.model.Video;
import com.example.playlisttracker.repository.CustomPlaylistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomPlaylistServiceTest {

    @Mock
    private CustomPlaylistRepository customPlaylistRepository;

    @Mock
    private YouTubeService youTubeService;

    @InjectMocks
    private CustomPlaylistService customPlaylistService;

    private CustomPlaylist mockPlaylist;
    private final String userId = "user-123";
    private final String playlistId = "playlist-abc";

    @BeforeEach
    void setUp() {
        mockPlaylist = CustomPlaylist.builder()
                .id(playlistId)
                .anonymousUserId(userId)
                .name("Web Development")
                .videos(new ArrayList<>())
                .build();
    }

    @Test
    void testCreatePlaylist() {
        when(customPlaylistRepository.save(any(CustomPlaylist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomPlaylist result = customPlaylistService.createPlaylist(userId, "Web Development");
        assertNotNull(result);
        assertEquals("Web Development", result.getName());
        assertEquals(userId, result.getAnonymousUserId());
        assertTrue(result.getVideos().isEmpty());
    }

    @Test
    void testAddSingleVideo_Success() {
        when(customPlaylistRepository.findByIdAndAnonymousUserId(playlistId, userId))
                .thenReturn(Optional.of(mockPlaylist));
        when(youTubeService.fetchSingleVideo("vid123")).thenReturn(
                Video.builder()
                        .videoId("vid123")
                        .title("HTML Basics")
                        .thumbnail("thumb.jpg")
                        .duration("15:00")
                        .build()
        );
        when(customPlaylistRepository.save(any(CustomPlaylist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AddContentResponse response = customPlaylistService.addContent(
                userId, playlistId, "https://www.youtube.com/watch?v=vid123");

        assertNotNull(response);
        assertEquals("VIDEO", response.getContentType());
        assertEquals(1, response.getAddedCount());
        assertEquals(0, response.getAlreadyExistedCount());
        assertEquals(1, response.getPlaylist().getVideos().size());
        assertEquals("HTML Basics", response.getPlaylist().getVideos().get(0).getTitle());
    }

    @Test
    void testAddSingleVideo_DuplicatePrevention() {
        // Pre-populate with vid123
        mockPlaylist.getVideos().add(CustomVideo.builder()
                .videoId("vid123")
                .title("HTML Basics")
                .position(1)
                .build());

        when(customPlaylistRepository.findByIdAndAnonymousUserId(playlistId, userId))
                .thenReturn(Optional.of(mockPlaylist));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                customPlaylistService.addContent(userId, playlistId, "https://www.youtube.com/watch?v=vid123"));

        assertTrue(ex.getReason().contains("Video already exists in this playlist"));
        verify(youTubeService, never()).fetchSingleVideo(anyString());
    }

    @Test
    void testAddPlaylist_PartialDuplicates() {
        // Pre-populate with vid1
        mockPlaylist.getVideos().add(CustomVideo.builder()
                .videoId("vid1")
                .title("HTML Basics")
                .position(1)
                .build());

        when(customPlaylistRepository.findByIdAndAnonymousUserId(playlistId, userId))
                .thenReturn(Optional.of(mockPlaylist));

        // YouTube playlist has vid1, vid2, vid3
        List<Video> fetched = List.of(
                Video.builder().videoId("vid1").title("HTML Basics").duration("10:00").build(),
                Video.builder().videoId("vid2").title("CSS Basics").duration("12:00").build(),
                Video.builder().videoId("vid3").title("JS Basics").duration("14:00").build()
        );
        when(youTubeService.fetchAllPlaylistVideos("PL_list123")).thenReturn(fetched);
        when(customPlaylistRepository.save(any(CustomPlaylist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AddContentResponse response = customPlaylistService.addContent(
                userId, playlistId, "https://www.youtube.com/playlist?list=PL_list123");

        assertNotNull(response);
        assertEquals("PLAYLIST", response.getContentType());
        assertEquals(3, response.getTotalFound());
        assertEquals(2, response.getAddedCount());
        assertEquals(1, response.getAlreadyExistedCount());
        assertEquals(3, response.getPlaylist().getVideos().size()); // 1 original + 2 new
    }

    @Test
    void testUserIsolation_CannotAccessOtherUserPlaylist() {
        // User B attempts to access User A's playlist
        when(customPlaylistRepository.findByIdAndAnonymousUserId(playlistId, "user-B"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                customPlaylistService.getPlaylist("user-B", playlistId));
    }

    @Test
    void testUpdateVideoProgress() {
        mockPlaylist.getVideos().add(CustomVideo.builder()
                .videoId("vid1")
                .title("HTML Basics")
                .position(1)
                .completed(false)
                .build());

        when(customPlaylistRepository.findByIdAndAnonymousUserId(playlistId, userId))
                .thenReturn(Optional.of(mockPlaylist));
        when(customPlaylistRepository.save(any(CustomPlaylist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomPlaylist updated = customPlaylistService.updateVideoProgress(userId, playlistId, "vid1", true);

        assertTrue(updated.getVideos().get(0).isCompleted());
    }
}
