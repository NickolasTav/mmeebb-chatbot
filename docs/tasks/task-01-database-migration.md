# Task 01: Setup do Projeto e Migration Flyway do Schema Dinâmico

| Metadado | Detalhe |
| :--- | :--- |
| **Spec de Origem** | [`docs/specs/spec-mmeebb-dinamico-core.md`](../specs/spec-mmeebb-dinamico-core.md) |
| **Status** | Concluída |
| **Escopo do Commit** | `schema-flyway` (ex: `feat(schema): inicializa schema dinamico flyway com pgvector`) |
| **Complexidade** | M (Média) |
| **Pode Rodar em Paralelo** | Não |
| **Dependências** | Nenhuma |

---

## 🎯 Objetivo da Tarefa
Inicializar a estrutura do projeto Spring Boot 3 (Maven `pom.xml`, `application.yml`, classe principal `MmeebbChatbotApplication`) e criar o script Flyway `V1__init_schema.sql` com a modelagem relacional dinâmica multi-curso (`tb_course`, `tb_subject`, `tb_student`, `tb_student_course`, `tb_flashcard`, `tb_repetition_schedule`, `tb_chat_session`) e suporte ao banco vetorial `pgvector` (`tb_knowledge_embedding`) com índices de performance.

---

## 📋 Escopo Detalhado

### Dentro do Escopo (O que FAZER)
- [ ] Configurar `pom.xml` com Java 17/21, Spring Boot 3.3.x, Spring Data JPA, PostgreSQL, Flyway, pgvector, LangChain4j Gemini, RabbitMQ, Lombok, Actuator, JUnit 5, Mockito.
- [ ] Configurar `src/main/resources/application.yml` e `src/test/resources/application-test.yml`.
- [ ] Criar classe principal `MmeebbChatbotApplication.java`.
- [ ] Criar o script de migração Flyway `src/main/resources/db/migration/V1__init_schema.sql`.
- [ ] Criar Smoke Test `MmeebbChatbotApplicationTests.java` para validar compilação e carregamento de contexto.

### Fora do Escopo (O que NÃO fazer nesta tarefa)
- ❌ Não implementar entidades JPA completas nesta task (será na Task 02).
- ❌ Não implementar regras de negócio de IA ou WhatsApp (será em tasks posteriores).

---

## 📂 Arquivos Afetados

| Operação | Caminho do Arquivo |
| :--- | :--- |
| `NEW` | `pom.xml` |
| `NEW` | `src/main/resources/application.yml` |
| `NEW` | `src/test/resources/application-test.yml` |
| `NEW` | `src/main/resources/db/migration/V1__init_schema.sql` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/MmeebbChatbotApplication.java` |
| `NEW` | `src/test/java/br/edu/unipam/tcc/MmeebbChatbotApplicationTests.java` |

---

## 🧪 Estratégia de TDD & Critérios de Aceitação

### Testes a Desenvolver Primeiro (Red)
1. **Smoke Test**: `MmeebbChatbotApplicationTests` — Valida compilação e bootstrap da aplicação.

### Critérios de Aceitação (Definition of Done)
- [ ] O script `V1__init_schema.sql` contém a criação de todas as tabelas com tipos, constraints, chaves estrangeiras e índices B-tree/GIN/HNSW.
- [ ] O projeto compila com sucesso via `./mvnw.cmd clean compile test-compile`.
