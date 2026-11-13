import { useEffect, useState } from 'react'
import api, { errorMessage } from './api'
import { Message, TicketTable } from './components'
import { humanize } from './helpers'

const STATUS_OPTIONS = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

export default function SalesDashboard() {
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [selected, setSelected] = useState(null)
  const [form, setForm] = useState({ status: 'IN_PROGRESS', resolutionNotes: '' })
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState(null)

  async function loadTickets() {
    setLoading(true)
    setLoadError('')
    try {
      const response = await api.get('/api/sales/tickets')
      setTickets(response.data.data || [])
    } catch (error) {
      setLoadError(errorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTickets()
  }, [])

  function openUpdate(ticket) {
    setSelected(ticket)
    setForm({ status: ticket.status, resolutionNotes: ticket.resolutionNotes || '' })
    setMessage(null)
  }

  async function handleUpdate(event) {
    event.preventDefault()
    setSaving(true)
    setMessage(null)

    try {
      const response = await api.put(`/api/sales/tickets/${selected.id}/status`, {
        status: form.status,
        resolutionNotes: form.resolutionNotes.trim() || null,
      })
      const updated = response.data.data

      setTickets(tickets.map((ticket) => (ticket.id === updated.id ? updated : ticket)))
      setSelected(null)
      setMessage({
        type: 'success',
        text: `Ticket ${updated.ticketNumber} is now ${humanize(updated.status)}.`,
      })
    } catch (error) {
      setMessage({ type: 'error', text: errorMessage(error) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <section className="card">
        <div className="card-header">
          <div>
            <h2>Assigned tickets</h2>
            <p className="hint">
              Loaded from <code>GET /api/sales/tickets</code>. The backend returns only the tickets
              assigned to your account.
            </p>
          </div>
          <span className="count-pill">{tickets.length} assigned</span>
        </div>

        {message && <Message type={message.type}>{message.text}</Message>}
        {loadError && <Message type="error">{loadError}</Message>}

        {loading ? (
          <p className="empty">Loading assigned tickets…</p>
        ) : (
          <TicketTable
            tickets={tickets}
            emptyText="No tickets are assigned to you right now."
            renderActions={(ticket) => (
              <button
                type="button"
                className="btn btn-secondary btn-small"
                onClick={() => openUpdate(ticket)}
              >
                Update status
              </button>
            )}
          />
        )}
      </section>

      {selected && (
        <section className="card update-panel">
          <div className="card-header">
            <div>
              <h2>Update {selected.ticketNumber}</h2>
              <p className="hint">
                {selected.subject} · {selected.customerName || selected.potentialCustomerName}
              </p>
            </div>
            <button
              type="button"
              className="btn btn-ghost btn-small"
              onClick={() => setSelected(null)}
            >
              Cancel
            </button>
          </div>

          <form onSubmit={handleUpdate}>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="ticket-status">Ticket status</label>
                <select
                  id="ticket-status"
                  value={form.status}
                  onChange={(event) => setForm({ ...form, status: event.target.value })}
                >
                  {STATUS_OPTIONS.map((status) => (
                    <option key={status} value={status}>
                      {humanize(status)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field full">
                <label htmlFor="resolution-notes">
                  Resolution notes <span className="optional">(optional)</span>
                </label>
                <textarea
                  id="resolution-notes"
                  rows="3"
                  value={form.resolutionNotes}
                  onChange={(event) => setForm({ ...form, resolutionNotes: event.target.value })}
                  placeholder="What was done to resolve the query?"
                />
              </div>

              <div className="form-actions">
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  {saving ? 'Saving…' : 'Save status'}
                </button>
                <span className="hint-text">
                  Sent to <code>PUT /api/sales/tickets/{selected.id}/status</code>.
                </span>
              </div>
            </div>
          </form>
        </section>
      )}
    </>
  )
}
