import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class ValidationUtils {

    private ValidationUtils() {}

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseSensitive()
                    .appendPattern("uuuu-MM-dd HH:mm")
                    .toFormatter(Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String DATE_TIME_REGEX = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$";

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        String u = username.trim();
        return !u.isEmpty() && u.matches(USERNAME_REGEX);
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        if (e.isEmpty()) return false;
        if (!e.matches(EMAIL_REGEX)) return false;
        return true;
    }

    public static boolean isValidDate(String date) {
        if (date == null) return false;
        String d = date.trim();
        if (!d.matches(DATE_TIME_REGEX)) return false;
        try {
            LocalDateTime.parse(d, DATE_TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim().replaceAll("\\s+", " ");
        if (normalized.contains("@")) {
            return normalized.toLowerCase();
        }
        return normalized;
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            String name = (fieldName == null || fieldName.isBlank()) ? "value" : fieldName.trim();
            throw new IllegalArgumentException(name + " не может быть пустым");
        }
    }
}
