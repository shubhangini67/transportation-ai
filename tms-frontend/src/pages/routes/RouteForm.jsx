import React, { useState } from 'react';
import FormField from '../../components/common/FormField';
import '../../styles/forms.css';

function RouteForm({ route, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    origin: route?.origin || '',
    destination: route?.destination || '',
    distance: route?.distance || '',
    estimatedTimeMinutes: route?.estimatedTimeMinutes || '',
    description: route?.description || '',
    active: route?.active ?? true,
  });
  const [errors, setErrors] = useState({});

  const validate = () => {
    const errs = {};
    if (!form.origin.trim()) errs.origin = 'Required';
    if (!form.destination.trim()) errs.destination = 'Required';
    if (!form.distance || form.distance <= 0) errs.distance = 'Must be positive';
    if (!form.estimatedTimeMinutes || form.estimatedTimeMinutes <= 0) errs.estimatedTimeMinutes = 'Must be positive';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validate()) {
      onSubmit({
        ...form,
        distance: parseFloat(form.distance),
        estimatedTimeMinutes: parseInt(form.estimatedTimeMinutes),
      });
    }
  };

  const handleChange = (field) => (e) => {
    const value = field === 'active' ? e.target.checked : e.target.value;
    setForm({ ...form, [field]: value });
    if (errors[field]) setErrors({ ...errors, [field]: null });
  };

  return (
    <form onSubmit={handleSubmit} className="entity-form">
      <div className="form-grid">
        <FormField label="Origin *" error={errors.origin}>
          <input className="form-input" value={form.origin} onChange={handleChange('origin')} />
        </FormField>
        <FormField label="Destination *" error={errors.destination}>
          <input className="form-input" value={form.destination} onChange={handleChange('destination')} />
        </FormField>
        <FormField label="Distance (km) *" error={errors.distance}>
          <input className="form-input" type="number" step="0.1" value={form.distance} onChange={handleChange('distance')} />
        </FormField>
        <FormField label="Est. Time (min) *" error={errors.estimatedTimeMinutes}>
          <input className="form-input" type="number" value={form.estimatedTimeMinutes} onChange={handleChange('estimatedTimeMinutes')} />
        </FormField>
        <FormField label="Description">
          <textarea className="form-input form-textarea" value={form.description} onChange={handleChange('description')} />
        </FormField>
        <FormField label="">
          <label className="checkbox-label">
            <input type="checkbox" checked={form.active} onChange={handleChange('active')} />
            Active
          </label>
        </FormField>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{route ? 'Update' : 'Create'}</button>
      </div>
    </form>
  );
}

export default RouteForm;

