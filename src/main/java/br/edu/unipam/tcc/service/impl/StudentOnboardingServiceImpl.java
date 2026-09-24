package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.service.ScheduleSeedingService;
import br.edu.unipam.tcc.service.StudentOnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentOnboardingServiceImpl implements StudentOnboardingService {

    private final StudentRepository studentRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final CourseRepository courseRepository;
    private final ScheduleSeedingService scheduleSeedingService;
    private final Clock clock;

    @Override
    @Transactional
    public Student register(String phoneNumber, String fullName, String ra, Long courseId, Integer academicPeriod) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso com ID " + courseId + " não encontrado."));

        Student student = studentRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> Student.builder().phoneNumber(phoneNumber).active(true).build());

        student.setFullName(fullName);
        student.setRa(ra);
        student.setActive(true);
        // A mensagem de boas-vindas já informa as pendências do dia; o lembrete seria redundante.
        student.setLastReviewNotificationOn(LocalDate.now(clock));
        student = studentRepository.save(student);

        linkToCourse(student, course, academicPeriod);
        int seeded = scheduleSeedingService.seedMissingSchedules(student, course);

        log.info("[Onboarding] Estudante [{}] cadastrado no curso {} ({}º período) com {} agendamento(s) inicializado(s).",
                phoneNumber, course.getName(), academicPeriod, seeded);

        return student;
    }

    private void linkToCourse(Student student, Course course, Integer academicPeriod) {
        StudentCourse link = studentCourseRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseGet(() -> StudentCourse.builder().student(student).course(course).build());

        link.setAcademicPeriod(academicPeriod);
        link.setActive(true);
        studentCourseRepository.save(link);
    }
}
