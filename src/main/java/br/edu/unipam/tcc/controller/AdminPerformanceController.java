package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.PerformanceOverviewDto;
import br.edu.unipam.tcc.dto.StudentPerformanceReportDto;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.service.PerformanceAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Acompanhamento do grupo piloto: expõe o mesmo diagnóstico que o aluno vê no WhatsApp, além da
 * visão agregada por disciplina. É a fonte de dados da seção de resultados da monografia.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/performance")
@RequiredArgsConstructor
public class AdminPerformanceController {

    private final PerformanceAnalysisService performanceAnalysisService;
    private final StudentRepository studentRepository;

    @GetMapping("/students/{studentId}")
    public ResponseEntity<StudentPerformanceReportDto> byStudent(@PathVariable UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudante", studentId));

        return ResponseEntity.ok(performanceAnalysisService.buildStudentReport(student));
    }

    @GetMapping("/overview")
    public ResponseEntity<PerformanceOverviewDto> overview() {
        return ResponseEntity.ok(performanceAnalysisService.buildOverview());
    }
}
