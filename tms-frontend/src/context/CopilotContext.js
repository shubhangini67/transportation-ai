import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from './AuthContext';
import aiService from '../services/aiService';

const CopilotContext = createContext(null);
const STORAGE_OPEN = 'tms-copilot-open';

function timeGreeting() {
  const h = new Date().getHours();
  if (h < 12) return 'good morning';
  if (h < 17) return 'good afternoon';
  return 'good evening';
}

export function CopilotProvider({ children }) {
  const { hasRole, user } = useAuth();
  const enabled = hasRole('ADMIN', 'DISPATCHER');
  const location = useLocation();
  const [open, setOpen] = useState(() => sessionStorage.getItem(STORAGE_OPEN) === '1');
  const [live, setLive] = useState(false);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [messages, setMessages] = useState([]);
  const started = useRef(false);
  const inflight = useRef(false);

  useEffect(() => {
    sessionStorage.setItem(STORAGE_OPEN, open ? '1' : '0');
  }, [open]);

  useEffect(() => {
    if (!user) {
      setMessages([]);
      setOpen(false);
      started.current = false;
    }
  }, [user]);

  useEffect(() => {
    if (!enabled) return;
    aiService.status()
      .then((res) => setLive(!!res.data?.live))
      .catch(() => {});
  }, [enabled]);

  const noteLive = (data) => {
    if (typeof data?.live === 'boolean') setLive(data.live);
    else if (typeof data?.usedLlm === 'boolean') setLive(data.usedLlm);
  };

  const loadWelcome = useCallback(async () => {
    if (!enabled || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    try {
      const res = await aiService.ask(timeGreeting(), { pagePath: location.pathname, history: [] });
      setMessages([{ role: 'assistant', ...res.data }]);
      noteLive(res.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Copilot could not start');
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }, [enabled, location.pathname]);

  const loadBriefing = useCallback(async () => {
    if (!enabled || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    try {
      const res = await aiService.ask('briefing', { pagePath: location.pathname, history: [] });
      setMessages([{ role: 'assistant', ...res.data }]);
      noteLive(res.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Copilot could not load the board');
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }, [enabled, location.pathname]);

  useEffect(() => {
    const onAiPage = location.pathname === '/ai';
    if (!enabled) return;
    if (!open && !onAiPage) return;
    if (started.current || messages.length > 0) return;
    started.current = true;
    loadWelcome();
  }, [enabled, loadWelcome, location.pathname, messages.length, open]);

  const send = useCallback(async (text) => {
    const message = (text || input).trim();
    if (!message || inflight.current || !enabled) return;
    setInput('');
    setMessages((m) => [...m, { role: 'user', answer: message }]);
    inflight.current = true;
    setBusy(true);
    try {
      const history = messages
        .filter((m) => m.answer)
        .slice(-6)
        .map((m) => ({ role: m.role, content: m.answer }));
      const res = await aiService.ask(message, { pagePath: location.pathname, history });
      setMessages((m) => [...m, { role: 'assistant', ...res.data }]);
      noteLive(res.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Copilot is unavailable');
      setLive(false);
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }, [enabled, input, location.pathname, messages]);

  const reset = useCallback(() => {
    started.current = true;
    setMessages([]);
    setInput('');
    aiService.reset().catch(() => {});
    loadWelcome();
  }, [loadWelcome]);

  const toggle = useCallback(() => setOpen((v) => !v), []);

  const value = useMemo(() => ({
    enabled, open, setOpen, toggle, live, input, setInput, busy, messages, send, reset, loadBriefing,
  }), [busy, enabled, input, live, loadBriefing, messages, open, reset, send, toggle]);

  return <CopilotContext.Provider value={value}>{children}</CopilotContext.Provider>;
}

export function useCopilot() {
  const ctx = useContext(CopilotContext);
  if (!ctx) throw new Error('useCopilot must be used inside CopilotProvider');
  return ctx;
}
