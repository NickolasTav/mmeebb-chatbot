package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.NgrokTunnelDto;
import br.edu.unipam.tcc.service.NgrokTunnelService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Consulta a API local de controle do Ngrok ({@code 127.0.0.1:4040}) para descobrir a URL
 * pública HTTPS do túnel ativo, sem exigir que o desenvolvedor copie/cole a URL manualmente.
 */
@Slf4j
@Service
public class NgrokTunnelServiceImpl implements NgrokTunnelService {

    private final RestClient restClient;
    private final String clientApiUrl;
    private final String localAddrFallback;

    public NgrokTunnelServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${ngrok.client-api-url:http://127.0.0.1:4040}") String clientApiUrl,
            @Value("${server.port:8080}") String serverPort
    ) {
        this.clientApiUrl = clientApiUrl != null ? clientApiUrl.replaceAll("/+$", "") : "http://127.0.0.1:4040";
        this.localAddrFallback = "http://localhost:" + serverPort;
        this.restClient = restClientBuilder.baseUrl(this.clientApiUrl).build();
    }

    @Override
    public NgrokTunnelDto getTunnelStatus() {
        return discoverTunnel();
    }

    @Override
    public NgrokTunnelDto discoverTunnel() {
        try {
            NgrokTunnelsResponse response = restClient.get()
                    .uri("/api/tunnels")
                    .retrieve()
                    .body(NgrokTunnelsResponse.class);

            if (response == null || response.tunnels() == null || response.tunnels().isEmpty()) {
                log.debug("[NgrokTunnelService] Nenhum túnel ativo retornado por {}.", clientApiUrl);
                return NgrokTunnelDto.offline("Nenhum túnel Ngrok ativo encontrado. Execute 'ngrok http " + portFromLocalAddr() + "'.");
            }

            return response.tunnels().stream()
                    .filter(tunnel -> "https".equalsIgnoreCase(tunnel.proto()))
                    .findFirst()
                    .map(tunnel -> NgrokTunnelDto.online(
                            tunnel.publicUrl(),
                            tunnel.config() != null ? tunnel.config().addr() : localAddrFallback,
                            clientApiUrl))
                    .orElseGet(() -> NgrokTunnelDto.offline("Túnel Ngrok ativo, porém sem endpoint HTTPS exposto."));
        } catch (RestClientException | IllegalStateException e) {
            log.debug("[NgrokTunnelService] Ngrok não detectado em {}: {}", clientApiUrl, e.getMessage());
            return NgrokTunnelDto.offline("Ngrok não detectado em " + clientApiUrl + ". Inicie com 'ngrok http " + portFromLocalAddr() + "'.");
        }
    }

    @Override
    public boolean isTunnelOnline() {
        return discoverTunnel().online();
    }

    private String portFromLocalAddr() {
        int lastColon = localAddrFallback.lastIndexOf(':');
        return lastColon >= 0 ? localAddrFallback.substring(lastColon + 1) : "8080";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NgrokTunnelsResponse(List<NgrokApiTunnel> tunnels) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NgrokApiTunnel(
            String name,
            String proto,
            @JsonProperty("public_url") String publicUrl,
            NgrokApiTunnelConfig config
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NgrokApiTunnelConfig(String addr) {
    }
}
