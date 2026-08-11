---
id: ADR-0003
title: O composition root vive no ciclo da aplicação, não no da tela
status: Approved
author: Chirstian Alexandre
date: 2026-08-11
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# ADR 0003 — O composition root vive no ciclo da aplicação, não no da tela

## Contexto

O ADR 0001 decidiu **onde** o composition root mora no código
(`com.kmpfoundation.di.AppGraph`, fora de `core/`) e **como** ele monta as
dependências (manual, um grafo por feature). Não decidiu **quanto tempo ele
vive** — e a primeira implementação, por omissão, o amarrou à tela:

- `MainActivity.onCreate` criava `AppGraph(AndroidGreetingEngine())`;
- `MainViewController` criava o `AppGraph` dentro do lambda do
  `ComposeUIViewController`.

Com o grafo atual (uma engine barata) isso não tem efeito observável. O defeito
aparece na primeira dependência com estado: **toda rotação de tela no Android
reconstrói tudo o que o grafo segura**. Para o app alvo, isso significa
recriar cliente HTTP (e seu pool de conexões), banco, cache e as sessões dos
SDKs nativos (Imou/Dahua, Tuya) a cada mudança de configuração — perda de
estado e de conexões abertas, difícil de atribuir à causa depois.

O ponto que decidiu a discussão: um framework de DI resolveria isso por
acidente (`@Singleton`) sem que ninguém precisasse entender o problema. Escopo
é a única parte da DI que **não** é edição local por feature ao migrar depois
(wiring é). Então ele precisa estar certo e explícito **antes** de qualquer
decisão de framework.

## Decisão

**O `AppGraph` é instanciado por quem é dono do ciclo de vida da aplicação, e
apenas uma vez por processo.**

- **Android:** `KmpFoundationApplication` (registrada em `android:name` no
  manifesto) expõe `val graph: AppGraph by lazy { ... }`. A `MainActivity` só
  **lê** o grafo (`application as KmpFoundationApplication`), nunca o constrói.
- **iOS:** `iOSApp` (o `@main`) mantém `private let graph = AppGraph(...)` e o
  repassa pela hierarquia SwiftUI. `MainViewController` passou a receber
  `graph: AppGraph` em vez de `greetingEngine: GreetingEngine`.
- As dependências de plataforma (engines nativas, SDKs) continuam sendo
  construídas pela casca — mas agora **junto do grafo**, no mesmo escopo.
- Nenhuma tela, composable, `UIViewControllerRepresentable` ou `Activity`
  constrói um `AppGraph`.

Isso **não** altera a regra do ADR 0001 sobre ViewModels: eles continuam vindo
de factory e tendo o ciclo de vida do `viewModel { }`. O grafo vive muito; o
ViewModel vive o que a tela viver. São dois escopos distintos, de propósito.

## Consequências

**A favor:**
- Dependências caras e com estado são criadas uma vez por processo — que é o que
  qualquer framework de DI chamaria de escopo singleton, aqui explícito.
- O template passa a ter **dois escopos nomeados e visíveis** (aplicação e tela).
  Quando/se um framework entrar, o mapeamento é direto em vez de adivinhado.
- As duas cascas ficam simétricas: `Application` ↔ `iOSApp`.

**Contra (aceito):**
- O Android ganha uma classe `Application` própria e uma linha no manifesto —
  que o clone precisa renomear junto com o resto do pacote.
- No iOS o grafo é repassado explicitamente pela hierarquia SwiftUI
  (`iOSApp` → `ContentView` → `ComposeView`). É verboso, e é o preço já aceito
  no ADR 0001 pelo repasse manual.

**Passa a ser proibido:**
- Construir `AppGraph` em `Activity`, `Fragment`, composable, `ContentView` ou
  qualquer coisa que a plataforma possa recriar.
- Guardar no grafo qualquer objeto cujo tempo de vida deva ser o da tela — o
  grafo vive o processo inteiro, então isso é vazamento por construção.

**Gatilho de reavaliação:** quando aparecer uma dependência que precise de um
escopo intermediário (viva mais que uma tela e menos que o processo — sessão de
usuário logado, sessão de pareamento de dispositivo). Aí o grafo manual passa a
ter três escopos, e o custo/benefício do framework muda: reabrir o gatilho do
ADR 0001 com esse dado.

## Supersede / Superseded by

Complementa o **ADR 0001** (que decidiu forma e lugar do grafo, não o tempo de
vida). Não o supersede: as decisões do 0001 seguem valendo.
