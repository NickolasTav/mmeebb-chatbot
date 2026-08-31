package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findBySubjectIdAndActiveTrue(Long subjectId);

    List<Flashcard> findBySubjectIdAndTopicAndActiveTrue(Long subjectId, String topic);
}
