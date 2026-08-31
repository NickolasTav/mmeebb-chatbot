package br.edu.unipam.tcc.exception;

/**
 * Exceção lançada quando ocorre violação de uma regra de negócio da aplicação.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
