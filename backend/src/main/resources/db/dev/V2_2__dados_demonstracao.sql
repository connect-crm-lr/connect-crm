-- V2.2 (SO DESENVOLVIMENTO): dados de demonstracao para a apresentacao (issue #72).
-- Roda apenas com o perfil "dev" (application-dev.yaml inclui classpath:db/dev), depois da V2.1.
-- Nunca vai para producao: sem o perfil, o Flyway nem enxerga esta pasta.
-- A V2.1 nao e editada; esta migration so ACRESCENTA dados.
--
-- Somado a V2.1, o banco fica com: 8 clientes (1 desativado), 6 tecidos com 13 linhas de
-- estoque (4 abaixo de 20 m), negociacoes nas 5 etapas, pedidos pagos, em aberto e
-- cancelados no mes atual e no anterior, interacoes e follow-ups (para hoje e atrasados).
--
-- Datas: tudo relativo ao momento em que o banco e criado, para os dados ficarem "atuais"
-- em qualquer dia. Os pedidos do "mes atual" usam uma fracao do tempo ja passado no mes:
--     inicio_do_mes + (agora - inicio_do_mes) * fracao
-- assim caem SEMPRE entre o dia 1 as 00:00 e agora, mesmo no dia 1 ou perto da meia-noite
-- (evita o problema do "now() - interval '2 hours'" da V2.1). Os do "mes anterior" ficam
-- de 8 a 18 dias antes do dia 1, sempre dentro do mes anterior (todo mes tem 28+ dias).
-- LOCALTIMESTAMP e CURRENT_DATE seguem o fuso do banco (America/Sao_Paulo, AGENTS 4).
--
-- Regras de coerencia (conferidas no bloco DO no fim do arquivo, para o banco inteiro):
--   saldo do estoque = soma das entradas - soma dos itens vendidos (pedidos nao cancelados)
--   total do pedido  = soma de metragem x preco_metro dos itens
--   cliente desativado nao tem pedido EM_ABERTO nem negociacao em andamento (regra 6.5)
--   pedido de negociacao tem o mesmo cliente da negociacao (regra 6.1)

-- ============================================================================
-- 1. CLIENTES NOVOS (3; um deles desativado)
-- ============================================================================

INSERT INTO cliente (id_usuario, nome, telefone, email, cpf_cnpj, cidade, uf, ativo)
SELECT u.id_usuario, c.nome, c.telefone, c.email, c.cpf_cnpj, c.cidade, c.uf, c.ativo
FROM usuario u
CROSS JOIN (VALUES
    ('Boutique Tecidos da Serra', '5562993456789', 'compras@tecidosdaserra.example.com', NULL,             'Goiânia',   'GO', true),
    ('Ateliê Costura Fina',       '5511987651234', 'atelie@costurafina.example.com',     NULL,             'São Paulo', 'SP', true),
    -- Desativado: mostra a opcao "exibir inativos" e que ele some das pendencias de hoje.
    ('Retalhos Centro Ltda',      '5562984561230', NULL,                                 NULL,             'Anápolis',  'GO', false)
) AS c (nome, telefone, email, cpf_cnpj, cidade, uf, ativo)
WHERE u.email = 'revendedor@lrrepresentacoes.com.br';

-- Tags criadas na V2 (Moda Festa, Linho, Estofaria, Malha, Denim)
INSERT INTO cliente_tag (id_cliente, id_tag)
SELECT c.id_cliente, t.id_tag
FROM (VALUES
    ('Boutique Tecidos da Serra', 'Linho'),
    ('Boutique Tecidos da Serra', 'Malha'),
    ('Ateliê Costura Fina',       'Moda Festa'),
    ('Retalhos Centro Ltda',      'Denim')
) AS v (cliente, tag)
JOIN cliente c ON c.nome = v.cliente
JOIN tag t ON LOWER(t.nome) = LOWER(v.tag);

-- ============================================================================
-- 2. ESTOQUE: novas cores dos tecidos da V2.1 (saldo final ja descontado das vendas)
-- ============================================================================
--   linha                       entrada   vendido (nao cancelado)        saldo
--   Linho Natural / Cru         60        22                             38
--   Malha Canelada / Preto      80        32 + 30                        18   (baixo)
--   Jeans Stretch / Preto       120       45                             75
--   Tule de Festa / Branco      50        35                             15   (baixo)
--   Crepe Georgette / Preto     100       0 (o pedido foi cancelado)     100
-- As entradas sao de 45 dias atras ou mais, antes de qualquer venda destas linhas.

INSERT INTO estoque (id_tecido, cor_codigo, metragem)
SELECT t.id_tecido, v.cor, v.saldo
FROM (VALUES
    ('Linho Natural',   'Cru',     38.00),
    ('Malha Canelada',  'Preto',   18.00),
    ('Jeans Stretch',   'Preto',   75.00),
    ('Tule de Festa',   'Branco',  15.00),
    ('Crepe Georgette', 'Preto',  100.00)
) AS v (tecido, cor, saldo)
JOIN tecido t ON t.nome = v.tecido;

INSERT INTO entrada_estoque (id_estoque, metragem, data_entrada)
SELECT e.id_estoque, v.metragem, CURRENT_DATE - v.dias_atras
FROM (VALUES
    ('Linho Natural',   'Cru',     60.00, 55),
    ('Malha Canelada',  'Preto',   80.00, 70),
    ('Jeans Stretch',   'Preto',  120.00, 75),
    ('Tule de Festa',   'Branco',  50.00, 45),
    ('Crepe Georgette', 'Preto',  100.00, 60)
) AS v (tecido, cor, metragem, dias_atras)
JOIN tecido t ON t.nome = v.tecido
JOIN estoque e ON e.id_tecido = t.id_tecido AND e.cor_codigo = v.cor;

-- ============================================================================
-- 3. NEGOCIACOES NOVAS (uma por etapa)
-- ============================================================================
-- Com a V2.1, o funil fica: 3 primeiro contato, 3 mostruario, 2 aguardando, 2 fechadas, 2 perdidas.
-- A FECHADA desta migration origina um pedido do mes atual: ela fecha no comeco do tempo
-- passado no mes (fracao 0,2) e o pedido vem logo depois (fracao 0,3).

INSERT INTO negociacao (id_cliente, id_usuario, descricao, estagio,
                        data_criacao, data_ultima_movimentacao, data_fechamento)
SELECT c.id_cliente, c.id_usuario, v.descricao, v.estagio, v.criada, v.movida,
       CASE WHEN v.estagio IN ('FECHADA', 'PERDIDA') THEN v.movida END
FROM (
    SELECT date_trunc('month', LOCALTIMESTAMP) AS inicio_mes, LOCALTIMESTAMP AS agora
) m
CROSS JOIN LATERAL (VALUES
    ('Ateliê Costura Fina',       'Crepe preto para coleção de inverno',      'PRIMEIRO_CONTATO',
        m.agora - interval '1 day',   m.agora - interval '1 day'),
    ('Ateliê Costura Fina',       'Tule e Crepe para vestidos de debutante',  'MOSTRUARIO_ENVIADO',
        m.agora - interval '9 days',  m.agora - interval '4 days'),
    ('Boutique Tecidos da Serra', 'Reposição de Malha Canelada preta',        'AGUARDANDO_RESPOSTA',
        m.agora - interval '6 days',  m.agora - interval '3 days'),
    ('Boutique Tecidos da Serra', 'Linho Cru para coleção de verão',          'FECHADA',
        m.inicio_mes - interval '10 days', m.inicio_mes + (m.agora - m.inicio_mes) * 0.2),
    ('Retalhos Centro Ltda',      'Jeans preto para reposição',               'PERDIDA',
        m.agora - interval '40 days', m.agora - interval '25 days')
) AS v (cliente, descricao, estagio, criada, movida)
JOIN cliente c ON c.nome = v.cliente;

-- ============================================================================
-- 4. PEDIDOS E ITENS (mes atual e mes anterior)
-- ============================================================================
--   cliente                    quando         status     itens                                   total
--   Boutique Tecidos da Serra  mes atual      PAGO       22 m Linho Cru x 42,50 (da negociacao)  935,00
--   Ateliê Costura Fina        mes atual      PAGO       35 m Tule Branco x 18,50                647,50
--   Boutique Tecidos da Serra  mes atual      EM_ABERTO  32 m Malha Preto x 24,90                796,80
--   Ateliê Costura Fina        mes atual      CANCELADO  10 m Linho Cru x 42,50                  425,00
--   Boutique Tecidos da Serra  mes anterior   PAGO       30 m Malha Preto x 22,90 (preco da epoca) 687,00
--   Retalhos Centro Ltda       mes anterior   PAGO       45 m Jeans Preto x 36,90                1.660,50
--   Ateliê Costura Fina        mes anterior   CANCELADO  15 m Crepe Preto x 28,90                433,50
-- Vendas do mes atual desta migration (sem cancelados): 935,00 + 647,50 + 796,80 = 2.379,30.
-- Cada pedido e localizado pelo cliente + total (combinacao unica tambem junto com a V2.1).

INSERT INTO pedido (id_cliente, id_usuario, id_negociacao, data_pedido, forma_pagamento, status, total)
SELECT c.id_cliente, c.id_usuario, n.id_negociacao, v.data_pedido, v.forma_pagamento, v.status, v.total
FROM (
    SELECT date_trunc('month', LOCALTIMESTAMP) AS inicio_mes, LOCALTIMESTAMP AS agora
) m
CROSS JOIN LATERAL (VALUES
    ('Boutique Tecidos da Serra', 'Linho Cru para coleção de verão',
        m.inicio_mes + (m.agora - m.inicio_mes) * 0.3, 'A_VISTA', 'PAGO',       935.00),
    ('Ateliê Costura Fina',       NULL,
        m.inicio_mes + (m.agora - m.inicio_mes) * 0.6, 'CARTAO',  'PAGO',       647.50),
    ('Boutique Tecidos da Serra', NULL,
        m.inicio_mes + (m.agora - m.inicio_mes) * 0.8, 'BOLETO',  'EM_ABERTO',  796.80),
    ('Ateliê Costura Fina',       NULL,
        m.inicio_mes + (m.agora - m.inicio_mes) * 0.5, 'CHEQUE',  'CANCELADO',  425.00),
    ('Boutique Tecidos da Serra', NULL,
        m.inicio_mes - interval '12 days' + interval '10 hours', 'A_VISTA', 'PAGO',      687.00),
    ('Retalhos Centro Ltda',      NULL,
        m.inicio_mes - interval '18 days' + interval '15 hours', 'BOLETO',  'PAGO',     1660.50),
    ('Ateliê Costura Fina',       NULL,
        m.inicio_mes - interval '8 days'  + interval '11 hours', 'CARTAO',  'CANCELADO', 433.50)
) AS v (cliente, negociacao, data_pedido, forma_pagamento, status, total)
JOIN cliente c ON c.nome = v.cliente
LEFT JOIN negociacao n ON n.descricao = v.negociacao AND n.id_cliente = c.id_cliente
ORDER BY v.data_pedido;

INSERT INTO item_pedido (id_pedido, id_estoque, metragem, preco_metro)
SELECT p.id_pedido, e.id_estoque, v.metragem, v.preco_metro
FROM (VALUES
    ('Boutique Tecidos da Serra',  935.00, 'Linho Natural',   'Cru',    22.00, 42.50),
    ('Ateliê Costura Fina',        647.50, 'Tule de Festa',   'Branco', 35.00, 18.50),
    ('Boutique Tecidos da Serra',  796.80, 'Malha Canelada',  'Preto',  32.00, 24.90),
    ('Ateliê Costura Fina',        425.00, 'Linho Natural',   'Cru',    10.00, 42.50),
    ('Boutique Tecidos da Serra',  687.00, 'Malha Canelada',  'Preto',  30.00, 22.90),
    ('Retalhos Centro Ltda',      1660.50, 'Jeans Stretch',   'Preto',  45.00, 36.90),
    ('Ateliê Costura Fina',        433.50, 'Crepe Georgette', 'Preto',  15.00, 28.90)
) AS v (cliente, total, tecido, cor, metragem, preco_metro)
JOIN cliente c ON c.nome = v.cliente
JOIN pedido p ON p.id_cliente = c.id_cliente AND p.total = v.total
JOIN tecido t ON t.nome = v.tecido
JOIN estoque e ON e.id_tecido = t.id_tecido AND e.cor_codigo = v.cor;

-- ============================================================================
-- 5. INTERACOES (conversas ja realizadas)
-- ============================================================================

INSERT INTO interacao (id_cliente, id_usuario, tipo, data_interacao, descricao)
SELECT c.id_cliente, c.id_usuario, v.tipo, CURRENT_DATE - v.dias_atras, v.descricao
FROM (VALUES
    ('Boutique Tecidos da Serra', 'WHATSAPP', 1,  'Confirmou o recebimento do Linho Cru e pediu cotação da Malha preta.'),
    ('Boutique Tecidos da Serra', 'VISITA',   35, 'Visita à loja para apresentar o mostruário de linhos.'),
    ('Ateliê Costura Fina',       'LIGACAO',  0,  'Ligou para saber o prazo de chegada do Crepe preto.'),
    ('Ateliê Costura Fina',       'VISITA',   4,  'Mostruário de Tule e Crepe entregue no ateliê.'),
    ('Retalhos Centro Ltda',      'OUTRO',    25, 'Avisou que vai encerrar as atividades; cadastro desativado.')
) AS v (cliente, tipo, dias_atras, descricao)
JOIN cliente c ON c.nome = v.cliente;

-- ============================================================================
-- 6. FOLLOW-UPS (data_conclusao NULL = pendente)
-- ============================================================================
-- Pendencias de hoje (somadas as da V2.1): Boutique (hoje) e Costura Fina (atrasado 3 dias).
-- O follow-up de hoje do Retalhos Centro NAO aparece: o cliente esta desativado (regra 6.4).

INSERT INTO follow_up (id_cliente, id_usuario, data_prevista, descricao, data_conclusao)
SELECT c.id_cliente, c.id_usuario, CURRENT_DATE + v.dias, v.descricao,
       CASE WHEN v.concluido_ha IS NOT NULL THEN LOCALTIMESTAMP - make_interval(days => v.concluido_ha) END
FROM (VALUES
    ('Boutique Tecidos da Serra', 0,   'Cobrar o boleto do pedido de Malha Canelada preta.',    NULL::int),
    ('Ateliê Costura Fina',       -3,  'Retornar sobre o mostruário de Tule e Crepe.',          NULL),
    ('Ateliê Costura Fina',       5,   'Enviar fotos do Crepe preto quando chegar.',            NULL),
    ('Boutique Tecidos da Serra', -10, 'Enviar tabela de preços dos linhos.',                   8),
    ('Retalhos Centro Ltda',      0,   'Ligar para confirmar o encerramento.',                  NULL)
) AS v (cliente, dias, descricao, concluido_ha)
JOIN cliente c ON c.nome = v.cliente;

-- ============================================================================
-- 7. AUTOVERIFICACAO (banco inteiro, V2.1 + V2.2): se alguem editar os dados acima e
--    quebrar a coerencia, a migration falha em vez de gravar dados errados.
-- ============================================================================

DO $$
DECLARE
    estoque_divergente   integer;
    pedido_divergente    integer;
    inativo_pendente     integer;
    negociacao_divergente integer;
BEGIN
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
        RAISE EXCEPTION 'Dados de demonstracao incoerentes: % linha(s) de estoque com saldo diferente de entradas - vendas',
            estoque_divergente;
    END IF;

    SELECT COUNT(*) INTO pedido_divergente
    FROM pedido p
    WHERE p.total <> (SELECT COALESCE(ROUND(SUM(ip.metragem * ip.preco_metro), 2), 0)
                      FROM item_pedido ip
                      WHERE ip.id_pedido = p.id_pedido)
       OR NOT EXISTS (SELECT 1 FROM item_pedido ip WHERE ip.id_pedido = p.id_pedido);

    IF pedido_divergente > 0 THEN
        RAISE EXCEPTION 'Dados de demonstracao incoerentes: % pedido(s) sem itens ou com total diferente da soma dos itens',
            pedido_divergente;
    END IF;

    -- Regra 6.5: cliente desativado nao pode ter pedido EM_ABERTO nem negociacao em andamento.
    SELECT COUNT(*) INTO inativo_pendente
    FROM cliente c
    WHERE c.ativo = false
      AND (EXISTS (SELECT 1 FROM pedido p WHERE p.id_cliente = c.id_cliente AND p.status = 'EM_ABERTO')
           OR EXISTS (SELECT 1 FROM negociacao n WHERE n.id_cliente = c.id_cliente
                                                 AND n.estagio NOT IN ('FECHADA', 'PERDIDA')));

    IF inativo_pendente > 0 THEN
        RAISE EXCEPTION 'Dados de demonstracao incoerentes: % cliente(s) desativado(s) com pedido em aberto ou negociacao em andamento',
            inativo_pendente;
    END IF;

    -- Regra 6.1: pedido vindo de negociacao tem o mesmo cliente dela.
    SELECT COUNT(*) INTO negociacao_divergente
    FROM pedido p
    JOIN negociacao n ON n.id_negociacao = p.id_negociacao
    WHERE n.id_cliente <> p.id_cliente;

    IF negociacao_divergente > 0 THEN
        RAISE EXCEPTION 'Dados de demonstracao incoerentes: % pedido(s) com cliente diferente da negociacao de origem',
            negociacao_divergente;
    END IF;
END $$;
