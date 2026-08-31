package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {

    List<StudentCourse> findByStudentIdAndActiveTrue(UUID studentId);

    Optional<StudentCourse> findByStudentIdAndCourseId(UUID studentId, Long courseId);
}
