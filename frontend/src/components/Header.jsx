import { useState } from 'react'
import { Link } from 'react-router-dom'
import FeedbackModal from './FeedbackModal'

/**
 * Header — top navigation bar with logo and Feedback button.
 */
export default function Header() {
  const [isFeedbackOpen, setIsFeedbackOpen] = useState(false)

  return (
    <>
      <header className="bg-white border-b border-gray-100 sticky top-0 z-10">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 py-3.5 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 group">
            <span className="text-2xl">▶️</span>
            <span className="text-xl font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
              Playlist Tracker
            </span>
          </Link>

          <button
            onClick={() => setIsFeedbackOpen(true)}
            className="text-xs sm:text-sm font-medium text-gray-600 hover:text-blue-600 bg-gray-50 hover:bg-blue-50 border border-gray-200 hover:border-blue-200 px-3 py-1.5 rounded-xl transition-all duration-200 flex items-center gap-1.5"
          >
            <span>💬</span>
            <span>Feedback</span>
          </button>
        </div>
      </header>

      <FeedbackModal
        isOpen={isFeedbackOpen}
        onClose={() => setIsFeedbackOpen(false)}
      />
    </>
  )
}
