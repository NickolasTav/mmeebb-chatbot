# Task 05: Implementação do Orquestrador de Fluxo Conversacional (FSM)

| Metadado | Detalhe |
| :--- | :--- |
| **Spec de Origem** | [`docs/specs/spec-chat-flow-orchestrator.md`](../specs/spec-chat-flow-orchestrator.md) |
| **Status** | Concluída |
| **Escopo do Commit** | `orchestrator` (ex: `feat(orchestrator): implementa maquina de estados conversacional e orquestrador de fluxo`) |
| **Complexidade** | G (Grande) |
| **Pode Rodar em Paralelo** | Não |
| **Dependências** | Task 04 |

---

## 🎯 Objetivo da Tarefa
Implementar a interface `ChatFlowOrchestrator` e a classe `ChatFlowOrchestratorImpl` no Spring Boot, gerenciando o ciclo de vida da sessão conversacional (`ChatSession`), comandos globais de reset/navegação, transição de estados via switch expression e integração com `MmeebbService`, `StudentRepository`, `ChatSessionRepository`, `RepetitionScheduleRepository`, `FlashcardRepository`, `CourseRepository`, `SubjectRepository` e `UazapiClientService`.

---

## 📋 Escopo Detalhado

### Dentro do Escopo (O que FAZER)
- [ ] Criar a interface `br.edu.unipam.tcc.service.ChatFlowOrchestrator`.
- [ ] Criar a implementação `br.edu.unipam.tcc.service.impl.ChatFlowOrchestratorImpl`.
- [ ] Tratar comandos globais de reset (`"menu"`, `"sair"`, `"inicio"` etc.).
- [ ] Implementar fluxo de novo contato (`NOVO` → cria `Student`, transiciona para `MENU_PRINCIPAL` e envia boas-vindas/menu).
- [ ] Implementar opções do menu principal (*1* - Revisão MMEEBB, *2* - Dúvidas RAG, *3* - Trocar Curso/Matéria).
- [ ] Implementar modo de revisão MMEEBB com apresentação de flashcard, processamento de resposta via `MmeebbService` ($IRA = 2^n$ e reset de penalidade) e avanço sequencial.
- [ ] Implementar ponto de extensão para o modo RAG.
- [ ] Desenvolver suíte completa de testes unitários com Mockito (`ChatFlowOrchestratorImplTest`).

### Fora do Escopo (O que NÃO fazer nesta tarefa)
- ❌ Não implementar a chamada real da API de embeddings/LLM LangChain4j (isso será no módulo RAG dedicado).

---

## 📂 Arquivos Afetados

| Operação | Caminho do Arquivo |
| :--- | :--- |
| `NEW` | `src/main/java/br/edu/unipam/tcc/service/ChatFlowOrchestrator.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/service/impl/ChatFlowOrchestratorImpl.java` |
| `NEW` | `src/test/java/br/edu/unipam/tcc/service/ChatFlowOrchestratorImplTest.java` |

---

## 🧪 Estratégia de TDD & Critérios de Aceitação

### Testes a Desenvolver Primeiro (Red)
1. **Smoke Test**: Injeção e inicialização do `ChatFlowOrchestratorImpl`.
2. **Testes Unitários**:
   - `deveCriarEstudanteESessaoQuandoEstadoForNovo()`
   - `deveResetarParaMenuPrincipalQuandoReceberComandoMenuOuSair()`
   - `deveIniciarModoRevisaoComPrimeiroFlashcardPendente()`
   - `deveInformarQuandoNaoHouverFlashcardsPendentes()`
   - `deveProcessarRespostaCorretaNoModoRevisaoEAtualizarMmeebb()`
   - `deveProcessarRespostaIncorretaNoModoRevisaoEResetarIntervalo()`
   - `deveTransicionarParaModoRagQuandoSelecionarOpcao2()`
   - `deveExibirOpcaoInvalidaQuandoEntradaForDesconhecidaNoMenu()`

### Critérios de Aceitação (Definition of Done)
- [ ] 100% dos testes unitários passam com cobertura de todas as ramificações do switch de estados.
- [ ] Controle transacional `@Transactional` aplicado no processamento da mensagem.
- [ ] Formatação nativa de mensagens para WhatsApp (negrito `*`, quebras de linha e emojis).
