package br.edu.unipam.tcc.messaging;

import java.util.Locale;

/**
 * Estratégia anti-banimento da fila de saída ({@code uazapi.anti-ban.mode}).
 * <ul>
 *   <li>{@link #GLOBAL} (padrão): comportamento original — um único consumidor dorme o jitter e o
 *       composing de cada mensagem, então alunos diferentes esperam uns pelos outros.</li>
 *   <li>{@link #PER_RECIPIENT}: cada destinatário mantém o mesmo jitter + composing de hoje, mas alunos
 *       diferentes não somam esperas; o único acoplamento entre eles é o teto global
 *       ({@code uazapi.anti-ban.global-min-interval-ms}) entre dois textos quaisquer.</li>
 * </ul>
 */
public enum AntiBanMode {
    GLOBAL("global"),
    PER_RECIPIENT("per-recipient");

    private final String propertyValue;

    AntiBanMode(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String propertyValue() {
        return propertyValue;
    }

    /**
     * Converte o valor da propriedade. Vazio mantém o padrão; valor desconhecido falha na subida,
     * para que um erro de digitação não troque a estratégia anti-ban em silêncio.
     */
    public static AntiBanMode from(String value) {
        if (value == null || value.isBlank()) {
            return GLOBAL;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AntiBanMode mode : values()) {
            if (mode.propertyValue.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("uazapi.anti-ban.mode invalido: '" + value
                + "'. Valores aceitos: global, per-recipient");
    }
}
