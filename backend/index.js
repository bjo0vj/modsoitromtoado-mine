const express = require('express');
const cors = require('cors');
require('dotenv').config();

const db = require('./config/database'); // Triggers auto-schema init
const pingRoutes = require('./routes/ping');
const playerRoutes = require('./routes/player');
const heartbeatRoutes = require('./routes/heartbeat');
const eventRoutes = require('./routes/event');
const proximityRoutes = require('./routes/proximity');
const dashboardRoutes = require('./routes/dashboard');
const auth = require('./middleware/auth');
const tokenAuth = require('./middleware/tokenAuth');

const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(cors({
    origin: process.env.CORS_ORIGIN || '*',
    methods: ['GET', 'POST']
}));
app.use(express.json({ limit: '1mb' }));

// Public Routes
app.use('/ping', pingRoutes);

// Protected API Routes
app.use('/api/player', playerRoutes);
app.use('/api/heartbeat', heartbeatRoutes);
app.use('/api/event', eventRoutes);
app.use('/api/proximity', proximityRoutes);

app.get('/api/config', auth, (req, res) => {
    res.json({
        heartbeatInterval: parseInt(process.env.HEARTBEAT_INTERVAL) || 300
    });
});

// Dashboard Login API
app.post('/api/dashboard/login', (req, res) => {
    const { username, password } = req.body;
    // Hardcoded credentials as requested
    if (username === 'zuy' && password === '3667') {
        res.json({ success: true, token: 'fubabeo-admin-token-3667' });
    } else {
        res.status(401).json({ success: false, message: 'Sai tài khoản hoặc mật khẩu' });
    }
});

// Dashboard API (protected by tokenAuth)
app.use('/api/dashboard', tokenAuth, dashboardRoutes);

// Serve Dashboard (Static files - no longer blocked by basicAuth)
app.use(express.static('public'));

// Global error handler — prevents unhandled errors from crashing the server
app.use((err, req, res, next) => {
    console.error('[UNHANDLED ERROR]', err.stack);
    res.status(500).json({
        success: false,
        message: 'Internal Server Error'
    });
});

app.listen(port, () => {
    console.log(`🚀 fubabeo.mod backend running on port ${port}`);
});
