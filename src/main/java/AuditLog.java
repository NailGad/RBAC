import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final BlockingQueue<Optional<AuditEntry>> inbound = new LinkedBlockingQueue<>();
    private final AtomicInteger enqueued = new AtomicInteger();
    private final AtomicInteger stored = new AtomicInteger();
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final Thread worker;

    public AuditLog() {
        worker = new Thread(this::consumeLoop, "audit-log-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void consumeLoop() {
        try {
            while (true) {
                Optional<AuditEntry> next = inbound.take();
                if (next.isEmpty()) {
                    break;
                }
                AuditEntry e = next.get();
                synchronized (entries) {
                    entries.add(e);
                }
                stored.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonEmpty(action, "action");
        ValidationUtils.requireNonEmpty(performer, "performer");
        ValidationUtils.requireNonEmpty(target, "target");

        if (shutdown.get()) {
            throw new IllegalStateException("AuditLog is shut down");
        }

        String ts = LocalDateTime.now().format(TS_FORMATTER);
        String normalizedAction = ValidationUtils.normalizeString(action).toUpperCase();
        String normalizedPerformer = ValidationUtils.normalizeString(performer);
        String normalizedTarget = ValidationUtils.normalizeString(target);
        String normalizedDetails = details == null ? "" : Objects.requireNonNullElse(ValidationUtils.normalizeString(details), "");

        AuditEntry entry = new AuditEntry(ts, normalizedAction, normalizedPerformer, normalizedTarget, normalizedDetails);
        try {
            inbound.put(Optional.of(entry));
            enqueued.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while enqueueing audit entry", e);
        }
    }

    /**
     * Дождаться, пока все записанные на момент вызова события попадут в список (для тестов).
     */
    public void awaitProcessed() throws InterruptedException {
        int target = enqueued.get();
        long deadline = System.currentTimeMillis() + 10_000;
        while (stored.get() < target && System.currentTimeMillis() < deadline) {
            Thread.sleep(1);
        }
        if (stored.get() < target) {
            throw new IllegalStateException("Timeout waiting for audit entries to be processed");
        }
    }

    public void shutdownAndAwait() throws InterruptedException {
        if (shutdown.compareAndSet(false, true)) {
            inbound.put(Optional.empty());
            worker.join(5000);
        }
    }

    public List<AuditEntry> getAll() {
        synchronized (entries) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null || performer.isBlank()) {
            return List.of();
        }
        String p = ValidationUtils.normalizeString(performer);

        synchronized (entries) {
            List<AuditEntry> result = new ArrayList<>();
            for (AuditEntry e : entries) {
                if (e.performer().equalsIgnoreCase(p)) {
                    result.add(e);
                }
            }
            return result;
        }
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null || action.isBlank()) {
            return List.of();
        }
        String a = ValidationUtils.normalizeString(action).toUpperCase();

        synchronized (entries) {
            List<AuditEntry> result = new ArrayList<>();
            for (AuditEntry e : entries) {
                if (e.action().equalsIgnoreCase(a)) {
                    result.add(e);
                }
            }
            return result;
        }
    }

    public void printLog() {
        List<AuditEntry> snapshot = getAll();
        if (snapshot.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }

        System.out.println("\n=== AUDIT LOG ===");
        for (AuditEntry e : snapshot) {
            String details = (e.details() == null || e.details().isBlank()) ? "" : " | " + e.details();
            System.out.printf("[%s] %s | performer=%s | target=%s%s%n",
                    e.timestamp(), e.action(), e.performer(), e.target(), details);
        }
        System.out.println();
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "filename");

        List<AuditEntry> snapshot = getAll();

        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,action,performer,target,details\n");
        for (AuditEntry e : snapshot) {
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
