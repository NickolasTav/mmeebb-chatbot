package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.UazapiConnectRequestDto;
import br.edu.unipam.tcc.dto.UazapiProvisionResponseDto;
import br.edu.unipam.tcc.dto.UazapiQrCodeDto;
import br.edu.unipam.tcc.dto.UazapiSetupStatusDto;

public interface UazapiInstanceService {

    UazapiQrCodeDto requestQrCode(UazapiConnectRequestDto request);

    UazapiSetupStatusDto.UazapiStatus getInstanceStatus();

    boolean configureWebhook(String webhookUrl);

    void sendSmokeTestMessage(String phoneNumber, String message);

    /**
     * Cria uma nova instância na Uazapi via {@code admintoken} e persiste o nome/token
     * gerados em {@code system_configurations}, substituindo a instância anterior (útil
     * porque instâncias do servidor demo gratuito expiram automaticamente após 1h).
     */
    UazapiProvisionResponseDto provisionInstance();
}
