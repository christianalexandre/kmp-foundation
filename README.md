# kmp-foundation

Template de fundação de engenharia para apps **Kotlin Multiplatform + Compose
Multiplatform** (Android + iOS). Este repositório não é um app: é a base que um
app real herda ao ser clonado — documentação viva, quality gates locais e
arquitetura por feature já configurados.

## O que vem pronto

| Área | O quê |
| --- | --- |
| Documentação | `docs/` com RFC, ADR, SDD e guidelines (ciclo de vida em `docs/README.md`) |
| Quality gates | Kover (cobertura de linha ≥ 80%) + Detekt (com formatting/ktlint), ambos em `./gradlew check` |
| Build | AGP 9 + Gradle 9.5, Version Catalog (`gradle/libs.versions.toml`) e convention plugins em `build-logic/` |
| Arquitetura | Package-by-feature (`core/` + `features/<nome>/{domain,data,presentation,ui}`), DI manual + ViewModel multiplataforma |
| Exemplo | `features/welcome/` demonstra o padrão completo com testes usando Fakes |

## Pré-requisitos

| Ferramenta | Versão | Por quê |
| --- | --- | --- |
| **Android Studio** | **Quail 3 (2026.1.3) ou mais recente** | Cada versão do Studio suporta uma faixa de AGP: Quail 2/3 (2026.1.2+) são as que aceitam **AGP 9.3**. Panda 3 (2025.3.3) vai só até o AGP 9.1 — nele o sync falha com erros de índice (ex.: *"Failed to build cache for …klib"*), mesmo com a build por terminal verde. Confira a [matriz oficial](https://developer.android.com/build/releases/about-agp) antes de mudar o AGP. |
| JDK | 17+ | Exigido pelo AGP 9. |
| XcodeGen | qualquer | `brew install xcodegen`, só para gerar o projeto iOS. |

> A build por linha de comando não depende do Android Studio — o wrapper do
> Gradle resolve tudo. O requisito acima é para trabalhar dentro do IDE.

## Módulos

| Módulo | Papel |
| --- | --- |
| `composeApp` | Lógica e UI compartilhadas (KMP): Android library + framework iOS. É onde vivem os gates. |
| `androidApp` | Casca Android (`MainActivity`). Sem lógica — o AGP 9 exige que o app seja um módulo separado do módulo KMP. |
| `iosApp` | Casca iOS (SwiftUI + XcodeGen). |

## Comandos

```sh
./gradlew check                                        # testes + cobertura + detekt
./gradlew :androidApp:assembleDebug                    # APK Android
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64  # framework iOS
cd iosApp && xcodegen                                  # gera o .xcodeproj (requer `brew install xcodegen`)
```

## Checklist de clonagem (transformar o template em app real)

1. Clonar/copiar este repositório e trocar `rootProject.name` em `settings.gradle.kts`.
2. Renomear o pacote `com.kmpfoundation` — código (`commonMain`/`iosMain` e o
   `MainActivity` do `androidApp`), o `namespace` de cada módulo, o `applicationId`
   em `androidApp/build.gradle.kts` e os filtros de cobertura em
   `build-logic/.../Quality.kt`.
3. Atualizar `iosApp/project.yml` (`bundleIdPrefix`, `PRODUCT_BUNDLE_IDENTIFIER`,
   `CFBundleDisplayName`) e rodar `xcodegen`.
4. **Deletar `features/welcome/`** (e seus testes) — é só exemplo do padrão.
5. Revisar `docs/sdd/` para refletir o produto real; iniciar ADRs a partir do 0001.
6. Manter o gate de 80%: lógica nova nasce em `presentation/`/`domain`/`data` com teste.

## Regras que sustentam o gate de cobertura

- Lógica **não** vive em `ui/` — composables são casca fina sobre um ViewModel.
- Pacotes `*.ui.*` ficam fora da medição de cobertura (filtro único no convention plugin).
- Feature não importa de outra feature; só de `core/`. Comunicação entre features
  passa pelo grafo de DI/navegação.
- Só `di/` conhece features; `core/` nunca (senão vira ciclo ao modularizar).
  Cada feature monta a si mesma em `features/<nome>/di/` — ver ADR 0001.

Detalhes em `docs/guidelines/01_style_guide.md` e `docs/sdd/02_arquitetura.md`.
