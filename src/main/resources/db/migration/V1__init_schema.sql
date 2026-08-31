-- =============================================================================
-- Migration: V1__init_schema.sql
-- Descrição: Modelagem Dinâmica Multi-Curso / Multi-Disciplina para Chatbot MMEEBB
--            e Suporte a Vetores Semânticos com Particionamento no RAG (pgvector)
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

-- 1. Habilitação de Extensões Essenciais
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tabela de Cursos (Multi-Curso: Medicina, Sistemas de Informação, Direito, etc.)
CREATE TABLE IF NOT EXISTS tb_course (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. Tabela de Matérias / Disciplinas vinculadas ao Curso
CREATE TABLE IF NOT EXISTS tb_subject (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES tb_course(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_subject_course_code UNIQUE (course_id, code)
);

-- 4. Tabela de Alunos / Estudantes
CREATE TABLE IF NOT EXISTS tb_student (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    ra VARCHAR(30) UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    preferred_study_time TIME WITHOUT TIME ZONE DEFAULT '08:00:00',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 5. Tabela Associativa: Vínculo de Matrícula do Estudante com Cursos
CREATE TABLE IF NOT EXISTS tb_student_course (
    id BIGSERIAL PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES tb_student(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES tb_course(id) ON DELETE CASCADE,
    academic_period INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_student_course UNIQUE (student_id, course_id)
);

-- 6. Tabela de Flashcards e Questões (vinculada à Matéria)
CREATE TABLE IF NOT EXISTS tb_flashcard (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL REFERENCES tb_subject(id) ON DELETE CASCADE,
    topic VARCHAR(150) NOT NULL,
    question_type VARCHAR(30) NOT NULL DEFAULT 'FLASHCARD',
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    options_json JSONB,
    explanation TEXT,
    difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    source VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 7. Tabela do Motor MMEEBB: Rastreamento do Progresso de Repetição Espaçada por Aluno e Flashcard
CREATE TABLE IF NOT EXISTS tb_repetition_schedule (
    id BIGSERIAL PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES tb_student(id) ON DELETE CASCADE,
    flashcard_id BIGINT NOT NULL REFERENCES tb_flashcard(id) ON DELETE CASCADE,
    n_index INT NOT NULL DEFAULT 0 CHECK (n_index >= 0 AND n_index <= 13),
    interval_days INT NOT NULL DEFAULT 1,
    repetition_count INT NOT NULL DEFAULT 0,
    consecutive_correct INT NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    next_review_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'NOTIFIED', 'COMPLETED', 'OVERDUE')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_schedule_student_flashcard UNIQUE (student_id, flashcard_id)
);

-- 8. Tabela de Sessões Conversacionais do Chatbot (Máquina de Estados)
CREATE TABLE IF NOT EXISTS tb_chat_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID REFERENCES tb_student(id) ON DELETE SET NULL,
    phone_number VARCHAR(30) NOT NULL UNIQUE,
    current_state VARCHAR(50) NOT NULL DEFAULT 'NOVO',
    selected_course_id BIGINT REFERENCES tb_course(id) ON DELETE SET NULL,
    selected_subject_id BIGINT REFERENCES tb_subject(id) ON DELETE SET NULL,
    current_flashcard_id BIGINT REFERENCES tb_flashcard(id) ON DELETE SET NULL,
    context_data JSONB,
    last_interaction_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 9. Tabela de Embeddings Vetoriais para RAG Particionado por Metadados (LangChain4j / pgvector)
CREATE TABLE IF NOT EXISTS tb_knowledge_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id BIGINT REFERENCES tb_course(id) ON DELETE CASCADE,
    subject_id BIGINT REFERENCES tb_subject(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    embedding vector(768),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- =============================================================================
-- ÍNDICES DE PERFORMANCE E INTEGRIDADE
-- =============================================================================

-- Índices em Chaves Estrangeiras
CREATE INDEX IF NOT EXISTS idx_subject_course_id ON tb_subject(course_id);
CREATE INDEX IF NOT EXISTS idx_student_course_student ON tb_student_course(student_id);
CREATE INDEX IF NOT EXISTS idx_student_course_course ON tb_student_course(course_id);
CREATE INDEX IF NOT EXISTS idx_flashcard_subject_id ON tb_flashcard(subject_id);
CREATE INDEX IF NOT EXISTS idx_flashcard_topic ON tb_flashcard(topic);
CREATE INDEX IF NOT EXISTS idx_schedule_student_id ON tb_repetition_schedule(student_id);
CREATE INDEX IF NOT EXISTS idx_schedule_flashcard_id ON tb_repetition_schedule(flashcard_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_student ON tb_chat_session(student_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_course ON tb_knowledge_embedding(course_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_subject ON tb_knowledge_embedding(subject_id);

-- Índice Crítico para Agendamento e Varredura Diária de Revisões Pendentes
CREATE INDEX IF NOT EXISTS idx_schedule_next_review_status ON tb_repetition_schedule(next_review_date, status);

-- Índice GIN para Filtragem Ultrarrápida por Metadados no RAG
CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_metadata ON tb_knowledge_embedding USING gin (metadata);

-- Índice HNSW para Busca por Similaridade de Cosseno no pgvector
CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_vector ON tb_knowledge_embedding USING hnsw (embedding vector_cosine_ops);
