-- =============================================================================
-- Migration: V7__add_student_preferences.sql
-- Descrição: Preferências editáveis pelo próprio aluno no menu de Configurações do WhatsApp.
--            (1) preferred_name: apelido de tratamento, separado do nome completo, que
--                permanece como identidade acadêmica vinculada ao RA.
--            (2) review_notifications_enabled: permite pausar o lembrete diário sem
--                desativar o estudante no sistema.
--            (3) last_review_notification_on: dia em que o scheduler já avaliou o aluno.
--                Como a rotina passa a rodar a cada 5 minutos (para atender o horário
--                individual de cada um), essa marca garante um único lembrete por dia,
--                sobrevive a restart e recupera o envio perdido se a aplicação cair.
--            (4) preferred_study_time (existente desde a V1) passa a ser obrigatório:
--                agora define o horário do lembrete de cada estudante.
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

ALTER TABLE tb_student ADD COLUMN IF NOT EXISTS preferred_name VARCHAR(30);
ALTER TABLE tb_student ADD COLUMN IF NOT EXISTS review_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tb_student ADD COLUMN IF NOT EXISTS last_review_notification_on DATE;

UPDATE tb_student SET preferred_study_time = '08:00:00' WHERE preferred_study_time IS NULL;
ALTER TABLE tb_student ALTER COLUMN preferred_study_time SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_student_review_notification_due
    ON tb_student (preferred_study_time)
    WHERE active = TRUE AND review_notifications_enabled = TRUE;
