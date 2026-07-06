const express = require('express');
const router = express.Router();
const db = require('../config/database');
const { sendSuccess, sendError } = require('../utils/response');
const auth = require('../middleware/auth');

async function getPlayerId(uuid, ign, serverIp) {
    const playerQuery = `
        INSERT INTO players (player_uuid, ign, server_ip, last_online)
        VALUES ($1, $2, $3, NOW())
        ON CONFLICT (player_uuid) 
        DO UPDATE SET ign = EXCLUDED.ign, last_online = NOW()
        RETURNING id;
    `;
    const result = await db.query(playerQuery, [uuid, ign, serverIp]);
    return result.rows[0].id;
}

// POST /api/proximity/scan
router.post('/scan', auth, async (req, res) => {
    const { uuid, ign, eventType, x, y, z, dimension, serverIp, nearbyPlayers, closestPlayer } = req.body;

    if (!uuid || !ign || !eventType || x === undefined || y === undefined || z === undefined || !dimension || !nearbyPlayers) {
        return sendError(res, 400, 'Missing required data');
    }

    if (eventType !== 'NEARBY_CHANGED' && eventType !== 'NEARBY_STABLE_10M') {
        return sendError(res, 400, 'Invalid eventType');
    }

    try {
        const playerId = await getPlayerId(uuid, ign, serverIp);
        const query = `
            INSERT INTO proximity_logs (player_id, event_type, x, y, z, dimension, nearby_players, closest_player, server_ip)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
        `;
        // nearbyPlayers should be a JSON array string
        const nearbyJson = Array.isArray(nearbyPlayers) ? JSON.stringify(nearbyPlayers) : nearbyPlayers;
        
        await db.query(query, [playerId, eventType, x, y, z, dimension, nearbyJson, closestPlayer, serverIp]);
        console.log(`[${serverIp}] - [PROXIMITY: ${eventType}] - Players: ${nearbyJson}`);
        return sendSuccess(res, null, 'Proximity event recorded');
    } catch (error) {
        return sendError(res, 500, 'Error recording proximity event', error);
    }
});

module.exports = router;
