---
id: ADR-0001
title: Injeção de dependência manual, com grafo por feature
status: Approved
author: Chirstian Alexandre
date: 2026-08-11
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# ADR 0001 — Injeção de dependência manual, com grafo por feature

## Contexto

O template precisa definir como as dependências são montadas antes de o app real
ser clonado a partir dele (horizonte de anos, time previsto de ~10 pessoas).

A primeira versão colocou um `AppGraph` único em `core/di/` que montava as tripas
de cada feature (repositório, use case, ViewModel). Duas falhas apareceram em
review:

1. **Direção de dependência invertida.** `core/` é infraestrutura que as features
   consomem; um grafo em `core/` que importa `features.*` cria ciclo
   (`feature → core → feature`) assim que as features virarem módulos Gradle —
   caminho já documentado no SDD 02.
2. **Ponto de contenção.** Com N features, o arquivo conheceria ~4N tipos e
   mudaria em todo PR, independentemente de qual feature foi tocada.

As alternativas de framework em KMP foram avaliadas (2026-08-11):

| Opção | Erro aparece em | Custo |
| --- | --- | --- |
| Manual | compilação | repasse de dependências compartilhadas é manual |
| Koin | **runtime** | dependência não declarada vira crash, não erro de build |
| kotlin-inject | compilação | KSP no tempo de build |
| Metro | compilação | framework novo, curva de aprendizado |

## Decisão

**DI manual, com um grafo por feature e o composition root no topo.**

- Cada feature expõe `features/<nome>/di/<Nome>Graph.kt`, único lugar que conhece
  as tripas dela. Dependências compartilhadas entram por construtor.
- O composition root vive em `com.kmpfoundation.di.AppGraph` — **fora de
  `core/`** — e cresce **uma linha por feature**.
- **ViewModels são criados por factory, nunca guardados no grafo.** O ciclo de
  vida pertence ao `viewModel { }` da UI (ViewModelStore da plataforma); o grafo
  só sabe montar. Guardar o ViewModel no grafo vazaria estado entre telas e
  anularia a sobrevivência à recriação da Activity.

Descartamos frameworks **por ora**, não por princípio: sem conhecer o formato do
grafo real (quantos singletons, quantas features), a escolha seria feita sem
dados. A estrutura por feature é idêntica nos quatro cenários, então adotar um
framework depois é uma edição **local** dentro de cada feature — um
`<Nome>Graph` vira um `module { }` do Koin ou um `@Component` do Metro sem
big-bang.

## Consequências

**A favor:**
- Zero dependência extra; grafo verificado pelo compilador (esqueceu de passar
  algo → não compila).
- Testes triviais: tudo entra por construtor, Fakes substituem sem framework.
- Crescimento distribuído — quem mexe na feature mexe no grafo dela.

**Contra (aceito conscientemente):**
- O repasse de dependências compartilhadas (`HttpClient`, banco, engines
  nativas) é manual e percorre a cadeia a cada dependência nova.

**Passa a ser proibido:**
- `core/` conhecer qualquer feature. Só `di/` conhece.
- Guardar instância de ViewModel em qualquer grafo.

**Gatilho de reavaliação:** quando o repasse manual de dependências
compartilhadas virar reclamação recorrente em review, ou o `AppGraph` passar a
concentrar mais de ~8 singletons compartilhados. Nesse momento, reabrir como RFC
comparando Metro (preferido, mantém segurança em compilação) e Koin.
