import { useState, useEffect, useCallback } from 'react'
import { customPlaylistService } from '../services/customPlaylistService'

/**
 * Hook to manage list of custom playlists.
 */
export function useCustomPlaylists() {
  const [customPlaylists, setCustomPlaylists] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchPlaylists = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await customPlaylistService.getUserPlaylists()
      setCustomPlaylists(data)
    } catch (err) {
      setError(err.message || 'Failed to load custom playlists.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchPlaylists()
  }, [fetchPlaylists])

  const createPlaylist = async (name) => {
    const created = await customPlaylistService.createPlaylist(name)
    setCustomPlaylists((prev) => [created, ...prev])
    return created
  }

  const deletePlaylist = async (id) => {
    await customPlaylistService.deletePlaylist(id)
    setCustomPlaylists((prev) => prev.filter((p) => p.id !== id))
  }

  return {
    customPlaylists,
    loading,
    error,
    refetch: fetchPlaylists,
    createPlaylist,
    deletePlaylist,
  }
}

/**
 * Hook to manage a single custom playlist and its video checklist.
 */
export function useCustomPlaylist(playlistId) {
  const [playlist, setPlaylist] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [updatingVideoId, setUpdatingVideoId] = useState(null)
  const [addingContent, setAddingContent] = useState(false)
  const [addResult, setAddResult] = useState(null)

  const fetchPlaylist = useCallback(async () => {
    if (!playlistId) return
    setLoading(true)
    setError(null)
    try {
      const data = await customPlaylistService.getPlaylist(playlistId)
      setPlaylist(data)
    } catch (err) {
      setError(err.message || 'Failed to load custom playlist.')
    } finally {
      setLoading(false)
    }
  }, [playlistId])

  useEffect(() => {
    fetchPlaylist()
  }, [fetchPlaylist])

  const toggleVideo = useCallback(
    async (videoId, completed) => {
      if (updatingVideoId) return

      // Optimistic UI update
      setPlaylist((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          videos: (prev.videos || []).map((v) =>
            v.videoId === videoId ? { ...v, completed } : v
          ),
        }
      })
      setUpdatingVideoId(videoId)

      try {
        const updated = await customPlaylistService.updateVideoProgress(
          playlistId,
          videoId,
          completed
        )
        setPlaylist(updated)
      } catch (err) {
        // Revert optimistic update on failure
        setPlaylist((prev) => {
          if (!prev) return prev
          return {
            ...prev,
            videos: (prev.videos || []).map((v) =>
              v.videoId === videoId ? { ...v, completed: !completed } : v
            ),
          }
        })
        console.error('Failed to update video progress:', err.message)
      } finally {
        setUpdatingVideoId(null)
      }
    },
    [playlistId, updatingVideoId]
  )

  const addContent = async (url) => {
    setAddingContent(true)
    setAddResult(null)
    try {
      const res = await customPlaylistService.addContent(playlistId, url)
      setPlaylist(res.playlist)
      setAddResult({
        success: true,
        type: res.contentType,
        totalFound: res.totalFound,
        addedCount: res.addedCount,
        alreadyExistedCount: res.alreadyExistedCount,
        message: res.message,
      })
      return res
    } catch (err) {
      setAddResult({
        success: false,
        message: err.message || 'Unable to add content right now.',
      })
      throw err
    } finally {
      setAddingContent(false)
    }
  }

  const removeVideo = async (videoId) => {
    const updated = await customPlaylistService.deleteVideo(playlistId, videoId)
    setPlaylist(updated)
  }

  const videos = playlist?.videos || []
  const totalVideos = videos.length
  const completedCount = videos.filter((v) => v.completed).length
  const progress = totalVideos > 0 ? Math.round((completedCount / totalVideos) * 100) : 0

  return {
    playlist,
    loading,
    error,
    toggleVideo,
    updatingVideoId,
    addContent,
    addingContent,
    addResult,
    setAddResult,
    removeVideo,
    totalVideos,
    completedCount,
    progress,
    refetch: fetchPlaylist,
  }
}
