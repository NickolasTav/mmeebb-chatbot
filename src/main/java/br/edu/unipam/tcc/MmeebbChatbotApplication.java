package br.edu.unipam.tcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MmeebbChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmeebbChatbotApplication.class, args);
    }
}
