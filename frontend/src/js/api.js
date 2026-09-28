/**
 * FestPass API Client
 * Centralized Fetch API utility communicating with Spring Boot backend on http://localhost:8080
 */

// If served via Vite dev server proxy on port 5173, use relative path to prevent CORS; otherwise use http://localhost:8080
const BASE_URL = window.location.port === '5173' ? '' : 'http://localhost:8080';

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const defaultHeaders = {
    'Accept': 'application/json',
  };

  if (options.body && typeof options.body === 'string') {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  let response;
  try {
    response = await fetch(url, config);
  } catch (networkError) {
    throw new Error('Backend server is not running or unreachable at http://localhost:8080.');
  }

  if (response.status === 204) {
    return null;
  }

  let data = null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch (e) {
      data = null;
    }
  } else {
    try {
      const text = await response.text();
      data = text ? { message: text } : null;
    } catch (e) {
      data = null;
    }
  }

  if (!response.ok) {
    let errorMessage = 'An unexpected server error occurred.';
    let validationErrors = null;

    if (data) {
      if (data.validationErrors && Object.keys(data.validationErrors).length > 0) {
        validationErrors = data.validationErrors;
        errorMessage = Object.values(data.validationErrors).join('. ');
      } else if (data.message) {
        errorMessage = data.message;
      } else if (data.error) {
        errorMessage = data.error;
      }
    } else {
      errorMessage = `Server returned status ${response.status} (${response.statusText})`;
    }

    const error = new Error(errorMessage);
    error.status = response.status;
    error.validationErrors = validationErrors;
    error.data = data;
    throw error;
  }

  return data;
}

export const api = {
  // Events API
  getAllEvents: () => request('/api/events'),
  getEventById: (id) => request(`/api/events/${id}`),
  createEvent: (data) =>
    request('/api/events', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  updateEvent: (id, data) =>
    request(`/api/events/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  deleteEvent: (id) =>
    request(`/api/events/${id}`, {
      method: 'DELETE',
    }),
  getEventAttendance: (id) => request(`/api/events/${id}/attendance`),

  // Tickets & QR Check-in API
  issueTicket: (data) =>
    request('/api/tickets/issue', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  checkInTicket: (qrCode) =>
    request('/api/tickets/check-in', {
      method: 'POST',
      body: JSON.stringify({ qrCode }),
    }),
  getTicketById: (id) => request(`/api/tickets/${id}`),
};
