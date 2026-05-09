import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        if (userManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers cannot be null");
        }

        List<User> users = userManager.findAll();
        users.sort(Comparator.comparing(User::username, String.CASE_INSENSITIVE_ORDER));

        StringBuilder sb = new StringBuilder();
        sb.append("=== USER REPORT ===\n");
        sb.append(String.format("Total users: %d%n%n", users.size()));

        if (users.isEmpty()) {
            sb.append("No users.\n");
            return sb.toString();
        }

        for (User u : users) {
            List<RoleAssignment> assignments = assignmentManager.findByUser(u);
            List<String> activeRoles = new ArrayList<>();
            for (RoleAssignment a : assignments) {
                if (a.isActive()) {
                    activeRoles.add(a.role().getName());
                }
            }
            activeRoles.sort(String.CASE_INSENSITIVE_ORDER);

            sb.append(String.format("- %s | %s | %s%n", u.username(), u.fullName(), u.email()));
            sb.append(String.format("  Roles (%d): %s%n",
                    activeRoles.size(),
                    activeRoles.isEmpty() ? "-" : String.join(", ", activeRoles)));
        }

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        if (roleManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers cannot be null");
        }

        List<Role> roles = roleManager.findAll();
        roles.sort(Comparator.comparing(Role::getName, String.CASE_INSENSITIVE_ORDER));

        Map<String, Set<String>> activeUsersByRole = new HashMap<>();
        for (RoleAssignment a : assignmentManager.getActiveAssignments()) {
            String roleName = a.role().getName();
            activeUsersByRole.computeIfAbsent(roleName, k -> new HashSet<>())
                    .add(a.user().username());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ROLE REPORT ===\n");
        sb.append(String.format("Total roles: %d%n%n", roles.size()));

        if (roles.isEmpty()) {
            sb.append("No roles.\n");
            return sb.toString();
        }

        sb.append(String.format("%-25s %-10s %-12s%n", "Role", "Perms", "Users"));
        sb.append("-".repeat(50)).append('\n');

        for (Role r : roles) {
            int usersCount = activeUsersByRole.getOrDefault(r.getName(), Set.of()).size();
            sb.append(String.format("%-25s %-10d %-12d%n",
                    r.getName(),
                    r.getPermissions().size(),
                    usersCount));
        }

        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        if (userManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers cannot be null");
        }

        List<User> users = userManager.findAll();
        users.sort(Comparator.comparing(User::username, String.CASE_INSENSITIVE_ORDER));

        SortedSet<String> resources = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (User u : users) {
            for (Permission p : assignmentManager.getUserPermissions(u)) {
                resources.add(p.resource());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== PERMISSION MATRIX (users x resources) ===\n");
        sb.append(String.format("Users: %d | Resources: %d%n%n", users.size(), resources.size()));

        if (users.isEmpty()) {
            sb.append("No users.\n");
            return sb.toString();
        }
        if (resources.isEmpty()) {
            sb.append("No permissions/resources.\n");
            return sb.toString();
        }

        List<String> resourceList = new ArrayList<>(resources);

        sb.append(String.format("%-20s", "Username"));
        for (String res : resourceList) {
            sb.append(String.format(" | %-18s", truncate(res, 18)));
        }
        sb.append('\n');
        sb.append("-".repeat(22 + resourceList.size() * 22)).append('\n');

        for (User u : users) {
            Map<String, SortedSet<String>> permsByRes = new HashMap<>();
            for (Permission p : assignmentManager.getUserPermissions(u)) {
                permsByRes.computeIfAbsent(p.resource(), k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
                        .add(p.name());
            }

            sb.append(String.format("%-20s", u.username()));
            for (String res : resourceList) {
                SortedSet<String> perms = permsByRes.get(res);
                String cell = (perms == null || perms.isEmpty()) ? "-" : String.join("/", perms);
                sb.append(String.format(" | %-18s", truncate(cell, 18)));
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        ValidationUtils.requireNonEmpty(report, "report");
        ValidationUtils.requireNonEmpty(filename, "filename");

        try {
            Files.writeString(Path.of(filename), report);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export report to file: " + filename, ex);
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }
}

