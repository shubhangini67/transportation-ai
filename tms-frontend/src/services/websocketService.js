import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api/v1';
const WS_URL = API_BASE.replace('/api/v1', '') + '/ws';

let client = null;
const subscriptions = {};

const websocketService = {
  connect(token) {
    if (client?.connected) return Promise.resolve();

    return new Promise((resolve, reject) => {
      client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('WebSocket connected');
          resolve();
        },
        onStompError: (frame) => {
          console.error('STOMP error', frame);
          reject(frame);
        },
      });
      client.activate();
    });
  },

  subscribe(destination, callback) {
    if (!client?.connected) {
      console.warn('WebSocket not connected, queuing subscription:', destination);
      return null;
    }
    const sub = client.subscribe(destination, (message) => {
      try {
        callback(JSON.parse(message.body));
      } catch {
        callback(message.body);
      }
    });
    subscriptions[destination] = sub;
    return sub;
  },

  unsubscribe(destination) {
    if (subscriptions[destination]) {
      subscriptions[destination].unsubscribe();
      delete subscriptions[destination];
    }
  },

  send(destination, body) {
    if (client?.connected) {
      client.publish({ destination, body: JSON.stringify(body) });
    }
  },

  disconnect() {
    if (client) {
      client.deactivate();
      client = null;
    }
  },

  isConnected() {
    return client?.connected || false;
  },
};

export default websocketService;

