# Task 06: Integração do Consumidor RabbitMQ com o Orquestrador de Fluxo

| Metadado | Detalhe |
| :--- | :--- |
| **Spec de Origem** | [`docs/specs/spec-chat-flow-orchestrator.md`](../specs/spec-chat-flow-orchestrator.md) |
| **Status** | Concluída |
| **Escopo do Commit** | `consumer` (ex: `feat(consumer): conecta whatsapp message consumer ao orquestrador de fluxo com typing presence`) |
| **Complexidade** | M (Média) |
| **Pode Rodar em Paralelo** | Não |
| **Dependências** | Task 05 |

---

## 🎯 Objetivo da Tarefa
Conectar o `WhatsappMessageConsumer` ao `ChatFlowOrchestrator`, executando simulação de presença de digitação (*composing*) com atraso controlado (1000ms a 1500ms) para comportamento humano e anti-ban antes de delegar a mensagem para o orquestrador, com tratamento estruturado de exceções via SLF4J.

---

## 📋 Escopo Detalhado

### Dentro do Escopo (O que FAZER)
- [ ] Atualizar `WhatsappMessageConsumer` para injetar `ChatFlowOrchestrator`.
- [ ] No método `consumeIncomingMessage`:
  - Validar payload e telefone do remetente.
  - Enviar presença `"composing"` via `UazapiClientService`.
  - Aplicar delay seguro de digitação (1000-1500ms).
  - Invocar `chatFlowOrchestrator.processIncomingMessage(incomingDto)`.
  - Enviar presença `"paused"` no bloco finally / pós-processamento.
  - Tratar exceções com logs estruturados de `ERROR` e `WARN`.
- [ ] Atualizar testes unitários em `WhatsappMessageConsumerTest` cobrindo o fluxo de entrada integrado.

### Fora do Escopo (O que NÃO fazer nesta tarefa)
- ❌ Não alterar a lógica interna de processamento de estados da FSM (já implementada na Task 05).

---

## 📂 Arquivos Afetados

| Operação | Caminho do Arquivo |
| :--- | :--- |
| `MODIFY` | `src/main/java/br/edu/unipam/tcc/consumer/WhatsappMessageConsumer.java` |
| `MODIFY` | `src/test/java/br/edu/unipam/tcc/consumer/WhatsappMessageConsumerTest.java` |

---

## 🧪 Estratégia de TDD & Critérios de Aceitação

### Testes a Desenvolver Primeiro (Red)
1. **Smoke Test**: Injeção do `ChatFlowOrchestrator` no `WhatsappMessageConsumer`.
2. **Testes Unitários**:
   - `deveConsumirMensagemDeEntradaSimularPresencaEDelegarAoOrquestrador()`
   - `deveIgnorarMensagemComPayloadNuloOuSemTelefone()`
   - `deveTratarExcecaoNoOrquestradorSemDerrubarConsumer()`

### Critérios de Aceitação (Definition of Done)
- [ ] `WhatsappMessageConsumerTest` passa com 100% de sucesso.
- [ ] Todos os 60+ testes da aplicação executam com `./mvnw test` e passam verdes.
