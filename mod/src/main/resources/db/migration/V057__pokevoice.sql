-- UUID-based communication consent; no audio is recorded.
CREATE TABLE pokevoice_relationship (
 low_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 high_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 known BOOLEAN NOT NULL DEFAULT FALSE,
 block_low BOOLEAN NOT NULL DEFAULT FALSE,
 block_high BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(low_uuid,high_uuid)
) ENGINE=InnoDB;
CREATE TABLE pokevoice_preferences (
 uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
 receive_requests BOOLEAN NOT NULL DEFAULT TRUE,
 allow_known BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
INSERT INTO schema_version(version,description) VALUES (57,'pokevoice');
