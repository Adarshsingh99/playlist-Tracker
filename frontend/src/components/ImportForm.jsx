import { useState } from 'react'
import Spinner from './Spinner'

/**
 * ImportForm — URL input + import button.
 * Calls onImport(url) and shows loading/error states.
 */
export default function ImportForm({ onImport }) {
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    const trimmed = url.trim()
    if (!trimmed) {
      setError('Please enter a YouTube playlist URL.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await onImport(trimmed)
      setUrl('')
    } catch (err) {
      setError(err.message || 'Unable to fetch playlist right now. Please try again later.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div className="flex flex-col sm:flex-row gap-3">
        <input
          type="url"
          value={url}
          onChange={(e) => {
            setUrl(e.target.value)
            if (error) setError(null)
          }}
          placeholder="https://www.youtube.com/playlist?list=PLxxxxxxxx"
          className="input-field flex-1"
          disabled={loading}
          aria-label="YouTube Playlist URL"
        />
        <button
          type="submit"
          disabled={loading || !url.trim()}
          className="btn-primary flex items-center justify-center gap-2 whitespace-nowrap"
        >
          {loading ? (
            <>
              <Spinner size="sm" />
              <span>Importing…</span>
            </>
          ) : (
            'Import Playlist'
          )}
        </button>
      </div>

      {loading && (
        <p className="text-sm text-gray-500 text-center animate-pulse">
          Fetching playlist… This may take a few seconds.
        </p>
      )}

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-3">
          <p className="text-sm text-red-700">⚠️ {error}</p>
        </div>
      )}
    </form>
  )
}
