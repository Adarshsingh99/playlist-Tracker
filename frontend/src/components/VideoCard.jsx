import Spinner from './Spinner'

/**
 * VideoCard — a single video row with thumbnail, metadata, and completion checkbox.
 * Clicking the thumbnail or title opens the YouTube video in a new tab.
 * Clicking the checkbox toggles completion.
 */
export default function VideoCard({ video, onToggle, isUpdating }) {
  const youtubeUrl = `https://www.youtube.com/watch?v=${video.videoId}`

  return (
    <div
      className={`card p-4 flex gap-4 transition-all duration-200 ${
        video.completed ? 'opacity-60' : ''
      }`}
    >
      {/* Thumbnail */}
      <a
        href={youtubeUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="shrink-0 group"
        tabIndex={-1}
        aria-label={`Watch ${video.title} on YouTube`}
      >
        {video.thumbnail ? (
          <img
            src={video.thumbnail}
            alt={video.title}
            className="w-28 h-16 sm:w-36 sm:h-20 object-cover rounded-lg group-hover:opacity-80 transition-opacity"
          />
        ) : (
          <div className="w-28 h-16 sm:w-36 sm:h-20 bg-gray-200 rounded-lg flex items-center justify-center">
            <span className="text-gray-400 text-xl">▶</span>
          </div>
        )}
      </a>

      {/* Info */}
      <div className="flex-1 min-w-0 flex flex-col justify-between">
        <div>
          {/* Position + Title */}
          <a
            href={youtubeUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="group"
          >
            <p
              className={`text-sm sm:text-base font-medium leading-snug group-hover:text-blue-600 transition-colors ${
                video.completed ? 'line-through text-gray-400' : 'text-gray-900'
              }`}
            >
              <span className="text-gray-400 mr-1">
                {String(video.position).padStart(2, '0')}.
              </span>
              {video.title}
            </p>
          </a>

          {/* Duration */}
          {video.duration && (
            <p className="text-xs text-gray-400 mt-1">{video.duration}</p>
          )}
        </div>

        {/* Completion checkbox */}
        <div className="flex items-center gap-2 mt-3">
          {isUpdating ? (
            <Spinner size="sm" />
          ) : (
            <input
              type="checkbox"
              id={`video-${video.videoId}`}
              checked={video.completed}
              onChange={(e) => onToggle(video.videoId, e.target.checked)}
              className="w-4 h-4 rounded accent-blue-600 cursor-pointer"
              aria-label={`Mark "${video.title}" as ${video.completed ? 'incomplete' : 'complete'}`}
            />
          )}
          <label
            htmlFor={`video-${video.videoId}`}
            className={`text-sm cursor-pointer select-none ${
              video.completed ? 'text-green-600 font-medium' : 'text-gray-500'
            }`}
          >
            {video.completed ? '✅ Completed' : 'Mark complete'}
          </label>
        </div>
      </div>
    </div>
  )
}
