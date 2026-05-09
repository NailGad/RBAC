import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AssignmentMetadata {
        ValidationUtils.requireNonEmpty(assignedBy, "assignedBy");
        assignedBy = ValidationUtils.normalizeString(assignedBy);

        ValidationUtils.requireNonEmpty(assignedAt, "assignedAt");
        assignedAt = ValidationUtils.normalizeString(assignedAt);
        if (!ValidationUtils.isValidDate(assignedAt)) {
            throw new IllegalArgumentException("assignedAt должен быть в формате yyyy-MM-dd HH:mm");
        }

        if (reason == null) {
            reason = "";
        }
        reason = ValidationUtils.normalizeString(reason);
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String now = LocalDateTime.now().format(FORMATTER);
        return new AssignmentMetadata(assignedBy, now, reason);
    }

    public String format() {
        if (reason == null || reason.isBlank()) {
            return String.format("Assigned by %s at %s", assignedBy, assignedAt);
        } else {
            return String.format("Assigned by %s at %s. Reason: %s",
                    assignedBy, assignedAt, reason);
        }
    }
}