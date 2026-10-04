import { Link } from 'react-router-dom'
import ProgressBar from './ProgressBar'

/**
 * CustomPlaylistCard — shown in "My Playlists" section.
 */
export default function CustomPlaylistCard({ playlist, onDelete }) {
  const videos = playlist.videos || []
  const total = videos.length
  const completedCount = videos.filter((v) => v.completed).length
  const progress = total > 0 ? Math.round((completedCount / total) * 100) : 0

  return (
    <div className="card p-5 relative group hover:shadow-md hover:border-blue-100 transition-all duration-200">
      <Link
        to={`/custom-playlist/${playlist.id}`}
        className="block"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xl">📚</span>
            <h3 className="font-semibold text-gray-900 text-base sm:text-lg group-hover:text-blue-600 transition-colors">
              {playlist.name}
            </h3>
          </div>
          <span className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full font-medium">
            Custom
          </span>
        </div>

        <p className="text-xs text-gray-500 mt-2">
          {completedCount} / {total} completed
        </p>

        <div className="mt-2">
          <ProgressBar progress={progress} />
        </div>
      </Link>
    </div>
  )
}
