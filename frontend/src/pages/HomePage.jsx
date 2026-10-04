import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePlaylists } from '../hooks/usePlaylists'
import { useCustomPlaylists } from '../hooks/useCustomPlaylists'
import { playlistService } from '../services/playlistService'
import ImportForm from '../components/ImportForm'
import PlaylistCard from '../components/PlaylistCard'
import CustomPlaylistCard from '../components/CustomPlaylistCard'
import CreatePlaylistModal from '../components/CreatePlaylistModal'
import Spinner from '../components/Spinner'
import ErrorMessage from '../components/ErrorMessage'

export default function HomePage() {
  const navigate = useNavigate()
  const { playlists, loading: loadingPlaylists, error: errorPlaylists, refetch: refetchPlaylists } = usePlaylists()
  const {
    customPlaylists,
    loading: loadingCustom,
    error: errorCustom,
    refetch: refetchCustom,
    createPlaylist,
  } = useCustomPlaylists()

  const [isModalOpen, setIsModalOpen] = useState(false)

  const handleImport = async (url) => {
    const imported = await playlistService.importPlaylist(url)
    navigate(`/playlist/${imported.id}`)
  }

  const handleCreateCustom = async (name) => {
    const created = await createPlaylist(name)
    navigate(`/custom-playlist/${created.id}`)
  }

  return (
    <main className="max-w-3xl mx-auto px-4 sm:px-6 py-10 space-y-10">
      {/* Hero */}
      <section className="text-center space-y-2">
        <h1 className="text-3xl sm:text-4xl font-bold text-gray-900">
          Track Your YouTube Learning
        </h1>
        <p className="text-gray-500 text-sm sm:text-base">
          Import complete playlists or build your own custom multi-source learning paths.
        </p>
      </section>

      {/* V1: Import YouTube Playlist */}
      <section className="card p-6 space-y-2">
        <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">
          Import YouTube Playlist
        </h2>
        <ImportForm onImport={handleImport} />
      </section>

      {/* V2: Custom Learning Playlists */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
              <span>📚</span> Custom Learning Playlists
            </h2>
            <p className="text-xs text-gray-500">
              Combine videos and playlists from any channel into one checklist
            </p>
          </div>
          <button
            onClick={() => setIsModalOpen(true)}
            className="btn-primary text-sm py-2 px-4 flex items-center gap-1.5 shrink-0"
          >
            <span>+ Create Playlist</span>
          </button>
        </div>

        {loadingCustom ? (
          <div className="flex justify-center py-8">
            <Spinner size="md" />
          </div>
        ) : errorCustom ? (
          <ErrorMessage message={errorCustom} onRetry={refetchCustom} />
        ) : customPlaylists.length === 0 ? (
          <div className="card p-8 text-center border-dashed border-2 border-gray-200 space-y-2">
            <p className="text-sm font-medium text-gray-600">No custom playlists yet</p>
            <p className="text-xs text-gray-400">
              Create a custom playlist to mix videos from multiple channels and topics.
            </p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="btn-secondary text-xs inline-flex items-center gap-1 mt-2"
            >
              + Create your first playlist
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {customPlaylists.map((cp) => (
              <CustomPlaylistCard key={cp.id} playlist={cp} />
            ))}
          </div>
        )}
      </section>

      {/* V1: Imported Playlists */}
      <section className="space-y-4 pt-2">
        <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
          <span>▶️</span> Imported Playlists
        </h2>

        {loadingPlaylists ? (
          <div className="flex justify-center py-8">
            <Spinner size="md" />
          </div>
        ) : errorPlaylists ? (
          <ErrorMessage message={errorPlaylists} onRetry={refetchPlaylists} />
        ) : playlists.length === 0 ? (
          <div className="card p-8 text-center space-y-1">
            <p className="text-gray-500 text-sm">You haven&apos;t imported any playlists yet.</p>
            <p className="text-gray-400 text-xs">
              Paste a public YouTube playlist URL in the box above to get started.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4">
            {playlists.map((playlist) => (
              <PlaylistCard key={playlist.id} playlist={playlist} />
            ))}
          </div>
        )}
      </section>

      {/* Create Playlist Modal */}
      <CreatePlaylistModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onCreate={handleCreateCustom}
      />

      {/* Anonymous user notice */}
      <footer className="text-center pt-4">
        <p className="text-xs text-gray-400">
          ⚠️ Your progress is tied to this browser. Clearing cookies will reset your data.
        </p>
      </footer>
    </main>
  )
}
