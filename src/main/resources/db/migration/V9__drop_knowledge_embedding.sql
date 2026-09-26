-- =============================================================================
-- Migration: V9__drop_knowledge_embedding.sql
-- Descrição: Remove a base vetorial do RAG. As dúvidas passaram a ser respondidas
--            livremente pela IA com o contexto do aluno (spec-09). A extensão vector
--            continua instalada porque a V1 a cria e não pode ser editada.
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

DROP TABLE IF EXISTS tb_knowledge_embedding;
