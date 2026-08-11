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
- **Só `di/` (o topo) conhece features — `core/` nunca.** `core/` é
  infraestrutura consumida pelas features; inverter isso cria ciclo quando as
  features virarem módulos.
- Cada feature monta a si mesma em `features/<nome>/di/<Nome>Graph.kt`.
  **ViewModel é factory, nunca instância guardada no grafo** (ADR 0001).
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
