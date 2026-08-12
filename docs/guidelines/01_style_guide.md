# Guidelines 01 — Style guide

## Formatação e análise estática

- **Detekt** com o ruleset **ktlint** embutido é a fonte única de verdade de
  estilo no Kotlin. Config: `detekt.yml` na raiz, construída sobre os defaults
  (`buildUponDefaultConfig`). O gate é `failOnSeverity = Info` — qualquer issue
  falha o build. (O antigo bloco `build: maxIssues:` não existe no Detekt 2.)
- Roda em `./gradlew check` (via `QualityConventionPlugin`), inclusive sobre os
  convention plugins em `build-logic/`; para só o lint: `./gradlew detekt`.
- **SwiftLint** faz o mesmo papel no código Swift (`.swiftlint.yml` na raiz,
  modo `--strict`). Não passa pelo Gradle: roda em `make lint` e no pre-commit.
- `.editorconfig` na raiz mantém IDE e ktlint alinhados (4 espaços, linha de 120).
- Exceções pontuais usam `@Suppress` **com justificativa no código**; exceções
  recorrentes viram regra no `detekt.yml` via PR.

## Idioma

- **Código em inglês**, sem exceção: nomes de classe, função, variável,
  **nomes de teste** e dados de teste. Base de código mista envelhece mal e
  produz híbridos como `devolveAMensagemDoRepositorio`.
- **Prosa em português**: comentários, `docs/`, mensagens de commit e de PR.
  O time é o leitor, e a precisão vale mais que a uniformidade aqui.
- **Textos de UI em português** — são conteúdo de produto, não código (quando
  houver i18n, viram chaves em inglês apontando para traduções).

## Comentários

Comentário existe para dizer **o que o código não consegue mostrar** — uma
restrição, uma armadilha, um "por que assim e não do jeito óbvio". Não para:

- repetir o nome do símbolo (`/** Entidade de domínio */` sobre uma data class
  em `domain/`);
- descrever a linha seguinte;
- explicar arquitetura. Isso vive no SDD e nos ADRs, e **duplicar é pior que
  omitir**: as duas cópias divergem, e a do código envelhece primeiro.

Quando a explicação for arquitetural, aponte em vez de repetir
(`// ver ADR 0001`). Os convention plugins em `build-logic/` são a exceção
legítima: ali os comentários registram armadilhas de versão do AGP/Gradle que
não estão documentadas em nenhum outro lugar do repositório.

## Testes

- **Fakes, não mocks.** Um Fake é uma implementação real e mínima da interface
  (ex.: `FakeMessageRepository` em `commonTest`), com estado inspecionável.
  Frameworks de mock (MockK etc.) não entram no projeto: acoplam o teste à
  implementação, quebram em refactors seguros e não existem em Kotlin/Native.
- Testes vivem em `commonTest` sempre que a lógica é comum — rodam em JVM e iOS.
- Estrutura: Arrange / Act / Assert, nomes em camelCase descrevendo o
  comportamento (`loadsOnCreationWithoutTheScreenAskingForIt`).
- **Cobertura ≥ 80% de linha** (Kover, gate local). O que conta: `domain`,
  `data`, `presentation`, `core` (a UI vive em `ui/`, fora da medição). O que não conta: `*.ui.*`
  e entry points de plataforma.
- **Corolário: lógica não vive em `ui/`.** Composable é casca fina — renderiza
  estado e delega ações ao ViewModel. Condicional de negócio num composable
  é defeito de review, porque escapa do gate.
- ViewModels são testados com `Dispatchers.setMain(StandardTestDispatcher())`
  no `@BeforeTest` (+ `resetMain` no `@AfterTest`) — ver `WelcomeViewModelTest`.

## Arquitetura (regras de dependência)

- Feature **não** importa de outra feature; só de `core/`. Composição entre
  features acontece no `AppGraph` e na navegação.
- **Só `di/` (o topo) conhece features — `core/` nunca.** `core/` é
  infraestrutura consumida pelas features; inverter isso cria ciclo quando as
  features virarem módulos.
- Cada feature monta a si mesma em `features/<nome>/di/<Nome>Graph.kt`.
  **ViewModel é factory, nunca instância guardada no grafo** (ADR 0001).
- **O `AppGraph` é construído pela casca da aplicação** (`Application` /
  `iOSApp`), nunca por uma Activity, composable ou `ContentView` — o que a
  plataforma pode recriar não pode ser dono do composition root (ADR 0003).
- `domain` é Kotlin puro (entidades e portas); `data` implementa as portas; `ui`
  só consome `presentation`. Detalhes no SDD 02.
- **Sem camada de use case** (ADR 0005): o ViewModel depende da porta de
  repositório. Operação que só repassaria não vira classe. Regra nova que não
  couber nem no ViewModel nem no repositório nasce como **função pura** em
  `domain/`.

## Telas (padrão que toda feature copia)

- **Carga inicial no `init` do ViewModel**, não em `LaunchedEffect`: o ViewModel
  sobrevive à recriação da Activity, a composição não — disparar pela tela
  refaz a busca a cada rotação.
- Ação que dispara trabalho **ignora reentrância** enquanto há uma execução em
  voo (`if (job?.isActive == true) return`); o estado de Loading já comunica.
- Colete com **`collectAsStateWithLifecycle`**, não `collectAsState`.

## Fronteira nativa

- Interface exportada para o Swift: sem `suspend`, sem `sealed`/genérico na
  assinatura, callback explícito.
- **Cancelamento é por chamada** — `start(...)` devolve um `Cancellable`. Engine
  com `cancel()` global, ou guardando "a chamada atual" num campo, é defeito de
  review: cancela a chamada dos outros (ADR 0004).
- O Kotlin nunca chama a interface direto — sempre pelo adapter `suspend`.

## Branches e commits

- Branches: `<tipo>/<ticket>-<descricao-curta>` — ex.:
  `feat/APP-123-netconfig-ble`, `fix/APP-456-token-refresh`.
  Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`.
- Commits: **Conventional Commits** — `tipo(escopo): descrição no imperativo`.
  Ex.: `feat(welcome): adiciona estado de loading na tela inicial`.
  O corpo explica o *porquê* quando não for óbvio; o diff já mostra o *o quê*.
- PR pequeno e focado; descrição aponta RFC/ADR quando a mudança decorre de um.
