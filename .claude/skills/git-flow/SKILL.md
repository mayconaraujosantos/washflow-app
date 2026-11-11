---
name: git-flow
description: Git flow do Washflow com a ferramenta git-flow (AVH). Use antes de qualquer commit, criação de branch, merge, push, release ou hotfix neste repositório. Nunca commitar direto em main ou develop.
---

# Git flow do Washflow

`main` é produção e `develop` é integração. Todo trabalho é feito numa branch de
apoio criada e finalizada pela ferramenta `git flow` (git-flow AVH), que faz os
merges e as tags localmente. **Não há PR**: depois do `finish`, envia-se `develop`
(e `main`, com a tag, em release/hotfix).

**Nunca commite direto em `main` ou `develop`.** O hook `.githooks/pre-commit`
recusa esses commits; os merges do `git flow ... finish` passam.

## Preparar o clone (uma vez)

`make hooks` liga os hooks versionados e grava a configuração do git flow
(`main`/`develop`, prefixos `feature/`, `fix/`, `release/`, `hotfix/`, tags `v`,
merge sem fast-forward e fetch antes do finish). Confira com `git flow config`.

## Qual branch usar

| Trabalho | Comando | Nasce de | Finish faz merge em |
|---|---|---|---|
| Funcionalidade, refatoração, infra, docs | `git flow feature start <assunto>` | `develop` | `develop` |
| Correção de algo ainda não lançado | `git flow bugfix start <assunto>` (branch `fix/`) | `develop` | `develop` |
| Preparar versão | `git flow release start <x.y.z>` | `develop` | `main` (tag `vx.y.z`) e `develop` |
| Correção urgente em produção | `git flow hotfix start <x.y.z>` | `main` | `main` (tag) e `develop` |

`<assunto>` em kebab-case, sem o prefixo (`git flow feature start customer-signin`
cria `feature/customer-signin`). Versões seguem SemVer a partir da última tag
(`git describe --tags --abbrev=0`): release sobe minor, hotfix sobe patch.

## Passo a passo

1. **Antes de editar:** `git branch --show-current`. Se for `main` ou `develop`,
   rode o `start` adequado (mudanças não commitadas vão junto para a nova branch).
2. **Commits na branch de apoio:** Conventional Commits em inglês, como o histórico
   (`feat(customer): ...`, `fix(web): ...`, `refactor: ...`, `test(e2e): ...`,
   `chore: ...`, `docs: ...`; escopo = agregado ou `web`/`infra`/`e2e`). Não inclua
   arquivos gerados (`web/.vitest/`, `build/`).
3. **Trabalho longo ou compartilhado:** `git flow feature publish` envia a branch.
4. **Finalizar** (só quando o usuário pedir):
   * `git flow feature finish <assunto>` / `git flow bugfix finish <assunto>`
     → merge `--no-ff` em `develop` e apaga a branch.
   * `git flow release finish -m "Release vx.y.z" <x.y.z>` e
     `git flow hotfix finish -m "Hotfix vx.y.z" <x.y.z>` → merge em `main`, tag,
     back-merge em `develop`.
5. **Enviar** (só quando o usuário pedir): `git push origin develop`; após
   release/hotfix, `git push origin main develop --tags`. O `pre-push` roda lint,
   formatação e compilação; se falhar, corrija numa nova branch de apoio, nunca
   com `--no-verify`.

## Se algo saiu do fluxo

* Commit feito em `main`/`develop` por engano e **ainda sem push**: guarde-o com
  `git branch feature/<assunto>` e volte a branch errada com
  `git reset --hard origin/<main|develop>`. Depois do push, não reescreva: avise o usuário.
* Conflito no `finish`: resolva, `git add` e `git commit` (o hook deixa concluir o
  merge) e rode o mesmo `finish` de novo.
* Nunca force push em `main`/`develop` nem apague essas branches.
