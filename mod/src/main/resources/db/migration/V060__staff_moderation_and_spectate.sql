-- V060__staff_moderation_and_spectate.sql
-- Sistema de moderación y auditoría de espectador para Staff

CREATE TABLE IF NOT EXISTS staff_punishment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_uuid VARCHAR(36) NOT NULL,
    target_name VARCHAR(32) NOT NULL,
    staff_uuid VARCHAR(36) NOT NULL,
    staff_name VARCHAR(32) NOT NULL,
    type VARCHAR(16) NOT NULL,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_punish_target_uuid (target_uuid),
    INDEX idx_punish_active (active),
    INDEX idx_punish_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS staff_spectate_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_uuid VARCHAR(36) NOT NULL,
    staff_name VARCHAR(32) NOT NULL,
    target_uuid VARCHAR(36) NOT NULL,
    target_name VARCHAR(32) NOT NULL,
    started_at BIGINT NOT NULL,
    ended_at BIGINT NULL,
    reason VARCHAR(255) NULL,
    INDEX idx_spec_staff_uuid (staff_uuid),
    INDEX idx_spec_target_uuid (target_uuid),
    INDEX idx_spec_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
