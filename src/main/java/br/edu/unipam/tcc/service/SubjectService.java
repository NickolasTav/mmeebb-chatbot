package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.SubjectRequestDto;
import br.edu.unipam.tcc.dto.SubjectResponseDto;
import br.edu.unipam.tcc.dto.SubjectStatusDto;

import java.util.List;

public interface SubjectService {

    List<SubjectResponseDto> findAll(Long courseId, Boolean active);

    SubjectResponseDto findById(Long id);

    SubjectResponseDto create(SubjectRequestDto requestDto);

    SubjectResponseDto update(Long id, SubjectRequestDto requestDto);

    SubjectResponseDto updateStatus(Long id, SubjectStatusDto statusDto);

    SubjectResponseDto activate(Long id);

    SubjectResponseDto deactivate(Long id);
}
