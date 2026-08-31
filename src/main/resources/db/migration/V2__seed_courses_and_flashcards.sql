-- =============================================================================
-- Migration: V2__seed_courses_and_flashcards.sql
-- Descrição: Carga Inicial de Cursos, Disciplinas e Questões/Flashcards para
--            Medicina e Sistemas de Informação (Validação de Banco e Testes MMEEBB)
-- Autor: Níckolas Tavares / Projeto TCC UNIPAM
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. CURSOS
-- -----------------------------------------------------------------------------
INSERT INTO tb_course (id, code, name, description, active)
VALUES 
    (1, 'MEDICINA', 'Medicina', 'Graduação em Medicina com ênfase em Internato, Raciocínio Clínico e Preparatório para Residência Médica.', TRUE),
    (2, 'SIS_INFO', 'Sistemas de Informação', 'Graduação em Sistemas de Informação com foco em Engenharia de Software, Arquitetura Enterprise e Banco de Dados.', TRUE)
ON CONFLICT (code) DO UPDATE 
SET name = EXCLUDED.name, description = EXCLUDED.description, active = EXCLUDED.active;

-- Ajusta a sequence do curso se necessário
SELECT setval('tb_course_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tb_course));

-- -----------------------------------------------------------------------------
-- 2. DISCIPLINAS / MATÉRIAS
-- -----------------------------------------------------------------------------

-- Disciplinas de Medicina (course_id = 1)
INSERT INTO tb_subject (id, course_id, code, name, description, active)
VALUES
    (1, 1, 'CLIN_MED', 'Clínica Médica', 'Cardiologia, Endocrinologia, Nefrologia, Pneumologia e Raciocínio Diagnóstico.', TRUE),
    (2, 1, 'CIR_GERAL', 'Cirurgia Geral', 'Abdome Agudo, Trauma (ATLS), Hérnias, Cicatrização e Pré/Pós-operatório.', TRUE),
    (3, 1, 'PEDIATRIA', 'Pediatria', 'Puericultura, Crescimento, Vacinação (PNI), Desidratação e Afecções Agudas da Infância.', TRUE),
    (4, 1, 'GINEC_OBST', 'Ginecologia e Obstetrícia', 'Pré-natal, Síndromes Hipertensivas da Gestação, Hemorragias e Rastreamento Oncológico Feminino.', TRUE),
    (5, 1, 'MED_FAM_COM', 'Medicina de Família e Comunidade', 'Atenção Primária, Princípios do SUS, Abordagem Familiar e Rastreamento Populacional.', TRUE)
ON CONFLICT (course_id, code) DO UPDATE 
SET name = EXCLUDED.name, description = EXCLUDED.description, active = EXCLUDED.active;

-- Disciplinas de Sistemas de Informação (course_id = 2)
INSERT INTO tb_subject (id, course_id, code, name, description, active)
VALUES
    (6, 2, 'ENG_SOFT', 'Engenharia de Software', 'Clean Architecture, Princípios SOLID, Padrões GoF, TDD e Metodologias Ágeis.', TRUE),
    (7, 2, 'BD_PERSIST', 'Bancos de Dados e Persistência', 'Modelagem Relacional, Normalização, Transações ACID, pgvector, Índices B-Tree e HNSW.', TRUE),
    (8, 2, 'ESTRUT_ALG', 'Estruturas de Dados e Algoritmos', 'Complexidade Assintótica (Big-O), Árvores Binárias, Grafos e Algoritmos de Ordenação/Busca.', TRUE),
    (9, 2, 'SEG_INFO', 'Segurança da Informação', 'Vulnerabilidades Web OWASP Top 10, Criptografia Simétrica/Assimétrica, JWT e OAuth2.', TRUE),
    (10, 2, 'REDES_DISTRIB', 'Redes e Sistemas Distribuídos', 'Pilha TCP/IP, Mensageria com RabbitMQ, Comunicação REST/gRPC e Arquitetura de Microsserviços.', TRUE)
ON CONFLICT (course_id, code) DO UPDATE 
SET name = EXCLUDED.name, description = EXCLUDED.description, active = EXCLUDED.active;

-- Ajusta sequence de matérias
SELECT setval('tb_subject_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tb_subject));

-- -----------------------------------------------------------------------------
-- 3. QUESTÕES E FLASHCARDS — MEDICINA
-- -----------------------------------------------------------------------------

-- --- 1. Clínica Médica (subject_id = 1) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    1,
    'Cardiologia - Insuficiência Cardíaca',
    'MULTIPLE_CHOICE',
    'Homem de 62 anos, com diagnóstico de Insuficiência Cardíaca com Fração de Ejeção Reduzida (ICFEr, FEVE = 32%), em classe funcional NYHA II. Qual combinação farmacológica constitui a terapia quádrupla padrão-ouro para redução de morbimortalidade?',
    'A',
    '["A) Inibidor de SGLT2 + Betabloqueador + Antagonista do Receptor Mineralocorticoide + IECA/BRA/INRA", "B) Digoxina + Furosemida + Hidralazina + Varfarina", "C) Amiodarona + Espironolactona + Losartana + AAS", "D) Bloqueador de Canal de Cálcio + Sinvastatina + Clopidogrel + Captopril"]'::jsonb,
    'Segundo a Diretriz Brasileira de Insuficiência Cardíaca (SBC) e diretrizes internacionais (ESC/AHA), o tratamento padrão da ICFEr é baseado no quarteto fantástico: 1) IECA/BRA ou Sacubitril/Valsartana (INRA), 2) Betabloqueador (carvedilol, succinato de metoprolol ou bisoprolol), 3) Antagonista do receptor mineralocorticoide (Espironolactona) e 4) Inibidor de SGLT2 (Dapagliflozina ou Empagliflozina).',
    'HARD',
    'Diretriz SBC / ENARE',
    TRUE
),
(
    1,
    'Endocrinologia - Diabetes Mellitus',
    'MULTIPLE_CHOICE',
    'Mulher de 54 anos realiza exames de rotina assintomática: Glicemia de jejum = 132 mg/dL e HbA1c = 6.8%. Na repetição confirmatória, glicemia = 129 mg/dL e HbA1c = 6.7%. Qual é o diagnóstico correto?',
    'B',
    '["A) Pré-diabetes (Tolerância diminuída à glicose)", "B) Diabetes Mellitus tipo 2", "C) Síndrome Metabólica sem critério para Diabetes", "D) Diabetes Mellitus tipo 1 de início tardio"]'::jsonb,
    'O diagnóstico de Diabetes Mellitus é estabelecido quando a Glicemia de Jejum é >= 126 mg/dL e/ou HbA1c >= 6.5% em duas amostras distintas na ausência de sintomas clássicos de hiperglicemia.',
    'MEDIUM',
    'SBD 2024 / Revalida INEP',
    TRUE
),
(
    1,
    'Nefrologia - Injúria Renal Aguda',
    'FLASHCARD',
    'Qual é a definição de Injúria Renal Aguda (IRA) estágio 1 pelos critérios KDIGO com base no aumento da creatinina sérica?',
    'Aumento da creatinina sérica >= 0.3 mg/dL em 48h ou aumento >= 1.5 a 1.9 vezes o valor basal em 7 dias.',
    NULL,
    'Os critérios KDIGO para IRA estágio 1 consideram elevação aguda de creatinina sérica em >= 0.3 mg/dL em até 48 horas ou elevação de 1.5 a 1.9x em relação ao valor basal nos últimos 7 dias, ou débito urinário < 0.5 mL/kg/h por 6 a 12 horas.',
    'MEDIUM',
    'KDIGO Clinical Practice Guideline',
    TRUE
),
(
    1,
    'Pneumologia - Asma e DPOC',
    'MULTIPLE_CHOICE',
    'Na espirometria de um paciente tabagista com suspeita de DPOC, qual achado pós-broncodilatador confirma a presença de obstrução fixa ao fluxo aéreo?',
    'C',
    '["A) CVF < 80% do previsto", "B) VEF1 > 80% com melhora de 200 mL pós-BD", "C) Relação VEF1/CVF < 0.70 pós-broncodilatador", "D) Aumento isolado do volume residual sem alteração da relação"]'::jsonb,
    'Pelo consenso GOLD (Global Initiative for Chronic Obstructive Lung Disease), o critério espirométrico mandatório para confirmação diagnóstica de DPOC é a relação VEF1/CVF < 0.70 (ou abaixo do limite inferior da normalidade) medida após o uso de broncodilatador.',
    'MEDIUM',
    'GOLD 2024',
    TRUE
);

-- --- 2. Cirurgia Geral (subject_id = 2) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    2,
    'Abdome Agudo - Apendicite Aguda',
    'MULTIPLE_CHOICE',
    'Paciente jovem do sexo masculino apresenta dor periumbilicar que migrou para a fossa ilíaca direita após 12 horas, acompanhada de anorexia, náuseas e febre baixa (37.9°C). Ao exame físico, apresenta descompressão brusca dolorosa no ponto de McBurney. Qual é o nome deste sinal semiológico característico?',
    'A',
    '["A) Sinal de Blumberg", "B) Sinal de Murphy", "C) Sinal de Rovsing", "D) Sinal de Cullen"]'::jsonb,
    'O Sinal de Blumberg consiste na dor à descompressão brusca no ponto de McBurney (fossa ilíaca direita), indicando irritação peritoneal típica de apendicite aguda. O sinal de Rovsing é dor na FID à palpação da FIE; Murphy é parada inspiratória na palpação do ponto cístico (colecistite); Cullen é equimose periumbilical (pancreatite/hemoperitônio).',
    'EASY',
    'Sabiston / Residência Médica',
    TRUE
),
(
    2,
    'Trauma - ATLS 10ª Edição',
    'MULTIPLE_CHOICE',
    'Vítima de colisão automobilística em alta velocidade chega ao pronto-socorro com hipotensão (PA: 70x40 mmHg), turgência jugular patológica, desvio da traqueia para a direita e murmúrio vesicular abolido no hemitórax esquerdo. Qual a conduta imediata preconizada pelo ATLS?',
    'B',
    '["A) Solicitar tomografia computadorizada de tórax em caráter de urgência", "B) Descompressão torácica imediata com agulha (toracocentese) no 4º/5º espaço intercostal na linha axilar anterior", "C) Intubação orotraqueal imediata com ventilação por pressão positiva", "D) Infusão de 2000 mL de soro fisiológico aquecido antes de qualquer procedimento invasivo"]'::jsonb,
    'O quadro é de Pneumotórax Hipertensivo à esquerda (emergência com risco iminente de morte por colapso cardiovascular). O tratamento deve ser clínico e imediato, sem aguardar exames de imagem, realizando descompressão com agulha no 4º ou 5º espaço intercostal entre a linha axilar média e anterior (conforme atualização ATLS 10ª ed) seguida de drenagem tubular em selo d água.',
    'HARD',
    'ATLS 10ª Edição',
    TRUE
);

-- --- 3. Pediatria (subject_id = 3) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    3,
    'Imunização - Calendário PNI',
    'MULTIPLE_CHOICE',
    'Segundo o Calendário Nacional de Vacinação do PNI (Ministério da Saúde do Brasil), quais vacinas são administradas na rotina aos 2 meses de vida do lactente?',
    'A',
    '["A) Pentavalente + VIP + Pneumocócica 10-valente + Rotavírus Humano", "B) BCG + Hepatite B + Tríplice Viral + Febre Amarela", "C) DTP + VOP + Meningocócica C + Varicela", "D) Hexavalente + Influenza + HPV + Hepatite A"]'::jsonb,
    'Aos 2 meses de vida, o bebê recebe: 1ª dose da Pentavalente (DTP + Hib + Hep B), 1ª dose da VIP (Vacina Inativada Poliomielite), 1ª dose da Pneumocócica 10V e 1ª dose da Vacina Oral de Rotavírus Humano (VRH).',
    'MEDIUM',
    'PNI / Ministério da Saúde 2024',
    TRUE
),
(
    3,
    'Gastroenterologia Pediátrica - Desidratação',
    'FLASHCARD',
    'De acordo com o Ministério da Saúde, qual é a conduta do Plano B para tratamento da desidratação em crianças?',
    'Terapia de Reidratação Oral (TRO) na unidade de saúde administrando 50 a 100 mL/kg de Solução de Reidratação Oral (SRO) em um período de 4 a 6 horas sob supervisão.',
    NULL,
    'O Plano B é indicado para desidratação moderada/sem choque. É realizado no próprio serviço de saúde com administração frequente de SRO em pequenos volumes até que os sinais de desidratação desapareçam.',
    'MEDIUM',
    'Manual de Diarreia e Desidratação MS/SBP',
    TRUE
);

-- --- 4. Ginecologia e Obstetrícia (subject_id = 4) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    4,
    'Obstetrícia - Síndromes Hipertensivas',
    'MULTIPLE_CHOICE',
    'Gestante de 34 semanas dá entrada na maternidade com PA = 165x110 mmHg, cefaleia intensa refratária, turvação visual (escotomas) e dor em hipocôndrio direito. Qual é a conduta farmacológica prioritária para a prevenção de crises convulsivas eclâmpticas?',
    'C',
    '["A) Diazepam intravenoso", "B) Fenitoína intravenosa", "C) Sulfato de Magnésio (Esquema de Pritchard ou Zuspan)", "D) Nitroprussiato de Sódio"]'::jsonb,
    'O Sulfato de Magnésio é a droga de escolha para profilaxia e tratamento das convulsões na pré-eclâmpsia grave e eclâmpsia. Atua no bloqueio neuromuscular e vasodilatação cerebral. Diazepam e Fenitoína são contraindicados para essa finalidade.',
    'HARD',
    'FEBRASGO / ENARE',
    TRUE
);

-- --- 5. Medicina de Família e Comunidade (subject_id = 5) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    5,
    'Atenção Primária - Princípios do SUS',
    'MULTIPLE_CHOICE',
    'Garantir que pessoas com maiores necessidades em saúde recebam maior atenção e recursos prioritários para reduzir disparidades e desigualdades sociais reflete diretamente qual princípio doutrinário do SUS?',
    'B',
    '["A) Universalidade", "B) Equidade", "C) Integralidade", "D) Descentralização"]'::jsonb,
    'A Equidade é o princípio de tratar desigualmente os desiguais, investindo mais onde a carência e vulnerabilidade são maiores. A Universalidade garante acesso a todos, e a Integralidade enxerga o indivíduo como um todo em todos os níveis de atenção.',
    'EASY',
    'Lei 8.080/1990 / SBMFC',
    TRUE
);

-- -----------------------------------------------------------------------------
-- 4. QUESTÕES E FLASHCARDS — SISTEMAS DE INFORMAÇÃO
-- -----------------------------------------------------------------------------

-- --- 6. Engenharia de Software (subject_id = 6) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    6,
    'Clean Architecture & SOLID',
    'MULTIPLE_CHOICE',
    'No princípio da Inversão de Dependência (Dependency Inversion Principle - DIP do SOLID), módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender de:',
    'A',
    '["A) Abstrações (Interfaces ou classes abstratas)", "B) Classes Concretas e Singleton", "C) Frameworks ORM externos", "D) Bancos de dados relacionais"]'::jsonb,
    'O DIP preceitua que módulos de alto nível (regras de negócio) não devem depender de detalhes de implementação (baixo nível, banco, UI), mas sim de abstrações/contratos estáveis.',
    'EASY',
    'Robert C. Martin (Clean Architecture)',
    TRUE
),
(
    6,
    'Design Patterns GoF',
    'MULTIPLE_CHOICE',
    'Qual padrão de projeto criacional garante que uma classe tenha apenas uma única instância em tempo de execução e fornece um ponto de acesso global para ela?',
    'B',
    '["A) Factory Method", "B) Singleton", "C) Observer", "D) Strategy"]'::jsonb,
    'O Singleton restringe a instanciação de uma classe a um único objeto compartilhado em toda a aplicação, comumente utilizado em gerenciadores de conexão ou contexto de aplicação.',
    'EASY',
    'GoF (Design Patterns)',
    TRUE
),
(
    6,
    'Test-Driven Development (TDD)',
    'FLASHCARD',
    'Quais são os 3 estágios fundamentais do ciclo Red-Green-Refactor no TDD?',
    '1) Red: Escrever um teste unitário que falha; 2) Green: Escrever o código mínimo de produção para fazê-lo passar; 3) Refactor: Melhorar o design e a estrutura do código mantendo todos os testes verdes.',
    NULL,
    'O ciclo TDD orienta o design do software a partir de especificações executáveis, garantindo alta cobertura de testes e prevenção contínua de regressões.',
    'MEDIUM',
    'Kent Beck (TDD by Example)',
    TRUE
);

-- --- 7. Bancos de Dados e Persistência (subject_id = 7) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    7,
    'Propriedades ACID',
    'MULTIPLE_CHOICE',
    'Em sistemas de gerenciamento de bancos de dados relacionais, qual propriedade ACID garante que uma transação concluída com commit persistirá suas alterações mesmo em caso de falha de energia ou reinicialização do servidor?',
    'D',
    '["A) Atomicidade", "B) Consistência", "C) Isolamento", "D) Durabilidade"]'::jsonb,
    'A Durabilidade assegura que os dados de transações efetivadas (committed) são gravados em armazenamento não volátil (WAL - Write-Ahead Logging) e não serão perdidos mesmo em quedas de energia.',
    'EASY',
    'Silberschatz / Elmasri',
    TRUE
),
(
    7,
    'Bancos Vetoriais & pgvector',
    'MULTIPLE_CHOICE',
    'Na indexação de embeddings de alta dimensionalidade para busca semântica em pgvector (como em sistemas RAG), qual tipo de índice aproximado baseado em grafos oferece excelente equilíbrio entre recall e velocidade de consulta?',
    'B',
    '["A) B-Tree clássico", "B) HNSW (Hierarchical Navigable Small World)", "C) Hash Index", "D) BRIN (Block Range Index)"]'::jsonb,
    'O índice HNSW constrói uma estrutura hierárquica em grafos multidimensionais (navegabilidade por grafos de mundo pequeno) permitindo buscas de K-Nearest Neighbors (KNN) de altíssima performance em pgvector.',
    'HARD',
    'PostgreSQL pgvector documentation',
    TRUE
);

-- --- 8. Estruturas de Dados e Algoritmos (subject_id = 8) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    8,
    'Complexidade Assintótica (Big-O)',
    'MULTIPLE_CHOICE',
    'Qual é a complexidade de tempo média e no pior caso do algoritmo de ordenação Merge Sort ao ordenar um array de N elementos?',
    'C',
    '["A) Médio: O(N), Pior caso: O(N^2)", "B) Médio: O(N log N), Pior caso: O(N^2)", "C) Médio: O(N log N), Pior caso: O(N log N)", "D) Médio: O(1), Pior caso: O(N)"]'::jsonb,
    'O Merge Sort é um algoritmo baseado na técnica de divisão e conquista que divide o array pela metade sucessivamente (log N níveis) e intercala em O(N) por nível, mantendo O(N log N) determinístico em todos os casos (melhor, médio e pior).',
    'MEDIUM',
    'Cormen et al. (Algoritmos: Teoria e Prática)',
    TRUE
);

-- --- 9. Segurança da Informação (subject_id = 9) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    9,
    'OWASP Top 10 - Vulnerabilidades Web',
    'MULTIPLE_CHOICE',
    'Uma aplicação concatena parâmetros fornecidos diretamente pelo usuário em consultas SQL brutas no backend sem sanitização nem uso de Prepared Statements / consultas parametrizadas. Qual vulnerabilidade crítica está presente?',
    'A',
    '["A) SQL Injection (Injeção de SQL)", "B) Cross-Site Scripting (XSS)", "C) Cross-Site Request Forgery (CSRF)", "D) Server-Side Request Forgery (SSRF)"]'::jsonb,
    'SQL Injection ocorre quando dados não confiáveis são concatenados como parte do comando SQL, permitindo ao atacante alterar a semântica da consulta para ler, modificar ou deletar dados do banco.',
    'EASY',
    'OWASP Top 10 (2021)',
    TRUE
),
(
    9,
    'Autenticação - JWT (JSON Web Token)',
    'FLASHCARD',
    'Quais são as 3 partes constituintes de um JSON Web Token (JWT) separadas por ponto (.)?',
    'Header (Cabeçalho), Payload (Carga útil/Claims) e Signature (Assinatura digital).',
    NULL,
    'O Header contém o algoritmo de assinatura e tipo de token; o Payload contém as declarações/claims da sessão do usuário; a Signature valida a integridade e autenticidade usando a chave secreta ou chave pública.',
    'MEDIUM',
    'RFC 7519',
    TRUE
);

-- --- 10. Redes e Sistemas Distribuídos (subject_id = 10) ---
INSERT INTO tb_flashcard (subject_id, topic, question_type, question, answer, options_json, explanation, difficulty, source, active)
VALUES
(
    10,
    'Mensageria Assíncrona & RabbitMQ',
    'MULTIPLE_CHOICE',
    'Em uma arquitetura orientada a eventos utilizando RabbitMQ, para onde são roteadas mensagens que falharam de forma irrecuperável após sucessivas tentativas de processamento por um consumidor?',
    'C',
    '["A) Direct Exchange Principal", "B) Fanout Exchange de Broadcast", "C) Dead Letter Queue (DLQ)", "D) Fila temporária na memória RAM descartável"]'::jsonb,
    'A Dead Letter Queue (DLQ) é o padrão de resiliência onde mensagens venenosas ou com erros permanentes são isoladas para não travar a fila principal e permitir inspeção ou reprocessamento posterior.',
    'MEDIUM',
    'Enterprise Integration Patterns / AMQP Spec',
    TRUE
);
