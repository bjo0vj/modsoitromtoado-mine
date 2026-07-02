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
    created_at TIMESTAMP DEFAULT NOW()
);

-- Events table — deaths + block placements
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

-- Heartbeats — vị trí cập nhật mỗi 5 phút
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

-- Waypoints — điểm đánh dấu của player
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

-- Indexes cho performance
CREATE INDEX IF NOT EXISTS idx_events_player ON events(player_id);
CREATE INDEX IF NOT EXISTS idx_events_type ON events(event_type);
CREATE INDEX IF NOT EXISTS idx_heartbeats_player ON heartbeats(player_id);
CREATE INDEX IF NOT EXISTS idx_waypoints_player ON waypoints(player_id);
CREATE INDEX IF NOT EXISTS idx_players_ign ON players(ign);
