package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.SubjectRequestDto;
import br.edu.unipam.tcc.dto.SubjectResponseDto;
import br.edu.unipam.tcc.dto.SubjectStatusDto;
import br.edu.unipam.tcc.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/subjects")
@RequiredArgsConstructor
public class AdminSubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<SubjectResponseDto>> findAll(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(subjectService.findAll(courseId, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.findById(id));
    }

    @PostMapping
    public ResponseEntity<SubjectResponseDto> create(@Valid @RequestBody SubjectRequestDto requestDto) {
        SubjectResponseDto created = subjectService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequestDto requestDto
    ) {
        return ResponseEntity.ok(subjectService.update(id, requestDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SubjectResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SubjectStatusDto statusDto
    ) {
        return ResponseEntity.ok(subjectService.updateStatus(id, statusDto));
    }
}
