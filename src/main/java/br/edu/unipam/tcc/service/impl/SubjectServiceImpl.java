package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.SubjectRequestDto;
import br.edu.unipam.tcc.dto.SubjectResponseDto;
import br.edu.unipam.tcc.dto.SubjectStatusDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.exception.BusinessException;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponseDto> findAll(Long courseId, Boolean active) {
        List<Subject> subjects;
        if (courseId != null && active != null) {
            subjects = subjectRepository.findByCourseIdAndActive(courseId, active);
        } else if (courseId != null) {
            subjects = subjectRepository.findByCourseId(courseId);
        } else if (active != null) {
            subjects = subjectRepository.findByActive(active);
        } else {
            subjects = subjectRepository.findAll();
        }
        return subjects.stream().map(this::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponseDto findById(Long id) {
        Subject subject = findSubjectOrThrow(id);
        return toResponseDto(subject);
    }

    @Override
    @Transactional
    public SubjectResponseDto create(SubjectRequestDto requestDto) {
        log.info("[SubjectService] Criando nova matéria: {} para Curso ID: {}", requestDto.code(), requestDto.courseId());
        Course course = courseRepository.findById(requestDto.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso com ID " + requestDto.courseId() + " não encontrado."));

        if (subjectRepository.existsByCourseIdAndCode(course.getId(), requestDto.code().trim().toUpperCase())) {
            throw new BusinessException("Já existe uma matéria com o código '" + requestDto.code() + "' neste curso.");
        }

        Subject subject = Subject.builder()
                .course(course)
                .code(requestDto.code().trim().toUpperCase())
                .name(requestDto.name().trim())
                .description(requestDto.description() != null ? requestDto.description().trim() : null)
                .active(requestDto.active() != null ? requestDto.active() : true)
                .build();

        Subject saved = subjectRepository.save(subject);
        log.info("[SubjectService] Matéria criada com sucesso! ID: {}", saved.getId());
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    public SubjectResponseDto update(Long id, SubjectRequestDto requestDto) {
        log.info("[SubjectService] Atualizando matéria ID: {}", id);
        Subject subject = findSubjectOrThrow(id);

        Course course = courseRepository.findById(requestDto.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso com ID " + requestDto.courseId() + " não encontrado."));

        String formattedCode = requestDto.code().trim().toUpperCase();
        if (!subject.getCode().equalsIgnoreCase(formattedCode) || !subject.getCourse().getId().equals(course.getId())) {
            if (subjectRepository.existsByCourseIdAndCode(course.getId(), formattedCode)) {
                throw new BusinessException("Já existe uma matéria com o código '" + requestDto.code() + "' neste curso.");
            }
            subject.setCode(formattedCode);
        }

        subject.setCourse(course);
        subject.setName(requestDto.name().trim());
        subject.setDescription(requestDto.description() != null ? requestDto.description().trim() : null);
        if (requestDto.active() != null) {
            subject.setActive(requestDto.active());
        }

        Subject updated = subjectRepository.save(subject);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public SubjectResponseDto updateStatus(Long id, SubjectStatusDto statusDto) {
        log.info("[SubjectService] Atualizando status da matéria ID: {} para active={}", id, statusDto.active());
        Subject subject = findSubjectOrThrow(id);
        subject.setActive(statusDto.active());
        Subject updated = subjectRepository.save(subject);
        return toResponseDto(updated);
    }

    private Subject findSubjectOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina com ID " + id + " não encontrada."));
    }

    private SubjectResponseDto toResponseDto(Subject subject) {
        return new SubjectResponseDto(
                subject.getId(),
                subject.getCourse().getId(),
                subject.getCourse().getName(),
                subject.getCode(),
                subject.getName(),
                subject.getDescription(),
                subject.getActive(),
                subject.getCreatedAt(),
                subject.getUpdatedAt()
        );
    }
}
