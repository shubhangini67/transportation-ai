import React, { createContext, useContext, useReducer } from 'react';
import api from '../api/axios';

const AuthContext = createContext(null);

const initialState = {
  user: JSON.parse(localStorage.getItem('user')),
  token: localStorage.getItem('token'),
  refreshToken: localStorage.getItem('refreshToken'),
  isAuthenticated: !!localStorage.getItem('token'),
  loading: false,
  error: null,
};

function authReducer(state, action) {
  switch (action.type) {
    case 'AUTH_START':
      return { ...state, loading: true, error: null };
    case 'AUTH_SUCCESS':
      return {
        ...state,
        loading: false,
        isAuthenticated: true,
        user: action.payload.user,
        token: action.payload.token,
        refreshToken: action.payload.refreshToken,
        error: null,
      };
    case 'AUTH_FAILURE':
      return { ...state, loading: false, error: action.payload };
    case 'CLEAR_ERROR':
      return { ...state, error: null };
    case 'LOGOUT':
      return { ...initialState, user: null, token: null, refreshToken: null, isAuthenticated: false };
    default:
      return state;
  }
}

function persistAuth(token, refreshToken, user) {
  localStorage.setItem('token', token);
  localStorage.setItem('refreshToken', refreshToken || '');
  localStorage.setItem('user', JSON.stringify(user));
}

export function AuthProvider({ children }) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  const login = async (username, password) => {
    dispatch({ type: 'AUTH_START' });
    try {
      const response = await api.post('/auth/login', { username, password });
      const { token, refreshToken, username: uname, email, fullName, role } = response.data;
      const user = { username: uname, email, fullName, role };

      persistAuth(token, refreshToken, user);
      dispatch({ type: 'AUTH_SUCCESS', payload: { user, token, refreshToken } });
      return true;
    } catch (error) {
      const message = error.response?.data?.message || 'Login failed';
      dispatch({ type: 'AUTH_FAILURE', payload: message });
      return false;
    }
  };

  const register = async (userData) => {
    dispatch({ type: 'AUTH_START' });
    try {
      const response = await api.post('/auth/register', userData);
      const { token, refreshToken, username, email, fullName, role } = response.data;
      const user = { username, email, fullName, role };

      persistAuth(token, refreshToken, user);
      dispatch({ type: 'AUTH_SUCCESS', payload: { user, token, refreshToken } });
      return { success: true };
    } catch (error) {
      const message =
        error.response?.data?.message ||
        error.response?.data?.validationErrors
          ? Object.values(error.response?.data?.validationErrors || {}).join('. ')
          : 'Registration failed';
      dispatch({ type: 'AUTH_FAILURE', payload: message });
      return { success: false, message };
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    dispatch({ type: 'LOGOUT' });
  };

  const clearError = () => {
    dispatch({ type: 'CLEAR_ERROR' });
  };

  const hasRole = (...roles) => {
    return state.user && roles.includes(state.user.role);
  };

  const loginWithOAuth = (token, refreshToken, user) => {
    persistAuth(token, refreshToken, user);
    dispatch({ type: 'AUTH_SUCCESS', payload: { user, token, refreshToken } });
  };

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout, clearError, hasRole, loginWithOAuth }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

