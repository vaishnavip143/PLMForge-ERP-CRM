// The backend may return "ROLE_CUSTOMER" or "CUSTOMER".
export function normalizeRole(role) {
  return String(role || '').replace('ROLE_', '')
}

// "2026-11-13T18:25:00" -> readable local date and time
export function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

// "NEW_LEAD" -> "New lead", "SLA_WARNING" -> "SLA warning"
export function humanize(value) {
  const text = String(value || '').toLowerCase().replace(/_/g, ' ')
  const capitalized = text.charAt(0).toUpperCase() + text.slice(1)
  return capitalized.replace(/^Sla\b/, 'SLA')
}
