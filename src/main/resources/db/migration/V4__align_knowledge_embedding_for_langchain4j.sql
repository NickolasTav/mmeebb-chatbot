-- =============================================================================
-- Migration: V4__align_knowledge_embedding_for_langchain4j.sql
-- Descrição: Alinha as colunas de tb_knowledge_embedding com o contrato estrito
--            esperado pelo PgVectorEmbeddingStore do LangChain4j (embedding_id e text).
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

DO $$
BEGIN
    -- Renomeia id para embedding_id se ainda não foi renomeado
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tb_knowledge_embedding' AND column_name = 'id'
    ) THEN
        ALTER TABLE tb_knowledge_embedding RENAME COLUMN id TO embedding_id;
    END IF;

    -- Renomeia content para text se ainda não foi renomeado
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tb_knowledge_embedding' AND column_name = 'content'
    ) THEN
        ALTER TABLE tb_knowledge_embedding RENAME COLUMN content TO text;
    END IF;

    -- Garante que text aceite nulo caso necessário
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tb_knowledge_embedding' AND column_name = 'text' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE tb_knowledge_embedding ALTER COLUMN text DROP NOT NULL;
    END IF;
END $$;
