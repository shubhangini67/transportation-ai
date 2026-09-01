import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';

function OAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithOAuth } = useAuth();

  useEffect(() => {
    const token = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');
    const username = searchParams.get('username');
    const email = searchParams.get('email');
    const fullName = searchParams.get('fullName');
    const role = searchParams.get('role');
    const error = searchParams.get('error');

    if (error) {
      toast.error(error);
      navigate('/login');
      return;
    }

    if (token && username) {
      const user = { username, email, fullName, role };
      loginWithOAuth(token, refreshToken, user);
      toast.success('Login successful!');
      navigate('/dashboard');
    } else {
      toast.error('OAuth login failed');
      navigate('/login');
    }
  }, [searchParams, navigate, loginWithOAuth]);

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
      <p>Processing login...</p>
    </div>
  );
}

export default OAuthCallback;

