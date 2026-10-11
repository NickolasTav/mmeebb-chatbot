package br.edu.unipam.tcc.exception;

import br.edu.unipam.tcc.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test-endpoint");
    }

    @Test
    @DisplayName("Smoke Test: Deve tratar ResourceNotFoundException com HTTP 404 Not Found")
    void deveTratarResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Flashcard", 123L);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Flashcard não encontrado(a) com identificador: 123", response.getBody().message());
        assertEquals("/api/test-endpoint", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Deve tratar BusinessException com HTTP 400 Bad Request")
    void deveTratarBusinessException() {
        BusinessException ex = new BusinessException("Intervalo MMEEBB não pode ser negativo");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBusinessException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Intervalo MMEEBB não pode ser negativo", response.getBody().message());
        assertEquals("/api/test-endpoint", response.getBody().path());
    }

    @Test
    @DisplayName("Deve tratar IllegalArgumentException com HTTP 400 Bad Request")
    void deveTratarIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Parâmetro inválido");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Parâmetro inválido", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException com detalhes dos campos inválidos")
    void deveTratarMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("dto", "phone", "O telefone é obrigatório");
        FieldError fieldError2 = new FieldError("dto", "text", "O texto não pode ser vazio");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation Error", response.getBody().error());
        assertNotNull(response.getBody().details());
        assertEquals(2, response.getBody().details().size());
        assertTrue(response.getBody().details().contains("O telefone é obrigatório"));
        assertTrue(response.getBody().details().contains("O texto não pode ser vazio"));
    }

    @Test
    @DisplayName("Deve tratar exceções genéricas inesperadas com HTTP 500 Internal Server Error")
    void deveTratarExcecaoGenerica() {
        Exception ex = new RuntimeException("Falha inesperada de conexão");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("Ocorreu um erro interno inesperado no servidor.", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar rota inexistente (ex.: favicon.ico) com HTTP 404, não 500")
    void deveTratarNoResourceFoundComo404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "favicon.ico");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNoResourceFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Rota não encontrada.", response.getBody().message());
    }

    @Test
    @DisplayName("Deve tratar GET em rota só-POST (ex.: / do webhook) com HTTP 405, não 500")
    void deveTratarMetodoNaoSuportadoComo405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET", List.of("POST"));

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodNotSupportedException(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, response.getBody().status());
        assertEquals("Method Not Allowed", response.getBody().error());
    }
}
