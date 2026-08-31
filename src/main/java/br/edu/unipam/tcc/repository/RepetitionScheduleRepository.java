package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepetitionScheduleRepository extends JpaRepository<RepetitionSchedule, Long> {

    Optional<RepetitionSchedule> findByStudentIdAndFlashcardId(UUID studentId, Long flashcardId);

    List<RepetitionSchedule> findByStudentId(UUID studentId);

    @Query("SELECT s FROM RepetitionSchedule s " +
           "JOIN FETCH s.flashcard f " +
           "JOIN FETCH f.subject sub " +
           "WHERE s.student.id = :studentId " +
           "AND s.nextReviewDate <= :currentDate " +
           "AND s.status = :status " +
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
           "ORDER BY s.nextReviewDate ASC")
    List<RepetitionSchedule> findPendingReviewsByStudentAndSubject(
            @Param("studentId") UUID studentId,
            @Param("subjectId") Long subjectId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") ScheduleStatus status
    );
}
