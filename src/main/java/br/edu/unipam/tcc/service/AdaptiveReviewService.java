package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.AdaptiveReviewOutcomeDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ponto único da personalização por desempenho. Concentra as três decisões adaptativas para que o
 * orquestrador de conversa continue responsável apenas por formatar a mensagem do estudante.
 */
public interface AdaptiveReviewService {

    /**
     * Aplica o MMEEBB modulado pelo domínio do tópico, persiste o agendamento, registra a tentativa
     * no histórico e, em caso de erro em tópico frágil, antecipa questões relacionadas.
     */
    AdaptiveReviewOutcomeDto processAnswer(Student student, Flashcard flashcard,
                                           boolean correct, LocalDateTime answeredAt);

    /**
     * Reordena as revisões vencidas do dia colocando os tópicos mais frágeis primeiro.
     *
     * <p>Importa porque o interno cansado costuma responder só as primeiras questões da sessão:
     * essas precisam ser as que mais pesam na retenção dele.
     *
     * <p>Espera agendamentos com flashcard e disciplina já carregados (as consultas de pendência
     * usam {@code JOIN FETCH}).
     */
    List<RepetitionSchedule> prioritize(UUID studentId, List<RepetitionSchedule> pending);
}
