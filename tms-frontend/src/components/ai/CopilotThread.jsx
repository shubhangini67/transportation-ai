import React, { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { FiSend } from 'react-icons/fi';
import { useCopilot } from '../../context/CopilotContext';

export const COPILOT_PROMPTS = [
  'Add a new driver',
  'Delete a vehicle',
  'Which trips are delayed?',
  'Who should I dispatch Delhi to Jaipur?',
];

function CopilotThread({ onNavigate }) {
  const { messages, busy, send, input, setInput } = useCopilot();
  const bottom = useRef(null);

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, busy]);

  const last = [...messages].reverse().find((m) => m.role === 'assistant');
  const chips = last?.suggestions?.length ? last.suggestions : COPILOT_PROMPTS;

  return (
    <>
      <div className="fab-chat-body">
        {messages.map((msg, i) => (
          <div key={i} className={`ai-bubble ai-bubble-${msg.role}`}>
            {msg.role === 'assistant' && <span className="ai-bot-label">Copilot</span>}
            <p className="ai-bubble-text">{msg.answer}</p>
            {msg.role === 'assistant' && msg.links?.length > 0 && (
              <div className="ai-links">
                {msg.links.map((l, idx) => (
                  <Link
                    key={`${l.path}-${l.label}`}
                    className={`btn btn-sm ${idx === 0 ? 'btn-primary' : 'btn-secondary'}`}
                    to={l.path}
                    onClick={onNavigate}
                  >
                    {l.label}
                  </Link>
                ))}
              </div>
            )}
          </div>
        ))}
        {busy && (
          <div className="ai-bubble ai-bubble-assistant">
            <span className="ai-typing"><i /><i /><i /></span>
            Copilot is typing…
          </div>
        )}
        <div ref={bottom} />
      </div>

      <div className="hub-chips fab-chips">
        {chips.slice(0, 4).map((p) => (
          <button key={p} type="button" className="hub-chip" disabled={busy} onClick={() => send(p)}>{p}</button>
        ))}
      </div>

      <form className="ai-composer" onSubmit={(e) => { e.preventDefault(); send(); }}>
        <input
          className="form-input"
          placeholder="Add crew, delete a truck, or ask about the fleet…"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
        <button className="btn btn-primary" type="submit" disabled={busy} aria-label="Send"><FiSend /></button>
      </form>
    </>
  );
}

export default CopilotThread;
