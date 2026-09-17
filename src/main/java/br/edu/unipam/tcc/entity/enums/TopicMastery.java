package br.edu.unipam.tcc.entity.enums;

/**
 * Nível de domínio do estudante sobre um tópico, medido pela taxa de erro histórica.
 *
 * <p>O teto de N é o mecanismo de personalização do MMEEBB adaptativo: enquanto o tópico for
 * frágil, nenhum cartão dele escapa do ciclo curto, mesmo com acertos seguidos. Como o intervalo
 * continua sendo 2^N, a base binária do método original permanece intacta — o que muda é até
 * onde o expoente pode crescer.
 *
 * <p>{@link #SEM_DADOS} e {@link #DOMINADO} usam o teto clássico, então o algoritmo se comporta
 * exatamente como o MMEEBB original para quem ainda não tem histórico.
 */
public enum TopicMastery {

    /** Amostra insuficiente na janela de análise: sem evidência, sem personalização. */
    SEM_DADOS(13, "ainda sem histórico suficiente"),

    /** Erra com frequência: o intervalo para em 2^3 = 8 dias até o desempenho melhorar. */
    FRAGIL(3, "ponto frágil"),

    /** Acerta mais do que erra, mas sem consistência: o intervalo para em 2^6 = 64 dias. */
    EM_CONSOLIDACAO(6, "em consolidação"),

    /** Acerta de forma consistente: segue o MMEEBB clássico até o teto de 2^13 dias. */
    DOMINADO(13, "dominado");

    /** Teto do MMEEBB clássico (2^13 = 8192 dias). Espelha {@code MmeebbService.MAX_N_INDEX}. */
    public static final int CLASSIC_MAX_N_INDEX = 13;

    private final int maxNIndex;
    private final String label;

    TopicMastery(int maxNIndex, String label) {
        this.maxNIndex = maxNIndex;
        this.label = label;
    }

    public int maxNIndex() {
        return maxNIndex;
    }

    public String label() {
        return label;
    }

    /**
     * Indica se este nível encurta o ciclo em relação ao MMEEBB clássico. Serve para decidir
     * quando a mensagem enviada ao aluno precisa explicar a decisão adaptativa.
     */
    public boolean limitsInterval() {
        return maxNIndex < CLASSIC_MAX_N_INDEX;
    }

    /**
     * Posição na fila de revisão do dia: quanto menor, mais cedo o cartão é apresentado.
     *
     * <p>Um tópico sem amostra vem antes de um dominado porque ainda não há prova de retenção;
     * vem depois dos frágeis porque estes já têm evidência de dificuldade. Importa para o aluno
     * exausto que responde só as primeiras questões da sessão (limite de cerca de quatro
     * <i>chunks</i> descrito por Cowan, 2001).
     */
    public int reviewPriority() {
        return switch (this) {
            case FRAGIL -> 0;
            case EM_CONSOLIDACAO -> 1;
            case SEM_DADOS -> 2;
            case DOMINADO -> 3;
        };
    }
}
