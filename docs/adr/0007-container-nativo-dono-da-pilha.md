---
id: ADR-0007
title: O container nativo é dono da pilha, com uma raiz Compose por destino
status: Approved
author: Chirstian Alexandre
date: 2026-08-13
jira_ticket: APP-XXXX
epic: APP-YYYY
---

# ADR 0007 — O container nativo é dono da pilha, com uma raiz Compose por destino

## Contexto

O requisito que forçou esta decisão: **em qualquer ponto da navegação, abrir uma
tela inteiramente nativa — e essa tela pode ser destino de topo, com telas Compose
empilhadas por cima dela.**

O motivo é econômico e legítimo. Telas que são casca fina sobre SDK (pareamento,
preview de câmera, player) não ganham nada passando por Compose, e a ponte para o
SDK custa caro. Fazer direto no nativo é mais barato e mais fiel.

Isso torna irrelevante a primeira pergunta que se faria ("qual biblioteca de
navegação?"). A pergunta que decide é **quem é dono da pilha** — e ela precede
qualquer biblioteca, porque a maioria delas assume que todo destino é um
Composable, o que uma tela de SDK não é.

**Evidência de produção.** O `imou-smart-home` roda `Compose → nativo → Compose`
nas duas plataformas. De lá vieram a forma do contrato (sem `suspend`, sem tipo
selado na assinatura, callback explícito) e a ideia da rota nativa como cidadã de
primeira classe do estado compartilhado. A tabela de alternativas do ADR 0001 de
lá já registrava que "Compose dono da pilha, nativo como folha modal" **deixa de
funcionar quando a playback view é destino de topo e dela saem telas Compose** —
exatamente o nosso caso. A decisão 4 daquele ADR apontava para container nativo,
mas estava `proposto`: o spike de validação nunca rodou.

**Evidência medida aqui** (RFC 0001, 2026-08-13, device físico e build Release):

| | Galaxy S10+ | iPhone 8 |
| --- | --- | --- |
| 1º frame, destino Compose | 45 ms | 67 ms |
| 1º frame, destino nativo | 47–60 ms | 75–100 ms |

Contra o critério de ~300 ms — a duração da animação de push do UIKit, acima da
qual o usuário veria tela em branco deslizando para dentro. Três a seis vezes de
folga, no hardware mais fraco do parque. E a conclusão que decide: **a raiz Compose
não é a parte cara** — não foi o destino mais lento em nenhuma das plataformas.

O swipe-back foi validado no uso, nos dois sentidos, no iPhone 8. Não é detalhe de
acabamento: o app que este projeto substitui é React Native e **já entrega**
swipe-back nativo, via `react-native-screens`, que embrulha `UINavigationController`.
Perder isso seria regressão percebida. O CMP 1.11.1 tem gesto de borda próprio, mas
entrega só o **gesto** — não a transição de push do UIKit. Delegar ao UIKit é a
mesma solução que o app atual já usa.

Empilhar dezenas de destinos num iPhone 8 de 2 GB não causou crash nem lentidão.

## Decisão

**O container nativo é dono da pilha. Cada destino Compose é sua própria raiz —
um `ComposeUIViewController` no iOS, uma Activity (ou equivalente) no Android.**

Junto com ela, e indivisíveis dela:

1. **O estado de rota vive em `commonMain`**, e os destinos nativos são rotas de
   primeira classe. Rotas carregam **ids, nunca objetos de domínio** — precisam ser
   reconstruíveis a partir de um deeplink ou de um push, e serializáveis para
   sobreviver à morte de processo.
2. **A casca nunca decide destino.** Ela apresenta o que a pilha mandar e reporta o
   que o usuário fez. Quem decide o próximo destino é o código comum.
3. **Resultado de tela nativa não volta como payload de callback.** O nativo
   escreve no repositório compartilhado e o Compose observa — decisão 5 do ADR 0001
   do imou. Isso também resolve morte de processo de graça: o repositório é
   durável, uma continuation não é.
4. **A pilha de rotas sobrevive à recriação de processo.**
5. **Abrir tela não é o padrão de engine.** `Cancellable`/`CallHandle` (ADR 0004 e
   0006) modelam chamada concorrente com recurso a cancelar; excursão de navegação
   tem no máximo uma em voo e não consome rádio nem sessão de SDK.

### O que este ADR **não** decide

- **A biblioteca.** Decompose ou `Navigator` próprio fica para RFC seguinte,
  decidido com dados de spike. Decidir agora repetiria o erro que a dívida #2 já
  registra sobre a comparação de frameworks de DI feita de memória.
- **O mecanismo no Android** (Activity por destino ou raiz única com Compose). O
  argumento de fidelidade de transição que motivou o container é **inteiramente
  iOS** — no Android não existe transição de push cuja qualidade se perca ao
  desenhar em Compose. A escolha depende da biblioteca e fica com ela.

O que vale para as duas plataformas é o **contrato e a posse da pilha**, que é o
que este ADR fixa e o que é caro reverter.

## Consequências

**A favor:**
- Swipe-back é o do UIKit, não uma imitação — inclusive nas transições
  Compose → Compose. Mantém a paridade com o app que está sendo substituído.
- Tela nativa como hub funciona, com Compose empilhado por cima. É o único modelo
  que suporta intercalação arbitrária, e agora está validado em device.
- A política de navegação fica em `commonMain`, testável em `commonTest` com um
  launcher fake — é o que impede este fluxo de virar mais um buraco de cobertura
  como o da dívida #9.
- Fecha o spike 3 do plano de validação do ADR 0001 do `imou-smart-home`, pendente
  desde julho. O número medido serve aos dois repositórios.

**Contra (aceito):**
- **Um adaptador de pilha por plataforma, e ele é bidirecional.** O Kotlin empurra
  rotas, mas o usuário também desempilha por gesto, e a plataforma se move antes de
  o Kotlin saber. O adaptador precisa reconhecer que a mudança que observa foi
  causada por ele mesmo, ou emite uma segunda ordem e duas telas voltam. É a parte
  de mais alto risco do desenho, e nenhuma biblioteca a elimina — ela nasce de duas
  pilhas reais existirem ao mesmo tempo.
- **Com N raízes Compose, estado compartilhado entre telas não cabe mais na
  composição.** Cada destino tem composição própria, isolada. Isso torna
  **obrigatória** uma camada que guarde, por destino, ciclo de vida, estado
  restaurável e instâncias retidas. Não é opcional e não é barato: à mão, é
  reconstruir `ViewModelStore` + `SavedStateHandle` para duas plataformas.
- **Custo de memória por raiz não foi medido.** O instrumento falhou três vezes
  (delta por push, acumulado desde o baseline, série assentada): PSS e
  `phys_footprint` amostrados pelo app não isolam custo de raiz nessa escala — a
  memória chegou a *cair* enquanto a pilha crescia. Aceito com base na ausência de
  jetsam empilhando dezenas de destinos num iPhone 8 de 2 GB, que é o aparelho e a
  condição em que o jetsam agiria. Medir direito exige Instruments/Memory Profiler.
- O template passa a ter navegação, que é peso que todo clone herda — inclusive os
  que não precisam de tela nativa nenhuma. Para esses, a opção 1 do RFC (Compose
  dono da pilha, nativo como folha) seria mais barata, e fica registrada lá.

**Passa a ser proibido:**
- Casca navegar no grafo, ou decidir para onde ir quando uma tela nativa fecha.
- Resultado de tela nativa voltando como payload de callback.
- Guardar dentro de uma composição estado que precise sobreviver à tela que o
  criou — com uma raiz por destino, isso é vazamento ou perda, nunca o que se
  esperava.
- Rota carregando objeto de domínio em vez de id.
- Aplicar o padrão de engine (`Cancellable`/`CallHandle`) a abertura de tela.

**Gatilho de reavaliação:**
- Medição em device mostrando custo por raiz alto o bastante para forçar limite de
  profundidade: aí a saída é a assimetria (container nativo só no iOS), não
  abandonar o desenho.
- O Compose iOS passar a entregar transição de push com fidelidade do UIKit: o
  motivo central desta decisão cai, e a opção 2 do RFC volta a ser suficiente.
- Um SDK cuja tela **não** possa ser apresentada pela casca (exigir ser a raiz da
  aplicação, por exemplo): o contrato de apresentação não a alcança.

## Supersede / Superseded by

Nasce do **RFC 0001**, que carrega a comparação de alternativas, a medição e os
métodos de medição descartados.

Complementa o **ADR 0004** e o **ADR 0006**, delimitando que excursão de tela
**não** é o padrão de engine — o SDD 03 passa a registrar essa fronteira.

Não supersede nenhum ADR. O **ADR 0003** (tempo de vida do composition root) segue
valendo sem alteração: o `AppGraph` continua vivendo no ciclo da aplicação, e agora
com mais razão — com N raízes Compose, ele é o único escopo que atravessa todas.
