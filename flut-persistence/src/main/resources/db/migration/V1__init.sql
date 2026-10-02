-- Scenarios, fully normalized: one row per scenario, per schuur (pile) and per box.
-- Results are not stored; they are computed on read.

CREATE SEQUENCE scenario_seq INCREMENT BY 50;
CREATE SEQUENCE schuur_seq INCREMENT BY 50;
CREATE SEQUENCE box_seq INCREMENT BY 50;

CREATE TABLE scenario (
    id         BIGINT      PRIMARY KEY DEFAULT nextval('scenario_seq'),
    name       TEXT        NOT NULL CHECK (btrim(name) <> ''),
    input_type VARCHAR(10) NOT NULL CHECK (input_type IN ('JSON', 'FILE')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_scenario_input_type ON scenario (input_type);

CREATE TABLE schuur (
    id          BIGINT  PRIMARY KEY DEFAULT nextval('schuur_seq'),
    scenario_id BIGINT  NOT NULL REFERENCES scenario (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL CHECK (position >= 0),
    -- Deferred: replacing a scenario's schuurs inserts new rows before the old ones are deleted.
    CONSTRAINT uq_schuur_position UNIQUE (scenario_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE box (
    id        BIGINT  PRIMARY KEY DEFAULT nextval('box_seq'),
    schuur_id BIGINT  NOT NULL REFERENCES schuur (id) ON DELETE CASCADE,
    position  INTEGER NOT NULL CHECK (position >= 0),
    price     INTEGER NOT NULL CHECK (price > 0),
    CONSTRAINT uq_box_position UNIQUE (schuur_id, position) DEFERRABLE INITIALLY DEFERRED
);
