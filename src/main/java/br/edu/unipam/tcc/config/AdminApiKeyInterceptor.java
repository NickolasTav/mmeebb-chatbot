package br.edu.unipam.tcc.config;

import br.edu.unipam.tcc.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    private final String configuredApiKey;

    public AdminApiKeyInterceptor(@Value("${admin.api-key:}") String configuredApiKey) {
        this.configuredApiKey = configuredApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Permite requisições pre-flight CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String apiKey = request.getHeader("api_key");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getHeader("X-API-KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getHeader("api-key");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getHeader("apikey");
        }

        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            log.error("[AdminApiKeyInterceptor] Chave de administração não configurada no servidor (ADMIN_API_KEY vazia).");
            throw new UnauthorizedException("Acesso administrativo desabilitado: chave de API não configurada no servidor.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AdminApiKeyInterceptor] Requisição rejeitada em [{}] - Header de api_key ausente.", request.getRequestURI());
            throw new UnauthorizedException("Header de autenticação 'api_key' ou 'X-API-KEY' é obrigatório.");
        }

        if (!configuredApiKey.equals(apiKey.trim())) {
            log.warn("[AdminApiKeyInterceptor] Requisição rejeitada em [{}] - api_key inválida.", request.getRequestURI());
            throw new UnauthorizedException("API Key de administração inválida ou não autorizada.");
        }

        return true;
    }
}
