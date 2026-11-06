# Processo de release — v1.0.0

Passo a passo para fechar a versão **1.0.0 (MVP)** seguindo o Git Flow descrito no [CONTRIBUTING](../CONTRIBUTING.md). Issue de referência: [#54](https://github.com/connect-crm-lr/connect-crm/issues/54).

> **Resumo:** no code freeze (23/11) sai a `release/v1.0.0` da `develop`. Ela só recebe correções, sempre por PR. No fim, um PR leva a release para a `main` (onde entra a tag `v1.0.0`) e outro PR traz as correções de volta para a `develop`.

```
develop ──●────────────●──────────────────●── (PR release → develop)
           \                             /
release/v1.0.0 ●──fix──fix──CHANGELOG──●
                                        \
main ────────────────────────────────────●── tag v1.0.0 (PR release → main)
```

---

## 1. Antes do code freeze

- [ ] Todas as issues da milestone `v1.0.0 — MVP` mergeadas na `develop` (ou movidas para depois do MVP, com decisão registrada).
- [ ] CI verde na `develop`.
- [ ] Ninguém com PR de funcionalidade aberto para a `develop` esperando entrar no MVP.
- [ ] Combinar na equipe quem cria a branch e quem revisa os PRs da release.

## 2. Criar a branch de release (23/11)

Na raiz do repositório:

```bash
git checkout develop
git pull
git checkout -b release/v1.0.0
git push -u origin release/v1.0.0
```

Depois de criada, **ninguém commita direto nela**. A partir daqui:

- funcionalidade nova **não entra** no MVP (fica na `develop` para a próxima versão);
- a `develop` continua aberta, mas nada dela vai para a release.

**Configuração no GitHub (quem tem acesso de admin):** em *Settings → Branches* (ou *Rules*), proteger `release/*` igual à `develop`: sem push direto, sem force push, sem exclusão e com 1 aprovação. Sem isso, a regra "só por PR" depende só da disciplina da equipe.

## 3. Correções da homologação

Cada defeito encontrado na homologação (ex.: teste de aceitação, item 5.3 do roadmap) vira uma issue e uma branch **a partir da release**:

```bash
git checkout release/v1.0.0
git pull
git checkout -b fix/descricao-curta
# ... correção + commits fix(escopo): ...
git push -u origin fix/descricao-curta
```

- O PR vai para **`release/v1.0.0`** (não para a `develop`). No template, a primeira linha do checklist já prevê essa exceção.
- O CI roda nesses PRs (o workflow aceita `release/**` como base).
- Mesmo fluxo de sempre: template preenchido, 1 aprovação, merge commit, apagar a branch.
- Só correção. Se aparecer algo que é funcionalidade nova, vai para a `develop` e fica fora do MVP.

## 4. Atualizar o CHANGELOG

Na própria release, por PR (ex.: branch `docs/changelog-v1.0.0` saindo da `release/v1.0.0`):

1. Renomear a seção `## [Não lançado]` para `## [1.0.0] - AAAA-MM-DD` (data do merge na `main`).
2. Conferir se todos os PRs mergeados desde a última versão estão listados em *Adicionado*, *Alterado* ou *Corrigido*, com o link do PR. Para listar:

   ```bash
   git log v0.1.0..release/v1.0.0 --merges --oneline
   ```

   (troque `v0.1.0` pela última tag que existir na época.)

3. Escrever uma linha de resumo da versão logo abaixo do título, como foi feito na `0.1.0`.
4. Criar uma `## [Não lançado]` nova e vazia no topo.
5. Atualizar os links do fim do arquivo:

   ```markdown
   [Não lançado]: https://github.com/connect-crm-lr/connect-crm/compare/v1.0.0...develop
   [1.0.0]: https://github.com/connect-crm-lr/connect-crm/compare/vX.Y.Z...v1.0.0
   ```

   (`vX.Y.Z` = tag anterior à `v1.0.0`.)

## 5. PR `release/v1.0.0` → `main`

- **Título:** `release: v1.0.0 — MVP`
- **Base:** `main` · **Compare:** `release/v1.0.0`
- **Descrição:** resumo da versão (o mesmo do CHANGELOG) e `Closes #54`.
- Tipo de mudança: `chore`. Tipo de manutenção: *Não se aplica*.
- CI verde e 1 aprovação. Merge com **merge commit**. **Não apagar** a branch ainda (ela é usada no passo 7).

## 6. Tag `v1.0.0` na `main`

Depois do merge:

```bash
git checkout main
git pull
git tag -a v1.0.0 -m "v1.0.0 — MVP"
git push origin v1.0.0
```

- A tag vai no **commit de merge** do PR do passo 5 (o topo da `main` logo depois do merge).
- `-a` cria uma tag anotada (guarda autor, data e mensagem). É o recomendado para versões.
- Opcional: em *Releases → Draft a new release*, escolher a tag `v1.0.0` e colar a seção `[1.0.0]` do CHANGELOG.

Conferir:

```bash
git describe --tags main   # deve mostrar v1.0.0
```

## 7. PR `release/v1.0.0` → `develop`

Traz as correções da homologação e o CHANGELOG de volta.

```bash
git checkout develop
git pull
git checkout release/v1.0.0
git pull
git merge develop
```

- Se a `develop` andou depois do code freeze, pode dar conflito. **Resolva na release antes de abrir o PR**, mantendo as correções da release e as mudanças novas da `develop`.
- **Título:** `chore(release): traz as correções da v1.0.0 para a develop`
- **Base:** `develop` · **Compare:** `release/v1.0.0`
- CI verde, 1 aprovação, merge commit.
- Depois do merge, **apagar** a `release/v1.0.0`.

Conferir que nenhuma correção ficou para trás (não deve listar nada):

```bash
git fetch
git log origin/develop..origin/main --oneline --no-merges
```

## 8. Critérios de aceitação da issue #54

- [ ] Tag `v1.0.0` existe e aponta para a `main`.
- [ ] `develop` tem todas as correções da release (comando do passo 7 sem saída).
- [ ] CHANGELOG com a seção `[1.0.0]` e links atualizados.
- [ ] Milestone `v1.0.0 — MVP` fechada.

## 9. E depois da v1.0.0?

Defeito encontrado no que já está na `main` → branch `hotfix/descricao-curta` a partir da `main`, PR para a `main` (nova tag `v1.0.1`) e PR para a `develop`, como está no CONTRIBUTING.
