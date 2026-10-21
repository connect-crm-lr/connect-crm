# Connect CRM — Revisão da Modelagem do Banco de Dados

> **Data:** 13/10/2026
> **Base da revisão:** MER/DER apresentado pelo João Pedro (tabelas USUARIO, CLIENTE, TAG, CLIENTE_TAG, TECIDO, ITEM_ESTOQUE, PEDIDO, ITEM_PEDIDO, OPORTUNIDADE e NOTIFICACAO_WHATSAPP)
> **Referências usadas:** mockup do Figma (prompt do protótipo), documento do projeto ("Modelo de projeto de software"), decisões da equipe e informações levantadas com o Yan sobre o trabalho do pai dele
> **SGBD:** PostgreSQL · **Migrations:** Flyway · **Backend:** Java + Spring Boot

---

## Sumário

1. [Objetivo deste documento](#1-objetivo-deste-documento)
2. [Contexto do negócio (base de todas as decisões)](#2-contexto-do-negócio-base-de-todas-as-decisões)
3. [Resumo das alterações](#3-resumo-das-alterações)
4. [Padrões técnicos gerais](#4-padrões-técnicos-gerais)
5. [Alterações tabela por tabela](#5-alterações-tabela-por-tabela)
6. [Regras de negócio que ficam no Java](#6-regras-de-negócio-que-ficam-no-java)
7. [Consultas de referência](#7-consultas-de-referência)
8. [Impactos fora do banco (front-end e documentação)](#8-impactos-fora-do-banco-front-end-e-documentação)
9. [Pendências e itens fora do MVP](#9-pendências-e-itens-fora-do-mvp)

---

## 1. Objetivo deste documento

O modelo original foi revisado tabela por tabela, comparando cada coluna com:

- o que as telas do mockup realmente mostram e pedem;
- as decisões de escopo já tomadas pela equipe;
- a forma real como o pai do Yan trabalha (informações confirmadas com o Yan);
- boas práticas de PostgreSQL (tipos, constraints, integridade).

Este documento registra **o que mudou, por que mudou e como cada decisão foi tomada**, para que as migrations do Flyway sejam escritas já alinhadas com toda a equipe e não haja retrabalho.

A estrutura geral do modelo original foi mantida: a separação entre catálogo (TECIDO) e estoque, a tabela associativa de tags, o preço guardado no item do pedido e o uso de PK/FK estavam corretos e foram aproveitados.

---

## 2. Contexto do negócio (base de todas as decisões)

Estas informações explicam a maior parte das escolhas do modelo:

| Fato sobre o negócio | Impacto no banco |
|---|---|
| O sistema é para **um único usuário**: o pai do Yan, revendedor autônomo de tecidos. | No MVP não há cadastro de usuários nem perfis de acesso. |
| Ele tem um **sócio** que compra os tecidos e mantém o estoque geral. O pai do Yan retira **só a parte dele**, revende e os dois dividem o lucro. | O estoque do sistema é **somente o dele**: serve para ele saber quantos metros ainda tem para revender. |
| Antes, ele controlava tudo em **planilhas**. | O sistema precisa fazer, no mínimo, o que a planilha fazia (por isso o histórico de entradas de estoque). |
| Ele atende os clientes **principalmente pelo WhatsApp**. | O contato é feito por link `wa.me`; o telefone precisa estar em formato padronizado. |
| Ele **vende tudo por metro**. | Não existe unidade "quilo"; o estoque é sempre em metros. |
| Ele **só vende o que tem em estoque** (confirmado com o Yan). | Todo item de pedido aponta obrigatoriamente para uma linha de estoque. |
| Ele aceita **à vista, cartão, boleto e cheque**. | Pedido tem forma de pagamento e status de pagamento (em aberto / pago). |

---

## 3. Resumo das alterações

| Tabela | O que aconteceu | Principais mudanças |
|---|---|---|
| USUARIO | Revisada | Sai `cargo`; `senha` vira `senha_hash`; `email` único |
| CLIENTE | Revisada | `endereco` vira `cidade` + `uf`; `status` vira `ativo`; `cpf_cnpj` único |
| TAG | Revisada | Nome único ignorando maiúsculas/minúsculas |
| CLIENTE_TAG | Revisada | PK composta; `ON DELETE CASCADE` |
| OPORTUNIDADE → **NEGOCIACAO** | Renomeada e revisada | Sai `valor_estimado`; `titulo` vira `descricao`; etapas fixas com `CHECK`; novas colunas de data |
| **INTERACAO** | **Nova** | Registro de conversas com o cliente |
| **FOLLOW_UP** | **Nova** | Próximas ações com data (pendências) |
| TECIDO | Revisada | Saem `tipo` e `descricao`; entram `gramatura`, `largura`, `foto_url`, `ativo`; `preco_metro` |
| ITEM_ESTOQUE → **ESTOQUE** | Renomeada e revisada | Uma linha por tecido + cor com a metragem total; saem `localizacao`, `data_entrada`, `status_estoque` |
| **ENTRADA_ESTOQUE** | **Nova** | Histórico de cada chegada de tecido |
| PEDIDO | Revisada | Entram `id_negociacao`, `forma_pagamento`; `status` com valores fixos |
| ITEM_PEDIDO | Revisada | `id_item_estoque` vira `id_estoque`; `quantidade` vira `metragem`; `preco_unitario` vira `preco_metro` |
| NOTIFICACAO_WHATSAPP | **Removida** | WhatsApp só por link |

---

## 4. Padrões técnicos gerais

Estes padrões valem para **todas** as tabelas.

### 4.1 Tipos de dados

| Antes (modelo original) | Depois | Motivo |
|---|---|---|
| `int` nas PKs | `bigint GENERATED ALWAYS AS IDENTITY` | Padrão moderno do PostgreSQL para chave auto-incremento; `bigint` evita limite de faixa e é o tipo que o JPA mapeia como `Long`. |
| `datetime` | `timestamp` (data e hora) ou `date` (só data) | `datetime` **não existe** no PostgreSQL; a migration falharia. |
| `decimal` sem precisão | `numeric(10,2)` | Dinheiro e metragem precisam de precisão definida para não haver arredondamento diferente em cada lugar. |
| `varchar` sem tamanho | `varchar(n)` com tamanho definido | Documenta o limite esperado de cada campo. |

### 4.2 Valores fixos (status, tipos, etapas)

Todo campo com uma lista fechada de valores usa:

- `varchar` + constraint `CHECK (coluna IN (...))`;
- valores em **MAIÚSCULO, sem acento e com underline** (ex.: `AGUARDANDO_RESPOSTA`, `LIGACAO`).

**Motivo:** o banco recusa qualquer valor fora da lista (sem depender só do Java), e os valores ficam idênticos aos `enum` do Java, mapeados com `@Enumerated(EnumType.STRING)`. A tela é que converte para o texto amigável ("Aguardando resposta", "Ligação").

### 4.3 Outras convenções

- Nomes de tabelas e colunas em `snake_case`, no singular.
- Chaves estrangeiras com o mesmo nome da PK referenciada (`id_cliente`, `id_tecido`...).
- **Criar índice em toda coluna FK.** O PostgreSQL não cria esses índices automaticamente, e as telas sempre buscam "tudo deste cliente", "itens deste pedido" etc.
- Registros importantes (cliente, tecido, pedido) **nunca são apagados**: são desativados ou cancelados, para preservar o histórico de vendas.
- Coluna `criado_em timestamp NOT NULL DEFAULT now()` nas tabelas de registro, para auditoria.

---

## 5. Alterações tabela por tabela

### 5.1 USUARIO

**Antes:** `id_usuario, nome, email, senha, cargo, data_criacao`

**Depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_usuario | bigint identity | PK |
| nome | varchar(100) | NOT NULL |
| email | varchar(150) | NOT NULL, UNIQUE |
| senha_hash | varchar(255) | NOT NULL |
| criado_em | timestamp | NOT NULL, default now() |

**Alterações e motivos:**

1. **`cargo` removido.** O sistema não tem perfis de acesso (sem gestor, sem administrador), então nenhuma tela ou regra usaria esse campo.
2. **`senha` → `senha_hash`.** A senha nunca é guardada em texto puro: o Spring Security grava o hash (BCrypt). O nome da coluna deixa isso explícito.
3. **`email` UNIQUE.** É o login; não pode haver dois usuários com o mesmo e-mail.
4. **`data_criacao` → `criado_em`**, seguindo o padrão do documento.

**Como foi decidido:**

- O mockup diz, de forma explícita, que **não deve existir** perfil, gestor ou administrador.
- A equipe decidiu que a **tela de cadastro de usuários fica como funcionalidade futura**: no MVP, só o pai do Yan usa o sistema.
- O usuário dele é criado por uma **migration de seed** do Flyway (INSERT com a senha já em hash).
- A equipe decidiu **manter as FKs `id_usuario`** em CLIENTE, NEGOCIACAO, PEDIDO, INTERACAO e FOLLOW_UP. No MVP elas sempre apontam para o mesmo usuário, mas quando o cadastro de usuários entrar, a estrutura já estará pronta, sem precisar de migrations em tabelas com dados.

---

### 5.2 CLIENTE

**Antes:** `id_cliente, id_usuario, nome, cpf_cnpj, email, telefone, endereco, status`

**Depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_cliente | bigint identity | PK |
| id_usuario | bigint | NOT NULL, FK → usuario |
| nome | varchar(150) | NOT NULL (nome ou razão social) |
| telefone | varchar(13) | NOT NULL — só dígitos, com 55 (ex.: `5562999998888`) |
| email | varchar(150) | NULL |
| cpf_cnpj | varchar(14) | NULL, UNIQUE — só dígitos |
| cidade | varchar(100) | NULL |
| uf | char(2) | NULL |
| ativo | boolean | NOT NULL, default true |
| criado_em | timestamp | NOT NULL, default now() |

**Alterações e motivos:**

1. **`endereco` → `cidade` + `uf`.** O mockup pede só cidade/UF no cadastro, e a equipe decidiu que o endereço completo não é necessário.
2. **`status` → `ativo` (boolean).** A ideia original era "preservar o histórico de clientes desativados". Um boolean diz isso diretamente: o cliente nunca é apagado (ele tem pedidos ligados), só desativado.
3. **Telefone só com dígitos e o DDI 55.** Assim o link do WhatsApp (`https://wa.me/5562999998888`) é montado sem nenhum tratamento. A máscara "(62) 99999-8888" é só visual, no front.
4. **`cpf_cnpj` UNIQUE e opcional.** No PostgreSQL, uma coluna UNIQUE aceita vários NULL; então vários clientes podem não ter CPF/CNPJ, mas dois clientes não podem ter o mesmo.
5. **A tarja "Pedido em aberto" não é coluna.** Ela é calculada a partir dos pedidos do cliente (ver seção 7). Se fosse guardada, poderia ficar desatualizada.

---

### 5.3 TAG e CLIENTE_TAG

**TAG — depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_tag | bigint identity | PK |
| nome | varchar(50) | NOT NULL; índice único em `LOWER(nome)` |

**CLIENTE_TAG — depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_cliente | bigint | FK → cliente, `ON DELETE CASCADE` |
| id_tag | bigint | FK → tag, `ON DELETE CASCADE` |
| | | **PK composta (id_cliente, id_tag)** |

**O que é uma tag no sistema:** uma etiqueta de interesse do cliente (ex.: "Moda Festa", "Linho", "Estofaria"), prevista no RF04 do documento. Ela permite filtrar clientes por interesse e enviar WhatsApp só para quem se interessa por determinado tecido (RF05).

**Alterações e motivos:**

1. **PK composta em CLIENTE_TAG.** Sem ela, o mesmo cliente poderia receber a mesma tag duas vezes.
2. **Nome único ignorando maiúsculas/minúsculas** (`CREATE UNIQUE INDEX ... ON tag (LOWER(nome))`). Um `UNIQUE` comum deixaria existir "Linho" e "linho" ao mesmo tempo.
3. **`ON DELETE CASCADE` na CLIENTE_TAG.** Ao apagar uma tag, as ligações dela com os clientes somem junto; os clientes continuam intactos.
4. **Relacionamento USUARIO → TAG removido do diagrama.** Havia uma linha ligando as duas tabelas, mas nenhuma coluna FK correspondente. Tags são globais.

**Como foi decidido:**

- **Tags são criadas na hora, pelo próprio usuário**, no modal do cliente. Motivo: se chegar um tecido novo que um cliente gostou e não existir tag para ele, uma lista fixa deixaria o usuário travado. Antes de criar, o sistema verifica se a tag já existe (ignorando maiúsculas) e reaproveita.
- O **seed** do Flyway cria algumas tags de exemplo (ex.: Moda Festa, Linho, Estofaria, Malha, Denim) para o sistema não começar vazio.
- **Tags podem ser apagadas** por um "x" ao lado de cada tag no filtro da tela Clientes, com confirmação ("A tag Denim está em 3 clientes e será removida deles"). Diferente de cliente e tecido, a tag não tem histórico importante, por isso pode ser apagada de verdade.

---

### 5.4 OPORTUNIDADE → NEGOCIACAO

**Antes:** `id_oportunidade, id_cliente, id_usuario, titulo, estagio, valor_estimado, data_fechamento`

**Depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_negociacao | bigint identity | PK |
| id_cliente | bigint | NOT NULL, FK → cliente |
| id_usuario | bigint | NOT NULL, FK → usuario |
| descricao | varchar(200) | NOT NULL |
| estagio | varchar(30) | NOT NULL, CHECK (ver abaixo) |
| data_criacao | timestamp | NOT NULL, default now() |
| data_ultima_movimentacao | timestamp | NOT NULL, default now() |
| data_fechamento | timestamp | NULL |

```sql
CHECK (estagio IN ('PRIMEIRO_CONTATO', 'MOSTRUARIO_ENVIADO',
                   'AGUARDANDO_RESPOSTA', 'FECHADA', 'PERDIDA'))
```

**Alterações e motivos:**

1. **Tabela renomeada para `negociacao`.** A equipe, o mockup ("Nova negociação") e o próprio usuário usam o termo "negociação". Com o mesmo nome no banco, no código (`Negociacao`, `NegociacaoService`), na API (`/negociacoes`) e na tela, ninguém precisa traduzir um termo para o outro.
2. **`valor_estimado` removido.** No caso dele, o funil serve para **acompanhar os clientes e medir a conversão** (quantas negociações foram ganhas, perdidas e estão em andamento), e isso se faz com contagens. Nas primeiras etapas (primeiro contato, mostruário enviado) ele ainda não sabe metragem nem tecido, então o valor seria um chute e geraria dados errados no dashboard. O valor real já existe no PEDIDO. O mockup também proíbe valor no funil. Se for necessário no futuro, basta uma migration `ADD COLUMN valor_estimado numeric(10,2)` (nula), sem quebrar nada.
3. **`titulo` → `descricao`**, que é o nome usado no mockup ("descrição curta").
4. **Etapas fixas com `CHECK`.** As cinco etapas já foram definidas pela equipe e não são editáveis. As cores de cada etapa ficam só no front.
5. **Novas colunas de data:**
   - `data_criacao`: quando a negociação começou;
   - `data_ultima_movimentacao`: o card do mockup mostra "data do último movimento"; atualizada a cada mudança de etapa;
   - `data_fechamento`: preenchida quando a negociação vai para FECHADA ou PERDIDA; nula enquanto está aberta.

   Com essas três datas dá para responder, sem valor nenhum, perguntas como "quantas negociações fechei em setembro?" e "quanto tempo, em média, uma negociação leva para fechar?".

**Decisões já existentes que o modelo respeita:**

- Um cliente pode ter **várias** negociações ao longo do tempo (cada card do funil é uma negociação separada).
- Os cards são movidos **à mão** (arrastando), sem automação.
- Não há campo de temperatura (frio/morno/quente).

---

### 5.5 INTERACAO (nova)

| Coluna | Tipo | Restrições |
|---|---|---|
| id_interacao | bigint identity | PK |
| id_cliente | bigint | NOT NULL, FK → cliente |
| id_usuario | bigint | NOT NULL, FK → usuario |
| tipo | varchar(20) | NOT NULL, CHECK (ver abaixo) |
| data_interacao | date | NOT NULL |
| descricao | text | NOT NULL |
| criado_em | timestamp | NOT NULL, default now() |

```sql
CHECK (tipo IN ('WHATSAPP', 'LIGACAO', 'VISITA', 'OUTRO'))
```

**Por que a tabela foi criada:** a equipe já tinha decidido registrar as interações com o cliente, e o mockup tem a aba **Interações** na ficha do cliente (lista de anotações + botão "Registrar interação"), mas o modelo original não tinha tabela para isso.

**O que é:** o registro do **passado**, ou seja, o que aconteceu numa conversa (ex.: "Mandei fotos do linho, gostou do bege").

**Decisões:**

1. **Ligada só ao cliente**, não à negociação (decisão anterior da equipe). Ela aparece em um único lugar: a aba Interações da ficha do cliente.
2. **Campo `tipo` incluído** (o mockup tinha só data e texto). A equipe decidiu incluir o canal da conversa para poder filtrar e contar contatos por canal. Valores: WhatsApp (canal principal), Ligação, Visita (ele indo até a loja ou ateliê) e Outro.
3. **Duas datas com papéis diferentes:**
   - `data_interacao`: quando a conversa **aconteceu** (informada pelo usuário; ele pode registrar hoje uma ligação de ontem);
   - `criado_em`: quando foi **digitada** no sistema (automática, para auditoria).
4. Esta tabela também substitui a ideia de registrar mensagens de WhatsApp na antiga NOTIFICACAO_WHATSAPP: uma conversa pelo WhatsApp é uma interação do tipo `WHATSAPP`.

---

### 5.6 FOLLOW_UP (nova)

| Coluna | Tipo | Restrições |
|---|---|---|
| id_follow_up | bigint identity | PK |
| id_cliente | bigint | NOT NULL, FK → cliente |
| id_usuario | bigint | NOT NULL, FK → usuario |
| data_prevista | date | NOT NULL |
| descricao | text | NOT NULL |
| data_conclusao | timestamp | NULL |
| criado_em | timestamp | NOT NULL, default now() |

**Por que a tabela foi criada:** decisão anterior da equipe (follow-ups simples, sem notificação, ligados ao cliente) e o mockup usa follow-ups em dois lugares: a aba **Follow-ups** da ficha e o bloco **"Pendências de hoje"** do dashboard. O modelo original não tinha tabela para isso.

**Diferença entre interação e follow-up:**

| | Interação | Follow-up |
|---|---|---|
| Olha para | o passado | o futuro |
| Responde | "o que aconteceu?" | "o que preciso fazer?" |
| Exemplo | "Mandei fotos do linho" | "Ligar dia 20/10 para confirmar a metragem" |
| Tem "concluir"? | Não | Sim |

**Decisões:**

1. **Sem coluna `concluido` (boolean).** A própria `data_conclusao` indica o estado:
   - `NULL` = pendente;
   - preenchida = concluído (e ainda registra **quando** foi concluído).

   Dois campos dizendo a mesma coisa poderiam ficar contraditórios (ex.: `concluido = false` com data de conclusão preenchida).
2. **"Pendências de hoje"** mostra os follow-ups não concluídos com `data_prevista` **igual ou anterior** a hoje, para que os atrasados não sumam da lista.
3. `data_prevista` é obrigatória (critério de aceitação do documento: "não é possível salvar um follow-up sem data").

---

### 5.7 TECIDO

**Antes:** `id_tecido, nome_tecido, tipo, descricao, composicao, preco_metro`

**Depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_tecido | bigint identity | PK |
| nome | varchar(100) | NOT NULL |
| composicao | varchar(150) | NULL (ex.: "95% Algodão / 5% Elastano") |
| gramatura | integer | NULL — em g/m² |
| largura | numeric(4,2) | NULL — em metros (ex.: 1,50) |
| preco_metro | numeric(10,2) | NOT NULL, CHECK (preco_metro > 0) |
| foto_url | varchar(255) | NULL — caminho da imagem, não a imagem em si |
| ativo | boolean | NOT NULL, default true |
| criado_em | timestamp | NOT NULL, default now() |

**Alterações e motivos:**

1. **Entram `gramatura`, `largura` e `foto_url`.** Pedidos pelo RF01 do documento e pelo modal "Novo Tecido" do mockup. A foto é guardada como caminho/URL; o arquivo fica fora do banco.
2. **Entra `ativo`.** O "excluir" do catálogo, na verdade, **desativa** o tecido: ele sai do catálogo, mas continua nos pedidos já registrados (o mockup pede exatamente esse comportamento). Isso atende à ideia original de "preservar o histórico de tecidos desativados", que não tinha coluna no modelo.
3. **Sai `tipo`.** Seria uma categoria (Plano, Malha...), mas o mockup não usa, com poucos tecidos a busca por nome já resolve, ela se confundiria com as tags dos clientes e, como texto livre, viraria inconsistente. A equipe decidiu remover.
4. **Sai `descricao`.** Não aparece no mockup e o benefício seria pequeno, porque o usuário conhece os tecidos de cabeça. A equipe decidiu remover. Se for necessária no futuro, é uma migration `ADD COLUMN descricao text` (nula), sem impacto.
5. **Sem coluna de unidade.** Foi confirmado que **ele vende tudo por metro**. Por isso o preço é sempre `preco_metro` e o estoque é sempre em metros.
6. **A cor não fica no TECIDO.** O mesmo tecido do catálogo (ex.: Linho Natural, um preço) pode ter várias cores no estoque. A cor fica na tabela ESTOQUE.

---

### 5.8 ITEM_ESTOQUE → ESTOQUE

**Antes:** `id_item_estoque, id_tecido, localizacao, quantidade_metro, data_entrada, status_estoque`

**Depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_estoque | bigint identity | PK |
| id_tecido | bigint | NOT NULL, FK → tecido |
| cor_codigo | varchar(50) | NOT NULL (ex.: "Bege 102") |
| metragem | numeric(10,2) | NOT NULL, CHECK (metragem >= 0) |
| atualizado_em | timestamp | NOT NULL, default now() |
| | | UNIQUE (id_tecido, cor_codigo) |

Exemplo de conteúdo:

| Tecido | cor_codigo | metragem |
|---|---|---|
| Linho Natural | Bege 102 | 85,00 |
| Linho Natural | Cru 100 | 18,00 |
| Tule de Festa | Branco 01 | 140,00 |

**Como foi decidido (controle por total, não por rolo):**
Foi perguntado ao Yan se o pai dele controla **cada rolo separadamente** ou só a **metragem total** de cada tecido e cor. O Yan respondeu que **o controle pela metragem total é o melhor**. Por isso o estoque tem **uma linha por tecido + cor**, guardando o total de metros.

**Alterações e motivos:**

1. **Tabela renomeada para `estoque`.** Com o controle por total, cada linha não é mais um "item" (um rolo), e sim o saldo de um tecido em uma cor.
2. **Entra `cor_codigo`.** A tela de estoque e a tabela de itens do pedido no mockup mostram "Cor/código".
3. **`UNIQUE (id_tecido, cor_codigo)`.** Impede duas linhas para "Linho Natural — Bege 102".
4. **`CHECK (metragem >= 0)`.** Atende à regra "impedir quantidades negativas no estoque" diretamente no banco, sem depender só do Java.
5. **Sai `localizacao`.** O estoque é só dele, e ele sabe onde estão os tecidos.
6. **Sai `data_entrada`.** Com uma linha só por tecido + cor, cada entrada nova **sobrescreveria** a data anterior, e o histórico se perderia. A informação de "quando chegou" foi para a nova tabela ENTRADA_ESTOQUE.
7. **Sai `status_estoque`.** O "estoque baixo" é **calculado** (`metragem < 20`, ver seção 7). Uma coluna de status precisaria ser atualizada a cada venda e poderia ficar desatualizada.
8. **Não existe coluna de número de peças/rolos.** O mockup mostrava "Peças", mas com o controle por metragem total o sistema não sabe quando um rolo acaba; o número dependeria de atualização manual e ficaria errado com o tempo. Como nenhuma regra usa esse número (alerta, venda e baixa usam metros), a equipe decidiu **mostrar só a metragem total**.

---

### 5.9 ENTRADA_ESTOQUE (nova)

| Coluna | Tipo | Restrições |
|---|---|---|
| id_entrada | bigint identity | PK |
| id_estoque | bigint | NOT NULL, FK → estoque |
| metragem | numeric(10,2) | NOT NULL, CHECK (metragem > 0) |
| data_entrada | date | NOT NULL |
| criado_em | timestamp | NOT NULL, default now() |

**Por que a tabela foi criada:**
A ESTOQUE guarda só o **saldo atual**. Sem histórico, não daria para saber "quantos metros entraram no mês passado" nem "quando chegou o último Linho Bege". A equipe avaliou os dois cenários:

| Sem ENTRADA_ESTOQUE | Com ENTRADA_ESTOQUE |
|---|---|
| O "Registrar entrada" só soma um número no saldo | Cada chegada vira um registro com data e metragem |
| Não responde "quanto entrou em setembro?" | Responde com uma consulta simples |
| Erro de digitação (500 m em vez de 50 m) não deixa rastro | O erro pode ser encontrado e corrigido |
| Não há como conferir se o saldo está certo | Saldo auditável (ver abaixo) |
| O sistema faria menos que a planilha antiga | Mantém o que a planilha fazia |

**Custo:** baixo. O mockup já tem o botão e o modal **"Registrar entrada"** (tecido, cor/código, metragem); muda só o acréscimo do campo de data e um INSERT a mais na mesma transação.

**Auditoria do saldo:** com as entradas e os itens de pedido guardados, sempre vale:

> **saldo do ESTOQUE = soma das ENTRADAS − soma dos ITENS vendidos (pedidos não cancelados)**

Se um dia o saldo não bater com essa conta, há um erro, e ele pode ser localizado.

**Decisões:**

1. **Só entradas.** As saídas já estão registradas no ITEM_PEDIDO (com a data do pedido); duplicá-las numa tabela de movimentação seria redundante.
2. **Duas datas com papéis diferentes** (mesma lógica da INTERACAO): `data_entrada` é quando o tecido **chegou de verdade** (informada no modal, com a data de hoje como padrão) e é a usada nos relatórios; `criado_em` é quando foi **digitado** no sistema.
3. **Fica fora do MVP:** valor de compra e controle do que é devido ao sócio (financeiro). A ENTRADA_ESTOQUE já registra quanto ele pegou e quando, o que serve de base para esse controle no futuro.

---

### 5.10 PEDIDO

**Antes:** `id_pedido, id_cliente, id_usuario, data_pedido, status_pedido, total`

**Depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_pedido | bigint identity | PK |
| id_cliente | bigint | NOT NULL, FK → cliente |
| id_usuario | bigint | NOT NULL, FK → usuario |
| id_negociacao | bigint | **NULL**, FK → negociacao (sem UNIQUE) |
| data_pedido | timestamp | NOT NULL, default now() |
| forma_pagamento | varchar(20) | NOT NULL, CHECK (ver abaixo) |
| status | varchar(20) | NOT NULL, CHECK (ver abaixo) |
| total | numeric(10,2) | NOT NULL, CHECK (total >= 0) |

```sql
CHECK (forma_pagamento IN ('A_VISTA', 'CARTAO', 'BOLETO', 'CHEQUE'))
CHECK (status IN ('EM_ABERTO', 'PAGO', 'CANCELADO'))
```

**Alterações e motivos:**

1. **Entra `id_negociacao` (FK, aceita nulo, sem UNIQUE).**
   - O diagrama original tinha uma linha entre PEDIDO e OPORTUNIDADE, mas nenhuma coluna FK.
   - A FK fica no **PEDIDO** porque, quando a negociação é criada, o pedido ainda não existe: é o pedido que "nasce" da negociação.
   - **Aceita nulo** porque o pedido pode existir sem negociação (venda direta: o cliente liga e compra). Essa era uma decisão anterior da equipe.
   - **Sem UNIQUE** porque **uma negociação pode gerar vários pedidos**. Exemplo: o cliente fecha o negócio, mas leva 30 m de linho hoje e mais 15 m daqui a 15 dias, quando tiver dinheiro. São duas compras (dois pedidos, cada um com data, total e pagamento próprios) ligadas ao mesmo negócio.

   | Situação | Como fica no banco |
   |---|---|
   | Cliente compra 3 tecidos de uma vez | 1 PEDIDO + 3 ITEM_PEDIDO |
   | Negociação fechada gera 2 remessas | 2 PEDIDOs com o mesmo `id_negociacao` |
   | Cliente compra direto, sem negociação | 1 PEDIDO com `id_negociacao` nulo |

2. **Entra `forma_pagamento`.** O mockup pede À vista, Cartão, Boleto e Cheque. Não existe desconto automático.
3. **`status_pedido` → `status`, com valores fixos de pagamento:**
   - `EM_ABERTO`: pedido feito, cliente ainda não pagou;
   - `PAGO`: cliente pagou;
   - `CANCELADO`: cliente desistiu ou o pedido foi lançado errado.
4. **`total` continua sendo guardado.** É calculado pelo Java no momento da confirmação (soma de `metragem × preco_metro` dos itens). Depois de confirmado, os itens não mudam, então o total não fica desatualizado, e o Histórico de Vendas soma os totais sem abrir item por item.
5. **O número exibido "#P-0045" não é coluna.** É só o `id_pedido` formatado pelo front.

**Como foi decidido o significado de "em aberto":**
Foi **confirmado pela equipe** que "pedido em aberto" significa **pedido não pago**. Isso é coerente com:

- o negócio: ele aceita cheque e boleto, em que o dinheiro entra depois, e a planilha antiga dele tinha controle de cheques;
- o mockup, que mostra a tarja "Pedido em aberto" e o alerta "Este cliente tem 2 pedidos em aberto — R$ 7.340,00", e que proíbe status de entrega e rastreio;
- o escopo do documento (3.5: alerta "constando se há ou não vendas em aberto").

A tarja serve para ele saber quem ainda deve antes de vender de novo para o mesmo cliente.

**O que ficou fora (para não virar um financeiro completo):** pagamento parcial, parcelas e datas de vencimento.

---

### 5.11 ITEM_PEDIDO

**Antes:** `id_item_pedido, id_pedido, id_item_estoque, quantidade, preco_unitario`

**Depois:**

| Coluna | Tipo | Restrições |
|---|---|---|
| id_item_pedido | bigint identity | PK |
| id_pedido | bigint | NOT NULL, FK → pedido |
| id_estoque | bigint | **NOT NULL**, FK → estoque |
| metragem | numeric(10,2) | NOT NULL, CHECK (metragem > 0) |
| preco_metro | numeric(10,2) | NOT NULL, CHECK (preco_metro > 0) |

**Alterações e motivos:**

1. **`id_item_estoque` → `id_estoque`.** Como cada linha do ESTOQUE é um tecido + cor, o item já sabe qual tecido e qual cor foram vendidos (a coluna "Cor/código" da tabela de itens do mockup vem daqui).
2. **`quantidade` → `metragem`**, deixando claro que a unidade é metro.
3. **`preco_unitario` → `preco_metro`, e continua guardado no item.** O modelo original já acertava aqui: o preço é o **do dia da venda**. Se o Linho custa R$ 42,50 hoje e sobe para R$ 45,00 amanhã, os pedidos antigos continuam com R$ 42,50.
4. **Subtotal não é coluna.** É `metragem × preco_metro`, calculado na hora.
5. **Relacionamento TECIDO → ITEM_PEDIDO removido do diagrama.** Havia uma linha sem coluna FK; o tecido é alcançado pelo estoque.

**Como foi decidido (`id_estoque` obrigatório):**
Foi avaliada uma opção de item "avulso" (vender algo que ainda não está no estoque e dar baixa depois). Foi perguntado ao Yan, e ele confirmou que **o pai dele só vende o que tem em estoque**. Por isso `id_estoque` é **NOT NULL**: todo item vendido sai do estoque, e o `CHECK (metragem >= 0)` da tabela ESTOQUE impede vender mais metros do que existem.

---

### 5.12 NOTIFICACAO_WHATSAPP (removida)

**Motivo:** a equipe decidiu que o contato pelo WhatsApp será feito **somente por link** (`https://wa.me/<telefone>?text=<mensagem>`), montado na hora a partir de `CLIENTE.telefone`.

- O link apenas abre o WhatsApp; o sistema **não tem como saber** se a mensagem foi enviada, entregue ou lida. Colunas como `status_envio` e `data_envio` nunca teriam valores confiáveis.
- O mockup proíbe envio automático, disparo em massa e caixa de entrada de WhatsApp.
- Se o usuário quiser registrar que falou com o cliente, isso é uma **INTERACAO** do tipo `WHATSAPP`.
- A integração pela API oficial do WhatsApp fica como funcionalidade futura.

---

## 6. Regras de negócio que ficam no Java

Algumas regras envolvem mais de uma tabela e precisam ser feitas no service, **sempre dentro de uma transação** (`@Transactional`): ou tudo acontece, ou nada acontece.

| Operação | O que acontece (na mesma transação) |
|---|---|
| **Registrar entrada** | 1) Busca a linha de ESTOQUE do tecido + cor; se não existir, cria com metragem 0. 2) Insere a ENTRADA_ESTOQUE. 3) Soma a metragem no ESTOQUE. |
| **Confirmar pedido** | 1) Insere o PEDIDO e os ITEM_PEDIDO. 2) Desconta a metragem de cada item no ESTOQUE (se faltar, o `CHECK` do banco barra e tudo é desfeito; o Java deve exibir uma mensagem clara, ex.: "Estoque insuficiente: Linho Bege 102 tem 18 m"). 3) Calcula e grava o `total`. 4) Status inicial: `EM_ABERTO` (ou `PAGO`, se pago no ato). |
| **Marcar como pago** | Muda o status de `EM_ABERTO` para `PAGO`. |
| **Cancelar pedido** | 1) Só é permitido se o status **não** for `CANCELADO` (cancelar duas vezes devolveria a metragem em dobro). 2) Devolve ao ESTOQUE a metragem de cada item. 3) Muda o status para `CANCELADO`. Cancelamento não pode ser desfeito; se foi engano, lança-se um pedido novo. O pedido não é apagado. A negociação ligada ao pedido não muda automaticamente. |
| **Mover card no funil** | Atualiza `estagio` e `data_ultima_movimentacao`. Ao ir para `FECHADA` ou `PERDIDA`, preenche `data_fechamento`; ao sair dessas etapas, volta para nulo. Ao soltar em `FECHADA`, o front pergunta "Criar pedido para este cliente?". |
| **Concluir follow-up** | Preenche `data_conclusao` com a data/hora atual. |
| **Criar tag** | Procura tag com o mesmo nome ignorando maiúsculas; se existir, reaproveita; se não, cria. |
| **"Excluir" tecido / cliente** | Marca `ativo = false`. Nunca apagar fisicamente. |

---

## 7. Consultas de referência

**Pedidos em aberto de um cliente** (tarja e alerta da ficha):

```sql
SELECT COUNT(*) AS qtd, COALESCE(SUM(total), 0) AS valor
FROM pedido
WHERE id_cliente = :idCliente AND status = 'EM_ABERTO';
```

**Pendências de hoje** (dashboard):

```sql
SELECT f.*, c.nome
FROM follow_up f
JOIN cliente c ON c.id_cliente = f.id_cliente
WHERE f.data_conclusao IS NULL
  AND f.data_prevista <= CURRENT_DATE
ORDER BY f.data_prevista;
```

**Tecidos com estoque baixo** (dashboard e destaque âmbar na tela Estoque):

```sql
SELECT t.nome, e.cor_codigo, e.metragem
FROM estoque e
JOIN tecido t ON t.id_tecido = e.id_tecido
WHERE t.ativo = true AND e.metragem < 20
ORDER BY e.metragem;
```

**Negociações por etapa** (dashboard):

```sql
SELECT estagio, COUNT(*) FROM negociacao GROUP BY estagio;
```

**Vendas do mês** (dashboard; cancelados não contam):

```sql
SELECT COALESCE(SUM(total), 0)
FROM pedido
WHERE status <> 'CANCELADO'
  AND data_pedido >= date_trunc('month', CURRENT_DATE);
```

**Metros que entraram em um período:**

```sql
SELECT SUM(metragem)
FROM entrada_estoque
WHERE data_entrada BETWEEN '2026-09-01' AND '2026-09-30';
```

**Auditoria do saldo de estoque** (deve retornar zero linhas):

```sql
SELECT e.id_estoque, e.metragem AS saldo_gravado,
       COALESCE(ent.total, 0) - COALESCE(sai.total, 0) AS saldo_calculado
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
```

---

## 8. Impactos fora do banco (front-end e documentação)

### 8.1 Ajustes no front-end (em relação ao mockup)

| Tela / componente | Ajuste |
|---|---|
| Usuários | A tela sai do MVP (cadastro de usuários é funcionalidade futura). |
| Ficha do cliente → modal "Registrar interação" | Acrescentar o campo **tipo** (WhatsApp, Ligação, Visita, Outro). |
| Clientes → filtro de tags | Acrescentar um **"x"** em cada tag para apagá-la, com confirmação. |
| Catálogo → modal "Novo Tecido" | Remover o campo **unidade** (tudo é por metro). |
| Estoque → tabela | Remover a coluna **Peças**. |
| Estoque → modal "Registrar entrada" | Acrescentar o campo **data de entrada** (padrão: hoje). |
| Ficha do cliente → aba Pedidos e/ou Histórico de Vendas | Acrescentar os botões **"Marcar como pago"** e **"Cancelar pedido"**. |
| Novo Pedido | Exibir mensagem clara quando a metragem pedida for maior que o estoque. |

### 8.2 Ajustes no documento do projeto

| Trecho do documento | Ajuste |
|---|---|
| 3.6 Fora do escopo: "Controle de estoque" | Remover: o controle de estoque **faz parte** do sistema (decisão da equipe). |
| RF01: "preço por metro/quilo" | Passa a ser apenas "preço por metro". |
| "Cadastro de usuários do sistema" e "Controle de acesso por perfil de usuário" | Mover para funcionalidades futuras. |
| História 7: "Como gestor, quero cadastrar usuários e vendedores com perfis de acesso" | Mover para funcionalidades futuras. |
| RF07 / escopo: "Envio de notificações" | Ajustar para "envio de mensagens por link do WhatsApp". |
| Diagrama MER/DER | Atualizar com as tabelas e colunas deste documento. |

---

## 9. Pendências e itens fora do MVP

### 9.1 Ainda não discutido

- **Planilhas antigas dele**: romaneio, fechamento semanal, controle de cheques e divisão de 50% do lucro com o sócio. Nenhuma delas está modelada ainda; podem gerar tabelas novas no futuro.

### 9.2 Fora do MVP (funcionalidades futuras)

| Item | Como entraria depois |
|---|---|
| Cadastro de usuários e perfil de administrador | A tabela USUARIO e as FKs já existem; acrescentar coluna `perfil` e `ativo` por migration. |
| Valor estimado da negociação | Migration `ADD COLUMN valor_estimado numeric(10,2)` na NEGOCIACAO. |
| Descrição do tecido | Migration `ADD COLUMN descricao text` no TECIDO. |
| Financeiro com o sócio (valor de compra, pagamentos, saldo devido) | Usa a ENTRADA_ESTOQUE como base; novas tabelas a definir. |
| Pagamento parcial e parcelas | Nova tabela de pagamentos ligada ao PEDIDO. |
| Integração com a API oficial do WhatsApp | Avaliar quando sair do MVP. |
| Venda de itens fora do estoque (encomenda) | Confirmado com o Yan que **não** é necessário hoje. |
