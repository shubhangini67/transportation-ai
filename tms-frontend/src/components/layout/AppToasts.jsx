import React from 'react';
import { ToastContainer } from 'react-toastify';
import { useTheme } from '../../context/ThemeContext';

function AppToasts() {
  const { theme } = useTheme();
  return <ToastContainer position="top-right" autoClose={3000} theme={theme === 'dark' ? 'dark' : 'light'} />;
}

export default AppToasts;
