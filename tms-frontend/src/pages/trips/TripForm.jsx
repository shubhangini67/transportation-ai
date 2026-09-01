import React, { useState, useEffect } from 'react';
import FormField from '../../components/common/FormField';
import vehicleService from '../../services/vehicleService';
import driverService from '../../services/driverService';
import routeService from '../../services/routeService';
import lrService from '../../services/lrService';
import '../../styles/forms.css';

function TripForm({ trip, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    vehicleId: trip?.vehicleId || '',
    driverId: trip?.driverId || '',
    routeId: trip?.routeId || '',
    startTime: trip?.startTime?.slice(0, 16) || '',
    endTime: trip?.endTime?.slice(0, 16) || '',
    notes: trip?.notes || '',
    lrIds: trip?.lorryReceipts?.map(lr => lr.id) || [],
  });
  const [errors, setErrors] = useState({});
  const [vehicles, setVehicles] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [lrs, setLrs] = useState([]);

  useEffect(() => {
    const load = async () => {
      try {
        const [vehicleRes, driverRes, routeRes, lrRes] = await Promise.all([
          trip ? vehicleService.getAll({ size: 100 }) : vehicleService.getAvailable(),
          trip ? driverService.getAll({ size: 100 }) : driverService.getActive(),
          routeService.getActive(),
          lrService.getAll({ size: 100 }),
        ]);
        // Paginated responses have .content, list responses are arrays
        setVehicles(vehicleRes.data?.content || vehicleRes.data || []);
        setDrivers(driverRes.data?.content || driverRes.data || []);
        setRoutes(routeRes.data || []);
        setLrs(lrRes.data?.content || lrRes.data || []);
      } catch (err) {
        console.error('Failed to load form data', err);
      }
    };
    load();
  }, [trip]);

  const validate = () => {
    const errs = {};
    if (!form.vehicleId) errs.vehicleId = 'Required';
    if (!form.driverId) errs.driverId = 'Required';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validate()) {
      onSubmit({
        vehicleId: form.vehicleId,
        driverId: form.driverId,
        routeId: form.routeId || null,
        startTime: form.startTime || null,
        endTime: form.endTime || null,
        notes: form.notes,
        lrIds: form.lrIds,
      });
    }
  };

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    if (errors[field]) setErrors({ ...errors, [field]: null });
  };

  const handleLrToggle = (lrId) => {
    setForm(prev => ({
      ...prev,
      lrIds: prev.lrIds.includes(lrId)
        ? prev.lrIds.filter(id => id !== lrId)
        : [...prev.lrIds, lrId],
    }));
  };

  return (
    <form onSubmit={handleSubmit} className="entity-form">
      <div className="form-grid">
        <FormField label="Vehicle *" error={errors.vehicleId}>
          <select className="form-input" value={form.vehicleId} onChange={handleChange('vehicleId')}>
            <option value="">Select vehicle</option>
            {vehicles.map(v => <option key={v.id} value={v.id}>{v.vehicleNumber} ({v.type})</option>)}
          </select>
        </FormField>
        <FormField label="Driver *" error={errors.driverId}>
          <select className="form-input" value={form.driverId} onChange={handleChange('driverId')}>
            <option value="">Select driver</option>
            {drivers.map(d => <option key={d.id} value={d.id}>{d.name} ({d.licenseNumber})</option>)}
          </select>
        </FormField>
        <FormField label="Route">
          <select className="form-input" value={form.routeId} onChange={handleChange('routeId')}>
            <option value="">No route selected</option>
            {routes.map(r => <option key={r.id} value={r.id}>{r.origin} → {r.destination} ({r.distance} km)</option>)}
          </select>
        </FormField>
        <FormField label="Start Time">
          <input className="form-input" type="datetime-local" value={form.startTime} onChange={handleChange('startTime')} />
        </FormField>
        <FormField label="End Time">
          <input className="form-input" type="datetime-local" value={form.endTime} onChange={handleChange('endTime')} />
        </FormField>
        <FormField label="Notes">
          <textarea className="form-input form-textarea" value={form.notes} onChange={handleChange('notes')} />
        </FormField>
      </div>

      {lrs.length > 0 && (
        <div style={{ marginTop: '16px' }}>
          <label className="form-label">Lorry Receipts</label>
          <div className="lr-checklist">
            {lrs.map(lr => (
              <label key={lr.id} className="checkbox-label">
                <input
                  type="checkbox"
                  checked={form.lrIds.includes(lr.id)}
                  onChange={() => handleLrToggle(lr.id)}
                />
                <span>{lr.lrNumber} — {lr.origin} → {lr.destination}</span>
              </label>
            ))}
          </div>
        </div>
      )}

      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{trip ? 'Update' : 'Create'}</button>
      </div>
    </form>
  );
}

export default TripForm;

