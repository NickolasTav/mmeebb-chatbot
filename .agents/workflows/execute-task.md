---
description: Pipeline sequencial completo — executa uma task passando por Desenvolvedor (TDD), Revisor (Code Review) e Escriba (Commit)
---

# Orquestrador de Task — Pipeline Sequencial (Projeto TCC)

Este workflow automatiza o ciclo completo de execução de uma tarefa individual, garantindo qualidade antes do commit.

## Entrada

O usuário indicará a tarefa a ser executada (ex: *"Execute a pipeline para `docs/tasks/task-01-database-migration.md`"*).

## Fluxo Automatizado da Esteira

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Agente Desenvolvedor
    actor Rev as Agente Revisor
    actor Esc as Agente Escriba
    
    Dev->>Dev: Lê Spec & Task
    Dev->>Dev: Escreve Smoke Tests & TDD (Red)
    Dev->>Dev: Implementa Código de Produção (Green)
    Dev->>Dev: Refatora & Roda Maven Tests
    
    loop Loop de Code Review
        Dev->>Rev: Submete Diff para Auditoria
        Rev->>Rev: Aplica Checklist Multidimensional
        alt Findings Críticos ou Altos
            Rev-->>Dev: Requer Correções (findings)
            Dev->>Dev: Corrige e Re-testa
        else Aprovado
            Rev-->>Esc: Código Aprovado para Merge
        end
    end
    
    Esc->>Esc: Formata Conventional Commit
    Esc->>Esc: Atualiza Status da Task para Concluída
    Esc->>Esc: Executa git commit
```

---

### Passo 1: Desenvolvedor (TDD & Implementação)
1. Carregue o workflow `.agents/workflows/developer.md`.
2. Escreva os testes unitários e smoke tests primeiro.
3. Implemente a regra de negócio e código de produção.
4. Valide a compilação e os testes com Maven (`mvn test`).

### Passo 2: Revisor (Code Review)
5. Carregue o workflow `.agents/workflows/reviewer.md`.
6. Avalie o checklist (Clean Code, Spring Boot, Postgres, RabbitMQ, Segurança, Cobertura).
7. Se reprovado por itens Críticos ou Altos, retorne ao Passo 1 até obter **Aprovado**.

### Passo 3: Escriba (Commit Semântico)
8. Carregue o workflow `.agents/workflows/commit.md`.
9. Atualize o status da task em `docs/tasks/` para **Concluída**.
10. Execute o commit semântico.
