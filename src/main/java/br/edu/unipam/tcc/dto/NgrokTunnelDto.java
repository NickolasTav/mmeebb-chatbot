package br.edu.unipam.tcc.dto;

public record NgrokTunnelDto(
        boolean online,
        String publicUrl,
        String localAddr,
        String webInterface,
        String message
) {

    public static NgrokTunnelDto offline(String message) {
        return new NgrokTunnelDto(false, null, null, null, message);
    }

    public static NgrokTunnelDto online(String publicUrl, String localAddr, String webInterface) {
        return new NgrokTunnelDto(true, publicUrl, localAddr, webInterface, "Túnel Ngrok detectado com sucesso.");
    }
}
