package br.edu.unipam.tcc.service;

/**
 * Contrato de serviço para Retrieval-Augmented Generation (RAG) dinâmico por disciplina.
 * Realiza a busca vetorial isolada no pgvector e gera respostas fundamentadas contra alucinações.
 */
public interface SubjectRagService {

    /**
     * Responde a uma dúvida acadêmica/clínica do estudante com base estrita no material didático
     * da disciplina selecionada no banco vetorial.
     *
     * @param userQuestion Pergunta ou dúvida enviada pelo estudante.
     * @param subjectId    Identificador da Disciplina / Matéria ativa na sessão do estudante.
     * @return Texto da resposta formatado em Markdown amigável para envio direto no WhatsApp.
     */
    String answerDoubt(String userQuestion, Long subjectId);
}
