import { Link } from 'react-router-dom'
import ProgressBar from './ProgressBar'

/**
 * PlaylistCard — shown on the Home page in the "My Playlists" grid.
 */
export default function PlaylistCard({ playlist }) {
  const completedCount = playlist.videos
    ? playlist.videos.filter((v) => v.completed).length
    : 0
  const total = playlist.totalVideos || 0
  const progress = total > 0 ? Math.round((completedCount / total) * 100) : 0

  return (
    <Link
      to={`/playlist/${playlist.id}`}
      className="card p-5 flex gap-4 items-start hover:shadow-md hover:border-blue-100 transition-all duration-200 group"
    >
      {/* Thumbnail */}
      {playlist.thumbnail ? (
        <img
          src={playlist.thumbnail}
          alt={playlist.title}
          className="w-20 h-14 sm:w-24 sm:h-16 object-cover rounded-lg shrink-0 group-hover:scale-105 transition-transform duration-200"
        />
      ) : (
        <div className="w-20 h-14 sm:w-24 sm:h-16 bg-gray-200 rounded-lg shrink-0 flex items-center justify-center">
          <span className="text-2xl">▶️</span>
        </div>
      )}

      {/* Info */}
      <div className="flex-1 min-w-0">
        <h3 className="font-semibold text-gray-900 text-sm sm:text-base truncate group-hover:text-blue-600 transition-colors">
          {playlist.title}
        </h3>
        <p className="text-xs text-gray-500 mt-0.5">
          {completedCount} / {total} videos completed
        </p>
        <div className="mt-2">
          <ProgressBar progress={progress} />
        </div>
      </div>
    </Link>
  )
}
