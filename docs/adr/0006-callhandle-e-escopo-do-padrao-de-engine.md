---
id: ADR-0006
title: CallHandle e o escopo real do padrão de engine nativa
status: Approved
author: Chirstian Alexandre
date: 2026-08-12
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# ADR 0006 — CallHandle e o escopo real do padrão de engine nativa

## Contexto

Duas coisas motivaram este ADR, ambas vindas de uma pergunta em review: *"por
que a engine Swift tem toda uma camada de cancelamento? Não é só chamar e
retornar?"*.

**Primeiro, o custo era real.** O `Cancellable` do ADR 0004 obrigava cada engine
Swift a declarar uma classe só para embrulhar o cancelamento — nove linhas por
engine, porque um `@protocol` Objective-C não aceita closure.

**Segundo, o ADR 0004 vendeu como correção óbvia o que era uma generalização
deliberada.** O `imou-smart-home`, projeto de referência deste template, usa
`cancel()` na **engine**, não handle por chamada
(`NetconfigEngine.kt` + `ImouBleNetconfigEngine.swift`). Lá isso é seguro porque
netconfig é singleton por natureza: provisiona-se uma câmera por vez, de uma
tela só. O bug de concorrência que o ADR 0004 descreve é real, mas não é um bug
que o imou tenha tido — é um que o app alvo *pode* ter numa tela de lista.

Pesquisa feita antes de decidir (2026-08-12):

| Fonte | O que diz |
| --- | --- |
| [kotlin-swift-interopedia](https://github.com/kotlin-hands-on/kotlin-swift-interopedia/blob/main/docs/coroutines/Suspend%20functions.md) | A exportação de `suspend` para Swift **não tem cancelamento**: *"even if the Task is cancelled, nothing happens"*. Cancelamento precisa estar no contrato. |
| SKIE / KMP-NativeCoroutines | Resolvem o sentido **Kotlin → Swift** (chamar `suspend` do Swift). Não se aplicam a Swift implementando contrato Kotlin. |
| [KaMPKit](https://github.com/touchlab/KaMPKit/blob/main/docs/GENERAL_ARCHITECTURE.md) | Valida a forma geral (interface Kotlin, impl Swift, injeção pela casca), mas suas dependências de plataforma são síncronas — não cobre cancelamento. |
| [Kable](https://github.com/JuulLabs/kable) (BLE KMP em produção) | Cancelamento por **tempo de vida de objeto**: um `Peripheral` por dispositivo, `close()` derruba o escopo. A unidade de cancelamento é um objeto por alvo, nunca uma engine compartilhada. |

Não existe padrão oficial documentado para este sentido específico. A evidência
é por analogia — Kable e os idiomas nativos das duas plataformas
(`CancellationSignal` no Android, `AnyCancellable` no Combine, ambos handles por
operação).

Comparando o custo real em Swift, a conclusão inverteu a intuição:

| Forma | Estado na engine Swift |
| --- | --- |
| `cancel()` de engine (imou) | `cancelled`, `pending`, `gateTimeout`, `matchedDevice` + guarda em cada callback |
| Handle por chamada + helper | nenhum |

O cancelamento de engine custou **mais** código Swift no imou do que um handle
custaria. A complexidade percebida não vinha do `Cancellable`, e sim da classe
Swift que ele obrigava a existir.

## Decisão

**Mantemos o handle por chamada e adicionamos `CallHandle` em `commonMain`.**

```kotlin
class CallHandle(private val onCancel: () -> Unit) : Cancellable {
    override fun cancel() = onCancel()
}
```

- Kotlin/Native exporta tipos de função como closures Swift, então o Swift
  passa a escrever `return CallHandle(onCancel: { work.cancel() })` — some a
  classe embrulho. Verificado compilando e linkando o app iOS.
- `Cancellable` e `CallHandle` saem de `GreetingEngine.kt` para
  `core/platform/Cancellable.kt`: são infraestrutura genérica, enquanto a
  `GreetingEngine` é exemplo. Isso também facilita o clone.

**O padrão de engine não é para toda ida ao nativo.** O escopo, com os critérios
e a tabela de cenários, está no SDD 03 — resumido: interface + callback quando o
resultado é **assíncrono**; `Cancellable` de verdade quando o trabalho nativo
**continua consumindo algo** se ninguém o parar (rádio, câmera, sessão de SDK,
requisição em voo).

## Consequências

**A favor:**
- Custo por engine Swift cai de nove linhas para uma.
- Concorrência segura por construção — sem regra que o time precise lembrar.
- O trade-off em relação ao imou fica registrado como escolha, não como fato.

**Contra (aceito):**
- Divergimos do projeto de referência. Quem vier do imou vai estranhar, e é por
  isso que este ADR existe.
- Uma alocação por chamada. Irrelevante nesta escala.

**Passa a ser proibido:**
- Engine Swift declarando classe própria de `Cancellable` — use `CallHandle`.
- Aplicar o padrão de engine a dado de plataforma síncrono; isso é
  `expect/actual` (SDD 03).

**Gatilho de reavaliação:** se aparecer um SDK cujo cancelamento seja global
(sem token por request), a engine que o encapsula não consegue honrar o
contrato. Nesse caso, uma instância de engine por operação — modelo do Kable —
em vez de relaxar a interface.

## Supersede / Superseded by

Complementa o **ADR 0004**, que segue valendo na decisão (handle por chamada) e
fica corrigido aqui quanto à justificativa: é generalização deliberada em
relação ao imou, não correção de um defeito que ele tivesse.
