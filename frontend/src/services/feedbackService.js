import api from './api'

export const feedbackService = {
  /**
   * Submit user feedback.
   */
  submitFeedback: (data) =>
    api.post('/api/feedback', data).then((r) => r.data),
}
