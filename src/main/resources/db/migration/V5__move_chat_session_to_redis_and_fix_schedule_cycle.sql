-- =============================================================================
-- Migration: V5__move_chat_session_to_redis_and_fix_schedule_cycle.sql
-- Descrição: (1) Remove tb_chat_session — o estado conversacional passa a viver no
--            Redis com TTL, dispensando escrita relacional a cada troca de estado.
--            (2) Reabre os agendamentos encerrados como COMPLETED: no MMEEBB a
--            repetição é cíclica, e o status terminal impedia o card de retornar
--            quando nextReviewDate vencesse.
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

DROP TABLE IF EXISTS tb_chat_session;

UPDATE tb_repetition_schedule
SET status = 'PENDING'
WHERE status = 'COMPLETED';
