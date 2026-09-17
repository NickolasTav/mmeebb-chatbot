package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.NgrokTunnelDto;
import br.edu.unipam.tcc.service.impl.NgrokTunnelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NgrokTunnelServiceImplTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private NgrokTunnelService ngrokTunnelService;

    private static final String CLIENT_API_URL = "http://127.0.0.1:4040";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        ngrokTunnelService = new NgrokTunnelServiceImpl(restClientBuilder, CLIENT_API_URL, "8080");
    }

    @Test
    @DisplayName("Deve retornar URL pública quando o Ngrok estiver ativo na porta 4040")
    void deveRetornarUrlPublicaQuandoNgrokEstiverAtivoNaPorta4040() {
        mockServer.expect(requestTo(CLIENT_API_URL + "/api/tunnels"))
                .andRespond(withSuccess("""
                        {
                          "tunnels": [
                            {"name": "command_line", "proto": "https", "public_url": "https://med-tcc.ngrok-free.app", "config": {"addr": "http://localhost:8080"}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        NgrokTunnelDto result = ngrokTunnelService.discoverTunnel();

        assertTrue(result.online());
        assertTrue(result.publicUrl().equals("https://med-tcc.ngrok-free.app"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve ignorar túnel HTTP e priorizar o túnel HTTPS")
    void deveIgnorarTunelHttpNaoSeguroEPriorizarHttps() {
        mockServer.expect(requestTo(CLIENT_API_URL + "/api/tunnels"))
                .andRespond(withSuccess("""
                        {
                          "tunnels": [
                            {"name": "command_line_http", "proto": "http", "public_url": "http://med-tcc.ngrok-free.app", "config": {"addr": "http://localhost:8080"}},
                            {"name": "command_line_https", "proto": "https", "public_url": "https://med-tcc.ngrok-free.app", "config": {"addr": "http://localhost:8080"}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        NgrokTunnelDto result = ngrokTunnelService.discoverTunnel();

        assertTrue(result.online());
        assertTrue(result.publicUrl().startsWith("https://"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve retornar status offline com timeout sem lançar exceção quando o Ngrok estiver fechado")
    void deveRetornarStatusOfflineComTimeoutSemLancarExcecaoQuandoNgrokEstiverFechado() {
        mockServer.expect(requestTo(CLIENT_API_URL + "/api/tunnels"))
                .andRespond(request -> {
                    throw new IOException("Connection refused: 127.0.0.1:4040");
                });

        NgrokTunnelDto result = assertDoesNotThrow(() -> ngrokTunnelService.discoverTunnel());

        assertFalse(result.online());
        assertTrue(result.publicUrl() == null);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve retornar offline sem lançar exceção quando a API do Ngrok responder com erro de servidor")
    void deveRetornarOfflineQuandoNgrokResponderComErro() {
        mockServer.expect(requestTo(CLIENT_API_URL + "/api/tunnels"))
                .andRespond(withServerError());

        NgrokTunnelDto result = assertDoesNotThrow(() -> ngrokTunnelService.discoverTunnel());

        assertFalse(result.online());
        mockServer.verify();
    }

    @Test
    @DisplayName("isTunnelOnline deve refletir o resultado de discoverTunnel")
    void isTunnelOnlineDeveRefletirDiscoverTunnel() {
        mockServer.expect(requestTo(CLIENT_API_URL + "/api/tunnels"))
                .andRespond(withServerError());

        assertFalse(ngrokTunnelService.isTunnelOnline());
        mockServer.verify();
    }
}
