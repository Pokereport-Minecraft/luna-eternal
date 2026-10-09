-- =====================================================================
-- Luna Eternal · Migración V059 — Licencias de Entrenador (Progresión 2.0)
-- =====================================================================

CREATE TABLE IF NOT EXISTS player_license (
    player_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    license VARCHAR(32) NOT NULL DEFAULT 'NOVATO',
    upgraded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_player_license_player FOREIGN KEY (player_id)
        REFERENCES player(player_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_version (version, description)
VALUES (59, 'licencias de entrenador progresion 2.0')
ON DUPLICATE KEY UPDATE version = version;
