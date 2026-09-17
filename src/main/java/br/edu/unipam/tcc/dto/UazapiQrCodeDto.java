package br.edu.unipam.tcc.dto;

public record UazapiQrCodeDto(
        String status,
        String instance,
        String qrcodeBase64,
        String pairingCode,
        Integer expiresInSeconds,
        String message
) {
}
