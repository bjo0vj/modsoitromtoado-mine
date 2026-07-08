import express from 'express';
import cors from 'cors';
import 'dotenv/config.js';

import * as db from './config/database.js'; // Triggers auto-schema init
import pingRoutes from './routes/ping.js';
import playerRoutes from './routes/player.js';
import heartbeatRoutes from './routes/heartbeat.js';
import eventRoutes from './routes/event.js';
import proximityRoutes from './routes/proximity.js';
import dashboardRoutes from './routes/dashboard.js';
import auth from './middleware/auth.js';

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

// Dashboard API (public)
app.use('/api/dashboard', dashboardRoutes);

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
