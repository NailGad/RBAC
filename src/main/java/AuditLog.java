import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {}

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<AuditEntry> entries = new ArrayList<>();

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonEmpty(action, "action");
        ValidationUtils.requireNonEmpty(performer, "performer");
        ValidationUtils.requireNonEmpty(target, "target");

        String ts = LocalDateTime.now().format(TS_FORMATTER);
        String normalizedAction = ValidationUtils.normalizeString(action).toUpperCase();
        String normalizedPerformer = ValidationUtils.normalizeString(performer);
        String normalizedTarget = ValidationUtils.normalizeString(target);
        String normalizedDetails = details == null ? "" : Objects.requireNonNullElse(ValidationUtils.normalizeString(details), "");

        entries.add(new AuditEntry(ts, normalizedAction, normalizedPerformer, normalizedTarget, normalizedDetails));
    }

    public List<AuditEntry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null || performer.isBlank()) {
            return List.of();
        }
        String p = ValidationUtils.normalizeString(performer);

        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry e : entries) {
            if (e.performer().equalsIgnoreCase(p)) {
                result.add(e);
            }
        }
        return result;
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null || action.isBlank()) {
            return List.of();
        }
        String a = ValidationUtils.normalizeString(action).toUpperCase();

        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry e : entries) {
            if (e.action().equalsIgnoreCase(a)) {
                result.add(e);
            }
        }
        return result;
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }

        System.out.println("\n=== AUDIT LOG ===");
        for (AuditEntry e : entries) {
            String details = (e.details() == null || e.details().isBlank()) ? "" : " | " + e.details();
            System.out.printf("[%s] %s | performer=%s | target=%s%s%n",
                    e.timestamp(), e.action(), e.performer(), e.target(), details);
        }
        System.out.println();
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "filename");

        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,action,performer,target,details\n");
        for (AuditEntry e : entries) {
            sb.append(csv(e.timestamp())).append(',')
                    .append(csv(e.action())).append(',')
                    .append(csv(e.performer())).append(',')
                    .append(csv(e.target())).append(',')
                    .append(csv(e.details()))
                    .append('\n');
        }

        try {
            Files.writeString(Path.of(filename), sb.toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save audit log to file: " + filename, ex);
        }
    }

    private static String csv(String value) {
        String v = value == null ? "" : value;
        boolean needsQuoting = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        if (!needsQuoting) return v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}

