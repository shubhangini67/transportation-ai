import React from 'react';
import { useLocation } from 'react-router-dom';
import { FiMessageCircle, FiMinus, FiRefreshCw } from 'react-icons/fi';
import { useCopilot } from '../../context/CopilotContext';
import CopilotThread from './CopilotThread';
import '../../styles/chatbot.css';

function FloatingCopilot() {
  const { enabled, open, toggle, live, reset, busy, setOpen } = useCopilot();
  const location = useLocation();

  if (!enabled || location.pathname === '/ai') return null;

  return (
    <div className="fab-copilot">
      {open && (
        <section className="fab-panel" aria-label="Copilot">
          <header className="fab-header">
            <div>
              <strong>Copilot</strong>
              <small className={live ? 'bot-status-live' : 'bot-status-assist'}>
                {live ? 'Live chat' : 'Saved answers'}
              </small>
            </div>
            <div className="fab-header-actions">
              <button type="button" className="fab-icon-btn" onClick={reset} disabled={busy} title="New chat">
                <FiRefreshCw />
              </button>
              <button type="button" className="fab-icon-btn" onClick={toggle} title="Minimize">
                <FiMinus />
              </button>
            </div>
          </header>
          <CopilotThread onNavigate={() => setOpen(false)} />
        </section>
      )}
      <button
        type="button"
        className={`fab-launcher ${open ? 'is-open' : ''} ${live ? 'is-live' : ''}`}
        onClick={toggle}
        aria-label={open ? 'Close Copilot' : 'Open Copilot'}
      >
        <FiMessageCircle />
        {!open && <span className="fab-pulse" />}
      </button>
    </div>
  );
}

export default FloatingCopilot;
