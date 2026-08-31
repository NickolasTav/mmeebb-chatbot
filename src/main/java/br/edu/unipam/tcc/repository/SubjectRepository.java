package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByCourseIdAndActiveTrue(Long courseId);

    Optional<Subject> findByCourseIdAndCode(Long courseId, String code);
}
