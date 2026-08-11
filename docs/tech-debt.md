# Dívidas técnicas conscientes

Registro de decisões de adiamento — cada entrada diz o que falta, por que foi
adiado e o gatilho para pagar.

## 1. Gate de regressão visual para `ui/` (snapshot tests)

- **O quê:** snapshot tests com **Roborazzi** (rodam como unit test JVM via
  Robolectric) comparando screenshots com baselines versionadas.
- **Por quê adiado:** o template não tem UI real. E decisão consciente
  (2026-08-11): snapshots **não** alimentam o gate de 80% — executam linhas de
  composable sem afirmar lógica, inflando a métrica. São um gate separado, de
  outra natureza ("nenhum pixel mudou sem baseline aprovado").
- **Gatilho:** primeira tela real com layout que valha proteger (no clone).

## 2. Enforcement automático das regras de dependência entre pacotes

- **O quê:** as regras "feature não importa de feature" e "domain é puro"
  (SDD 02) são verificadas só em review. Automatizar via Konsist ou detekt
  rule customizada.
- **Por quê adiado:** custo/benefício baixo com uma feature de exemplo.
- **Gatilho:** segunda feature real no clone.

## 3. Detekt em versão alpha

- **O quê:** usamos Detekt **2.0.0-alpha.6** (groupId novo `dev.detekt`).
- **Por quê assim, e não na "estável":** a estável (1.23.8) é oficialmente
  construída e testada contra **Gradle 8.12.1 e Kotlin 2.0.21**, enquanto o
  alpha da linha 2 é construído contra **Kotlin 2.3, Gradle 9.3 e AGP 9** — ou
  seja, exatamente esta stack. Entre um estável desalinhado e um alpha alinhado,
  escolhemos o alinhado: o gate de lint precisa entender a linguagem que analisa.
  O breaking change principal da linha 2 (a troca da API K1 pela Analysis API do
  K2) atinge quem escreve regras customizadas — nós só consumimos as embutidas.
  Isso também removeu a última chamada deprecada que bloqueava o Gradle 10
  (`ReportingExtension.file`, que vinha do plugin 1.23.8).
- **Risco aceito:** sendo alpha, pode regredir entre releases. O gate é rígido
  (`failOnSeverity = Info`), então uma regressão aparece como build vermelho, não
  como código ruim aprovado.
- **Gatilho:** trocar para a 2.0.0 estável quando sair; até lá, subir de alpha
  com o `check` como rede.

## 4. Duas regras de formatação desligadas

- **O quê:** `ktlint>ClassSignature` e `ktlint>FunctionSignature` estão
  `active: false` no `detekt.yml`. Elas exigem colapsar assinaturas numa linha
  só quando couber.
- **Por quê:** com DI manual por construtor, parâmetros entram e saem com
  frequência; a forma multi-linha com trailing comma mantém o diff restrito à
  linha alterada. Foi escolha de estilo, não limitação da ferramenta.
- **Gatilho:** revisar se o time preferir o estilo `ktlint_official` puro.

## 5. Ícone e tema do app Android

- **O quê:** o `androidApp` não declara `android:icon` (lint emite warning) nem
  tema próprio — usa `Theme.Material.Light.NoActionBar` do sistema.
- **Por quê adiado:** o template não tem identidade visual; o clone terá.
- **Gatilho:** clonagem para o app real.

## 6. CI/CD — e os gates que dependem dela

- **O quê:** pipeline executando `./gradlew check` + builds Android/iOS em PR.
- **Por quê adiado:** fora do escopo desta fase por decisão do projeto.
- **Escopo obrigatório quando existir** (decisão de 2026-08-11): **gate de
  cobertura ≥ 80%** e lint completo. A cobertura saiu do pre-push porque
  bloqueava o push de trabalho em andamento — subir branch para backup, abrir
  PR em draft ou empurrar um spike são usos legítimos, e cobertura deve
  **bloquear o merge, não o push**. Localmente ficou o que nunca é WIP legítimo:
  teste quebrado (`allTests`, JVM **e** iOS, ~6s no pre-push) e lint (`detekt`,
  <1s no pre-commit).
- **Gatilho:** repositório compartilhado com mais de um contribuidor ativo.
