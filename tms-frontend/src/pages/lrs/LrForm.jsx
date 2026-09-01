import React, { useState } from 'react';
import FormField from '../../components/common/FormField';
import '../../styles/forms.css';

const LR_STATUSES = ['CREATED', 'IN_TRANSIT', 'DELIVERED'];

function LrForm({ lr, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    lrNumber: lr?.lrNumber || '',
    consignor: lr?.consignor || '',
    consignee: lr?.consignee || '',
    origin: lr?.origin || '',
    destination: lr?.destination || '',
    material: lr?.material || '',
    weight: lr?.weight || '',
    quantity: lr?.quantity || '',
    status: lr?.status || 'CREATED',
  });
  const [errors, setErrors] = useState({});

  const validate = () => {
    const errs = {};
    if (!form.lrNumber.trim()) errs.lrNumber = 'LR Number is required';
    if (!form.consignor.trim()) errs.consignor = 'Consignor is required';
    if (!form.consignee.trim()) errs.consignee = 'Consignee is required';
    if (!form.origin.trim()) errs.origin = 'Origin is required';
    if (!form.destination.trim()) errs.destination = 'Destination is required';
    if (!form.weight || parseFloat(form.weight) <= 0) errs.weight = 'Weight must be greater than 0';
    if (!form.quantity || parseInt(form.quantity) <= 0) errs.quantity = 'Quantity must be greater than 0';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validate()) {
      onSubmit({
        ...form,
        weight: parseFloat(form.weight),
        quantity: parseInt(form.quantity),
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
        <FormField label="LR Number *" error={errors.lrNumber}>
          <input className="form-input" value={form.lrNumber} onChange={handleChange('lrNumber')} placeholder="e.g. LR-2026-0001" />
        </FormField>
        <FormField label="Status">
          <select className="form-input" value={form.status} onChange={handleChange('status')}>
            {LR_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
          </select>
        </FormField>
        <FormField label="Consignor *" error={errors.consignor}>
          <input className="form-input" value={form.consignor} onChange={handleChange('consignor')} placeholder="Sender name" />
        </FormField>
        <FormField label="Consignee *" error={errors.consignee}>
          <input className="form-input" value={form.consignee} onChange={handleChange('consignee')} placeholder="Receiver name" />
        </FormField>
        <FormField label="Origin *" error={errors.origin}>
          <input className="form-input" value={form.origin} onChange={handleChange('origin')} placeholder="Origin city" />
        </FormField>
        <FormField label="Destination *" error={errors.destination}>
          <input className="form-input" value={form.destination} onChange={handleChange('destination')} placeholder="Destination city" />
        </FormField>
        <FormField label="Weight (kg) *" error={errors.weight}>
          <input className="form-input" type="number" step="0.01" min="0.01" value={form.weight} onChange={handleChange('weight')} placeholder="Weight in kg" />
        </FormField>
        <FormField label="Quantity *" error={errors.quantity}>
          <input className="form-input" type="number" min="1" value={form.quantity} onChange={handleChange('quantity')} placeholder="Number of items" />
        </FormField>
        <FormField label="Material">
          <textarea className="form-input form-textarea" value={form.material} onChange={handleChange('material')} placeholder="Material description" />
        </FormField>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{lr ? 'Update' : 'Create'}</button>
      </div>
    </form>
  );
}

export default LrForm;

