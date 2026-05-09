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

        ValidationUtils.requireNonEmpty(expiresAt, "expiresAt");
        expiresAt = ValidationUtils.normalizeString(expiresAt);
        if (!ValidationUtils.isValidDate(expiresAt)) {
            throw new IllegalArgumentException("expiresAt должен быть в формате yyyy-MM-dd HH:mm");
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
        ValidationUtils.requireNonEmpty(newExpirationDate, "New expiration date");
        String normalized = ValidationUtils.normalizeString(newExpirationDate);
        if (!ValidationUtils.isValidDate(normalized)) {
            throw new IllegalArgumentException("New expiration date должен быть в формате yyyy-MM-dd HH:mm");
        }
        this.expiresAt = normalized;
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