-- V1: cria as 12 tabelas do MVP do Connect CRM, com indices em todas as FKs.
-- Os valores dos CHECK espelham os enums Java (sempre em MAIUSCULO).
-- Constraints nomeadas no padrao do schema: pk_, fk_, uk_, ck_.
-- Migration aplicada nunca e editada: qualquer mudanca de banco entra numa V3, V4...

-- ============================================================================
-- 1. ACESSO
-- ============================================================================

CREATE TABLE usuario (
    id_usuario  BIGINT GENERATED ALWAYS AS IDENTITY,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    senha_hash  VARCHAR(255) NOT NULL,
    criado_em   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuario       PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

-- ============================================================================
-- 2. CLIENTES E TAGS DE INTERESSE
-- ============================================================================

-- telefone: so digitos, com o 55 do Brasil (ex.: 5562999998888).
-- cpf_cnpj e opcional e unico (RD-015): o UNIQUE do PostgreSQL aceita varios NULL.
-- email e opcional e NAO e unico.
CREATE TABLE cliente (
    id_cliente  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_usuario  BIGINT       NOT NULL,
    nome        VARCHAR(150) NOT NULL,
    telefone    VARCHAR(13)  NOT NULL,
    email       VARCHAR(150) NULL,
    cpf_cnpj    VARCHAR(14)  NULL,
    cidade      VARCHAR(100) NULL,
    uf          CHAR(2)      NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT true,
    criado_em   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT pk_cliente          PRIMARY KEY (id_cliente),
    CONSTRAINT fk_cliente_usuario  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT uk_cliente_cpf_cnpj UNIQUE (cpf_cnpj)
);

CREATE INDEX idx_cliente_id_usuario ON cliente (id_usuario);

CREATE TABLE tag (
    id_tag  BIGINT GENERATED ALWAYS AS IDENTITY,
    nome    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id_tag)
);

-- Unico ignorando maiusculas/minusculas (evita "Linho" e "linho" ao mesmo tempo).
CREATE UNIQUE INDEX idx_tag_nome_lower ON tag (LOWER(nome));

CREATE TABLE cliente_tag (
    id_cliente  BIGINT NOT NULL,
    id_tag      BIGINT NOT NULL,
    CONSTRAINT pk_cliente_tag         PRIMARY KEY (id_cliente, id_tag),
    CONSTRAINT fk_cliente_tag_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente) ON DELETE CASCADE,
    CONSTRAINT fk_cliente_tag_tag     FOREIGN KEY (id_tag)     REFERENCES tag (id_tag)         ON DELETE CASCADE
);

CREATE INDEX idx_cliente_tag_id_cliente ON cliente_tag (id_cliente);
CREATE INDEX idx_cliente_tag_id_tag     ON cliente_tag (id_tag);

-- ============================================================================
-- 3. CRM: NEGOCIACOES (FUNIL), INTERACOES E FOLLOW-UPS
-- ============================================================================

CREATE TABLE negociacao (
    id_negociacao             BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cliente                BIGINT       NOT NULL,
    id_usuario                BIGINT       NOT NULL,
    descricao                 VARCHAR(200) NOT NULL,
    estagio                   VARCHAR(30)  NOT NULL,
    data_criacao              TIMESTAMP    NOT NULL DEFAULT now(),
    data_ultima_movimentacao  TIMESTAMP    NOT NULL DEFAULT now(),
    -- Preenchida quando a negociacao vai para FECHADA ou PERDIDA.
    data_fechamento           TIMESTAMP    NULL,
    CONSTRAINT pk_negociacao         PRIMARY KEY (id_negociacao),
    CONSTRAINT fk_negociacao_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_negociacao_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_negociacao_estagio CHECK (estagio IN (
        'PRIMEIRO_CONTATO',
        'MOSTRUARIO_ENVIADO',
        'AGUARDANDO_RESPOSTA',
        'FECHADA',
        'PERDIDA'
    ))
);

CREATE INDEX idx_negociacao_id_cliente ON negociacao (id_cliente);
CREATE INDEX idx_negociacao_id_usuario ON negociacao (id_usuario);

CREATE TABLE interacao (
    id_interacao    BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cliente      BIGINT      NOT NULL,
    id_usuario      BIGINT      NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    data_interacao  DATE        NOT NULL,
    descricao       TEXT        NOT NULL,
    criado_em       TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_interacao         PRIMARY KEY (id_interacao),
    CONSTRAINT fk_interacao_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_interacao_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_interacao_tipo    CHECK (tipo IN ('WHATSAPP', 'LIGACAO', 'VISITA', 'OUTRO'))
);

CREATE INDEX idx_interacao_id_cliente ON interacao (id_cliente);
CREATE INDEX idx_interacao_id_usuario ON interacao (id_usuario);

-- data_conclusao NULL = pendente; preenchida = concluido.
CREATE TABLE follow_up (
    id_follow_up    BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cliente      BIGINT    NOT NULL,
    id_usuario      BIGINT    NOT NULL,
    data_prevista   DATE      NOT NULL,
    descricao       TEXT      NOT NULL,
    data_conclusao  TIMESTAMP NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT pk_follow_up         PRIMARY KEY (id_follow_up),
    CONSTRAINT fk_follow_up_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_follow_up_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE INDEX idx_follow_up_id_cliente ON follow_up (id_cliente);
CREATE INDEX idx_follow_up_id_usuario ON follow_up (id_usuario);

-- ============================================================================
-- 4. CATALOGO E ESTOQUE
-- ============================================================================

CREATE TABLE tecido (
    id_tecido    BIGINT GENERATED ALWAYS AS IDENTITY,
    nome         VARCHAR(100)  NOT NULL,
    composicao   VARCHAR(150)  NULL,
    gramatura    INTEGER       NULL,
    largura      NUMERIC(4,2)  NULL,
    preco_metro  NUMERIC(10,2) NOT NULL,
    foto_url     VARCHAR(255)  NULL,
    ativo        BOOLEAN       NOT NULL DEFAULT true,
    criado_em    TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT pk_tecido             PRIMARY KEY (id_tecido),
    CONSTRAINT ck_tecido_preco_metro CHECK (preco_metro > 0)
);

-- CHECK (metragem >= 0) impede vender metragem que nao existe no saldo.
CREATE TABLE estoque (
    id_estoque     BIGINT GENERATED ALWAYS AS IDENTITY,
    id_tecido      BIGINT        NOT NULL,
    cor_codigo     VARCHAR(50)   NOT NULL,
    metragem       NUMERIC(10,2) NOT NULL DEFAULT 0,
    atualizado_em  TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT pk_estoque           PRIMARY KEY (id_estoque),
    CONSTRAINT fk_estoque_tecido    FOREIGN KEY (id_tecido) REFERENCES tecido (id_tecido),
    CONSTRAINT uk_estoque_tecido_cor UNIQUE (id_tecido, cor_codigo),
    CONSTRAINT ck_estoque_metragem  CHECK (metragem >= 0)
);

CREATE INDEX idx_estoque_id_tecido ON estoque (id_tecido);

CREATE TABLE entrada_estoque (
    id_entrada    BIGINT GENERATED ALWAYS AS IDENTITY,
    id_estoque    BIGINT        NOT NULL,
    metragem      NUMERIC(10,2) NOT NULL,
    data_entrada  DATE          NOT NULL,
    criado_em     TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT pk_entrada_estoque          PRIMARY KEY (id_entrada),
    CONSTRAINT fk_entrada_estoque_estoque  FOREIGN KEY (id_estoque) REFERENCES estoque (id_estoque),
    CONSTRAINT ck_entrada_estoque_metragem CHECK (metragem > 0)
);

CREATE INDEX idx_entrada_estoque_id_estoque ON entrada_estoque (id_estoque);

-- ============================================================================
-- 5. PEDIDOS E ITENS DO PEDIDO
-- ============================================================================

CREATE TABLE pedido (
    id_pedido        BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cliente       BIGINT        NOT NULL,
    id_usuario       BIGINT        NOT NULL,
    id_negociacao    BIGINT        NULL,
    data_pedido      TIMESTAMP     NOT NULL DEFAULT now(),
    forma_pagamento  VARCHAR(20)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    total            NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_pedido                 PRIMARY KEY (id_pedido),
    CONSTRAINT fk_pedido_cliente         FOREIGN KEY (id_cliente)    REFERENCES cliente (id_cliente),
    CONSTRAINT fk_pedido_usuario         FOREIGN KEY (id_usuario)    REFERENCES usuario (id_usuario),
    CONSTRAINT fk_pedido_negociacao      FOREIGN KEY (id_negociacao) REFERENCES negociacao (id_negociacao),
    CONSTRAINT ck_pedido_forma_pagamento CHECK (forma_pagamento IN ('A_VISTA', 'CARTAO', 'BOLETO', 'CHEQUE')),
    CONSTRAINT ck_pedido_status          CHECK (status IN ('EM_ABERTO', 'PAGO', 'CANCELADO')),
    CONSTRAINT ck_pedido_total           CHECK (total >= 0)
);

CREATE INDEX idx_pedido_id_cliente    ON pedido (id_cliente);
CREATE INDEX idx_pedido_id_usuario    ON pedido (id_usuario);
CREATE INDEX idx_pedido_id_negociacao ON pedido (id_negociacao);

-- preco_metro e um retrato do preco no ato da venda, nao segue o preco atual do tecido.
-- Regra: o mesmo estoque (tecido + cor) aparece uma unica vez por pedido.
CREATE TABLE item_pedido (
    id_item_pedido  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_pedido       BIGINT        NOT NULL,
    id_estoque      BIGINT        NOT NULL,
    metragem        NUMERIC(10,2) NOT NULL,
    preco_metro     NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_item_pedido                PRIMARY KEY (id_item_pedido),
    CONSTRAINT fk_item_pedido_pedido         FOREIGN KEY (id_pedido)  REFERENCES pedido (id_pedido),
    CONSTRAINT fk_item_pedido_estoque        FOREIGN KEY (id_estoque) REFERENCES estoque (id_estoque),
    CONSTRAINT uk_item_pedido_pedido_estoque UNIQUE (id_pedido, id_estoque),
    CONSTRAINT ck_item_pedido_metragem       CHECK (metragem > 0),
    CONSTRAINT ck_item_pedido_preco_metro    CHECK (preco_metro > 0)
);

CREATE INDEX idx_item_pedido_id_pedido  ON item_pedido (id_pedido);
CREATE INDEX idx_item_pedido_id_estoque ON item_pedido (id_estoque);
