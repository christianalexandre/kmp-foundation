# SDD 02 — Arquitetura

## Módulos Gradle

| Módulo | Plugin Android | Papel |
| --- | --- | --- |
| `composeApp` | `com.android.kotlin.multiplatform.library` | Lógica e UI compartilhadas (KMP): Android library + framework iOS. É onde vivem os gates. |
| `androidApp` | `com.android.application` | Casca Android: `MainActivity` + manifesto. Sem lógica. |
| `iosApp` | — (XcodeGen) | Casca iOS: entry SwiftUI que hospeda o `MainViewController`. |

A separação entre `composeApp` e `androidApp` **é exigência do AGP 9**: um
módulo KMP não pode mais ser `com.android.application`, e o entry point Android
precisa de um módulo próprio. O efeito colateral é bom — as duas cascas
(Android e iOS) ficam simétricas em volta do mesmo núcleo compartilhado.

## Organização: package-by-feature

Dentro do `composeApp`, organização por **feature**, não por camada:

```
com/kmpfoundation/
├── di/AppGraph.kt         # composition root: o topo, único que conhece features
├── core/                  # transversal; depois navigation, design system, network
│   ├── platform/          # fronteira nativa: expect/actual e engines (ver abaixo)
│   ├── result/            # Outcome + AppError (ADR 0002)
│   └── ui/App.kt          # raiz Compose (tema + casca de navegação)
└── features/
    └── welcome/           # feature-exemplo (deletar no clone)
        ├── domain/        # entidades, portas (interfaces) e use cases — Kotlin puro
        ├── data/          # implementações das portas (HTTP, banco, SDK nativo)
        ├── presentation/  # ViewModels: TODA a lógica de apresentação
        ├── ui/            # composables: casca fina sobre o ViewModel
        └── di/            # grafo da feature: monta as próprias dependências
```

**Regras de dependência** (verificadas em review; enforcement automático é
dívida registrada):

1. `domain` não importa de `data`, `presentation` nem `ui`. Kotlin puro.
2. `data` implementa as portas de `domain`; não conhece `presentation`/`ui`.
3. `ui` só consome `presentation`. Lógica em `ui/` é defeito de review.
4. **Feature não importa de outra feature** — só de `core/`. Composição entre
   features acontece no `AppGraph`/navegação. É esta regra que torna a extração
   de um módulo Gradle futuro um "mover pasta".

## Injeção de dependência: manual, com grafo por feature

Sem framework — decisão registrada no **ADR 0001**, com o gatilho para
reavaliar. O grafo é código explícito, em dois níveis:

- **`features/<nome>/di/<Nome>Graph.kt`** — único lugar que conhece as tripas da
  feature (repositório, use case, ViewModel). Dependências compartilhadas entram
  por construtor.
- **`di/AppGraph.kt`** — o composition root, no topo. Cresce **uma linha por
  feature**; cria as dependências compartilhadas e as repassa.

Regras que decorrem disso:

- O `AppGraph` mora **fora de `core/`**. `core/` é infraestrutura consumida pelas
  features; um grafo lá dentro criaria ciclo (`feature → core → feature`) assim
  que as features virarem módulos.
- **ViewModel é criado por factory, nunca guardado no grafo.** O ciclo de vida
  pertence ao `viewModel { }` da UI (ViewModelStore da plataforma). Guardar a
  instância no grafo vazaria estado entre telas.
- Dependências entram por **construtor**; defaults de produção, Fakes nos testes.
- Dependências de plataforma (engines nativas, players) são criadas em
  `MainActivity`/`MainViewController` e injetadas no `AppGraph`.

## Camada de apresentação: ViewModel multiplataforma

`presentation/` usa o **ViewModel da JetBrains** (`org.jetbrains.androidx.lifecycle`,
mesmo pacote `androidx.lifecycle` em `commonMain`) com `viewModelScope`:

- No Android, sobrevive à recriação da Activity; com navegação, fica escopado
  ao destino (motivo de tê-lo adotado já na POC).
- **Divisão de papéis com o DI manual**: o `AppGraph` monta o objeto (factory
  `welcomeViewModel()`), a UI o registra no ciclo de vida com
  `viewModel { graph.welcomeViewModel() }` — o grafo nunca guarda ViewModel.
- Nos testes, `Dispatchers.setMain(StandardTestDispatcher())` substitui o Main
  do `viewModelScope` (ver `WelcomeViewModelTest`); segue sem mocks, só Fakes.

## Tratamento de erro

`Outcome<T>` selado com `AppError` tipado, em `core/result/` — **ADR 0002**.
Operação que pode falhar declara isso na assinatura; a tradução de erro para
texto e ação acontece no ViewModel, nunca no composable.

## Fronteira KMP ↔ nativo

Convenções provadas no imou-smart-home e **executadas aqui** em
`core/platform/` — o exemplo é o `GreetingEngine`, deliberadamente trivial no
que faz e fiel no formato.

### Interface Swift-friendly para engines nativas

Quando o iOS precisa implementar um contrato Kotlin (engine de netconfig,
player, scanner), a interface em `commonMain` **não usa `suspend` nem `sealed`
nas assinaturas** — nenhum dos dois sobrevive bem à exportação para o header
Objective-C. O padrão, como está em `core/platform/GreetingEngine.kt`:

```kotlin
interface SomeEngine {
    val isAvailable: Boolean
    fun start(request: Request, callback: Callback)   // callback explícito
    fun cancel()
}
suspend fun SomeEngine.execute(request: Request): Outcome<T> =  // adapter p/ Kotlin
    suspendCancellableCoroutine { /* callback → continuation; cancel() no cancel */ }
```

O Swift conforma direto ao protocolo exportado no framework
(`iosApp/iosApp/GreetingEngine.swift`); o lado Kotlin usa o adapter `suspend`,
que também liga o cancelamento da coroutine ao `cancel()` da engine.

**Quem constrói a engine é a casca da plataforma** (`MainActivity` /
`MainViewController`), que a injeta no `AppGraph` — só ela conhece o SDK.
Em `commonTest`, um Fake da engine permite testar toda a lógica dependente de
plataforma sem device nem simulador.

O caso simples da fronteira (dado sem callback nem ciclo de vida) usa
`expect/actual` direto: ver `core/platform/Platform.kt`.

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
   (`kmpfoundation.kmp.module` + `kmpfoundation.quality`) — os gates e os
   targets vêm juntos; o módulo só declara o próprio `namespace`.
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
