package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.StudentProfileDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.service.ScheduleSeedingService;
import br.edu.unipam.tcc.service.StudentSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentSettingsServiceImpl implements StudentSettingsService {

    private final StudentRepository studentRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final CourseRepository courseRepository;
    private final ScheduleSeedingService scheduleSeedingService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public StudentProfileDto getProfile(UUID studentId) {
        Student student = loadStudent(studentId);
        Optional<StudentCourse> enrollment = studentCourseRepository.findByStudentIdAndActiveTrue(studentId)
                .stream()
                .findFirst();

        return new StudentProfileDto(
                student.getFullName(),
                student.getPreferredName(),
                student.displayName(),
                student.getRa(),
                enrollment.map(link -> link.getCourse().getId()).orElse(null),
                enrollment.map(link -> link.getCourse().getName()).orElse(null),
                enrollment.map(StudentCourse::getAcademicPeriod).orElse(null),
                student.getPreferredStudyTime(),
                Boolean.TRUE.equals(student.getReviewNotificationsEnabled())
        );
    }

    @Override
    @Transactional
    public void updatePreferredName(UUID studentId, String preferredName) {
        Student student = loadStudent(studentId);
        student.setPreferredName(preferredName);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public boolean updateStudyTime(UUID studentId, LocalTime studyTime) {
        Student student = loadStudent(studentId);
        student.setPreferredStudyTime(studyTime);
        boolean firesToday = skipTodayIfTimeAlreadyPassed(student);
        studentRepository.save(student);
        return firesToday;
    }

    @Override
    @Transactional
    public boolean toggleReviewNotifications(UUID studentId) {
        Student student = loadStudent(studentId);
        boolean enabled = !Boolean.TRUE.equals(student.getReviewNotificationsEnabled());
        student.setReviewNotificationsEnabled(enabled);
        if (enabled) {
            skipTodayIfTimeAlreadyPassed(student);
        }
        studentRepository.save(student);
        return enabled;
    }

    @Override
    @Transactional
    public void changeEnrollment(UUID studentId, Long courseId, int academicPeriod) {
        Student student = loadStudent(studentId);
        Course course = courseRepository.findById(courseId)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Curso com ID " + courseId + " não encontrado ou inativo."));

        studentCourseRepository.findByStudentIdAndActiveTrue(studentId).stream()
                .filter(link -> !link.getCourse().getId().equals(courseId))
                .forEach(link -> {
                    link.setActive(false);
                    studentCourseRepository.save(link);
                });

        StudentCourse target = studentCourseRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseGet(() -> StudentCourse.builder().student(student).course(course).active(false).build());
        boolean switchingCourse = !Boolean.TRUE.equals(target.getActive());

        target.setAcademicPeriod(academicPeriod);
        target.setActive(true);
        studentCourseRepository.save(target);

        if (switchingCourse) {
            // Só ao entrar em um curso: cards já agendados mantêm o progresso MMEEBB intacto.
            int seeded = scheduleSeedingService.seedMissingSchedules(student, course);
            log.info("[StudentSettings] Estudante [{}] matriculado em {} ({}º período); {} agendamento(s) novo(s).",
                    student.getPhoneNumber(), course.getName(), academicPeriod, seeded);
        }
    }

    /**
     * Um horário que já passou hoje só vale a partir de amanhã: marcar o dia como avaliado evita
     * que a próxima rodada do scheduler dispare um lembrete logo depois de o aluno configurar.
     *
     * @return {@code true} se o lembrete ainda pode sair hoje
     */
    private boolean skipTodayIfTimeAlreadyPassed(Student student) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        if (today.equals(student.getLastReviewNotificationOn())) {
            return false;
        }
        if (!student.getPreferredStudyTime().isAfter(now.toLocalTime())) {
            student.setLastReviewNotificationOn(today);
            return false;
        }
        return true;
    }

    private Student loadStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudante com ID " + studentId + " não encontrado."));
    }
}
