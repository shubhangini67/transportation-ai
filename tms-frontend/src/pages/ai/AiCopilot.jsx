import React from 'react';
import { FiRefreshCw } from 'react-icons/fi';
import { useCopilot } from '../../context/CopilotContext';
import CopilotThread from '../../components/ai/CopilotThread';
import '../../styles/dashboard.css';
import '../../styles/forms.css';
import '../../styles/chatbot.css';

function AiCopilot() {
  const { live, reset, busy, setOpen, loadBriefing } = useCopilot();

  React.useEffect(() => {
    setOpen(false);
  }, [setOpen]);

  return (
    <div className="ai-page">
      <div className="page-header">
        <h2 className="page-title">Copilot</h2>
        <div className="ai-page-tools">
          <span className={`bot-pill ${live ? 'is-green' : 'is-yellow'}`}>
            {live ? 'Live chat' : 'Saved answers'}
          </span>
          <button type="button" className="btn btn-secondary" onClick={reset} disabled={busy}>
            <FiRefreshCw /> New chat
          </button>
          <button type="button" className="btn btn-secondary" onClick={loadBriefing} disabled={busy}>
            Shift board
          </button>
        </div>
      </div>
      <p className="insight-engine">
        Green means live conversation. Ask Copilot to add, update, or delete crew, fleet, lanes, bookings, costs, or zones — it fills the form, then gives you a list link.
      </p>
      <div className="ai-page-chat">
        <CopilotThread />
      </div>
    </div>
  );
}

export default AiCopilot;
