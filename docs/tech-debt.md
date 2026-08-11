# Dívidas técnicas conscientes

Registro de decisões de adiamento — cada entrada diz o que falta, por que foi
adiado e o gatilho para pagar.

## 1. Pre-commit hook (enforcement automático dos gates)

- **O quê:** `scripts/install-hooks.sh` instalando um hook de pre-commit que
  rode as checagens rápidas (`./gradlew detekt`) antes de cada commit.
- **Por quê adiado:** decisão de 2026-08-11 — sem CI/CD nesta fase, manter a
  POC enxuta; o gate existe via `./gradlew check` manual.
- **Gatilho:** primeira vez que código que falha o `check` entrar na branch
  principal, ou a chegada de um segundo desenvolvedor.

## 2. Gate de regressão visual para `ui/` (snapshot tests)

- **O quê:** snapshot tests com **Roborazzi** (rodam como unit test JVM via
  Robolectric) comparando screenshots com baselines versionadas.
- **Por quê adiado:** o template não tem UI real. E decisão consciente
  (2026-08-11): snapshots **não** alimentam o gate de 80% — executam linhas de
  composable sem afirmar lógica, inflando a métrica. São um gate separado, de
  outra natureza ("nenhum pixel mudou sem baseline aprovado").
- **Gatilho:** primeira tela real com layout que valha proteger (no clone).

## 3. Enforcement automático das regras de dependência entre pacotes

- **O quê:** as regras "feature não importa de feature" e "domain é puro"
  (SDD 02) são verificadas só em review. Automatizar via Konsist ou detekt
  rule customizada.
- **Por quê adiado:** custo/benefício baixo com uma feature de exemplo.
- **Gatilho:** segunda feature real no clone.

## 4. CI/CD

- **O quê:** pipeline executando `./gradlew check` + builds Android/iOS em PR.
- **Por quê adiado:** fora do escopo desta fase por decisão do projeto.
- **Gatilho:** repositório compartilhado com mais de um contribuidor ativo.
