const express = require('express');
const router = express.Router();
const db = require('../config/database');
const { sendSuccess, sendError } = require('../utils/response');
const auth = require('../middleware/auth');

router.post('/', auth, async (req, res) => {
    const { uuid, ign, x, y, z, dimension, serverIp, timestamp } = req.body;

    if (!uuid || !ign || x === undefined || y === undefined || z === undefined || !dimension) {
        return sendError(res, 400, 'Missing required heartbeat data');
    }

    try {
        // First ensure player exists/update their info
        const playerQuery = `
            INSERT INTO players (player_uuid, ign, server_ip, last_x, last_y, last_z, last_dimension, last_online)
            VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
            ON CONFLICT (player_uuid) 
            DO UPDATE SET 
                ign = EXCLUDED.ign,
                server_ip = EXCLUDED.server_ip,
                last_x = EXCLUDED.last_x,
                last_y = EXCLUDED.last_y,
                last_z = EXCLUDED.last_z,
                last_dimension = EXCLUDED.last_dimension,
                last_online = NOW()
            RETURNING id;
        `;
        const playerResult = await db.query(playerQuery, [uuid, ign, serverIp, x, y, z, dimension]);
        const playerId = playerResult.rows[0].id;

        // Insert heartbeat record
        const hbQuery = `
            INSERT INTO heartbeats (player_id, x, y, z, dimension, server_ip, created_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
        `;
        const hbTime = timestamp ? new Date(timestamp) : new Date();
        await db.query(hbQuery, [playerId, x, y, z, dimension, serverIp, hbTime]);

        // Console log 10% of the time to avoid spamming the logs too much, or maybe log all? 
        // Let's log it all but keep it short
        console.log(`[${serverIp}] - [HEARTBEAT] - [${x} ${y} ${z}]`);

        return sendSuccess(res, null, 'Heartbeat recorded successfully');
    } catch (error) {
        return sendError(res, 500, 'Database error recording heartbeat', error);
    }
});

module.exports = router;
