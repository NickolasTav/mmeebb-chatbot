---
description: Orquestrador de execução paralela para tasks independentes e banca de revisores simultâneos
---

# Orquestrador Paralelo — Parallel Multi-Agent Pipeline (Projeto TCC)

Este workflow orienta e estrutura a execução de **agentes em paralelo**, acelerando o desenvolvimento e aprofundando as auditorias de qualidade.

---

## ⚡ Modos de Execução em Paralelo

### Modo 1: Tasks Independentes em Paralelo (Múltiplos Desenvolvedores)
Quando uma funcionalidade é quebrada em tarefas desacopladas (ex: `task-02-relatorios-pdf` e `task-03-i18n-espanhol`), elas podem ser disparadas concorrentemente.

```mermaid
flowchart TD
    MGR[Agente Gerente] -->|Decompõe Spec| T1[Task 01 - DB Base]
    T1 --> SYNC[Ponto de Sincronização]
    
    subgraph Paralelo ["Execução Concorrente em Paralelo"]
        SYNC --> SUB1[Subagente 1: Task 02 - DTOs & Endpoints]
        SYNC --> SUB2[Subagente 2: Task 03 - Templates i18n]
        SYNC --> SUB3[Subagente 3: Task 04 - Métricas Actuator]
    end
    
    SUB1 --> M_REV[Revisão e Integração]
    SUB2 --> M_REV
    SUB3 --> M_REV
```

#### Regras para Paralelização de Tasks:
1. **Sem Dependências Circulares**: Apenas dispare em paralelo tarefas cujos arquivos de saída não colidam diretamente.
2. **Isolamento de Contexto**: Cada subagente recebe seu arquivo de task individual e roda de forma independente.
3. **Sincronização Final**: Após o retorno de todos os subagentes paralelos, a suíte de testes global (`mvn test`) é executada para garantir integridade.

---

### Modo 2: Banca de Revisores Especializados em Paralelo (Multi-Agent Review)
Para entregas críticas de arquitetura, segurança ou motor MMEEBB, disparamos **3 subagentes de revisão simultaneamente**:

```mermaid
flowchart LR
    DEV_DIFF[Código Implementado / PR] --> REV_SEC[Subagente 1: Security & Injection Reviewer]
    DEV_DIFF --> REV_PERF[Subagente 2: Performance & Query Reviewer]
    DEV_DIFF --> REV_QA[Subagente 3: TDD & Edge Cases Reviewer]
    
    REV_SEC --> CONSOLIDATE[Consolidação de Laudo & Veredito Final]
    REV_PERF --> CONSOLIDATE
    REV_QA --> CONSOLIDATE
```

1. **Revisor 1 (Segurança & Resiliência)**: Analisa sanitização, injeção SQL/JPQL, exposição de tokens, tratamento de exceções e rate-limiting.
2. **Revisor 2 (Performance & Spring Data)**: Analisa queries N+1, isolamento de transações `@Transactional`, índices de banco e pools de conexão.
3. **Revisor 3 (Qualidade de Testes & TDD)**: Analisa cobertura de branches, asserts significativos, mocks precisos e ausência de testes *flaky*.

---

## 🛠️ Como Invocar no Antigravity

No Antigravity, a chamada paralela é realizada diretamente passando múltiplos subagentes no array `Subagents`:

```json
{
  "Subagents": [
    {
      "Role": "Security Auditor",
      "TypeName": "research",
      "Prompt": "Audite os arquivos modificados em busca de vulnerabilidades de segurança e injeção."
    },
    {
      "Role": "Performance Auditor",
      "TypeName": "research",
      "Prompt": "Audite as consultas Spring Data e repositórios em busca de queries N+1 e gargalos."
    },
    {
      "Role": "QA Test Specialist",
      "TypeName": "research",
      "Prompt": "Verifique a cobertura dos testes unitários criados e casos de borda não tratados."
    }
  ]
}
```

---

## 🚀 Como Executar via Script Python

Para execução em lote paralela via terminal:
```bash
# Executar tasks 2 a 4 em paralelo com 3 workers simultâneos
python scripts/run_parallel_tasks.py 2 4 --concurrency 3
```
