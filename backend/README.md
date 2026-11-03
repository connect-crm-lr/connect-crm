# Connect CRM — Back-end

API REST do Connect CRM (Java 21 + Spring Boot 4.1 + PostgreSQL 17 + Flyway).

## Pré-requisitos

- Java 21 (JDK)
- Docker (para o PostgreSQL)
- Arquivo `database/.env` criado a partir do `database/.env.example`

## Subir o banco

```bash
cd database
docker compose up -d
```

## Rodar a aplicação

**Sem perfil** (padrão: é assim que roda em produção e no CI). O Flyway aplica só `db/migration` (V1 e V2) e o banco fica **sem dados de teste**:

```bash
# Linux / macOS / WSL
./mvnw spring-boot:run

# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

**Com o perfil `dev`** (dados de teste). O Flyway aplica também `db/dev`, com clientes, tecidos, estoque, negociações, pedidos, interações e follow-ups de exemplo:

```bash
# Linux / macOS / WSL
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows (PowerShell): as aspas são necessárias
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Também funciona com a variável de ambiente `SPRING_PROFILES_ACTIVE=dev` (útil na IDE: *Run Configuration → Active profiles: `dev`*).

No log deve aparecer `Migrating schema "public" to version "2.1 - dados teste"`.

## Testes

```bash
./mvnw test          # Windows: .\mvnw.cmd test
```

Os testes rodam **sem** o perfil `dev`, igual ao CI.

## Dados de teste (perfil `dev`)

Ficam em `src/main/resources/db/dev/` e **nunca vão para produção**: sem o perfil, o Flyway nem lê essa pasta. A configuração está em `src/main/resources/application-dev.yaml`.

| O que tem | Quantidade |
|---|---|
| Clientes (telefone com DDI 55, com tags) | 5 |
| Tecidos | 6 |
| Linhas de estoque (2 com estoque baixo, < 20 m) e entradas | 8 e 10 |
| Negociações (todas as etapas do funil) | 7 |
| Pedidos com itens (pago, em aberto e cancelado) | 6 |
| Interações e follow-ups (2 pendências para hoje) | 7 e 4 |

Os nomes são os mesmos dos mocks do front-end, e as datas são relativas ao dia em que o banco é criado. Assim o dashboard sempre tem vendas do mês, pendências e estoque baixo.

Os dados são coerentes:

- **saldo do estoque = entradas − itens vendidos** (pedidos não cancelados);
- **total do pedido = soma de metragem × preço** dos itens.

A própria migration confere essas duas regras no final. Se alguém editar os dados e quebrar a coerência, ela falha e nada é gravado.

Para conferir à mão, rode a auditoria do estoque (deve retornar **zero linhas**):

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

### Cuidados

- **Trocar de "com dev" para "sem dev" no mesmo banco dá erro.** O Flyway vê a versão 2.1 aplicada e não encontra o arquivo, então a validação falha. Para voltar ao banco sem dados de teste, recrie o banco:

  ```bash
  cd database
  docker compose down -v
  docker compose up -d
  ```

- **Versão das migrations de dev:** use `V<n>_1__...`, logo depois da última migration normal (`V2_1` vem depois da `V2`). Quando uma migration nova mudar as tabelas (ex.: `V3`), os dados de teste que dependem dela entram numa nova migration de dev (`V3_1__...`), sem editar as já aplicadas.
- O usuário inicial continua sendo o da V2, com senha só de desenvolvimento.
