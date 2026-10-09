-- =====================================================================
-- Luna Eternal · Migración V058 — Registro de eventos de eclosión de misiones
-- =====================================================================

CREATE TABLE IF NOT EXISTS quest_hatch_event (
  player_id BIGINT UNSIGNED NOT NULL,
  event_key VARCHAR(48) NOT NULL,
  amount BIGINT UNSIGNED NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (player_id, event_key),
  CONSTRAINT fk_hatch_player FOREIGN KEY (player_id) REFERENCES player (player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_version (version, description)
VALUES (58, 'confirmed hatch mission history')
ON DUPLICATE KEY UPDATE version = version;
