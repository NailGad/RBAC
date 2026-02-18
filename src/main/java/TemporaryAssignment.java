import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private boolean autoRenew;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);

        if (expiresAt == null || expiresAt.isBlank()) {
            throw new IllegalArgumentException("expiresAt не может быть пустым");
        }

        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }


    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
            return expiry.isBefore(LocalDateTime.now());
        } catch (Exception e) {
            return true;
        }
    }

    public void extend(String newExpirationDate) {
        if (newExpirationDate == null || newExpirationDate.isBlank()) {
            throw new IllegalArgumentException("Новая дата не может быть пустой");
        }
        this.expiresAt = newExpirationDate;
    }

    public String getTimeRemaining() {
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
            LocalDateTime now = LocalDateTime.now();

            if (expiry.isBefore(now)) {
                return "Expired";
            }

            long days = ChronoUnit.DAYS.between(now, expiry);
            long hours = ChronoUnit.HOURS.between(now, expiry) % 24;

            if (days > 0) {
                return String.format("%d days %d hours", days, hours);
            } else {
                return String.format("%d hours", hours);
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }


    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }


    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }


    @Override
    public String summary() {
        String baseSummary = super.summary();
        String autoRenewText = autoRenew ? "yes" : "no";
        String status = isExpired() ? "EXPIRED" : "ACTIVE";

        return String.format("%s\nExpires: %s (Remaining: %s) Auto-renew: %s Status: %s",
                baseSummary,
                expiresAt,
                getTimeRemaining(),
                autoRenewText,
                status);
    }
}