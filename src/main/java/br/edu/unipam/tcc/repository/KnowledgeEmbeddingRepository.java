package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.KnowledgeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeEmbeddingRepository extends JpaRepository<KnowledgeEmbedding, UUID> {

    List<KnowledgeEmbedding> findByCourseId(Long courseId);

    List<KnowledgeEmbedding> findBySubjectId(Long subjectId);
}
