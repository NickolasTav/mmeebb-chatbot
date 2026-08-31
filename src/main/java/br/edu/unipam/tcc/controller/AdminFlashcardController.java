package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.FlashcardRequestDto;
import br.edu.unipam.tcc.dto.FlashcardResponseDto;
import br.edu.unipam.tcc.dto.FlashcardStatusDto;
import br.edu.unipam.tcc.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/flashcards")
@RequiredArgsConstructor
public class AdminFlashcardController {

    private final FlashcardService flashcardService;

    @GetMapping
    public ResponseEntity<List<FlashcardResponseDto>> findAll(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(flashcardService.findAll(subjectId, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlashcardResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(flashcardService.findById(id));
    }

    @PostMapping
    public ResponseEntity<FlashcardResponseDto> create(@Valid @RequestBody FlashcardRequestDto requestDto) {
        FlashcardResponseDto created = flashcardService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlashcardResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody FlashcardRequestDto requestDto
    ) {
        return ResponseEntity.ok(flashcardService.update(id, requestDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FlashcardResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody FlashcardStatusDto statusDto
    ) {
        return ResponseEntity.ok(flashcardService.updateStatus(id, statusDto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<FlashcardResponseDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(flashcardService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<FlashcardResponseDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(flashcardService.deactivate(id));
    }
}
