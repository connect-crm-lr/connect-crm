# Changelog

Todas as mudanças relevantes do Connect CRM ficam registradas neste arquivo.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto adota o [Versionamento Semântico](https://semver.org/lang/pt-BR/) (SemVer): `MAIOR.MENOR.CORREÇÃO`.

- **MAIOR** muda quando há alteração incompatível com a versão anterior.
- **MENOR** muda quando entra funcionalidade nova compatível. Cada milestone fechada gera uma versão MENOR (`v0.1.0`, `v0.2.0`...).
- **CORREÇÃO** muda quando entra só correção de defeito.

Enquanto a versão começa com `0`, o sistema está em desenvolvimento. A `v1.0.0` é o MVP entregue.

## [Não lançado]

### Adicionado
- Dados de teste no perfil `dev`, que nunca vão para produção ([#20](https://github.com/connect-crm-lr/connect-crm/pull/20)).
- API de Tags: listar, criar reaproveitando a tag existente e excluir informando o uso ([#26](https://github.com/connect-crm-lr/connect-crm/pull/26)).
- Scripts de backup e restauração do banco para Linux/WSL e PowerShell ([#28](https://github.com/connect-crm-lr/connect-crm/pull/28)).

## [0.1.0] - 2026-11-03

Base do repositório: ambiente igual para toda a equipe, esqueletos do back-end e do front-end e padrões de trabalho.

### Adicionado
- Estrutura do monorepo e template de pull request ([#1](https://github.com/connect-crm-lr/connect-crm/pull/1)).
- PostgreSQL 17 no Docker Compose, com credenciais no `database/.env` ([#4](https://github.com/connect-crm-lr/connect-crm/pull/4)).
- Esqueleto do back-end com Spring Boot 4.1 e Java 21 ([#7](https://github.com/connect-crm-lr/connect-crm/pull/7)).
- Migrations Flyway V1 (12 tabelas) e V2 (usuário inicial e tags de exemplo) ([#9](https://github.com/connect-crm-lr/connect-crm/pull/9)).
- Front-end em React + Vite com layout e telas com dados mockados ([#10](https://github.com/connect-crm-lr/connect-crm/pull/10)).
- CI no GitHub Actions: build e testes do back-end com PostgreSQL, lint e build do front-end ([#13](https://github.com/connect-crm-lr/connect-crm/pull/13)).
- `CONTRIBUTING.md` com os padrões de Git, commits, labels e PR ([#18](https://github.com/connect-crm-lr/connect-crm/pull/18)).
- `AGENTS.md`, roadmap e revisão da modelagem do banco ([#19](https://github.com/connect-crm-lr/connect-crm/pull/19)).
- `README.md`, `CHANGELOG.md` e `.gitattributes` da raiz.

[Não lançado]: https://github.com/connect-crm-lr/connect-crm/compare/v0.1.0...develop
[0.1.0]: https://github.com/connect-crm-lr/connect-crm/releases/tag/v0.1.0
