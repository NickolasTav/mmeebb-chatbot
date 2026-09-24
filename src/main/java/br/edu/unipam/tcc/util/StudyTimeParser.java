package br.edu.unipam.tcc.util;

import java.time.LocalTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta o horário do lembrete digitado livremente no WhatsApp (formato 24h).
 * Aceita "12:15", "12.15", "12h15", "12h15min", "12h" e "12", com ou sem espaços.
 * Minutos exigem dois dígitos: "7:5" e "1215" são ambíguos demais para adivinhar.
 */
public final class StudyTimeParser {

    private static final Pattern TIME_PATTERN =
            Pattern.compile("^(\\d{1,2})(?:[:.](\\d{2})|h(\\d{2})?(?:min)?)?$");

    private StudyTimeParser() {
    }

    public static Optional<LocalTime> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }

        Matcher matcher = TIME_PATTERN.matcher(input.replaceAll("\\s+", "").toLowerCase());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int hour = Integer.parseInt(matcher.group(1));
        String minuteGroup = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
        int minute = minuteGroup != null ? Integer.parseInt(minuteGroup) : 0;

        if (hour > 23 || minute > 59) {
            return Optional.empty();
        }
        return Optional.of(LocalTime.of(hour, minute));
    }
}
