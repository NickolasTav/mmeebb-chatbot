package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {

    Optional<SystemConfiguration> findByConfigKey(String configKey);
}
