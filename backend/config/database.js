import pkg from 'pg';
const { Pool } = pkg;
import 'dotenv/config.js';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

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
        console.log('✅ Connected to PostgreSQL');
        release();
    }
});

// Auto-create tables on startup
async function initializeSchema() {
    try {
        const schemaPath = path.join(__dirname, '../database/schema.sql');
        const schema = fs.readFileSync(schemaPath, 'utf8');
        await pool.query(schema);
        console.log('✅ Database schema initialized from schema.sql');
    } catch (err) {
        console.error('❌ Schema initialization failed:', err.message);
    }
}

// Run schema on import
initializeSchema();

export const query = (text, params) => pool.query(text, params);
