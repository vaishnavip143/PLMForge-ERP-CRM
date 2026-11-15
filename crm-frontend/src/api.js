import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080"

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Module-level on purpose: the token stays in memory, the session copy lives in App.jsx.
let token = null

export function setToken(value) {
  token = value || null
}

api.interceptors.request.use((config) => {
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Turns whatever Axios threw into one sentence we can put in a Message.
export function friendlyError(error) {
  if (!error.response) {
    return `Cannot reach the backend at ${API_BASE_URL}. Is Spring Boot running?`
  }

  const { data, status } = error.response

  // Bean validation hands back a field -> message map; flatten it to one line.
  if (data && data.validationErrors) {
    return Object.values(data.validationErrors).join(' ')
  }

  if (data && data.message) {
    return data.message
  }

  return `Request failed with status ${status}.`
}

export default api
