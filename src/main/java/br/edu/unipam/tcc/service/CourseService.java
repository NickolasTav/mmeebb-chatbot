package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.CourseRequestDto;
import br.edu.unipam.tcc.dto.CourseResponseDto;
import br.edu.unipam.tcc.dto.CourseStatusDto;

import java.util.List;

public interface CourseService {

    List<CourseResponseDto> findAll(Boolean active);

    CourseResponseDto findById(Long id);

    CourseResponseDto create(CourseRequestDto requestDto);

    CourseResponseDto update(Long id, CourseRequestDto requestDto);

    CourseResponseDto updateStatus(Long id, CourseStatusDto statusDto);

    CourseResponseDto activate(Long id);

    CourseResponseDto deactivate(Long id);
}
