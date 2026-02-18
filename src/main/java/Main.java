public class Main {
    public static void main(String[] args) {

        User user = User.create("john_doe", "John Doe", "john@example.com");
        System.out.println("User: " + user.format());

        Permission readUsers = new Permission("READ", "users", "Can read users");
        Permission writeUsers = new Permission("WRITE", "users", "Can write users");

        Role role = new Role("Editor", "Can edit content");
        role.addPermission(readUsers);
        role.addPermission(writeUsers);
        System.out.println("Role: " + role.getName());

        AssignmentMetadata metadata = AssignmentMetadata.now("admin", "Temporary access");
        System.out.println("Metadata: " + metadata.format());
        System.out.println();

        String futureDate = "2025-12-31 23:59";
        TemporaryAssignment assignment = new TemporaryAssignment(
                user, role, metadata, futureDate, true);

        System.out.println("assignmentId: " + assignment.assignmentId());
        System.out.println("type: " + assignment.assignmentType());
        System.out.println("expiresAt: " + assignment.getExpiresAt());
        System.out.println("autoRenew: " + assignment.isAutoRenew());
        System.out.println("isExpired: " + assignment.isExpired());
        System.out.println("isActive: " + assignment.isActive());
        System.out.println("timeRemaining: " + assignment.getTimeRemaining());
        System.out.println();

        System.out.println("SUMMARY:");
        System.out.println(assignment.summary());
        System.out.println();

        System.out.println("Продление назначения:");
        String newDate = "2026-12-31 23:59";
        assignment.extend(newDate);
        System.out.println("new expiresAt: " + assignment.getExpiresAt());
        System.out.println("timeRemaining: " + assignment.getTimeRemaining());
        System.out.println();

        assignment.setAutoRenew(false);
        System.out.println("autoRenew: " + assignment.isAutoRenew());
        System.out.println();

        String pastDate = "2020-01-01 00:00";
        TemporaryAssignment expiredAssignment = new TemporaryAssignment(
                user, role, metadata, pastDate, false);

        System.out.println("Истекшее назначение:");
        System.out.println("expiresAt: " + expiredAssignment.getExpiresAt());
        System.out.println("isExpired: " + expiredAssignment.isExpired());
        System.out.println("isActive: " + expiredAssignment.isActive());
        System.out.println("timeRemaining: " + expiredAssignment.getTimeRemaining());
        System.out.println(expiredAssignment.summary());
    }
}