package br.edu.unipam.tcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EnableScheduling
@SpringBootApplication
public class MmeebbChatbotApplication {

    public static void main(String[] args) {
        loadDotEnvIfExists();
        SpringApplication.run(MmeebbChatbotApplication.class, args);
    }

    private static void loadDotEnvIfExists() {
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            try {
                Files.readAllLines(envPath).stream()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
                        .forEach(line -> {
                            int idx = line.indexOf('=');
                            String key = line.substring(0, idx).trim();
                            String value = line.substring(idx + 1).trim();
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                            }
                        });
            } catch (IOException ignored) {
                // Silenciosamente ignora se não conseguir ler
            }
        }
    }
}
