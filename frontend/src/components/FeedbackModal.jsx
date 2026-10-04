import { useState } from 'react'
import { feedbackService } from '../services/feedbackService'
import Spinner from './Spinner'

export default function FeedbackModal({ isOpen, onClose }) {
  const [name, setName] = useState('')
  const [feedback, setFeedback] = useState('')
  const [status, setStatus] = useState('idle') // 'idle' | 'sending' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  if (!isOpen) return null

  const handleClose = () => {
    if (status === 'sending') return
    setName('')
    setFeedback('')
    setStatus('idle')
    setErrorMessage(null)
    setFieldErrors({})
    onClose()
  }

  const validate = () => {
    const errors = {}
    if (!name.trim()) {
      errors.name = 'Name cannot be empty.'
    }
    if (!feedback.trim()) {
      errors.feedback = 'Feedback cannot be empty.'
    }
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!validate()) {
      return
    }

    setStatus('sending')

    try {
      await feedbackService.submitFeedback({
        name: name.trim(),
        feedback: feedback.trim(),
      })
      setStatus('success')
      // Auto close after short delay
      setTimeout(() => {
        handleClose()
      }, 1800)
    } catch (err) {
      setStatus('error')
      setErrorMessage(err.message || 'Unable to send feedback. Please try again.')
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-fadeIn">
      <div className="card w-full max-w-md p-6 bg-white shadow-xl relative animate-scaleUp">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <span>💬</span> Send Feedback
          </h2>
          <button
            onClick={handleClose}
            disabled={status === 'sending'}
            className="text-gray-400 hover:text-gray-600 text-xl font-medium w-8 h-8 rounded-lg flex items-center justify-center hover:bg-gray-100 disabled:opacity-50"
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        {status === 'success' ? (
          <div className="py-8 text-center space-y-2 animate-fadeIn">
            <p className="text-3xl">✓</p>
            <p className="text-base font-semibold text-green-700">
              Thank you for your feedback!
            </p>
            <p className="text-xs text-gray-500">Closing automatically…</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Name field */}
            <div>
              <label htmlFor="feedbackName" className="block text-sm font-medium text-gray-700 mb-1">
                Name
              </label>
              <input
                id="feedbackName"
                type="text"
                value={name}
                onChange={(e) => {
                  setName(e.target.value)
                  if (fieldErrors.name) {
                    setFieldErrors((prev) => ({ ...prev, name: null }))
                  }
                }}
                placeholder="Your name"
                className={`input-field ${fieldErrors.name ? 'border-red-400 focus:ring-red-400' : ''}`}
                disabled={status === 'sending'}
              />
              {fieldErrors.name && (
                <p className="text-xs text-red-600 mt-1">⚠️ {fieldErrors.name}</p>
              )}
            </div>

            {/* Feedback textarea */}
            <div>
              <label htmlFor="feedbackText" className="block text-sm font-medium text-gray-700 mb-1">
                Your Feedback
              </label>
              <textarea
                id="feedbackText"
                rows={4}
                value={feedback}
                onChange={(e) => {
                  setFeedback(e.target.value)
                  if (fieldErrors.feedback) {
                    setFieldErrors((prev) => ({ ...prev, feedback: null }))
                  }
                }}
                placeholder="What do you think of Playlist Tracker? Tell us what you like or what to improve."
                className={`input-field resize-none ${fieldErrors.feedback ? 'border-red-400 focus:ring-red-400' : ''}`}
                disabled={status === 'sending'}
              />
              {fieldErrors.feedback && (
                <p className="text-xs text-red-600 mt-1">⚠️ {fieldErrors.feedback}</p>
              )}
            </div>

            {/* Error banner */}
            {status === 'error' && errorMessage && (
              <div className="rounded-xl border border-red-200 bg-red-50 p-3">
                <p className="text-xs text-red-700">⚠️ {errorMessage}</p>
              </div>
            )}

            {/* Submit button */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={status === 'sending'}
                className="btn-primary w-full text-sm flex items-center justify-center gap-2"
              >
                {status === 'sending' ? (
                  <>
                    <Spinner size="sm" />
                    <span>Sending…</span>
                  </>
                ) : (
                  'Send Feedback'
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
