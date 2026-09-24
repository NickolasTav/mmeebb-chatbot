package br.edu.unipam.tcc.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Timeouts dos RestClient da aplicação (Uazapi e Ngrok). Sem isso o Spring Boot escolhe o
 * OkHttp (trazido pelo LangChain4j) com leitura limitada a 10s, e o {@code POST /instance/init}
 * do servidor gratuito da Uazapi leva ~16s para responder, então o provisionamento de
 * instância no painel /setup sempre falhava por timeout.
 */
@Configuration
public class HttpClientConfig {

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
