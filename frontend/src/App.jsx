import { useState, useEffect } from 'react'
import { api } from './services/api'

function App() {
  const [loading, setLoading] = useState(true)
  const [healthData, setHealthData] = useState(null)
  const [error, setError] = useState(null)

  const verifyHealth = async () => {
    setLoading(true)
    setError(null)
    setHealthData(null)
    try {
      const data = await api.checkHealth()
      setHealthData(data)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    verifyHealth()
  }, [])

  return (
    <div className="container py-5 text-center">
      <div className="card shadow-sm p-4 mx-auto" style={{ maxWidth: '600px' }}>
        <h1 className="h3 mb-3 text-primary">SE2030 Dental Management System</h1>
        <h2 className="h6 mb-4 text-muted text-uppercase tracking-wider">Foundation Connectivity Check</h2>

        <div className="my-4">
          {loading && (
            <div className="d-flex flex-column align-items-center">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
              <p className="mt-3 text-muted">Checking connectivity to backend and database...</p>
            </div>
          )}

          {!loading && healthData && (
            <div className="alert alert-success d-flex flex-column align-items-center" role="alert">
              <h3 className="h5 alert-heading mb-2">✓ Connection Successful</h3>
              <hr className="w-100 my-2" />
              <div className="text-start w-100 mt-2">
                <div><strong>Server Status:</strong> <span className="badge bg-success">{healthData.status}</span></div>
                <div><strong>Database Status:</strong> <span className="badge bg-success">{healthData.database}</span></div>
                <div className="small text-muted mt-2">Checked at: {new Date(healthData.timestamp).toLocaleString()}</div>
              </div>
            </div>
          )}

          {!loading && error && (
            <div className="alert alert-danger d-flex flex-column align-items-center" role="alert">
              <h3 className="h5 alert-heading mb-2">✗ Connection Failed</h3>
              <p className="small mb-2">{error.message}</p>
              <hr className="w-100 my-2" />
              <div className="text-start w-100 mt-2">
                <div><strong>Error Code:</strong> <code>{error.error}</code></div>
                <div><strong>HTTP Status:</strong> <span className="badge bg-dark">{error.status}</span></div>
                {error.details && (
                  <div className="mt-1">
                    <strong>Details:</strong>
                    <pre className="small bg-light p-2 rounded mt-1 mb-0">{JSON.stringify(error.details, null, 2)}</pre>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        <div className="mt-4">
          <button 
            type="button" 
            className="btn btn-primary"
            onClick={verifyHealth}
            disabled={loading}
          >
            {loading ? 'Verifying...' : 'Re-verify Connection'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default App
