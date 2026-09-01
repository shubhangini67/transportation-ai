import React, { useState } from 'react';
import FormField from '../../components/common/FormField';
import '../../styles/forms.css';

const VEHICLE_TYPES = ['BUS', 'MINI_BUS', 'VAN', 'TRUCK'];
const VEHICLE_STATUSES = ['AVAILABLE', 'BUSY', 'MAINTENANCE'];

function VehicleForm({ vehicle, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    vehicleNumber: vehicle?.vehicleNumber || '',
    type: vehicle?.type || 'TRUCK',
    capacity: vehicle?.capacity || '',
    status: vehicle?.status || 'AVAILABLE',
    currentLocation: vehicle?.currentLocation || '',
    make: vehicle?.make || '',
    model: vehicle?.model || '',
    year: vehicle?.year || '',
    odometerKm: vehicle?.odometerKm || '',
    lastServiceDate: vehicle?.lastServiceDate || '',
    nextServiceDueKm: vehicle?.nextServiceDueKm || '',
  });
  const [errors, setErrors] = useState({});

  const validate = () => {
    const errs = {};
    if (!form.vehicleNumber.trim()) errs.vehicleNumber = 'Required';
    if (!form.capacity || form.capacity <= 0) errs.capacity = 'Must be greater than 0';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validate()) {
      onSubmit({
        ...form,
        capacity: parseInt(form.capacity, 10),
        year: form.year ? parseInt(form.year, 10) : null,
        odometerKm: form.odometerKm ? parseInt(form.odometerKm, 10) : null,
        lastServiceDate: form.lastServiceDate || null,
        nextServiceDueKm: form.nextServiceDueKm ? parseInt(form.nextServiceDueKm, 10) : null,
      });
    }
  };

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    if (errors[field]) setErrors({ ...errors, [field]: null });
  };

  return (
    <form onSubmit={handleSubmit} className="entity-form">
      <div className="form-grid">
        <FormField label="Vehicle Number *" error={errors.vehicleNumber}>
          <input className="form-input" value={form.vehicleNumber} onChange={handleChange('vehicleNumber')} placeholder="e.g. MH-12-AB-1234" />
        </FormField>
        <FormField label="Type *">
          <select className="form-input" value={form.type} onChange={handleChange('type')}>
            {VEHICLE_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
          </select>
        </FormField>
        <FormField label="Capacity *" error={errors.capacity}>
          <input className="form-input" type="number" min="1" value={form.capacity} onChange={handleChange('capacity')} />
        </FormField>
        <FormField label="Status">
          <select className="form-input" value={form.status} onChange={handleChange('status')}>
            {VEHICLE_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
          </select>
        </FormField>
        <FormField label="Make">
          <input className="form-input" value={form.make} onChange={handleChange('make')} />
        </FormField>
        <FormField label="Model">
          <input className="form-input" value={form.model} onChange={handleChange('model')} />
        </FormField>
        <FormField label="Year">
          <input className="form-input" type="number" value={form.year} onChange={handleChange('year')} />
        </FormField>
        <FormField label="Current Location">
          <input className="form-input" value={form.currentLocation} onChange={handleChange('currentLocation')} />
        </FormField>
        <FormField label="Odometer (km)">
          <input className="form-input" type="number" min="0" value={form.odometerKm} onChange={handleChange('odometerKm')} />
        </FormField>
        <FormField label="Last service date">
          <input className="form-input" type="date" value={form.lastServiceDate} onChange={handleChange('lastServiceDate')} />
        </FormField>
        <FormField label="Next service due (km)">
          <input className="form-input" type="number" min="0" value={form.nextServiceDueKm} onChange={handleChange('nextServiceDueKm')} />
        </FormField>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{vehicle ? 'Update' : 'Create'}</button>
      </div>
    </form>
  );
}

export default VehicleForm;
