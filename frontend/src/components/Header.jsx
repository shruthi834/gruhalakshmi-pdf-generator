import React from 'react';

export default function Header() {
  return (
    <header className="site-header">
      <div className="header-inner">
        <div className="brand-section">
          <div className="brand-logo-badge" title="Government of Karnataka">
            🏛️
          </div>
          <div className="brand-text">
            <h1>
              <span>Gruha Lakshmi Scheme</span>
              <span className="kannada-subtitle">ಗೃಹಲಕ್ಷ್ಮಿ ಯೋಜನೆ</span>
            </h1>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Official Sanction Order (ಮಂಜೂರಾತಿ ಪತ್ರ) Generator
            </p>
          </div>
        </div>
      </div>
    </header>
  );
}
