const express = require('express');
const router = express.Router();
const db = require('../config/database');
const { sendSuccess, sendError } = require('../utils/response');

// GET /api/dashboard/players
router.get('/players', async (req, res) => {
    try {
        const query = `
            SELECT id, player_uuid, ign, server_ip, mod_version, 
                   last_x, last_y, last_z, last_dimension, last_online 
            FROM players
            ORDER BY last_online DESC NULLS LAST
        `;
        const result = await db.query(query);
        return sendSuccess(res, result.rows, 'Players retrieved successfully');
    } catch (error) {
        return sendError(res, 500, 'Error fetching players', error);
    }
});

// GET /api/dashboard/player/:uuid
router.get('/player/:uuid', async (req, res) => {
    const { uuid } = req.params;
    try {
        // Fetch player
        const playerQuery = `SELECT * FROM players WHERE player_uuid = $1`;
        const playerRes = await db.query(playerQuery, [uuid]);
        
        if (playerRes.rows.length === 0) {
            return sendError(res, 404, 'Player not found');
        }
        const player = playerRes.rows[0];
        const playerId = player.id;

        // Fetch recent events (deaths & blocks)
        const eventsQuery = `
            SELECT id, event_type, x, y, z, dimension, server_ip, created_at 
            FROM events 
            WHERE player_id = $1 
            ORDER BY created_at DESC 
            LIMIT 50
        `;
        const eventsRes = await db.query(eventsQuery, [playerId]);

        // Fetch waypoints
        const waypointsQuery = `
            SELECT id, name, x, y, z, dimension, server_ip, created_at 
            FROM waypoints 
            WHERE player_id = $1 
            ORDER BY created_at DESC
        `;
        const waypointsRes = await db.query(waypointsQuery, [playerId]);

        // Fetch latest heartbeats
        const heartbeatsQuery = `
            SELECT id, x, y, z, dimension, server_ip, created_at 
            FROM heartbeats 
            WHERE player_id = $1 
            ORDER BY created_at DESC 
            LIMIT 10
        `;
        const hbRes = await db.query(heartbeatsQuery, [playerId]);

        return sendSuccess(res, {
            player,
            events: eventsRes.rows,
            waypoints: waypointsRes.rows,
            heartbeats: hbRes.rows
        }, 'Player details retrieved');
    } catch (error) {
        return sendError(res, 500, 'Error fetching player details', error);
    }
});

module.exports = router;
