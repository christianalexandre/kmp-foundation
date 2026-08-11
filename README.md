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
| Build | Version Catalog (`gradle/libs.versions.toml`) + convention plugins em `build-logic/` |
| Arquitetura | Package-by-feature (`core/` + `features/<nome>/{domain,data,presentation,ui}`), DI manual |
| Exemplo | `features/welcome/` demonstra o padrão completo com testes usando Fakes |

## Comandos

```sh
./gradlew check                                        # testes + cobertura + detekt
./gradlew :composeApp:assembleDebug                    # APK Android
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64  # framework iOS
cd iosApp && xcodegen                                  # gera o .xcodeproj (requer `brew install xcodegen`)
```

## Checklist de clonagem (transformar o template em app real)

1. Clonar/copiar este repositório e trocar `rootProject.name` em `settings.gradle.kts`.
2. Renomear o pacote `com.kmpfoundation` (commonMain/androidMain/iosMain, `namespace`
   e `applicationId` no `composeApp/build.gradle.kts`, filtros de cobertura no
   `build-logic/.../QualityConventionPlugin.kt`).
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

Detalhes em `docs/guidelines/01_style_guide.md` e `docs/sdd/02_arquitetura.md`.
