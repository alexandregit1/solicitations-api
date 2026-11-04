CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(60) NOT NULL,
    actor_id    UUID,
    resource_id UUID,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_id);
