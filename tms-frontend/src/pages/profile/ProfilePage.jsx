import React, { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { FiUser, FiSave, FiUpload } from 'react-icons/fi';
import profileService from '../../services/profileService';
import { useAuth } from '../../context/AuthContext';
import '../../styles/forms.css';

function ProfilePage() {
  const { user } = useAuth();
  const [form, setForm] = useState({ fullName: '', email: '' });
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    profileService.getProfile().then(res => {
      setForm({ fullName: res.data.fullName || '', email: res.data.email || '' });
      setAvatarUrl(res.data.avatarUrl || null);
      setLoading(false);
    }).catch(() => { setLoading(false); toast.error('Failed to load profile'); });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await profileService.updateProfile(form);
      toast.success('Profile updated');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    }
  };

  const handleAvatarUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      const res = await profileService.uploadAvatar(file);
      setAvatarUrl(res.data.avatarUrl);
      toast.success('Avatar updated');
    } catch (err) {
      toast.error('Avatar upload failed');
    }
  };

  if (loading) return <div className="page-loader">Loading...</div>;

  return (
    <div>
      <div className="page-header"><h2 className="page-title"><FiUser /> My profile</h2></div>
      <div style={{ maxWidth: 500, background: 'var(--white)', borderRadius: 'var(--radius)', padding: 24, boxShadow: 'var(--shadow-sm)' }}>
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <div style={{
            width: 80, height: 80, borderRadius: '50%', margin: '0 auto 12px',
            background: 'var(--gray-200)', display: 'flex', alignItems: 'center', justifyContent: 'center',
            overflow: 'hidden', fontSize: '2rem', color: 'var(--gray-500)'
          }}>
            {avatarUrl ? (
              <img src={avatarUrl} alt="Avatar" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            ) : (
              <FiUser />
            )}
          </div>
          <label className="btn btn-outline" style={{ cursor: 'pointer', display: 'inline-flex', fontSize: '0.8rem' }}>
            <FiUpload /> Upload Avatar
            <input type="file" accept="image/*" onChange={handleAvatarUpload} style={{ display: 'none' }} />
          </label>
        </div>
        <form onSubmit={handleSubmit} className="entity-form">
          <div style={{ marginBottom: 16 }}>
            <label className="form-label">Username</label>
            <input className="form-input" value={user?.username || ''} disabled />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="form-label">Role</label>
            <input className="form-input" value={user?.role || ''} disabled />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="form-label">Full Name</label>
            <input className="form-input" value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="form-label">Email</label>
            <input className="form-input" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
          </div>
          <button type="submit" className="btn btn-primary"><FiSave /> Save Changes</button>
        </form>
      </div>
    </div>
  );
}

export default ProfilePage;

