-- V2.3 (SO DESENVOLVIMENTO): troca os tecidos de exemplo pelos tricolines que o revendedor
-- realmente trabalha (mostruarios enviados pelo Yan em 16/11/2026) e acrescenta mais exemplos
-- do mesmo nicho.
-- Roda apenas com o perfil "dev" (application-dev.yaml inclui classpath:db/dev), depois da V2.2.
--
-- Por que uma migration nova e nao editar a V2.1/V2.2: migration aplicada nunca e editada
-- (AGENTS 1). Quem ja tem o banco criado recebe so esta V2.3 ao subir o back-end; quem recria
-- o banco do zero roda V2.1 -> V2.2 -> V2.3 e chega no mesmo resultado.
--
-- O que muda:
--   * os 6 tecidos viram tricolines (mesmos ids: pedidos e estoque continuam ligados);
--   * as cores do estoque viram codigo do mostruario + nome (ex.: "05 Marinho");
--   * descricoes de negociacoes, interacoes e follow-ups passam a falar dos tricolines;
--   * as tags de exemplo passam a ser do nicho (camisaria, xadrez...);
--   * entram 1 tecido e 5 linhas de estoque novas, cada uma com a sua entrada.
-- Os itens de pedido NAO mudam: guardam o preco da epoca da venda (regra 6.2).
-- O tecido "Tricoline Lisa com Elastano Premium" fica de fora de proposito: e o tecido
-- cadastrado ao vivo no roteiro da apresentacao.

-- ============================================================================
-- 1. CATALOGO: cada tecido de exemplo vira um tricoline (largura padrao 1,50 m)
-- ============================================================================

UPDATE tecido t
SET nome = v.novo_nome,
    composicao = v.composicao,
    gramatura = v.gramatura,
    largura = 1.50,
    preco_metro = v.preco_metro
FROM (VALUES
    ('Crepe Georgette',  'Tricoline Pura Xadrez',         '100% Algodão', 110, 24.90),
    ('Malha Canelada',   'Tricoline Pura Xadrez Premium', '100% Algodão', 110, 27.90),
    ('Linho Natural',    'Tricoline Flame Premium',       '100% Algodão', 120, 32.90),
    ('Suede Automotivo', 'Tricoline Lisa',                '100% Algodão', 115, 21.90),
    ('Tule de Festa',    'Tricoline Estampada',           '100% Algodão', 110, 26.90),
    ('Jeans Stretch',    'Tricoline Listrada',            '100% Algodão', 110, 23.90)
) AS v (nome_antigo, novo_nome, composicao, gramatura, preco_metro)
WHERE t.nome = v.nome_antigo;

-- ============================================================================
-- 2. ESTOQUE: cor = codigo do mostruario + nome
-- ============================================================================

UPDATE estoque e
SET cor_codigo = v.cor_nova
FROM (VALUES
    ('Tricoline Pura Xadrez',         'Off-white',    '05 Marinho'),
    ('Tricoline Pura Xadrez',         'Rose',         '07 Rosa'),
    ('Tricoline Pura Xadrez',         'Preto',        '06 Preto'),
    ('Tricoline Flame Premium',       'Natural',      '02 Cru'),
    ('Tricoline Flame Premium',       'Cru',          '01 Branco'),
    ('Tricoline Lisa',                'Cinza Chumbo', '16 Preto'),
    ('Tricoline Lisa',                'Bege',         '07 Bege'),
    ('Tricoline Pura Xadrez Premium', 'Cru',          '13 Branco e marinho'),
    ('Tricoline Pura Xadrez Premium', 'Preto',        '02 Preto e branco'),
    ('Tricoline Estampada',           'Marfim',       '02 Floral rosa'),
    ('Tricoline Estampada',           'Branco',       '01 Floral azul'),
    ('Tricoline Listrada',            'Azul Índigo',  '05 Azul e branco'),
    ('Tricoline Listrada',            'Preto',        '11 Preto e branco')
) AS v (tecido, cor_antiga, cor_nova)
JOIN tecido t ON t.nome = v.tecido
WHERE e.id_tecido = t.id_tecido AND e.cor_codigo = v.cor_antiga;

-- ============================================================================
-- 3. MAIS EXEMPLOS DO NICHO: um tecido novo e novas cores (saldo = entrada, sem vendas)
-- ============================================================================
--   linha                                    entrada   saldo
--   Tricoline Pura Xadrez / 17 Vinho          45        45
--   Tricoline Pura Xadrez / 24 Azul royal     30        30
--   Tricoline Pura Xadrez / 31 Vermelho       12        12   (baixo)
--   Tricoline Mista / 01 Branco              120       120
--   Tricoline Mista / 10 Azul-marinho         60        60

INSERT INTO tecido (nome, composicao, gramatura, largura, preco_metro)
VALUES ('Tricoline Mista', '67% Poliéster / 33% Algodão', 105, 1.50, 16.90);

INSERT INTO estoque (id_tecido, cor_codigo, metragem)
SELECT t.id_tecido, v.cor, v.metragem
FROM (VALUES
    ('Tricoline Pura Xadrez', '17 Vinho',          45.00),
    ('Tricoline Pura Xadrez', '24 Azul royal',     30.00),
    ('Tricoline Pura Xadrez', '31 Vermelho',       12.00),
    ('Tricoline Mista',       '01 Branco',        120.00),
    ('Tricoline Mista',       '10 Azul-marinho',   60.00)
) AS v (tecido, cor, metragem)
JOIN tecido t ON t.nome = v.tecido;

INSERT INTO entrada_estoque (id_estoque, metragem, data_entrada)
SELECT e.id_estoque, v.metragem, CURRENT_DATE - v.dias_atras
FROM (VALUES
    ('Tricoline Pura Xadrez', '17 Vinho',          45.00, 20),
    ('Tricoline Pura Xadrez', '24 Azul royal',     30.00, 12),
    ('Tricoline Pura Xadrez', '31 Vermelho',       12.00, 6),
    ('Tricoline Mista',       '01 Branco',        120.00, 28),
    ('Tricoline Mista',       '10 Azul-marinho',   60.00, 9)
) AS v (tecido, cor, metragem, dias_atras)
JOIN tecido t ON t.nome = v.tecido
JOIN estoque e ON e.id_tecido = t.id_tecido AND e.cor_codigo = v.cor;

-- ============================================================================
-- 4. TAGS DE EXEMPLO DO NICHO (os clientes continuam com as mesmas tags, so o nome muda)
-- ============================================================================

UPDATE tag t
SET nome = v.novo_nome
FROM (VALUES
    ('Moda Festa', 'Moda Infantil'),
    ('Linho',      'Camisaria'),
    ('Estofaria',  'Uniformes'),
    ('Malha',      'Xadrez'),
    ('Denim',      'Confecção')
) AS v (nome_antigo, novo_nome)
WHERE t.nome = v.nome_antigo;

-- ============================================================================
-- 5. TEXTOS: negociacoes, interacoes e follow-ups passam a falar dos tricolines
-- ============================================================================

UPDATE negociacao n
SET descricao = v.nova
FROM (VALUES
    ('Proposta de Linho Natural - 200m',        'Proposta de Tricoline Flame Premium - 200 m'),
    ('Reposição de Malha Canelada',             'Reposição de Tricoline Xadrez Premium'),
    ('Tule de Festa e Crepe',                   'Tricoline Estampada e Xadrez para vestidos'),
    ('Suede para linha de poltronas',           'Tricoline Lisa para linha de camisas'),
    ('Jeans Stretch',                           'Tricoline Listrada para camisas sociais'),
    ('Crepe Georgette para coleção de noivas',  'Tricoline Pura Xadrez para coleção de verão'),
    ('Linho para cortinas',                     'Tricoline Flame para coleção de camisas'),
    ('Crepe preto para coleção de inverno',     'Tricoline Xadrez preto para coleção de inverno'),
    ('Tule e Crepe para vestidos de debutante', 'Tricoline Estampada e Xadrez para vestidos infantis'),
    ('Reposição de Malha Canelada preta',       'Reposição de Xadrez Premium preto e branco'),
    ('Linho Cru para coleção de verão',         'Tricoline Flame branca para coleção de verão'),
    ('Jeans preto para reposição',              'Tricoline Listrada preta para reposição')
) AS v (antiga, nova)
WHERE n.descricao = v.antiga;

UPDATE interacao i
SET descricao = v.nova
FROM (VALUES
    ('Enviou fotos do Tule de Festa marfim e do Crepe rose; cliente gostou do caimento.',
     'Enviou fotos da Tricoline Estampada e da Xadrez rosa; cliente gostou do toque.'),
    ('Visita no ateliê para fechar o Crepe Georgette da coleção de noivas.',
     'Visita no ateliê para fechar a Tricoline Pura Xadrez da coleção de verão.'),
    ('Pediu proposta de 200 m de Linho Natural para estofaria.',
     'Pediu proposta de 200 m de Tricoline Flame Premium para camisaria.'),
    ('Mostruário de Suede Automotivo enviado por foto.',
     'Mostruário de Tricoline Lisa enviado por foto.'),
    ('Desistiu do linho para cortinas: comprou de outro fornecedor.',
     'Desistiu da Tricoline Flame: comprou de outro fornecedor.'),
    ('Perguntou sobre reposição de Malha Canelada cru.',
     'Perguntou sobre reposição da Xadrez Premium branco e marinho.'),
    ('Conversa na feira de confecções sobre Jeans Stretch para a nova coleção.',
     'Conversa na feira de confecções sobre Tricoline Listrada para a nova coleção.'),
    ('Confirmou o recebimento do Linho Cru e pediu cotação da Malha preta.',
     'Confirmou o recebimento da Tricoline Flame branca e pediu cotação da Xadrez Premium preto e branco.'),
    ('Visita à loja para apresentar o mostruário de linhos.',
     'Visita à loja para apresentar o mostruário de tricolines.'),
    ('Ligou para saber o prazo de chegada do Crepe preto.',
     'Ligou para saber o prazo de chegada da Xadrez preta.'),
    ('Mostruário de Tule e Crepe entregue no ateliê.',
     'Mostruário de Tricoline Estampada e Xadrez entregue no ateliê.')
) AS v (antiga, nova)
WHERE i.descricao = v.antiga;

UPDATE follow_up f
SET descricao = v.nova
FROM (VALUES
    ('Retornar sobre disponibilidade de Tule na cor marfim.',
     'Retornar sobre disponibilidade da Tricoline Estampada floral rosa.'),
    ('Cobrar resposta sobre o Jeans Stretch.',
     'Cobrar resposta sobre a Tricoline Listrada.'),
    ('Confirmar recebimento do mostruário de Suede.',
     'Confirmar recebimento do mostruário de Tricoline Lisa.'),
    ('Cobrar o boleto do pedido de Malha Canelada preta.',
     'Cobrar o boleto do pedido de Xadrez Premium preto e branco.'),
    ('Retornar sobre o mostruário de Tule e Crepe.',
     'Retornar sobre o mostruário de Tricoline Estampada e Xadrez.'),
    ('Enviar fotos do Crepe preto quando chegar.',
     'Enviar fotos da Xadrez preta quando chegar.'),
    ('Enviar tabela de preços dos linhos.',
     'Enviar tabela de preços das tricolines.')
) AS v (antiga, nova)
WHERE f.descricao = v.antiga;

-- ============================================================================
-- 6. AUTOVERIFICACAO: nenhum tecido antigo sobrou e o estoque continua coerente
-- ============================================================================

DO $$
DECLARE
    tecido_antigo       integer;
    cor_antiga          integer;
    estoque_divergente  integer;
BEGIN
    SELECT COUNT(*) INTO tecido_antigo
    FROM tecido
    WHERE nome IN ('Crepe Georgette', 'Malha Canelada', 'Linho Natural',
                   'Suede Automotivo', 'Tule de Festa', 'Jeans Stretch');

    IF tecido_antigo > 0 THEN
        RAISE EXCEPTION 'V2.3: % tecido(s) de exemplo antigo(s) nao foram trocados', tecido_antigo;
    END IF;

    -- Toda cor passa a comecar pelo codigo do mostruario (dois digitos).
    SELECT COUNT(*) INTO cor_antiga
    FROM estoque
    WHERE cor_codigo !~ '^[0-9]{2} ';

    IF cor_antiga > 0 THEN
        RAISE EXCEPTION 'V2.3: % linha(s) de estoque sem o codigo do mostruario na cor', cor_antiga;
    END IF;

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
        RAISE EXCEPTION 'V2.3: % linha(s) de estoque com saldo diferente de entradas - vendas', estoque_divergente;
    END IF;
END $$;
