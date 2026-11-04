# Plano de testes — Connect CRM

Casos de teste que comprovam os requisitos do MVP. Cada caso aponta para um requisito (RF, RD ou RNF), para que a equipe consiga mostrar o caminho **requisito → teste → código**.

Issue: [#35](https://github.com/connect-crm-lr/connect-crm/issues/35) · Execução e resultado: issue 5.5 (relatório de execução) · Regras de negócio: [AGENTS.md](../AGENTS.md)

> [!IMPORTANT]
> **De onde vêm os requisitos:** a ERS fica no Google Docs da equipe e não está no repositório. Os códigos RF/RD/RNF deste plano são os que o [roadmap](connect-crm-roadmap.md) liga a cada issue. Os resultados esperados seguem as regras do `AGENTS.md`. Antes da execução, quem cuida da ERS deve conferir a seção [Pendências](#9-pendências) (principalmente a divisão de RD-003 a RD-009).

---

## 1. Objetivo e metas

| Meta | Origem | Como medir |
|---|---|---|
| **≥ 90% dos casos aprovados** | TAP, critérios de sucesso | casos aprovados ÷ casos executados, no relatório da issue 5.5 |
| **Satisfação > 80%** nos testes de aceitação | TAP e RNF-001 | questionário SUS da issue 5.3 (nota SUS > 80) |
| Todo RF e RD do MVP com **pelo menos 1 caso** | issue #35 | matriz da seção 5 |
| Toda regra de negócio do back com **teste do service** | CONTRIBUTING (definição de pronto) e AGENTS 8 | casos do tipo **U** |

## 2. Escopo

**Entra:** tudo o que está no escopo do MVP (AGENTS 9): login e recuperação de senha, clientes e tags, ficha do cliente, interações, follow-ups, funil, catálogo, estoque com entradas, pedidos (pagar e cancelar), WhatsApp por link, dashboard, histórico de vendas, reativação de cliente e tecido, backup e padrão de erros da API.

**Não entra:** cadastro de usuários e perfis, log de acesso, API do WhatsApp, valor estimado de negociação, pagamento parcial, controle com o sócio e exportação de relatórios (fora do MVP pelo AGENTS 9).

## 3. Tipos de teste

| Sigla | Tipo | Ferramenta | Onde fica | Quem roda |
|---|---|---|---|---|
| **U** | Unidade do service (regra de negócio, repository mockado) | JUnit 5 + Mockito | `backend/src/test/.../service/` | CI em todo PR |
| **C** | Controller / API (status HTTP e formato do erro) | MockMvc | `backend/src/test/.../controller/` | CI em todo PR |
| **I** | Integração com o banco real (transação, concorrência, constraints) | `@SpringBootTest` + PostgreSQL | `backend/src/test/` | CI em todo PR |
| **M** | Manual de tela (passo a passo no navegador) | roteiro deste plano | — | autor do PR e revisor |
| **A** | Aceitação com o usuário real | roteiro + SUS | issue 5.3 | Yan e Maria Eduarda |

Os tipos **U**, **C** e **I** são **automatizados** (rodam no CI). Os tipos **M** e **A** são **manuais**.

## 4. Ambiente e dados

- **Banco:** PostgreSQL 17 no Docker (`database/`). No CI, o serviço PostgreSQL do workflow.
- **Dados:** perfil `dev` (`backend/src/main/resources/db/dev/V2_1__dados_teste.sql`). Ele tem clientes, tecidos, estoque abaixo e acima de 20 m, pedidos `EM_ABERTO`, `PAGO` e `CANCELADO`.
- **Navegador:** Chrome, Edge e Firefox atualizados, em 1440 px de largura.
- **Usuário:** o usuário inicial da migration V2.
- **"Hoje":** os casos com data usam a data do dia da execução, no fuso `America/Sao_Paulo`.

### Convenção dos casos

- **ID:** `CT-<requisito>-<nº>`, ex.: `CT-RD008-01` (padrão do roadmap, seção 6).
- **Status:** `existente` = já há teste no repositório (nome do método na coluna *Passos*); `planejado` = entra junto com a issue indicada.
- O teste automatizado deve citar o ID no `@DisplayName` ou num comentário, para que a matriz possa ser conferida.

---

## 5. Matriz requisito → casos

| Requisito | Assunto (pelo roadmap) | Issue | Casos |
|---|---|---|---|
| RF-001 | Login | 4.2.3, 4.2.4 | CT-RF001-01 a 05 |
| RF-002 | Recuperar senha (plano B) | 4.2.5 | CT-RF002-01, 02 |
| RF-003, RF-004 | Clientes: cadastrar, listar, buscar, filtrar por tag, editar | 4.3.1, 4.3.3 | CT-RF003-01 a 06, CT-RF004-01 a 03 |
| RF-005 | Tags | 4.3.2, 4.3.3 | CT-RF005-01 a 06 |
| RF-006 | Ficha do cliente e alerta de pedido em aberto | 4.3.4, 4.5.2 | CT-RF006-01 a 03 |
| RF-007 | Funil de negociações | 4.6.3, 4.6.4 | CT-RF007-01 a 03 |
| RF-008, RF-017, RF-018 | Pedidos: confirmar, pagar e cancelar | 4.5.1, 4.5.2 | CT-RF008-01 a 04, CT-RF017-01, CT-RF018-01 |
| RF-009, RF-010 | Estoque: consulta e registrar entrada | 4.4.4, 4.4.5 | CT-RF009-01 a 03, CT-RF010-01 a 02 |
| RF-011, RF-012 | Interações e follow-ups | 4.6.1, 4.6.2 | CT-RF011-01 a 02, CT-RF012-01 a 03 |
| RF-013 a RF-016 | Catálogo de tecidos | 4.4.1 a 4.4.3 | CT-RF013-01 a 03, CT-RF014-01, CT-RF015-01, CT-RF016-01 |
| RF-019 | WhatsApp por link | 4.3.4 | CT-RF019-01 a 03 |
| RF-020 | Desativar cliente (com bloqueio) | 4.3.1 | CT-RF020-01 a 03 |
| RF-021 | Dashboard | 4.7.1 | CT-RF021-01 a 04 |
| RF-022 | Histórico de vendas | 4.7.2 | CT-RF022-01 a 03 |
| RF-023 | Reativar tecido | 4.4.1 | CT-RF023-01, 02 |
| RF-024 | Reativar cliente | 4.3.1 | CT-RF024-01, 02 |
| RD-003 a RD-009 | Regras do pedido | 4.5.1 | CT-RD003a009-01 a 07, CT-RD008-01 |
| RD-004, RD-012, RD-013 | Regras do estoque | 4.4.4 | CT-RD004-01, CT-RD012-01, CT-RD013-01 |
| RD-010 | Datas do funil | 4.6.3 | CT-RD010-01 a 03 |
| RD-011 | Pendências de hoje | 4.6.1 | CT-RD011-01 a 03 |
| RD-014, RD-016 | Regras do cliente | 4.3.1 | CT-RD014-01, CT-RD016-01 |
| RD-015 | Tag reaproveitada sem diferenciar maiúsculas | 4.3.2 | CT-RD015-01, 02 |
| RNF-001 | Usabilidade | 3.5, 5.3 | CT-RNF001-01, 02 |
| RNF-003 | Segurança (senha com hash) | 4.2.3 | CT-RNF003-01, 02 |
| RNF-004 | Backup e restauração | 4.2.6 | CT-RNF004-01 a 03 |
| RNF-006 | Padrão de erros da API e CORS | 4.2.1 | CT-RNF006-01 a 05 |

---

## 6. Casos de teste

### 6.1 Acesso (RF-001, RF-002, RNF-003)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF001-01 | C | planejado | `POST` de login com credenciais válidas | e-mail e senha do usuário da V2 | 200 com o token/sessão |
| CT-RF001-02 | C | planejado | `POST` de login com senha errada | senha incorreta | 401, mensagem que não diz se o erro foi no e-mail ou na senha |
| CT-RF001-03 | C | planejado | chamar um endpoint protegido sem autenticação | — | 401 |
| CT-RF001-04 | M | planejado | entrar pela tela de login | credenciais válidas | abre o Dashboard |
| CT-RF001-05 | M | planejado | abrir `/clientes` direto na barra de endereço, sem login | — | volta para `/login` |
| CT-RF002-01 | M | planejado | "Esqueci minha senha" com e-mail cadastrado | e-mail da V2 | *"Se o e-mail estiver cadastrado, enviaremos o link"* |
| CT-RF002-02 | M | planejado | "Esqueci minha senha" com e-mail não cadastrado | `naoexiste@teste.com` | **a mesma** mensagem do CT-RF002-01 |
| CT-RNF003-01 | I | planejado | ler a coluna `senha_hash` do usuário | — | valor começa com `$2a$` (BCrypt), nunca a senha em texto |
| CT-RNF003-02 | U | planejado | autenticar usando `passwordEncoder.matches()` | senha correta e incorreta | `true` e `false` |

### 6.2 Clientes e tags (RF-003, RF-004, RF-005, RF-020, RF-024, RD-014, RD-015, RD-016)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF003-01 | U | planejado | cadastrar cliente | nome + telefone válidos | cliente salvo, `ativo = true` |
| CT-RF003-02 | C | planejado | cadastrar sem nome | nome vazio | 400 com o erro no campo `nome` |
| CT-RF003-03 | U | planejado | cadastrar com telefone com máscara | `(62) 99999-8888` | gravado só com dígitos e DDI: `5562999998888` |
| CT-RF003-04 | U | planejado | buscar por parte do nome | `bella` | retorna "Ateliê Bella Noiva" (sem diferenciar maiúsculas) |
| CT-RF003-05 | U | planejado | editar cliente | novo telefone | alteração gravada |
| CT-RF003-06 | M | planejado | contador da tela Clientes com 1 e com 5 clientes | — | "1 cliente" / "5 clientes" |
| CT-RF004-01 | U | planejado | filtrar por tag | tag "Linho" | só clientes ativos com a tag |
| CT-RF004-02 | M | planejado | escolher **Todos** no filtro de tag | — | lista todos os clientes ativos |
| CT-RF004-03 | U | planejado | listar clientes | 1 cliente desativado na base | o desativado **não** aparece |
| CT-RF005-01 | U | **existente** | `TagServiceTest.criar_quandoNomeNaoExiste_salvaNovaTag` | "Malha" | tag nova salva |
| CT-RF005-02 | U | **existente** | `TagServiceTest.criar_removeEspacosDoInicioEDoFimDoNome` | `"  Linho  "` | salva "Linho" |
| CT-RF005-03 | U | **existente** | `TagServiceTest.consultarUso_informaEmQuantosClientesATagEsta` | tag usada em 3 clientes | informa 3 |
| CT-RF005-04 | U | **existente** | `TagServiceTest.excluir_quandoTagExiste_apagaATag` | tag existente | tag apagada |
| CT-RF005-05 | C | **existente** | `TagControllerTest.excluir_tagInexistente_responde404ComMensagemClara` | id inexistente | 404 com mensagem clara |
| CT-RF005-06 | M | planejado | clicar no "x" da tag no filtro | tag em 2 clientes | confirmação mostra "2 clientes"; após confirmar, a tag sai dos clientes |
| CT-RD015-01 | U | **existente** | `TagServiceTest.criar_quandoNomeJaExisteComOutrasMaiusculas_reaproveitaATagExistente` | "moda festa" com "Moda Festa" já existente | devolve a tag existente, sem criar outra |
| CT-RD015-02 | C | **existente** | `TagControllerTest.criar_comNomeVazio_responde400ComOErroDoCampo` | nome vazio | 400 com o erro do campo |
| CT-RF020-01 | U | planejado | desativar cliente sem pendências | cliente sem pedido em aberto | `ativo = false`, histórico preservado |
| CT-RF020-02 | U | planejado | desativar cliente com pedido `EM_ABERTO` | — | recusado, mensagem diz o que resolver |
| CT-RF020-03 | U | planejado | desativar cliente com negociação fora de `FECHADA`/`PERDIDA` | — | recusado, mensagem diz o que resolver |
| CT-RF024-01 | U | planejado | reativar cliente desativado | — | `ativo = true`, volta a aparecer na lista |
| CT-RF024-02 | M | planejado | marcar "mostrar inativos" na tela Clientes | — | inativos aparecem com a opção de reativar |
| CT-RD014-01 | U | planejado | criar pedido ou negociação para cliente desativado | — | recusado |
| CT-RD016-01 | U | planejado | desativar cliente com follow-up pendente | — | permitido; o follow-up sai de "Pendências de hoje" |

### 6.3 Ficha do cliente e WhatsApp (RF-006, RF-019)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF006-01 | M | planejado | abrir a ficha pelo cartão do cliente | — | aba **Dados** com e-mail, CPF/CNPJ, cidade/UF e tags |
| CT-RF006-02 | I | planejado | consultar cliente com pedido `EM_ABERTO` | — | indicador "pedido em aberto" = verdadeiro |
| CT-RF006-03 | I | planejado | consultar cliente com pedido só `CANCELADO` | — | indicador = falso (cancelado não conta) |
| CT-RF019-01 | M | planejado | botão WhatsApp sem filtro de tag | cliente `5562999998888` | abre `https://wa.me/5562999998888?text=...` com *"Olá <nome>, como podemos ajudar hoje?"* |
| CT-RF019-02 | M | planejado | botão WhatsApp com a tag "Linho" filtrada | — | mensagem *"...acabou de chegar um novo mostruário de Linho, posso agendar uma visita?"* |
| CT-RF019-03 | M | planejado | nome com acento e "&" | "Malhas Oliveira & Filhos" | texto chega certo no WhatsApp (mensagem codificada na URL) |

### 6.4 Catálogo de tecidos (RF-013 a RF-016, RF-023)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF013-01 | U | planejado | cadastrar tecido só com nome e preço | "Viscose", 29,90 | salvo (demais campos opcionais) |
| CT-RF013-02 | C | planejado | cadastrar sem preço | preço vazio | 400 com o erro no campo |
| CT-RF013-03 | C | planejado | cadastrar com preço negativo | -1 | recusado |
| CT-RF014-01 | M | planejado | abrir o Catálogo | — | cards com nome, composição, gramatura, largura e preço `R$ 42,50/m` |
| CT-RF015-01 | U | planejado | buscar por composição | "linho" | retorna os tecidos com "Linho" na composição |
| CT-RF016-01 | U | planejado | alterar o preço de um tecido | 42,50 → 45,00 | novo preço gravado; pedidos antigos mantêm o preço antigo (ver CT-RD003a009-04) |
| CT-RF023-01 | U | planejado | desativar e reativar tecido | — | some do catálogo e volta ao reativar |
| CT-RF023-02 | M | planejado | "Excluir" no card do tecido | — | pede confirmação e o tecido é desativado, não apagado |

### 6.5 Estoque (RF-009, RF-010, RD-004, RD-012, RD-013)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF009-01 | U | planejado | consultar estoque | base do perfil `dev` | uma linha por tecido + cor, em metros |
| CT-RF009-02 | U | planejado | linha com 19,99 m e linha com 20,00 m | — | 19,99 → estoque baixo; 20,00 → **não** (limite `< 20`) |
| CT-RF009-03 | U | planejado | consultar com tecido desativado | — | linhas desse tecido não aparecem |
| CT-RF010-01 | I | planejado | registrar entrada de cor já existente | +50 m em linha com 15 m | saldo 65 m e 1 registro novo em `entrada_estoque` |
| CT-RF010-02 | M | planejado | abrir o modal "Registrar entrada" | — | data de entrada vem preenchida com hoje |
| CT-RD004-01 | I | planejado | registrar entrada de cor nova | tecido + cor inexistentes | cria a linha (começa em 0) e soma: saldo = metragem da entrada |
| CT-RD012-01 | U | planejado | registrar entrada para tecido desativado | — | recusado, pedindo para reativar o tecido |
| CT-RD013-01 | I | planejado | forçar saldo negativo direto no banco | `UPDATE` com metragem -1 | o banco recusa (constraint) |

### 6.6 Pedidos (RF-008, RF-017, RF-018, RD-003 a RD-009)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF008-01 | I | planejado | confirmar pedido com estoque suficiente | 10 m de Linho Natural (linha com 15 m) | pedido `EM_ABERTO`, estoque 5 m |
| CT-RF008-02 | U | planejado | calcular total | 10 m × 42,50 + 2,5 m × 28,90 | total = 497,25 (soma de `metragem × preco_metro`) |
| CT-RF008-03 | U | planejado | confirmar pedido "pago no ato" | — | status inicial `PAGO` |
| CT-RF008-04 | M | planejado | tela Novo Pedido | 2 itens | subtotal e total ao vivo; número no formato `#P-0000` |
| CT-RF017-01 | U | planejado | marcar pedido `EM_ABERTO` como pago | — | status `PAGO`; o cliente perde a tarja "Pedido em aberto" |
| CT-RF018-01 | I | planejado | cancelar pedido `EM_ABERTO` | pedido de 10 m | status `CANCELADO`, 10 m devolvidos ao estoque, pedido continua no banco |
| CT-RD008-01 | U | planejado | cancelar o mesmo pedido duas vezes | pedido já `CANCELADO` | 2ª tentativa recusada (422), estoque não muda |
| CT-RD003a009-01 | U | planejado | confirmar com estoque insuficiente | 20 m numa linha com 18 m | recusado com *"Estoque insuficiente: <tecido> <cor> tem 18 m"*; nada gravado |
| CT-RD003a009-02 | I | planejado | 2º item falha no meio da confirmação | item 1 ok, item 2 sem saldo | **nada** gravado: nem pedido, nem baixa do item 1 (`@Transactional`) |
| CT-RD003a009-03 | I | planejado | **concorrência:** dois pedidos ao mesmo tempo pelo último metro | 2 threads, 1 m cada, saldo 1 m | só 1 confirma; saldo final 0, nunca negativo |
| CT-RD003a009-04 | U | planejado | preço histórico | mudar o preço do tecido depois do pedido | o item mantém o `preco_metro` da venda |
| CT-RD003a009-05 | U | planejado | adicionar o mesmo tecido + cor duas vezes | 5 m + 3 m | uma linha com 8 m |
| CT-RD003a009-06 | U | planejado | pedido vindo de negociação com outro cliente | negociação do cliente A, pedido do B | recusado |
| CT-RD003a009-07 | U | planejado | vender tecido desativado ou para cliente desativado | — | recusado |

### 6.7 Interações e follow-ups (RF-011, RF-012, RD-011)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF011-01 | U | planejado | registrar interação | tipo `WHATSAPP`, data, descrição | salva no cliente |
| CT-RF011-02 | C | planejado | registrar com tipo inválido | `EMAIL` | 400 (aceita só `WHATSAPP`, `LIGACAO`, `VISITA`, `OUTRO`) |
| CT-RF012-01 | U | planejado | criar follow-up | data prevista + descrição | salvo com `data_conclusao` vazia |
| CT-RF012-02 | C | planejado | criar follow-up sem data | data vazia | 400 |
| CT-RF012-03 | U | planejado | concluir follow-up | — | `data_conclusao` = data/hora atual |
| CT-RD011-01 | U | planejado | pendências com follow-ups de ontem, hoje e amanhã | 3 follow-ups não concluídos | aparecem ontem e hoje; amanhã não |
| CT-RD011-02 | U | planejado | pendências com follow-up já concluído | — | não aparece |
| CT-RD011-03 | U | planejado | pendências com follow-up de cliente desativado | — | não aparece |

### 6.8 Funil (RF-007, RD-010)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF007-01 | U | planejado | criar negociação | cliente ativo | estágio `PRIMEIRO_CONTATO` |
| CT-RF007-02 | M | planejado | arrastar card para outra coluna | — | card muda de etapa e continua lá ao recarregar |
| CT-RF007-03 | M | planejado | soltar card em **Fechada** e responder OK | — | pergunta "Criar pedido para este cliente?" e abre o Novo Pedido com o cliente da negociação |
| CT-RD010-01 | U | planejado | mover entre etapas abertas | `PRIMEIRO_CONTATO` → `MOSTRUARIO_ENVIADO` | `data_ultima_movimentacao` atualizada; `data_fechamento` vazia |
| CT-RD010-02 | U | planejado | mover para `FECHADA` ou `PERDIDA` | — | `data_fechamento` preenchida |
| CT-RD010-03 | U | planejado | tirar de `FECHADA` | `FECHADA` → `AGUARDANDO_RESPOSTA` | `data_fechamento` volta para `null` |

### 6.9 Dashboard e histórico (RF-021, RF-022)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RF021-01 | I | planejado | vendas do mês | pedidos `PAGO`, `EM_ABERTO` e `CANCELADO` no mês | soma sem o `CANCELADO` |
| CT-RF021-02 | I | planejado | negociações por etapa | base do perfil `dev` | contagem igual à do banco |
| CT-RF021-03 | I | planejado | pendências de hoje | ver CT-RD011 | mesma regra, só clientes ativos |
| CT-RF021-04 | I | planejado | estoque baixo | linhas com 15 m e 245 m | só a de 15 m |
| CT-RF022-01 | I | planejado | histórico por período | início e fim do mês | só pedidos do período |
| CT-RF022-02 | I | planejado | total do período com cancelado | 1 cancelado no período | total sem o cancelado |
| CT-RF022-03 | M | planejado | tela Histórico de Vendas | — | número `#P-0000`, valores `R$ 0,00`, datas `dd/mm/aaaa` |

### 6.10 Requisitos não funcionais (RNF-001, RNF-004, RNF-006)

| ID | Tipo | Status | Passos | Entrada | Resultado esperado |
|---|---|---|---|---|---|
| CT-RNF001-01 | M | planejado | avaliação heurística de Nielsen (issue 3.5) | telas do MVP | lista de problemas com gravidade; os graves corrigidos |
| CT-RNF001-02 | A | planejado | teste com o usuário real + SUS (issue 5.3) | roteiro de tarefas | nota SUS > 80 |
| CT-RNF004-01 | M | **existente** | `bash database/scripts/backup.sh` | banco com dados | arquivo `.sql` com data e hora em `database/backups/` |
| CT-RNF004-02 | M | **existente** | backup → `docker compose down -v` → restaurar | backup do passo anterior | contagens das tabelas iguais antes e depois |
| CT-RNF004-03 | M | **existente** | restaurar com o back-end conectado | — | restauração recusada, mostrando a conexão aberta |
| CT-RNF006-01 | U | **existente** | `GlobalExceptionHandlerTest.validacao_devolve400ComOErroDeCadaCampo` | — | 400 com a lista de campos |
| CT-RNF006-02 | U | **existente** | `GlobalExceptionHandlerTest.recursoNaoEncontrado_devolve404ComAMensagemDoService` | — | 404 |
| CT-RNF006-03 | U | **existente** | `GlobalExceptionHandlerTest.regraNegocio_devolve422ComAMensagemDaRegra` | — | 422 |
| CT-RNF006-04 | U | **existente** | `GlobalExceptionHandlerTest.integridade_devolve409SemExporODetalheDoBanco` | — | 409 sem detalhe do banco |
| CT-RNF006-05 | U | **existente** | `GlobalExceptionHandlerTest.erroInesperado_devolve500ComMensagemGenerica` | — | 500 com mensagem genérica |

---

## 7. Resumo

| | U | C | I | M | A | Total |
|---|---|---|---|---|---|---|
| Casos | 50 | 10 | 16 | 23 | 1 | **100** |
| Já existentes | 10 | 2 | 0 | 3 | 0 | **15** |

Os automatizados (U + C + I) somam 76 casos. Os manuais (M + A), 24.

## 8. Execução e registro

1. **Automatizados:** rodam no CI a cada PR. A issue que implementa o módulo cria os testes dos casos `planejado` dela e troca o status para `existente`, com o nome do método.
2. **Manuais:** o autor do PR executa os casos **M** do módulo e registra no *Como testar* do PR. A rodada completa acontece antes do code freeze (23/11).
3. **Resultado:** cada caso recebe *aprovado*, *reprovado* ou *bloqueado*, com data e quem executou. Esses resultados alimentam o relatório da issue 5.5.
4. **Defeito encontrado:** abrir uma issue com o ID do caso no título (ex.: `[CT-RD008-01] cancelar duas vezes devolve o estoque duas vezes`) e a label `manutenção: corretiva`.
5. **Critério de saída da v1.0.0:** ≥ 90% dos casos executados aprovados e nenhum reprovado em RD de pedido ou estoque (ver seção 1).

## 9. Pendências

- **Conferir com a ERS:** o roadmap agrupa as regras do pedido como "RD-003 a 009". Só a RD-008 tem o texto citado (*só cancela pedido não cancelado*). Por isso os casos dessas regras usam o ID de grupo `CT-RD003a009-xx`. Quem cuida da ERS deve trocar cada um pelo RD exato.
- **RD-001 e RD-002** não aparecem no roadmap. Se forem do MVP, precisam de casos.
- **RF-002:** o plano segue o plano B decidido em 03/11 (só a tela, sem envio de e-mail).
- **Prazo dos casos de login:** dependem da decisão JWT ou sessão (issue 4.2.3). Os casos CT-RF001-01 a 03 já valem para as duas opções.
