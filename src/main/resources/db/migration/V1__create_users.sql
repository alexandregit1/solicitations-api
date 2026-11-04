CREATE TABLE users (
    id            UUID PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

-- UFs an ANALYST covers
CREATE TABLE user_states (
    user_id UUID       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    state   VARCHAR(2) NOT NULL,
    PRIMARY KEY (user_id, state)
);
