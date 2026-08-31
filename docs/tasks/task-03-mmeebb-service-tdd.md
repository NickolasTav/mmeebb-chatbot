# Task 03: Implementação do MmeebbService com Ciclo TDD Completo

| Metadado | Detalhe |
| :--- | :--- |
| **Spec de Origem** | [`docs/specs/spec-mmeebb-dinamico-core.md`](../specs/spec-mmeebb-dinamico-core.md) |
| **Status** | Concluída |
| **Escopo do Commit** | `mmeebb-engine` (ex: `feat(engine): implementa calculo do intervalo 2^n do mmeebb`) |
| **Complexidade** | M (Média) |
| **Pode Rodar em Paralelo** | Não |
| **Dependências** | Task 02 |

---

## 🎯 Objetivo da Tarefa
Implementar o motor de repetição espaçada MMEEBB (`MmeebbService` e `MmeebbServiceImpl`) seguindo rigorosamente o ciclo TDD (Red -> Green -> Refactor), cobrindo o cálculo do intervalo exponencial na base binária ($IRA = 2^N$ para $N \in [0, 13]$), a progressão de acertos consecutivos, o reset em caso de erros e a inicialização de novos agendamentos.

---

## 📋 Escopo Detalhado

### Dentro do Escopo (O que FAZER)
- [ ] Criar a interface `MmeebbService` no pacote `br.edu.unipam.tcc.service`.
- [ ] Criar a suíte de testes unitários `MmeebbServiceImplTest` com JUnit 5 antes do código de produção (Fase Red).
- [ ] Implementar `MmeebbServiceImpl` no pacote `br.edu.unipam.tcc.service.impl` para fazer todos os testes passarem (Fase Green).
- [ ] Validar cenários de borda: $N=0$ até $N=13$, saturação acima de 13, exceção para números negativos, cálculo de `next_review_date` a partir de `LocalDate.now()`, e transições completas de acerto/erro no `RepetitionSchedule`.

### Fora do Escopo (O que NÃO fazer nesta tarefa)
- ❌ Não integrar com webhook ou controllers do WhatsApp ainda.
- ❌ Não criar agendador `@Scheduled` nesta task (será em módulo posterior).

---

## 📂 Arquivos Afetados

| Operação | Caminho do Arquivo |
| :--- | :--- |
| `NEW` | `src/main/java/br/edu/unipam/tcc/service/MmeebbService.java` |
| `NEW` | `src/main/java/br/edu/unipam/tcc/service/impl/MmeebbServiceImpl.java` |
| `NEW` | `src/test/java/br/edu/unipam/tcc/service/MmeebbServiceImplTest.java` |

---

## 🧪 Estratégia de TDD & Critérios de Aceitação

### Testes a Desenvolver Primeiro (Red)
1. **Smoke Test**: `deveCalcularIntervaloInicialDeUmDiaQuandoNZero()` ($2^0 = 1$).
2. **Cálculo da Tabela Binária Exponencial**:
   - `deveCalcularIntervalosExponenciaisDeZeroATrezeCorretamente()` (1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192 dias).
3. **Casos Limítrofes**:
   - `deveSaturarEmTrezeQuandoNFoxMaiorQueTreze()`.
   - `deveLancarExcecaoQuandoNFoxNegativo()`.
4. **Ciclo de Respostas**:
   - `deveAvancarCicloEDobrarIntervaloQuandoRespostaCorreta()`.
   - `deveResetarParaZeroEIntervaloDeUmDiaQuandoRespostaIncorreta()`.
   - `deveInicializarNovoAgendamentoComNZeroEIntervaloDeUmDia()`.

### Critérios de Aceitação (Definition of Done)
- [ ] 100% dos testes unitários do MMEEBB passam com sucesso no Maven.
- [ ] O código segue Clean Code, injeção de dependências sem `@Autowired` em campos e sem comentários óbvios.
- [ ] Documentação da task atualizada.
