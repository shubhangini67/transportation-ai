import React, { useState, useEffect } from 'react';
import FormField from '../../components/common/FormField';
import tripService from '../../services/tripService';
import '../../styles/forms.css';

function BookingForm({ booking, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    customerName: booking?.customerName || '',
    customerPhone: booking?.customerPhone || '',
    customerEmail: booking?.customerEmail || '',
    tripId: booking?.tripId || '',
    seatCount: booking?.seatCount || '',
    notes: booking?.notes || '',
  });
  const [errors, setErrors] = useState({});
  const [trips, setTrips] = useState([]);

  useEffect(() => {
    const load = async () => {
      try {
        const res = booking
          ? await tripService.getAll({ size: 100 })
          : await tripService.getByStatus('PLANNED');
        // Paginated responses have .content, list responses are arrays
        setTrips(res.data?.content || res.data || []);
      } catch (err) {
        console.error('Failed to load trips', err);
      }
    };
    load();
  }, [booking]);

  const validate = () => {
    const errs = {};
    if (!form.customerName.trim()) errs.customerName = 'Required';
    if (!form.customerPhone.trim()) errs.customerPhone = 'Required';
    if (!form.tripId) errs.tripId = 'Required';
    if (!form.seatCount || form.seatCount <= 0) errs.seatCount = 'Must be positive';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validate()) {
      onSubmit({
        ...form,
        tripId: parseInt(form.tripId),
        seatCount: parseInt(form.seatCount),
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
        <FormField label="Customer Name *" error={errors.customerName}>
          <input className="form-input" value={form.customerName} onChange={handleChange('customerName')} />
        </FormField>
        <FormField label="Customer Phone *" error={errors.customerPhone}>
          <input className="form-input" value={form.customerPhone} onChange={handleChange('customerPhone')} />
        </FormField>
        <FormField label="Customer Email">
          <input className="form-input" type="email" value={form.customerEmail} onChange={handleChange('customerEmail')} />
        </FormField>
        <FormField label="Trip *" error={errors.tripId}>
          <select className="form-input" value={form.tripId} onChange={handleChange('tripId')}>
            <option value="">Select trip</option>
            {trips.map(t => (
              <option key={t.id} value={t.id}>
                {t.routeOrigin} → {t.routeDestination} ({t.bookedSeats}/{t.totalSeats} seats)
              </option>
            ))}
          </select>
        </FormField>
        <FormField label="Seat Count *" error={errors.seatCount}>
          <input className="form-input" type="number" min="1" value={form.seatCount} onChange={handleChange('seatCount')} />
        </FormField>
        <FormField label="Notes">
          <textarea className="form-input form-textarea" value={form.notes} onChange={handleChange('notes')} />
        </FormField>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{booking ? 'Update' : 'Create'}</button>
      </div>
    </form>
  );
}

export default BookingForm;

