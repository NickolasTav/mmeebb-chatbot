package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    List<Course> findByActiveTrue();

    List<Course> findByActive(Boolean active);
}
