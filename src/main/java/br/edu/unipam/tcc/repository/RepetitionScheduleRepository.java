package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepetitionScheduleRepository extends JpaRepository<RepetitionSchedule, Long> {

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

    Optional<RepetitionSchedule> findByStudentIdAndFlashcardId(UUID studentId, Long flashcardId);

    List<RepetitionSchedule> findByStudentId(UUID studentId);

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
           "AND s.flashcard.active = true " +
           ACTIVE_ENROLLMENT_FILTER)
    long countPendingReviewsByStudent(
            @Param("studentId") UUID studentId,
            @Param("currentDate") LocalDate currentDate,
            @Param("statuses") Collection<ScheduleStatus> statuses
    );

    @Query("SELECT COUNT(s) FROM RepetitionSchedule s " +
           "WHERE s.student.id = :studentId " +
           "AND s.nextReviewDate <= :currentDate " +
           "AND s.flashcard.active = true " +
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
           ACTIVE_ENROLLMENT_FILTER +
           "ORDER BY s.nextReviewDate ASC")
    List<RepetitionSchedule> findPendingReviewsByStudent(
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
           ACTIVE_ENROLLMENT_FILTER +
           "ORDER BY s.nextReviewDate ASC")
    List<RepetitionSchedule> findPendingReviewsByStudentAndSubject(
            @Param("studentId") UUID studentId,
            @Param("subjectId") Long subjectId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") ScheduleStatus status
    );
}
