import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {

    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

   
    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        this.assignmentId = "assign_" + UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }


    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }


    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    public String summary() {
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        String type = assignmentType();
        String reason = metadata().reason();

        String reasonPart = (reason == null || reason.isBlank())
                ? ""
                : " Reason: " + reason;

        return String.format("[%s] %s assigned to %s by %s at %s%s Status: %s",
                type,
                role().getName(),
                user().username(),
                metadata().assignedBy(),
                metadata().assignedAt(),
                reasonPart,
                status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return assignmentId.equals(that.assignmentId);
    }

    @Override
    public int hashCode() {
        return assignmentId.hashCode();
    }

    @Override
    public String toString() {
        String shortId = assignmentId.length() > 8
                ? assignmentId.substring(0, 8) + "..."
                : assignmentId;
        return String.format("%s{id='%s', user=%s, role=%s}",
                getClass().getSimpleName(),
                shortId,
                user.username(),
                role.getName());
    }
}