---
id: ADR-0005
title: MVVM enxuto — o ViewModel fala com o repositório, sem camada de use case
status: Approved
author: Chirstian Alexandre
date: 2026-08-12
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# ADR 0005 — MVVM enxuto, sem camada de use case

## Contexto

O SDD 02 definia `domain/` como "entidades, portas (interfaces) e **use cases**",
e a feature-exemplo seguia isso à risca. O resultado, medido:

- **8 saltos e ~12 tipos** para produzir uma string na tela;
- o `GetWelcomeMessageUseCase` inteiro era `repository.welcomeMessage()` —
  uma linha existindo só para a camada existir.

O problema não é a linha extra; é o que ela ensina. O template é lido como
referência, e a feature-exemplo é a única coisa que o leitor vê antes de copiar
o padrão. Um pass-through no exemplo ensina **cerimônia**, não arquitetura: cada
feature nova nasceria com um use case por operação, a maioria repassando.

A camada foi avaliada pelo que ela entregaria de fato neste projeto:

| O que um use case daria | Já temos onde |
| --- | --- |
| Reuso de regra entre telas | Não há duas telas ainda; quando houver, o repositório é compartilhável do mesmo jeito |
| Orquestrar duas fontes | Cabe no repositório, que é quem conhece as fontes |
| Ponto de teste isolado | O ViewModel já é testado com Fake de repositório, sem device |
| Manter o `domain` puro | Continua puro: entidade + porta, sem Android/iOS |

## Decisão

**MVVM enxuto: o ViewModel depende diretamente da porta de repositório.**

- `domain/` mantém **entidades e portas** (`WelcomeMessage`, `MessageRepository`)
  e segue Kotlin puro. O que sai é apenas a classe de use case.
- `presentation/` (ViewModel) consome a porta e concentra a lógica de
  apresentação — inclusive a tradução de `AppError` para texto (ADR 0002).
- `data/` implementa as portas e é onde a composição de fontes acontece.
- Regra prática para quem ficar em dúvida: **se a operação só repassa, não crie
  camada.** Se um dia uma regra precisar viver fora do ViewModel e fora do
  repositório, ela nasce como uma função pura em `domain/` — não como uma
  hierarquia de use cases.

Isso reduz o caminho da tela ao nativo de 8 para 7 saltos e tira 2 arquivos do
exemplo, sem mexer em nenhuma das regras de dependência do SDD 02.

## Consequências

**A favor:**
- Menos indireção para ler e para escrever; o exemplo passa a ensinar o formato
  em vez da cerimônia.
- Uma camada a menos para o clone replicar por engano em toda feature.

**Contra (aceito):**
- Lógica de aplicação que não caiba nem no ViewModel nem no repositório não tem
  um lugar óbvio pré-definido. A regra acima (função pura em `domain/`) cobre o
  caso sem reabrir a camada.
- ViewModels de features complexas podem crescer. Se crescerem além do
  razoável, o sintoma é falta de decomposição — não falta de use case.

**Passa a ser proibido:**
- Classe de use case que apenas repassa para o repositório.

**Gatilho de reavaliação:** quando a mesma regra de aplicação precisar ser
compartilhada por três ou mais ViewModels e não couber no repositório. Aí a
discussão volta — mas começando por função pura, e só virando camada se houver
evidência de que a função não basta.

## Supersede / Superseded by

Altera o desenho de camadas descrito no **SDD 02**, que passa a documentar
`domain/` como entidades e portas. Não afeta os ADRs 0001–0004.
