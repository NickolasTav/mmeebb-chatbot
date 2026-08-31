package br.edu.unipam.tcc.config;

import br.edu.unipam.tcc.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminApiKeyInterceptorTest {

    private AdminApiKeyInterceptor interceptor;
    private static final String VALID_KEY = "test-secret-key-123";

    @BeforeEach
    void setUp() {
        interceptor = new AdminApiKeyInterceptor(VALID_KEY);
    }

    @Test
    @DisplayName("Deve permitir requisição com header api_key correto")
    void shouldAllowWhenApiKeyHeaderIsValid() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/courses");
        request.addHeader("api_key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve permitir requisição com header X-API-KEY correto")
    void shouldAllowWhenXApiKeyHeaderIsValid() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/courses");
        request.addHeader("X-API-KEY", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve permitir requisição com header api-key correto")
    void shouldAllowWhenHyphenApiKeyHeaderIsValid() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/courses");
        request.addHeader("api-key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve permitir requisição OPTIONS (CORS preflight) sem header")
    void shouldAllowOptionsRequestWithoutHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/admin/courses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando o header de api_key estiver ausente")
    void shouldThrowWhenApiKeyHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/courses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Header de autenticação 'api_key' ou 'X-API-KEY' é obrigatório.");
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando a api_key informada for incorreta")
    void shouldThrowWhenApiKeyIsInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/courses");
        request.addHeader("api_key", "chave-errada-999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("API Key de administração inválida ou não autorizada.");
    }
}
