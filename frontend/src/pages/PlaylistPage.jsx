import { useParams, Link } from 'react-router-dom'
import { usePlaylist } from '../hooks/usePlaylists'
import VideoCard from '../components/VideoCard'
import ProgressBar from '../components/ProgressBar'
import Spinner from '../components/Spinner'
import ErrorMessage from '../components/ErrorMessage'

export default function PlaylistPage() {
  const { playlistId } = useParams()
  const {
    playlist,
    loading,
    error,
    toggleVideo,
    updatingVideoId,
    completedCount,
    progress,
  } = usePlaylist(playlistId)

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 gap-4">
        <Spinner size="lg" />
        <p className="text-gray-400 text-sm animate-pulse">Loading playlist…</p>
      </div>
    )
  }

  if (error) {
    return (
      <main className="max-w-3xl mx-auto px-4 sm:px-6 py-10">
        <Link to="/" className="btn-secondary inline-flex items-center gap-1 mb-6">
          ← Back
        </Link>
        <ErrorMessage message={error} />
      </main>
    )
  }

  if (!playlist) return null

  const remaining = playlist.totalVideos - completedCount

  return (
    <main className="max-w-3xl mx-auto px-4 sm:px-6 py-8 space-y-6">
      {/* Back */}
      <Link to="/" className="btn-secondary inline-flex items-center gap-1 text-sm">
        ← Back
      </Link>

      {/* Playlist header */}
      <section className="card p-6 space-y-5">
        <div className="flex gap-4 items-start">
          {/* Playlist thumbnail */}
          {playlist.thumbnail && (
            <img
              src={playlist.thumbnail}
              alt={playlist.title}
              className="w-24 h-16 sm:w-32 sm:h-20 object-cover rounded-xl shrink-0"
            />
          )}
          <div className="flex-1 min-w-0">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 leading-tight">
              {playlist.title}
            </h1>
            <p className="text-sm text-gray-500 mt-1">
              {playlist.totalVideos} Videos
            </p>
          </div>
        </div>

        {/* Progress section */}
        <div className="space-y-3">
          <ProgressBar progress={progress} />

          {/* Stats row */}
          <div className="grid grid-cols-3 gap-3 text-center">
            <div className="bg-gray-50 rounded-xl p-3">
              <p className="text-lg font-bold text-gray-900">{playlist.totalVideos}</p>
              <p className="text-xs text-gray-500">Total</p>
            </div>
            <div className="bg-green-50 rounded-xl p-3">
              <p className="text-lg font-bold text-green-600">{completedCount}</p>
              <p className="text-xs text-green-600">Completed</p>
            </div>
            <div className="bg-blue-50 rounded-xl p-3">
              <p className="text-lg font-bold text-blue-600">{remaining}</p>
              <p className="text-xs text-blue-600">Remaining</p>
            </div>
          </div>
        </div>
      </section>

      {/* Video list */}
      <section className="space-y-3">
        <h2 className="text-base font-semibold text-gray-700">
          Videos
        </h2>
        {playlist.videos && playlist.videos.length > 0 ? (
          playlist.videos.map((video) => (
            <VideoCard
              key={video.videoId}
              video={video}
              onToggle={toggleVideo}
              isUpdating={updatingVideoId === video.videoId}
            />
          ))
        ) : (
          <div className="card p-8 text-center">
            <p className="text-gray-400 text-sm">No videos found in this playlist.</p>
          </div>
        )}
      </section>
    </main>
  )
}
