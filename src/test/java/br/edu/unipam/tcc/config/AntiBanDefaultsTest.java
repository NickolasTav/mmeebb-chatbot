package br.edu.unipam.tcc.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O ritmo por destinatário só liga por configuração explícita: sem variáveis de ambiente, o
 * application.yml mantém a estratégia anti-ban original (D8/N3).
 */
class AntiBanDefaultsTest {

    @Test
    @DisplayName("application.yml: modo anti-ban padrão é global e o teto global padrão é 1000 ms")
    void padraoEGlobalComTetoDeUmSegundo() throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        for (PropertySource<?> source : new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"))) {
            sources.addLast(source);
        }
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(sources);

        assertEquals("global", resolver.getProperty("uazapi.anti-ban.mode"));
        assertEquals(1000L, resolver.getProperty("uazapi.anti-ban.global-min-interval-ms", Long.class));
    }
}
