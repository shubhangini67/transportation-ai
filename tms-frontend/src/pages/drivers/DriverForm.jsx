import React, { useState } from 'react';
import FormField from '../../components/common/FormField';
import '../../styles/forms.css';

const DRIVER_STATUSES = ['ACTIVE', 'INACTIVE'];

function DriverForm({ driver, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    name: driver?.name || '',
    phone: driver?.phone || '',
    licenseNumber: driver?.licenseNumber || '',
    email: driver?.email || '',
    status: driver?.status || 'ACTIVE',
  });
  const [errors, setErrors] = useState({});

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = 'Required';
    if (!form.licenseNumber.trim()) errs.licenseNumber = 'Required';
    if (!form.phone.trim()) errs.phone = 'Required';
    else if (!/^\+?[0-9]{7,15}$/.test(form.phone)) errs.phone = 'Invalid phone format';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validate()) onSubmit(form);
  };

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    if (errors[field]) setErrors({ ...errors, [field]: null });
  };

  return (
    <form onSubmit={handleSubmit} className="entity-form">
      <div className="form-grid">
        <FormField label="Name *" error={errors.name}>
          <input className="form-input" value={form.name} onChange={handleChange('name')} />
        </FormField>
        <FormField label="Phone *" error={errors.phone}>
          <input className="form-input" value={form.phone} onChange={handleChange('phone')} placeholder="+1234567890" />
        </FormField>
        <FormField label="License Number *" error={errors.licenseNumber}>
          <input className="form-input" value={form.licenseNumber} onChange={handleChange('licenseNumber')} />
        </FormField>
        <FormField label="Email">
          <input className="form-input" type="email" value={form.email} onChange={handleChange('email')} />
        </FormField>
        <FormField label="Status">
          <select className="form-input" value={form.status} onChange={handleChange('status')}>
            {DRIVER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </FormField>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{driver ? 'Update' : 'Create'}</button>
      </div>
    </form>
  );
}

export default DriverForm;
