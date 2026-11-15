import { useEffect, useState } from 'react'
import api, { setToken } from './api'
import { Layout, Message } from './components'
import CustomerDashboard from './CustomerDashboard'
import ManagerDashboard from './ManagerDashboard'
import SalesDashboard from './SalesDashboard'
import LoginScreen from './LoginScreen'
import { normalizeRole } from './helpers'

const SESSION_KEY = "plmforge-crm-session";

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

  if (checking) {
    return <div className="boot-screen">Checking your session…</div>
  }

  if (!session) {
    return (
      <LoginScreen
        key={preferredRole || 'login'}
        roleOptions={ROLE_OPTIONS}
        preferredRole={preferredRole}
        onLogin={login}
        onRegister={registerCustomer}
      />
    )
  }

  const role = normalizeRole(session.user.role)

  if (role !== session.portalRole) {
    return (
      <div className="boot-screen">
        <p>Role mismatch. Sign out and log in through the correct portal.</p>
        <button type="button" className="btn btn-primary" onClick={signOut}>
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
        The backend returned the role &quot;{role}&quot;. Sign out and use a demo role.
      </Message>
    </Layout>
  )
}
