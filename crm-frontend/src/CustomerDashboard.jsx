import { useState } from 'react'
import api, { friendlyError } from './api'
import { Message } from './components'
import { formatDateTime } from './helpers'

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
      setTicket(response.data.data.ticket)
      setMessage({ type: 'success', text: response.data.message })
      setForm({ ...form, subject: '', description: '', phone: '' })
    } catch (error) {
      setMessage({ type: 'error', text: friendlyError(error) })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <div className="card-header">
        <div>
          <h2>Submit a query</h2>
          <p className="hint">
            Sent to <code>POST /api/tickets/ingest</code>. A ticket with a 24-hour TAT is created.
            Keep your account email to stay linked to the ticket; a new email becomes a potential
            customer.
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
  )
}
