# Guidelines 01 — Style guide

## Formatação e análise estática

- **Detekt** com o ruleset **formatting** (ktlint embutido) é a fonte única de
  verdade de estilo. Config: `detekt.yml` na raiz, construída sobre os defaults
  (`buildUponDefaultConfig`). `maxIssues: 0` — qualquer issue falha o build.
- Roda em `./gradlew check` (via `QualityConventionPlugin`); para só o lint:
  `./gradlew detekt`.
- `.editorconfig` na raiz mantém IDE e ktlint alinhados (4 espaços, linha de 120).
- Exceções pontuais usam `@Suppress` **com justificativa no código**; exceções
  recorrentes viram regra no `detekt.yml` via PR.

## Testes

- **Fakes, não mocks.** Um Fake é uma implementação real e mínima da interface
  (ex.: `FakeMessageRepository` em `commonTest`), com estado inspecionável.
  Frameworks de mock (MockK etc.) não entram no projeto: acoplam o teste à
  implementação, quebram em refactors seguros e não existem em Kotlin/Native.
- Testes vivem em `commonTest` sempre que a lógica é comum — rodam em JVM e iOS.
- Estrutura: Arrange / Act / Assert, nomes em camelCase descrevendo o
  comportamento (`loadEmiteContentComAMensagemDoUseCase`).
- **Cobertura ≥ 80% de linha** (Kover, gate local). O que conta: `domain`,
  `data`, `presentation`, `core` (exceto `core/ui`). O que não conta: `*.ui.*`
  e entry points de plataforma.
- **Corolário: lógica não vive em `ui/`.** Composable é casca fina — renderiza
  estado e delega ações ao ViewModel. Condicional de negócio num composable
  é defeito de review, porque escapa do gate.
- ViewModels são testados com `Dispatchers.setMain(StandardTestDispatcher())`
  no `@BeforeTest` (+ `resetMain` no `@AfterTest`) — ver `WelcomeViewModelTest`.

## Arquitetura (regras de dependência)

- Feature **não** importa de outra feature; só de `core/`. Composição entre
  features acontece no `AppGraph` e na navegação.
- `domain` é Kotlin puro; `data` implementa as portas do domain; `ui` só
  consome `presentation`. Detalhes no SDD 02.

## Branches e commits

- Branches: `<tipo>/<ticket>-<descricao-curta>` — ex.:
  `feat/APP-123-netconfig-ble`, `fix/APP-456-token-refresh`.
  Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`.
- Commits: **Conventional Commits** — `tipo(escopo): descrição no imperativo`.
  Ex.: `feat(welcome): adiciona estado de loading na tela inicial`.
  O corpo explica o *porquê* quando não for óbvio; o diff já mostra o *o quê*.
- PR pequeno e focado; descrição aponta RFC/ADR quando a mudança decorre de um.
