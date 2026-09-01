import React from 'react';
import { Link } from 'react-router-dom';
import { FiChevronRight } from 'react-icons/fi';
import '../../styles/dashboard.css';

function StatCard({ icon, label, value, color, to, onClick }) {
  const inner = (
    <>
      <div className="stat-icon">{icon}</div>
      <div className="stat-info">
        <span className="stat-value">{value}</span>
        <span className="stat-label">{label}</span>
      </div>
      {(to || onClick) && <FiChevronRight className="stat-chevron" />}
    </>
  );

  const style = color ? { borderLeftColor: color } : undefined;

  if (to) {
    return (
      <Link to={to} className="stat-card stat-card-link" style={style}>
        {inner}
      </Link>
    );
  }
  if (onClick) {
    return (
      <button type="button" className="stat-card stat-card-link" style={style} onClick={onClick}>
        {inner}
      </button>
    );
  }
  return (
    <div className="stat-card" style={style}>
      {inner}
    </div>
  );
}

export default StatCard;
