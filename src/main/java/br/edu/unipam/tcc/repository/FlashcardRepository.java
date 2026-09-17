package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findBySubjectIdAndActiveTrue(Long subjectId);

    List<Flashcard> findBySubjectIdAndTopicAndActiveTrue(Long subjectId, String topic);

    List<Flashcard> findBySubjectId(Long subjectId);

    List<Flashcard> findBySubjectIdAndActive(Long subjectId, Boolean active);

    List<Flashcard> findByActive(Boolean active);

    /**
     * Projeção do ID da disciplina sem navegar pela relação lazy {@code Flashcard.subject},
     * evitando LazyInitializationException fora de uma transação (ex.: no consumidor RabbitMQ).
     */
    @Query("SELECT f.subject.id FROM Flashcard f WHERE f.id = :flashcardId")
    Optional<Long> findSubjectIdById(@Param("flashcardId") Long flashcardId);
}
