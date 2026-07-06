-- Players table — mỗi mod instance có UUID riêng
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
    is_live_tracking BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Events table — block placements + item drops
CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    player_id INT NOT NULL REFERENCES players(id),
    event_type VARCHAR(20) NOT NULL, -- 'BLOCK_PLACE' or 'ITEM_DROP'
    metadata VARCHAR(255), -- Lưu tên block (ví dụ: minecraft:chest) hoặc tên item
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    dimension VARCHAR(100) NOT NULL,
    server_ip VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Proximity Logs — chức năng quét người chơi ở gần
CREATE TABLE IF NOT EXISTS proximity_logs (
    id SERIAL PRIMARY KEY,
    player_id INT NOT NULL REFERENCES players(id),
    event_type VARCHAR(50) NOT NULL, -- 'NEARBY_CHANGED' or 'NEARBY_STABLE_10M'
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    dimension VARCHAR(100) NOT NULL,
    nearby_players TEXT, -- Lưu danh sách người chơi (JSON array dạng chuỗi)
    closest_player VARCHAR(50),
    server_ip VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Heartbeats — vị trí cập nhật mỗi 5 phút hoặc Live Tracking
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

-- Indexes cho performance
CREATE INDEX IF NOT EXISTS idx_events_player ON events(player_id);
CREATE INDEX IF NOT EXISTS idx_events_type ON events(event_type);
CREATE INDEX IF NOT EXISTS idx_heartbeats_player ON heartbeats(player_id);
CREATE INDEX IF NOT EXISTS idx_proximity_player ON proximity_logs(player_id);
CREATE INDEX IF NOT EXISTS idx_players_ign ON players(ign);
