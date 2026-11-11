# Deploy — Connect CRM

Passo a passo para publicar o sistema. Issue de referência: [#43](https://github.com/connect-crm-lr/connect-crm/issues/43).

> [!IMPORTANT]
> **A hospedagem ainda não foi escolhida pela equipe.** O repositório já está pronto para qualquer serviço que rode um container Docker (back-end), sirva arquivos estáticos (front-end) e ofereça PostgreSQL. Quando a hospedagem for decidida, falta só preencher a seção [Endereços](#7-endereços) e a *Versão publicada* do README.

---

## 1. Como o sistema é publicado

| Peça | O que é | Onde está |
|---|---|---|
| **Banco** | PostgreSQL 17 | serviço de banco da hospedagem |
| **API** | imagem Docker do Spring Boot | `backend/Dockerfile` |
| **Front** | arquivos estáticos gerados pelo Vite (`dist/`) | `frontend/` |

```
navegador ──► front (estático) ──VITE_API_URL──► API (container) ──POSTGRES_*──► PostgreSQL
                                   ◄── CORS_ALLOWED_ORIGINS ──┘
```

## 2. Banco de dados

1. Criar um PostgreSQL 17 na hospedagem e anotar host, porta, nome do banco, usuário e senha.
2. **Não** criar tabelas à mão: o Flyway cria tudo quando a API sobe (V1 = 12 tabelas, V2 = usuário inicial e tags de exemplo).
3. A senha do usuário inicial da V2 é **só de desenvolvimento**. Depois do primeiro deploy, ela precisa ser trocada (seção 2.1).

### 2.1 Trocar a senha do usuário inicial

O MVP não tem tela nem endpoint para trocar a senha (fica para uma versão futura). A troca é feita direto no banco da hospedagem, gravando o **hash BCrypt** da senha nova, nunca a senha em texto. A migration V2 **não** é editada: ela continua com a senha de desenvolvimento, e só o banco do servidor recebe a nova.

1. **Gerar o hash da senha nova.** Com o Docker Desktop aberto, em qualquer pasta:

   ```bash
   docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 revendedor 'SENHA_NOVA'
   ```

   Troque `SENHA_NOVA` pela senha escolhida, **entre aspas simples** (funciona igual no Git Bash e no PowerShell). A saída é uma linha assim:

   ```
   revendedor:$2y$10$...
   ```

   O hash é **tudo depois de `revendedor:`**: começa com `$2y$10$` e tem 60 caracteres. É BCrypt com custo 10, o mesmo do `BCryptPasswordEncoder` da API, que confere hashes `$2a$`, `$2b$` e `$2y$`. A senha digitada fica no histórico do terminal: feche o terminal depois, ou limpe o histórico.

2. **Gravar no banco da hospedagem**, pelo console SQL do serviço (ou `psql` conectado nele):

   ```sql
   UPDATE usuario SET senha_hash = '<hash>' WHERE email = 'revendedor@lrrepresentacoes.com.br';
   ```

   A resposta precisa ser `UPDATE 1`. `UPDATE 0` = e-mail digitado errado.

   > Cuidado com o `$` do hash: dentro de aspas simples no SQL ele é texto comum, mas num `psql -c "..."` com aspas **duplas** o terminal troca `$2y`, `$10`... por variáveis vazias e grava um hash quebrado. Use o console SQL ou o `psql` interativo.

3. **Conferir:** o `POST /auth/login` (seção 3.2), apontando para o endereço da API publicada, devolve 200 com a senha nova e 401 com a antiga.

4. Entregar a senha nova ao revendedor por um canal privado. Ela não vai para o repositório, issues, PRs nem para o Obsidian.

## 3. API (back-end)

### 3.1 Variáveis de ambiente

Configurar **no painel da hospedagem**, nunca no repositório:

| Variável | Exemplo | Para quê |
|---|---|---|
| `POSTGRES_HOST` | `db.exemplo.com` | endereço do banco (sem ela, usa `localhost`) |
| `POSTGRES_PORT` | `5432` | porta do banco |
| `POSTGRES_DB` | `connect_crm` | nome do banco |
| `POSTGRES_USER` | `connectcrm` | usuário do banco |
| `POSTGRES_PASSWORD` | *(senha forte)* | senha do banco |
| `CORS_ALLOWED_ORIGINS` | `https://connectcrm.exemplo.com` | endereço do front publicado, **sem barra no final**. Mais de um: separar por vírgula |
| `PORT` | `8080` | porta da API. Muitos serviços definem sozinhos; sem ela, usa 8080 |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75` | opcional, ajuste de memória da JVM |
| `JWT_SECRET` | *(chave gerada, ver abaixo)* | **obrigatória**: chave que assina o token de login. Sem ela, ou com menos de 32 caracteres, a API não sobe |
| `JWT_EXPIRACAO_HORAS` | `8` | opcional, quantas horas o login vale; sem ela, 8 |

- **`JWT_SECRET` do servidor:** gere uma chave nova só para a hospedagem, **diferente** da que está no seu `database/.env` local, e não a coloque em nenhum arquivo do repositório:

  ```bash
  openssl rand -base64 48
  ```

  (O Git Bash do Windows já traz o `openssl`.) A saída tem 64 caracteres, acima do mínimo de 32 exigido pela assinatura HS256. Trocar a chave depois desloga todo mundo: os tokens antigos deixam de valer.

- **Banco que exige SSL** (comum em bancos gerenciados): em vez de `POSTGRES_HOST`/`POSTGRES_PORT`/`POSTGRES_DB`, use a URL completa em `SPRING_DATASOURCE_URL`, ex.: `jdbc:postgresql://db.exemplo.com:5432/connect_crm?sslmode=require`. Usuário e senha continuam em `POSTGRES_USER` e `POSTGRES_PASSWORD`.
- **Nunca** ativar o perfil `dev` em produção (`SPRING_PROFILES_ACTIVE` vazio). O perfil `dev` grava os dados de teste da `db/dev`.

### 3.2 Construir e rodar a imagem

Na pasta `backend/`:

```bash
docker build -t connect-crm-backend .
```

Testar a imagem no próprio computador, usando o banco do `database/` (Docker Desktop no Windows/Mac):

```bash
docker run --rm -p 8080:8080 \
  -e POSTGRES_HOST=host.docker.internal \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=connect_crm \
  -e POSTGRES_USER=connectcrm \
  -e POSTGRES_PASSWORD=<senha do database/.env> \
  -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
  -e JWT_SECRET=<JWT_SECRET do database/.env> \
  connect-crm-backend
```

(No PowerShell, troque `\` por `` ` `` no fim das linhas. Se o banco local usa a porta 5433, informe `POSTGRES_PORT=5433`.)

Para conferir:

1. O log mostra o Flyway aplicando as migrations e `Started ConnectCrmApplication`.
2. O login com o usuário inicial da V2 devolve o token:

   ```bash
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"revendedor@lrrepresentacoes.com.br","senha":"<senha do usuário inicial>"}'
   ```

   No PowerShell:

   ```powershell
   Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/login -ContentType 'application/json' `
     -Body '{"email":"revendedor@lrrepresentacoes.com.br","senha":"<senha do usuário inicial>"}'
   ```

   Resposta esperada (200):

   ```json
   {"token":"eyJ...","tipo":"Bearer","expiraEmSegundos":28800,"nome":"Revendedor LR"}
   ```

   `expiraEmSegundos` é o `JWT_EXPIRACAO_HORAS` em segundos (8 h = 28800).

> Toda rota fora de `/auth/**` exige o cabeçalho `Authorization: Bearer <token>`. Por isso, abrir `http://localhost:8080/tags` no navegador responde **401** com `"mensagem":"Faça login para continuar"`. Esse 401 em JSON também mostra que a API está no ar: quem respondeu foi o Spring Security da aplicação, não um erro do servidor.

> O build da imagem pula os testes (`-DskipTests`) porque eles precisam do PostgreSQL. Os testes rodam no CI em todo PR, e o CI também confere se a imagem continua construindo (job **Back-end (imagem Docker)**).

## 4. Front-end

1. Configurar no serviço que publica o front:

   | Variável | Exemplo |
   |---|---|
   | `VITE_API_URL` | `https://api.connectcrm.exemplo.com` |

2. Comandos de build, com a pasta `frontend/` como raiz:

   ```bash
   npm ci
   npm run build
   ```

   Pasta publicada: **`frontend/dist`**.

3. **Rotas do React:** configurar o serviço para devolver o `index.html` em qualquer caminho (*rewrite* / *SPA fallback*). Sem isso, recarregar a página em `/clientes` dá erro 404.

> O Vite grava o `VITE_API_URL` no código **na hora do build**. Mudou o endereço da API? Faça um build novo do front.
>
> Em desenvolvimento não é preciso configurar nada: sem a variável, o front usa `http://localhost:8080` (ver `frontend/.env.example`). O endereço fica em `frontend/src/services/api.js`, junto com a função `chamarApi`, que as telas vão usar quando forem ligadas à API.

## 5. Ordem do primeiro deploy

1. Criar o banco (seção 2).
2. Publicar a API com as variáveis (seção 3), deixando `CORS_ALLOWED_ORIGINS` por enquanto com qualquer valor.
3. Publicar o front com `VITE_API_URL` = endereço da API.
4. Voltar na API e colocar em `CORS_ALLOWED_ORIGINS` o endereço real do front. Reiniciar a API.
5. Conferir os critérios da seção 6.
6. Trocar a senha do usuário inicial (seção 2.1) e conferir o login com a senha nova.

## 6. Critérios de aceitação da issue #43

- [ ] Front e API acessíveis por um endereço público.
- [ ] O Flyway criou as tabelas no banco do servidor: o log da API mostra as migrations V1 e V2 aplicadas.
- [ ] O front publicado consegue chamar a API: no navegador, aba *Rede* (F12), as chamadas para a API respondem sem erro de CORS.
- [ ] Nenhuma senha no repositório (`git grep -i password` só encontra nomes de variáveis).

## 7. Endereços

| Item | Valor |
|---|---|
| Hospedagem escolhida | *a definir* |
| Front | *a definir* |
| API | *a definir* |

## 8. Cuidados

- **Planos gratuitos que "dormem":** alguns serviços desligam a API quando ninguém usa, e a primeira chamada pode levar dezenas de segundos. Antes de uma apresentação ou do teste com o usuário (issue 5.3), abra o sistema alguns minutos antes.
- **Backup:** os scripts de `database/scripts/` funcionam com o banco local do Docker. No servidor, use o backup do próprio serviço de banco ou rode o `pg_dump` apontando para o banco da hospedagem.
- **Dados de teste:** produção começa só com a V1 e a V2. Nada da `db/dev` vai para o servidor.
