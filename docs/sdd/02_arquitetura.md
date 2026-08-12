# SDD 02 — Arquitetura

## Módulos Gradle

| Módulo | Plugin Android | Papel |
| --- | --- | --- |
| `composeApp` | `com.android.kotlin.multiplatform.library` | Lógica e UI compartilhadas (KMP): Android library + framework iOS. É onde vivem os gates. |
| `androidApp` | `com.android.application` | Casca Android: `Application` (dona do composition root) + `MainActivity` + manifesto. Sem lógica. |
| `iosApp` | — (XcodeGen) | Casca iOS: `iOSApp` (dona do composition root) + engines nativas em Swift + a view que hospeda o `MainViewController`. |

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
        ├── domain/        # entidades e portas (interfaces) — Kotlin puro, sem use cases
        ├── data/          # implementações das portas (HTTP, banco, SDK nativo)
        ├── presentation/  # ViewModels: TODA a lógica de apresentação
        ├── ui/            # composables: casca fina sobre o ViewModel
        └── di/            # grafo da feature: monta as próprias dependências
```

**Regras de dependência** (verificadas em review; enforcement automático é
dívida registrada):

1. `domain` não importa de `data`, `presentation` nem `ui`. Kotlin puro.
2. `data` implementa as portas de `domain`; não conhece `presentation`/`ui`.
3. `presentation` depende da **porta** de `domain`, não da implementação —
   **sem camada de use case** entre os dois (ADR 0005). Operação que só
   repassaria não vira classe.
4. `ui` só consome `presentation`. Lógica em `ui/` é defeito de review.
5. **Feature não importa de outra feature** — só de `core/`. Composição entre
   features acontece no `AppGraph`/navegação. É esta regra que torna a extração
   de um módulo Gradle futuro um "mover pasta".

## Injeção de dependência: manual, com grafo por feature

Sem framework — decisão registrada no **ADR 0001**, com o gatilho para
reavaliar. O grafo é código explícito, em dois níveis:

- **`features/<nome>/di/<Nome>Graph.kt`** — único lugar que conhece as tripas da
  feature (repositório, ViewModel). Dependências compartilhadas entram por
  construtor.
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

### Escopos (quem vive quanto tempo) — ADR 0003

O template tem **dois escopos explícitos**, e a distinção é deliberada:

| Escopo | Dono | O que vive nele |
| --- | --- | --- |
| Aplicação | `KmpFoundationApplication` (Android) / `iOSApp` (iOS) | O `AppGraph` e tudo que ele segura: cliente HTTP, banco, cache, engines/SDKs nativos. Uma vez por processo. |
| Tela | `viewModel { }` (ViewModelStore da plataforma) | ViewModels e o estado da tela. |

As dependências de plataforma são construídas pela casca, **junto do grafo** —
não na `Activity`/`ContentView`. Construir o `AppGraph` em algo que a plataforma
possa recriar (Activity, composable, `ContentView`) é proibido: reconstruiria
conexões e sessões a cada rotação.

## Camada de apresentação: ViewModel multiplataforma

`presentation/` usa o **ViewModel da JetBrains** (`org.jetbrains.androidx.lifecycle`,
mesmo pacote `androidx.lifecycle` em `commonMain`) com `viewModelScope`:

- No Android, sobrevive à recriação da Activity; com navegação, fica escopado
  ao destino (motivo de tê-lo adotado já na POC).
- **Divisão de papéis com o DI manual**: o grafo da feature monta o objeto
  (factory `WelcomeGraph.viewModel()`), a UI o registra no ciclo de vida com
  `viewModel { graph.welcome.viewModel() }` — o grafo nunca guarda ViewModel.
- **A carga inicial nasce no `init` do ViewModel**, não num `LaunchedEffect` da
  tela. O ViewModel sobrevive à recriação da Activity, a composição não:
  disparar pela tela refaz a busca a cada rotação. Pelo mesmo motivo, `load()`
  ignora chamadas enquanto já há uma em voo (retry não empilha buscas).
- A tela coleta com **`collectAsStateWithLifecycle`**, não `collectAsState`:
  sem isso a coleta continua com o app em background.
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
    fun start(request: Request, callback: Callback): Cancellable  // handle POR CHAMADA
}
interface Cancellable { fun cancel() }

suspend fun SomeEngine.execute(request: Request): Outcome<T> =  // adapter p/ Kotlin
    suspendCancellableCoroutine { /* callback → continuation; handle.cancel() no cancel */ }
```

O cancelamento é **por chamada, nunca da engine** — ADR 0004. A engine é
compartilhada (vive no escopo de aplicação), então um `cancel()` nela teria
semântica errada: cancelaria as chamadas dos outros. Corolário obrigatório:
**a engine não guarda estado de chamada**; quem guarda é o handle.

O Swift conforma direto ao protocolo exportado no framework
(`iosApp/iosApp/GreetingEngine.swift`); o lado Kotlin usa o adapter `suspend`,
que amarra o handle daquela chamada ao ciclo de vida da coroutine.

**Quem constrói a engine é a casca da plataforma**
(`KmpFoundationApplication` / `iOSApp`), que a injeta no `AppGraph` — só ela
conhece o SDK. Em `commonTest`, um Fake da engine permite testar toda a lógica
dependente de plataforma sem device nem simulador, inclusive o comportamento
sob chamadas concorrentes.

### Qual dos dois mecanismos usar

São **dois, e a escolha é objetiva**:

| Situação | Mecanismo |
| --- | --- |
| Dado de plataforma, síncrono, sem ciclo de vida (versão do SO, locale) | `expect/actual` — `core/platform/Platform.kt` |
| Trabalho assíncrono de um SDK nativo, com cancelamento | Interface + callback + `Cancellable` — `GreetingEngine` |

A engine devolve **só o que é específico da plataforma**; texto de produto e
composição ficam em `commonMain` (ver `PlatformMessageRepository`, que junta a
saudação da engine com o `platformName()`). Se cada engine montasse a string
final, a mesma decisão de produto existiria duas vezes — uma em Kotlin, outra em
Swift — e divergiria na primeira alteração.

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
2. Mover a pasta da feature (a regra 5 garante que não há imports cruzados).
3. **iOS**: novos módulos precisam de `export()` no framework umbrella do
   `composeApp` para seus tipos aparecerem no header Objective-C.
4. `core/` se divide por último (`core:navigation`, `core:design`, …).

## Quality gates (contrato)

| Gate | Ferramenta | Regra | Onde |
| --- | --- | --- | --- |
| Cobertura | Kover | ≥ 80% de linha; `*.ui.*` e entry points fora da medição | `QualityConventionPlugin` |
| Estática/formato (Kotlin) | Detekt (+ktlint) | zero issues (`failOnSeverity = Info`) | `Quality.kt` + `detekt.yml` raiz |
| Estática/formato (Swift) | SwiftLint | zero issues (`--strict`) | `.swiftlint.yml` raiz |

Kover e Detekt falham `./gradlew check` localmente — inclusive sobre os
convention plugins em `build-logic/`, que o `check` da raiz alcança pela
included build. O SwiftLint roda fora do Gradle (`make lint` / `make check` e o
pre-commit), porque o código Swift não passa pelo build do Gradle.
