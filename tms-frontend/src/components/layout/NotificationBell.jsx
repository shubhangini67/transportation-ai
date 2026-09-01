import React, { useEffect, useState, useRef, useCallback } from 'react';
import { FiBell } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import notificationService from '../../services/notificationService';
import websocketService from '../../services/websocketService';
import '../../styles/components.css';

function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [open, setOpen] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();

  const loadUnreadCount = useCallback(async () => {
    try {
      const res = await notificationService.getUnreadCount();
      setUnreadCount(res.data?.count || 0);
    } catch { /* ignore */ }
  }, []);

  const loadNotifications = useCallback(async () => {
    try {
      const res = await notificationService.getAll({ page: 0, size: 15 });
      setNotifications(res.data?.content || []);
      setLoaded(true);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    loadUnreadCount();
    const interval = setInterval(loadUnreadCount, 30000);
    return () => clearInterval(interval);
  }, [loadUnreadCount]);

  // WebSocket subscription for real-time notifications
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;
    let mounted = true;
    const connectWs = async () => {
      try {
        await websocketService.connect(token);
        if (!mounted) return;
        websocketService.subscribe('/user/queue/notifications', (notification) => {
          if (!mounted) return;
          setUnreadCount(prev => prev + 1);
          setNotifications(prev => [notification, ...prev].slice(0, 15));
        });
      } catch { /* WebSocket optional */ }
    };
    connectWs();
    return () => { mounted = false; websocketService.unsubscribe('/user/queue/notifications'); };
  }, []);

  useEffect(() => {
    const handleClickOutside = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggle = () => {
    if (!open && !loaded) loadNotifications();
    setOpen(!open);
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllRead();
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    } catch { /* ignore */ }
  };

  const handleClick = async (n) => {
    if (!n.read) {
      try {
        await notificationService.markAsRead(n.id);
        setUnreadCount(prev => Math.max(0, prev - 1));
        setNotifications(prev => prev.map(x => x.id === n.id ? { ...x, read: true } : x));
      } catch { /* ignore */ }
    }
    if (n.link) { navigate(n.link); setOpen(false); }
  };

  const timeAgo = (dateStr) => {
    if (!dateStr) return '';
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  };

  return (
    <div className="notification-bell-wrapper" ref={ref}>
      <button className="btn-icon notification-bell" onClick={handleToggle} title="Notifications">
        <FiBell />
        {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
      </button>
      {open && (
        <div className="notification-dropdown">
          <div className="notification-dropdown-header">
            <strong>Notifications</strong>
            {unreadCount > 0 && <button className="btn-link" onClick={handleMarkAllRead}>Mark all read</button>}
          </div>
          <div className="notification-dropdown-body">
            {notifications.length === 0 ? (
              <div className="notification-empty">No notifications</div>
            ) : notifications.map(n => (
              <div key={n.id} className={`notification-item ${!n.read ? 'unread' : ''}`} onClick={() => handleClick(n)}>
                <div className="notification-item-title">{n.title}</div>
                <div className="notification-item-message">{n.message}</div>
                <div className="notification-item-time">{timeAgo(n.createdAt)}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default NotificationBell;

