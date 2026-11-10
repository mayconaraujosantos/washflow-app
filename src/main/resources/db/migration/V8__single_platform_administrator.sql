ALTER TABLE administradores
  ADD COLUMN singleton_key SMALLINT NOT NULL DEFAULT 1
  CHECK (singleton_key = 1);

ALTER TABLE administradores
  ADD CONSTRAINT uq_administradores_singleton UNIQUE (singleton_key);
