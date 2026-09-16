package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.MmeebbService;
import br.edu.unipam.tcc.service.StudentOnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentOnboardingServiceImpl implements StudentOnboardingService {

    private final StudentRepository studentRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FlashcardRepository flashcardRepository;
    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final MmeebbService mmeebbService;

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
        student = studentRepository.save(student);

        linkToCourse(student, course, academicPeriod);
        int seeded = seedSchedules(student, course);

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

    private int seedSchedules(Student student, Course course) {
        List<Subject> subjects = subjectRepository.findByCourseIdAndActiveTrue(course.getId());
        int created = 0;

        for (Subject subject : subjects) {
            for (Flashcard card : flashcardRepository.findBySubjectIdAndActiveTrue(subject.getId())) {
                boolean exists = repetitionScheduleRepository
                        .findByStudentIdAndFlashcardId(student.getId(), card.getId())
                        .isPresent();
                if (exists) {
                    continue;
                }

                RepetitionSchedule schedule = mmeebbService.initializeSchedule(student, card, LocalDate.now());
                // Disponibiliza a primeira rodada imediatamente, sem esperar o IRA de 1 dia.
                schedule.setNextReviewDate(LocalDate.now());
                repetitionScheduleRepository.save(schedule);
                created++;
            }
        }

        return created;
    }
}
