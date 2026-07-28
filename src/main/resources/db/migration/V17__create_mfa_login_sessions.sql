-- V17__create_mfa_login_sessions.sql
CREATE TABLE IF NOT EXISTS mfa_login_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITHOUT TIME ZONE,
    resend_count INT NOT NULL DEFAULT 0,
    last_resend_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_mfa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mfa_user_id ON mfa_login_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_mfa_expires_at ON mfa_login_sessions(expires_at);
