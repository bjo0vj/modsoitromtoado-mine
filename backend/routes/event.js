const express = require('express');
const router = express.Router();
const db = require('../config/database');
const { sendSuccess, sendError } = require('../utils/response');
const auth = require('../middleware/auth');

// Helper to get or create player ID
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

// POST /api/event/death
router.post('/death', auth, async (req, res) => {
    const { uuid, ign, x, y, z, dimension, serverIp } = req.body;

    if (!uuid || !ign || x === undefined || y === undefined || z === undefined || !dimension) {
        return sendError(res, 400, 'Missing required data (uuid, ign, x, y, z, dimension)');
    }

    try {
        const playerId = await getPlayerId(uuid, ign, serverIp);
        const query = `
            INSERT INTO events (player_id, event_type, x, y, z, dimension, server_ip)
            VALUES ($1, 'death', $2, $3, $4, $5, $6)
        `;
        await db.query(query, [playerId, x, y, z, dimension, serverIp]);
        console.log(`[${serverIp}] - [DEATH] - [${x} ${y} ${z}]`);
        return sendSuccess(res, null, 'Death event recorded');
    } catch (error) {
        return sendError(res, 500, 'Error recording death', error);
    }
});

// POST /api/event/block_place
router.post('/block_place', auth, async (req, res) => {
    const { uuid, ign, blockType, x, y, z, dimension, serverIp } = req.body;

    if (!uuid || !ign || !blockType || x === undefined || y === undefined || z === undefined || !dimension) {
        return sendError(res, 400, 'Missing required data');
    }

    // Validate blockType whitelist
    const validTypes = ['bed', 'chest', 'ender_chest', 'enchanting_table', 'shulker_box'];
    if (!validTypes.includes(blockType)) {
        return sendError(res, 400, `Invalid block type: ${blockType}`);
    }

    try {
        const playerId = await getPlayerId(uuid, ign, serverIp);
        const query = `
            INSERT INTO events (player_id, event_type, x, y, z, dimension, server_ip)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
        `;
        await db.query(query, [playerId, blockType, x, y, z, dimension, serverIp]);
        console.log(`[${serverIp}] - [${blockType}] - [${x} ${y} ${z}]`);
        return sendSuccess(res, null, 'Block place event recorded');
    } catch (error) {
        return sendError(res, 500, 'Error recording block placement', error);
    }
});

// POST /api/event/waypoint
router.post('/waypoint', auth, async (req, res) => {
    const { uuid, ign, name, x, y, z, dimension, serverIp } = req.body;

    if (!uuid || !ign || !name || x === undefined || y === undefined || z === undefined || !dimension) {
        return sendError(res, 400, 'Missing required data');
    }

    // Sanitize waypoint name
    const safeName = name.substring(0, 50).replace(/[<>\"'&]/g, '');

    try {
        const playerId = await getPlayerId(uuid, ign, serverIp);
        const query = `
            INSERT INTO waypoints (player_id, name, x, y, z, dimension, server_ip)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
        `;
        await db.query(query, [playerId, safeName, x, y, z, dimension, serverIp]);
        console.log(`[${serverIp}] - [WAYPOINT: ${safeName}] - [${x} ${y} ${z}]`);
        return sendSuccess(res, null, 'Waypoint recorded');
    } catch (error) {
        return sendError(res, 500, 'Error recording waypoint', error);
    }
});

module.exports = router;
