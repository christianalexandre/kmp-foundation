# SDD 02 — Arquitetura

## Organização: package-by-feature

Um módulo Gradle (`composeApp`), organizado por **feature**, não por camada:

```
com/kmpfoundation/
├── core/                  # transversal: di, ui raiz; depois navigation, design system, network
│   ├── di/AppGraph.kt     # composition root (DI manual)
│   └── ui/App.kt          # raiz Compose (tema + casca de navegação)
└── features/
    └── welcome/           # feature-exemplo (deletar no clone)
        ├── domain/        # entidades, portas (interfaces) e use cases — Kotlin puro
        ├── data/          # implementações das portas (HTTP, banco, SDK nativo)
        ├── presentation/  # state holders: TODA a lógica de apresentação
        └── ui/            # composables: casca fina sobre o state holder
```

**Regras de dependência** (verificadas em review; enforcement automático é
dívida registrada):

1. `domain` não importa de `data`, `presentation` nem `ui`. Kotlin puro.
2. `data` implementa as portas de `domain`; não conhece `presentation`/`ui`.
3. `ui` só consome `presentation`. Lógica em `ui/` é defeito de review.
4. **Feature não importa de outra feature** — só de `core/`. Composição entre
   features acontece no `AppGraph`/navegação. É esta regra que torna a extração
   de um módulo Gradle futuro um "mover pasta".

## Injeção de dependência: manual

Sem framework. O grafo é código explícito em `core/di/AppGraph.kt` (padrão
herdado do `di/ImouGraph.kt` do imou-smart-home):

- Dependências entram por **construtor**; defaults de produção, Fakes nos testes.
- O `AppGraph` é o único lugar que conhece todas as features.
- Dependências de plataforma (engines nativas, players) são criadas em
  `MainActivity`/`MainViewController` e injetadas no grafo.

## Fronteira KMP ↔ nativo (convenções herdadas do imou-smart-home)

Estas convenções foram provadas no projeto imou-smart-home e valem como padrão:

### Interface Swift-friendly para engines nativas

Quando o iOS precisa implementar um contrato Kotlin (engine de netconfig,
player, scanner), a interface em `commonMain` **não usa `suspend` nem `sealed`
nas assinaturas** — Swift não os consome bem via Objective-C header. O padrão é:

```kotlin
interface SomeEngine {
    val isAvailable: Boolean
    fun start(request: Request, callback: Callback)   // callback explícito
    fun cancel()
}
suspend fun SomeEngine.execute(request: Request): Result = // adapter p/ Kotlin
    suspendCancellableCoroutine { /* callback → continuation; cancel() no cancel */ }
```

O Swift conforma direto ao protocolo exportado no framework; o lado Kotlin usa
o adapter `suspend`. Precedente: `NetconfigEngine` no imou-smart-home.

### Navegação: pilha única, bottom bar só na raiz

`Navigator` mantém `List<Route>`; a aba selecionada é parâmetro da rota raiz.
A tab bar é desenhada em Compose na tela raiz — o iOS usa `UINavigationController`
puro, sem `UITabBarController`. Navegar para frente empilha e esconde a barra.

### Ilhas nativas

Superfícies onde o SDK de plataforma é dono do rendering/gestos (ex.: player de
vídeo) são **nativas por plataforma**, integradas ao Compose como ilhas com
contrato Kotlin (interface Swift-friendly acima). O restante do app permanece
Compose em `commonMain`.

## Caminho de modularização (quando o time/projeto crescer)

O template nasce com um módulo por simplicidade, mas preparado para dividir:

1. Criar `features/<nome>/` como módulo Gradle aplicando os convention plugins
   (`kmpfoundation.kmp.module` + `kmpfoundation.quality`) — os gates vêm juntos.
2. Mover a pasta da feature (a regra 4 garante que não há imports cruzados).
3. **iOS**: novos módulos precisam de `export()` no framework umbrella do
   `composeApp` para seus tipos aparecerem no header Objective-C.
4. `core/` se divide por último (`core:navigation`, `core:design`, …).

## Quality gates (contrato)

| Gate | Ferramenta | Regra | Onde |
| --- | --- | --- | --- |
| Cobertura | Kover | ≥ 80% de linha; `*.ui.*` e entry points fora da medição | `QualityConventionPlugin` |
| Estática/formato | Detekt (+formatting) | zero issues (`maxIssues: 0`) | `detekt.yml` raiz |

Ambos falham `./gradlew check` localmente.
