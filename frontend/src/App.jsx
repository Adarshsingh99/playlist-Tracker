import { Routes, Route } from 'react-router-dom'
import Header from './components/Header'
import HomePage from './pages/HomePage'
import PlaylistPage from './pages/PlaylistPage'
import CustomPlaylistPage from './pages/CustomPlaylistPage'

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/playlist/:playlistId" element={<PlaylistPage />} />
        <Route path="/custom-playlist/:playlistId" element={<CustomPlaylistPage />} />
        {/* Catch-all */}
        <Route
          path="*"
          element={
            <div className="max-w-3xl mx-auto px-4 py-24 text-center space-y-4">
              <p className="text-5xl">🤔</p>
              <h1 className="text-2xl font-bold text-gray-900">Page Not Found</h1>
              <a href="/" className="btn-primary inline-block">Go Home</a>
            </div>
          }
        />
      </Routes>
    </div>
  )
}
