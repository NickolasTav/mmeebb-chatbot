package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.config.AdaptiveProperties;
import br.edu.unipam.tcc.config.TimeConfig;
import br.edu.unipam.tcc.dto.AttemptTotalsDto;
import br.edu.unipam.tcc.dto.TopicAttemptAggregateDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.repository.ReviewAttemptRepository;
import br.edu.unipam.tcc.service.impl.PerformanceAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerformanceAnalysisServiceImplTest {

    private static final UUID ALUNO = UUID.randomUUID();
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 17, 10, 0);
    private static final Long FARMACO = 4L;

    private ReviewAttemptRepository reviewAttemptRepository;
    private PerformanceAnalysisService service;

    @BeforeEach
    void setUp() {
        reviewAttemptRepository = mock(ReviewAttemptRepository.class);
        service = newService(defaultProperties());
    }

    private PerformanceAnalysisService newService(AdaptiveProperties properties) {
        Clock clock = Clock.fixed(AGORA.atZone(TimeConfig.APP_ZONE).toInstant(), TimeConfig.APP_ZONE);
        return new PerformanceAnalysisServiceImpl(reviewAttemptRepository, properties, clock);
    }

    private AdaptiveProperties defaultProperties() {
        return new AdaptiveProperties(true, 90, 3, 0.50, 0.20, 2, 5);
    }

    private void givenAggregates(TopicAttemptAggregateDto... aggregates) {
        when(reviewAttemptRepository.aggregateByTopic(eq(ALUNO), any())).thenReturn(List.of(aggregates));
    }

    private TopicAttemptAggregateDto aggregate(String topic, long attempts, long errors) {
        return new TopicAttemptAggregateDto(FARMACO, "Farmacologia Clinica", topic, attempts, errors);
    }

    @Test
    @DisplayName("Smoke Test: deve devolver SEM_DADOS quando o estudante não tem histórico")
    void deveDevolverSemDadosQuandoNaoHaHistorico() {
        givenAggregates();

        TopicPerformanceDto desempenho = service.analyzeTopic(ALUNO, FARMACO, "Antibioticoterapia");

        assertThat(desempenho.mastery()).isEqualTo(TopicMastery.SEM_DADOS);
        assertThat(desempenho.attempts()).isZero();
        assertThat(desempenho.topic()).isEqualTo("Antibioticoterapia");
    }

    @Test
    @DisplayName("Deve classificar como FRÁGIL a partir de 50% de erro")
    void deveClassificarComoFragilAPartirDeMetadeDeErro() {
        givenAggregates(aggregate("Antibioticoterapia", 10, 5));

        TopicPerformanceDto desempenho = service.analyzeTopic(ALUNO, FARMACO, "Antibioticoterapia");

        assertThat(desempenho.mastery()).isEqualTo(TopicMastery.FRAGIL);
        assertThat(desempenho.errorRate()).isEqualTo(0.5);
        assertThat(desempenho.maxIntervalDays()).isEqualTo(8);
    }

    @Test
    @DisplayName("Deve classificar como EM_CONSOLIDACAO na fronteira de 20% de erro")
    void deveClassificarComoEmConsolidacaoNaFronteiraDeVintePorCento() {
        givenAggregates(aggregate("Antibioticoterapia", 10, 2));

        TopicPerformanceDto desempenho = service.analyzeTopic(ALUNO, FARMACO, "Antibioticoterapia");

        assertThat(desempenho.mastery()).isEqualTo(TopicMastery.EM_CONSOLIDACAO);
        assertThat(desempenho.maxIntervalDays()).isEqualTo(64);
    }

    @Test
    @DisplayName("Deve classificar como DOMINADO abaixo de 20% de erro")
    void deveClassificarComoDominadoAbaixoDeVintePorCento() {
        givenAggregates(aggregate("Antibioticoterapia", 10, 1));

        TopicPerformanceDto desempenho = service.analyzeTopic(ALUNO, FARMACO, "Antibioticoterapia");

        assertThat(desempenho.mastery()).isEqualTo(TopicMastery.DOMINADO);
        assertThat(desempenho.maxIntervalDays()).isEqualTo(8192);
        assertThat(desempenho.correctAttempts()).isEqualTo(9);
    }

    @Test
    @DisplayName("Deve exigir amostra mínima antes de personalizar: 2 erros em 2 tentativas ainda é SEM_DADOS")
    void deveExigirAmostraMinimaAntesDePersonalizar() {
        givenAggregates(aggregate("Antibioticoterapia", 2, 2));

        TopicPerformanceDto desempenho = service.analyzeTopic(ALUNO, FARMACO, "Antibioticoterapia");

        assertThat(desempenho.mastery()).isEqualTo(TopicMastery.SEM_DADOS);
        assertThat(desempenho.errorRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Deve consultar o banco usando a janela configurada a partir do relógio da aplicação")
    void deveConsultarUsandoAJanelaConfigurada() {
        givenAggregates();

        service.analyzeTopics(ALUNO);

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reviewAttemptRepository).aggregateByTopic(eq(ALUNO), since.capture());
        assertThat(since.getValue()).isEqualTo(AGORA.minusDays(90));
    }

    @Test
    @DisplayName("Deve ordenar os tópicos do mais frágil ao mais dominado")
    void deveOrdenarDoMaisFragilAoMaisDominado() {
        givenAggregates(
                aggregate("Dominado", 10, 0),
                aggregate("Consolidando", 10, 3),
                aggregate("Fragil moderado", 10, 6),
                aggregate("Fragil grave", 10, 9),
                aggregate("Sem amostra", 2, 1)
        );

        List<TopicPerformanceDto> topicos = service.analyzeTopics(ALUNO);

        assertThat(topicos).extracting(TopicPerformanceDto::topic)
                .containsExactly("Fragil grave", "Fragil moderado", "Consolidando", "Sem amostra", "Dominado");
    }

    @Test
    @DisplayName("Com a camada adaptativa desligada, nenhum tópico é classificado")
    void naoDeveClassificarComACamadaAdaptativaDesligada() {
        service = newService(new AdaptiveProperties(false, 90, 3, 0.50, 0.20, 2, 5));
        givenAggregates(aggregate("Antibioticoterapia", 10, 9));

        TopicPerformanceDto desempenho = service.analyzeTopic(ALUNO, FARMACO, "Antibioticoterapia");

        assertThat(desempenho.mastery()).isEqualTo(TopicMastery.SEM_DADOS);
        assertThat(desempenho.errorRate()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("Deve delegar os totais do estudante respeitando a janela")
    void deveDelegarOsTotaisDoEstudante() {
        when(reviewAttemptRepository.totalsByStudent(eq(ALUNO), any()))
                .thenReturn(new AttemptTotalsDto(20L, 13L));

        AttemptTotalsDto totais = service.totals(ALUNO);

        assertThat(totais.attempts()).isEqualTo(20);
        assertThat(totais.accuracy()).isEqualTo(0.65);
        verify(reviewAttemptRepository, never()).aggregateBySubject(any());
    }

    @Test
    @DisplayName("Deve expor a janela de análise usada, para a mensagem exibida ao estudante")
    void deveExporAJanelaDeAnalise() {
        assertThat(service.windowDays()).isEqualTo(90);
    }
}
