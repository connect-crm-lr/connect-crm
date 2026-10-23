---
titulo: Connect CRM — Roadmap de Milestones e Issues
projeto: Connect CRM (LR Representações)
versao: 1.0
atualizado: 2026-10-20
status: proposta para validação da equipe
tags: [connect-crm, roadmap, milestones, issues, contexto-ia]
---

# Connect CRM — Roadmap de Milestones e Issues

> [!info] Para que serve este arquivo
> É o **plano de desenvolvimento** do Connect CRM até a apresentação do PI (08/12/2026): quais milestones existem, quais issues entram em cada uma, em que ordem, quem faz e como cada entrega vira evidência para as matérias do semestre.
>
> Serve para **duas coisas**:
> 1. **Equipe:** saber o que vem a seguir e o que depende do quê.
> 2. **IA de cada integrante:** colar junto com o `AGENTS.md` como contexto, antes de pedir ajuda numa issue.

> [!warning] Validação pendente
> As datas e os responsáveis são **sugestões**. Quem confirma é a **Maria Eduarda** (gerente do projeto) junto com a equipe.

---

## Sumário

1. [[#1. Visão geral]]
2. [[#2. Equipe e papéis]]
3. [[#3. Regras de trabalho]]
4. [[#4. Cronograma das milestones]]
5. [[#5. Milestones e issues]]
6. [[#6. Trilha de testes]]
7. [[#7. Trilha de gestão e documentação]]
8. [[#8. Evidências por matéria]]
9. [[#9. Decisões pendentes]]
10. [[#10. Riscos e plano B]]
11. [[#11. Uso do Obsidian]]
12. [[#12. Como usar este arquivo com IA]]

---

## 1. Visão geral

**Sistema:** CRM web para um revendedor autônomo de tecidos (LR Representações). Ele atende lojas, ateliês e confecções pelo WhatsApp, **vende por metro**, **só vende o que tem em estoque** e controla **apenas o estoque dele** (não o do sócio).

**Stack:** Java 21 + Spring Boot 4.1 (API REST) · React (Vite) · PostgreSQL 17 (Docker Compose) · Flyway · Spring Security + BCrypt.

**Repositório:** org `connect-crm-lr`, repo `connect-crm` (monorepo: `backend/`, `frontend/`, `database/`, `docs/`, `.github/`).

**Datas-chave:**

| Data | Marco |
|---|---|
| 24/10/2026 | fim da base do repositório (`v0.1.0`) |
| **23/11/2026** | **code freeze**: MVP pronto, nenhuma funcionalidade nova depois disso |
| 24/11 → 07/12 | **14 dias** de estudo do sistema, correções e preparação da apresentação |
| **08/12/2026** | **apresentação final do PI** |

**Documentos relacionados (fontes da verdade):**

| Assunto | Documento |
|---|---|
| Regras para IA e padrões de código | `AGENTS.md` |
| Requisitos (RF, RNF, RD) | `ERS - Connect CRM` |
| Banco de dados | `backend/src/main/resources/db/migration/V1__criar_tabelas.sql` (após o merge do PR #9) |
| Motivo das decisões de modelagem | `connect-crm-revisao-modelagem-banco.md` |
| Padrões de Git e PR | `CONTRIBUTING.md` (issue 4.1.6) |

---

## 2. Equipe e papéis

| Integrante | Papel principal | Foco no desenvolvimento |
|---|---|---|
| **Maria Eduarda Martins Passos** | Gerente do projeto / PO | documentação, TAP/EAP, ERS, Plano de Manutenção, apoio no front |
| **Gabriel Carvalho de Oliveira** | Desenvolvedor back-end / revisor dos PRs | API, segurança, CI, padrões do repositório |
| **João Pedro Abdala** | Banco de dados / back-end | migrations, consultas, regras transacionais |
| **Yan Santos** | Desenvolvedor front-end | telas React, integração com a API, contato com o cliente (pai dele) |

---

## 3. Regras de trabalho

### 3.1 Princípios do roadmap

1. **Uma milestone = um módulo de ponta a ponta** (banco → API → tela), que o usuário consegue testar.
2. **Back e front em issues separadas** do mesmo módulo, rodando em paralelo. A issue de back publica primeiro o **contrato da API** (rotas e JSON de entrada e saída), e o front usa dados mockados até a API ficar pronta.
3. **Issues pequenas** (1 a 3 dias). PR pequeno é fácil de revisar.
4. **Ordem por dependência:** uma issue só começa quando o que ela precisa já está na `develop`.
5. **Estudo contínuo:** o relatório de cada PR vai para o Obsidian **no dia do merge** (ver [[#11. Uso do Obsidian]]).

### 3.2 Padrão das issues

- **Título:** `[código EAP] Módulo — o que faz`. Exemplo: `[4.3.1] Clientes — API de cadastro e listagem`.
- **Corpo:** Objetivo · Tarefas (checklist) · Critérios de aceitação · Fora do escopo · Dependências.
- **Lateral:** responsável, label `prioridade: alta/média/baixa`, milestone.

### 3.3 Fluxo de Git (Git Flow)

1. Atualizar a `develop` (`git checkout develop` e `git pull`).
2. Criar a branch da issue a partir dela: `tipo/descricao-curta` (ex.: `feat/clientes-api`, `chore/esqueleto-backend`). *Padrão em uso; oficializar no CONTRIBUTING.*
3. Commits pequenos em Conventional Commits: `feat(clientes): adiciona cadastro de cliente`.
4. PR **sempre para a `develop`**, com o template preenchido e `Closes #número`.
5. 1 aprovação de outro integrante → merge com **merge commit** → apagar a branch.
6. Fechou a milestone: merge `develop` → `main` + tag (`v0.2.0`) + CHANGELOG.
7. No code freeze (23/11): criar `release/v1.0.0`. Dali em diante, só correções.

### 3.4 Definição de pronto (DoD)

Uma issue só fecha quando:

- [ ] o código compila e a aplicação sobe;
- [ ] **issue de back:** tem teste unitário do service cobrindo as regras de negócio;
- [ ] `mvnw test` passa (e o CI fica verde, depois da issue 4.1.8);
- [ ] o PR foi aprovado por outro integrante;
- [ ] o autor consegue **explicar cada classe** que criou;
- [ ] o **relatório do PR** está no Obsidian.

---

## 4. Cronograma das milestones

As datas seguem o **peso** (quantidade de trabalho) e a **complexidade** (conceitos novos e risco) de cada milestone. Pontuação das issues: 1 simples · 2 média · 3 grande · 5 complexa ou com conceito novo.

| Milestone | Período | Dias | Pontos | Complexidade | Prazo |
|---|---|---|---|---|---|
| `v0.1.0 — Base do repositório` | 21/10 → 24/10 | 4 | 9 | 🟢 baixa | **sáb 24/10** |
| `v0.2.0 — Acesso e clientes` | 25/10 → 05/11 | 12 | 22 | 🔴 alta | **qui 05/11** |
| `v0.3.0 — Catálogo e estoque` | 06/11 → 12/11 | 7 | 15 | 🟡 média | **qui 12/11** |
| `v0.4.0 — Vendas e relacionamento` | 13/11 → 19/11 | 7 | 13 | 🔴 alta | **qui 19/11** |
| `v1.0.0 — MVP` (code freeze) | 20/11 → 23/11 | 4 | 6 | 🟢 baixa | **seg 23/11** |
| **Estudo e apresentação** | 24/11 → 07/12 | 14 | — | — | apresentação 08/12 |

**Por que essa distribuição:**

- **v0.2.0 tem mais dias** porque concentra os conceitos novos (Spring Security) e o **primeiro CRUD**, que define o padrão de todos os outros. Inclui o feriado de Finados (02/11).
- **v0.3.0 é mais curta** porque repete o padrão do CRUD. Também recebe, **adiantadas**, as APIs de interações, follow-ups e funil, que só dependem de clientes.
- **v0.4.0** tem poucas issues, mas a regra de negócio mais crítica (pedidos). A API de pedidos começa no primeiro dia.
- **v1.0.0** é leve porque os testes são feitos **dentro de cada issue**, e não no fim. Tem o feriado da Consciência Negra (20/11).

---

## 5. Milestones e issues

Legenda dos lados: **back** · **front** · **banco** · **doc** · **equipe**.

### 5.1 `v0.1.0 — Base do repositório` (até 24/10)

> [!summary] Entrega
> Ambiente igual para os 4 integrantes, esqueletos do back e do front rodando, banco criado pelo Flyway, padrões do repositório documentados e CI verificando cada PR.

| Código | Issue | Lado | Responsável | Depende de | Situação |
|---|---|---|---|---|---|
| 4.1.1 | Configurar PostgreSQL via Docker Compose (#2) | banco | Gabriel | — | ✔ fechada |
| 4.1.2 | Esqueleto do back-end com Spring Boot (#3) | back | Gabriel | 4.1.1 | PR aberto |
| 4.1.3 | Migrations Flyway V1 (tabelas) e V2 (seed) (#8) | banco | João | 4.1.2 | PR #9 com ajustes pedidos |
| 4.1.4 | Esqueleto do front-end com React (Vite) | front | Yan | — | em andamento |
| 4.1.5 | Layout, componentes base e telas mockadas | front | Yan | 4.1.4 | em andamento |
| 4.1.6 | CONTRIBUTING.md: branches, commits, labels, PR, uso de IA | doc | Gabriel | — | a criar |
| 4.1.7 | README, CHANGELOG e .gitattributes da raiz | doc | Maria Eduarda | — | a criar |
| 4.1.8 | CI no GitHub Actions: build + testes do back com PostgreSQL; build do front | back | Gabriel ou João | 4.1.2, 4.1.4 | a criar |
| 4.1.9 | Atualizar AGENTS.md: seção front-end, decisões de 19/10, V1 como fonte da verdade do banco | doc | Gabriel | 4.1.3 | a criar |

**Fechamento:** merge na `main` + tag `v0.1.0`.

### 5.2 `v0.2.0 — Acesso e clientes` (25/10 → 05/11)

> [!summary] Entrega
> O revendedor faz login, cadastra e encontra clientes, organiza por tags, vê a ficha e chama no WhatsApp com um clique.

> [!important] Antes de começar
> Decidir **JWT ou sessão** para o login e o **escopo do RF-002** (recuperação real por e-mail ou só a tela). Ver [[#9. Decisões pendentes]].

| Código | Issue | Lado | Responsável | Depende de | RF / RD |
|---|---|---|---|---|---|
| 4.2.1 | Padrão da API: tratamento global de erros (`@RestControllerAdvice`), formato da resposta de erro, CORS para o front | back | Gabriel | v0.1.0 | RNF-006 |
| 4.2.2 | Dados de teste em perfil de desenvolvimento (não vão para produção) | banco | João | 4.1.3 | — |
| 4.2.3 | Autenticação: Spring Security + endpoint de login + BCrypt | back | Gabriel | 4.2.1 | RF-001, RNF-003 |
| 4.2.4 | Tela de login integrada + rotas protegidas | front | Yan | 4.2.3 | RF-001 |
| 4.2.5 | Recuperar senha (escopo a decidir) | back + front | a definir | 4.2.3 | RF-002 |
| 4.2.6 | Rotina de backup do banco (`pg_dump` no container) + instrução de restauração | banco | João | 4.1.1 | RNF-004 |
| 4.3.1 | Clientes — API: cadastrar, listar, buscar, filtrar por tag, editar, desativar (com bloqueio) e reativar | back | Gabriel | 4.2.1 | RF-003, 004, 020, 024 · RD-014, 016 |
| 4.3.2 | Tags — API: criar reaproveitando a existente (sem diferenciar maiúsculas), listar, excluir informando em quantos clientes está | back | João | 4.2.1 | RF-005 · RD-015 |
| 4.3.3 | Clientes — telas: lista, busca, filtro por tag com "Todos", contador "1 cliente / 5 clientes", cadastro com tags, "x" para excluir tag, mostrar inativos | front | Yan | 4.3.1, 4.3.2 | RF-003, 004, 005, 024 |
| 4.3.4 | Ficha do cliente (aba Dados) + botão WhatsApp (`wa.me`) | front | Maria Eduarda ou Yan | 4.3.1 | RF-006 (parcial), RF-019 |
| 3.5 | Avaliação heurística (Nielsen) das telas mockadas, com lista de ajustes | doc | Maria Eduarda | 4.1.5 | RNF-001 |
| 5.0 | Plano de testes: casos de teste ligados aos RF e RD | doc | a definir | — | todos |

**Regras importantes desta milestone:**

- Cliente desativado não aparece nas listagens, e o histórico é preservado.
- **Desativar cliente é bloqueado** enquanto houver pedido `EM_ABERTO` ou negociação em andamento. A mensagem diz o que resolver.
- Telefone gravado só com dígitos e DDI 55 (ex.: `5562999998888`); a máscara é só visual.
- Senha sempre com `passwordEncoder.encode()` / `matches()`.

### 5.3 `v0.3.0 — Catálogo e estoque` (06/11 → 12/11)

> [!summary] Entrega
> O revendedor mantém o catálogo de tecidos com foto, vê o estoque por tecido e cor (com alerta abaixo de 20 m) e registra as entradas de tecido.

> [!important] Antes de começar
> Decidir onde guardar a **foto do tecido** (pasta do servidor ou serviço externo).

| Código | Issue | Lado | Responsável | Depende de | RF / RD |
|---|---|---|---|---|---|
| 4.4.1 | Tecidos — API: cadastrar, listar, buscar, alterar, desativar e reativar | back | Gabriel | v0.2.0 | RF-013, 014, 015, 016, 023 |
| 4.4.2 | Upload da foto do tecido | back | Gabriel | 4.4.1 | RF-013 |
| 4.4.3 | Catálogo — telas: cards, busca, modal novo/editar (preço `R$ 34,00`), excluir com confirmação, mostrar inativos | front | Yan | 4.4.1 | RF-013–016, 023 |
| 4.4.4 | Estoque — API: consulta por tecido e cor, estoque baixo (< 20 m), registrar entrada (`@Transactional`) | back | João | 4.4.1 | RF-009, 010 · RD-004, 012, 013 |
| 4.4.5 | Estoque — tela + modal "Registrar entrada" (data padrão: hoje) | front | Maria Eduarda ou Yan | 4.4.4 | RF-009, 010 |
| 4.6.1 | **(adiantada)** Interações e follow-ups — API (concluir follow-up) | back | João | v0.2.0 | RF-011, 012 · RD-011 |
| 4.6.3 | **(adiantada)** Funil — API: criar negociação, mover etapa (datas de movimentação e fechamento) | back | Gabriel | v0.2.0 | RF-007 · RD-010 |

**Regras importantes desta milestone:**

- Tecido desativado sai do catálogo, dos selects e da tela de estoque, mas continua nos pedidos antigos.
- Registrar entrada de tecido desativado: **não reativa sozinho**; a mensagem pede para reativar antes.
- Entrada de estoque: busca a linha tecido + cor (cria com 0 se não existir), grava a entrada e soma o saldo, tudo na mesma transação.

### 5.4 `v0.4.0 — Vendas e relacionamento` (13/11 → 19/11)

> [!summary] Entrega
> O revendedor lança pedidos com baixa automática de estoque, marca como pago ou cancela, acompanha as negociações no kanban e registra interações e follow-ups na ficha do cliente.

| Código | Issue | Lado | Responsável | Depende de | RF / RD |
|---|---|---|---|---|---|
| 4.5.1 | Pedidos — API: confirmar (transacional, baixa de estoque, total, preço histórico, **tratamento de concorrência no estoque**), marcar como pago, cancelar (devolve estoque) | back | Gabriel + João | v0.3.0 | RF-008, 017, 018 · RD-003 a 009 |
| 4.5.2 | Novo pedido — tela (itens, subtotal e total ao vivo, `#P-0000`) + ações pagar/cancelar + aba Pedidos e alerta "em aberto" na ficha | front | Yan | 4.5.1 | RF-006, 008, 017, 018 |
| 4.6.2 | Abas Interações e Follow-ups da ficha | front | Maria Eduarda | 4.6.1 | RF-011, 012 |
| 4.6.4 | Funil — kanban com arrastar + pergunta "Criar pedido para este cliente?" ao soltar em Fechada | front | Yan | 4.6.3, 4.5.2 | RF-007 |

**Regras importantes desta milestone:**

- Pedido não pode ser criado para cliente desativado nem com tecido desativado.
- Pedido vindo de negociação tem obrigatoriamente o mesmo cliente dela.
- Estoque insuficiente: mensagem clara, ex.: *"Estoque insuficiente: Linho Bege 102 tem 18 m"*.
- Mesmo tecido + cor uma vez por pedido: adicionar de novo soma a metragem.
- Cancelar só o que não está cancelado; o pedido nunca é apagado.
- **Concorrência:** dois pedidos ao mesmo tempo para o último metro não podem passar os dois (transação + bloqueio da linha de estoque).

### 5.5 `v1.0.0 — MVP` (20/11 → 23/11, code freeze)

> [!summary] Entrega
> Dashboard e histórico de vendas prontos, sistema testado pelo usuário real e branch `release/v1.0.0` criada.

| Código | Issue | Lado | Responsável | Depende de | RF |
|---|---|---|---|---|---|
| 4.7.1 | Dashboard — API e tela: vendas do mês, negociações por etapa, pendências de hoje (só clientes ativos), estoque baixo | back + front | Gabriel + Yan | v0.4.0 | RF-021 |
| 4.7.2 | Histórico de vendas por período, com total sem os cancelados | back + front | João + Maria Eduarda | 4.5.1 | RF-022 |
| 5.3 | Teste de aceitação e usabilidade com o pai do Yan (roteiro + questionário SUS) — **sugestão: dom 22/11** | equipe | Yan + Maria Eduarda | 4.7.1 | RNF-001 |
| 5.5 | Relatório de execução dos testes (meta da TAP: ≥ 90% aprovados) | doc | a definir | 5.3 | — |

### 5.6 Estudo e apresentação (24/11 → 07/12)

| Código | Item | Responsável |
|---|---|---|
| 5.4 | Correções do teste de aceitação (só branches `hotfix/`, nada de funcionalidade nova) | equipe |
| — | **Estudo do código:** cada um apresenta para os outros o módulo que fez, usando os relatórios dos PRs no Obsidian | equipe |
| 6.3 | Manual do usuário + README final | Maria Eduarda |
| — | Release: merge `release/v1.0.0` → `main`, tag `v1.0.0`, CHANGELOG | Gabriel |
| — | Seminário: antes/depois, histórico do repositório, demonstração ao vivo | equipe |

---

## 6. Trilha de testes

| Quando | O quê | Ferramenta | Tipo |
|---|---|---|---|
| v0.1.0 | CI rodando `mvnw test` em cada PR | GitHub Actions | regressão automatizada |
| v0.2.0 | **Plano de testes** (5.0): casos ligados aos RF/RD, ex.: `CT-RD008-01 — cancelar pedido já cancelado retorna erro` | documento | planejamento |
| toda issue de back | testes unitários do service (regras de negócio, repository mockado) | JUnit 5 + Mockito | unidade |
| v0.2.0 em diante | testes de integração: API + banco real (ex.: confirmar pedido desconta o estoque de verdade) | `@SpringBootTest` + PostgreSQL | integração |
| cada milestone | coleção de requisições da API, para testar e repetir | Postman ou Insomnia | API / sistema |
| v0.4.0 | fluxos transacionais e casos-limite (estoque insuficiente, cancelar duas vezes, último metro) | JUnit | caixa-branca / valor-limite |
| v1.0.0 | aceitação e usabilidade com o usuário real (meta > 80% de satisfação) | roteiro + SUS | aceitação |
| v1.0.0 | relatório de execução (meta ≥ 90% aprovados) | documento | evidência |

> [!tip] Rastreabilidade
> Cada **RD** da ERS vira pelo menos um caso de teste. Exemplo: RD-008 (*só cancela pedido não cancelado*) → teste que tenta cancelar duas vezes e espera erro. Isso mostra **requisito → teste → código**.

---

## 7. Trilha de gestão e documentação

| Item | Quando | Responsável | Motivo |
|---|---|---|---|
| Atualizar TAP (marcos com as datas deste roadmap) e EAP (fase 4 por módulo, com os códigos usados aqui) | até 24/10 | Maria Eduarda | marcos vencidos; alinhar EAP ↔ GitHub |
| Alinhar a ERS com as decisões de 19/10 (inativos, RF-023/024, UNIQUE do item do pedido, "pago no ato") | até 31/10 | Maria Eduarda | ERS desatualizada |
| Plano de Manutenção (mín. 3 páginas: cronograma, responsabilidades, critérios de priorização) | 27/10 → 17/11 | Maria Eduarda + Gabriel | Manutenção e Configuração |
| Relatório de auditoria de configuração (funcional e física) | 18/11 → 28/11 | Gabriel | Manutenção e Configuração |

---

## 8. Evidências por matéria

O mesmo projeto gera evidência para todas as matérias do semestre:

| Matéria | O que mostrar | Onde está |
|---|---|---|
| **Manutenção e Configuração de Software** | Git Flow, Conventional Commits, labels de manutenção (ISO/IEC 14764), template de PR, tags SemVer, CHANGELOG, CI, Docker, Plano de Manutenção, auditoria | todas as milestones + seção 7 |
| **Teste de Software** | plano de testes, unitários, integração, coleção da API, aceitação, relatório de execução | seção 6 |
| **Interface Homem-Computador** | protótipo no Figma, avaliação heurística (3.5), teste de usabilidade com SUS (5.3), acessibilidade (contraste, rótulos, sem hover) | v0.2.0 e v1.0.0 |
| **Desenvolvimento Web** | React consumindo API REST, autenticação, CORS, tratamento de erros HTTP | v0.2.0 em diante |
| **Sistemas Distribuídos** | arquitetura em 3 camadas (front, API, banco) em containers, concorrência no estoque (4.5.1), autenticação *stateless* (se JWT) | v0.1.0, v0.2.0, v0.4.0 |
| **Gerenciamento de Banco de Dados** | modelagem, constraints, índices, migrations, transações (ACID), backup (4.2.6), consultas do dashboard com `EXPLAIN` | v0.1.0, v0.2.0, v1.0.0 |

---

## 9. Decisões pendentes

| Decisão | Trava | Prazo |
|---|---|---|
| Login com **JWT ou sessão** | 4.2.3, 4.2.4 | **24/10** |
| Escopo do **RF-002** (e-mail real ou só a tela) | 4.2.5 | 24/10 |
| Padrão oficial do **nome das branches** | CONTRIBUTING (4.1.6) | 24/10 |
| **Prioridade** do RF-023 e RF-024 (reativar tecido e cliente) | 4.3.1, 4.4.1 | 24/10 |
| Onde guardar a **foto do tecido** | 4.4.2 | 05/11 |
| **Senha real** do usuário inicial (hoje `Trocar@123`, só para desenvolvimento) | uso real | antes da aceitação |
| Data do **teste com o pai do Yan** | 5.3 | agendar já |

---

## 10. Riscos e plano B

O cronograma é apertado: 34 dias de desenvolvimento para 4 pessoas. Em toda milestone, um **ponto de controle no meio**: se estiver mais de 2 dias atrasado, a equipe escolhe o que simplificar. **Nada é cortado sem consulta à equipe.**

| Se apertar | Simplificação possível |
|---|---|
| Kanban com arrastar (4.6.4) | botões "mover para a próxima etapa" |
| Recuperar senha (4.2.5) | só a tela com a mensagem padrão |
| Upload de foto (4.4.2) | campo com a URL da imagem |
| Reativação (RF-023/024) | depois da apresentação |

**Outros riscos:**

- Decisão de autenticação atrasar → trava a milestone mais pesada.
- Docker nas máquinas da faculdade → os dados podem sumir; as migrations recriam tudo.
- Agenda do pai do Yan → o teste de aceitação depende dele, não da equipe.

---

## 11. Uso do Obsidian

O Obsidian é a **base de estudo da equipe**: cada PR vira uma nota explicando o que foi feito, para que nos 14 dias finais todos revisem o sistema inteiro.

### 11.1 Estrutura do cofre (vault)

```
Connect CRM/
├── 00 - Contexto/
│   ├── Connect CRM — Roadmap.md        ← este arquivo
│   ├── AGENTS.md
│   ├── ERS - resumo.md
│   └── Decisões.md                     ← registro de decisões com data
├── 01 - Milestones/
│   ├── v0.1.0 — Base do repositório.md
│   ├── v0.2.0 — Acesso e clientes.md
│   └── ...
├── 02 - Relatórios de PR/
│   ├── [4.1.2] Esqueleto do back-end.md
│   ├── [4.1.3] Migrations Flyway.md
│   └── ...
├── 03 - Módulos/                        ← visão de estudo por módulo
│   ├── Clientes e tags.md
│   ├── Catálogo e estoque.md
│   ├── Pedidos.md
│   └── Funil e relacionamento.md
├── 04 - Matérias/                       ← evidências por matéria
│   ├── Manutenção e Configuração.md
│   ├── Teste de Software.md
│   └── ...
└── 05 - Modelos/
    └── Modelo — Relatório de PR.md
```

### 11.2 Modelo de relatório de PR

Copie para `05 - Modelos/Modelo — Relatório de PR.md`. Cada autor preenche **no dia do merge**, com as próprias palavras:

```markdown
---
issue: "#"
pr: "#"
codigo-eap: "4.x.x"
milestone: "v0.x.0"
autor: ""
revisor: ""
data-merge: AAAA-MM-DD
modulo: ""
materias: []
tags: [connect-crm, relatorio-pr]
---

# [4.x.x] Título da issue

## O que foi feito
(2 a 4 frases: o problema e a solução)

## Como funciona
(o fluxo, do clique na tela até o banco, ou da requisição até a resposta)

## Classes e métodos principais
| Classe | Camada | Responsabilidade |
|---|---|---|
| | controller / service / repository / entity / dto | |

## Regras de negócio aplicadas
- RF-___ / RD-___: como o código garante

## Como testar
1.
2.

## O que eu aprendi
(conceitos novos: ex.: @Transactional, JWT, useEffect)

## Perguntas que a banca pode fazer
- Por que ...?

## Ligações
Milestone: [[v0.x.0 — ...]] · Módulo: [[...]]
```

### 11.3 Boas práticas no cofre

- Use **links internos** (`[[Pedidos]]`, `[[v0.4.0 — Vendas e relacionamento]]`) para ligar relatórios, módulos e milestones. O **Graph View** mostra o sistema inteiro conectado.
- Use **tags** para filtrar: `#modulo/clientes`, `#materia/testes`, `#conceito/transacao`.
- Registre toda decisão da equipe em `Decisões.md`, com data e motivo.
- O cofre pode ficar numa pasta sincronizada (Google Drive, OneDrive) para os quatro acessarem. **Não** coloque senhas nem o `.env` no Obsidian.

---

## 12. Como usar este arquivo com IA

Cada integrante usa IA para codar. Para a IA seguir o mesmo padrão:

1. **Cole como contexto:** este arquivo + o `AGENTS.md` + o texto da issue em que você vai trabalhar.
2. **Peça uma issue por vez.** Exemplo de pedido:
   > "Estou trabalhando na issue [4.3.1] Clientes — API. Leia o AGENTS.md e o roadmap. Implemente só o que a issue pede, seguindo o schema e as regras. Explique cada classe que criar."
3. **A IA não deve:**
   - mexer em arquivos de outras issues;
   - inventar regra, coluna ou tabela que não está no schema, na ERS ou no AGENTS.md (na dúvida, perguntar à equipe);
   - implementar itens fora do MVP (ver `AGENTS.md`, seção 8).
4. **Ao terminar,** peça à IA um resumo no formato do [[#11.2 Modelo de relatório de PR]]. Revise e **reescreva com as suas palavras** antes de colocar no Obsidian e no PR. Na banca, quem explica o código é você.
5. **Transparência:** PRs feitos com ajuda de IA mantêm a indicação de coautoria (regra a confirmar no CONTRIBUTING).
