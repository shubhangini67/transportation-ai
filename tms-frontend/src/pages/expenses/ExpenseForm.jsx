import React, { useState } from 'react';
import FormField from '../../components/common/FormField';
import '../../styles/forms.css';

const CATEGORIES = ['FUEL', 'TOLL', 'MAINTENANCE', 'DRIVER_ALLOWANCE', 'OTHER'];

function ExpenseForm({ expense, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    category: expense?.category || 'FUEL',
    amount: expense?.amount || '',
    description: expense?.description || '',
    expenseDate: expense?.expenseDate || new Date().toISOString().slice(0, 10),
    tripId: expense?.tripId || '',
    vehicleId: expense?.vehicleId || '',
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({ ...form, tripId: form.tripId || null, vehicleId: form.vehicleId || null });
  };

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <form onSubmit={handleSubmit} className="entity-form">
      <div className="form-grid">
        <FormField label="Category *">
          <select className="form-input" value={form.category} onChange={handleChange('category')}>
            {CATEGORIES.map(c => <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>)}
          </select>
        </FormField>
        <FormField label="Amount *">
          <input className="form-input" type="number" step="0.01" value={form.amount} onChange={handleChange('amount')} required />
        </FormField>
        <FormField label="Date *">
          <input className="form-input" type="date" value={form.expenseDate} onChange={handleChange('expenseDate')} required />
        </FormField>
        <FormField label="Description">
          <textarea className="form-input form-textarea" value={form.description} onChange={handleChange('description')} />
        </FormField>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{expense ? 'Update' : 'Create'}</button>
      </div>
    </form>
  );
}

export default ExpenseForm;

