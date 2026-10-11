package br.edu.unipam.tcc.dto;

/**
 * Pedido de conexão da instância pelo painel {@code /setup}. Sem {@code phone}, a Uazapi devolve o QR Code;
 * com o número do bot (DDI + DDD + número), devolve o código de pareamento para digitar no celular.
 */
public record UazapiConnectRequestDto(
        String baseUrl,
        String instance,
        String apiKey,
        String phone
) {
}
