# Connect CRM — Guia de desenvolvimento

Sistema web de CRM para um **revendedor autônomo de tecidos** (LR Representações). Ele atende lojas, ateliês e confecções pelo WhatsApp, vende **por metro**, **só vende o que tem em estoque** e controla **apenas o próprio estoque** (não o do sócio).

Este arquivo reúne as regras técnicas e de negócio que todo código do projeto deve seguir. Fluxo de Git, commits e PR estão no [`CONTRIBUTING.md`](CONTRIBUTING.md); como rodar o projeto está no [`README.md`](README.md).

---

## 1. Fontes da verdade

| Assunto | Onde está | Quando consultar |
|---|---|---|
| Banco de dados | `backend/src/main/resources/db/migration/` (V1 em diante) | antes de criar ou alterar entidade, repository, DTO ou consulta |
| Requisitos e regras (RF, RNF, RD) | ERS do Connect CRM (Google Docs da equipe) | quando a issue citar um RF/RD ou envolver regra que não está aqui |
| Decisões de modelagem | `docs/connect-crm-revisao-modelagem-banco.md` | quando precisar do motivo de uma decisão |
| Plano de milestones e issues | `docs/connect-crm-roadmap.md` | para saber a ordem e as dependências das issues |
| Git, commits, labels e PR | `CONTRIBUTING.md` | antes de criar branch, commit ou PR |

As migrations são a **fonte da verdade do banco**: nomes de tabelas, colunas, tipos e restrições saem delas, exatamente como estão. Migration aplicada **nunca é editada**; toda mudança de banco entra numa nova (`V3__...`, `V4__...`).

---

## 2. Como trabalhar uma issue

1. Ler a issue inteira. O escopo do trabalho é o que ela pede.
2. Conferir nas migrations as tabelas envolvidas e usar os nomes e tipos exatos.
3. Alterar só os arquivos da issue. Outros módulos pertencem a outras issues e mexer neles gera conflito.
4. Quando a issue pedir algo que não está nas migrations, na ERS ou neste arquivo, ou que contradiga uma regra daqui, **confirmar com a equipe antes de implementar**. Decisão de modelagem e de escopo é da equipe.
5. Testar (teste automatizado e/ou passo a passo manual) e registrar no PR como testar.

A issue está pronta quando cumpre a definição de pronto do `CONTRIBUTING.md`.

---

## 3. Stack e versões

| Camada | Tecnologia | Versão |
|---|---|---|
| Back-end | Java + Spring Boot (API REST), Maven Wrapper | Java 21 · Spring Boot 4.1 |
| Front-end | React + Vite, React Router, ícones `lucide-react` | Node 24 |
| Banco | PostgreSQL (Docker Compose) | 17 |
| Migrations | Flyway | gerenciado pelo Spring Boot |
| Segurança | Spring Security, senha com BCrypt | — |
| CI | GitHub Actions (`.github/workflows/ci.yml`) | — |

Monorepo: `backend/`, `frontend/`, `database/`, `docs/`, `.github/`.

Pacote base do back-end: **`br.com.lrrepresentacoes.connectcrm`**.

---

## 4. Ambiente e configuração

- O banco local sobe com `docker compose up -d` em `database/`. O container usa o fuso **`America/Sao_Paulo`** (`TZ` e `PGTZ`).
- As credenciais ficam em `database/.env` (fora do Git). O modelo é o `database/.env.example`.
- O `backend/src/main/resources/application.yaml` **lê o mesmo `database/.env`**. Nunca colocar senha, usuário ou URL fixos no código.
- `spring.jpa.hibernate.ddl-auto: validate`: o Hibernate **não cria nem altera tabelas**. Quem cria é o Flyway.
- Datas do Hibernate no fuso `America/Sao_Paulo`, igual ao banco.

---

## 5. Modelo de dados

### 5.1 Tabelas

| Tabela | Papel |
|---|---|
| `usuario` | Quem usa o sistema. No MVP existe **um único usuário**, sem perfis de acesso, criado pela migration V2 |
| `cliente` | Loja, ateliê ou confecção. Entidade central do CRM |
| `tag` / `cliente_tag` | Etiquetas de interesse do cliente (N:N) |
| `interacao` | Conversa **já realizada** com o cliente |
| `follow_up` | Próxima ação **a fazer**, com data |
| `negociacao` | Card do funil de vendas |
| `tecido` | Catálogo (preço por metro). A **cor fica no estoque** |
| `estoque` | Saldo em metros de um tecido **em uma cor** |
| `entrada_estoque` | Histórico de cada chegada de tecido |
| `pedido` / `item_pedido` | Venda e seus itens (cada item aponta para uma linha de estoque) |

### 5.2 Convenções Java

| Banco | Java |
|---|---|
| `bigint GENERATED ALWAYS AS IDENTITY` | `Long` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `numeric(10,2)` (dinheiro, metragem) | `BigDecimal` (nunca `double`) |
| `timestamp` | `LocalDateTime` |
| `date` | `LocalDate` |
| colunas com valores fixos | `enum` + `@Enumerated(EnumType.STRING)` |

- A entidade do funil se chama **`negociacao`** em todas as camadas: tabela, classe `Negociacao`, `NegociacaoService`, rota `/negociacoes`, telas. Nunca "oportunidade".
- Toda tabela de negócio com `id_usuario` recebe o usuário logado.

### 5.3 Enums

Gravados em MAIÚSCULO, sem acento. O front converte para o texto de exibição ("Aguardando resposta", "Ligação").

| Coluna | Enum | Valores |
|---|---|---|
| `negociacao.estagio` | `EstagioNegociacao` | `PRIMEIRO_CONTATO`, `MOSTRUARIO_ENVIADO`, `AGUARDANDO_RESPOSTA`, `FECHADA`, `PERDIDA` |
| `interacao.tipo` | `TipoInteracao` | `WHATSAPP`, `LIGACAO`, `VISITA`, `OUTRO` |
| `pedido.forma_pagamento` | `FormaPagamento` | `A_VISTA`, `CARTAO`, `BOLETO`, `CHEQUE` |
| `pedido.status` | `StatusPedido` | `EM_ABERTO`, `PAGO`, `CANCELADO` |

---

## 6. Regras de negócio

O banco garante as restrições de dados (únicos, valores válidos, estoque nunca negativo). As regras abaixo ficam **no service**.

### 6.1 Operações transacionais

Cada operação roda num método `@Transactional`: tudo é gravado, ou nada é gravado.

| Operação | Passos |
|---|---|
| **Confirmar pedido** | 1) Cliente e tecidos precisam estar ativos. 2) Se o pedido vier de uma negociação, o cliente do pedido é o cliente da negociação; outro cliente → recusar. 3) Validar o saldo de cada item; saldo insuficiente → mensagem clara, ex.: *"Estoque insuficiente: Linho Bege 102 tem 18 m"*. 4) Gravar pedido e itens. 5) Descontar a metragem de cada item no estoque, tratando concorrência (dois pedidos ao mesmo tempo não podem vender o mesmo metro). 6) Calcular e gravar o `total`. 7) Status inicial `EM_ABERTO`, ou `PAGO` se pago no ato. |
| **Cancelar pedido** | 1) Aceitar só pedido com status diferente de `CANCELADO`. 2) Devolver a metragem de cada item ao estoque. 3) Status → `CANCELADO`. O cancelamento é definitivo e o pedido permanece no banco. |
| **Registrar entrada de estoque** | 1) Tecido desativado → recusar, pedindo para reativar antes. 2) Buscar a linha de estoque do tecido + cor; se não existir, criar com metragem 0. 3) Gravar a `entrada_estoque`. 4) Somar a metragem ao saldo. |

### 6.2 Pedido

- `total` = soma de `metragem × preco_metro` dos itens, calculado pelo sistema.
- O item guarda o `preco_metro` do tecido **no momento da venda**. Pedidos antigos mantêm o preço da época.
- Cada tecido + cor aparece **uma vez** por pedido. Ao adicionar um item que já está na lista, somar a metragem na linha existente.
- **"Pedido em aberto" = pedido não pago** (`status = EM_ABERTO`). A tarja e o alerta da ficha do cliente são calculados por consulta.
- Pedidos `CANCELADO` ficam fora de vendas do mês, histórico de vendas e da tarja "Pedido em aberto".
- `id_negociacao` é opcional (vazio = venda direta), e uma negociação pode originar vários pedidos.

### 6.3 Funil de negociações

- Etapas fixas, na ordem do enum. Os cards são movidos manualmente.
- Toda mudança de etapa atualiza `data_ultima_movimentacao`.
- Entrar em `FECHADA` ou `PERDIDA` preenche `data_fechamento`; sair dessas etapas volta para `null`.
- Ao mover para `FECHADA`, o front pergunta "Criar pedido para este cliente?" e abre o Novo Pedido com o cliente da negociação já definido.
- Não se cria negociação para cliente desativado.

### 6.4 Clientes, tags, interações e follow-ups

- Telefone gravado só com dígitos e DDI 55 (ex.: `5562999998888`); a máscara é visual. WhatsApp: `https://wa.me/<telefone>?text=<mensagem codificada>`, sem acrescentar nada ao telefone.
- Tags são criadas na hora, no cadastro do cliente. Antes de criar, buscar a tag pelo nome sem diferenciar maiúsculas e reaproveitar a existente. A exclusão da tag remove os vínculos com os clientes e pede confirmação informando em quantos clientes ela está.
- Interação: registra tipo, data em que ocorreu e descrição; pertence a um cliente.
- Follow-up: concluir = preencher `data_conclusao` com a data/hora atual. **Pendências de hoje** = `data_conclusao IS NULL AND data_prevista <= hoje` **e cliente ativo**.

### 6.5 Registros inativos

Cliente e tecido nunca são apagados: são **desativados** (`ativo = false`), preservando o histórico. As listagens mostram só os ativos, com opção de exibir os inativos para reativar.

| Situação | Regra |
|---|---|
| Pedido ou negociação para cliente desativado | bloqueado |
| Vender estoque de tecido desativado | bloqueado |
| Tela Estoque | não mostra linhas de tecido desativado |
| Desativar cliente com pedido `EM_ABERTO` ou negociação em andamento (estágio diferente de `FECHADA`/`PERDIDA`) | bloqueado, com mensagem que diz o que resolver |
| Follow-ups pendentes de cliente desativado | não bloqueiam a desativação; saem de "Pendências de hoje" |
| Reativar cliente ou tecido | permitido (RF-023 e RF-024) |

Essas validações ficam no service, porque dependem de outras tabelas.

### 6.6 Estoque e catálogo

- Todo estoque é em **metros**. O preço do tecido é `preco_metro`.
- Uma linha de estoque por tecido + cor. Venda só do que existe em estoque.
- **Estoque baixo** = `metragem < 20`, calculado por consulta.
- Não existe número de peças ou rolos.

### 6.7 Segurança

- Senha gravada com `passwordEncoder.encode()` e conferida com `passwordEncoder.matches()`.
- Recuperação de senha responde sempre *"Se o e-mail estiver cadastrado, enviaremos o link"*, para qualquer e-mail informado.

---

## 7. Front-end

### 7.1 Estrutura

| Pasta | Conteúdo |
|---|---|
| `frontend/src/pages/` | uma tela por arquivo (`Clientes.jsx`, `Estoque.jsx`...) |
| `frontend/src/components/` | componentes reutilizáveis (layout, menu lateral, modais) |
| `frontend/src/routes/` | rotas do React Router |
| `frontend/src/services/` | chamadas à API (quando a API existir) |
| `frontend/src/mocks/` | dados de exemplo usados enquanto a API não está pronta |

### 7.2 Padrões de tela

- Textos e mensagens em **português do Brasil**.
- Dinheiro `R$ 34,00`, datas `dd/mm/aaaa`, número do pedido `#P-0000`, metragem em `m`.
- Contador com singular e plural ("1 cliente", "5 clientes").
- Erro de validação **abaixo do campo**.
- Botões de ação visíveis sem depender de hover.
- Layout principal para 1440 px; tabelas rolam na horizontal em telas menores.
- Dados mockados seguem o formato da API: telefone com DDI 55, enums em MAIÚSCULO (convertidos para texto na tela).
- Navegação sem a tela de Usuários.

### 7.3 Ajustes de tela já decididos (em relação ao protótipo)

- Modal **Registrar interação**: campos tipo, data e texto.
- Filtro de tags da tela Clientes: um **"x"** em cada tag para excluí-la.
- Modal **Novo Tecido**: foto, nome, composição, gramatura, largura e preço por metro (só nome e preço obrigatórios).
- Tela **Estoque**: tecido, cor/código e metragem; modal **Registrar entrada** com tecido, cor, metragem e data (padrão: hoje, no fuso local).
- Pedidos: botões **Marcar como pago** e **Cancelar pedido**.

---

## 8. Qualidade

- **Back-end:** toda regra de negócio do service tem teste unitário (JUnit 5 + Mockito). Fluxos transacionais também têm teste de integração com o banco.
- **Front-end:** `npm run lint` e `npm run build` sem erros antes do PR.
- **CI:** todo PR roda build, testes e lint no GitHub Actions. PR com CI vermelho não é mergeado.
- Comentários explicam a **regra de negócio** ou o motivo de uma decisão, não o óbvio.

---

## 9. Escopo do MVP

O MVP cobre: login e recuperação de senha, clientes e tags, ficha do cliente, interações, follow-ups, funil, catálogo, estoque com entradas, pedidos (pagar e cancelar), WhatsApp por link, dashboard, histórico de vendas e reativação de cliente e tecido.

Ficam para versões futuras: cadastro de usuários e perfis de acesso, log de acesso, integração com a API do WhatsApp, valor estimado de negociação, pagamento parcial e parcelas, controle financeiro com o sócio, exportação de relatórios. Se uma issue parecer pedir um desses itens, confirmar com a equipe antes de implementar.

---

## 10. Banco local para desenvolvimento

- `docker compose up -d` em `database/` sobe o PostgreSQL vazio.
- Ao iniciar o back-end, o Flyway aplica as migrations: **V1** cria as 12 tabelas e **V2** cria o usuário inicial e as tags de exemplo.
- O usuário inicial da V2 tem uma senha **só de desenvolvimento**, que deve ser trocada antes de o sistema entrar em uso.
- Para recriar o banco do zero: `docker compose down -v` e `docker compose up -d` (apaga os dados locais).
