package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.CourseRequestDto;
import br.edu.unipam.tcc.dto.CourseResponseDto;
import br.edu.unipam.tcc.dto.CourseStatusDto;
import br.edu.unipam.tcc.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseResponseDto>> findAll(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(courseService.findAll(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CourseResponseDto> create(@Valid @RequestBody CourseRequestDto requestDto) {
        CourseResponseDto created = courseService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequestDto requestDto
    ) {
        return ResponseEntity.ok(courseService.update(id, requestDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CourseResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody CourseStatusDto statusDto
    ) {
        return ResponseEntity.ok(courseService.updateStatus(id, statusDto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CourseResponseDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CourseResponseDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.deactivate(id));
    }
}
