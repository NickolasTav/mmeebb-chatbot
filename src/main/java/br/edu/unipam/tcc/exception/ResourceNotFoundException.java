package br.edu.unipam.tcc.exception;

/**
 * Exceção lançada quando um recurso ou entidade solicitada não é encontrada no banco de dados.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s não encontrado(a) com identificador: %s", resourceName, identifier));
    }
}
