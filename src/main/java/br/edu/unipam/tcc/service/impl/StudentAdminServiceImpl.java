package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.SeedScheduleRequestDto;
import br.edu.unipam.tcc.dto.SeedScheduleResponseDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.MmeebbService;
import br.edu.unipam.tcc.service.StudentAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentAdminServiceImpl implements StudentAdminService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final FlashcardRepository flashcardRepository;
    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final MmeebbService mmeebbService;

    @Override
    @Transactional
    public SeedScheduleResponseDto seedSchedulesForStudent(String phoneNumber, SeedScheduleRequestDto requestDto) {
        String cleanPhone = phoneNumber != null ? phoneNumber.replaceAll("[^0-9]", "") : "";
        if (cleanPhone.isBlank()) {
            throw new IllegalArgumentException("O número de telefone é obrigatório.");
        }

        log.info("[StudentAdminService] Inicializando agendamentos para [{}] na Matéria ID: {}", cleanPhone, requestDto.subjectId());

        Student student = studentRepository.findByPhoneNumber(cleanPhone)
                .orElseGet(() -> studentRepository.save(
                        Student.builder()
                                .phoneNumber(cleanPhone)
                                .fullName("Estudante Teste")
                                .active(true)
                                .build()
                ));

        Subject subject = subjectRepository.findById(requestDto.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina com ID " + requestDto.subjectId() + " não encontrada."));

        List<Flashcard> activeCards = flashcardRepository.findBySubjectIdAndActiveTrue(subject.getId());
        int createdCount = 0;

        for (Flashcard card : activeCards) {
            boolean exists = repetitionScheduleRepository
                    .findByStudentIdAndFlashcardId(student.getId(), card.getId())
                    .isPresent();

            if (!exists) {
                RepetitionSchedule schedule = mmeebbService.initializeSchedule(student, card, LocalDate.now().minusDays(1));
                schedule.setNextReviewDate(LocalDate.now());
                repetitionScheduleRepository.save(schedule);
                createdCount++;
            }
        }

        log.info("[StudentAdminService] {} novo(s) agendamento(s) inicializado(s) para [{}]", createdCount, cleanPhone);

        return new SeedScheduleResponseDto(
                student.getId(),
                student.getPhoneNumber(),
                student.getFullName(),
                subject.getId(),
                subject.getName(),
                createdCount
        );
    }
}
