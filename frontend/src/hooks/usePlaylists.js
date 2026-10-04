import { useState, useEffect, useCallback } from 'react'
import { playlistService } from '../services/playlistService'

/**
 * Fetches and manages the list of user playlists.
 */
export function usePlaylists() {
  const [playlists, setPlaylists] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchPlaylists = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await playlistService.getUserPlaylists()
      setPlaylists(data)
    } catch (err) {
      setError(err.message || 'Failed to load playlists.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchPlaylists()
  }, [fetchPlaylists])

  return { playlists, setPlaylists, loading, error, refetch: fetchPlaylists }
}

/**
 * Fetches a single playlist by its _id and manages video progress updates.
 */
export function usePlaylist(playlistId) {
  const [playlist, setPlaylist] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [updatingVideoId, setUpdatingVideoId] = useState(null)

  useEffect(() => {
    if (!playlistId) return
    let cancelled = false

    const fetch = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await playlistService.getPlaylist(playlistId)
        if (!cancelled) setPlaylist(data)
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load playlist.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    fetch()
    return () => { cancelled = true }
  }, [playlistId])

  const toggleVideo = useCallback(async (videoId, completed) => {
    if (updatingVideoId) return // prevent double-click

    // Optimistic UI update
    setPlaylist((prev) => {
      if (!prev) return prev
      return {
        ...prev,
        videos: prev.videos.map((v) =>
          v.videoId === videoId ? { ...v, completed } : v
        ),
      }
    })
    setUpdatingVideoId(videoId)

    try {
      const updated = await playlistService.updateVideoProgress(playlistId, videoId, completed)
      setPlaylist(updated)
    } catch (err) {
      // Revert optimistic update on failure
      setPlaylist((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          videos: prev.videos.map((v) =>
            v.videoId === videoId ? { ...v, completed: !completed } : v
          ),
        }
      })
      console.error('Failed to update video progress:', err.message)
    } finally {
      setUpdatingVideoId(null)
    }
  }, [playlistId, updatingVideoId])

  const completedCount = playlist
    ? playlist.videos.filter((v) => v.completed).length
    : 0
  const progress = playlist && playlist.totalVideos > 0
    ? Math.round((completedCount / playlist.totalVideos) * 100)
    : 0

  return { playlist, loading, error, toggleVideo, updatingVideoId, completedCount, progress }
}
