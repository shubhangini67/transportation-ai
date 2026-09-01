import React, { useState } from 'react';
import FormField from '../../components/common/FormField';

function InvoiceForm({ invoice, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    clientName: invoice?.clientName || '',
    clientEmail: invoice?.clientEmail || '',
    taxRate: invoice?.taxRate || '18.00',
    notes: invoice?.notes || '',
    dueDate: invoice?.dueDate || '',
    items: invoice?.items?.length > 0
      ? invoice.items.map(i => ({ description: i.description, quantity: i.quantity, unitPrice: i.unitPrice }))
      : [{ description: '', quantity: 1, unitPrice: '' }],
  });

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleItemChange = (idx, field, value) => {
    const items = [...form.items];
    items[idx] = { ...items[idx], [field]: value };
    setForm({ ...form, items });
  };

  const addItem = () => setForm({ ...form, items: [...form.items, { description: '', quantity: 1, unitPrice: '' }] });
  const removeItem = (idx) => setForm({ ...form, items: form.items.filter((_, i) => i !== idx) });

  const subtotal = form.items.reduce((sum, it) => sum + (Number(it.unitPrice) || 0) * (Number(it.quantity) || 0), 0);
  const taxAmount = subtotal * (Number(form.taxRate) || 0) / 100;
  const total = subtotal + taxAmount;

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...form,
      taxRate: Number(form.taxRate),
      items: form.items.map(it => ({ ...it, quantity: Number(it.quantity), unitPrice: Number(it.unitPrice) })),
    });
  };

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Client Name"><input className="form-input" name="clientName" value={form.clientName} onChange={handleChange} required /></FormField>
      <FormField label="Client Email"><input className="form-input" name="clientEmail" type="email" value={form.clientEmail} onChange={handleChange} /></FormField>
      <FormField label="Tax Rate (%)"><input className="form-input" name="taxRate" type="number" step="0.01" value={form.taxRate} onChange={handleChange} /></FormField>
      <FormField label="Due Date"><input className="form-input" name="dueDate" type="date" value={form.dueDate} onChange={handleChange} /></FormField>
      <FormField label="Notes"><textarea className="form-input" name="notes" value={form.notes} onChange={handleChange} rows={2} /></FormField>

      <h4 style={{ margin: '16px 0 8px' }}>Line Items</h4>
      {form.items.map((item, idx) => (
        <div key={idx} style={{ display: 'flex', gap: 8, marginBottom: 8, alignItems: 'flex-end' }}>
          <input className="form-input" placeholder="Description" value={item.description} onChange={e => handleItemChange(idx, 'description', e.target.value)} required style={{ flex: 3 }} />
          <input className="form-input" placeholder="Qty" type="number" min="1" value={item.quantity} onChange={e => handleItemChange(idx, 'quantity', e.target.value)} required style={{ flex: 1 }} />
          <input className="form-input" placeholder="Unit Price" type="number" step="0.01" value={item.unitPrice} onChange={e => handleItemChange(idx, 'unitPrice', e.target.value)} required style={{ flex: 1 }} />
          <button type="button" className="btn btn-danger" onClick={() => removeItem(idx)} style={{ padding: '6px 10px' }}>✕</button>
        </div>
      ))}
      <button type="button" className="btn btn-secondary" onClick={addItem} style={{ marginBottom: 12 }}>+ Add Item</button>

      <div style={{ background: 'var(--bg-secondary)', padding: 12, borderRadius: 'var(--radius)', marginBottom: 16 }}>
        <div>Subtotal: <strong>₹{subtotal.toFixed(2)}</strong></div>
        <div>Tax ({form.taxRate}%): <strong>₹{taxAmount.toFixed(2)}</strong></div>
        <div style={{ fontSize: 18, marginTop: 4 }}>Total: <strong>₹{total.toFixed(2)}</strong></div>
      </div>

      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
        <button type="submit" className="btn btn-primary">{invoice ? 'Update' : 'Create'} Invoice</button>
      </div>
    </form>
  );
}

export default InvoiceForm;

