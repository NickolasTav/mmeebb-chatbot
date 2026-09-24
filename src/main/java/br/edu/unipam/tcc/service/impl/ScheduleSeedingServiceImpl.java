package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.MmeebbService;
import br.edu.unipam.tcc.service.ScheduleSeedingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ScheduleSeedingServiceImpl implements ScheduleSeedingService {

    private final SubjectRepository subjectRepository;
    private final FlashcardRepository flashcardRepository;
    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final MmeebbService mmeebbService;
    private final Clock clock;

    @Override
    public int seedMissingSchedules(Student student, Course course) {
        LocalDate today = LocalDate.now(clock);
        int created = 0;

        for (Subject subject : subjectRepository.findByCourseIdAndActiveTrue(course.getId())) {
            for (Flashcard card : flashcardRepository.findBySubjectIdAndActiveTrue(subject.getId())) {
                boolean exists = repetitionScheduleRepository
                        .findByStudentIdAndFlashcardId(student.getId(), card.getId())
                        .isPresent();
                if (exists) {
                    continue;
                }

                RepetitionSchedule schedule = mmeebbService.initializeSchedule(student, card, today);
                // Disponibiliza a primeira rodada imediatamente, sem esperar o IRA de 1 dia.
                schedule.setNextReviewDate(today);
                repetitionScheduleRepository.save(schedule);
                created++;
            }
        }

        return created;
    }
}
