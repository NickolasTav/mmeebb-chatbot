package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.SystemConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SystemConfigurationRepositoryTest {

    @Autowired
    private SystemConfigurationRepository repository;

    @Test
    @DisplayName("Deve salvar e buscar configuração por chave")
    void deveSalvarEBuscarConfiguracaoPorChave() {
        SystemConfiguration config = SystemConfiguration.builder()
                .configKey("uazapi.instance")
                .configValue("med-instance-01")
                .description("Instância ativa na Uazapi")
                .build();

        repository.save(config);

        Optional<SystemConfiguration> found = repository.findByConfigKey("uazapi.instance");

        assertThat(found).isPresent();
        assertThat(found.get().getConfigValue()).isEqualTo("med-instance-01");
    }

    @Test
    @DisplayName("Deve atualizar valor de configuração existente")
    void deveAtualizarValorDeConfiguracaoExistente() {
        repository.save(SystemConfiguration.builder()
                .configKey("ngrok.custom-domain")
                .configValue("")
                .description("Domínio estático opcional do Ngrok")
                .build());

        SystemConfiguration existing = repository.findById("ngrok.custom-domain").orElseThrow();
        existing.setConfigValue("meu-bot.ngrok-free.app");
        repository.save(existing);

        SystemConfiguration updated = repository.findById("ngrok.custom-domain").orElseThrow();

        assertThat(updated.getConfigValue()).isEqualTo("meu-bot.ngrok-free.app");
    }
}
