import axios from 'axios'

// In development, Vite proxy forwards /api → http://localhost:8080
// In production, set VITE_API_BASE_URL to your deployed backend URL
const baseURL = import.meta.env.VITE_API_BASE_URL || ''

const api = axios.create({
  baseURL,
  withCredentials: true, // Required for the anonymous user cookie
  headers: {
    'Content-Type': 'application/json',
  },
})

// Response interceptor for consistent error messages
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      // Network error
      return Promise.reject({
        message: 'Something went wrong. Please check your internet connection.',
        isNetworkError: true,
      })
    }
    // Use backend message if available
    const message = error.response?.data?.message || 'An unexpected error occurred.'
    return Promise.reject({ message, status: error.response.status })
  }
)

export default api
