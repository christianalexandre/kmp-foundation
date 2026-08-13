# kmp-foundation

Template de fundação de engenharia para apps **Kotlin Multiplatform + Compose
Multiplatform** (Android + iOS). Este repositório não é um app: é a base que um
app real herda ao ser clonado — documentação viva, quality gates locais e
arquitetura por feature já configurados.

## O que vem pronto

| Área | O quê |
| --- | --- |
| Documentação | `docs/` com RFC, ADR, SDD e guidelines (ciclo de vida em `docs/README.md`) |
| Quality gates | Kover (cobertura ≥ 80%) + Detekt/ktlint (código **e** convention plugins) em `./gradlew check`; SwiftLint no código Swift |
| Build | AGP 9 + Gradle 9.5, Version Catalog (`gradle/libs.versions.toml`) e convention plugins em `build-logic/` |
| Arquitetura | Package-by-feature (`core/` + `features/<nome>/{domain,data,presentation,ui}`), MVVM sem use case, DI manual + ViewModel multiplataforma. Mapa em `docs/sdd/03_mapa_de_codigo.md` |
| Navegação | Sem biblioteca: o container nativo é dono da pilha, uma raiz Compose por destino (ADR 0007). Rotas e política em `nav/`, testadas em `commonTest` |
| Exemplos | `features/devices/` cobre os três casos, sem repetição: navegação Compose → Compose, Compose → tela 100% nativa → Compose por cima dela, e chamada assíncrona à casca nativa com cancelamento real. Testes com Fakes |

## Pré-requisitos

| Ferramenta | Versão | Por quê |
| --- | --- | --- |
| **Android Studio** | **Quail 3 (2026.1.3) ou mais recente** | Cada versão do Studio suporta uma faixa de AGP: Quail 2/3 (2026.1.2+) são as que aceitam **AGP 9.3**. Panda 3 (2025.3.3) vai só até o AGP 9.1 — nele o sync falha com erros de índice (ex.: *"Failed to build cache for …klib"*), mesmo com a build por terminal verde. Confira a [matriz oficial](https://developer.android.com/build/releases/about-agp) antes de mudar o AGP. |
| JDK | 17+ | Exigido pelo AGP 9. |
| XcodeGen | qualquer | `brew install xcodegen`, só para gerar o projeto iOS. |
| SwiftLint | qualquer | Gate do código Swift. Vem no `make bootstrap` (Brewfile). |

> A build por linha de comando não depende do Android Studio — o wrapper do
> Gradle resolve tudo. O requisito acima é para trabalhar dentro do IDE.

## Módulos

| Módulo | Papel |
| --- | --- |
| `composeApp` | Lógica e UI compartilhadas (KMP): Android library + framework iOS. É onde vivem os gates. |
| `androidApp` | Casca Android: `Application`, `HostActivity` (host de um destino Compose), as telas nativas e as engines nativas em Kotlin. Sem lógica de negócio — o AGP 9 exige que o app seja um módulo separado do módulo KMP. |
| `iosApp` | Casca iOS: SwiftUI + XcodeGen, o `RootCoordinator` (dono da pilha), as telas nativas e as engines nativas em Swift. |

> `androidMain` é par de `iosMain` (source sets do módulo compartilhado), **não**
> de `iosApp`. O mapa completo de onde cada coisa fica está em
> [`docs/sdd/03_mapa_de_codigo.md`](docs/sdd/03_mapa_de_codigo.md).

## Comandos

Tudo passa pelo `make` — rode `make` (ou `make help`) para a lista completa.

```sh
make bootstrap    # instala ferramentas, instala os git hooks e gera o projeto iOS
make check        # gate completo: testes (JVM + iOS) + cobertura + detekt + swiftlint
make test         # só os testes multiplataforma
make lint         # análise estática (Kotlin + Swift) | make format  para autocorrigir
make coverage     # verifica o gate de 80% e abre o relatório HTML
make android-run  # instala e abre no device/emulador Android
make ios-run      # compila, instala e abre no simulador iOS
```

O `make bootstrap` instala os hooks do [lefthook](https://lefthook.dev):
`detekt` + `swiftlint` no pre-commit (<1s), validação da mensagem no commit-msg
(Conventional Commits, sem trailer `Co-Authored-By`) e os testes das **duas
plataformas** no pre-push (~6s) — o mesmo `commonTest` pode passar na JVM e
falhar no iOS.

O **gate de cobertura não roda nos hooks** de propósito: ele bloquearia o push
de trabalho em andamento (branch de backup, PR em draft, spike). Cobertura e
lint completo são gate de **PR**, não de push — rode `make check` quando quiser
o gate inteiro localmente.

## Checklist de clonagem (transformar o template em app real)

1. Clonar/copiar este repositório e trocar `rootProject.name` em `settings.gradle.kts`.
2. Renomear o pacote `com.kmpfoundation` — código (`commonMain`/`iosMain`, a
   `HostActivity` e a `KmpFoundationApplication` do `androidApp`, incluindo o
   `android:name` e os `activity` no manifesto), o `namespace` de cada módulo, o
   `applicationId` em `androidApp/build.gradle.kts` e os filtros de cobertura em
   `build-logic/.../Quality.kt`.
3. Atualizar `iosApp/project.yml` (`bundleIdPrefix`, `PRODUCT_BUNDLE_IDENTIFIER`,
   `CFBundleDisplayName`) e rodar `xcodegen`.
4. **Deletar `features/devices/`** (e seus testes), as telas nativas de exemplo
   (`DeviceDetailActivity` + a entrada dela no manifesto, `DeviceDetailView`) e
   as engines em `androidApp/platform/` e `iosApp/` — são demonstração da
   fronteira nativa (SDD 03) e da navegação (ADR 0007). **O que fica** é o
   `nav/` e o `ui/RouteContent.kt`: apague as rotas de exemplo, deixe a rota
   inicial da sua primeira tela e siga.
5. Revisar `docs/sdd/` para refletir o produto real; iniciar ADRs a partir do 0001.
6. Manter o gate de 80%: lógica nova nasce em `presentation/`/`domain`/`data` com teste.

## Regras que sustentam o gate de cobertura

- Lógica **não** vive em `ui/` — composables são casca fina sobre um ViewModel.
- Pacotes `*.ui.*` ficam fora da medição de cobertura (filtro único no convention plugin).
- Feature não importa de outra feature; só de `core/`. Comunicação entre features
  passa pelo grafo de DI/navegação.
- Só `di/` conhece features; `core/` nunca (senão vira ciclo ao modularizar).
  Cada feature monta a si mesma em `features/<nome>/di/` — ver ADR 0001.
- O `AppGraph` é construído pela casca da **aplicação** (`Application` / `iOSApp`),
  nunca por uma Activity ou tela — ver ADR 0003.
- Engine nativa devolve um `Cancellable` **por chamada** e não guarda estado de
  chamada — ver ADR 0004.
- **Toda tela Compose mora em `features/<nome>/ui/`.** `ui/RouteContent.kt` só
  faz moldura e despacho rota → tela; nenhuma tela nasce dentro dele.
- **A casca nunca decide destino** — quem decide é `nav/NavigationPolicy`, que é
  função pura e testada em `commonTest`. Ver ADR 0007.
- Estado que precisa atravessar telas vive no `AppGraph`, **não** na composição:
  com uma raiz Compose por destino, cada tela tem composição própria.

Detalhes em `docs/guidelines/01_style_guide.md` e `docs/sdd/02_arquitetura.md`.
Para "onde ponho este arquivo?", `docs/sdd/03_mapa_de_codigo.md`.
