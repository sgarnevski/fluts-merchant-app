-- Scenarios belong to a merchant (a client of the system). Merchants are registered here; users,
-- SFTP accounts and signing secrets live with the parties that use them, not in the database.

CREATE TABLE merchant (
    code VARCHAR(30) PRIMARY KEY CHECK (code ~ '^[a-z][a-z0-9-]*$'),
    name TEXT        NOT NULL CHECK (btrim(name) <> '')
);

INSERT INTO merchant (code, name) VALUES ('dutch', 'Dutch merchant'), ('french', 'French merchant');

ALTER TABLE scenario ADD COLUMN merchant_code VARCHAR(30);
UPDATE scenario SET merchant_code = 'dutch';
ALTER TABLE scenario ALTER COLUMN merchant_code SET NOT NULL;
ALTER TABLE scenario ADD CONSTRAINT fk_scenario_merchant FOREIGN KEY (merchant_code) REFERENCES merchant (code);
CREATE INDEX ix_scenario_merchant ON scenario (merchant_code);
