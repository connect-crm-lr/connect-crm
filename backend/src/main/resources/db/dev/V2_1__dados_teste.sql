-- V2.1 (SO DESENVOLVIMENTO): dados de teste para exercitar a API e as telas.
-- Roda apenas com o perfil "dev" (application-dev.yaml inclui classpath:db/dev).
-- Nunca vai para producao: sem o perfil, o Flyway nem enxerga esta pasta.
--
-- Os nomes seguem os mocks do front-end (clientes, catalogo, estoque, funil e dashboard),
-- para a tela mostrar o mesmo que o banco.
-- As datas sao relativas ao dia em que o banco e criado, para o dashboard sempre ter
-- "vendas do mes", "pendencias de hoje" e "estoque baixo" com conteudo.
--
-- Regras de coerencia (conferidas no bloco DO no fim do arquivo):
--   saldo do estoque = soma das entradas - soma dos itens vendidos (pedidos nao cancelados)
--   total do pedido  = soma de metragem x preco_metro dos itens

-- ============================================================================
-- 1. CLIENTES (telefone so com digitos e DDI 55)
-- ============================================================================

INSERT INTO cliente (id_usuario, nome, telefone, email, cpf_cnpj, cidade, uf)
SELECT u.id_usuario, c.nome, c.telefone, c.email, c.cpf_cnpj, c.cidade, c.uf
FROM usuario u
CROSS JOIN (VALUES
    ('Ateliê Bella Noiva',        '5511982345678', 'contato@bellanoiva.example.com', NULL,             'São Paulo', 'SP'),
    ('Casa Têxtil Moreno',        '551134567890',  'compras@moreno.example.com',     '11222333000181', 'São Paulo', 'SP'),
    ('Studio Decor SP',           '5511976543210', NULL,                             '52998224725',    'São Paulo', 'SP'),
    ('Malhas Oliveira & Filhos',  '5562991234567', 'oliveira@malhas.example.com',    NULL,             'Goiânia',   'GO'),
    ('Confecções Araújo ME',      '5562998765432', NULL,                             NULL,             'Anápolis',  'GO')
) AS c (nome, telefone, email, cpf_cnpj, cidade, uf)
WHERE u.email = 'revendedor@lrrepresentacoes.com.br';

-- Tags criadas na V2 (Moda Festa, Linho, Estofaria, Malha, Denim)
INSERT INTO cliente_tag (id_cliente, id_tag)
SELECT c.id_cliente, t.id_tag
FROM (VALUES
    ('Ateliê Bella Noiva',       'Moda Festa'),
    ('Casa Têxtil Moreno',       'Linho'),
    ('Casa Têxtil Moreno',       'Estofaria'),
    ('Studio Decor SP',          'Estofaria'),
    ('Malhas Oliveira & Filhos', 'Malha'),
    ('Confecções Araújo ME',     'Denim'),
    ('Confecções Araújo ME',     'Malha')
) AS v (cliente, tag)
JOIN cliente c ON c.nome = v.cliente
JOIN tag t ON LOWER(t.nome) = LOWER(v.tag);

-- ============================================================================
-- 2. CATALOGO (6 tecidos)
-- ============================================================================

INSERT INTO tecido (nome, composicao, gramatura, largura, preco_metro)
VALUES
    ('Crepe Georgette',  '100% Poliéster',                 80, 1.50, 28.90),
    ('Linho Natural',    '100% Linho',                     185, 1.40, 42.50),
    ('Suede Automotivo', '80% Poliéster / 20% Poliamida',  270, 1.40, 55.00),
    ('Malha Canelada',   '96% Algodão / 4% Elastano',      220, 1.80, 24.90),
    ('Tule de Festa',    '100% Poliamida',                 30, 1.60, 18.50),
    ('Jeans Stretch',    '98% Algodão / 2% Elastano',      340, 1.60, 36.90);

-- ============================================================================
-- 3. ESTOQUE (saldo final ja descontado das vendas) E ENTRADAS
-- ============================================================================
--   linha                          entradas   vendido (nao cancelado)   saldo
--   Crepe Georgette / Off-white    150 + 125  30                        245
--   Crepe Georgette / Rose         200        20                        180
--   Linho Natural / Natural        40         25                        15   (baixo)
--   Suede Automotivo / Cinza Chumbo 200 + 160 40                        320
--   Suede Automotivo / Bege        150        10                        140
--   Malha Canelada / Cru           250        40                        210
--   Tule de Festa / Marfim         28.5       20                        8.5  (baixo)
--   Jeans Stretch / Azul Indigo    95         0 (o pedido foi cancelado) 95

INSERT INTO estoque (id_tecido, cor_codigo, metragem)
SELECT t.id_tecido, v.cor, v.saldo
FROM (VALUES
    ('Crepe Georgette',  'Off-white',    245.00),
    ('Crepe Georgette',  'Rose',         180.00),
    ('Linho Natural',    'Natural',       15.00),
    ('Suede Automotivo', 'Cinza Chumbo', 320.00),
    ('Suede Automotivo', 'Bege',         140.00),
    ('Malha Canelada',   'Cru',          210.00),
    ('Tule de Festa',    'Marfim',         8.50),
    ('Jeans Stretch',    'Azul Índigo',   95.00)
) AS v (tecido, cor, saldo)
JOIN tecido t ON t.nome = v.tecido;

INSERT INTO entrada_estoque (id_estoque, metragem, data_entrada)
SELECT e.id_estoque, v.metragem, CURRENT_DATE - v.dias_atras
FROM (VALUES
    ('Crepe Georgette',  'Off-white',    150.00, 90),
    ('Crepe Georgette',  'Off-white',    125.00, 40),
    ('Crepe Georgette',  'Rose',         200.00, 45),
    ('Linho Natural',    'Natural',       40.00, 75),
    ('Suede Automotivo', 'Cinza Chumbo', 200.00, 80),
    ('Suede Automotivo', 'Cinza Chumbo', 160.00, 15),
    ('Suede Automotivo', 'Bege',         150.00, 30),
    ('Malha Canelada',   'Cru',          250.00, 25),
    ('Tule de Festa',    'Marfim',        28.50, 50),
    ('Jeans Stretch',    'Azul Índigo',   95.00, 35)
) AS v (tecido, cor, metragem, dias_atras)
JOIN tecido t ON t.nome = v.tecido
JOIN estoque e ON e.id_tecido = t.id_tecido AND e.cor_codigo = v.cor;

-- ============================================================================
-- 4. NEGOCIACOES (funil: 2 primeiro contato, 2 mostruario, 1 aguardando, 1 fechada, 1 perdida)
-- ============================================================================

INSERT INTO negociacao (id_cliente, id_usuario, descricao, estagio,
                        data_criacao, data_ultima_movimentacao, data_fechamento)
SELECT c.id_cliente, c.id_usuario, v.descricao, v.estagio,
       now() - make_interval(days => v.criada_ha),
       now() - make_interval(days => v.movida_ha),
       CASE WHEN v.estagio IN ('FECHADA', 'PERDIDA')
            THEN now() - make_interval(days => v.movida_ha) END
FROM (VALUES
    ('Casa Têxtil Moreno',       'Proposta de Linho Natural - 200m',        'PRIMEIRO_CONTATO',     6,  6),
    ('Malhas Oliveira & Filhos', 'Reposição de Malha Canelada',             'PRIMEIRO_CONTATO',     4,  4),
    ('Ateliê Bella Noiva',       'Tule de Festa e Crepe',                   'MOSTRUARIO_ENVIADO',  12,  8),
    ('Studio Decor SP',          'Suede para linha de poltronas',           'MOSTRUARIO_ENVIADO',  10,  7),
    ('Confecções Araújo ME',     'Jeans Stretch',                           'AGUARDANDO_RESPOSTA', 15,  2),
    ('Ateliê Bella Noiva',       'Crepe Georgette para coleção de noivas',  'FECHADA',             50, 36),
    ('Studio Decor SP',          'Linho para cortinas',                     'PERDIDA',             40, 20)
) AS v (cliente, descricao, estagio, criada_ha, movida_ha)
JOIN cliente c ON c.nome = v.cliente;

-- ============================================================================
-- 5. PEDIDOS E ITENS
-- ============================================================================
--   cliente             origem              status     itens                                   total
--   Ateliê Bella Noiva  negociacao FECHADA  PAGO       30 m Crepe Off-white x 28,90 + 20 m Tule x 18,50  1.237,00
--   Ateliê Bella Noiva  venda direta        EM_ABERTO  20 m Crepe Rose x 28,90                 578,00
--   Studio Decor SP     venda direta        EM_ABERTO  40 m Suede Cinza x 55 + 10 m Suede Bege x 55     2.750,00
--   Casa Têxtil Moreno  venda direta        PAGO       25 m Linho x 39,90 (preco da epoca)     997,50
--   Malhas Oliveira     venda direta        PAGO       40 m Malha Cru x 24,90 (hoje)           996,00
--   Confecções Araújo   venda direta        CANCELADO  30 m Jeans x 36,90 (estoque devolvido)  1.107,00

INSERT INTO pedido (id_cliente, id_usuario, id_negociacao, data_pedido, forma_pagamento, status, total)
SELECT c.id_cliente, c.id_usuario, n.id_negociacao,
       now() - v.ha, v.forma_pagamento, v.status, v.total
FROM (VALUES
    ('PED-1', 'Ateliê Bella Noiva',       'Crepe Georgette para coleção de noivas', interval '35 days', 'CARTAO',  'PAGO',      1237.00),
    ('PED-2', 'Ateliê Bella Noiva',       NULL,                                     interval '3 days',  'BOLETO',  'EM_ABERTO',  578.00),
    ('PED-3', 'Studio Decor SP',          NULL,                                     interval '5 days',  'A_VISTA', 'EM_ABERTO', 2750.00),
    ('PED-4', 'Casa Têxtil Moreno',       NULL,                                     interval '60 days', 'A_VISTA', 'PAGO',       997.50),
    ('PED-5', 'Malhas Oliveira & Filhos', NULL,                                     interval '2 hours', 'CHEQUE',  'PAGO',       996.00),
    ('PED-6', 'Confecções Araújo ME',     NULL,                                     interval '20 days', 'BOLETO',  'CANCELADO', 1107.00)
) AS v (ref, cliente, negociacao, ha, forma_pagamento, status, total)
JOIN cliente c ON c.nome = v.cliente
LEFT JOIN negociacao n ON n.descricao = v.negociacao AND n.id_cliente = c.id_cliente
ORDER BY v.ha DESC;

-- Cada pedido e localizado pelo cliente + total (combinacao unica nestes dados de teste).
INSERT INTO item_pedido (id_pedido, id_estoque, metragem, preco_metro)
SELECT p.id_pedido, e.id_estoque, v.metragem, v.preco_metro
FROM (VALUES
    ('Ateliê Bella Noiva',       1237.00, 'Crepe Georgette',  'Off-white',    30.00, 28.90),
    ('Ateliê Bella Noiva',       1237.00, 'Tule de Festa',    'Marfim',       20.00, 18.50),
    ('Ateliê Bella Noiva',        578.00, 'Crepe Georgette',  'Rose',         20.00, 28.90),
    ('Studio Decor SP',          2750.00, 'Suede Automotivo', 'Cinza Chumbo', 40.00, 55.00),
    ('Studio Decor SP',          2750.00, 'Suede Automotivo', 'Bege',         10.00, 55.00),
    ('Casa Têxtil Moreno',        997.50, 'Linho Natural',    'Natural',      25.00, 39.90),
    ('Malhas Oliveira & Filhos',  996.00, 'Malha Canelada',   'Cru',          40.00, 24.90),
    ('Confecções Araújo ME',     1107.00, 'Jeans Stretch',    'Azul Índigo',  30.00, 36.90)
) AS v (cliente, total, tecido, cor, metragem, preco_metro)
JOIN cliente c ON c.nome = v.cliente
JOIN pedido p ON p.id_cliente = c.id_cliente AND p.total = v.total
JOIN tecido t ON t.nome = v.tecido
JOIN estoque e ON e.id_tecido = t.id_tecido AND e.cor_codigo = v.cor;

-- ============================================================================
-- 6. INTERACOES (conversas ja realizadas)
-- ============================================================================

INSERT INTO interacao (id_cliente, id_usuario, tipo, data_interacao, descricao)
SELECT c.id_cliente, c.id_usuario, v.tipo, CURRENT_DATE - v.dias_atras, v.descricao
FROM (VALUES
    ('Ateliê Bella Noiva',       'WHATSAPP', 8,  'Enviou fotos do Tule de Festa marfim e do Crepe rose; cliente gostou do caimento.'),
    ('Ateliê Bella Noiva',       'VISITA',   36, 'Visita no ateliê para fechar o Crepe Georgette da coleção de noivas.'),
    ('Casa Têxtil Moreno',       'LIGACAO',  6,  'Pediu proposta de 200 m de Linho Natural para estofaria.'),
    ('Studio Decor SP',          'WHATSAPP', 7,  'Mostruário de Suede Automotivo enviado por foto.'),
    ('Studio Decor SP',          'LIGACAO',  20, 'Desistiu do linho para cortinas: comprou de outro fornecedor.'),
    ('Malhas Oliveira & Filhos', 'WHATSAPP', 4,  'Perguntou sobre reposição de Malha Canelada cru.'),
    ('Confecções Araújo ME',     'OUTRO',    2,  'Conversa na feira de confecções sobre Jeans Stretch para a nova coleção.')
) AS v (cliente, tipo, dias_atras, descricao)
JOIN cliente c ON c.nome = v.cliente;

-- ============================================================================
-- 7. FOLLOW-UPS (data_conclusao NULL = pendente)
-- ============================================================================

INSERT INTO follow_up (id_cliente, id_usuario, data_prevista, descricao, data_conclusao)
SELECT c.id_cliente, c.id_usuario, CURRENT_DATE + v.dias, v.descricao,
       CASE WHEN v.concluido_ha IS NOT NULL THEN now() - make_interval(days => v.concluido_ha) END
FROM (VALUES
    ('Ateliê Bella Noiva',       0,  'Retornar sobre disponibilidade de Tule na cor marfim.',     NULL::int),
    ('Casa Têxtil Moreno',       -1, 'Enviar tabela de preços atualizada.',                       NULL),
    ('Confecções Araújo ME',     3,  'Cobrar resposta sobre o Jeans Stretch.',                    NULL),
    ('Studio Decor SP',          -6, 'Confirmar recebimento do mostruário de Suede.',             4)
) AS v (cliente, dias, descricao, concluido_ha)
JOIN cliente c ON c.nome = v.cliente;

-- ============================================================================
-- 8. AUTOVERIFICACAO: se alguem editar os dados acima e quebrar a coerencia,
--    a migration falha em vez de gravar dados errados.
-- ============================================================================

DO $$
DECLARE
    estoque_divergente integer;
    pedido_divergente  integer;
BEGIN
    -- Auditoria do saldo (docs/connect-crm-revisao-modelagem-banco.md): deve retornar zero linhas.
    SELECT COUNT(*) INTO estoque_divergente
    FROM estoque e
    LEFT JOIN (SELECT id_estoque, SUM(metragem) AS total
               FROM entrada_estoque GROUP BY id_estoque) ent
           ON ent.id_estoque = e.id_estoque
    LEFT JOIN (SELECT ip.id_estoque, SUM(ip.metragem) AS total
               FROM item_pedido ip
               JOIN pedido p ON p.id_pedido = ip.id_pedido
               WHERE p.status <> 'CANCELADO'
               GROUP BY ip.id_estoque) sai
           ON sai.id_estoque = e.id_estoque
    WHERE e.metragem <> COALESCE(ent.total, 0) - COALESCE(sai.total, 0);

    IF estoque_divergente > 0 THEN
        RAISE EXCEPTION 'Dados de teste incoerentes: % linha(s) de estoque com saldo diferente de entradas - vendas',
            estoque_divergente;
    END IF;

    SELECT COUNT(*) INTO pedido_divergente
    FROM pedido p
    WHERE p.total <> (SELECT COALESCE(ROUND(SUM(ip.metragem * ip.preco_metro), 2), 0)
                      FROM item_pedido ip
                      WHERE ip.id_pedido = p.id_pedido);

    IF pedido_divergente > 0 THEN
        RAISE EXCEPTION 'Dados de teste incoerentes: % pedido(s) com total diferente da soma dos itens',
            pedido_divergente;
    END IF;
END $$;
