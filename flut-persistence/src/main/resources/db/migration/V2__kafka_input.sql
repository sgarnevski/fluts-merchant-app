-- Scenarios can arrive as Kafka messages. Each message carries an id; storing it (unique) lets the
-- consumer skip a message that is delivered twice.

ALTER TABLE scenario DROP CONSTRAINT scenario_input_type_check;
ALTER TABLE scenario ADD CONSTRAINT scenario_input_type_check CHECK (input_type IN ('JSON', 'FILE', 'KAFKA'));

ALTER TABLE scenario ADD COLUMN message_id UUID;
ALTER TABLE scenario ADD CONSTRAINT uq_scenario_message_id UNIQUE (message_id);
