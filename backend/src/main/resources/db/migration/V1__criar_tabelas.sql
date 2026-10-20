-- V1: cria as 12 tabelas do MVP do Connect CRM, com indices em todas as FKs.
-- Os valores dos CHECK espelham os enums Java (sempre em MAIUSCULO).
-- Migration aplicada nunca e editada: qualquer mudanca de banco entra numa V3, V4...

-- ============================================================================
-- 1. ACESSO
-- ============================================================================

CREATE TABLE usuario (
    id_usuario BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- 2. CLIENTES E TAGS DE INTERESSE
-- ============================================================================

-- email e cpf_cnpj sao opcionais: o UNIQUE do PostgreSQL aceita varios NULL.
CREATE TABLE cliente (
    id_cliente BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuario (id_usuario),
    nome VARCHAR(150) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NULL UNIQUE,
    cpf_cnpj VARCHAR(14) NULL UNIQUE,
    cidade VARCHAR(100) NULL,
    uf CHAR(2) NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_cliente_id_usuario ON cliente (id_usuario);

CREATE TABLE tag (
    id_tag BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(50) NOT NULL
);

-- Unico ignorando maiusculas/minusculas (evita "Linho" e "linho" ao mesmo tempo).
CREATE UNIQUE INDEX idx_tag_nome_lower ON tag (LOWER(nome));

CREATE TABLE cliente_tag (
    id_cliente BIGINT NOT NULL REFERENCES cliente (id_cliente) ON DELETE CASCADE,
    id_tag BIGINT NOT NULL REFERENCES tag (id_tag) ON DELETE CASCADE,
    PRIMARY KEY (id_cliente, id_tag)
);

CREATE INDEX idx_cliente_tag_id_cliente ON cliente_tag (id_cliente);
CREATE INDEX idx_cliente_tag_id_tag ON cliente_tag (id_tag);

-- ============================================================================
-- 3. CRM: NEGOCIACOES (FUNIL), INTERACOES E FOLLOW-UPS
-- ============================================================================

CREATE TABLE negociacao (
    id_negociacao BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente (id_cliente),
    id_usuario BIGINT NOT NULL REFERENCES usuario (id_usuario),
    descricao VARCHAR(200) NOT NULL,
    estagio VARCHAR(30) NOT NULL CHECK (estagio IN (
        'PRIMEIRO_CONTATO',
        'MOSTRUARIO_ENVIADO',
        'AGUARDANDO_RESPOSTA',
        'FECHADA',
        'PERDIDA'
    )),
    data_criacao TIMESTAMP NOT NULL DEFAULT now(),
    data_ultima_movimentacao TIMESTAMP NOT NULL DEFAULT now(),
    -- Preenchida quando a negociacao vai para FECHADA ou PERDIDA.
    data_fechamento TIMESTAMP NULL
);

CREATE INDEX idx_negociacao_id_cliente ON negociacao (id_cliente);
CREATE INDEX idx_negociacao_id_usuario ON negociacao (id_usuario);

CREATE TABLE interacao (
    id_interacao BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente (id_cliente),
    id_usuario BIGINT NOT NULL REFERENCES usuario (id_usuario),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('WHATSAPP', 'LIGACAO', 'VISITA', 'OUTRO')),
    data_interacao DATE NOT NULL,
    descricao TEXT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_interacao_id_cliente ON interacao (id_cliente);
CREATE INDEX idx_interacao_id_usuario ON interacao (id_usuario);

-- data_conclusao NULL = pendente; preenchida = concluido.
CREATE TABLE follow_up (
    id_follow_up BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente (id_cliente),
    id_usuario BIGINT NOT NULL REFERENCES usuario (id_usuario),
    data_prevista DATE NOT NULL,
    descricao TEXT NOT NULL,
    data_conclusao TIMESTAMP NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_follow_up_id_cliente ON follow_up (id_cliente);
CREATE INDEX idx_follow_up_id_usuario ON follow_up (id_usuario);

-- ============================================================================
-- 4. CATALOGO E ESTOQUE
-- ============================================================================

CREATE TABLE tecido (
    id_tecido BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    composicao VARCHAR(150) NULL,
    gramatura INTEGER NULL,
    largura NUMERIC(4,2) NULL,
    preco_metro NUMERIC(10,2) NOT NULL CHECK (preco_metro > 0),
    foto_url VARCHAR(255) NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

-- CHECK (metragem >= 0) impede vender metragem que nao existe no saldo.
CREATE TABLE estoque (
    id_estoque BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tecido BIGINT NOT NULL REFERENCES tecido (id_tecido),
    cor_codigo VARCHAR(50) NOT NULL,
    metragem NUMERIC(10,2) NOT NULL CHECK (metragem >= 0),
    atualizado_em TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_estoque_tecido_cor UNIQUE (id_tecido, cor_codigo)
);

CREATE INDEX idx_estoque_id_tecido ON estoque (id_tecido);

CREATE TABLE entrada_estoque (
    id_entrada BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_estoque BIGINT NOT NULL REFERENCES estoque (id_estoque),
    metragem NUMERIC(10,2) NOT NULL CHECK (metragem > 0),
    data_entrada DATE NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_entrada_estoque_id_estoque ON entrada_estoque (id_estoque);

-- ============================================================================
-- 5. PEDIDOS E ITENS DO PEDIDO
-- ============================================================================

CREATE TABLE pedido (
    id_pedido BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente (id_cliente),
    id_usuario BIGINT NOT NULL REFERENCES usuario (id_usuario),
    id_negociacao BIGINT NULL REFERENCES negociacao (id_negociacao),
    data_pedido TIMESTAMP NOT NULL DEFAULT now(),
    forma_pagamento VARCHAR(20) NOT NULL CHECK (forma_pagamento IN ('A_VISTA', 'CARTAO', 'BOLETO', 'CHEQUE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('EM_ABERTO', 'PAGO', 'CANCELADO')),
    total NUMERIC(10,2) NOT NULL CHECK (total >= 0)
);

CREATE INDEX idx_pedido_id_cliente ON pedido (id_cliente);
CREATE INDEX idx_pedido_id_usuario ON pedido (id_usuario);
CREATE INDEX idx_pedido_id_negociacao ON pedido (id_negociacao);

-- preco_metro e um retrato do preco no ato da venda, nao segue o preco atual do tecido.
CREATE TABLE item_pedido (
    id_item_pedido BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedido (id_pedido),
    id_estoque BIGINT NOT NULL REFERENCES estoque (id_estoque),
    metragem NUMERIC(10,2) NOT NULL CHECK (metragem > 0),
    preco_metro NUMERIC(10,2) NOT NULL CHECK (preco_metro > 0)
);

CREATE INDEX idx_item_pedido_id_pedido ON item_pedido (id_pedido);
CREATE INDEX idx_item_pedido_id_estoque ON item_pedido (id_estoque);
