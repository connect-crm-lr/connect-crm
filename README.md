# Connect CRM

[![CI](https://github.com/connect-crm-lr/connect-crm/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/connect-crm-lr/connect-crm/actions/workflows/ci.yml)

Sistema web de CRM para a **LR Representações**, um revendedor autônomo de tecidos que atende lojas, ateliês e confecções pelo WhatsApp. O sistema organiza clientes, tags de interesse, interações, follow-ups, o funil de negociações, o catálogo de tecidos, o estoque em metros e os pedidos.

Projeto acadêmico do curso de Análise e Desenvolvimento de Sistemas (ADS) da Faculdade SESI SENAI FATESG.

## Equipe

| Integrante | Papel |
|---|---|
| Maria Eduarda Martins Passos | Gerente do projeto e Product Owner |
| Gabriel Carvalho de Oliveira | Desenvolvimento back-end e revisão de código |
| João Pedro Abdala | Banco de dados e back-end |
| Yan Santos | Desenvolvimento front-end |

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Back-end | Java 21, Spring Boot 4.1 (API REST), Maven Wrapper |
| Front-end | React + Vite, React Router, `lucide-react` |
| Banco de dados | PostgreSQL 17 no Docker Compose |
| Migrations | Flyway |
| Integração contínua | GitHub Actions |

## Estrutura do repositório

```
connect-crm/
├── backend/     API REST em Spring Boot (pacote br.com.lrrepresentacoes.connectcrm)
├── frontend/    telas em React + Vite
├── database/    docker-compose do PostgreSQL, .env.example e scripts de backup
├── docs/        manual do usuário, roadmap e decisões de modelagem do banco
├── .github/     template de PR e workflow de CI
├── AGENTS.md    regras técnicas e de negócio
└── CONTRIBUTING.md  fluxo de Git, commits, labels e PR
```

## Como rodar localmente

### Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Java 21 (JDK)
- Node.js 24

### 1. Banco de dados

```bash
cd database
cp .env.example .env        # Windows: copy .env.example .env
```

Abra o `database/.env` e defina a senha (`POSTGRES_PASSWORD`). Se a porta 5432 já estiver em uso no seu computador, troque o `POSTGRES_PORT` (por exemplo, para 5433). Depois:

```bash
docker compose up -d
docker compose ps           # o container connect-crm-db deve aparecer como "running"
```

### 2. Back-end

```bash
cd backend
./mvnw spring-boot:run      # Windows (PowerShell): .\mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`. Na primeira execução, o Flyway cria as tabelas. Para subir com dados de teste, use o perfil `dev` (veja o [README do back-end](backend/README.md)).

### 3. Front-end

```bash
cd frontend
npm install
npm run dev
```

As telas abrem em `http://localhost:5173`.

### 4. Primeiro acesso

Abra `http://localhost:5173` e entre com o usuário inicial criado pela migration V2 (`revendedor@lrrepresentacoes.com.br`). A senha dele é **só de desenvolvimento**: peça para a equipe e troque antes de o sistema entrar em uso. Como usar cada tela está no [manual do usuário](docs/manual/manual-do-usuario.md).

### Problemas comuns

| Sintoma | O que fazer |
|---|---|
| `docker compose up` falha com porta em uso | troque o `POSTGRES_PORT` no `database/.env` (ex.: 5433) e suba de novo |
| O back-end não conecta no banco | confira se o container está `running` (`docker compose ps` em `database/`) e se o `database/.env` existe |
| `mvnw` sem permissão (Linux/WSL) | rode `chmod +x mvnw` em `backend/` |
| O front abre, mas sem dados da API | confira se o back-end está rodando em `http://localhost:8080` |

## Versão publicada

> A versão publicada do sistema **ainda não foi definida** pela equipe (hospedagem e endereço). Quando a v1.0.0 for publicada, esta seção recebe o link de acesso e as instruções de login.

| Item | Valor |
|---|---|
| Endereço | *a definir* |
| Versão | *a definir* (ver [CHANGELOG](CHANGELOG.md)) |

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/manual/manual-do-usuario.md](docs/manual/manual-do-usuario.md) | manual do usuário, com os prints das telas |
| [CONTRIBUTING.md](CONTRIBUTING.md) | branches, commits, labels, PR e definição de pronto |
| [AGENTS.md](AGENTS.md) | regras técnicas e de negócio do projeto |
| [CHANGELOG.md](CHANGELOG.md) | histórico de versões |
| [backend/README.md](backend/README.md) | perfis, testes e dados de teste do back-end |
| [database/README.md](database/README.md) | banco, backup e restauração |
| [docs/connect-crm-roadmap.md](docs/connect-crm-roadmap.md) | milestones, issues e cronograma |
