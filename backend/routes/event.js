import express from 'express';
const router = express.Router();
import * as db from '../config/database.js';
import { sendSuccess, sendError } from '../utils/response.js';
import auth from '../middleware/auth.js';

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

// POST /api/event/block_place
router.post('/block_place', auth, async (req, res) => {
    const { uuid, ign, blockType, x, y, z, dimension, serverIp } = req.body;

    if (!uuid || !ign || !blockType || x === undefined || y === undefined || z === undefined || !dimension) {
        return sendError(res, 400, 'Missing required data');
    }

    // Validate blockType whitelist
    const validTypes = [
        'minecraft:chest', 'minecraft:barrel', 'minecraft:ender_chest', 'minecraft:enchanting_table', 'minecraft:hopper',
        'minecraft:white_bed', 'minecraft:red_bed', 'minecraft:black_bed', 'minecraft:blue_bed', 'minecraft:brown_bed', 
        'minecraft:cyan_bed', 'minecraft:gray_bed', 'minecraft:green_bed', 'minecraft:light_blue_bed', 'minecraft:light_gray_bed', 
        'minecraft:lime_bed', 'minecraft:magenta_bed', 'minecraft:orange_bed', 'minecraft:pink_bed', 'minecraft:purple_bed', 'minecraft:yellow_bed',
        'OPEN:chest', 'OPEN:ender_chest', 'OPEN:barrel'
    ];
    if (!validTypes.includes(blockType)) {
        return sendError(res, 400, `Invalid block type: ${blockType}`);
    }

    try {
        const playerId = await getPlayerId(uuid, ign, serverIp);
        const query = `
            INSERT INTO events (player_id, event_type, metadata, x, y, z, dimension, server_ip)
            VALUES ($1, 'BLOCK_PLACE', $2, $3, $4, $5, $6, $7)
        `;
        await db.query(query, [playerId, blockType, x, y, z, dimension, serverIp]);
        console.log(`[${serverIp}] - [BLOCK_PLACE: ${blockType}] - [${x} ${y} ${z}]`);
        return sendSuccess(res, null, 'Block place event recorded');
    } catch (error) {
        return sendError(res, 500, 'Error recording block placement', error);
    }
});

// POST /api/event/item_drop
router.post('/item_drop', auth, async (req, res) => {
    const { uuid, ign, itemId, count, x, y, z, dimension, serverIp } = req.body;

    if (!uuid || !ign || !itemId || count === undefined || x === undefined || y === undefined || z === undefined || !dimension) {
        return sendError(res, 400, 'Missing required data');
    }

    try {
        const playerId = await getPlayerId(uuid, ign, serverIp);
        const query = `
            INSERT INTO events (player_id, event_type, metadata, x, y, z, dimension, server_ip)
            VALUES ($1, 'ITEM_DROP', $2, $3, $4, $5, $6, $7)
        `;
        // include count in metadata
        const metadata = `${count}x ${itemId}`;
        await db.query(query, [playerId, metadata, x, y, z, dimension, serverIp]);
        console.log(`[${serverIp}] - [ITEM_DROP: ${metadata}] - [${x} ${y} ${z}]`);
        return sendSuccess(res, null, 'Item drop event recorded');
    } catch (error) {
        return sendError(res, 500, 'Error recording item drop', error);
    }
});

export default router;
