import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AssignmentMetadata {
        if (assignedBy == null || assignedBy.isBlank()) {
            throw new IllegalArgumentException("assignedBy не может быть пустым");
        }

        if (assignedAt == null || assignedAt.isBlank()) {
            throw new IllegalArgumentException("assignedAt не может быть пустым");
        }

        if (reason == null) {
            reason = "";
        }
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