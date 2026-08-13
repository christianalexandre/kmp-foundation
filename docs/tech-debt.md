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

## 2. A comparação de frameworks de DI do ADR 0001 já envelheceu

- **O quê:** a tabela de alternativas do ADR 0001 (2026-08-11) registra dois
  fatos que não valem mais. ADR é imutável por regra (`docs/README.md`), então
  a correção fica aqui, e vale como insumo para a RFC quando o gatilho disparar:
  - **Koin não é mais "erro em runtime".** Ganhou um plugin de compilador que
    verifica o grafo em tempo de compilação — bindings faltando quebram o build.
    Era o argumento central contra ele.
  - **Metro não é mais "framework novo".** Está em 1.x, em produção no Cash App,
    e a tabela de compatibilidade cobre o Kotlin **2.3.21** deste projeto
    (Metro 0.10.0+). É plugin de compilador, não KSP — este projeto hoje não tem
    KSP nenhum, então evitá-lo é um ganho real de superfície de build.
- **O que continua valendo:** a política de compatibilidade do Metro é `N+.2`
  (uma versão construída contra 2.3.0 tenta suportar até 2.3.20), ou seja, é o
  mesmo acoplamento versão-a-versão que já pagamos com AGP ↔ Android Studio
  (invariante no SDD 01). Toda subida de Kotlin passa a exigir consultar a
  matriz do Metro antes.
- **Por quê não decidimos agora:** o motivo do ADR 0001 segue de pé — o grafo
  real não existe. O que mudou é que os **escopos** agora estão explícitos
  (ADR 0003), que era o pré-requisito para escolher com critério.
- **Gatilho:** o do ADR 0001 (repasse manual virando reclamação recorrente, ou
  ~8 singletons compartilhados no `AppGraph`), ou o do ADR 0003 (aparecer um
  terceiro escopo, entre aplicação e tela). Ao disparar: RFC comparando Metro e
  Koin **com os dados de um spike**, não com tabela de memória.

## 3. Enforcement automático das regras de dependência entre pacotes

- **O quê:** as regras "feature não importa de feature" e "domain é puro"
  (SDD 02) são verificadas só em review. Automatizar via Konsist ou detekt
  rule customizada.
- **Por quê adiado:** custo/benefício baixo com uma feature de exemplo.
- **Gatilho:** segunda feature real no clone.

## 4. Detekt em versão alpha

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

## 5. Duas regras de formatação desligadas

- **O quê:** `ktlint>ClassSignature` e `ktlint>FunctionSignature` estão
  `active: false` no `detekt.yml`. Elas exigem colapsar assinaturas numa linha
  só quando couber.
- **Por quê:** com DI manual por construtor, parâmetros entram e saem com
  frequência; a forma multi-linha com trailing comma mantém o diff restrito à
  linha alterada. Foi escolha de estilo, não limitação da ferramenta.
- **Gatilho:** revisar se o time preferir o estilo `ktlint_official` puro.

## 6. Ícone e tema do app Android

- **O quê:** o `androidApp` não declara `android:icon` (lint emite warning) nem
  tema próprio — usa `Theme.Material.Light.NoActionBar` do sistema.
- **Por quê adiado:** o template não tem identidade visual; o clone terá.
- **Gatilho:** clonagem para o app real.

## 7. Ambientes de build e segredos fora do binário

- **O quê:** não há `buildTypes`/flavors nem geração de configuração — uma única
  build, sem noção de dev/staging/prod e sem caminho para credenciais.
- **Por quê adiado:** o template não fala com backend nenhum; a decisão depende
  de como o middleware do app real exporá ambientes.
- **Precedente a seguir:** no `imou-smart-home`, `appId`/`appSecret` vêm do
  `local.properties` (ou variável de ambiente) e são gerados em código por uma
  task do Gradle (`ImouSecrets`). O ADR 0001 de lá registra que **manter segredo
  no binário é inviável em produção** — extraível do APK/IPA. O template deve
  nascer com o caminho pronto para não repetir a correção tardia.
- **Gatilho:** primeira chamada de rede real no clone.

## 8. CI/CD — e os gates que dependem dela

- **O quê:** pipeline executando `./gradlew check` + builds Android/iOS em PR.
- **Por quê adiado:** fora do escopo desta fase por decisão do projeto.
- **Escopo obrigatório quando existir** (decisão de 2026-08-11): **gate de
  cobertura ≥ 80%** e lint completo. A cobertura saiu do pre-push porque
  bloqueava o push de trabalho em andamento — subir branch para backup, abrir
  PR em draft ou empurrar um spike são usos legítimos, e cobertura deve
  **bloquear o merge, não o push**. Localmente ficou o que nunca é WIP legítimo:
  teste quebrado (`allTests`, JVM **e** iOS, ~6s no pre-push) e lint (`detekt`,
  <1s no pre-commit).
- **Urgência maior desde 2026-08-11:** ao tirar a cobertura do pre-push
  (bloqueava WIP legítimo), o gate de 80% **deixou de rodar automaticamente em
  qualquer lugar**. Hoje ele só existe se alguém rodar `make check` na mão. A CI
  não é só conveniência — é o que devolve um gate que o projeto já tinha.
- **Gatilho:** repositório compartilhado com mais de um contribuidor ativo, ou
  antes disso se o gate de cobertura começar a ser violado sem ninguém notar.

## 9. As implementações de engine nativa estão fora de qualquer gate

- **O quê:** `AndroidDeviceScanner` (Kotlin, em `androidApp/`) e
  `SwiftDeviceScanner` (Swift, em `iosApp/`) não têm teste. O contrato e o
  adapter são cobertos em `commonTest` pelo `FakeDeviceScanner`, mas as duas
  implementações reais, não.
- **Piorou em 2026-08-13** ao remover a `features/welcome`: as engines de
  saudação saíram junto, e com elas o exemplo síncrono. Sobrou só o scanner, que
  é o caso **mais** difícil de testar dos dois — tem timer e cancelamento.
- **Piorou em 2026-08-12** ao mover a engine Android para a casca (SDD 03): a
  decisão de arquitetura está certa, mas o efeito colateral é que as duas
  implementações passaram a viver em módulos **sem medição de cobertura**. O
  Kover marcar 100% hoje diz respeito ao módulo compartilhado, não ao app.
- **Por quê adiado:** exige escolher e montar duas ferramentas novas —
  Robolectric (ou teste instrumentado) para o `androidApp` e XCTest para o
  `iosApp` — e o template não tem engine real para justificar o custo.
- **Risco aceito:** um defeito na engine só aparece rodando o app. Mitigado em
  parte porque a engine é fina por contrato (ADR 0004: sem estado de chamada).
- **Gatilho:** primeira engine real no clone (Imou/Tuya). Aí a implementação
  deixa de ser trivial e o teste passa a valer mais que o custo do setup.

## 10. Três números que o ADR 0007 assumiu sem medir

O ADR 0007 foi decidido com os dois critérios bloqueantes medidos (1º frame e
swipe-back). Ficaram três lacunas, todas conscientes:

- **Custo de memória por raiz Compose.** O instrumento falhou três vezes — delta
  por push, acumulado desde o baseline e série assentada. `totalPss` e
  `phys_footprint` amostrados pelo próprio app não isolam custo de raiz nessa
  escala: a memória chegou a **cair** enquanto a pilha crescia. Medir direito exige
  Instruments/Allocations no iOS e Memory Profiler no Android — sessão própria, não
  um HUD. **Aceito** porque empilhar dezenas de destinos num iPhone 8 de 2 GB não
  causou jetsam, que é o aparelho e a condição em que ele agiria.
  **Gatilho:** primeira tela de SDK real (player, preview) no clone — aí o conteúdo
  passa a dominar o custo e o número muda de natureza.
- **Frame rate do pop interativo, em número.** Foi validado no uso, no iPhone 8,
  não com instrumento. A bancada mede, mas a sessão não colheu.
  **Gatilho:** qualquer relato de engasgo no gesto, ou a primeira tela pesada.
- **Gesto de voltar no Android.** O aparelho disponível (Galaxy S10+) é API 31, e o
  back preditivo exige API 33+ — animação entre Activities, API 34+. A bancada
  reporta "sem amostras" em vez de fingir um número. É a lacuna menos grave: o
  argumento de fidelidade de transição sempre foi do iOS.
  **Gatilho:** aparelho API 34+ disponível, ou a decisão do mecanismo Android
  (Activity por destino contra raiz única) precisar do dado.

## 11. O mecanismo de navegação no Android ficou em aberto

- **O quê:** o ADR 0007 fixou a posse da pilha e o contrato, mas **não** decidiu se
  o Android usa Activity por destino ou raiz única com Compose desenhando o topo.
- **Por quê adiado:** a motivação do container nativo é fidelidade de transição e
  swipe-back, argumento **inteiramente iOS**. No Android não existe transição de
  push cuja qualidade se perca ao desenhar em Compose. E a escolha depende da
  biblioteca, que também não foi decidida.
- **Consequência aceita:** as duas plataformas podem divergir no mecanismo mantendo
  o mesmo contrato — assimetria deliberada, não acidente. É o que já acontece hoje
  na bancada e no `imou-smart-home`.
- **Gatilho:** o RFC da biblioteca de navegação. Decidir os dois juntos.

## 12. Deeplink e push não têm caminho de entrada

- **O quê:** o ADR 0007 exige que a rota seja reconstruível a partir de um
  deeplink ou de um push, e é por isso que ela carrega **id, nunca objeto**. Hoje
  o único caminho de entrada exercitado é o `Intent` restaurado depois de morte
  de processo (`RouteCodec`) — não há esquema de URL registrado no `Info.plist`
  nem `intent-filter` no manifesto.
- **O que saiu, e por quê (2026-08-13):** existia uma `NavigationPolicy.fromDeeplink`
  testada e um `RootCoordinator.open(deeplink:)` que **ninguém chamava**, e o
  Android não tinha nada. Num repositório cujo valor é ser lido e entendido,
  código inalcançável ensina errado: dá a impressão de que o caminho existe.
  Preferimos a ausência declarada à presença falsa.
- **O que continua de pé:** a decisão que motivava tudo isso — rota carrega id, e
  a política decide o destino — está exercitada pelos outros caminhos e testada
  em `commonTest`. Reintroduzir o deeplink é escrever a política de volta e ligar
  duas pontas na casca, não redesenhar nada.
- **Cuidado ao reintroduzir:** deeplink vira **pilha**, não tela solta. Abrir um
  device direto da notificação sem a lista embaixo faz o voltar jogar o usuário
  para fora do app — no iOS é `setViewControllers`, no Android `startActivities`.
- **Gatilho:** primeiro deeplink ou push real no clone.
