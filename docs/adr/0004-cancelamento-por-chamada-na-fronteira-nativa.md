---
id: ADR-0004
title: Cancelamento por chamada na fronteira nativa
status: Approved
author: Chirstian Alexandre
date: 2026-08-11
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# ADR 0004 — Cancelamento por chamada na fronteira nativa

## Contexto

O SDD 02 define o formato das engines nativas: interface em `commonMain` sem
`suspend` e sem tipos selados na assinatura (para sobreviver à exportação
Objective-C), callback explícito, e um adapter `suspend` do lado Kotlin que liga
o cancelamento da coroutine ao cancelamento da engine.

A primeira versão modelava o cancelamento como um método da **engine**:

```kotlin
interface GreetingEngine {
    fun greet(name: String, callback: GreetingCallback)
    fun cancel()          // ← cancela "o que estiver pendente"
}
```

E o adapter fazia `continuation.invokeOnCancellation { cancel() }`.

O defeito: a engine é **uma instância compartilhada** (vive no composition root,
ADR 0003), mas `cancel()` tem semântica de chamada. Com duas chamadas
concorrentes na mesma engine, cancelar uma cancela a outra. As implementações
confirmavam o problema por construção — `AndroidGreetingEngine` guardava um
único `pending: Runnable?`, sobrescrito a cada `greet`; o `SwiftGreetingEngine`
guardava um único `DispatchWorkItem`. Efeitos colaterais:

1. cancelar a chamada A cancela silenciosamente a chamada B em voo;
2. a chamada B, ao sobrescrever `pending`, tornava a A incancelável;
3. o `Runnable`/`WorkItem` nunca era liberado após executar — o callback (e por
   tabela a continuation, e o que ela capturar) ficava retido.

Nada disso aparece no template, que faz uma chamada por vez. Aparece no app
alvo, onde uma tela lista dispositivos e dispara N operações concorrentes contra
o mesmo SDK. É um bug de concorrência — o tipo que não falha em teste e falha em
produção.

## Decisão

**`greet` devolve um handle de cancelamento daquela chamada. A engine não tem
método de cancelamento.**

```kotlin
interface GreetingEngine {
    val isAvailable: Boolean
    fun greet(name: String, callback: GreetingCallback): Cancellable
}

interface Cancellable {
    fun cancel()
}
```

- O handle é criado **pela engine, por chamada**, e é ele quem retém o estado
  daquela operação (`Runnable`, `DispatchWorkItem`, id de request do SDK).
- **A engine não guarda estado de chamada.** É a regra que torna o problema
  impossível de reintroduzir: sem campo compartilhado, não há o que sobrescrever.
- No adapter, `invokeOnCancellation` é registrado **depois** de obter o handle.
  Se a coroutine já tiver sido cancelada nesse meio-tempo, o próprio
  `invokeOnCancellation` dispara na hora — nenhuma chamada nativa fica órfã.
- `Cancellable` é um tipo plano de um método só, então atravessa o header
  Objective-C sem ponte (o Swift devolve uma classe que conforma ao protocolo).

## Consequências

**A favor:**
- Chamadas concorrentes contra a mesma engine são independentes — o padrão
  aguenta a tela de lista do app alvo.
- O estado por chamada morre com o handle; some a retenção do callback.
- É testável em `commonTest` sem device: `FakeGreetingEngine` registra as
  chamadas e cada uma responde/cancela por conta própria
  (`cancellingOneCallLeavesConcurrentCallsRunning`).

**Contra (aceito):**
- Uma alocação por chamada (o handle). Irrelevante nesta escala.
- Implementar a engine ficou marginalmente mais verboso nos dois lados: é
  preciso criar o objeto de handle em vez de guardar um campo.

**Passa a ser proibido:**
- Engine com método `cancel()` global, ou com campo guardando "a chamada atual".
- Chamar a interface nativa direto do Kotlin sem passar pelo adapter `suspend` —
  é ele que amarra o handle ao ciclo de vida da coroutine.

**Gatilho de reavaliação:** se algum SDK nativo só oferecer cancelamento global
(sem token/id por request), a engine que o encapsula não conseguirá honrar este
contrato. Nesse caso, isolar **uma instância de engine por operação** em vez de
relaxar a interface — e registrar aqui o SDK que forçou isso.

## Supersede / Superseded by

Refina o padrão de fronteira nativa descrito no SDD 02, que passa a documentar
esta forma.
