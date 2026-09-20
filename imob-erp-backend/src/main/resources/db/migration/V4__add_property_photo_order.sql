-- IMOB-50: a posicao da foto passa a ser persistida (Property.photos usa @OrderColumn).
-- property_photos nao tem chave primaria; o backfill segue a ordem fisica atual (ctid), a melhor
-- aproximacao da ordem de insercao que ja era devolvida ate aqui.
ALTER TABLE property_photos ADD COLUMN photo_order INTEGER;

UPDATE property_photos p
SET photo_order = ranked.position
FROM (SELECT ctid AS row_id,
             row_number() OVER (PARTITION BY property_id ORDER BY ctid) - 1 AS position
      FROM property_photos) ranked
WHERE p.ctid = ranked.row_id;

ALTER TABLE property_photos ALTER COLUMN photo_order SET NOT NULL;

CREATE INDEX idx_property_photos_property_order ON property_photos (property_id, photo_order);
