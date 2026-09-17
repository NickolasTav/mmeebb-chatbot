package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.StudentProfileDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.service.impl.StudentSettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentSettingsServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);
    private static final LocalTime NOW = LocalTime.of(14, 0);

    @Mock private StudentRepository studentRepository;
    @Mock private StudentCourseRepository studentCourseRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleSeedingService scheduleSeedingService;

    private StudentSettingsServiceImpl service;
    private Student student;
    private Course medicina;
    private Course sistemas;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(TODAY, NOW).atZone(ZONE).toInstant(), ZONE);
        service = new StudentSettingsServiceImpl(
                studentRepository, studentCourseRepository, courseRepository, scheduleSeedingService, clock);

        student = Student.builder()
                .id(UUID.randomUUID())
                .phoneNumber("5534999998888")
                .fullName("Maria Silva Andrade")
                .ra("23000388")
                .preferredStudyTime(LocalTime.of(8, 0))
                .build();
        medicina = Course.builder().id(1L).code("MED").name("Medicina").active(true).build();
        sistemas = Course.builder().id(2L).code("SI").name("Sistemas de Informação").active(true).build();

        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(medicina));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(sistemas));
    }

    private StudentCourse link(Course course, boolean active) {
        return StudentCourse.builder()
                .id(course.getId() * 10).student(student).course(course).academicPeriod(8).active(active).build();
    }

    // ============================================================ perfil

    @Test
    @DisplayName("Deve montar o perfil com curso e período da matrícula ativa")
    void deveMontarPerfilComMatriculaAtiva() {
        student.setPreferredName("Mari");
        when(studentCourseRepository.findByStudentIdAndActiveTrue(student.getId()))
                .thenReturn(List.of(link(medicina, true)));

        StudentProfileDto profile = service.getProfile(student.getId());

        assertEquals("Maria Silva Andrade", profile.fullName());
        assertEquals("Mari", profile.preferredName());
        assertEquals("Mari", profile.displayName());
        assertEquals("23000388", profile.ra());
        assertEquals(1L, profile.courseId());
        assertEquals("Medicina", profile.courseName());
        assertEquals(8, profile.academicPeriod());
        assertEquals(LocalTime.of(8, 0), profile.preferredStudyTime());
        assertTrue(profile.reviewNotificationsEnabled());
    }

    @Test
    @DisplayName("Deve montar o perfil sem curso quando não houver matrícula ativa")
    void deveMontarPerfilSemMatricula() {
        when(studentCourseRepository.findByStudentIdAndActiveTrue(student.getId())).thenReturn(List.of());

        StudentProfileDto profile = service.getProfile(student.getId());

        assertNull(profile.courseId());
        assertNull(profile.courseName());
        assertNull(profile.academicPeriod());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException para estudante inexistente")
    void deveLancarExcecaoParaEstudanteInexistente() {
        UUID desconhecido = UUID.randomUUID();
        when(studentRepository.findById(desconhecido)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getProfile(desconhecido));
    }

    // ============================================================ apelido

    @Test
    @DisplayName("Deve salvar o apelido e removê-lo quando receber nulo")
    void deveSalvarERemoverApelido() {
        service.updatePreferredName(student.getId(), "Mari");
        assertEquals("Mari", student.getPreferredName());

        service.updatePreferredName(student.getId(), null);
        assertNull(student.getPreferredName());
        verify(studentRepository, times(2)).save(student);
    }

    // ============================================================ horário

    @Test
    @DisplayName("Horário ainda por vir hoje: lembrete sai hoje e o dia não é marcado")
    void deveManterLembreteDeHojeQuandoHorarioAindaNaoChegou() {
        boolean firesToday = service.updateStudyTime(student.getId(), LocalTime.of(18, 0));

        assertTrue(firesToday);
        assertEquals(LocalTime.of(18, 0), student.getPreferredStudyTime());
        assertNull(student.getLastReviewNotificationOn());
        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("Horário que já passou hoje: marca o dia e o lembrete só volta amanhã")
    void devePularHojeQuandoHorarioJaPassou() {
        boolean firesToday = service.updateStudyTime(student.getId(), LocalTime.of(12, 15));

        assertFalse(firesToday);
        assertEquals(TODAY, student.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Horário igual ao minuto atual conta como já passado")
    void deveTratarMinutoAtualComoJaPassado() {
        assertFalse(service.updateStudyTime(student.getId(), NOW));
        assertEquals(TODAY, student.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Aluno já avaliado hoje não recebe segundo lembrete mesmo com horário futuro")
    void naoDeveDispararSegundoLembreteNoMesmoDia() {
        student.setLastReviewNotificationOn(TODAY);

        assertFalse(service.updateStudyTime(student.getId(), LocalTime.of(20, 0)));
        assertEquals(TODAY, student.getLastReviewNotificationOn());
    }

    // ============================================================ pausa

    @Test
    @DisplayName("Deve pausar lembretes ativos sem mexer na marcação do dia")
    void devePausarLembretes() {
        assertFalse(service.toggleReviewNotifications(student.getId()));
        assertFalse(student.getReviewNotificationsEnabled());
        assertNull(student.getLastReviewNotificationOn());
        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("Reativar com horário já passado hoje: lembrete só volta amanhã")
    void deveReativarPulandoHojeQuandoHorarioJaPassou() {
        student.setReviewNotificationsEnabled(false);

        assertTrue(service.toggleReviewNotifications(student.getId()));
        assertTrue(student.getReviewNotificationsEnabled());
        assertEquals(TODAY, student.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Reativar com horário ainda por vir: lembrete sai hoje")
    void deveReativarMantendoLembreteDeHoje() {
        student.setReviewNotificationsEnabled(false);
        student.setPreferredStudyTime(LocalTime.of(19, 30));

        assertTrue(service.toggleReviewNotifications(student.getId()));
        assertNull(student.getLastReviewNotificationOn());
    }

    // ============================================================ matrícula

    @Test
    @DisplayName("Mesmo curso: atualiza só o período, sem semear nem desativar matrícula")
    void deveAtualizarApenasPeriodoNoMesmoCurso() {
        StudentCourse atual = link(medicina, true);
        when(studentCourseRepository.findByStudentIdAndActiveTrue(student.getId())).thenReturn(List.of(atual));
        when(studentCourseRepository.findByStudentIdAndCourseId(student.getId(), 1L)).thenReturn(Optional.of(atual));

        service.changeEnrollment(student.getId(), 1L, 9);

        assertEquals(9, atual.getAcademicPeriod());
        assertTrue(atual.getActive());
        verify(scheduleSeedingService, never()).seedMissingSchedules(any(), any());
    }

    @Test
    @DisplayName("Curso novo: desativa a matrícula antiga, cria a nova ativa e semeia agendamentos")
    void deveTrocarParaCursoNovo() {
        StudentCourse antiga = link(medicina, true);
        when(studentCourseRepository.findByStudentIdAndActiveTrue(student.getId())).thenReturn(List.of(antiga));
        when(studentCourseRepository.findByStudentIdAndCourseId(student.getId(), 2L)).thenReturn(Optional.empty());

        service.changeEnrollment(student.getId(), 2L, 3);

        assertFalse(antiga.getActive());
        ArgumentCaptor<StudentCourse> saved = ArgumentCaptor.forClass(StudentCourse.class);
        verify(studentCourseRepository, times(2)).save(saved.capture());
        StudentCourse criada = saved.getAllValues().get(1);
        assertEquals(sistemas, criada.getCourse());
        assertEquals(3, criada.getAcademicPeriod());
        assertTrue(criada.getActive());
        verify(scheduleSeedingService).seedMissingSchedules(student, sistemas);
    }

    @Test
    @DisplayName("Retorno a curso antigo: reativa a mesma matrícula em vez de criar outra")
    void deveReativarMatriculaAoVoltarParaCursoAntigo() {
        StudentCourse atual = link(sistemas, true);
        StudentCourse anterior = link(medicina, false);
        when(studentCourseRepository.findByStudentIdAndActiveTrue(student.getId())).thenReturn(List.of(atual));
        when(studentCourseRepository.findByStudentIdAndCourseId(student.getId(), 1L)).thenReturn(Optional.of(anterior));

        service.changeEnrollment(student.getId(), 1L, 10);

        assertFalse(atual.getActive());
        assertTrue(anterior.getActive());
        assertEquals(10, anterior.getAcademicPeriod());
        verify(studentCourseRepository).save(anterior);
        verify(scheduleSeedingService).seedMissingSchedules(student, medicina);
    }

    @Test
    @DisplayName("Curso inativo: lança ResourceNotFoundException e não altera nada")
    void deveRecusarCursoInativo() {
        sistemas.setActive(false);

        assertThrows(ResourceNotFoundException.class, () -> service.changeEnrollment(student.getId(), 2L, 3));
        verify(studentCourseRepository, never()).save(any());
        verify(scheduleSeedingService, never()).seedMissingSchedules(any(), any());
    }

    @Test
    @DisplayName("Deve recusar troca de matrícula para estudante inexistente")
    void deveRecusarTrocaParaEstudanteInexistente() {
        UUID desconhecido = UUID.randomUUID();
        when(studentRepository.findById(desconhecido)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.changeEnrollment(desconhecido, 1L, 8));
        verify(studentCourseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar matrícula ativa quando o estudante não tinha nenhuma")
    void deveCriarMatriculaQuandoNaoHaviaNenhuma() {
        when(studentCourseRepository.findByStudentIdAndActiveTrue(student.getId())).thenReturn(List.of());
        when(studentCourseRepository.findByStudentIdAndCourseId(student.getId(), 1L)).thenReturn(Optional.empty());

        service.changeEnrollment(student.getId(), 1L, 5);

        ArgumentCaptor<StudentCourse> saved = ArgumentCaptor.forClass(StudentCourse.class);
        verify(studentCourseRepository).save(saved.capture());
        assertEquals(5, saved.getValue().getAcademicPeriod());
        verify(scheduleSeedingService).seedMissingSchedules(student, medicina);
    }
}
