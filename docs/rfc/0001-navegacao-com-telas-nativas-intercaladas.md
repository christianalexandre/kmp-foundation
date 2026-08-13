---
id: RFC-0001
title: Navegação com telas nativas intercaladas
status: Approved
author: Chirstian Alexandre
date: 2026-08-12
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# RFC 0001 — Navegação com telas nativas intercaladas

## Problema

O template não tem navegação. `ui/App.kt` empilha as duas features de exemplo num
`Column` e carrega o comentário *"vira a casca de navegação quando ela existir"*.
Enquanto isso, o README e o SDD 02 já enunciam a regra *"comunicação entre
features passa pelo grafo de DI/navegação"* — uma regra sobre uma camada que não
existe. O clone vai improvisar.

Mas o requisito real não é "adicionar navegação". É:

> Em qualquer ponto da navegação, poder abrir uma tela **inteiramente nativa** e
> seguir dali para a próxima tela Compose — inclusive com a tela nativa sendo
> **destino de topo do qual saem telas Compose**.

O motivo é econômico e legítimo: telas que são casca fina sobre SDK (pareamento,
preview de câmera, player) não ganham nada passando por Compose, e a ponte para o
SDK custa caro. Fazer direto no nativo é mais barato **e** mais fiel.

Isso descarta a pergunta que eu faria primeiro ("qual biblioteca de navegação?").
A pergunta que decide é **quem é dono da pilha**, e ela precisa ser respondida
antes de escolher qualquer biblioteca — porque a maioria delas assume que todo
destino é um Composable.

## Evidência: o `imou-smart-home` já fez isso

O projeto de referência tem o fluxo `Compose → nativo → Compose` **em produção
nas duas plataformas**. Não é teoria, e vale ler antes de decidir.

**O contrato** (`nav/NativeScreens.kt`) segue as mesmas regras das engines deste
template — sem `suspend`, sem tipo selado na assinatura, callback explícito:

```kotlin
interface NativeScreens {
    fun openPlayer(deviceId: String, onClosed: () -> Unit)
}
```

**O destino nativo é uma rota compartilhada de primeira classe** (`nav/Route.kt`):
`Route.Player(deviceId)` existe na pilha Kotlin, com `isNative = true`, e
simplesmente **não tem Composable**. O `Navigator` expõe `currentComposable` — a
última rota que o Compose sabe desenhar — para que a tela de baixo siga composta
enquanto a nativa está por cima, evitando frame em branco no fechamento.

**As duas cascas implementam o mesmo contrato com mecanismos diferentes:**

| | Mecanismo | Retorno |
| --- | --- | --- |
| Android (`MainActivity.kt:20`) | `Activity` separada via `registerForActivityResult` | `ActivityResult` **descartado**; só dispara `onClosed` |
| iOS (`ContentView.swift:117`) | `UINavigationController` com o Compose como VC **raiz**; nativo entra por `pushViewController` | `didShow` detecta a saída da pilha e chama `onClosed` |

Que os dois mecanismos convivam sob um contrato só é a evidência mais forte de que
o contrato está bem escolhido.

### O que o imou **não** faz, e por quê

1. **Não devolve resultado.** `App.kt:94` fixa o retorno em
   `{ nav.popTo(Route.Dashboard) }`. É `A → nativo → A`, com **A fixo**.
   Ir para C é uma linha em `App.kt` (`nav.replace(Route.SensorDetail(deviceId))`)
   e **nenhuma** mudança nas cascas, porque o id foi capturado na ida.
   O caso que exigiria canal de resultado é outro: quando o dado **nasce** no
   fluxo nativo (pareamento gerando um device novo).
2. **Não sobrevive a recriação.** `Navigator.kt:67` usa `remember`, não
   `rememberSaveable`. A pilha de rotas morre em morte de processo — e a tela
   nativa (`Activity` na pilha do sistema) sobrevive. Aceitável em POC; é defeito
   num template.
3. **Não implementa a própria decisão 4 do ADR 0001 de lá.** Aquele ADR decidiu
   "navegação nativa é dona da pilha", mas está `proposto`, e o spike 3 do plano
   de validação ("casca de navegação") **nunca rodou**. O que existe hoje no iOS é
   um meio-termo: container nativo, mas **uma** raiz Compose para todas as telas
   Compose — que é justamente o que não aguenta tela nativa como hub.

## Proposta

**O container nativo é dono da pilha; cada destino Compose é sua própria
`ComposeUIViewController`/Activity.** É a opção 3 da tabela abaixo, e é a decisão 4
do ADR 0001 do imou — aqui saindo do papel pela primeira vez.

A escolha é forçada pelo requisito: com a tela nativa podendo ser **hub** (destino
de topo do qual saem telas Compose), qualquer desenho com raiz Compose única deixa
de funcionar, e isso está registrado na tabela de alternativas do ADR 0001 do imou.
Não é preferência — é o único modelo que suporta intercalação arbitrária.

O que **continua** valendo do desenho do imou:

1. **Estado de rota em `commonMain`**, incluindo os destinos nativos. Rotas
   carregam **ids, nunca objetos de domínio** — precisam ser reconstruíveis a
   partir de deeplink ou push. A pilha compartilhada é a fonte da verdade; o
   container nativo é o espelho dela.
2. **A casca nativa nunca decide destino.** Ela apresenta, e reporta o que o
   usuário fez. Quem decide o próximo destino é o código comum. Sem isso, a
   política de navegação passa a existir em três lugares (comum, Android, iOS) e
   sai do alcance de `commonTest` — que é o que mantém o gate de 80% significando
   algo neste fluxo, em vez de virar outro buraco como o da dívida #9.
3. **Resultado de tela nativa não volta por payload de callback.** Segue a decisão
   5 do ADR 0001 do imou: o nativo escreve no repositório compartilhado e o Compose
   observa. Isso resolve de graça a morte de processo — o repositório é durável, a
   continuation não é.
4. **A pilha de rotas sobrevive à recriação.** Correção direta do defeito 2 acima,
   e mais crítica aqui: com N raízes Compose, o estado que antes vivia numa
   composição só passa a ter que viver fora dela de qualquer jeito.

### Swipe-back não é bônus: é requisito de paridade

O app que este projeto vai substituir é React Native e **tem swipe-back
funcional** — via `react-navigation`/`react-native-screens`, que embrulha
`UINavigationController`. Perder isso seria regressão percebida pelo usuário, e a
solução do app atual é a mesma da opção 3: **delegar ao UIKit** em vez de imitar.

O CMP não resolve isso sozinho. O ADR 0001 do imou verificou que o CMP 1.11.1 tem
gesto de borda próprio (`UIKitBackGestureRecognizer`), mas que ele entrega só o
**gesto** — não a transição de push do UIKit (parallax, sombra, crossfade da nav
bar). E swipe-back nativo é uma transição **interativa e cancelável**, não um
gesto que dispara um pop: no imou, o `BackHandler` do `App.kt:67` recebe o gesto e
a tela troca instantaneamente, sem animação.

Com um `ComposeUIViewController` por destino dentro de um `UINavigationController`,
o comportamento é o do UIKit por construção — inclusive nas transições
Compose→Compose. É o ganho que a decisão 4 do ADR 0001 do imou cita textualmente.

O custo aparece no mesmo lugar: durante um pop **interativo**, duas composições
ficam vivas renderizando simultaneamente enquanto o dedo se move. É o pior caso do
desenho, e cai dentro da medição que a questão 1 já pede.

O que é **novo** e é o coração do trabalho: um **adaptador de pilha por
plataforma**, que reconcilia a pilha Kotlin com a pilha do container. É
bidirecional — o Kotlin empurra rotas, mas o usuário também desempilha por gesto de
borda e back do sistema, e a plataforma precisa reportar sem que o adaptador
re-emita a mesma transição. É onde os bugs desse desenho moram, e é a parte que
nenhum dos dois repositórios tem hoje.

Sobre biblioteca: **Navigation Compose (androidx) está fora** — ele assume que todo
destino é um Composable, e uma rota nativa não é. Sobram um `Navigator` próprio
(dezenas de linhas, como o do imou) e **Decompose**, cuja árvore de componentes é
agnóstica de UI e que já traz preservação de estado nas duas plataformas. Com a
opção 3, o peso pende para Decompose: "um componente por destino, renderizado por
quem for" é literalmente o modelo dele. Merece spike; não merece adoção por
reputação.

Nada disso usa o padrão de engine (`Cancellable`/`CallHandle`, ADR 0004 e 0006).
Abrir tela não é chamada concorrente com recurso a cancelar — é transição de
navegação, com no máximo uma em voo. O SDD 03 ganha uma linha dizendo isso.

## Alternativas consideradas

| Opção | O que é | Custo | Quebra quando |
| --- | --- | --- | --- |
| **1. Compose dono da pilha, nativo como folha** *(imou hoje)* | Rota nativa na pilha compartilhada; casca apresenta por cima | Baixo. Já validado nas duas plataformas | A tela nativa vira **hub**: destino de topo do qual saem telas Compose |
| **2. Container nativo, raiz Compose única** *(imou no iOS hoje)* | `UINavigationController` com Compose como VC raiz | Médio: um adaptador por plataforma | Mesma limitação da 1 para telas Compose acima de nativas |
| **3. Container nativo dono da pilha, uma tela Compose por VC/Activity** ← **escolhida** | Cada destino Compose é seu próprio `ComposeUIViewController`/Activity | Alto: adaptador por plataforma **+ N raízes Compose**, estado compartilhado sai do Compose | — (é o modelo geral) |

As opções 1 e 2 estão descartadas por decisão de produto: a tela nativa **pode ser
hub**. Ficam registradas porque são o que o projeto de referência roda hoje, e
porque a 1 é o caminho certo para quem clonar este template **sem** esse requisito
— o custo da 3 não se justifica se o nativo for sempre folha.

Descartadas sem tabela: Navigation Compose (destino tem que ser Composable) e
Voyager (mesma premissa).

## Impacto

- **`composeApp`**: novo pacote `nav/` em `commonMain` (rotas, pilha, contrato de
  apresentação). `ui/App.kt` **deixa de existir como raiz do app** — vira um
  renderizador por destino. É a mudança conceitual maior do RFC.
- **`androidApp`**: `MainActivity` deixa de ser dona de `setContent` para o app
  inteiro; entra um container (Activity por destino ou Navigation component
  hospedando Compose por destino) e o adaptador de pilha.
- **`iosApp`**: `UINavigationController` com um `ComposeUIViewController` **por
  destino**, mais o adaptador. A casca precisa guardar referência ao **VC
  hospedeiro**: vários SDKs fazem `present(from:)` sozinhos e exigem um VC real,
  não uma `View` SwiftUI. Vale mesmo com a UI sendo SwiftUI — o
  `UIViewControllerRepresentable` que vocês já usam (`ContentView.swift:6`) resolve
  o sentido UIKit→SwiftUI, e `UIHostingController` o inverso.
- **Catálogo**: `kotlinx-serialization` está declarado
  (`libs.versions.toml:19` e `:55`) e não é aplicado nem consumido em lugar nenhum.
  Rotas serializáveis (deeplink + sobrevivência a morte de processo) dão função a ele.
- **Exemplo**: seguindo o precedente das duas engines, o template entrega **uma**
  excursão nativa de demonstração, que entra no passo 4 do checklist de clonagem
  como coisa a deletar. O clone herda contrato, casca e ADR — não a tela.
- **Custo de cobertura**: com a navegação virando adaptador por plataforma, boa
  parte do fluxo passa a viver em módulos **sem medição** — o mesmo efeito que a
  dívida #9 já registra para as engines. A mitigação é a proposta 2: política de
  destino fica em `commonMain`, o adaptador fica burro o suficiente para não
  precisar de teste.
- **Risco principal**: o custo das N raízes Compose no iOS (memória e primeiro
  frame por destino) é **desconhecido** — o spike que existia para medi-lo nunca
  rodou. Estamos adotando uma decisão não validada. Ver questão 1.

## Bancada: o que ela já provou

A bancada existe e roda **nas duas plataformas**, montando a forma da opção 3 em
cada uma: `UINavigationController` com um `ComposeUIViewController` por destino no
iOS, e uma `Activity` por destino no Android. As duas rodam o **mesmo**
`SpikeScreen` de `commonMain`, para que a diferença medida seja de plataforma e
não de conteúdo — e as duas têm um destino **100% nativo** (SwiftUI puro no iOS,
UI de `View` no Android) empilhável em qualquer ponto, do qual sai outro destino
Compose.

| Peça | Onde |
| --- | --- |
| Conteúdo representativo (lista rolável, não tela vazia) | `composeApp/src/commonMain/.../ui/spike/SpikeScreen.kt` |
| Uma raiz Compose por destino (iOS) | `composeApp/src/iosMain/.../SpikeViewController.kt` |
| Container, destino nativo, `phys_footprint` e `CADisplayLink` | `iosApp/iosApp/Spike/` |
| Activity por destino, destino nativo, PSS e `Choreographer` | `androidApp/src/main/.../spike/` |

**Validado em simulador e emulador (2026-08-12) — questão de comportamento,
respondida:**

- a intercalação `Compose → nativo → Compose` funciona em profundidade arbitrária,
  nas duas plataformas;
- o voltar atravessa a fronteira nos dois sentidos;
- as raízes Compose sobrevivem embaixo de um destino nativo;
- **tela nativa como hub**, com destino Compose empilhado por cima dela, funciona —
  que era o caso que forçou a opção 3 e o que nenhum dos dois repositórios tinha.

Isso era o risco de desenho, e não depende de hardware: o código é o mesmo. **O
que continua em aberto é quantitativo**, e é o que a tabela abaixo espera.

**Medir memória como delta por push não funciona — foi tentado e descartado.**
Na janela curta em volta do push (~150 ms), tanto o `phys_footprint` quanto o PSS
oscilam por GC, páginas compartilhadas e alocação preguiçosa mais do que a raiz
nova custa: o delta sai **negativo** nas duas plataformas. A bancada por isso
amostra a memória **com a tela assentada** (800 ms após a transição) e reporta a
série por profundidade — a **inclinação** dela é o custo por raiz. O tempo até o
primeiro frame continua sendo por push, que é onde o carimbo de relógio é confiável.

Os números das duas plataformas **não são comparáveis em valor absoluto**
(`phys_footprint` e `totalPss` medem coisas diferentes). Só as inclinações são,
e como ordem de grandeza — os critérios abaixo valem por plataforma.

**Como rodar.** Enquanto o spike está em validação, a bancada é a raiz nos dois
apps — qualquer run cai nela, sem flag:

- **iOS:** `SpikeRootView()` em `iosApp/iosApp/iOSApp.swift`. Voltar ao app é
  trocar por `ContentView(graph: graph)`, numa linha.
- **Android:** o `intent-filter` de LAUNCHER está em `.spike.SpikeActivity`
  (`AndroidManifest.xml`). Voltar ao app é movê-lo de volta para `.MainActivity`.

**Medir exige device físico E build Release.** As duas condições, não uma:

| | Por quê |
| --- | --- |
| Device físico | Simulador roda na CPU do Mac, sem restrição térmica; emulador não tem limite de jetsam. Nenhum dos dois renderiza como o aparelho. |
| Build **Release** | Em Debug o Kotlin/Native é ordens de grandeza mais lento e o app Android é `debuggable`, o que desliga otimizações da ART. Um número de device em Debug é tão enganoso quanto um de simulador — **com aparência de confiável**, que é pior. |

Para isso existem `make android-release` (APK Release assinado com a chave de
debug, só para instalar) e o scheme **`iosAppRelease`** no Xcode. `make ios-release`
faz o equivalente no simulador, para conferir que a bancada sobe em Release antes
de levar ao aparelho. A primeira build Release do Kotlin/Native demora vários
minutos (`linkRelease`) — não é travamento.

No Android o `android:enableOnBackInvokedCallback` está ligado no manifesto: sem
ele não há **back preditivo**, e sem back preditivo não existe gesto progressivo
para medir — a bancada reporta "sem amostras" em vez de fingir um número.

## Resultado da medição (2026-08-13)

Medido em **device físico, build Release**: Galaxy S10+ (Android 12) e iPhone 8
(iOS 16.7, A11, 2 GB). Pilha `Compose(d1) → nativo(d2) → Compose(d3) → nativo(d4)`
nos dois, o que permite separar o custo por tipo de destino.

### Tempo até o primeiro frame — critério bloqueante, **passou**

| Profundidade | Tipo | S10+ | iPhone 8 |
| --- | --- | --- | --- |
| d2 | nativo | 47 ms | 100 ms |
| d3 | **Compose** | **45 ms** | **67 ms** |
| d4 | nativo | 60 ms | 75 ms |

Pior caso absoluto: **100 ms contra o critério de ~300 ms** — três a seis vezes de
folga, no hardware mais fraco do parque.

A conclusão que decide a opção 3: **a raiz Compose não é a parte cara.** Em
nenhuma das duas plataformas o destino Compose foi o mais lento; no iPhone 8 foi o
mais barato dos três. Isso **não** é um veredito "Compose contra SwiftUI" — os
ganchos de primeiro desenho diferem (`drawWithContent` contra `UIView.draw`) e são
três amostras. O que é robusto é que nenhuma chega perto do limite.

### Memória — instrumento descartado

| | Série medida |
| --- | --- |
| S10+ | d1 47,2 · d2 68 · d3 57,7 · d4 48,3 MB |
| iPhone 8 | d1 34 · d3 48,3 · d4 42,7 MB |

**A memória cai enquanto a pilha cresce** — fisicamente impossível se o número
isolasse o custo das raízes. Ruído de GC, de páginas comprimidas e da própria
transição domina o sinal.

Foi a **terceira** forma tentada, depois de delta por push e de acumulado desde o
baseline (ambas registradas acima). A conclusão é que `totalPss` e
`phys_footprint` amostrados pelo próprio app **não isolam custo de raiz nesta
escala**. Medir isso direito exige Instruments/Allocations no iOS e Memory
Profiler no Android — sessão própria, não um HUD.

**A pergunta por trás ficou respondida por outro caminho, e melhor:** dezenas de
destinos empilhados num iPhone 8 de 2 GB **sem crash e sem lentidão**. Se o custo
por raiz fosse relevante, o jetsam teria agido — é exatamente o aparelho e a
condição em que ele age. Ausência de jetsam sob profundidade alta responde "a
memória explode?" com mais autoridade que qualquer inclinação de PSS.

### Gesto de voltar — parcialmente medido

O swipe-back foi validado **no uso**, nos dois sentidos (nativo → Compose e
Compose → Compose), no iPhone 8: comportamento indistinguível do nativo, sem
engasgo. Quem julgou conhece o app React Native que está sendo substituído, que é
a régua de paridade que importa.

**Não medido em número:** o fps do pop interativo (só avaliação de uso) e o gesto
no Android — o S10+ é API 31, e o back preditivo exige API 33+ (animação entre
Activities, API 34+). Ver dívida registrada.

### Critérios de decisão, e como cada um saiu

| Critério | Régua | Resultado |
| --- | --- | --- |
| 1º frame (bloqueante) | < ~300 ms, que é a duração da animação de push do UIKit — acima disso o usuário vê tela em branco deslizando para dentro, a cada navegação | **Passou.** Pior caso 100 ms |
| Gesto de voltar (bloqueante) | Igualar o swipe-back que o app React Native já entrega; gesto que engasga é pior que não ter gesto | **Passou** por avaliação de uso; sem número |
| Memória por raiz (negociável) | Se alta mas o resto passando, a saída era a assimetria da questão 3 ou limitar profundidade — não abandonar o desenho | **Instrumento falhou**; respondido pela ausência de jetsam |

**Veredito: a opção 3 está validada.** Os dois critérios bloqueantes passaram, no
pior hardware disponível e em build Release.

O código do spike sai inteiro quando esta decisão virar ADR — mesmo tratamento das
engines de exemplo no checklist de clonagem.

## Questões em aberto

1. ~~Custo de N raízes Compose no iOS.~~ **Respondida em 2026-08-13** — ver
   "Resultado da medição". Fecha também o spike 3 do ADR 0001 do imou, pendente
   desde julho. O que sobrou virou dívida, não questão em aberto: o fps do pop
   interativo em número, e o gesto no Android (exige aparelho API 34+).
2. **Decompose ou `Navigator` próprio?** Depende do spike acima e de quanto do
   adaptador de pilha o Decompose já entrega pronto nas duas plataformas.
3. **Android precisa mesmo de container nativo?** A motivação do ADR 0001 do imou
   para a decisão 4 é fidelidade de transição e swipe-back — argumento muito mais
   forte no iOS que no Android. Cabe considerar assimetria deliberada: container
   nativo no iOS, raiz Compose única no Android, mesmo contrato. Reduz o custo pela
   metade se a tela-hub for aceitável dentro de uma `Activity` só.
4. O dado que volta do nativo é sempre um id que o lado Compose **já tinha na ida**,
   ou existe fluxo em que o nativo **cria** o dado (pareamento gerando device novo)?
   O primeiro caso não precisa de canal de resultado nenhum.
