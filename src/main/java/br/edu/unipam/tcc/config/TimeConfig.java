package br.edu.unipam.tcc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio único da aplicação. Horários de lembrete e datas de revisão seguem o calendário
 * do aluno (Brasília); o fuso da JVM em contêiner costuma ser UTC, o que faria o "hoje"
 * virar o dia seguinte a partir das 21h e adiantar revisões e notificações.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(APP_ZONE);
    }
}
