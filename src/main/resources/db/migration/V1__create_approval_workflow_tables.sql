CREATE TABLE approval_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200)  NOT NULL,
    description     VARCHAR(2000),
    department      VARCHAR(100)  NOT NULL,
    requester_name  VARCHAR(150)  NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    current_level   INTEGER,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE approval_steps (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_request_id UUID NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
    level_number        INTEGER      NOT NULL,
    approver_name       VARCHAR(150) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    comments            VARCHAR(1000),
    acted_at            TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_request_level UNIQUE (approval_request_id, level_number)
);

CREATE INDEX idx_approval_requests_status     ON approval_requests(status);
CREATE INDEX idx_approval_requests_department ON approval_requests(department);
CREATE INDEX idx_approval_steps_request_id    ON approval_steps(approval_request_id);
