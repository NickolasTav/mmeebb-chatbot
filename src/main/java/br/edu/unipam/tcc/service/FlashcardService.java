package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.FlashcardRequestDto;
import br.edu.unipam.tcc.dto.FlashcardResponseDto;
import br.edu.unipam.tcc.dto.FlashcardStatusDto;

import java.util.List;

public interface FlashcardService {

    List<FlashcardResponseDto> findAll(Long subjectId, Boolean active);

    FlashcardResponseDto findById(Long id);

    FlashcardResponseDto create(FlashcardRequestDto requestDto);

    FlashcardResponseDto update(Long id, FlashcardRequestDto requestDto);

    FlashcardResponseDto updateStatus(Long id, FlashcardStatusDto statusDto);
}
