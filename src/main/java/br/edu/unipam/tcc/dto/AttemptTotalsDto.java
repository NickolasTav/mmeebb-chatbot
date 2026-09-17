package br.edu.unipam.tcc.dto;

/**
 * Totais de tentativas e acertos em uma janela de análise.
 */
public record AttemptTotalsDto(long attempts, long correctAttempts) {

    public static AttemptTotalsDto empty() {
        return new AttemptTotalsDto(0L, 0L);
    }

    public double accuracy() {
        return attempts == 0 ? 0.0 : (double) correctAttempts / attempts;
    }
}
