import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FiLock, FiUser, FiMail, FiUserCheck } from 'react-icons/fi';
import { toast } from 'react-toastify';
import AuthShell from '../components/brand/AuthShell';
import '../styles/forms.css';

const ROLE_OPTIONS = [
  { value: 'CLIENT', label: 'Client' },
  { value: 'DISPATCHER', label: 'Dispatcher' },
  { value: 'DRIVER', label: 'Driver' },
];

function Register() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    fullName: '',
    role: 'CLIENT',
  });
  const [formErrors, setFormErrors] = useState({});
  const { register, loading, error } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const validate = () => {
    const errors = {};
    if (!formData.fullName.trim()) errors.fullName = 'Full name is required';
    if (!formData.username.trim()) errors.username = 'Username is required';
    else if (formData.username.length < 3) errors.username = 'Username must be at least 3 characters';
    if (!formData.email.trim()) errors.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) errors.email = 'Invalid email format';
    if (!formData.password) errors.password = 'Password is required';
    else if (formData.password.length < 6) errors.password = 'Password must be at least 6 characters';
    if (formData.password !== formData.confirmPassword) errors.confirmPassword = 'Passwords do not match';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    const { confirmPassword, ...payload } = formData;
    const result = await register(payload);
    if (result.success) {
      toast.success('Account created successfully!');
      navigate('/dashboard');
    } else {
      toast.error(result.message || 'Registration failed');
    }
  };

  return (
    <AuthShell kicker="New account">
      <div className="auth-panel-inner">
        <header className="auth-panel-head">
          <h2>Create an account</h2>
          <p>For the demo, click Admin, Dispatcher, Driver, or Customer on the sign-in page.</p>
        </header>
        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-field">
            <label className="form-label">Full Name</label>
            <div className="input-icon-wrapper">
              <FiUserCheck className="input-icon" />
              <input
                type="text"
                className="form-input"
                name="fullName"
                placeholder="Enter full name"
                value={formData.fullName}
                onChange={handleChange}
              />
            </div>
            {formErrors.fullName && <span className="form-error">{formErrors.fullName}</span>}
          </div>

          <div className="form-field">
            <label className="form-label">Username</label>
            <div className="input-icon-wrapper">
              <FiUser className="input-icon" />
              <input
                type="text"
                className="form-input"
                name="username"
                placeholder="Choose a username"
                value={formData.username}
                onChange={handleChange}
              />
            </div>
            {formErrors.username && <span className="form-error">{formErrors.username}</span>}
          </div>

          <div className="form-field">
            <label className="form-label">Email</label>
            <div className="input-icon-wrapper">
              <FiMail className="input-icon" />
              <input
                type="email"
                className="form-input"
                name="email"
                placeholder="Enter email address"
                value={formData.email}
                onChange={handleChange}
              />
            </div>
            {formErrors.email && <span className="form-error">{formErrors.email}</span>}
          </div>

          <div className="form-field">
            <label className="form-label">Password</label>
            <div className="input-icon-wrapper">
              <FiLock className="input-icon" />
              <input
                type="password"
                className="form-input"
                name="password"
                placeholder="Create a password"
                value={formData.password}
                onChange={handleChange}
              />
            </div>
            {formErrors.password && <span className="form-error">{formErrors.password}</span>}
          </div>

          <div className="form-field">
            <label className="form-label">Confirm Password</label>
            <div className="input-icon-wrapper">
              <FiLock className="input-icon" />
              <input
                type="password"
                className="form-input"
                name="confirmPassword"
                placeholder="Confirm your password"
                value={formData.confirmPassword}
                onChange={handleChange}
              />
            </div>
            {formErrors.confirmPassword && <span className="form-error">{formErrors.confirmPassword}</span>}
          </div>

          <div className="form-field">
            <label className="form-label">Role</label>
            <select
              className="form-input"
              name="role"
              value={formData.role}
              onChange={handleChange}
            >
              {ROLE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          {error && <div className="form-error-banner">{error}</div>}

          <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
            {loading ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>
        <p className="auth-alt">
          Already have an account? <Link to="/login" className="auth-link">Sign in</Link>
        </p>
      </div>
    </AuthShell>
  );
}

export default Register;

