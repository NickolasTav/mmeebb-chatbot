package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepetitionScheduleRepository extends JpaRepository<RepetitionSchedule, Long> {

    /**
     * Questão desativada no banco (por exemplo, as abertas que a V10 tirou de circulação) não entra
     * na fila, nas contagens nem nos lembretes. Um filtro único em todas as consultas mantém o
     * cabeçalho "faltam N hoje", o lembrete e a questão apresentada sempre de acordo.
     */
    String ACTIVE_FLASHCARD_FILTER = "AND s.flashcard.active = true ";

    /**
     * Restringe revisões e lembretes aos cards de cursos em que o estudante está matriculado.
     * Trocar de curso apenas desativa a matrícula anterior, então o progresso MMEEBB do curso
     * antigo fica guardado e volta a valer se o aluno retornar a ele.
     */
    String ACTIVE_ENROLLMENT_FILTER =
            "AND EXISTS (SELECT sc.id FROM StudentCourse sc " +
            "WHERE sc.student.id = s.student.id " +
            "AND sc.course.id = s.flashcard.subject.course.id " +
            "AND sc.active = true) ";

    /**
     * O agendamento do card em revisão é usado fora de transação pelo orquestrador, e o motor
     * MMEEBB lê a disciplina do flashcard para a métrica de domínio. Sem trazer os dois carregados
     * aqui, esse acesso estoura com LazyInitializationException e o bot emudece na resposta do aluno.
     */
    @EntityGraph(attributePaths = {"flashcard", "flashcard.subject"})
    Optional<RepetitionSchedule> findByStudentIdAndFlashcardId(UUID studentId, Long flashcardId);

    List<RepetitionSchedule> findByStudentId(UUID studentId);

    /**
     * Guarda a posição em que a alternativa correta foi exibida, no momento em que a questão é
     * apresentada (vale mesmo se o aluno sair sem responder). É memória de exibição, não estado do
     * MMEEBB: o UPDATE direto não incrementa a versão, então não compete com a gravação do ciclo
     * feita depois pela resposta do aluno.
     */
    @Modifying
    @Transactional
    @Query("UPDATE RepetitionSchedule s SET s.lastCorrectPosition = :position WHERE s.id = :id")
    int updateLastCorrectPosition(@Param("id") Long id, @Param("position") Short position);

    long countByStudentIdAndNextReviewDateLessThanEqualAndStatus(
            UUID studentId,
            LocalDate nextReviewDate,
            ScheduleStatus status
    );

    long countByStudentIdAndNextReviewDateLessThanEqualAndStatusIn(
            UUID studentId,
            LocalDate nextReviewDate,
            Collection<ScheduleStatus> statuses
    );

    @Query("SELECT COUNT(s) FROM RepetitionSchedule s " +
           "WHERE s.student.id = :studentId " +
           "AND s.nextReviewDate <= :currentDate " +
           "AND s.status IN :statuses " +
           ACTIVE_FLASHCARD_FILTER +
           ACTIVE_ENROLLMENT_FILTER)
    long countPendingReviewsByStudent(
            @Param("studentId") UUID studentId,
            @Param("currentDate") LocalDate currentDate,
            @Param("statuses") Collection<ScheduleStatus> statuses
    );

    @Query("SELECT COUNT(s) FROM RepetitionSchedule s " +
           "WHERE s.student.id = :studentId " +
           "AND s.nextReviewDate <= :currentDate " +
           ACTIVE_FLASHCARD_FILTER +
           "AND s.status != 'COMPLETED' " +
           ACTIVE_ENROLLMENT_FILTER)
    long countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(
            @Param("studentId") UUID studentId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT s FROM RepetitionSchedule s " +
           "JOIN FETCH s.flashcard f " +
           "JOIN FETCH f.subject sub " +
           "WHERE s.student.id = :studentId " +
           "AND s.nextReviewDate <= :currentDate " +
           "AND s.status = :status " +
           ACTIVE_FLASHCARD_FILTER +
           ACTIVE_ENROLLMENT_FILTER +
           "ORDER BY s.nextReviewDate ASC")
    List<RepetitionSchedule> findPendingReviewsByStudent(
            @Param("studentId") UUID studentId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") ScheduleStatus status
    );

    /**
     * Revisões vencidas: questões que o estudante já respondeu ao menos uma vez. Mesmo recorte da
     * {@link #findPendingReviewsByStudent}, para o lembrete anunciar exatamente a fila que o aluno
     * vai receber, sem carregar centenas de agendamentos só para contá-los.
     */
    @Query("SELECT COUNT(s) FROM RepetitionSchedule s " +
           "WHERE s.student.id = :studentId " +
           "AND s.nextReviewDate <= :currentDate " +
           "AND s.status = :status " +
           "AND s.lastReviewedAt IS NOT NULL " +
           ACTIVE_FLASHCARD_FILTER +
           ACTIVE_ENROLLMENT_FILTER)
    long countDueReviewsByStudent(
            @Param("studentId") UUID studentId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") ScheduleStatus status
    );

    /** Questões vencidas que o estudante nunca respondeu (as "novas" da fila do dia). */
    @Query("SELECT COUNT(s) FROM RepetitionSchedule s " +
           "WHERE s.student.id = :studentId " +
           "AND s.nextReviewDate <= :currentDate " +
           "AND s.status = :status " +
           "AND s.lastReviewedAt IS NULL " +
           ACTIVE_FLASHCARD_FILTER +
           ACTIVE_ENROLLMENT_FILTER)
    long countDueNewQuestionsByStudent(
            @Param("studentId") UUID studentId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") ScheduleStatus status
    );

    @Query("SELECT s FROM RepetitionSchedule s " +
           "JOIN FETCH s.flashcard f " +
           "JOIN FETCH f.subject sub " +
           "WHERE s.student.id = :studentId " +
           "AND sub.id = :subjectId " +
           "AND s.nextReviewDate <= :currentDate " +
           "AND s.status = :status " +
           ACTIVE_FLASHCARD_FILTER +
           ACTIVE_ENROLLMENT_FILTER +
           "ORDER BY s.nextReviewDate ASC")
    List<RepetitionSchedule> findPendingReviewsByStudentAndSubject(
            @Param("studentId") UUID studentId,
            @Param("subjectId") Long subjectId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") ScheduleStatus status
    );

    /**
     * Quantas questões do tópico já estão vencidas hoje. É o que limita o reforço dirigido:
     * um tópico que já lotou a fila do dia não recebe antecipações adicionais.
     */
    @Query("SELECT COUNT(s) FROM RepetitionSchedule s " +
           "WHERE s.student.id = :studentId " +
           "AND s.flashcard.subject.id = :subjectId " +
           "AND s.flashcard.topic = :topic " +
           "AND s.nextReviewDate <= :currentDate " +
           ACTIVE_FLASHCARD_FILTER +
           "AND s.status <> 'COMPLETED' " +
           ACTIVE_ENROLLMENT_FILTER)
    long countDueSchedulesByTopic(
            @Param("studentId") UUID studentId,
            @Param("subjectId") Long subjectId,
            @Param("topic") String topic,
            @Param("currentDate") LocalDate currentDate
    );

    /**
     * Questões do mesmo tópico ainda agendadas para o futuro, candidatas a serem antecipadas como
     * reforço depois de um erro. A ordem traz primeiro as que já estavam mais próximas de vencer.
     */
    @Query("SELECT s FROM RepetitionSchedule s " +
           "JOIN FETCH s.flashcard f " +
           "JOIN FETCH f.subject sub " +
           "WHERE s.student.id = :studentId " +
           "AND sub.id = :subjectId " +
           "AND f.topic = :topic " +
           "AND s.nextReviewDate > :currentDate " +
           ACTIVE_FLASHCARD_FILTER +
           "AND s.status <> 'COMPLETED' " +
           ACTIVE_ENROLLMENT_FILTER +
           "ORDER BY s.nextReviewDate ASC")
    List<RepetitionSchedule> findFutureSchedulesByTopic(
            @Param("studentId") UUID studentId,
            @Param("subjectId") Long subjectId,
            @Param("topic") String topic,
            @Param("currentDate") LocalDate currentDate
    );
}
