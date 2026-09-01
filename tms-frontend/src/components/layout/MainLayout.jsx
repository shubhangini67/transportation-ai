import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import FloatingCopilot from '../ai/FloatingCopilot';
import { CopilotProvider } from '../../context/CopilotContext';
import '../../styles/layout.css';

function MainLayout() {
  return (
    <CopilotProvider>
      <div className="app-layout">
        <Sidebar />
        <div className="main-content">
          <Navbar />
          <main className="page-content">
            <Outlet />
          </main>
        </div>
        <FloatingCopilot />
      </div>
    </CopilotProvider>
  );
}

export default MainLayout;
