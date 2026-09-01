export const VEHICLE_EMOJI = {
  TRUCK: '🚚',
  BUS: '🚌',
  MINI_BUS: '🚐',
  VAN: '🚐',
};

export function vehicleEmoji(type) {
  return VEHICLE_EMOJI[type] || '🚛';
}

export const STATUS_EMOJI = {
  AVAILABLE: '🟢',
  BUSY: '🟠',
  MAINTENANCE: '🔧',
  ACTIVE: '✅',
  INACTIVE: '💤',
  PLANNED: '📋',
  IN_PROGRESS: '🛣️',
  COMPLETED: '🏁',
  CONFIRMED: '✅',
  CANCELLED: '❌',
  CREATED: '📝',
  IN_TRANSIT: '📦',
  DELIVERED: '📬',
  DRAFT: '✏️',
  SENT: '📤',
  PAID: '💰',
};

export function statusEmoji(status) {
  return STATUS_EMOJI[status] || '●';
}

export function initials(name) {
  if (!name) return '?';
  const parts = String(name).trim().split(/\s+/);
  return ((parts[0]?.[0] || '') + (parts[1]?.[0] || '')).toUpperCase() || '?';
}

export function avatarHue(seed) {
  let h = 0;
  for (const ch of String(seed || 'x')) h = (h * 31 + ch.charCodeAt(0)) % 360;
  return h;
}
