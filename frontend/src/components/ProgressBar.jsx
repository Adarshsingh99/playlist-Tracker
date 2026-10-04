/**
 * ProgressBar — displays a gradient fill bar with a percentage label.
 */
export default function ProgressBar({ progress }) {
  return (
    <div className="w-full">
      <div className="flex items-center justify-between mb-1.5">
        <span className="text-sm text-gray-500 font-medium">Progress</span>
        <span className="text-sm font-semibold text-blue-600">{progress}%</span>
      </div>
      <div className="progress-bar-track">
        <div
          className="progress-bar-fill"
          style={{ width: `${progress}%` }}
          role="progressbar"
          aria-valuenow={progress}
          aria-valuemin={0}
          aria-valuemax={100}
        />
      </div>
    </div>
  )
}
