CREATE TABLE solicitations (
    id               UUID PRIMARY KEY,
    client_id        UUID        NOT NULL REFERENCES users (id),
    status           VARCHAR(20) NOT NULL,
    current_step     INTEGER     NOT NULL,

    -- Step 1: basic info
    service_type     VARCHAR(20),
    title            VARCHAR(80),
    description      VARCHAR(1000),

    -- Step 2: address (street, neighborhood, city and state come from ViaCEP)
    cep              VARCHAR(8),
    street           VARCHAR(200),
    neighborhood     VARCHAR(120),
    city             VARCHAR(120),
    state            VARCHAR(2),
    number           VARCHAR(20),
    complement       VARCHAR(100),

    -- Step 3: confirmation
    priority         VARCHAR(10),
    preferred_date   DATE,
    estimated_value  NUMERIC(12, 2),
    terms_accepted   BOOLEAN,

    -- Review
    analyst_id       UUID REFERENCES users (id),
    decision_comment VARCHAR(1000),

    created_at       TIMESTAMPTZ NOT NULL,
    submitted_at     TIMESTAMPTZ,
    decided_at       TIMESTAMPTZ,
    version          BIGINT      NOT NULL
);
