package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.dto.AttemptTotalsDto;
import br.edu.unipam.tcc.dto.SubjectAttemptAggregateDto;
import br.edu.unipam.tcc.dto.TopicAttemptAggregateDto;
import br.edu.unipam.tcc.entity.ReviewAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Histórico de tentativas e as agregações que alimentam a camada adaptativa.
 *
 * <p>As agregações são calculadas sob demanda, sem tabela materializada de desempenho: na escala
 * do projeto (dezenas de estudantes) o {@code GROUP BY} indexado é irrisório, enquanto manter
 * agregados exigiria consistência transacional e recálculo a cada mudança de limiar.
 */
@Repository
public interface ReviewAttemptRepository extends JpaRepository<ReviewAttempt, Long> {

    @Query("SELECT new br.edu.unipam.tcc.dto.TopicAttemptAggregateDto(" +
           "  sub.id, sub.name, f.topic, COUNT(a), " +
           "  SUM(CASE WHEN a.correct = true THEN 0L ELSE 1L END)) " +
           "FROM ReviewAttempt a " +
           "JOIN a.flashcard f " +
           "JOIN f.subject sub " +
           "WHERE a.student.id = :studentId " +
           "AND a.answeredAt >= :since " +
           "GROUP BY sub.id, sub.name, f.topic")
    List<TopicAttemptAggregateDto> aggregateByTopic(
            @Param("studentId") UUID studentId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT new br.edu.unipam.tcc.dto.AttemptTotalsDto(" +
           "  COUNT(a), COALESCE(SUM(CASE WHEN a.correct = true THEN 1L ELSE 0L END), 0L)) " +
           "FROM ReviewAttempt a " +
           "WHERE a.student.id = :studentId " +
           "AND a.answeredAt >= :since")
    AttemptTotalsDto totalsByStudent(
            @Param("studentId") UUID studentId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT new br.edu.unipam.tcc.dto.AttemptTotalsDto(" +
           "  COUNT(a), COALESCE(SUM(CASE WHEN a.correct = true THEN 1L ELSE 0L END), 0L)) " +
           "FROM ReviewAttempt a " +
           "WHERE a.answeredAt >= :since")
    AttemptTotalsDto totalsOverall(@Param("since") LocalDateTime since);

    @Query("SELECT new br.edu.unipam.tcc.dto.SubjectAttemptAggregateDto(" +
           "  sub.id, sub.name, COUNT(a), " +
           "  COALESCE(SUM(CASE WHEN a.correct = true THEN 1L ELSE 0L END), 0L)) " +
           "FROM ReviewAttempt a " +
           "JOIN a.flashcard f " +
           "JOIN f.subject sub " +
           "WHERE a.answeredAt >= :since " +
           "GROUP BY sub.id, sub.name " +
           "ORDER BY COUNT(a) DESC, sub.name ASC")
    List<SubjectAttemptAggregateDto> aggregateBySubject(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.student.id) FROM ReviewAttempt a WHERE a.answeredAt >= :since")
    long countDistinctStudents(@Param("since") LocalDateTime since);
}
