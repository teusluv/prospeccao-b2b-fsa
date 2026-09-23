-- O identificador passa a servir para qualquer fonte (Google Maps ou OpenStreetMap).
ALTER TABLE empresas RENAME COLUMN google_place_id TO id_externo;
