-- Identificador do Google Maps, usado para não importar a mesma empresa duas vezes.
ALTER TABLE empresas ADD COLUMN google_place_id VARCHAR(200);
CREATE UNIQUE INDEX uk_empresas_google_place_id ON empresas (google_place_id);
