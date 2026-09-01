const { createProxyMiddleware } = require('http-proxy-middleware');

const backend = process.env.TMS_BACKEND_URL || 'http://127.0.0.1:8080';

module.exports = function setupProxy(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: backend,
      changeOrigin: true,
    })
  );
  // SockJS/STOMP lives at /ws. Webpack HMR is moved to /sockjs-node so they do not collide.
  app.use(
    '/ws',
    createProxyMiddleware({
      target: backend,
      changeOrigin: true,
      ws: true,
      logLevel: 'warn',
    })
  );
  app.use(
    '/uploads',
    createProxyMiddleware({
      target: backend,
      changeOrigin: true,
    })
  );
};
