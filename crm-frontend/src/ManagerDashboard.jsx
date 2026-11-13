import { useEffect, useState } from 'react'
import api, { errorMessage } from './api'
import { Message, TicketTable } from './components'
import { formatDateTime, humanize } from './helpers'

export default function ManagerDashboard() {
  const [tickets, setTickets] = useState([])
  const [ticketsLoading, setTicketsLoading] = useState(true)
  const [ticketsError, setTicketsError] = useState('')

  const [leads, setLeads] = useState([])
  const [leadsLoading, setLeadsLoading] = useState(true)
  const [leadsError, setLeadsError] = useState('')

  const [notifications, setNotifications] = useState([])
  const [notificationsLoading, setNotificationsLoading] = useState(true)
  const [notificationsError, setNotificationsError] = useState('')

  const [sla, setSla] = useState(null)
  const [slaLoading, setSlaLoading] = useState(true)
  const [slaError, setSlaError] = useState('')

  const [markingId, setMarkingId] = useState(null)
  const [message, setMessage] = useState(null)

  async function loadTickets() {
    try {
      const response = await api.get('/api/manager/tickets')
      setTickets(response.data.data || [])
      setTicketsError('')
    } catch (error) {
      setTicketsError(errorMessage(error))
    } finally {
      setTicketsLoading(false)
    }
  }

  async function loadLeads() {
    try {
      const response = await api.get('/api/manager/leads')
      setLeads(response.data.data || [])
      setLeadsError('')
    } catch (error) {
      setLeadsError(errorMessage(error))
    } finally {
      setLeadsLoading(false)
    }
  }

  async function loadNotifications() {
    try {
      const response = await api.get('/api/manager/notifications')
      setNotifications(response.data.data || [])
      setNotificationsError('')
    } catch (error) {
      setNotificationsError(errorMessage(error))
    } finally {
      setNotificationsLoading(false)
    }
  }

  async function loadSlaAlerts() {
    try {
      const response = await api.get('/api/manager/sla-alerts')
      setSla(response.data.data)
      setSlaError('')
    } catch (error) {
      setSlaError(errorMessage(error))
    } finally {
      setSlaLoading(false)
    }
  }

  useEffect(() => {
    loadTickets()
    loadLeads()
    loadNotifications()
    loadSlaAlerts()
  }, [])

  async function markAsRead(notification) {
    setMarkingId(notification.id)
    setMessage(null)
    try {
      const response = await api.patch(`/api/notifications/${notification.id}/read`)
      const updated = response.data.data
      setNotifications(
        notifications.map((item) => (item.id === updated.id ? updated : item)),
      )
      setMessage({ type: 'success', text: 'Notification marked as read.' })
    } catch (error) {
      setMessage({ type: 'error', text: errorMessage(error) })
    } finally {
      setMarkingId(null)
    }
  }

  const unreadCount = notifications.filter((notification) => !notification.read).length

  return (
    <>
      <section className="card">
        <div className="card-header">
          <div>
            <h2>Standard Tickets</h2>
            <p className="hint">All tickets in the system with their current status.</p>
          </div>
          <span className="count-pill">{tickets.length} tickets</span>
        </div>

        {ticketsError && <Message type="error">{ticketsError}</Message>}

        {ticketsLoading ? (
          <p className="empty">Loading tickets…</p>
        ) : (
          <TicketTable tickets={tickets} emptyText="No tickets found." showAssigned />
        )}
      </section>

      <section className="card">
        <div className="card-header">
          <div>
            <h2>Potential Customers</h2>
            <p className="hint">Leads captured from queries sent by unknown email addresses.</p>
          </div>
          <span className="count-pill">{leads.length} leads</span>
        </div>

        {leadsError && <Message type="error">{leadsError}</Message>}

        {leadsLoading ? (
          <p className="empty">Loading potential customers…</p>
        ) : leads.length === 0 ? (
          <p className="empty">No potential customers yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Source</th>
                  <th>Status</th>
                  <th>Captured</th>
                </tr>
              </thead>
              <tbody>
                {leads.map((lead) => (
                  <tr key={lead.id}>
                    <td className="strong">{lead.name}</td>
                    <td>{lead.email}</td>
                    <td>{lead.phone || '—'}</td>
                    <td>{humanize(lead.source)}</td>
                    <td>
                      <span className="badge badge-lead">{humanize(lead.status)}</span>
                    </td>
                    <td className="muted small">{formatDateTime(lead.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <div className="card-header">
          <div>
            <h2>Alerts Panel</h2>
            <p className="hint">New lead notifications and tickets approaching their SLA deadline.</p>
          </div>
          <span className="count-pill">{unreadCount} unread</span>
        </div>

        {message && <Message type={message.type}>{message.text}</Message>}

        <div className="subsection">
          <h3>New lead &amp; SLA notifications</h3>

          {notificationsError && <Message type="error">{notificationsError}</Message>}

          {notificationsLoading ? (
            <p className="empty">Loading notifications…</p>
          ) : notifications.length === 0 ? (
            <p className="empty">No notifications yet.</p>
          ) : (
            <ul className="notification-list">
              {notifications.map((notification) => (
                <li
                  key={notification.id}
                  className={`notification ${notification.read ? 'read' : ''}`}
                >
                  <div>
                    <div className="notification-title">
                      {!notification.read && <span className="unread-dot" />}
                      <span className="strong">{humanize(notification.type)}</span>
                      {notification.ticketNumber && (
                        <span className="muted small">· {notification.ticketNumber}</span>
                      )}
                    </div>
                    <p className="notification-message">{notification.message}</p>
                    <span className="muted small">
                      {formatDateTime(notification.createdAt)} ·{' '}
                      {notification.read ? 'Read' : 'Unread'}
                    </span>
                  </div>

                  {!notification.read && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-small"
                      disabled={markingId === notification.id}
                      onClick={() => markAsRead(notification)}
                    >
                      {markingId === notification.id ? 'Marking…' : 'Mark as read'}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="subsection">
          <h3>Tickets approaching SLA breach</h3>

          {slaError && <Message type="error">{slaError}</Message>}

          {slaLoading ? (
            <p className="empty">Loading SLA alerts…</p>
          ) : (
            <>
              <div className="sla-summary">
                <span className="badge badge-in-progress">
                  {sla?.warningCount ?? 0} near breach
                </span>
                <span className="badge badge-closed">{sla?.overdueCount ?? 0} overdue</span>
                <span className="muted small">Generated {formatDateTime(sla?.generatedAt)}</span>
              </div>

              <h4 className="mini-heading">Less than 2 hours remaining</h4>
              <TicketTable
                tickets={sla?.warningTickets || []}
                emptyText="No tickets are close to breaching SLA."
                showAssigned
              />

              <h4 className="mini-heading">Overdue</h4>
              <TicketTable
                tickets={sla?.overdueTickets || []}
                emptyText="No overdue tickets."
                showAssigned
              />
            </>
          )}
        </div>
      </section>
    </>
  )
}
