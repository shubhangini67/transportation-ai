import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useI18n } from '../../i18n/I18nProvider';
import { useIntl } from 'react-intl';
import { FiLogOut, FiMoon, FiSun, FiGlobe } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import NotificationBell from './NotificationBell';
import { ROLE_LABEL } from '../../constants/demoAccess';
import '../../styles/layout.css';

const LOCALES = [
  { code: 'en', label: 'EN' },
  { code: 'hi', label: 'HI' },
];

function Navbar() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const { locale, setLocale } = useI18n();
  const intl = useIntl();

  return (
    <header className="navbar">
      <div className="navbar-title">
        {intl.formatMessage({ id: 'app.title' })}
      </div>
      <div className="navbar-actions">
        <div className="locale-selector">
          <FiGlobe style={{ marginRight: 4 }} />
          <select
            value={locale}
            onChange={(e) => setLocale(e.target.value)}
            className="locale-select"
          >
            {LOCALES.map((l) => (
              <option key={l.code} value={l.code}>{l.label}</option>
            ))}
          </select>
        </div>
        <NotificationBell />
        <button className="btn-theme-toggle" onClick={toggleTheme} title={theme === 'light' ? 'Dark mode' : 'Light mode'}>
          {theme === 'light' ? <FiMoon /> : <FiSun />}
        </button>
        <Link to="/profile" className="navbar-user" style={{ cursor: 'pointer' }}>
          <span className="navbar-avatar" aria-hidden="true">
            {(user?.fullName || user?.username || '?').slice(0, 1).toUpperCase()}
          </span>
          <span>{user?.fullName || user?.username}</span>
          <span className="navbar-role-badge">{ROLE_LABEL[user?.role] || user?.role}</span>
        </Link>
        <button className="btn-logout" onClick={logout}>
          <FiLogOut />
          <span>{intl.formatMessage({ id: 'btn.logout' })}</span>
        </button>
      </div>
    </header>
  );
}

export default Navbar;
