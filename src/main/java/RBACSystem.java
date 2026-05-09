import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.currentUser = "system";
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void initialize() {
        Permission readUsers = new Permission("READ", "users", "Read users");
        Permission writeUsers = new Permission("WRITE", "users", "Write users");
        Permission deleteUsers = new Permission("DELETE", "users", "Delete users");
        Permission readReports = new Permission("READ", "reports", "Read reports");
        Permission writeReports = new Permission("WRITE", "reports", "Write reports");
        Permission deleteReports = new Permission("DELETE", "reports", "Delete reports");

        Role admin = new Role("Admin", "Full system access");
        admin.addPermission(readUsers);
        admin.addPermission(writeUsers);
        admin.addPermission(deleteUsers);
        admin.addPermission(readReports);
        admin.addPermission(writeReports);
        admin.addPermission(deleteReports);
        roleManager.add(admin);

        Role manager = new Role("Manager", "Can manage content");
        manager.addPermission(readUsers);
        manager.addPermission(writeUsers);
        manager.addPermission(readReports);
        manager.addPermission(writeReports);
        roleManager.add(manager);

        Role viewer = new Role("Viewer", "Read-only access");
        viewer.addPermission(readUsers);
        viewer.addPermission(readReports);
        roleManager.add(viewer);

        User adminUser = User.create("admin", "System Administrator", "admin@system.com");
        userManager.add(adminUser);

        AssignmentMetadata metadata = AssignmentMetadata.now(currentUser, "Initial admin assignment");
        PermanentAssignment assignment = new PermanentAssignment(adminUser, admin, metadata);
        assignmentManager.add(assignment);
    }

    public String generateStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SYSTEM STATISTICS ===\n");

        int userCount = userManager.count();
        int roleCount = roleManager.count();
        int assignmentCount = assignmentManager.count();

        sb.append(String.format("Users: %d\n", userCount));
        sb.append(String.format("Roles: %d\n", roleCount));
        sb.append(String.format("Assignments: %d\n", assignmentCount));

        List<RoleAssignment> activeAssignments = assignmentManager.getActiveAssignments();
        List<RoleAssignment> expiredAssignments = assignmentManager.getExpiredAssignments();
        sb.append(String.format("Active assignments: %d\n", activeAssignments.size()));
        sb.append(String.format("Expired/revoked assignments: %d\n", expiredAssignments.size()));

        if (userCount > 0) {
            long usersWithRoles = assignmentManager.findAll().stream()
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::user)
                    .distinct()
                    .count();
            double avgRolesPerUser = (double) usersWithRoles / (double) userCount;
            sb.append(String.format("Average roles per user: %.2f\n", avgRolesPerUser));
        } else {
            sb.append("Average roles per user: 0.00\n");
        }

        Map<Role, Long> rolePopularity = assignmentManager.findAll().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.groupingBy(RoleAssignment::role, Collectors.counting()));

        List<Map.Entry<Role, Long>> topRoles = rolePopularity.entrySet().stream()
                .sorted(Map.Entry.<Role, Long>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        sb.append("Top 3 roles:\n");
        if (topRoles.isEmpty()) {
            sb.append("  - No assignments\n");
        } else {
            for (Map.Entry<Role, Long> entry : topRoles) {
                sb.append(String.format("  - %s: %d users\n", entry.getKey().getName(), entry.getValue()));
            }
        }

        return sb.toString();
    }
}