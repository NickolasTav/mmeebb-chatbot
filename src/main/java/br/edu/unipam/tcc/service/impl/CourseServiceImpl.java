package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.CourseRequestDto;
import br.edu.unipam.tcc.dto.CourseResponseDto;
import br.edu.unipam.tcc.dto.CourseStatusDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.exception.BusinessException;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDto> findAll(Boolean active) {
        List<Course> courses;
        if (active != null) {
            courses = courseRepository.findByActive(active);
        } else {
            courses = courseRepository.findAll();
        }
        return courses.stream().map(this::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponseDto findById(Long id) {
        Course course = findCourseOrThrow(id);
        return toResponseDto(course);
    }

    @Override
    @Transactional
    public CourseResponseDto create(CourseRequestDto requestDto) {
        log.info("[CourseService] Criando novo curso com código: {}", requestDto.code());
        courseRepository.findByCode(requestDto.code()).ifPresent(c -> {
            throw new BusinessException("Já existe um curso com o código '" + requestDto.code() + "'");
        });

        Course course = Course.builder()
                .code(requestDto.code().trim().toUpperCase())
                .name(requestDto.name().trim())
                .description(requestDto.description() != null ? requestDto.description().trim() : null)
                .active(requestDto.active() != null ? requestDto.active() : true)
                .build();

        Course saved = courseRepository.save(course);
        log.info("[CourseService] Curso criado com sucesso! ID: {}, Código: {}", saved.getId(), saved.getCode());
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    public CourseResponseDto update(Long id, CourseRequestDto requestDto) {
        log.info("[CourseService] Atualizando curso ID: {}", id);
        Course course = findCourseOrThrow(id);

        if (!course.getCode().equalsIgnoreCase(requestDto.code().trim())) {
            courseRepository.findByCode(requestDto.code().trim()).ifPresent(c -> {
                if (!c.getId().equals(id)) {
                    throw new BusinessException("Já existe um curso com o código '" + requestDto.code() + "'");
                }
            });
            course.setCode(requestDto.code().trim().toUpperCase());
        }

        course.setName(requestDto.name().trim());
        course.setDescription(requestDto.description() != null ? requestDto.description().trim() : null);
        if (requestDto.active() != null) {
            course.setActive(requestDto.active());
        }

        Course updated = courseRepository.save(course);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public CourseResponseDto updateStatus(Long id, CourseStatusDto statusDto) {
        log.info("[CourseService] Atualizando status do curso ID: {} para active={}", id, statusDto.active());
        Course course = findCourseOrThrow(id);
        course.setActive(statusDto.active());
        Course updated = courseRepository.save(course);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public CourseResponseDto activate(Long id) {
        return updateStatus(id, new CourseStatusDto(true));
    }

    @Override
    @Transactional
    public CourseResponseDto deactivate(Long id) {
        return updateStatus(id, new CourseStatusDto(false));
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso com ID " + id + " não encontrado."));
    }

    private CourseResponseDto toResponseDto(Course course) {
        return new CourseResponseDto(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getDescription(),
                course.getActive(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
