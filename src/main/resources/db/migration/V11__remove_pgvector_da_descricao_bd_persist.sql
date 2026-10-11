-- =============================================================================
-- Migration: V11__remove_pgvector_da_descricao_bd_persist.sql
-- Descrição: Tira o pgvector da descrição da disciplina BD_PERSIST, sobra da base
--            de conhecimento (RAG) removida na V9. A V2 não pode ser editada, então
--            a descrição é atualizada aqui. Índices HNSW continuam como conteúdo de SI.
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

UPDATE tb_subject
SET description = 'Modelagem Relacional, Normalização, SQL, Transações ACID, Índices B-Tree e HNSW, NoSQL.'
WHERE code = 'BD_PERSIST';
