---
id: ADR-0002
title: Tratamento de erro com resultado tipado
status: Approved
author: Chirstian Alexandre
date: 2026-08-11
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# ADR 0002 — Tratamento de erro com resultado tipado

## Contexto

Até aqui nenhuma operação do template podia falhar, então cada feature do app
real inventaria o próprio dialeto: exceção em uma, `null` em outra,
`kotlin.Result` numa terceira. O app alvo tem fontes de falha abundantes e
heterogêneas — rede, SDKs nativos (câmera, BLE, IoT), permissões negadas,
sessão expirada.

As opções consideradas:

| Opção | Problema |
| --- | --- |
| Exceções | Invisíveis na assinatura; o compilador não ajuda a lembrar de tratar. Em Kotlin/Native, exceção não capturada mata o app. |
| `null` | Perde a razão da falha — a UI não consegue distinguir "sem rede" de "não existe". |
| `kotlin.Result` | Não tipa o erro (`Throwable`), então o `when` exaustivo é impossível e volta-se a `instanceof`. |

## Decisão

**`Outcome<T>` selado com erro tipado (`AppError`), em `core/result/`.**

- Toda operação que pode falhar devolve `Outcome<T>` — a assinatura declara a
  falha, e o `when` exaustivo é garantido pelo compilador.
- `AppError` é selado e **fechado por comportamento de UI, não por causa
  técnica**: um caso existe quando a tela reage a ele de forma diferente
  (`Connectivity` oferece "tentar de novo", `Unauthorized` leva ao login).
  Erro que não muda o comportamento da tela não merece um caso próprio.
- **A tradução erro → texto acontece no ViewModel**, nunca na tela. É decisão de
  produto sobre cada falha, e assim fica coberta por teste.
- A camada `data` traduz o erro da fonte (HTTP, SDK nativo) para `AppError`; o
  `domain` propaga sem reinterpretar.

## Consequências

**A favor:**
- Esquecer de tratar uma falha vira erro de compilação, não bug em produção.
- Os textos de erro são testáveis sem UI — há teste garantindo que cada
  `AppError` produz uma mensagem distinta.
- Kotlin/Native não recebe exceção atravessando a fronteira nativa.

**Contra (aceito):**
- Mais verboso que exceções no caminho feliz; exige `map`/`when` para compor.
- Adicionar um caso a `AppError` quebra a compilação de todos os `when`
  exaustivos — o que é o ponto, mas custa uma passada por todas as features.

**Passa a ser proibido:**
- Lançar exceção como fluxo de controle entre camadas.
- Construir texto de erro dentro de composable.

**Gatilho de reavaliação:** se o `when` de tradução começar a se repetir
idêntico em muitas features, extrair um mapeamento padrão em `core/` e deixar a
feature sobrescrever só o que difere.
