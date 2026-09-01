/** One-click demo accounts. Same users as DataSeeder. */
export const DEMO_DESKS = [
  {
    id: 'admin',
    rank: '01',
    role: 'ADMIN',
    title: 'Admin',
    blurb: 'Sees everything — fleet, billing, users.',
    username: 'admin',
    password: 'admin123',
  },
  {
    id: 'dispatcher',
    rank: '02',
    role: 'DISPATCHER',
    title: 'Dispatcher',
    blurb: 'Plans trips and assigns trucks.',
    username: 'dispatcher',
    password: 'dispatch123',
  },
  {
    id: 'driver',
    rank: '03',
    role: 'DRIVER',
    title: 'Driver',
    blurb: 'My trips and the live map.',
    username: 'driver1',
    password: 'driver123',
  },
  {
    id: 'customer',
    rank: '04',
    role: 'CLIENT',
    title: 'Customer',
    blurb: 'Bookings, waybills, and status.',
    username: 'client1',
    password: 'client123',
  },
];

export const DEMO_TRACK_TOKENS = [
  { token: 'LANE-DEMO', label: 'Live run — Ahmedabad → Surat' },
  { token: 'LIVE-DEMO', label: 'Live run — Chennai → Madurai' },
];

export const ROLE_LABEL = {
  ADMIN: 'Admin',
  DISPATCHER: 'Dispatcher',
  DRIVER: 'Driver',
  CLIENT: 'Customer',
};
