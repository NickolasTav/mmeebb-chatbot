package br.edu.unipam.tcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * O arquivo .env é carregado pelo próprio Spring via
 * {@code spring.config.import=optional:file:.env[.properties]} em application.yml.
 */
@EnableScheduling
@SpringBootApplication
public class MmeebbChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmeebbChatbotApplication.class, args);
    }
}
