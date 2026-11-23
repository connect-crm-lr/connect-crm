-- ============================================================================
-- V3 - Separa estoque.cor_codigo ("05 Marinho") em codigo (opcional) e cor (obrigatoria)
-- ============================================================================
-- "05 Marinho"  -> codigo "05", cor "Marinho"
-- "Marinho"     -> codigo NULL, cor "Marinho" (sem numero no inicio, ou so o numero)
-- A unicidade passa a ser por tecido + cor (o codigo e apenas informativo).

ALTER TABLE estoque
    ADD COLUMN codigo VARCHAR(20),
    ADD COLUMN cor    VARCHAR(50);

UPDATE estoque
SET codigo = substring(cor_codigo FROM '^(\d{1,20})\s+\S'),
    cor    = CASE
                 WHEN cor_codigo ~ '^\d{1,20}\s+\S' THEN btrim(regexp_replace(cor_codigo, '^\d{1,20}\s+', ''))
                 ELSE btrim(cor_codigo)
             END;

ALTER TABLE estoque ALTER COLUMN cor SET NOT NULL;

ALTER TABLE estoque DROP CONSTRAINT uk_estoque_tecido_cor;
ALTER TABLE estoque DROP COLUMN cor_codigo;
ALTER TABLE estoque ADD CONSTRAINT uk_estoque_tecido_cor UNIQUE (id_tecido, cor);
