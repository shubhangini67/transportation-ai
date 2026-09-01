import React from 'react';
import { Link } from 'react-router-dom';
import { FiMoon, FiSun } from 'react-icons/fi';
import { useTheme } from '../../context/ThemeContext';
import TruckSchematic from './TruckSchematic';

function AuthShell({ children }) {
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="auth-shell">
      <button
        type="button"
        className="auth-theme"
        onClick={toggleTheme}
        title={theme === 'light' ? 'Night' : 'Day'}
      >
        {theme === 'light' ? <FiMoon /> : <FiSun />}
      </button>

      <section className="auth-visual">
        <div className="auth-visual-inner">
          <div className="auth-visual-top">
            <p className="auth-kicker">India control room</p>
            <h1 className="auth-brand">Transportation <span>AI</span></h1>
            <p className="auth-tag">Crew, trucks, and every live run — one quiet desk.</p>
          </div>
          <div className="auth-visual-hero">
            <TruckSchematic className="auth-truck" />
          </div>
          <ul className="auth-highlights">
            <li><strong>Live GPS</strong>India lanes</li>
            <li><strong>Copilot</strong>Ops answers</li>
            <li><strong>Assign</strong>Ranked crews</li>
          </ul>
        </div>
      </section>

      <section className="auth-panel">
        {children}
      </section>
    </div>
  );
}

export function AuthBrandLink() {
  return (
    <Link to="/login" className="auth-mini-brand">
      Transportation AI
    </Link>
  );
}

export default AuthShell;
