package br.edu.unipam.tcc.dto;

import br.edu.unipam.tcc.entity.RepetitionSchedule;

import java.util.List;

/**
 * A fila de questões do dia já cortada no limite diário do estudante.
 *
 * @param queue         questões na ordem de apresentação (vazia no resumo usado pelo lembrete)
 * @param reviews       revisões vencidas que cabem no limite de hoje
 * @param newQuestions  questões nunca respondidas que completam o que sobrou do limite
 * @param dailyLimit    limite vigente (o do estudante ou o padrão global)
 * @param answeredToday questões já respondidas hoje (dúvidas não contam)
 * @param beyondLimit   questões vencidas que ficaram fora do limite e seguem para os próximos dias
 */
public record DailyReviewPlanDto(
        List<RepetitionSchedule> queue,
        int reviews,
        int newQuestions,
        int dailyLimit,
        long answeredToday,
        long beyondLimit
) {

    public int total() {
        return reviews + newQuestions;
    }

    /** Tudo o que está vencido hoje, dentro e fora do limite. */
    public long dueTotal() {
        return total() + beyondLimit;
    }

    /**
     * A cota do dia acabou mas ainda há questões vencidas: a meta de hoje foi cumprida, e não que
     * o estudante esteja em dia com o banco.
     */
    public boolean goalReached() {
        return total() == 0 && beyondLimit > 0;
    }

    public String breakdown() {
        return breakdown(reviews, newQuestions);
    }

    /**
     * Detalhamento da fila no formato das mensagens: {@code 12 revisões + 8 novas}. O lado vazio é
     * omitido ({@code 8 novas}), e sem nenhuma questão o texto é vazio.
     */
    public static String breakdown(long reviews, long newQuestions) {
        String reviewsLabel = reviews + (reviews == 1 ? " revisão" : " revisões");
        String newLabel = newQuestions + (newQuestions == 1 ? " nova" : " novas");

        if (reviews > 0 && newQuestions > 0) {
            return reviewsLabel + " + " + newLabel;
        }
        if (reviews > 0) {
            return reviewsLabel;
        }
        return newQuestions > 0 ? newLabel : "";
    }
}
