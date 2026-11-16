import { useEffect, useState } from 'react'
import api, { setToken, API_BASE_URL, friendlyError } from './api'
import { Layout, Message } from './components'
import CustomerDashboard from './CustomerDashboard'
import ManagerDashboard from './ManagerDashboard'
import SalesDashboard from './SalesDashboard'
import LoginScreen from './LoginScreen'
import { normalizeRole } from './helpers'

const SESSION_KEY = "plmforge-crm-session"

// Portal hints for the login screen. Sales accounts come from the backend seed data;
// customers register their own, so there is no default customer email here.
const ROLE_OPTIONS = [
  {
    role: 'CUSTOMER',
    title: 'Customer',
    email: '',
    description: 'Register first, then sign in with your own account.',
  },
  {
    role: 'SALES_REPRESENTATIVE',
    title: 'Sales Representative',
    email: 'salesrep@erp.com',
    description: 'Use the seeded sales representative account.',
  },
  {
    role: 'SALES_MANAGER',
    title: 'Sales Manager',
    email: 'manager@erp.com',
    description: 'Use the seeded sales manager account.',
  },
]

function readSession() {
  try {
    const saved = window.localStorage.getItem(SESSION_KEY)
    return saved ? JSON.parse(saved) : null
  } catch {
    return null
  }
}

export default function App() {
  const [session, setSession] = useState(readSession)
  const [checking, setChecking] = useState(() => Boolean(readSession()))
  const [preferredRole, setPreferredRole] = useState('')
  const [showLogin, setShowLogin] = useState(false)
  const [showRegister, setShowRegister] = useState(false)
  const [formData, setFormData] = useState({
    name: '', email: '', phone: '', subject: '', description: ''
  })
  const [message, setMessage] = useState('')
  const [submittingPublic, setSubmittingPublic] = useState(false)

  useEffect(() => {
    if (session) {
      setToken(session.token)
      window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    } else {
      setToken(null)
      window.localStorage.removeItem(SESSION_KEY)
    }
  }, [session])

  // Validate a saved token on load. Expired or tampered -> straight back to login.
  useEffect(() => {
    if (!checking) return undefined

    let active = true
    api
      .get('/api/auth/me')
      .then((response) => {
        if (!active) return
        const me = response.data.data
        const actualRole = normalizeRole(me.role)

        setSession((current) => {
          if (!current) return current
          // A saved session is only valid for the portal that created it.
          if (actualRole !== current.portalRole) return null
          return {
            ...current,
            user: {
              name: me.name,
              email: me.email,
              role: actualRole,
            },
          }
        })
      })
      .catch(() => {
        if (active) setSession(null)
      })
      .finally(() => {
        if (active) setChecking(false)
      })

    return () => {
      active = false
    }
  }, [checking])

  async function login(email, password, portalRole) {
    const response = await api.post('/api/auth/login', { email, password })
    const data = response.data.data
    const actualRole = normalizeRole(data.role)

    // The portal the user picked and the role in the backend response must match.
    // On a mismatch the new JWT is discarded and no session is created.
    if (portalRole && actualRole !== portalRole) {
      setToken(null)
      setSession(null)
      window.localStorage.removeItem(SESSION_KEY)

      const error = new Error('Role mismatch! Please select the correct login portal.')
      error.roleMismatch = true
      throw error
    }

    setToken(data.token)
    setSession({
      token: data.token,
      portalRole: actualRole,
      user: {
        name: data.name,
        email: data.email,
        role: actualRole,
      },
    })
    setPreferredRole('')
  }

  async function registerCustomer(form) {
    const response = await api.post('/api/auth/register', form)
    return response.data.data
  }

  function signOut() {
    setSession(null)
    setPreferredRole('')
  }

  // TODO: keep the last portal in localStorage so signing out does not reset the picker.
  // Changing role signs out - the next login needs that role's own credentials.
  function changeRole(nextRole) {
    setSession(null)
    setPreferredRole(nextRole)
  }

  async function submitPublicQuery() {
    setMessage('')
    setSubmittingPublic(true)
    try {
      const response = await api.post('/api/public/ingest', formData)
      const payload = response.data
      if (payload.success) {
        const ticket = payload.data?.ticket
        const ticketNumber = ticket?.ticketNumber || 'N/A'
        const status = ticket?.status || 'N/A'
        const successText =
          'Your query has been submitted successfully! Thank you for contacting PLMForge. Our support team will review your query and get back to you as soon as possible.'
        setMessage(
          `${successText}\nTicket Number: ${ticketNumber}\nStatus: ${status}\nExpected response time: 24 hours`
        )
        setFormData({ name: '', email: '', phone: '', subject: '', description: '' })
      } else {
        setMessage(`Error: ${payload.message || 'Unknown error'}`)
      }
    } catch (err) {
      setMessage(friendlyError(err))
    } finally {
      setSubmittingPublic(false)
    }
  }

  function handleNameChange(event) {
    setFormData({ ...formData, name: event.target.value })
  }
  function handleEmailChange(event) {
    setFormData({ ...formData, email: event.target.value })
  }
  function handlePhoneChange(event) {
    setFormData({ ...formData, phone: event.target.value })
  }
  function handleSubjectChange(event) {
    setFormData({ ...formData, subject: event.target.value })
  }
  function handleDescriptionChange(event) {
    setFormData({ ...formData, description: event.target.value })
  }

  if (checking) {
    return <div className="boot-screen">Checking your session…</div>
  }

  if (!session) {
    return (
      <div className="auth-screen" style={{ padding: '2rem' }}>
        <div style={{ maxWidth: '960px', margin: '0 auto' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex start',
              marginBottom: '1.5rem',
            }}
          >
            <div>
              <h1 style={{ color: '#2c3e50', marginBottom: '0.5rem' }}>
                PLMForge CRM
              </h1>
              <p style={{ color: '#64748b', fontSize: '14px' }}>
                Customer Relationship Management
              </p>
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <a
                href="#"
                style={{
                  padding: '0.5rem 1rem',
                  background: '#e2e8f0',
                  borderRadius: '20px',
                  fontSize: '12px',
                  color: '#334155',
                  textDecoration: 'none',
                }}
                onClick={() => setShowLogin(true)}
              >
                CRM Login
              </a>
            </div>
          </div>

          <div
            style={{
              border: '1px solid #e0e0e0',
              borderRadius: '8px',
              padding: '2rem',
              marginBottom: '2rem',
              background: '#f8fafc',
            }}
          >
            <h2 style={{ marginTop: '0', marginBottom: '1rem', color: '#1d4ed8' }}>
              Submit Your Query
            </h2>
            <form
              style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}
              onSubmit={(event) => {
                event.preventDefault()
                submitPublicQuery()
              }}
            >
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    Full Name
                  </label>
                  <input
                    type="text"
                    name="name"
                    required
                    value={formData.name}
                    onChange={handleNameChange}
                    style={{ padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
                    placeholder="John Doe"
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    Email Address
                  </label>
                  <input
                    type="email"
                    name="email"
                    required
                    value={formData.email}
                    onChange={handleEmailChange}
                    style={{ padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
                    placeholder="john@example.com"
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                  Phone Number
                </label>
                <input
                  type="text"
                  name="phone"
                  value={formData.phone}
                  onChange={handlePhoneChange}
                  style={{ padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
                  placeholder="+1-555-0100"
                />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                  Subject
                </label>
                <input
                  type="text"
                  name="subject"
                  required
                  value={formData.subject}
                  onChange={handleSubjectChange}
                  style={{ padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
                  placeholder="Query subject"
                />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                  Query Description
                </label>
                <textarea
                  name="description"
                  rows="3"
                  required
                  value={formData.description}
                  onChange={handleDescriptionChange}
                  style={{
                    padding: '0.5rem',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    resize: 'vertical',
                  }}
                  placeholder="Describe your query..."
                ></textarea>
              </div>

              <button
                type="submit"
                disabled={submittingPublic}
                style={{
                  padding: '0.75rem 1.5rem',
                  background: submittingPublic ? '#93c5fd' : '#3498db',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  fontSize: '1rem',
                  cursor: submittingPublic ? 'not-allowed' : 'pointer',
                }}
              >
                {submittingPublic ? 'Submitting...' : 'Submit Query'}
              </button>
            </form>
          </div>

          {showLogin && (
            <LoginScreen
              roleOptions={ROLE_OPTIONS}
              preferredRole={preferredRole}
              onLogin={async (email, password, portalRole) =>
                login(email, password, portalRole)
              }
              onRegister={async (form) => registerCustomer(form)}
            />
          )}

          <div style={{ textAlign: 'center', color: '#555', fontSize: '0.9rem' }}>
            <a
              href="#"
              style={{ margin: '0 1rem', color: '#3498db', textDecoration: 'none' }}
              onClick={() => setShowLogin(true)}
            >
              Already registered? Sign In
            </a>
            <a
              href="#"
              style={{ margin: '0 1rem', color: '#3498db', textDecoration: 'none' }}
              onClick={() => setShowRegister(true)}
            >
              New customer? Register
            </a>
          </div>

          <div
            id="message"
            style={{
              textAlign: 'center',
              marginTop: '1rem',
              minHeight: '2rem',
              color: message.startsWith('Error') || message.startsWith('Cannot reach') || message.startsWith('Request failed') ? '#e74c3c' : '#166534',
              whiteSpace: 'pre-line',
            }}
          >
            {message}
          </div>
        </div>
      </div>
    )
  }

  const role = normalizeRole(session.user.role)

  if (role !== session.portalRole) {
    return (
      <div className="boot-screen">
        <p>
          Role mismatch. Sign out and log in through the correct portal.
        </p>
        <button
          type="button"
          className="btn btn-primary"
          onClick={signOut}
        >
          Sign out
        </button>
      </div>
    )
  }

  const layoutProps = {
    user: session.user,
    role,
    roleOptions: ROLE_OPTIONS,
    onChangeRole: changeRole,
    onLogout: signOut,
  }

  if (role === 'CUSTOMER') {
    return (
      <Layout
        {...layoutProps}
        title="Customer Dashboard"
        subtitle="Submit a query and see its ticket and TAT."
      >
        <CustomerDashboard user={session.user} />
      </Layout>
    )
  }

  if (role === 'SALES_REPRESENTATIVE') {
    return (
      <Layout
        {...layoutProps}
        title="Sales Representative Dashboard"
        subtitle="Your assigned tickets with customer details, status and remaining TAT."
      >
        <SalesDashboard />
      </Layout>
    )
  }

  if (role === 'SALES_MANAGER') {
    return (
      <Layout
        {...layoutProps}
        title="Sales Manager Dashboard"
        subtitle="Standard tickets, potential customers and alerts."
      >
        <ManagerDashboard />
      </Layout>
    )
  }

  return (
    <Layout
      {...layoutProps}
      title="Unsupported role"
      subtitle="No dashboard exists for this account."
    >
      <Message type="error">
        The backend returned the role \"{role}\". Sign out and use a demo role.
      </Message>
    </Layout>
  )
}