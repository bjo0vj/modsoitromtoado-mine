const express = require('express');
const router = express.Router();
const db = require('../config/database');
const { sendSuccess, sendError } = require('../utils/response');
const auth = require('../middleware/auth');

router.post('/login', auth, async (req, res) => {
    const { uuid, ign, serverIp, modVersion } = req.body;

    if (!uuid || !ign) {
        return sendError(res, 400, 'UUID and IGN are required');
    }

    try {
        // Upsert logic: Update if exists, insert if not
        const query = `
            INSERT INTO players (player_uuid, ign, server_ip, mod_version, last_online)
            VALUES ($1, $2, $3, $4, NOW())
            ON CONFLICT (player_uuid) 
            DO UPDATE SET 
                ign = EXCLUDED.ign,
                server_ip = EXCLUDED.server_ip,
                mod_version = EXCLUDED.mod_version,
                last_online = NOW()
            RETURNING id, player_uuid, ign, is_live_tracking;
        `;
        const values = [uuid, ign, serverIp || null, modVersion || null];
        const result = await db.query(query, values);
        
        return sendSuccess(res, result.rows[0], 'Player login recorded successfully');
    } catch (error) {
        return sendError(res, 500, 'Database error during player login', error);
    }
});

module.exports = router;
