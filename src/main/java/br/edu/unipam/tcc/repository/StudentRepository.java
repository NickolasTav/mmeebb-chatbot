package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByPhoneNumber(String phoneNumber);

    Optional<Student> findByRa(String ra);

    boolean existsByPhoneNumber(String phoneNumber);

    List<Student> findByActiveTrue();

    /**
     * Alunos cujo horário de lembrete já chegou e que ainda não foram avaliados hoje.
     * Comparar com "<=" (em vez de igualdade) permite rodadas de poucos em poucos minutos
     * com qualquer minuto escolhido pelo aluno e recupera o lembrete de quem ficaria de fora
     * enquanto a aplicação esteve indisponível.
     */
    @Query("SELECT s FROM Student s " +
           "WHERE s.active = true " +
           "AND s.reviewNotificationsEnabled = true " +
           "AND s.preferredStudyTime <= :currentTime " +
           "AND (s.lastReviewNotificationOn IS NULL OR s.lastReviewNotificationOn < :today)")
    List<Student> findDueForReviewNotification(
            @Param("currentTime") LocalTime currentTime,
            @Param("today") LocalDate today
    );
}
