import { useState } from 'react'
import { friendlyError } from './api'
import { Message } from './components'

export default function LoginScreen({ roleOptions, preferredRole, onLogin, onRegister }) {
  const startingRole =
    roleOptions.find((option) => option.role === preferredRole) || roleOptions[0]

  const [selectedRole, setSelectedRole] = useState(startingRole.role)
  const [mode, setMode] = useState('login')
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loginForm, setLoginForm] = useState({ email: '', password: '' })
  const [registerForm, setRegisterForm] = useState({ name: '', email: '', password: '' })

  const selectedOption =
    roleOptions.find((option) => option.role === selectedRole) || roleOptions[0]

  // Picking a portal only changes the hint text. It does not sign anyone in and does
  // not fill the email field, so the typed email always decides the login.
  function selectRole(option) {
    setSelectedRole(option.role)
    setLoginForm({ email: '', password: '' })
    setError('')
    setNotice('')
  }

  function changeMode(nextMode) {
    setMode(nextMode)
    setError('')
    setNotice('')
  }

  async function submitLogin(event) {
    event.preventDefault()
    if (!loginForm.email.trim() || !loginForm.password) {
      setError('Please enter your email and password.')
      return
    }

    setBusy('login')
    setError('')
    try {
      await onLogin(loginForm.email.trim(), loginForm.password, selectedRole)
    } catch (err) {
      setError(err.roleMismatch ? err.message : friendlyError(err))
    } finally {
      setBusy('')
    }
  }

  async function submitRegister(event) {
    event.preventDefault()

    if (!registerForm.name.trim() || !registerForm.email.trim() || !registerForm.password) {
      setError('Please fill in name, email and password.')
      return
    }
    if (registerForm.password.length < 8) {
      setError('The backend requires a password of at least 8 characters.')
      return
    }

    setBusy('register')
    setError('')
    setNotice('')
    try {
      const registered = await onRegister({
        name: registerForm.name.trim(),
        email: registerForm.email.trim(),
        password: registerForm.password,
      })

      // Registration never logs you in. Flip to the login tab, prefill the email and
      // select the Customer portal because that is the only role registration creates.
      setSelectedRole('CUSTOMER')
      setMode('login')
      setLoginForm({ email: registered.email || registerForm.email.trim(), password: '' })
      setRegisterForm({ name: '', email: '', password: '' })
      setNotice(
        `Customer ${registered.email} was registered. Now sign in with that email and password.`,
      )
    } catch (err) {
      setError(friendlyError(err))
    } finally {
      setBusy('')
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-panel">
        <div className="auth-header">
          <h1>PLMForge CRM</h1>
          <p>
            New customers register first, then sign in. Sales Representatives and Sales Managers use
            their seeded backend accounts. After sign-in, the JWT returned by the backend decides
            your dashboard and permissions.
          </p>
        </div>

        {error && <Message type="error">{error}</Message>}
        {notice && <Message type="success">{notice}</Message>}

        <div className="role-grid">
          {roleOptions.map((option) => (
            <button
              key={option.role}
              type="button"
              className={`role-option ${option.role === selectedRole ? 'selected' : ''}`}
              onClick={() => selectRole(option)}
              aria-pressed={option.role === selectedRole}
            >
              <span className="role-option-title">{option.title}</span>
              <span className="role-option-text">{option.description}</span>
            </button>
          ))}
        </div>

        <p className="auth-note">
          Picking a role only helps you identify the account to use; it never grants access. Customer
          accounts come from registration, while representative and manager accounts come from the
          backend seed data. No passwords are shown in this UI.
        </p>

        <div style={{ textAlign: 'center', margin: '1rem 0' }}>
          <p style={{ margin: '0.5rem 0' }}>
            <strong>Submit a query without login:</strong> Anyone can submit a query using their name and email.
          </p>
          <a
            href="#"
            onClick={() => setNotice('Use the public API endpoint to submit a query without login.')}
            style={{ color: '#0066cc', textDecoration: 'none', fontWeight: '500' }}
          >Submit without login →</a>
        </div>

        <p className="auth-note" style={{ color: '#666' }}>
          Picking a role only helps you identify the account to use; it never grants access. Customer
          accounts come from registration, while representative and manager accounts come from the
          backend seed data. No passwords are shown in this UI.
        </p>

        <section className="auth-form-card">
          <div className="auth-tabs">
            <button
              type="button"
              className={`auth-tab ${mode === 'login' ? 'active' : ''}`}
              onClick={() => changeMode('login')}
            >
              Sign in
            </button>
            <button
              type="button"
              className={`auth-tab ${mode === 'register' ? 'active' : ''}`}
              onClick={() => changeMode('register')}
            >
              Register as customer
            </button>
          </div>

          {mode === 'login' ? (
            <form onSubmit={submitLogin} autoComplete="off">
              {selectedRole === 'CUSTOMER' ? (
                <p className="role-hint">
                  Customers must register first. Use the <strong>Register as customer</strong> tab,
                  then sign in here with the email and password you chose.
                </p>
              ) : (
                <p className="role-hint">
                  Selected role: <strong>{selectedOption.title}</strong>. Seeded backend account:{' '}
                  <code>{selectedOption.email}</code>. Type that email and its backend password
                  below; the JWT returned for those credentials decides your actual role.
                </p>
              )}

              <div className="form-grid">
                <div className="field">
                  <label htmlFor="login-email">Email</label>
                  <input
                    id="login-email"
                    type="email"
                    autoComplete="off"
                    value={loginForm.email}
                    onChange={(event) =>
                      setLoginForm({ ...loginForm, email: event.target.value })
                    }
                    placeholder="you@example.com"
                  />
                </div>
                <div className="field">
                  <label htmlFor="login-password">Password</label>
                  <input
                    id="login-password"
                    type="password"
                    autoComplete="off"
                    value={loginForm.password}
                    onChange={(event) =>
                      setLoginForm({ ...loginForm, password: event.target.value })
                    }
                    placeholder="Your password"
                  />
                </div>
                <div className="form-actions">
                  <button className="btn btn-primary" type="submit" disabled={Boolean(busy)}>
                    {busy === 'login' ? 'Signing in…' : 'Sign in'}
                  </button>
                  <span className="hint-text">
                    The dashboard comes from the JWT, not from the role buttons.
                  </span>
                </div>
              </div>
            </form>
          ) : (
            <form onSubmit={submitRegister}>
              <div className="form-grid">
                <div className="field">
                  <label htmlFor="register-name">Full name</label>
                  <input
                    id="register-name"
                    value={registerForm.name}
                    onChange={(event) =>
                      setRegisterForm({ ...registerForm, name: event.target.value })
                    }
                    placeholder="Jane Customer"
                  />
                </div>
                <div className="field">
                  <label htmlFor="register-email">Email</label>
                  <input
                    id="register-email"
                    type="email"
                    autoComplete="email"
                    value={registerForm.email}
                    onChange={(event) =>
                      setRegisterForm({ ...registerForm, email: event.target.value })
                    }
                    placeholder="jane@example.com"
                  />
                </div>
                <div className="field full">
                  <label htmlFor="register-password">
                    Password <span className="optional">(at least 8 characters)</span>
                  </label>
                  <input
                    id="register-password"
                    type="password"
                    autoComplete="new-password"
                    value={registerForm.password}
                    onChange={(event) =>
                      setRegisterForm({ ...registerForm, password: event.target.value })
                    }
                    placeholder="Choose a password"
                  />
                </div>
                <div className="form-actions">
                  <button className="btn btn-primary" type="submit" disabled={Boolean(busy)}>
                    {busy === 'register' ? 'Creating account…' : 'Create customer account'}
                  </button>
                  <span className="hint-text">
                    Registration only creates the account. You sign in separately afterwards.
                  </span>
                </div>
              </div>
            </form>
          )}
        </section>
        <p className="auth-note" style={{ textAlign: 'center', marginTop: '1rem' }}>
          <a
            href="#"
            onClick={() => setNotice('Use the public API endpoint to submit a query without login.')}
          >Submit without login</a>
        </p>
      </div>
    </div>
  )
}
