const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: {
        rejectUnauthorized: false
    },
    max: 5,                    // Neon free tier limit
    idleTimeoutMillis: 30000,  // Close idle connections after 30s
    connectionTimeoutMillis: 10000
});

// Test connection
pool.connect((err, client, release) => {
    if (err) {
        console.error('❌ Database connection failed:', err.message);
    } else {
        console.log('✅ Connected to PostgreSQL (Neon.tech)');
        release();
    }
});

// Auto-create tables on startup
async function initializeSchema() {
    const schema = `
        CREATE TABLE IF NOT EXISTS players (
            id SERIAL PRIMARY KEY,
            player_uuid VARCHAR(36) UNIQUE NOT NULL,
            ign VARCHAR(16) NOT NULL,
            server_ip VARCHAR(255),
            mod_version VARCHAR(20),
            last_x DOUBLE PRECISION,
            last_y DOUBLE PRECISION,
            last_z DOUBLE PRECISION,
            last_dimension VARCHAR(100),
            last_online TIMESTAMP,
            created_at TIMESTAMP DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS events (
            id SERIAL PRIMARY KEY,
            player_id INT NOT NULL REFERENCES players(id),
            event_type VARCHAR(20) NOT NULL,
            x DOUBLE PRECISION NOT NULL,
            y DOUBLE PRECISION NOT NULL,
            z DOUBLE PRECISION NOT NULL,
            dimension VARCHAR(100) NOT NULL,
            server_ip VARCHAR(255),
            created_at TIMESTAMP DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS heartbeats (
            id SERIAL PRIMARY KEY,
            player_id INT NOT NULL REFERENCES players(id),
            x DOUBLE PRECISION,
            y DOUBLE PRECISION,
            z DOUBLE PRECISION,
            dimension VARCHAR(100),
            server_ip VARCHAR(255),
            created_at TIMESTAMP DEFAULT NOW()
        );

        CREATE TABLE IF NOT EXISTS waypoints (
            id SERIAL PRIMARY KEY,
            player_id INT NOT NULL REFERENCES players(id),
            name VARCHAR(50) NOT NULL,
            x DOUBLE PRECISION NOT NULL,
            y DOUBLE PRECISION NOT NULL,
            z DOUBLE PRECISION NOT NULL,
            dimension VARCHAR(100) NOT NULL,
            server_ip VARCHAR(255),
            created_at TIMESTAMP DEFAULT NOW()
        );

        CREATE INDEX IF NOT EXISTS idx_events_player ON events(player_id);
        CREATE INDEX IF NOT EXISTS idx_events_type ON events(event_type);
        CREATE INDEX IF NOT EXISTS idx_heartbeats_player ON heartbeats(player_id);
        CREATE INDEX IF NOT EXISTS idx_waypoints_player ON waypoints(player_id);
        CREATE INDEX IF NOT EXISTS idx_players_ign ON players(ign);
    `;

    try {
        await pool.query(schema);
        console.log('✅ Database schema initialized');
    } catch (err) {
        console.error('❌ Schema initialization failed:', err.message);
    }
}

// Run schema on import
initializeSchema();

module.exports = {
    query: (text, params) => pool.query(text, params),
};
