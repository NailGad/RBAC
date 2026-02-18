public class Main {
    public static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ PERMANENTASSIGNMENT\n");

        User user = User.create("john_doe", "John Doe", "john@example.com");
        System.out.println("User: " + user.format());

        Permission readUsers = new Permission("READ", "users", "Can read users");
        Permission writeUsers = new Permission("WRITE", "users", "Can write users");

        Role role = new Role("Editor", "Can edit content");
        role.addPermission(readUsers);
        role.addPermission(writeUsers);
        System.out.println("Role: " + role.getName());

        AssignmentMetadata metadata = AssignmentMetadata.now("admin", "Permanent assignment");
        System.out.println("Metadata: " + metadata.format());
        System.out.println();

        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        System.out.println("assignmentId: " + assignment.assignmentId());
        System.out.println("user: " + assignment.user().username());
        System.out.println("role: " + assignment.role().getName());
        System.out.println("type: " + assignment.assignmentType());
        System.out.println("isActive: " + assignment.isActive());
        System.out.println("isRevoked: " + assignment.isRevoked());
        System.out.println();

        System.out.println("Summary: " + assignment.summary());
        System.out.println();

        System.out.println("Отзыв назначения:");
        assignment.revoke();
        System.out.println("isActive: " + assignment.isActive());
        System.out.println("isRevoked: " + assignment.isRevoked());
        System.out.println("Summary: " + assignment.summary());
    }
}