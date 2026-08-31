# Task 02: Criação das Entidades JPA Dinâmicas e Repositórios

| Metadado | Detalhe |
| :--- | :--- |
| **Spec de Origem** | [`docs/specs/spec-mmeebb-dinamico-core.md`](../specs/spec-mmeebb-dinamico-core.md) |
| **Status** | Concluída |
| **Escopo do Commit** | `jpa-entities` (ex: `feat(entities): adiciona entidades jpa multi-curso e agendamento`) |
| **Complexidade** | M (Média) |
| **Pode Rodar em Paralelo** | Não |
| **Dependências** | Task 01 |

---

## 🎯 Objetivo da Tarefa
Mapear as tabelas do schema relacional dinâmico em entidades JPA robustas e expressivas (`Course`, `Subject`, `Student`, `StudentCourse`, `Flashcard`, `RepetitionSchedule`, `ChatSession`, `KnowledgeEmbedding`), incluindo constraints, enums (`QuestionType`, `DifficultyLevel`, `ScheduleStatus`, `ChatState`), versionamento com `@Version` para concorrência otimista e seus respectivos repositórios Spring Data JPA.

---

## 📋 Escopo Detalhado

### Dentro do Escopo (O que FAZER)
- [ ] Criar enums de domínio: `QuestionType`, `DifficultyLevel`, `ScheduleStatus`, `ChatState`.
- [ ] Criar entidades JPA no pacote `br.edu.unipam.tcc.entity`:
  - `Course`
  - `Subject`
  - `Student`
  - `StudentCourse`
  - `Flashcard`
  - `RepetitionSchedule` (com `@Version private Long version`)
  - `ChatSession`
  - `KnowledgeEmbedding`
- [ ] Criar interfaces de repositório no pacote `br.edu.unipam.tcc.repository`:
  - `CourseRepository`
  - `SubjectRepository`
  - `StudentRepository`
  - `FlashcardRepository`
  - `RepetitionScheduleRepository`
  - `ChatSessionRepository`
  - `KnowledgeEmbeddingRepository`
- [ ] Criar testes unitários de mapeamento/instanciação de entidades.

### Fora do Escopo (O que NÃO fazer nesta tarefa)
- ❌ Não implementar serviços de orquestração do WhatsApp ou Gemini nesta task.

---

## 📂 Arquivos Afetados

| Operação | Caminho do Arquivo |
| :--- | :--- |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/enums/QuestionType.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/enums/DifficultyLevel.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/enums/ScheduleStatus.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/enums/ChatState.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/Course.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/Subject.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/Student.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/StudentCourse.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/Flashcard.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/RepetitionSchedule.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/ChatSession.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/entity/KnowledgeEmbedding.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/repository/CourseRepository.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/repository/SubjectRepository.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/repository/StudentRepository.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/repository/FlashcardRepository.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/repository/RepetitionScheduleRepository.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/repository/ChatSessionRepository.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/repository/KnowledgeEmbeddingRepository.java` |
| `NEW` | `src/test/java/br/edu/unipam/tcc/entity/EntityInstantiationTest.java` |

---

## 🧪 Estratégia de TDD & Critérios de Aceitação

### Testes a Desenvolver Primeiro (Red)
1. **Smoke / Instantiation Test**: `EntityInstantiationTest` — Valida a integridade dos construtores, builders e valores padrão das entidades.

### Critérios de Aceitação (Definition of Done)
- [ ] Todas as entidades JPA mapeiam as tabelas da migration `V1__init_schema.sql` corretamente.
- [ ] Repositórios expõem métodos de busca derivados (ex: `findByPhoneNumber`, `findByCourseIdAndActiveTrue`, `findPendingReviewsByStudentAndDate`).
- [ ] Testes unitários passam com 100% de sucesso via `./mvnw.cmd test`.
