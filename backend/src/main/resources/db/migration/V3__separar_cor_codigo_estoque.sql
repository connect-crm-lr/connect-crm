-- ============================================================================
-- V3 - Separa estoque.cor_codigo ("05 Marinho") em codigo (opcional) e cor (obrigatoria)
-- ============================================================================
-- "05 Marinho"  -> codigo "05", cor "Marinho" (codigo = 1o termo, comeca com numero: "05", "10B")
-- "Marinho"     -> codigo NULL, cor "Marinho" (sem numero no inicio, ou so o numero)
-- A unicidade passa a ser por tecido + cor (o codigo e apenas informativo).

ALTER TABLE estoque
    ADD COLUMN codigo VARCHAR(20),
    ADD COLUMN cor    VARCHAR(50);

UPDATE estoque
SET codigo = substring(btrim(cor_codigo) FROM '^([0-9][[:alnum:]]{0,19})[[:space:]]+[^[:space:]]'),
    cor    = CASE
                 WHEN btrim(cor_codigo) ~ '^[0-9][[:alnum:]]{0,19}[[:space:]]+[^[:space:]]'
                     THEN btrim(regexp_replace(btrim(cor_codigo), '^[0-9][[:alnum:]]{0,19}[[:space:]]+', ''))
                 ELSE btrim(cor_codigo)
             END;

-- Mesma normalizacao do EstoqueService: cor so com a primeira letra maiuscula, codigo em maiusculas.
UPDATE estoque
SET cor    = upper(left(cor, 1)) || lower(substring(cor FROM 2)),
    codigo = upper(codigo);

ALTER TABLE estoque ALTER COLUMN cor SET NOT NULL;

ALTER TABLE estoque DROP CONSTRAINT uk_estoque_tecido_cor;
ALTER TABLE estoque DROP COLUMN cor_codigo;
ALTER TABLE estoque ADD CONSTRAINT uk_estoque_tecido_cor UNIQUE (id_tecido, cor);
