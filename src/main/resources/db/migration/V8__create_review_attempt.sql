-- =============================================================================
-- Migration: V8__create_review_attempt.sql
-- Descrição: Histórico de tentativas de revisão — fundação da personalização
--            adaptativa por desempenho (Tech Spec 08).
--
--            Até aqui o resultado da correção semântica feita pela IA era usado
--            para atualizar o índice N e descartado em seguida. Sem esse registro
--            não há como identificar em quais conteúdos o estudante erra mais,
--            que é exatamente o que o Projeto de Trabalho promete.
--
--            n_index_before / n_index_after / topic_mastery não são redundantes:
--            registram a decisão tomada e o estado que a produziu, permitindo
--            reconstruir na monografia por que cada intervalo foi escolhido,
--            mesmo depois que os limiares de configuração mudarem.
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

CREATE TABLE IF NOT EXISTS tb_review_attempt (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          UUID        NOT NULL REFERENCES tb_student(id),
    flashcard_id        BIGINT      NOT NULL REFERENCES tb_flashcard(id),
    correct             BOOLEAN     NOT NULL,
    n_index_before      INTEGER     NOT NULL,
    n_index_after       INTEGER     NOT NULL,
    interval_days_after INTEGER     NOT NULL,
    topic_mastery       VARCHAR(20) NOT NULL,
    answered_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Cobre a agregação por janela temporal: filtro por estudante + answered_at >= início da janela.
CREATE INDEX IF NOT EXISTS idx_review_attempt_student_answered
    ON tb_review_attempt (student_id, answered_at DESC);

-- Cobre o JOIN com tb_flashcard, de onde vêm subject_id e topic na agregação por tópico.
CREATE INDEX IF NOT EXISTS idx_review_attempt_flashcard
    ON tb_review_attempt (flashcard_id);
