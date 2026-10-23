# Como contribuir com o Connect CRM

Este guia explica como a equipe trabalha no repositório: branches, commits, labels, pull requests e revisão. Siga-o em toda issue.

---

## 1. Fluxo de branches (Git Flow)

| Branch | Para que serve |
|---|---|
| `main` | Só versões fechadas. Cada versão recebe uma tag (`v0.1.0`, `v0.2.0`...). Ninguém commita direto. |
| `develop` | Branch de integração. Todo PR vai para ela. Ninguém commita direto. |
| `tipo/descricao-curta` | Uma branch por issue, criada a partir da `develop` atualizada. |
| `release/vX.Y.Z` | Criada no fechamento de uma versão. Só recebe correções. |
| `hotfix/descricao-curta` | Correção urgente de algo que já está na `main`. |

As branches `main` e `develop` são protegidas: não aceitam push direto, force push nem exclusão, e só recebem código por PR aprovado.

### Nome das branches

Padrão: **`tipo/descricao-curta`**, em minúsculas, com palavras separadas por hífen. O tipo é o mesmo dos commits (seção 2).

| Exemplo | Quando |
|---|---|
| `feat/clientes-api` | nova funcionalidade |
| `fix/telefone-whatsapp` | correção de defeito |
| `docs/contributing` | documentação |
| `ci/github-actions` | pipeline de CI |
| `chore/esqueleto-backend` | configuração do repositório |

### Criar a branch de uma issue

```bash
git checkout develop
git pull
git checkout -b tipo/descricao-curta
```

---

## 2. Commits (Conventional Commits)

Formato: **`tipo(escopo): descrição no imperativo`**, em português.

```
feat(clientes): adiciona cadastro de cliente
fix(estoque): impede saldo negativo na entrada
docs(readme): explica como subir o banco
```

| Tipo | Uso |
|---|---|
| `feat` | nova funcionalidade |
| `fix` | correção de defeito |
| `docs` | só documentação |
| `style` | formatação, sem mudar lógica |
| `refactor` | melhoria de código sem mudar comportamento |
| `test` | testes novos ou ajustados |
| `build` | dependências ou sistema de build |
| `ci` | pipeline de integração contínua |
| `chore` | manutenção e configuração do repositório |
| `perf` | melhoria de desempenho |

**Escopo** = a parte do sistema afetada: `clientes`, `tags`, `estoque`, `pedidos`, `funil`, `backend`, `frontend`, `database`...

**Boas práticas:**

- Commits pequenos, cada um com uma mudança só.
- Antes de commitar, confira o `git status` e adicione os arquivos pelo nome (evite `git add .`).
- Nunca commite o `.env`, senhas, `node_modules/`, `dist/` ou `target/`.

---

## 3. Labels

### Prioridade

| Label | Quando usar |
|---|---|
| `prioridade: alta` | bloqueia outras issues ou faz parte do núcleo do sistema |
| `prioridade: média` | importante, mas não bloqueia ninguém |
| `prioridade: baixa` | melhoria que pode esperar |

### Tipo de manutenção (ISO/IEC 14764)

| Label | Quando usar |
|---|---|
| `manutenção: corretiva` | corrige um defeito |
| `manutenção: adaptativa` | adapta a mudanças de ambiente (versões, dependências, infraestrutura) |
| `manutenção: perfectiva` | melhora ou amplia uma funcionalidade existente |
| `manutenção: preventiva` | previne problemas futuros (refatoração, redução de dívida técnica) |

Em **implementação nova** ou configuração inicial, nenhuma label de manutenção se aplica. No PR, marque "Não se aplica" na seção *Tipo de manutenção* do template.

---

## 4. Pull requests

1. **Traga a `develop` atualizada para a sua branch** antes de abrir o PR e resolva os conflitos:

```bash
   git checkout develop
   git pull
   git checkout sua-branch
   git merge develop
```

2. **Abra o PR sempre para a `develop`** (exceto `release/` e `hotfix/`).
3. **Preencha o template inteiro**: descrição, `Closes #número`, tipo de mudança, tipo de manutenção, como testar e checklist.
4. **Na lateral:** milestone da versão, label de prioridade e um revisor.
5. **O CI precisa estar verde** (build, testes e lint). PR com CI vermelho não é mergeado.
6. **1 aprovação de outro integrante.** O autor não aprova o próprio PR.
7. **Quem faz o merge é o autor**, com **merge commit**, e depois apaga a branch.
8. Depois do merge, atualize a máquina:

```bash
   git checkout develop
   git pull
   git branch -d sua-branch
```

---

## 5. Revisão de código

Quem revisa confere:

- [ ] O PR faz **só o que a issue pede**, sem mexer em arquivos de outras issues
- [ ] Segue o modelo de dados (migrations em `backend/src/main/resources/db/migration/`) e as regras do `AGENTS.md`
- [ ] Os testes passam e as regras de negócio do back têm teste
- [ ] Não há arquivo estranho no diff (`.env`, `node_modules`, arquivos criados por engano)
- [ ] O template do PR está preenchido e o CI está verde

Se algo precisa mudar, use **Request changes** com comentários claros. Se está tudo certo, **Approve**.

---

## 6. Definição de pronto

Uma issue só é fechada quando:

- [ ] o código compila e a aplicação sobe
- [ ] os testes passam (e, no back, a regra de negócio tem teste do service)
- [ ] o CI está verde
- [ ] o PR foi aprovado e mergeado na `develop`
- [ ] o autor consegue explicar cada classe e arquivo que criou
- [ ] o relatório do PR está no Obsidian da equipe

---

## 7. Onde rodar cada comando

| Comando | Pasta |
|---|---|
| `docker compose ...` | `database/` |
| `mvnw ...` (Windows: `mvnw.cmd`) | `backend/` |
| `npm ...` | `frontend/` |
| `git ...` | qualquer pasta do repositório |

Para subir o ambiente do zero, veja o [README](README.md).