import { Fragment, useEffect, useState } from 'react'
import api, { friendlyError } from './api'
import { Message } from './components'
import { formatDateTime, humanize } from './helpers'

export default function CustomerDashboard({ user }) {
  const [form, setForm] = useState({
    name: user.name,
    email: user.email,
    subject: '',
    description: '',
    phone: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState(null)
  const [ticket, setTicket] = useState(null)

  const [tickets, setTickets] = useState([])
  const [ticketsLoading, setTicketsLoading] = useState(true)
  const [ticketsError, setTicketsError] = useState('')
  const [selectedTicketId, setSelectedTicketId] = useState(null)

  async function loadMyTickets() {
    try {
      const response = await api.get('/api/customers/my-tickets')
      setTickets(response.data.data || [])
      setTicketsError('')
    } catch (error) {
      setTicketsError(friendlyError(error))
    } finally {
      setTicketsLoading(false)
    }
  }

  useEffect(() => {
    loadMyTickets()
  }, [])

  function updateField(event) {
    const { name, value } = event.target
    setForm({ ...form, [name]: value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setMessage(null)
    setTicket(null)

    if (!form.name.trim() || !form.email.trim() || !form.subject.trim() || !form.description.trim()) {
      setMessage({ type: 'error', text: 'Please complete name, email, subject and query message.' })
      return
    }

    const payload = {
      name: form.name.trim(),
      email: form.email.trim(),
      subject: form.subject.trim(),
      description: form.description.trim(),
    }
    if (form.phone.trim()) {
      payload.phone = form.phone.trim()
    }

    setSubmitting(true)
    try {
      const response = await api.post('/api/tickets/ingest', payload)
      const createdTicket = response.data.data.ticket
      setTicket(createdTicket)
      setMessage({ type: 'success', text: response.data.message })
      setForm({ ...form, subject: '', description: '', phone: '' })
      if (createdTicket) {
        setTickets((current) => [createdTicket, ...current.filter((t) => t.id !== createdTicket.id)])
      } else {
        loadMyTickets()
      }
    } catch (error) {
      setMessage({ type: 'error', text: friendlyError(error) })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <section className="card">
        <div className="card-header">
          <div>
            <h2>Submit a query</h2>
            <p className="hint">
              Sent to <code>POST /api/tickets/ingest</code>. A ticket with a 24-hour TAT is created.
              Authenticated customer submissions use your logged-in account identity.
            </p>
          </div>
        </div>

        {message && <Message type={message.type}>{message.text}</Message>}

        {ticket && (
          <div className="ticket-receipt">
            <div>
              <span className="muted small">Ticket</span>
              <div className="strong">{ticket.ticketNumber}</div>
            </div>
            <div>
              <span className="muted small">Status</span>
              <div className="strong">{ticket.status}</div>
            </div>
            <div>
              <span className="muted small">Remaining TAT</span>
              <div className="strong">{ticket.remainingTime}</div>
            </div>
            <div>
              <span className="muted small">Due</span>
              <div>{formatDateTime(ticket.dueAt)}</div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="name">Full name</label>
              <input id="name" name="name" value={form.name} onChange={updateField} />
            </div>

            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                value={form.email}
                onChange={updateField}
              />
            </div>

            <div className="field">
              <label htmlFor="subject">Subject</label>
              <input
                id="subject"
                name="subject"
                value={form.subject}
                onChange={updateField}
                placeholder="Short summary of your query"
              />
            </div>

            <div className="field">
              <label htmlFor="phone">
                Phone <span className="optional">(optional)</span>
              </label>
              <input
                id="phone"
                name="phone"
                value={form.phone}
                onChange={updateField}
                placeholder="+1-555-0100"
              />
            </div>

            <div className="field full">
              <label htmlFor="description">Query message</label>
              <textarea
                id="description"
                name="description"
                rows="5"
                value={form.description}
                onChange={updateField}
                placeholder="Describe your query"
              />
            </div>

            <div className="form-actions">
              <button className="btn btn-primary" type="submit" disabled={submitting}>
                {submitting ? 'Submitting…' : 'Submit query'}
              </button>
            </div>
          </div>
        </form>
      </section>

      <section className="card">
        <div className="card-header">
          <div>
            <h2>My tickets</h2>
            <p className="hint">View your own tickets, current status, and ticket details.</p>
          </div>
          <span className="count-pill">{tickets.length} tickets</span>
        </div>

        {ticketsError && <Message type="error">{ticketsError}</Message>}

        {ticketsLoading ? (
          <p className="empty">Loading your tickets…</p>
        ) : tickets.length === 0 ? (
          <p className="empty">You have not submitted any tickets yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Ticket</th>
                  <th>Subject</th>
                  <th>Status</th>
                  <th>Remaining TAT</th>
                  <th>Due</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((item) => (
                  <Fragment key={item.id}>
                    <tr>
                      <td>
                        <div className="strong">{item.ticketNumber}</div>
                        <div className="muted small">#{item.id}</div>
                      </td>
                      <td>
                        <div>{item.subject}</div>
                        <div className="muted small">{humanize(item.priority)} priority</div>
                      </td>
                      <td>{item.status}</td>
                      <td>{item.remainingTime || '—'}</td>
                      <td className="muted small">{formatDateTime(item.dueAt)}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary btn-small"
                          onClick={() =>
                            setSelectedTicketId(selectedTicketId === item.id ? null : item.id)
                          }
                        >
                          {selectedTicketId === item.id ? 'Hide details' : 'View details'}
                        </button>
                      </td>
                    </tr>
                    {selectedTicketId === item.id && (
                      <tr>
                        <td colSpan={6}>
                          <div style={{ padding: '0.5rem 0' }}>
                            <div><strong>Description:</strong> {item.description || '—'}</div>
                            <div><strong>Status:</strong> {item.status}</div>
                            <div><strong>Created:</strong> {formatDateTime(item.createdAt)}</div>
                            {item.status === 'RESOLVED' || item.status === 'CLOSED' ? (
                              <>
                                <div><strong>Resolved at:</strong> {formatDateTime(item.resolvedAt)}</div>
                                {item.resolutionNotes ? (
                                  <div><strong>Resolution message:</strong> {item.resolutionNotes}</div>
                                ) : (
                                  <div><strong>Resolution message:</strong> No resolution message recorded.</div>
                                )}
                                <div style={{ marginTop: '0.25rem', color: '#166534', fontWeight: '600' }}>
                                  This query has been resolved by the support team.
                                </div>
                              </>
                            ) : (
                              <>
                                <div><strong>Resolved at:</strong> {formatDateTime(item.resolvedAt) || '—'}</div>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
