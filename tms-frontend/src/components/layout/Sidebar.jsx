import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useIntl } from 'react-intl';
import {
  FiGrid, FiTruck, FiUsers, FiMap, FiNavigation, FiBookOpen, FiFileText, FiShield,
  FiDollarSign, FiBarChart2, FiActivity, FiLink, FiCreditCard, FiTarget, FiDroplet,
  FiCpu, FiAlertTriangle, FiUser, FiMapPin, FiTool, FiAward, FiPercent, FiMessageCircle
} from 'react-icons/fi';
import { ROLE_LABEL } from '../../constants/demoAccess';
import '../../styles/layout.css';

const navGroups = [
  {
    id: 'nav.group.operate',
    items: [
      { path: '/dashboard', labelId: 'nav.dashboard', icon: <FiGrid />, roles: ['ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT'] },
      { path: '/ai', labelId: 'nav.ai', icon: <FiMessageCircle />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/operations', labelId: 'nav.operations', icon: <FiAlertTriangle />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/fleet-map', labelId: 'nav.fleetMap', icon: <FiMapPin />, roles: ['ADMIN', 'DISPATCHER', 'DRIVER'] },
      { path: '/dispatch', labelId: 'nav.dispatch', icon: <FiCpu />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/driver-console', labelId: 'nav.driverConsole', icon: <FiUser />, roles: ['DRIVER'] },
    ],
  },
  {
    id: 'nav.group.fleet',
    items: [
      { path: '/vehicles', labelId: 'nav.vehicles', icon: <FiTruck />, roles: ['ADMIN', 'DISPATCHER', 'DRIVER'] },
      { path: '/drivers', labelId: 'nav.drivers', icon: <FiUsers />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/routes', labelId: 'nav.routes', icon: <FiMap />, roles: ['ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT'] },
      { path: '/trips', labelId: 'nav.trips', icon: <FiNavigation />, roles: ['ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT'] },
      { path: '/maintenance', labelId: 'nav.maintenance', icon: <FiTool />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/geofences', labelId: 'nav.geofences', icon: <FiTarget />, roles: ['ADMIN', 'DISPATCHER'] },
    ],
  },
  {
    id: 'nav.group.trade',
    items: [
      { path: '/bookings', labelId: 'nav.bookings', icon: <FiBookOpen />, roles: ['ADMIN', 'DISPATCHER', 'CLIENT'] },
      { path: '/lrs', labelId: 'nav.lrs', icon: <FiFileText />, roles: ['ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT'] },
      { path: '/expenses', labelId: 'nav.expenses', icon: <FiDollarSign />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/invoices', labelId: 'nav.invoices', icon: <FiCreditCard />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/rates', labelId: 'nav.rates', icon: <FiPercent />, roles: ['ADMIN', 'DISPATCHER'] },
    ],
  },
  {
    id: 'nav.group.insight',
    items: [
      { path: '/reports', labelId: 'nav.reports', icon: <FiBarChart2 />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/fuel-analytics', labelId: 'nav.fuelAnalytics', icon: <FiDroplet />, roles: ['ADMIN', 'DISPATCHER'] },
      { path: '/scorecards', labelId: 'nav.scorecards', icon: <FiAward />, roles: ['ADMIN', 'DISPATCHER'] },
    ],
  },
  {
    id: 'nav.group.admin',
    items: [
      { path: '/users', labelId: 'nav.users', icon: <FiShield />, roles: ['ADMIN'] },
      { path: '/audit-logs', labelId: 'nav.auditLogs', icon: <FiActivity />, roles: ['ADMIN'] },
      { path: '/webhooks', labelId: 'nav.webhooks', icon: <FiLink />, roles: ['ADMIN'] },
    ],
  },
];

function Sidebar() {
  const { user } = useAuth();
  const intl = useIntl();

  const visible = navGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.roles || item.roles.includes(user?.role)),
    }))
    .filter((group) => group.items.length > 0);

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <span className="sidebar-mark" aria-hidden="true">T</span>
        <div>
          <h2>{intl.formatMessage({ id: 'app.short' })}</h2>
          <small>{intl.formatMessage({ id: 'app.kicker' })}</small>
        </div>
      </div>
      <nav className="sidebar-nav">
        {visible.map((group) => (
          <div key={group.id} className="nav-group">
            <p className="nav-group-label">{intl.formatMessage({ id: group.id })}</p>
            {group.items.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <span className="nav-icon">{item.icon}</span>
                <span className="nav-label">{intl.formatMessage({ id: item.labelId })}</span>
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
      <div className="sidebar-footer">
        <span className="role-badge">{ROLE_LABEL[user?.role] || user?.role}</span>
      </div>
    </aside>
  );
}

export default Sidebar;
