/**
 * Normalized API error structure.
 */
export class ApiError extends Error {
  constructor(status, error, message, details = null) {
    super(message);
    this.status = status;
    this.error = error;
    this.details = details;
  }
}

/**
 * Normalized fetch wrapper.
 */
async function request(url, options = {}) {
  try {
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      if (data && typeof data.status === 'number') {
        // Structured error response from GlobalExceptionHandler
        throw new ApiError(data.status, data.error, data.message, data.details);
      } else {
        throw new ApiError(response.status, 'HTTP_ERROR', response.statusText || 'An unexpected HTTP error occurred');
      }
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    // Network/Connectivity failure
    throw new ApiError(
      503,
      'BACKEND_UNAVAILABLE',
      'Backend server is unreachable. Please verify that the backend application is running.'
    );
  }
}

/**
 * Service methods.
 */
export const api = {
  checkHealth: () => request('/api/health'),
};
