import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import MainLayout from './components/layout/MainLayout';
import ProtectedRoute from './components/common/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import VehicleList from './pages/vehicles/VehicleList';
import DriverList from './pages/drivers/DriverList';
import RouteList from './pages/routes/RouteList';
import TripList from './pages/trips/TripList';
import TripTracking from './pages/trips/TripTracking';
import BookingList from './pages/bookings/BookingList';
import LrList from './pages/lrs/LrList';
import UserList from './pages/users/UserList';
import ProfilePage from './pages/profile/ProfilePage';
import AuditLogList from './pages/audit/AuditLogList';
import ExpenseList from './pages/expenses/ExpenseList';
import ReportsPage from './pages/reports/ReportsPage';
import InvoiceList from './pages/invoices/InvoiceList';
import WebhookList from './pages/webhooks/WebhookList';
import GeofenceList from './pages/geofences/GeofenceList';
import FuelAnalytics from './pages/analytics/FuelAnalytics';
import SmartDispatch from './pages/dispatch/SmartDispatch';
import OperationsBoard from './pages/operations/OperationsBoard';
import DriverConsole from './pages/drivers/DriverConsole';
import FleetMap from './pages/fleet/FleetMap';
import MaintenanceBoard from './pages/maintenance/MaintenanceBoard';
import DriverScorecards from './pages/performance/DriverScorecards';
import RateCards from './pages/rates/RateCards';
import AiCopilot from './pages/ai/AiCopilot';
import PublicTrack from './pages/PublicTrack';
import OAuthCallback from './pages/OAuthCallback';

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={!isAuthenticated ? <Login /> : <Navigate to="/dashboard" />} />
      <Route path="/register" element={!isAuthenticated ? <Register /> : <Navigate to="/dashboard" />} />
      <Route path="/oauth2/callback" element={<OAuthCallback />} />
      <Route path="/track/:token" element={<PublicTrack />} />
      <Route element={<ProtectedRoute><MainLayout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/ai" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <AiCopilot />
          </ProtectedRoute>
        } />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/vehicles" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER', 'DRIVER']}>
            <VehicleList />
          </ProtectedRoute>
        } />
        <Route path="/drivers" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <DriverList />
          </ProtectedRoute>
        } />
        <Route path="/routes" element={<RouteList />} />
        <Route path="/trips" element={<TripList />} />
        <Route path="/trips/:id/tracking" element={<TripTracking />} />
        <Route path="/bookings" element={<BookingList />} />
        <Route path="/lrs" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT']}>
            <LrList />
          </ProtectedRoute>
        } />
        <Route path="/expenses" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <ExpenseList />
          </ProtectedRoute>
        } />
        <Route path="/invoices" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <InvoiceList />
          </ProtectedRoute>
        } />
        <Route path="/reports" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <ReportsPage />
          </ProtectedRoute>
        } />
        <Route path="/users" element={
          <ProtectedRoute roles={['ADMIN']}>
            <UserList />
          </ProtectedRoute>
        } />
        <Route path="/audit-logs" element={
          <ProtectedRoute roles={['ADMIN']}>
            <AuditLogList />
          </ProtectedRoute>
        } />
        <Route path="/webhooks" element={
          <ProtectedRoute roles={['ADMIN']}>
            <WebhookList />
          </ProtectedRoute>
        } />
        <Route path="/geofences" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <GeofenceList />
          </ProtectedRoute>
        } />
        <Route path="/fuel-analytics" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <FuelAnalytics />
          </ProtectedRoute>
        } />
        <Route path="/fleet-map" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER', 'DRIVER']}>
            <FleetMap />
          </ProtectedRoute>
        } />
        <Route path="/maintenance" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <MaintenanceBoard />
          </ProtectedRoute>
        } />
        <Route path="/scorecards" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <DriverScorecards />
          </ProtectedRoute>
        } />
        <Route path="/rates" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <RateCards />
          </ProtectedRoute>
        } />
        <Route path="/dispatch" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <SmartDispatch />
          </ProtectedRoute>
        } />
        <Route path="/operations" element={
          <ProtectedRoute roles={['ADMIN', 'DISPATCHER']}>
            <OperationsBoard />
          </ProtectedRoute>
        } />
        <Route path="/driver-console" element={
          <ProtectedRoute roles={['DRIVER']}>
            <DriverConsole />
          </ProtectedRoute>
        } />
      </Route>
      <Route path="*" element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} />} />
    </Routes>
  );
}

export default App;
