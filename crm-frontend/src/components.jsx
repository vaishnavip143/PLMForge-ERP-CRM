import { humanize } from './helpers'

export function Message({ type = 'info', children }) {
  if (!children) return null

  return (
    <div className={`message message-${type}`} role={type === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}

/**
 * Dashboard layout: sidebar with the signed-in user, the role simulator
 * buttons and the page content on the right.
 */
export function Layout({ user, role, title, subtitle, roleOptions, onChangeRole, onLogout, children }) {
  const roleLabel = roleOptions.find((option) => option.role === role)?.title || role

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div>
          <div className="brand">
            PLMForge <span>CRM</span>
          </div>
          <p className="sidebar-tagline">Role simulator</p>
        </div>

        <div>
          <p className="sidebar-label">Signed in as</p>
          <div className="user-box">
            <strong>{user.name}</strong>
            <span>{user.email}</span>
            <span className="user-role">{roleLabel}</span>
          </div>
        </div>

        <div>
          <p className="sidebar-label">Switch role</p>
          <div className="role-list">
            {roleOptions.map((option) => (
              <button
                key={option.role}
                type="button"
                className={`role-switch ${option.role === role ? 'active' : ''}`}
                onClick={() => onChangeRole(option.role)}
                disabled={option.role === role}
              >
                <span>{option.title}</span>
                {option.role === role && <span className="current">Current</span>}
              </button>
            ))}
          </div>
          <p className="sidebar-note">
            Changing role signs you out. Sign in with that role&apos;s own account — the JWT returned
            by the backend decides which dashboard opens.
          </p>
        </div>

        <div className="sidebar-footer">
          <button type="button" className="btn btn-ghost" onClick={onLogout}>
            Sign out
          </button>
        </div>
      </aside>

      <main className="main">
        <header className="page-header">
          <div>
            <h1>{title}</h1>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <span className="role-badge">{roleLabel}</span>
        </header>
        {children}
      </main>
    </div>
  )
}

function StatusBadge({ status }) {
  const labels = {
    OPEN: 'Open',
    IN_PROGRESS: 'In progress',
    RESOLVED: 'Resolved',
    CLOSED: 'Closed',
  }
  const className = String(status || '').toLowerCase().replace(/_/g, '-')

  return <span className={`badge badge-${className}`}>{labels[status] || status || '—'}</span>
}

function RemainingTime({ ticket }) {
  if (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED') {
    return <span className="muted">Closed</span>
  }
  if (ticket.overdue) {
    return <span className="tat-danger">{ticket.remainingTime || 'Overdue'}</span>
  }
  if (ticket.slaAlert) {
    return <span className="tat-warning">{ticket.remainingTime || 'Near SLA breach'}</span>
  }
  return <span>{ticket.remainingTime || '—'}</span>
}

/**
 * Ticket table shared by the representative and the manager.
 * showAssigned adds the representative column, renderActions adds a button column.
 */
export function TicketTable({
  tickets,
  emptyText = 'No tickets found.',
  showAssigned = false,
  renderActions,
}) {
  if (!tickets || tickets.length === 0) {
    return <p className="empty">{emptyText}</p>
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Ticket</th>
            <th>Customer</th>
            <th>Subject</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Remaining TAT</th>
            {showAssigned && <th>Assigned to</th>}
            {renderActions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {tickets.map((ticket) => (
            <tr key={ticket.id}>
              <td>
                <div className="strong">{ticket.ticketNumber}</div>
                <div className="muted small">#{ticket.id}</div>
              </td>
              <td>
                <div className="strong">
                  {ticket.customerName || ticket.potentialCustomerName || '—'}
                </div>
                <div className="muted small">
                  {ticket.customerEmail || ticket.potentialCustomerEmail || '—'}
                </div>
              </td>
              <td>
                <div>{ticket.subject}</div>
                {ticket.customerType === 'POTENTIAL_CUSTOMER' && <span className="tag">Lead</span>}
              </td>
              <td>{humanize(ticket.priority)}</td>
              <td>
                <StatusBadge status={ticket.status} />
              </td>
              <td>
                <RemainingTime ticket={ticket} />
              </td>
              {showAssigned && (
                <td>
                  {ticket.assignedToName ? (
                    <>
                      <div>{ticket.assignedToName}</div>
                      <div className="muted small">{ticket.assignedToEmail}</div>
                    </>
                  ) : (
                    <span className="muted">Unassigned</span>
                  )}
                </td>
              )}
              {renderActions && <td>{renderActions(ticket)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
