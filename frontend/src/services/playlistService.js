import api from './api'

export const playlistService = {
  /**
   * Import a YouTube playlist by URL.
   */
  importPlaylist: (playlistUrl) =>
    api.post('/api/playlists/import', { playlistUrl }).then((r) => r.data),

  /**
   * Get all playlists for the current anonymous user.
   */
  getUserPlaylists: () =>
    api.get('/api/playlists').then((r) => r.data),

  /**
   * Get a single playlist by its MongoDB _id.
   */
  getPlaylist: (playlistId) =>
    api.get(`/api/playlists/${playlistId}`).then((r) => r.data),

  /**
   * Update a video's completion status.
   */
  updateVideoProgress: (playlistId, videoId, completed) =>
    api
      .patch(`/api/playlists/${playlistId}/videos/${videoId}`, { completed })
      .then((r) => r.data),
}
