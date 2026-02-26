import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class AssignmentSorters {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Comparator<RoleAssignment> byUsername() {
        return (a1, a2) -> a1.user().username()
                .compareToIgnoreCase(a2.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return (a1, a2) -> a1.role().getName()
                .compareToIgnoreCase(a2.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return (a1, a2) -> {
            try {
                LocalDateTime date1 = LocalDateTime.parse(
                        a1.metadata().assignedAt(), FORMATTER);
                LocalDateTime date2 = LocalDateTime.parse(
                        a2.metadata().assignedAt(), FORMATTER);
                return date1.compareTo(date2);
            } catch (Exception e) {
                return 0;
            }
        };
    }
}