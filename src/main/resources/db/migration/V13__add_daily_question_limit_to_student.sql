-- =============================================================================
-- Migration: V13__add_daily_question_limit_to_student.sql
-- Descrição: Limite diário de questões por estudante.
--            A semeadura agenda todas as questões do curso para o primeiro dia, e o
--            aluno de Medicina recebia "faltam 381 hoje" logo no cadastro. A fila do
--            dia passa a ser cortada em um limite: revisões vencidas primeiro, questões
--            novas completam o que sobrar, o excedente fica para os dias seguintes.
--
--            Nulo = usa o padrão global (mmeebb.daily-limit.default). A faixa aceita
--            (mín./máx.) é configuração da aplicação, então o banco só garante que o
--            valor seja positivo.
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

ALTER TABLE tb_student
    ADD COLUMN IF NOT EXISTS daily_question_limit INTEGER NULL;

ALTER TABLE tb_student
    ADD CONSTRAINT ck_student_daily_question_limit
    CHECK (daily_question_limit IS NULL OR daily_question_limit > 0);

COMMENT ON COLUMN tb_student.daily_question_limit IS
    'Máximo de questões respondidas por dia escolhido pelo estudante; nulo usa o padrão global da aplicação.';
