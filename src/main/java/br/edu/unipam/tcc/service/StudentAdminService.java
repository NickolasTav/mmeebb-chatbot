package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.SeedScheduleRequestDto;
import br.edu.unipam.tcc.dto.SeedScheduleResponseDto;

public interface StudentAdminService {

    SeedScheduleResponseDto seedSchedulesForStudent(String phoneNumber, SeedScheduleRequestDto requestDto);
}
