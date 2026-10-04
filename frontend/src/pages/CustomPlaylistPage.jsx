import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useCustomPlaylist } from '../hooks/useCustomPlaylists'
import VideoCard from '../components/VideoCard'
import ProgressBar from '../components/ProgressBar'
import Spinner from '../components/Spinner'
import ErrorMessage from '../components/ErrorMessage'

export default function CustomPlaylistPage() {
  const { playlistId } = useParams()
  const {
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
  } = useCustomPlaylist(playlistId)

  const [urlInput, setUrlInput] = useState('')
  const [showAddForm, setShowAddForm] = useState(false)
  const [formError, setFormError] = useState(null)
  const [deletingVideoId, setDeletingVideoId] = useState(null)

  const handleAddSubmit = async (e) => {
    e.preventDefault()
    const trimmed = urlInput.trim()
    if (!trimmed) {
      setFormError('Please enter a YouTube video or playlist URL.')
      return
    }

    setFormError(null)
    setAddResult(null)

    try {
      await addContent(trimmed)
      setUrlInput('')
    } catch (err) {
      setFormError(err.message || 'Unable to add content right now.')
    }
  }

  const handleDeleteVideo = async (videoId) => {
    if (!confirm('Remove this video from your custom playlist?')) return
    setDeletingVideoId(videoId)
    try {
      await removeVideo(videoId)
    } catch (err) {
      alert(err.message || 'Failed to remove video.')
    } finally {
      setDeletingVideoId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 gap-4">
        <Spinner size="lg" />
        <p className="text-gray-400 text-sm animate-pulse">Loading custom playlist…</p>
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

  const remaining = totalVideos - completedCount

  return (
    <main className="max-w-3xl mx-auto px-4 sm:px-6 py-8 space-y-6">
      {/* Back button */}
      <Link to="/" className="btn-secondary inline-flex items-center gap-1 text-sm">
        ← Back
      </Link>

      {/* Header card */}
      <section className="card p-6 space-y-5">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-2xl">📚</span>
              <h1 className="text-xl sm:text-2xl font-bold text-gray-900 leading-tight">
                {playlist.name}
              </h1>
            </div>
            <p className="text-xs text-blue-600 font-medium mt-1 uppercase tracking-wider">
              Custom Learning Playlist
            </p>
          </div>

          <button
            onClick={() => {
              setShowAddForm(!showAddForm)
              setFormError(null)
              setAddResult(null)
            }}
            className="btn-primary text-sm flex items-center gap-1.5 shrink-0"
          >
            <span>{showAddForm ? '✕ Close' : '+ Add Content'}</span>
          </button>
        </div>

        {/* Progress & stats */}
        <div className="space-y-3">
          <ProgressBar progress={progress} />

          <div className="grid grid-cols-3 gap-3 text-center">
            <div className="bg-gray-50 rounded-xl p-3">
              <p className="text-lg font-bold text-gray-900">{totalVideos}</p>
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

      {/* Add Content Form (Collapsible / Active) */}
      {showAddForm && (
        <section className="card p-6 border-blue-200 bg-blue-50/30 animate-fadeIn space-y-4">
          <div>
            <h2 className="text-base font-semibold text-gray-900">
              Add YouTube Video or Playlist
            </h2>
            <p className="text-xs text-gray-500 mt-0.5">
              Paste a single video URL or an entire playlist URL. Duplicates will be automatically prevented.
            </p>
          </div>

          <form onSubmit={handleAddSubmit} className="space-y-3">
            <div className="flex flex-col sm:flex-row gap-3">
              <input
                type="url"
                value={urlInput}
                onChange={(e) => {
                  setUrlInput(e.target.value)
                  if (formError) setFormError(null)
                }}
                placeholder="https://www.youtube.com/watch?v=... or playlist?list=..."
                className="input-field flex-1"
                disabled={addingContent}
                aria-label="YouTube video or playlist URL"
              />
              <button
                type="submit"
                disabled={addingContent || !urlInput.trim()}
                className="btn-primary flex items-center justify-center gap-2 whitespace-nowrap"
              >
                {addingContent ? (
                  <>
                    <Spinner size="sm" />
                    <span>Adding…</span>
                  </>
                ) : (
                  'Add'
                )}
              </button>
            </div>

            {addingContent && (
              <p className="text-sm text-blue-600 text-center animate-pulse font-medium">
                Fetching YouTube content… This may take a few seconds.
              </p>
            )}

            {formError && (
              <div className="rounded-xl border border-red-200 bg-red-50 p-3">
                <p className="text-sm text-red-700">⚠️ {formError}</p>
              </div>
            )}
          </form>
        </section>
      )}

      {/* Success / Result Notification Banner */}
      {addResult?.success && (
        <div className="rounded-xl border border-green-200 bg-green-50 p-4 flex items-start justify-between gap-3 animate-fadeIn">
          <div className="flex gap-2.5 items-start">
            <span className="text-green-600 text-lg">✅</span>
            <div>
              <p className="text-sm font-semibold text-green-900">
                {addResult.type === 'PLAYLIST'
                  ? 'Playlist Import Complete'
                  : 'Video Added'}
              </p>
              <p className="text-xs text-green-700 mt-0.5">
                {addResult.type === 'PLAYLIST' ? (
                  <>
                    <span className="font-semibold">{addResult.totalFound}</span> videos found: <span className="font-semibold">{addResult.addedCount}</span> new videos added, <span className="font-semibold">{addResult.alreadyExistedCount}</span> already existed.
                  </>
                ) : (
                  '1 video added to this playlist.'
                )}
              </p>
            </div>
          </div>
          <button
            onClick={() => setAddResult(null)}
            className="text-green-600 hover:text-green-800 text-sm font-bold"
          >
            ✕
          </button>
        </div>
      )}

      {/* Videos Section */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-700">
            Checklist ({totalVideos})
          </h2>
          {!showAddForm && (
            <button
              onClick={() => setShowAddForm(true)}
              className="text-xs text-blue-600 hover:underline font-medium"
            >
              + Add more videos
            </button>
          )}
        </div>

        {totalVideos === 0 ? (
          <div className="card p-10 text-center space-y-3">
            <p className="text-3xl">🎬</p>
            <p className="text-gray-600 text-sm font-medium">This custom playlist is empty.</p>
            <p className="text-gray-400 text-xs">
              Click &quot;+ Add Content&quot; above to add your first YouTube video or full playlist.
            </p>
            {!showAddForm && (
              <button
                onClick={() => setShowAddForm(true)}
                className="btn-primary text-sm inline-block mt-2"
              >
                + Add Content
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {playlist.videos.map((video) => (
              <div key={video.videoId} className="relative group">
                <VideoCard
                  video={video}
                  onToggle={toggleVideo}
                  isUpdating={updatingVideoId === video.videoId}
                />
                <button
                  onClick={() => handleDeleteVideo(video.videoId)}
                  disabled={deletingVideoId === video.videoId}
                  title="Remove video from custom playlist"
                  className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity text-gray-400 hover:text-red-500 p-1 rounded-lg hover:bg-gray-100 text-xs font-semibold"
                  aria-label="Remove video"
                >
                  {deletingVideoId === video.videoId ? '…' : '🗑️'}
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}
