# SDD 01 — Visão geral

## Propósito

Este repositório é um **template de fundação de engenharia** para apps Kotlin
Multiplatform (Android + iOS, UI em Compose Multiplatform). Ele não é um
produto: é a base que um app real herda ao ser clonado — documentação viva,
quality gates e arquitetura por feature já validados e funcionando.

O primeiro app previsto a nascer deste template é a reconstrução do ecossistema
de smart home hoje prototipado no repositório `imou-smart-home` (câmeras via SDK
Imou/Dahua, IoT via SDK Tuya, backend próprio como middleware). As convenções
deste template foram extraídas do que já foi aprendido — e pago — naquele
projeto.

## Fluxo de vida do template

1. **Validar aqui**: toda mudança de fundação (gate, convenção, estrutura) é
   experimentada e provada neste repositório.
2. **Clonar**: um app real nasce clonando o template (checklist no README raiz).
3. **Retroalimentar**: aprendizado do app real que sirva a projetos futuros
   volta para cá via RFC/ADR.

## Requisitos de engenharia (invariantes)

- **KMP**: lógica compartilhada em `commonMain`; código de plataforma só onde a
  plataforma exige (`androidMain`/`iosMain` + ilhas nativas documentadas no SDD 02).
- **Cobertura mínima de 80% de linha** no código de lógica (Kover, gate local
  que falha o build). Pacotes `*.ui.*` ficam fora da medição — em contrapartida,
  é proibido ter lógica neles (style guide).
- **Análise estática**: Detekt com formatting (ktlint); zero issues tolerados.
- **Tudo local**: os gates rodam em `./gradlew check`, sem depender de CI.
  (CI/CD é etapa futura; ver `docs/tech-debt.md`.)
- **Documentação viva**: decisões passam por RFC → ADR e o SDD reflete o estado
  atual (processo em `docs/README.md`).
