/**
 * ErrorMessage — displays an error alert with an icon.
 */
export default function ErrorMessage({ message, onRetry }) {
  if (!message) return null
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4 flex gap-3 items-start">
      <span className="text-red-500 text-xl shrink-0">⚠️</span>
      <div className="flex-1">
        <p className="text-sm text-red-700 font-medium">{message}</p>
        {onRetry && (
          <button
            onClick={onRetry}
            className="mt-2 text-xs text-red-600 underline hover:text-red-800"
          >
            Try again
          </button>
        )}
      </div>
    </div>
  )
}
