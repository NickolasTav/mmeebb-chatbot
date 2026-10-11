-- =============================================================================
-- Migration: V12__add_last_correct_position_to_schedule.sql
-- Descrição: Alternativas embaralhadas a cada apresentação (decisão D11 da spec-10).
--            Guarda, por aluno e questão, a posição em que a alternativa correta
--            apareceu na última apresentação, para que a próxima sorteie outra letra.
--            Sem essa memória, quem decora "a resposta desta questão é a B" acerta
--            sem saber o conteúdo, e o MMEEBB mede memória de letra, não do conceito.
--
--            Índice 0-based (0 = A). Nulo = a questão ainda não foi apresentada
--            depois desta migration (qualquer posição vale na próxima vez).
--            O gabarito em tb_flashcard não muda.
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

ALTER TABLE tb_repetition_schedule
    ADD COLUMN IF NOT EXISTS last_correct_position SMALLINT NULL;

ALTER TABLE tb_repetition_schedule
    ADD CONSTRAINT ck_schedule_last_correct_position
    CHECK (last_correct_position IS NULL OR last_correct_position >= 0);

COMMENT ON COLUMN tb_repetition_schedule.last_correct_position IS
    'Posição (0 = A) da alternativa correta na última apresentação da questão ao aluno; nula se nunca apresentada após a V12.';

-- A explicação da questão de colisão em tabela hash (V3) citava "A alternativa C". Com as
-- alternativas embaralhadas a letra muda a cada apresentação, então a referência passa a ser
-- pelo conteúdo. A V3 não pode ser editada.
UPDATE tb_flashcard
SET explanation = REPLACE(explanation,
        'A alternativa C descreve o Endereçamento Aberto com Sondagem Linear.',
        'Percorrer as posições seguintes até achar uma vazia é outra técnica: o Endereçamento Aberto com Sondagem Linear.'),
    updated_at = CURRENT_TIMESTAMP
WHERE topic = 'Tabelas Hash - Tratamento de Colisões'
  AND explanation LIKE '%A alternativa C descreve o Endereçamento Aberto%';
