# Connect CRM — Banco de dados

PostgreSQL 17 rodando no Docker (container `connect-crm-db`). As tabelas são criadas pelo Flyway quando o back-end sobe (veja o [README do back-end](../backend/README.md)).

| Arquivo | Para que serve |
|---|---|
| `docker-compose.yml` | sobe o container do banco |
| `.env.example` | modelo do `.env` com usuário, senha, nome do banco e porta |
| `.env` | credenciais locais (**fora do Git**) |
| `scripts/` | backup e restauração do banco |
| `backups/` | arquivos gerados pelo backup (**fora do Git**, criada pelo script) |

## Subir o banco

```bash
cd database
cp .env.example .env      # Windows: copy .env.example .env  — depois ajuste a senha
docker compose up -d
```

---

## Backup e restauração (RNF-004)

Os scripts leem o **usuário** e o **nome do banco** do `database/.env` e executam o `pg_dump` / `psql` **dentro do container**, pelo socket local. A senha não é usada nem aparece nos scripts.

| Sistema | Backup | Restauração |
|---|---|---|
| Linux / WSL | `scripts/backup.sh` | `scripts/restaurar.sh` |
| Windows (PowerShell) | `scripts/backup.ps1` | `scripts/restaurar.ps1` |

Os comandos abaixo são rodados na **raiz do repositório**, com o container `connect-crm-db` rodando.

### Fazer backup

```bash
# Linux / WSL
bash database/scripts/backup.sh

# Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File .\database\scripts\backup.ps1
```

O arquivo é salvo em `database/backups/` com a data e a hora no nome:

```
database/backups/connect_crm_2026-10-26_1530.sql
```

O backup é um `.sql` completo (estrutura + dados + histórico do Flyway). Ele já começa apagando cada objeto antes de recriá-lo, por isso a restauração substitui o que estiver no banco.

### Restaurar

```bash
# Linux / WSL
bash database/scripts/restaurar.sh database/backups/connect_crm_2026-10-26_1530.sql

# Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File .\database\scripts\restaurar.ps1 .\database\backups\connect_crm_2026-10-26_1530.sql
```

- Pode passar o caminho do arquivo ou só o nome dele, se estiver em `database/backups/`.
- Sem nenhum arquivo, o script mostra os backups disponíveis (mais recente primeiro).
- O script pede para digitar `SIM` antes de restaurar. Para pular a confirmação, use `--sim` (Linux/WSL) ou `-Sim` (PowerShell).

### Cuidados

- **A restauração sobrescreve os dados atuais.** Tudo o que foi gravado depois do backup é perdido. Na dúvida, faça um backup novo antes de restaurar.
- **Pare o back-end e feche o pgAdmin antes de restaurar.** Se houver outra conexão aberta no banco, o script para e mostra quem está conectado.
- A restauração roda **numa transação só**: se der erro no meio, nada é gravado e o banco continua como estava.
- Restaure o backup no **mesmo PostgreSQL 17** do `docker-compose.yml`. Um backup gerado por uma versão mais nova pode não abrir numa mais antiga.
- O backup contém **todos os dados do sistema**, inclusive o hash da senha do usuário. Não envie o arquivo por WhatsApp, e-mail ou Git. A pasta `database/backups/` já está no `.gitignore`.
- `docker compose down -v` **apaga o volume com os dados**. Faça um backup antes, se precisar deles.
- No Windows, o `-ExecutionPolicy Bypass` libera só aquela execução do script; nenhuma configuração do sistema é alterada.

### Conferir se a restauração funcionou

Teste completo (o mesmo dos critérios de aceite da issue #24):

```bash
# 1. Contar as linhas de algumas tabelas e anotar
docker exec connect-crm-db psql -U connectcrm -d connect_crm -c "SELECT (SELECT COUNT(*) FROM cliente) AS clientes, (SELECT COUNT(*) FROM tecido) AS tecidos, (SELECT COUNT(*) FROM pedido) AS pedidos, (SELECT COUNT(*) FROM item_pedido) AS itens"

# 2. Fazer o backup
bash database/scripts/backup.sh

# 3. Apagar o banco e subir vazio
cd database
docker compose down -v
docker compose up -d
cd ..

# 4. Restaurar e contar de novo: os números têm que ser iguais aos do passo 1
bash database/scripts/restaurar.sh <arquivo gerado no passo 2>
docker exec connect-crm-db psql -U connectcrm -d connect_crm -c "SELECT (SELECT COUNT(*) FROM cliente) AS clientes, (SELECT COUNT(*) FROM tecido) AS tecidos, (SELECT COUNT(*) FROM pedido) AS pedidos, (SELECT COUNT(*) FROM item_pedido) AS itens"
```

Troque `connectcrm` e `connect_crm` se o seu `.env` usar outro usuário ou outro nome de banco.

Fora do escopo por enquanto: backup automático agendado e backup em nuvem.
