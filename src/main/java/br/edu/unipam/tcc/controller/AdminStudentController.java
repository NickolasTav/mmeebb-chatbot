package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.SeedScheduleRequestDto;
import br.edu.unipam.tcc.dto.SeedScheduleResponseDto;
import br.edu.unipam.tcc.service.StudentAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentAdminService studentAdminService;

    @PostMapping("/by-phone/{phoneNumber}/seed-schedules")
    public ResponseEntity<SeedScheduleResponseDto> seedSchedules(
            @PathVariable String phoneNumber,
            @Valid @RequestBody SeedScheduleRequestDto requestDto
    ) {
        return ResponseEntity.ok(studentAdminService.seedSchedulesForStudent(phoneNumber, requestDto));
    }
}
