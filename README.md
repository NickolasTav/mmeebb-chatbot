<div align="center">

# 🧠 Chatbot de Repetição Espaçada MMEEBB
### *Automação da Memorização Exponencial na Base Binária para Educação Médica e Superior*

[![Java CI with Maven](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL & pgvector](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3%20Management-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-3.5%20Flash--Lite%20%2B%20Failover-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35.0-1C3C3C?style=for-the-badge)](https://github.com/langchain4j/langchain4j)
[![Tests](https://img.shields.io/badge/Tests-100%25%20Passing-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)

---

**Trabalho de Conclusão de Curso (TCC)**  
**Instituição:** Centro Universitário de Patos de Minas ([UNIPAM](https://unipam.edu.br))  
**Curso:** Bacharelado em Sistemas de Informação  
**Autor:** Níckolas Tavares do Nascimento  
**Orientadora:** Profa. Dra. Mislene Dalila da Silva  

</div>

---

## 📌 Sumário
- [1. Visão Geral e Problema](#1-visão-geral-e-problema)
- [2. Fundamentação Teórica](#2-fundamentação-teórica)
- [3. O Algoritmo MMEEBB ($2^n$)](#3-o-algoritmo-mmeebb-2n)
- [3.1. Personalização Adaptativa por Desempenho](#31-personalização-adaptativa-por-desempenho-mmeebb-adaptativo)
- [4. Arquitetura da Solução](#4-arquitetura-da-solução)
- [5. Modelagem de Dados & DER](#5-modelagem-de-dados--der)
- [6. Stack Tecnológica](#6-stack-tecnológica)
- [7. Como Executar o Projeto](#7-como-executar-o-projeto)
- [8. Variáveis de Ambiente](#8-variáveis-de-ambiente)
- [9. Integração com WhatsApp (Uazapi & Ngrok)](#9-integração-com-whatsapp-uazapi--ngrok)
- [10. Suíte de Testes (TDD)](#10-suíte-de-testes-tdd)
- [11. Estrutura do Código](#11-estrutura-do-código)
- [12. Governança Git & Commits Semânticos](#12-governança-git--commits-semânticos)

---

## 1. Visão Geral e Problema

Estudantes de graduações densas — em especial os cursos de **Medicina** durante as fases de **Internato e Residência Médica** — enfrentam jornadas exaustivas que ultrapassam 40 a 60 horas semanais. Essa rotina gera dois desafios críticos:
1. **Sobrecarga Cognitiva e Fadiga:** Dificuldade extrema em encontrar tempo dedicado para abrir aplicativos tradicionais de estudo (como Anki ou flashcards manuais).
2. **Declínio Rápido da Retenção:** Conhecimentos clínicos e diagnósticos complexos são rapidamente esquecidos se não forem reforçados ativamente em intervalos matematicamente dosados.

Este projeto propõe e implementa um **Chatbot Inteligente no WhatsApp** que automatiza o agendamento de revisões ativas com o método **MMEEBB**, atua como um **Preceptor/Tutor Médico Virtual via IA Generativa (Google Gemini + RAG)** e elimina toda a fricção de uso ao entregar os questionamentos diretamente no canal de comunicação mais acessado pelo estudante.

O sistema vai além do cálculo fixo de intervalo: ele **mede em quais conteúdos cada aluno erra mais e ajusta as revisões a isso**, encurtando o ciclo dos tópicos frágeis, priorizando-os na fila do dia e antecipando questões relacionadas após um erro — sem abandonar a base binária do método (ver [seção 3.1](#31-personalização-adaptativa-por-desempenho-mmeebb-adaptativo)).

---

## 2. Fundamentação Teórica

O projeto fundamenta-se na convergência de três pilares científicos e comportamentais:

```mermaid
graph LR
    A["Nelson Cowan (2001)<br/>Limite de Memória de Trabalho<br/>~4 chunks simultâneos"] --> D["Chatbot MMEEBB<br/>Interação em doses pílulas no WhatsApp"]
    B["Hermann Ebbinghaus (1885)<br/>Curva do Esquecimento<br/>Perda exponencial sem reforço"] --> D
    C["BJ Fogg (2009)<br/>Modelo Comportamental<br/>B = MAT: Gatilho Ativo sem Atrito"] --> D
```

1. **A Capacidade Mágica de Cowan (2001):** Demonstra que a atenção humana imediata comporta apenas $\approx 4$ blocos (*chunks*) de informação. O chatbot envia pílulas clínicas diárias que respeitam essa barreira cognitiva.
2. **A Curva do Esquecimento de Ebbinghaus (1885):** Mostra que a retenção cai drasticamente nas primeiras horas após o aprendizado. Revisões espaçadas ativas achatam a curva e consolidam as memórias de longo prazo.
3. **O Modelo Comportamental de BJ Fogg (2009) ($B = M \times A \times T$):** Um comportamento ($B$) só ocorre quando há Motivação ($M$), Habilidade/Facilidade ($A$) e Gatilho (*Trigger* - $T$). O chatbot atua como o **Gatilho Ativo (Push Notification)** dentro do WhatsApp, reduzindo o esforço do aluno a quase zero.

---

## 3. O Algoritmo MMEEBB ($2^n$)

O sistema utiliza o **Método de Memorização Exponencial Efetivo na Base Binária** (Ferreira et al., 2014):

### Fórmula do Intervalo de Reforço de Aprendizado (IRA):
$$\text{IRA} = 2^n \text{ dias}, \quad \text{onde } n \in [0, 13]$$

```mermaid
stateDiagram-v2
    [*] --> N0_Dia1: Novo Card Agendado (n=0, IRA=1d)
    
    N0_Dia1 --> N1_Dia2: Acerto (n=1, IRA=2d)
    N0_Dia1 --> N0_Dia1: Erro (n=0, IRA=1d)
    
    N1_Dia2 --> N2_Dia4: Acerto (n=2, IRA=4d)
    N1_Dia2 --> N0_Dia1: Erro (n=0, IRA=1d)
    
    N2_Dia4 --> N3_Dia8: Acerto (n=3, IRA=8d)
    N2_Dia4 --> N0_Dia1: Erro (n=0, IRA=1d)
    
    N3_Dia8 --> N_Exp: Acertos Consecutivos (2^n)
    N_Exp --> N0_Dia1: Erro (n=0, IRA=1d)
    N_Exp --> N13_Teto: Teto Máximo n=13 (IRA=8192d)
```

- **Início ($n = 0$):** O primeiro reforço ocorre $2^0 = 1$ dia após o contato com o conteúdo.
- **Acerto (Feedback Positivo):** O índice $n$ avança uma posição ($n \leftarrow \min(n+1, 13)$), dobrando o intervalo ($1 \to 2 \to 4 \to 8 \to 16 \dots$ dias).
- **Erro (Feedback Negativo):** O índice é resetado ($n \leftarrow 0$), retornando imediatamente para a fila de revisão do dia seguinte ($2^0 = 1$ dia).

---

## 3.1. Personalização Adaptativa por Desempenho (MMEEBB Adaptativo)

O MMEEBB clássico aplica exatamente a mesma progressão a todo aluno e a todo conteúdo. Na prática, um interno que erra sistematicamente *antibioticoterapia* e acerta sempre *semiologia cardíaca* recebia o mesmo tratamento nos dois assuntos. A camada adaptativa corrige isso **sem abandonar a base binária**: o intervalo continua sendo exatamente $2^n$ — o que muda é **até onde $n$ pode crescer** e **o tamanho do recuo no erro**.

### Como o sistema descobre onde o aluno erra

Cada resposta corrigida vira um evento em `tb_review_attempt` (com o $n$ antes, o $n$ depois e o domínio vigente). A partir desse histórico, o sistema calcula a **taxa de erro por tópico** dentro de uma janela móvel (padrão: 90 dias) e classifica o domínio:

| Taxa de erro no tópico | Domínio | $N_{max}$ | Intervalo máximo |
| :--- | :--- | :---: | :--- |
| amostra < 3 respostas | `SEM_DADOS` | 13 | 8192 dias *(MMEEBB clássico)* |
| $\ge 50\%$ | `FRAGIL` | 3 | **8 dias** |
| $20\%$ a $50\%$ | `EM_CONSOLIDACAO` | 6 | **64 dias** |
| $< 20\%$ | `DOMINADO` | 13 | 8192 dias *(MMEEBB clássico)* |

A unidade de análise é o **tópico**, não o cartão: "o conteúdo em que o aluno erra mais" é um assunto, e uma questão nova de um tópico frágil já nasce tratada como frágil. O desempenho de um cartão isolado já está representado pelo próprio $n$.

### A fórmula adaptativa

$$n' = \min\big(\max(\text{candidato},\ 0),\ \min(13,\ N_{max})\big), \qquad \text{IRA} = 2^{n'}$$

$$\text{candidato} = \begin{cases} n + 1 & \text{acerto} \\ n - 2 & \text{erro em tópico } \texttt{DOMINADO} \\ 0 & \text{erro nos demais casos} \end{cases}$$

> **Degradação graciosa:** com `SEM_DADOS` o teto é o próprio 13 e a fórmula colapsa em $n+1$ / $0$ — o MMEEBB original, bit a bit. Um teste percorre $n = 0 \dots 13$, em acerto e em erro, provando essa equivalência. Sem evidência de desempenho, **não há personalização alguma**.

```mermaid
flowchart LR
    R[Resposta corrigida] --> H[(tb_review_attempt)]
    H --> C{Taxa de erro<br/>no tópico}
    C -->|>= 50%| F[FRAGIL<br/>teto 8 dias]
    C -->|20% a 50%| E[EM_CONSOLIDACAO<br/>teto 64 dias]
    C -->|< 20%| D[DOMINADO<br/>lapso recua 2 casas]
    C -->|amostra < 3| S[SEM_DADOS<br/>MMEEBB clássico]
    F --> P[Fila do dia priorizada<br/>pelos tópicos frágeis]
    F --> RF[Reforço dirigido:<br/>antecipa questões do tópico]
```

### As quatro decisões adaptativas

1. **Teto por domínio** — enquanto o tópico for frágil, o cartão não escapa do ciclo curto, por mais que o aluno acerte. Ele volta a subir sozinho quando a taxa de erro do tópico cair.
2. **Lapso graduado** — errar um cartão de tópico dominado recua duas casas ($64 \to 16$ dias) em vez de zerar: um deslize isolado não apaga meses de consolidação. O reset total continua valendo em todos os outros casos.
3. **Fila priorizada** — entre as revisões vencidas do dia, os tópicos mais frágeis vêm primeiro. Importa porque o interno cansado costuma responder só as primeiras questões da sessão (limite de ~4 *chunks* de Cowan).
4. **Reforço dirigido** — ao errar em tópico frágil, o sistema antecipa até 2 questões **do mesmo tópico** para o dia. O limite se auto-regula contando quantas já estão vencidas, então errar várias vezes seguidas não gera uma fila infinita.

### Onde a IA entra (e onde não entra)

| Etapa | Responsável | Por quê |
| :--- | :--- | :--- |
| Corrigir a resposta dissertativa | 🤖 Gemini | É o sinal que alimenta todo o resto |
| Classificar o domínio do tópico | ⚙️ Determinístico | Precisa ser reprodutível e auditável diante da banca |
| Decidir o intervalo | ⚙️ Determinístico | É o algoritmo do TCC; precisa ser verificável |
| Explicar o desempenho ao aluno | 🤖 Gemini | Linguagem natural é onde o modelo agrega |

Toda decisão que diverge do MMEEBB clássico é **explicada na própria mensagem** do WhatsApp — um intervalo que encurta depois de um acerto, sem justificativa, seria lido pelo aluno como defeito do sistema.

> A camada inteira pode ser desligada com `MMEEBB_ADAPTIVE_ENABLED=false`, o que devolve o sistema ao MMEEBB clássico. Além de servir de contingência, isso habilita a comparação experimental *clássico × adaptativo* na monografia.

---

## 4. Arquitetura da Solução

O sistema foi concebido sob o padrão **Direct-to-Queue Messaging** e **Arquitetura em Camadas Enterprise**, garantindo isolamento assíncrono, proteção contra sobrecargas e tolerância a falhas.

```mermaid
flowchart TB
    subgraph WhatsApp_Gateway [WhatsApp & Webhook]
        ALUNO[Estudante / Interno] <-->|Mensagens| UAZ[Gateway Uazapi]
        UAZ -->|POST /webhook/uazapi| NGROK[Túnel Ngrok :8080]
    end

    subgraph Spring_Backend [Backend Spring Boot 3]
        NGROK -->|Recepção Rápida 200 OK| CTRL[UazapiWebhookController]
        CTRL -->|Direct to Queue| Q_IN[whatsapp.incoming.queue]
        
        Q_IN -->|Consumo Assíncrono| CONS[WhatsappMessageConsumer]
        CONS --> ORCH[ChatFlowOrchestrator]
        
        ORCH -->|1. Fast Path Menus & Comandos| FSM[Máquina de Estados Finita]
        ORCH -->|2. Avaliação de Respostas| ADAPT[AdaptiveReviewService]
        ADAPT --> ANALYSIS[PerformanceAnalysisService<br/>domínio por tópico]
        ADAPT --> MMEEBB[MmeebbService 2^n<br/>com teto adaptativo]
        ORCH -->|3. Dúvidas Clínicas & RAG| RAG[SubjectRagService + Gemini]
        ORCH -->|4. Relatório de Desempenho| PERF[StudentPerformanceFlowHandler]
        
        SCHED[DailyReviewNotificationScheduler] -->|Rodada 5min: horário de cada aluno| Q_OUT[whatsapp.outgoing.queue]
        Q_OUT -->|Rate Limit + Anti-Ban + Composing| OUT_CONS[WhatsappOutgoingConsumer]
        
        OUT_CONS -->|POST /message/sendText| REST_CLI[UazapiClientService]
        CONS -->|POST /message/sendText| REST_CLI
        REST_CLI --> UAZ
    end

    subgraph Persistence_Layer [Camada de Persistência & IA]
        PG[(PostgreSQL 16 Relacional)]
        VEC[(pgvector Embeddings 768d)]
        FLY[Flyway Migrations]
        GEMINI[Google Gemini 3.5 Flash-Lite<br/>+ Failover 3.5 Flash]
    end

    ORCH <--> PG
    RAG <--> VEC
    RAG <--> GEMINI
```

### 4.1. Máquina de Estados Finita (FSM) & Comandos Conversacionais

O fluxo conversacional é gerenciado por uma **FSM determinística** (`ChatFlowOrchestrator`) cujo estado vive no **Redis**, chaveado pelo número de WhatsApp e com TTL de expiração automática. O PostgreSQL guarda apenas o que é durável (estudante, matrícula e agendamentos), eliminando a escrita relacional a cada troca de estado.

#### Primeiro contato: formulário de cadastro

O número de WhatsApp é a chave única do estudante. Quando um número desconhecido envia a primeira mensagem, o bot aplica um formulário de três etapas antes de liberar o menu:

| Etapa | Estado da FSM | Campo coletado |
| :--- | :--- | :--- |
| 1 de 3 | `AWAITING_FULL_NAME` | Nome completo (exige nome e sobrenome) |
| 2 de 3 | `AWAITING_RA` | RA — aceita `pular`, e recusa RA já vinculado a outro número |
| 3 de 3 | `AWAITING_COURSE` → `AWAITING_ACADEMIC_PERIOD` | Curso e período letivo |

Ao concluir, o sistema grava `tb_student` + `tb_student_course` e **inicializa os agendamentos MMEEBB** de todos os flashcards ativos do curso, deixando a primeira rodada disponível imediatamente. Números já cadastrados nunca repetem o formulário, mesmo após a sessão expirar no Redis.

#### Após o cadastro: menu fixo + texto livre

O estudante pode usar os números do menu ou **escrever livremente**. Texto livre passa por um classificador de intenção (Gemini) que também extrai a disciplina citada e restringe a busca vetorial a ela:

| Intenção | Gatilhos | Comportamento |
| :--- | :--- | :--- |
| **📚 START_REVIEW** | `1`, ou texto livre como *"quero estudar um pouco"* | Inicia o ciclo de flashcards pendentes do dia ($2^n$). |
| **💡 ASK_DOUBT** | `2`, ou qualquer pergunta de conteúdo | Responde via RAG. Se a mensagem citar uma disciplina (*"dúvida de cardiologia"*), a busca é particionada por `subject_id`. |
| **⚙️ OPEN_SETTINGS** | `3`, `configurações`, `config`, `ajustes`, ou *"quero mudar meu horário"* | Abre o menu de Configurações (nome de tratamento, horário do lembrete, pausar lembretes, curso e período). |
| **📊 SHOW_PERFORMANCE** | `4`, `desempenho`, `progresso`, `estatísticas`, `como estou`, ou *"será que estou melhorando?"* | Envia o relatório de desempenho: taxa de acerto, tópicos em que mais erra (com o teto de intervalo ativo em cada um), conteúdos consolidados e a leitura do preceptor virtual. |
| **📋 SHOW_MENU** | `menu`, `oi`, `bom dia`, `ajuda` | Reexibe o menu principal. |
| **🚪 EXIT** | `sair`, `tchau`, `encerrar`, `flw`, `/sair` | Finaliza a sessão com despedida e limpa o card ativo. |

> Durante o formulário de cadastro os comandos globais ficam desativados: `menu` ali é uma resposta possível do estudante, não um comando de reset.

#### Correção das respostas

Múltipla escolha é resolvida por comparação direta e também aceita a letra da alternativa colada por extenso pelo aluno (`"A) Inibidor de SGLT2..."`); respostas dissertativas passam por **correção semântica via Gemini**, que aceita o conceito correto expresso com palavras próprias e devolve um comentário pedagógico curto.

O resultado dessa correção não é descartado: ele vira um evento em `tb_review_attempt` e alimenta a [personalização adaptativa](#31-personalização-adaptativa-por-desempenho-mmeebb-adaptativo). Pedidos de ajuda (*"não entendi, pode explicar?"*) são detectados como dúvida, respondidos pelo RAG e **não contam como erro** — contabilizá-los puniria justamente quem pede explicação.

### 4.2. Resiliência do Gemini (Retry & Failover Automático)

O LangChain4j 0.35.0 usa por padrão um timeout rígido de 10s no `OkHttpClient` do cliente Gemini, insuficiente para respostas de RAG mais elaboradas. O `GeminiServiceCustomizer` (`dev.langchain4j.model.googleai`) substitui o `OkHttpClient` interno via reflection e adiciona:

- **Timeouts estendidos:** `connect`/`read`/`write`/`call` de 60s (`DEFAULT_TIMEOUT` em `LangChain4jConfig`), eliminando `SocketTimeoutException` em respostas mais longas do Tutor Clínico.
- **Interceptor de alta demanda (`HighDemandRetryInterceptor`):** intercepta HTTP `503` (High Demand) e `429` (Rate Limit) do Google, retentando até 3 vezes com backoff progressivo (800ms, 1600ms).
- **Failover dinâmico de modelo:** na penúltima tentativa, reescreve a URL da chamada Retrofit trocando `gemini-3.5-flash-lite` ↔ `gemini-3.5-flash` (clusters/pools separados no Google), aumentando a chance de sucesso sem intervenção manual.

---

## 5. Modelagem de Dados & DER

O banco de dados adota modelagem dinâmica multi-curso e multi-disciplina, particionando embeddings vetoriais para evitar cruzamento indevido de conteúdos no módulo RAG:

```mermaid
erDiagram
    COURSE ||--o{ SUBJECT : contem
    COURSE ||--o{ STUDENT_COURSE : matricula
    STUDENT ||--o{ STUDENT_COURSE : participa
    SUBJECT ||--o{ FLASHCARD : categoriza
    STUDENT ||--o{ REPETITION_SCHEDULE : revisa
    FLASHCARD ||--o{ REPETITION_SCHEDULE : agendado_em
    STUDENT ||--o{ REVIEW_ATTEMPT : responde
    FLASHCARD ||--o{ REVIEW_ATTEMPT : registrado_em
    STUDENT ||--o{ CHAT_SESSION : mantem
    COURSE ||--o{ KNOWLEDGE_EMBEDDING : escopo
    SUBJECT ||--o{ KNOWLEDGE_EMBEDDING : escopo

    COURSE {
        bigint id PK
        string code UK "MEDICINA, SIS_INFO, DIREITO"
        string name
        boolean active
    }
    SUBJECT {
        bigint id PK
        bigint course_id FK
        string code "CARDIO, PEDIATRIA, GINECO"
        string name
        boolean active
    }
    STUDENT {
        uuid id PK
        string phone_number UK "5534999998888"
        string full_name
        string ra UK "RA Institucional"
        string preferred_name "Apelido de tratamento"
        time preferred_study_time "Horario do lembrete diario"
        boolean review_notifications_enabled
        date last_review_notification_on "Dia ja avaliado pelo scheduler"
        boolean active
    }
    STUDENT_COURSE {
        bigint id PK
        uuid student_id FK
        bigint course_id FK
        int academic_period
    }
    FLASHCARD {
        bigint id PK
        bigint subject_id FK
        string topic
        string question_type "FLASHCARD | MULTIPLE_CHOICE"
        text question
        text answer
        jsonb options_json
        text explanation
        string difficulty "EASY | MEDIUM | HARD"
    }
    REPETITION_SCHEDULE {
        bigint id PK
        uuid student_id FK
        bigint flashcard_id FK
        int n_index "Índice n (0 a 13)"
        int interval_days "2^n dias"
        int repetition_count
        int consecutive_correct
        date next_review_date
        string status "PENDING | COMPLETED"
        bigint version "Lock Otimista"
    }
    REVIEW_ATTEMPT {
        bigint id PK
        uuid student_id FK
        bigint flashcard_id FK
        boolean correct
        int n_index_before "N antes da decisao"
        int n_index_after "N depois da decisao"
        int interval_days_after "2^n aplicado"
        string topic_mastery "SEM_DADOS | FRAGIL | EM_CONSOLIDACAO | DOMINADO"
        timestamp answered_at
    }
    CHAT_SESSION {
        uuid id PK
        uuid student_id FK
        string phone_number UK
        string current_state "NEW | MAIN_MENU | REVIEW_MODE | RAG_DOUBT_MODE"
        bigint selected_course_id FK
        bigint selected_subject_id FK
        bigint current_flashcard_id FK
        jsonb context_data
        timestamp last_interaction_at
    }
    KNOWLEDGE_EMBEDDING {
        uuid id PK
        bigint course_id FK
        bigint subject_id FK
        text content
        vector embedding "768 dimensões"
        jsonb metadata "Filtros course_id e subject_id"
    }
```

---

## 6. Stack Tecnológica

| Componente | Tecnologia | Finalidade |
| :--- | :--- | :--- |
| **Backend** | Java 17 + Spring Boot 3.3.x | Core da aplicação e regras de negócio |
| **Banco Relacional** | PostgreSQL 16 | Persistência transacional das entidades e agendamentos |
| **Banco Vetorial** | `pgvector` (PostgreSQL extension) | Armazenamento de embeddings semânticos para o RAG |
| **Migrations** | Flyway | Versionamento e automação do schema SQL |
| **Mensageria** | RabbitMQ 3 Management | Desacoplamento assíncrono, buffers e proteção anti-ban |
| **Estado Conversacional** | Redis 7 | FSM do chatbot com TTL, sem escrita relacional por mensagem |
| **IA / LLM & RAG** | Google Gemini 3.5 Flash-Lite (fallback 3.5 Flash) + LangChain4j 0.35.0 | Preceptor clínico, roteamento de intenção e RAG, com interceptor de retry/failover automático em 503/429 |
| **Parser Universal** | Apache Tika | Extração de texto de PDFs, DOCX e Markdown para ingestão |
| **Gateway WhatsApp** | Uazapi / UazapiGO | Conexão com o WhatsApp, webhooks e envio de mensagens |
| **Túnel Local** | Ngrok | Exposição segura da porta `8080` para recepção de eventos |
| **Testes** | JUnit 5, Mockito, Spring Test | Metodologia TDD com 100% de cobertura de serviços |

---

## 7. Como Executar o Projeto

### Pré-requisitos
- [Docker e Docker Compose](https://www.docker.com/)
- [Java Development Kit (JDK 17+)](https://adoptium.net/)
- [Ngrok CLI](https://ngrok.com/download)

### Passo 1: Subir os Containers Docker
Na raiz do repositório, execute:
```powershell
docker compose up -d
```
> Isso iniciará o **PostgreSQL 16 com pgvector** na porta `5432`, o **RabbitMQ** nas portas `5672` (AMQP) e `15672` (Painel Web: [http://localhost:15672](http://localhost:15672)) e o **Redis 7** na porta `6379`, usado para o estado conversacional do chatbot.
>
> Se a porta `5432` já estiver ocupada por outro Postgres na sua máquina, defina `POSTGRES_HOST_PORT=5433` (ou outra porta livre) no `.env` **antes** de subir os containers — o `docker-compose.yml` lê essa variável (`${POSTGRES_HOST_PORT:-5432}:5432`) e ajuste `SPRING_DATASOURCE_URL` de acordo.

### Passo 2: Configurar Variáveis de Ambiente (.env) e Rodar o Backend
Copie o modelo de ambiente ou edite o arquivo `.env` na raiz do projeto:
```powershell
Copy-Item .env.example .env
```
Preencha suas chaves no `.env`:
```properties
# Google Gemini (Chave gratuita em: https://aistudio.google.com/app/apikey)
GEMINI_API_KEY=sua_chave_gemini_aqui
GEMINI_MODEL_NAME=gemini-3.5-flash-lite
GEMINI_EMBEDDING_MODEL_NAME=gemini-embedding-001
GEMINI_TEMPERATURE=0.2

# Chave de Administração REST
ADMIN_API_KEY=teste

# Gateway Uaizap
UAZAPI_BASE_URL=https://free.uazapi.com
UAZAPI_API_KEY=sua_chave_uazapi
UAZAPI_INSTANCE=sua_instancia
# Opcional: só se você quiser que o painel /setup provisione instâncias sozinho (ver seção 9)
UAZAPI_ADMIN_TOKEN=
```

Em seguida, inicialize a aplicação:
```powershell
.\mvnw.cmd spring-boot:run
```

> **💡 Dica de Custo/Performance com Google Gemini:**  
> O modelo padrão configurado é o **`gemini-3.5-flash`** (ou `gemini-3.6-flash`), a geração mais moderna, econômica e de altíssima velocidade do Google. Para a geração de vetores semânticos do RAG, utilizamos o modelo oficial **`gemini-embedding-001`** configurado com `outputDimensionality: 768` (dimensões compatíveis com o índice HNSW do pgvector).

### Passo 3: Sincronizar Flashcards e Questões no RAG (pgvector)
Para que o tutor virtual responda a qualquer dúvida clínica ou técnica no WhatsApp com base no acervo de mais de 85 questões cadastradas, execute a sincronização via endpoint administrativo:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/admin/rag/sync-flashcards" -H "X-API-KEY: teste"
```
*(Ou passe `?courseId=1` ou `?subjectId=1` para sincronizar uma disciplina específica).*

#### Cadastrando conteúdo por payload

Para indexar material novo sem depender de arquivos no servidor, envie o conteúdo no corpo da requisição. Curso, disciplina e tópico viram **metadados do embedding**, o que permite particionar a busca vetorial por matéria:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/admin/rag/ingest" `
  -H "X-API-KEY: teste" -H "Content-Type: application/json" `
  -d '{
        "courseId": 1,
        "subjectId": 10,
        "topic": "Arritmias",
        "title": "Manejo da Fibrilação Atrial",
        "content": "Na FA com instabilidade hemodinâmica, a conduta é a cardioversão elétrica sincronizada..."
      }'
```

O texto é segmentado (300 tokens, overlap de 30) e cada trecho recebe `course_id`, `subject_id` e `topic`. A API valida que a disciplina informada pertence de fato ao curso informado.

#### Acompanhando o desempenho do grupo piloto

Os dois endpoints abaixo expõem os dados de retenção que embasam a seção de resultados da monografia — o mesmo diagnóstico que o aluno vê no WhatsApp, em formato consultável:

```powershell
# Perfil individual: taxa de acerto, tópicos frágeis e o teto de intervalo ativo em cada um
curl.exe -s "http://localhost:8080/api/admin/performance/students/<UUID>" -H "api_key: teste"

# Visão agregada de todos os estudantes, com desempenho por disciplina
curl.exe -s "http://localhost:8080/api/admin/performance/overview" -H "api_key: teste"
```

```json
{
  "windowDays": 90, "students": 7, "totalAttempts": 312, "overallAccuracy": 0.7115,
  "bySubject": [
    { "subjectId": 4, "subjectName": "Farmacologia Clínica", "attempts": 88, "accuracy": 0.5909 }
  ]
}
```

### Passo 4: Conectar o WhatsApp (túnel, QR Code e webhook)

Suba o túnel (o painel ainda não inicia o processo do Ngrok sozinho, só detecta um já rodando):
```powershell
ngrok http 8080
```

**Caminho recomendado a partir daqui — painel web automático:** acesse **`http://localhost:8080/setup`** com a aplicação rodando. Ele detecta o túnel Ngrok ativo automaticamente, provisiona uma instância na Uazapi se necessário (usando `UAZAPI_ADMIN_TOKEN`), gera e exibe o QR Code, faz o pareamento por polling e sincroniza o webhook sozinho assim que a instância conecta — zero passo manual além de rodar o `ngrok http 8080` e escanear o QR. Detalhes completos na seção 9.

**Caminho 100% manual (fallback, sem o painel):** copie a URL pública HTTPS gerada pelo Ngrok (ex: `https://xxxx.ngrok-free.app`) e, no painel da Uazapi, configure:
- **Webhook URL:** `https://xxxx.ngrok-free.app/webhook/uazapi`
- **Eventos:** `messages.upsert` (ou `messages`)

---

## 8. Variáveis de Ambiente

| Variável | Valor Padrão | Descrição |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/mmeebb_db` | URL de conexão com o PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuário do banco de dados |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Senha do banco de dados |
| `POSTGRES_HOST_PORT` | `5432` | Porta do host mapeada pro Postgres no `docker-compose.yml` (mude se `5432` já estiver em uso) |
| `SPRING_RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `SPRING_RABBITMQ_PORT` | `5672` | Porta AMQP do RabbitMQ |
| `SPRING_RABBITMQ_USERNAME` | `guest` | Usuário do RabbitMQ |
| `SPRING_RABBITMQ_PASSWORD` | `guest` | Senha do RabbitMQ |
| `SPRING_REDIS_HOST` | `localhost` | Host do Redis (estado conversacional) |
| `SPRING_REDIS_PORT` | `6379` | Porta do Redis |
| `SPRING_REDIS_PASSWORD` | *(Vazio)* | Senha do Redis, se houver |
| `MMEEBB_SESSION_TTL_MINUTES` | `60` | Tempo de expiração da sessão conversacional no Redis |
| `MMEEBB_SCHEDULER_CRON` | `0 */5 * * * *` | Frequência das rodadas de lembrete; cada aluno recebe no horário definido em Configurações |
| `MMEEBB_ADAPTIVE_ENABLED` | `true` | Liga a [personalização adaptativa](#31-personalização-adaptativa-por-desempenho-mmeebb-adaptativo). `false` devolve o sistema ao MMEEBB clássico — útil para a comparação experimental na monografia |
| `MMEEBB_ADAPTIVE_WINDOW_DAYS` | `90` | Janela de análise do desempenho: um mês ruim não marca o tópico para sempre |
| `MMEEBB_ADAPTIVE_MIN_ATTEMPTS` | `3` | Respostas mínimas no tópico antes de personalizar qualquer coisa |
| `MMEEBB_ADAPTIVE_FRAGILE_ERROR_RATE` | `0.50` | Taxa de erro que classifica o tópico como frágil (teto de 8 dias) |
| `MMEEBB_ADAPTIVE_CONSOLIDATING_ERROR_RATE` | `0.20` | Taxa de erro que classifica o tópico como em consolidação (teto de 64 dias) |
| `MMEEBB_ADAPTIVE_REINFORCEMENT_MAX_CARDS` | `2` | Questões do mesmo tópico antecipadas como reforço após erro em tópico frágil |
| `MMEEBB_ADAPTIVE_REPORT_MIN_ATTEMPTS` | `5` | Respostas mínimas antes de exibir percentuais no relatório de desempenho |
| `UAZAPI_BASE_URL` | `https://free.uazapi.com` | URL base do gateway da Uazapi |
| `UAZAPI_API_KEY` | *(Vazio)* | Token/Chave de autenticação da Uazapi |
| `UAZAPI_INSTANCE` | *(Vazio)* | Nome da instância do WhatsApp conectada |
| `UAZAPI_ADMIN_TOKEN` | *(Vazio)* | Admintoken da sua conta Uazapi (dashboard, **não** é a mesma coisa que `UAZAPI_API_KEY`) — habilita o botão "Provisionar Nova Instância" no painel `/setup` |
| `UAZAPI_TYPING_DELAY_MS` | `2000` | Delay simulado de digitação (*composing*) |
| `GEMINI_API_KEY` | *(Vazio)* | Chave de API do Google AI Studio (Gemini) |
| `GEMINI_MODEL_NAME` | `gemini-3.5-flash-lite` | Modelo de chat principal (failover automático para `gemini-3.5-flash` em picos de demanda) |
| `GEMINI_EMBEDDING_MODEL_NAME` | `gemini-embedding-001` | Modelo de embeddings do RAG (768 dimensões) |
| `GEMINI_TEMPERATURE` | `0.2` | Temperatura do modelo de chat (baixa, para respostas clínicas objetivas) |
| `ADMIN_API_KEY` | `teste` | Chave de segurança para endpoints `/api/admin/**` **e `/api/v1/setup/**`** (painel de conectividade) |
| `NGROK_CLIENT_API_URL` | `http://127.0.0.1:4040` | URL da API local de controle do Ngrok, usada pelo painel `/setup` para descobrir a URL pública do túnel |

---

## 9. Integração com WhatsApp (Uazapi & Ngrok)

### 9.1. Painel de Conectividade (`/setup`)

Conectar o bot ao WhatsApp manualmente (abrir o Ngrok, copiar a URL, colar no painel da Uazapi, gerar QR na mão) é um processo repetitivo e frágil para demonstrações — principalmente porque o **servidor demo gratuito da Uazapi (`free.uazapi.com`) expira instâncias automaticamente após 1 hora**. Para resolver isso, a aplicação expõe um painel web nativo em **`http://localhost:8080/setup`**, protegido pela mesma `ADMIN_API_KEY` dos endpoints `/api/admin/**`.

Fluxo do painel:
1. **Detecção do túnel**: consulta `GET http://127.0.0.1:4040/api/tunnels` (API local do Ngrok) para achar a URL pública ativa. Não inicia o processo do Ngrok sozinho — ele precisa estar rodando (`ngrok http 8080`).
2. **Provisionamento automático de instância** (`POST /api/v1/setup/instance/provision`): se não houver instância/token salvos (ou quando você clicar em "Provisionar Nova Instância"), a aplicação chama `POST /instance/init` na Uazapi com o **admintoken** da sua conta (`UAZAPI_ADMIN_TOKEN`, **diferente** do token de instância), recebe um token novo e salva **instância + token na tabela `system_configurations`** — não fica hardcoded em `.env`, então sobrevive a reinícios da aplicação mesmo trocando de instância a cada expiração de 1h.
3. **QR Code** (`POST /api/v1/setup/instance/connect`): chama `POST /instance/connect` com o token salvo, extrai `instance.qrcode` (já em `data:image/png;base64,...`) e exibe no painel com polling de status a cada 3s.
4. **Webhook automático**: assim que o polling detecta a instância `connected`, a aplicação configura o webhook sozinha (`POST /webhook` com a URL do Ngrok + eventos `messages.upsert`/`messages`) — idempotente, não reconfigura se já estiver apontando pra URL certa.
5. **Teste de fumaça**: botão dedicado dispara uma mensagem de teste real para o número informado.

### 9.2. Contrato real da API Uazapi

A documentação oficial (`docs.uazapi.com`) é uma SPA em JavaScript sem conteúdo acessível via scraping simples; o contrato abaixo foi confirmado testando diretamente contra `https://free.uazapi.com` (não por documentação lida):

| Ação | Rota | Header | Observação |
| :--- | :--- | :--- | :--- |
| Criar instância | `POST /instance/init` | `admintoken` | Body `{"name": "..."}`. Resposta traz `token` (novo) no root e em `instance.token`. |
| Conectar / obter QR | `POST /instance/connect` | `token` | **Sem** nome de instância na URL — o token sozinho identifica a instância. QR vem em `instance.qrcode`. |
| Consultar status | `GET /instance/status` | `token` | Estado em `instance.status` (`connecting`/`connected`/`disconnected`). |
| Configurar webhook | `POST /webhook` | `token` | Body `{"url", "events", "enabled"}`. |
| Desconectar / apagar | `POST /instance/disconnect` / `DELETE /instance` | `token` | Disconnect mantém a instância; delete remove de vez. |

### 9.3. Referência adicional (arquivos locais, não versionados)

`docs/GUIA_NGROK_WEBHOOK.md` e `docs/uazapi/UAZAPI_API_REFERENCE.md` existem no repositório de trabalho local mas **não são versionados** (`docs/` está no `.gitignore` — são notas de planejamento, não documentação pública). Se você tem o repositório clonado localmente com esses arquivos, eles cobrem o fluxo 100% manual passo a passo.

---

## 10. Suíte de Testes (TDD)

O projeto adota rigorosamente a metodologia **TDD (Test-Driven Development)** e testes de fumaça (*smoke tests*) em todas as camadas de serviço, controllers, DTOs e entidades.

Para executar a suíte completa de testes:
```powershell
.\mvnw.cmd test
```

### Resultados Atuais:
- **Total de Testes Unitários:** 428
- **Taxa de Aprovação:** 100% (0 Falhas, 0 Erros, 1 Ignorado)
- **Cobertura:** Cálculo matemático $2^n$, FSM de Sessões no Redis, formulário de cadastro, roteamento por intenção, correção semântica de respostas (inclusive por letra da alternativa), Tratamento de Intenção de Saída (*Exit Intent*), Ingestão e Sincronização RAG (particionada e global), Consumidores RabbitMQ, Notificações Ativas Push, Controladores Administrativos, **Configurações do Estudante** (apelido, horário individual do lembrete, pausa, troca de matrícula preservando progresso), **Painel de Conectividade Uazapi/Ngrok** (auto-descoberta de túnel, provisionamento de instância, QR Code, sincronização de webhook) e **Personalização Adaptativa** (classificação de domínio por tópico, teto de intervalo, lapso graduado, fila priorizada, reforço dirigido e relatório de desempenho).

> **Teste-chave do algoritmo:** `MmeebbServiceImplTest` percorre $n = 0 \dots 13$, em acerto e em erro, comparando a versão adaptativa sem histórico contra a clássica. É a prova executável de que a personalização **não descaracteriza o MMEEBB** — a contribuição central do trabalho permanece verificável.

> O teste `RedisChatSessionStoreTest` valida a ida e volta do estado por um Redis real e é **ignorado automaticamente** quando não há Redis acessível (porta `6399` por padrão, configurável via `REDIS_IT_PORT`), mantendo a suíte executável sem dependências externas.

> Os testes `RepetitionScheduleRepositoryTest` e `ReviewAttemptRepositoryTest` sobem um **Postgres real com pgvector via Testcontainers** e aplicam as migrations Flyway para validar o filtro de matrícula ativa, as consultas do reforço dirigido e as agregações de desempenho — o H2 não serve porque desconhece o tipo `jsonb` de `tb_flashcard`. Sem Docker disponível, eles são ignorados automaticamente.

---

## 11. Estrutura do Código

```text
c:\projeto-tcc
├── src/main/java/br/edu/unipam/tcc/
│   ├── config/          # Beans Spring (RabbitMQ, LangChain4j, Redis, etc.)
│   ├── consumer/        # Consumidores AMQP (@RabbitListener)
│   ├── controller/      # Endpoints REST e Webhooks (/webhook/uazapi)
│   ├── dto/             # DTOs de transporte de dados e mapeamento Uazapi
│   ├── entity/          # Entidades relacionais JPA (Course, Student, ReviewAttempt, etc.)
│   │   └── enums/       # ChatIntent, ChatState, TopicMastery
│   ├── exception/       # GlobalExceptionHandler e exceções de domínio
│   ├── flow/            # Handlers conversacionais (Configurações, Meu desempenho)
│   ├── observability/   # Métricas de domínio (Micrometer) e correlação de logs
│   ├── repository/      # Repositórios Spring Data JPA
│   ├── scheduler/       # Rotinas de disparo diário de revisões (@Scheduled)
│   ├── session/         # FSM conversacional no Redis (ChatSessionStore, ChatSessionState)
│   └── service/         # Interfaces e Implementações de regras de negócio
│       └── impl/        # ChatFlowOrchestrator, MmeebbService, AdaptiveReviewService,
│                        # PerformanceAnalysisService, SubjectRagService, IntentRouterService...
├── src/main/java/dev/langchain4j/model/googleai/
│   └── GeminiServiceCustomizer.java  # Timeout estendido + retry/failover 503/429 no cliente Gemini
├── src/main/resources/
│   ├── db/migration/    # Scripts SQL Flyway (V1__init_schema.sql em diante)
│   ├── static/setup/    # Painel web de conectividade Uazapi/Ngrok (index.html + js/setup.js)
│   └── application.yml  # Configurações do Spring Boot
├── docs/                # Especificações técnicas e manuais operacionais (local, não versionado)
│   ├── uazapi/          # Referência completa da API Uazapi
│   └── GUIA_NGROK_WEBHOOK.md
├── docker-compose.yml   # PostgreSQL 16 (pgvector) + RabbitMQ Management + Redis
└── pom.xml              # Dependências Maven do projeto
```

---

## 12. Governança Git & Commits Semânticos

O repositório adota o padrão **Conventional Commits**:

- `feat(escopo): descrição da nova funcionalidade`
- `fix(escopo): correção de bug`
- `test(escopo): adição ou melhoria de testes unitários`
- `docs(escopo): atualização de documentação ou especificações`
- `refactor(escopo): refatoração de código sem alteração de comportamento`
- `chore(escopo): tarefas de build, dependências ou configurações`

---

<div align="center">
  <sub>Desenvolvido com ☕ e dedicação por <b>Níckolas Tavares do Nascimento</b> — TCC UNIPAM 2026</sub>
</div>
