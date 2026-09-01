import React from 'react';
import { statusEmoji } from '../../constants/entityVisuals';
import '../../styles/components.css';

const statusColors = {
  // Vehicle
  AVAILABLE: '#4caf50',
  BUSY: '#ff9800',
  MAINTENANCE: '#9e9e9e',
  // Driver
  ACTIVE: '#4caf50',
  INACTIVE: '#9e9e9e',
  // Trip
  PLANNED: '#2196f3',
  IN_PROGRESS: '#ff9800',
  COMPLETED: '#4caf50',
  // Booking
  CONFIRMED: '#4caf50',
  CANCELLED: '#f44336',
  // LR
  CREATED: '#2196f3',
  IN_TRANSIT: '#ff9800',
  DELIVERED: '#4caf50',
  // Invoice
  DRAFT: '#9e9e9e',
  SENT: '#2196f3',
  PAID: '#4caf50',
};

function StatusBadge({ status }) {
  const color = statusColors[status] || '#9e9e9e';
  return (
    <span className="status-badge" style={{ backgroundColor: color }}>
      <span aria-hidden="true">{statusEmoji(status)}</span>
      {status?.replace(/_/g, ' ')}
    </span>
  );
}

export default StatusBadge;
