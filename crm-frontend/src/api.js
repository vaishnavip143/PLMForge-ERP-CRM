import axios from 'axios'

// The Spring Boot backend runs on port 8080.
export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// The JWT is stored in memory here and sent with every request.
let token = null

export function setToken(value) {
  token = value || null
}

api.interceptors.request.use((config) => {
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Turns any Axios error into one sentence that can be shown on screen.
export function errorMessage(error) {
  if (!error.response) {
    return `Cannot reach the backend at ${API_BASE_URL}. Is Spring Boot running?`
  }

  const { data, status } = error.response

  if (data && data.validationErrors) {
    return Object.values(data.validationErrors).join(' ')
  }

  if (data && data.message) {
    return data.message
  }

  return `Request failed with status ${status}.`
}

export default api
