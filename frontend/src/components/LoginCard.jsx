import React, { useState } from 'react';
import { Phone, Lock, AlertCircle, Sparkles, ShieldCheck } from 'lucide-react';

export default function LoginCard({ onLoginSuccess }) {
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handlePhoneChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setPhone(val);
    if (error) setError(null);
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    if (phone.length !== 10) {
      setError('Please enter a valid 10-digit mobile number.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone }),
      });

      if (res.ok) {
        const data = await res.json();
        sessionStorage.setItem('auth_token', data.token);
        sessionStorage.setItem('phone', phone);
        onLoginSuccess(data.token, phone);
      } else if (res.status === 404) {
        setError('This phone number is not registered under the Gruha Lakshmi scheme.');
      } else {
        const body = await res.json().catch(() => ({}));
        setError(body.message || 'Login failed. Please try again.');
      }
    } catch (err) {
      console.error(err);
      setError('Unable to connect to the server. Please check your connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="glass-card login-card">
        <div className="card-header" style={{ justifyContent: 'center', textAlign: 'center' }}>
          <div className="card-title" style={{ fontSize: '1.25rem' }}>
            <Sparkles size={22} color="var(--accent-gold)" />
            <span>Beneficiary Login (ಲಾಗಿನ್)</span>
          </div>
        </div>

        <p style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem', lineHeight: '1.5' }}>
          Enter your registered mobile number to generate and download your official Gruha Lakshmi Sanction Order PDF.
        </p>

        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label className="form-label" htmlFor="login-phone">
              <span>Mobile Number (ಮೊಬೈಲ್ ಸಂಖ್ಯೆ)</span>
              <span className="kannada-label">10-ಅಂಕಿಗಳ ಸಂಖ್ಯೆ</span>
            </label>
            <div className="input-wrapper">
              <input
                id="login-phone"
                type="tel"
                className="form-input"
                placeholder="Enter 10-digit mobile number"
                maxLength={10}
                value={phone}
                onChange={handlePhoneChange}
                disabled={loading}
                autoFocus
                required
              />
              <div className="input-icon">
                <Phone size={18} />
              </div>
            </div>
          </div>

          {error && (
            <div className="status-banner banner-error" style={{ marginBottom: '1.25rem' }}>
              <AlertCircle size={18} />
              <div>
                <p className="status-title">Login Failed</p>
                <p className="status-desc">{error}</p>
              </div>
            </div>
          )}

          <div className="button-stack">
            <button
              id="login-submit-btn"
              type="submit"
              className="btn-primary"
              disabled={loading || phone.length !== 10}
            >
              {loading ? (
                <>
                  <span className="spinner"></span>
                  <span>Verifying...</span>
                </>
              ) : (
                <>
                  <Lock size={18} />
                  <span>Login</span>
                </>
              )}
            </button>
          </div>
        </form>

        <div style={{ marginTop: '1.5rem', textAlign: 'center', borderTop: '1px solid var(--border-subtle)', paddingTop: '1rem' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: '#06d6a0', fontSize: '0.82rem' }}>
            <ShieldCheck size={16} />
            <span>Government of Karnataka Secure Portal</span>
          </div>
        </div>
      </div>
    </div>
  );
}
