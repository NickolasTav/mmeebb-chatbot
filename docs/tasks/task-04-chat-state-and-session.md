# Task 04: Atualização do Enum ChatState e ChatSession

| Metadado | Detalhe |
| :--- | :--- |
| **Spec de Origem** | [`docs/specs/spec-chat-flow-orchestrator.md`](../specs/spec-chat-flow-orchestrator.md) |
| **Status** | Concluída |
| **Escopo do Commit** | `chat-state` (ex: `refactor(chat-state): atualiza enum de estados conversacionais para pt-br`) |
| **Complexidade** | P (Pequena) |
| **Pode Rodar em Paralelo** | Não |
| **Dependências** | Nenhuma |

---

## 🎯 Objetivo da Tarefa
Atualizar o enum `ChatState` para os estados padronizados em português (`NOVO`, `MENU_PRINCIPAL`, `SELECIONANDO_CURSO`, `SELECIONANDO_MATERIA`, `MODO_REVISAO_MMEEBB`, `MODO_RAG_DUVIDAS`), ajustar o valor default na entidade `ChatSession` e atualizar os testes de instanciação existentes.

---

## 📋 Escopo Detalhado

### Dentro do Escopo (O que FAZER)
- [ ] Atualizar `br.edu.unipam.tcc.entity.enums.ChatState` com os novos valores.
- [ ] Atualizar o valor default em `br.edu.unipam.tcc.entity.ChatSession` para `ChatState.NOVO`.
- [ ] Atualizar `V1__init_schema.sql` para garantir default `'NOVO'`.
- [ ] Atualizar os testes unitários em `EntityInstantiationTest` para validar `ChatState.NOVO`.

### Fora do Escopo (O que NÃO fazer nesta tarefa)
- ❌ Não implementar a lógica do orquestrador ou regras do switch nesta task.

---

## 📂 Arquivos Afetados

| Operação | Caminho do Arquivo |
| :--- | :--- |
| `MODIFY` | `src/main/java/br/edu/unipam/tcc/entity/enums/ChatState.java` |
| `MODIFY` | `src/main/java/br/edu/unipam/tcc/entity/ChatSession.java` |
| `MODIFY` | `src/main/resources/db/migration/V1__init_schema.sql` |
| `MODIFY` | `src/test/java/br/edu/unipam/tcc/entity/EntityInstantiationTest.java` |

---

## 🧪 Estratégia de TDD & Critérios de Aceitação

### Testes a Desenvolver Primeiro (Red)
1. **Smoke Test**: `EntityInstantiationTest` — `deveInstanciarChatSessionCorretamente()` validando `ChatState.NOVO`.

### Critérios de Aceitação (Definition of Done)
- [ ] Enum contém exatamente: `NOVO`, `MENU_PRINCIPAL`, `SELECIONANDO_CURSO`, `SELECIONANDO_MATERIA`, `MODO_REVISAO_MMEEBB`, `MODO_RAG_DUVIDAS`.
- [ ] `EntityInstantiationTest` compila e passa com 100% de sucesso.
