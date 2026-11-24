# Avaliação N2 — Testes automatizados (Teste de Software, ADS 4)

Testes da regra **pedido × estoque** do Connect CRM, feitos sobre o código real do projeto.

| Caso | Tipo | Arquivo | O que confere |
|---|---|---|---|
| U-01 | Unitário (JUnit 6 + Mockito) | `backend/src/test/java/br/com/lrrepresentacoes/connectcrm/n2/PedidoServiceN2Test.java` | Pedido com estoque suficiente: total R$ 350,45 e baixa de cada linha |
| U-02 | Unitário (JUnit 6 + Mockito) | idem | Metragem maior que o saldo: recusa com "Estoque insuficiente: … tem 15 m" e nada gravado |
| U-03 | Unitário (JUnit 6 + Mockito) | idem | Limite: vender exatamente o saldo é aceito e a linha fica com 0 m |
| C-01 | Integração (Spring Boot + MockMvc + PostgreSQL) | `backend/src/test/java/br/com/lrrepresentacoes/connectcrm/n2/PedidoFluxoN2IntegrationTest.java` | Login → cadastro → pedido pela API → estoque 27,5 m → cancelamento → estoque 40 m |

## Versão do projeto

- Repositório: https://github.com/connect-crm-lr/connect-crm
- Base: branch `develop`, commit `6c9a6ba`. Os testes ficam na branch `test/n2-teste-software`.

## O que precisa estar instalado

- JDK 21 (o Maven Wrapper `mvnw` já vem no projeto; não precisa instalar o Maven)
- Docker Desktop (para o PostgreSQL 17 do C-01)
- Git

Bibliotecas usadas (já declaradas no `pom.xml` pelos *starters* de teste do Spring Boot 4.1.1;
o Maven baixa sozinho): JUnit Jupiter 6.0.3, Mockito 5.23.0, AssertJ 3.27.7, Spring Test 7.0.9
(MockMvc), JsonPath 2.10.0 e driver PostgreSQL 42.7.13.

## Como preparar o ambiente

1. Clonar o repositório e entrar na branch dos testes:
   ```bash
   git clone https://github.com/connect-crm-lr/connect-crm.git
   cd connect-crm
   git switch test/n2-teste-software
   ```
2. Criar o arquivo de configuração do banco a partir do exemplo e preencher usuário, senha,
   porta e a chave do JWT (ele não vai para o Git):
   ```bash
   cp database/.env.example database/.env        # Linux/macOS
   copy database\.env.example database\.env      # Windows (cmd)
   ```
   O usuário inicial da aplicação (`revendedor@lrrepresentacoes.com.br`) é criado pela migration V2;
   o C-01 faz login com ele.
3. Subir o PostgreSQL:
   ```bash
   cd database
   docker compose up -d
   cd ..
   ```
   As tabelas e o usuário inicial são criados pelo Flyway (migrations V1 e V2) quando o teste sobe a aplicação.

## Como rodar os quatro casos

Windows (cmd), sem espaço depois da vírgula:

```cmd
cd backend
mvnw.cmd test -Dtest=PedidoServiceN2Test,PedidoFluxoN2IntegrationTest
```

Windows (PowerShell):

```powershell
cd backend
.\mvnw.cmd test "-Dtest=PedidoServiceN2Test,PedidoFluxoN2IntegrationTest"
```

Linux / macOS:

```bash
cd backend
./mvnw test -Dtest='PedidoServiceN2Test,PedidoFluxoN2IntegrationTest'
```

Só os unitários (não precisam de banco):

```bash
./mvnw test -Dtest=PedidoServiceN2Test
```

Resultado esperado no terminal: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`.

Os relatórios de cada classe ficam em `backend/target/surefire-reports/` (arquivos `.txt` e `.xml`).

Resultado obtido em 24/11/2026 (Windows 11, JDK 21.0.7, PostgreSQL 17 no Docker): os quatro casos
passaram (`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`).

## Dados de teste

- Unitários: tecidos, cores e saldos montados no próprio teste (Tricoline Lisa 07 Bege 40 m,
  Tricoline Listrada 05 Azul e branco 20 m, Tricoline Flame Premium 02 Cru 15 m, Tricoline Mista 01 Branco 15 m).
- C-01: cadastra pela API o cliente "N2 Boutique Teste", o tecido "N2 Tricoline Teste" e uma
  entrada de 40 m. O teste roda numa transação desfeita no fim, então nada fica gravado no banco.
