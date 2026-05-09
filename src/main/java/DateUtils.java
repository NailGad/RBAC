import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class DateUtils {

    private DateUtils() {}

    private static final DateTimeFormatter DATE_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseSensitive()
                    .appendPattern("uuuu-MM-dd")
                    .toFormatter(Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseSensitive()
                    .appendPattern("uuuu-MM-dd HH:mm")
                    .toFormatter(Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATE_TIME_SECONDS_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseSensitive()
                    .appendPattern("uuuu-MM-dd HH:mm:ss")
                    .toFormatter(Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATE_TIME_SECONDS_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        LocalDateTime d1 = parseFlexible(date1);
        LocalDateTime d2 = parseFlexible(date2);
        if (d1 == null || d2 == null) return false;
        return d1.isBefore(d2);
    }

    public static boolean isAfter(String date1, String date2) {
        LocalDateTime d1 = parseFlexible(date1);
        LocalDateTime d2 = parseFlexible(date2);
        if (d1 == null || d2 == null) return false;
        return d1.isAfter(d2);
    }

    public static String addDays(String date, int days) {
        LocalDateTime d = parseFlexible(date);
        if (d == null) {
            throw new IllegalArgumentException("Invalid date: " + date);
        }

        LocalDateTime result = d.plusDays(days);
        if (looksLikeDateOnly(date)) {
            return result.toLocalDate().format(DATE_FORMATTER);
        }
        return result.format(DATE_TIME_FORMATTER);
    }

    public static String formatRelativeTime(String date) {
        LocalDateTime d = parseFlexible(date);
        if (d == null) {
            return "unknown";
        }

        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(now.toLocalDate().atStartOfDay(), d.toLocalDate().atStartOfDay());

        if (days == 0) return "today";
        if (days > 0) return "in " + days + " days";
        return Math.abs(days) + " days ago";
    }

    private static boolean looksLikeDateOnly(String value) {
        if (value == null) return false;
        String v = value.trim();
        return v.length() == 10 && v.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    private static LocalDateTime parseFlexible(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        try {
            if (looksLikeDateOnly(v)) {
                return LocalDate.parse(v, DATE_FORMATTER).atStartOfDay();
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }

        // allow existing project format for temporary assignments: "yyyy-MM-dd HH:mm"
        if (v.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")) {
            try {
                return LocalDateTime.parse(v, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }

        // allow seconds for getCurrentDateTime() output
        if (v.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")) {
            try {
                return LocalDateTime.parse(v, DATE_TIME_SECONDS_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }

        return null;
    }
}

