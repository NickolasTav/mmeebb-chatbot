package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StudentRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);
    private static final LocalTime TICK = LocalTime.of(12, 20);

    @Autowired
    private StudentRepository repository;

    private void save(String phone, LocalTime studyTime, LocalDate lastNotifiedOn, boolean enabled, boolean active) {
        repository.save(Student.builder()
                .phoneNumber(phone)
                .fullName("Aluno " + phone)
                .preferredStudyTime(studyTime)
                .lastReviewNotificationOn(lastNotifiedOn)
                .reviewNotificationsEnabled(enabled)
                .active(active)
                .build());
    }

    @Test
    @DisplayName("Deve incluir aluno cujo horário é exatamente o da rodada")
    void deveIncluirAlunoNoHorarioExatoDaRodada() {
        save("1", LocalTime.of(12, 20), null, true, true);

        assertThat(repository.findDueForReviewNotification(TICK, TODAY))
                .extracting(Student::getPhoneNumber).containsExactly("1");
    }

    @Test
    @DisplayName("Deve incluir na rodada das 12:20 o aluno que escolheu 12:17")
    void deveIncluirAlunoComHorarioEntreRodadas() {
        save("2", LocalTime.of(12, 17), null, true, true);

        assertThat(repository.findDueForReviewNotification(TICK, TODAY))
                .extracting(Student::getPhoneNumber).containsExactly("2");
    }

    @Test
    @DisplayName("Deve recuperar aluno avaliado ontem cujo horário já passou hoje")
    void deveRecuperarAlunoAvaliadoOntem() {
        save("3", LocalTime.of(8, 0), TODAY.minusDays(1), true, true);

        assertThat(repository.findDueForReviewNotification(TICK, TODAY))
                .extracting(Student::getPhoneNumber).containsExactly("3");
    }

    @Test
    @DisplayName("Não deve incluir aluno cujo horário ainda não chegou")
    void naoDeveIncluirAlunoAntesDoHorario() {
        save("4", LocalTime.of(12, 21), null, true, true);

        assertThat(repository.findDueForReviewNotification(TICK, TODAY)).isEmpty();
    }

    @Test
    @DisplayName("Não deve incluir aluno já avaliado hoje, evitando segundo lembrete no mesmo dia")
    void naoDeveIncluirAlunoJaAvaliadoHoje() {
        save("5", LocalTime.of(8, 0), TODAY, true, true);

        assertThat(repository.findDueForReviewNotification(TICK, TODAY)).isEmpty();
    }

    @Test
    @DisplayName("Não deve incluir aluno com lembretes pausados")
    void naoDeveIncluirAlunoComLembretesPausados() {
        save("6", LocalTime.of(8, 0), null, false, true);

        assertThat(repository.findDueForReviewNotification(TICK, TODAY)).isEmpty();
    }

    @Test
    @DisplayName("Não deve incluir estudante inativo")
    void naoDeveIncluirEstudanteInativo() {
        save("7", LocalTime.of(8, 0), null, true, false);

        assertThat(repository.findDueForReviewNotification(TICK, TODAY)).isEmpty();
    }
}
