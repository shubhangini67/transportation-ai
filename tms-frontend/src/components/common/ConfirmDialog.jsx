import React from 'react';
import Modal from './Modal';
import '../../styles/components.css';

function ConfirmDialog({ isOpen, onClose, onConfirm, title, message }) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title || 'Confirm'}>
      <p className="confirm-message">{message || 'Are you sure?'}</p>
      <div className="confirm-actions">
        <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
        <button className="btn btn-danger" onClick={onConfirm}>Confirm</button>
      </div>
    </Modal>
  );
}

export default ConfirmDialog;

