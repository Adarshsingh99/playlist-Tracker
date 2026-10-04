import api from './api'

export const customPlaylistService = {
  /**
   * Create a new custom playlist.
   */
  createPlaylist: (name) =>
    api.post('/api/custom-playlists', { name }).then((r) => r.data),

  /**
   * Get all custom playlists for the current anonymous user.
   */
  getUserPlaylists: () =>
    api.get('/api/custom-playlists').then((r) => r.data),

  /**
   * Get a single custom playlist by ID.
   */
  getPlaylist: (id) =>
    api.get(`/api/custom-playlists/${id}`).then((r) => r.data),

  /**
   * Add a single YouTube video or an entire playlist to a custom playlist.
   */
  addContent: (id, url) =>
    api.post(`/api/custom-playlists/${id}/content`, { url }).then((r) => r.data),

  /**
   * Update video completion progress.
   */
  updateVideoProgress: (playlistId, videoId, completed) =>
    api
      .patch(`/api/custom-playlists/${playlistId}/videos/${videoId}`, { completed })
      .then((r) => r.data),

  /**
   * Delete a video from a custom playlist.
   */
  deleteVideo: (playlistId, videoId) =>
    api
      .delete(`/api/custom-playlists/${playlistId}/videos/${videoId}`)
      .then((r) => r.data),

  /**
   * Delete an entire custom playlist.
   */
  deletePlaylist: (playlistId) =>
    api.delete(`/api/custom-playlists/${playlistId}`).then((r) => r.data),
}
