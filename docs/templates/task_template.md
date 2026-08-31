# Task {{NN}}: {{Nome da Tarefa}}

| Metadado | Detalhe |
| :--- | :--- |
| **Spec de Origem** | [`docs/specs/spec-{{nome-da-spec}}.md`](../specs/spec-{{nome-da-spec}}.md) |
| **Status** | Pendente \| Em Andamento \| Em Review \| Concluída |
| **Escopo do Commit** | `{{escopo-semantico}}` (ex: `feat({{escopo}}): ...`) |
| **Complexidade** | P (Pequena) \| M (Média) \| G (Grande) |
| **Pode Rodar em Paralelo** | Sim \| Não (Depende da Task {{XX}}) |
| **Dependências** | {{Nenhuma | Task-01}} |

---

## 🎯 Objetivo da Tarefa
{{Descreva em 1 ou 2 parágrafos o que esta tarefa realiza e por que é necessária.}}

---

## 📋 Escopo Detalhado

### Dentro do Escopo (O que FAZER)
- [ ] {{Sub-tarefa 1}}
- [ ] {{Sub-tarefa 2}}
- [ ] {{Sub-tarefa 3}}

### Fora do Escopo (O que NÃO fazer nesta tarefa)
- ❌ {{Item fora de escopo}}

---

## 📂 Arquivos Afetados

| Operação | Caminho do Arquivo |
| :--- | :--- |
| `NEW` / `MODIFY` | `{{caminho/do/arquivo}}` |

---

## 🧪 Estratégia de TDD & Critérios de Aceitação

### Testes a Desenvolver Primeiro (Red)
1. **Smoke Test**: `{{NomeDaClasseTest}}` — Teste mínimo de fiação e inicialização.
2. **Testes Unitários**:
   - `deve{{CenarioEsperado}}Quando{{Condicao}}()`
   - `deveLancarExcecaoQuando{{EntradaInvalida}}()`

### Critérios de Aceitação (Definition of Done)
- [ ] Todos os testes unitários criados passam com 100% de sucesso.
- [ ] Código atende às regras de Clean Code e arquitetura em camadas do projeto.
- [ ] Nenhuma credencial ou hardcode presente.
- [ ] Código aprovado no Code Review (`reviewer.md`).
