import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    private final CommandParser parser;
    private final RBACSystem system;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CommandRegistry(RBACSystem system) {
        this.system = system;
        this.parser = new CommandParser();
        registerAllCommands();
    }

    private void registerAllCommands() {
        registerUserCommands();
        registerRoleCommands();
        registerAssignmentCommands();
        registerPermissionCommands();
        registerUtilityCommands();
    }

    private void registerUserCommands() {
        parser.registerCommand("user-list", "List all users", (scanner, sys) -> {
            List<User> users = sys.getUserManager().findAll();
            if (users.isEmpty()) {
                System.out.println("No users found.");
                return;
            }
            List<String[]> rows = new ArrayList<>();
            for (User u : users) {
                rows.add(new String[]{u.username(), u.fullName(), u.email()});
            }
            System.out.println();
            System.out.println(FormatUtils.formatHeader("Users"));
            System.out.println(FormatUtils.formatTable(
                    new String[]{"Username", "Full Name", "Email"},
                    rows
            ));
            System.out.println();
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);
            String fullName = ConsoleUtils.promptString(scanner, "Enter full name: ", true);
            String email = ConsoleUtils.promptString(scanner, "Enter email: ", true);

            try {
                User user = User.create(username, fullName, email);
                sys.getUserManager().add(user);
                sys.getAuditLog().log("USER_CREATE", sys.getCurrentUser(), user.username(),
                        "email=" + user.email());
                System.out.println("User created successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user details", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            User user = userOpt.get();
            System.out.println();
            System.out.println(FormatUtils.formatHeader("User Details"));
            System.out.println(FormatUtils.formatBox(
                    "Username: " + user.username() + "\n" +
                            "Full Name: " + user.fullName() + "\n" +
                            "Email: " + user.email()
            ));

            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
            System.out.println("\nAssigned Roles:");
            if (assignments.isEmpty()) {
                System.out.println("  No roles assigned.");
            } else {
                for (RoleAssignment a : assignments) {
                    String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                    System.out.printf("  - %s [%s] - %s\n", a.role().getName(), a.assignmentType(), status);
                }
            }

            System.out.println("\nPermissions:");
            Set<Permission> permissions = sys.getAssignmentManager().getUserPermissions(user);
            if (permissions.isEmpty()) {
                System.out.println("  No permissions.");
            } else {
                permissions.stream()
                        .collect(Collectors.groupingBy(Permission::resource))
                        .forEach((resource, perms) -> {
                            System.out.println("  " + resource + ":");
                            perms.forEach(p -> System.out.println("    - " + p.name()));
                        });
            }
            System.out.println();
        });

        parser.registerCommand("user-update", "Update user data", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            if (!sys.getUserManager().exists(username)) {
                System.out.println("User not found.");
                return;
            }

            String fullName = ConsoleUtils.promptString(scanner, "Enter new full name (leave empty to keep current): ", false);
            String email = ConsoleUtils.promptString(scanner, "Enter new email (leave empty to keep current): ", false);

            try {
                sys.getUserManager().update(username, fullName.isEmpty() ? null : fullName, email.isEmpty() ? null : email);
                System.out.println("User updated successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            User user = userOpt.get();
            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
            if (!assignments.isEmpty()) {
                System.out.println("User has " + assignments.size() + " assignments. They will be removed.");
            }

            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Confirm deletion (yes/no): ");
            if (!confirm) {
                System.out.println("Deletion cancelled.");
                return;
            }

            for (RoleAssignment a : assignments) {
                sys.getAssignmentManager().remove(a);
            }
            sys.getUserManager().remove(user);
            sys.getAuditLog().log("USER_DELETE", sys.getCurrentUser(), user.username(),
                    "removedAssignments=" + assignments.size());
            System.out.println("User deleted successfully.");
        });

        parser.registerCommand("user-search", "Search users by filters", (scanner, sys) -> {
            System.out.println("\nSearch filters:");
            System.out.println("1. By username contains");
            System.out.println("2. By email contains");
            System.out.println("3. By email domain");
            System.out.println("4. By full name contains");
            int choice = ConsoleUtils.promptInt(scanner, "Choose filter (1-4): ", 1, 4);
            UserFilter filter = null;

            switch (choice) {
                case 1:
                    filter = UserFilters.byUsernameContains(
                            ConsoleUtils.promptString(scanner, "Enter username substring: ", true));
                    break;
                case 2:
                    filter = UserFilters.byEmailContains(
                            ConsoleUtils.promptString(scanner, "Enter email substring: ", true));
                    break;
                case 3:
                    filter = UserFilters.byEmailDomain(
                            ConsoleUtils.promptString(scanner, "Enter email domain (e.g., @example.com): ", true));
                    break;
                case 4:
                    filter = UserFilters.byFullNameContains(
                            ConsoleUtils.promptString(scanner, "Enter full name substring: ", true));
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }

            List<User> results = sys.getUserManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("No users found.");
            } else {
                System.out.println("\nFound " + results.size() + " user(s):");
                for (User u : results) {
                    System.out.println("  " + u.username() + " (" + u.fullName() + ") - " + u.email());
                }
            }
            System.out.println();
        });
    }

    private void registerRoleCommands() {
        parser.registerCommand("role-list", "List all roles", (scanner, sys) -> {
            List<Role> roles = sys.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("No roles found.");
                return;
            }
            List<String[]> rows = new ArrayList<>();
            for (Role r : roles) {
                rows.add(new String[]{
                        r.getName(),
                        String.valueOf(r.getPermissions().size()),
                        r.getDescription()
                });
            }
            System.out.println();
            System.out.println(FormatUtils.formatHeader("Roles"));
            System.out.println(FormatUtils.formatTable(
                    new String[]{"Name", "Permissions", "Description"},
                    rows
            ));
            System.out.println();
        });

        parser.registerCommand("role-create", "Create a new role", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name: ", true);
            String description = ConsoleUtils.promptString(scanner, "Enter role description: ", true);

            try {
                Role role = new Role(name, description);
                sys.getRoleManager().add(role);
                sys.getAuditLog().log("ROLE_CREATE", sys.getCurrentUser(), role.getName(), role.getDescription());
                System.out.println("Role created successfully.");

                boolean addNow = ConsoleUtils.promptYesNo(scanner, "Do you want to add permissions now? (y/n): ");
                if (addNow) {
                    while (true) {
                        String permName = ConsoleUtils.promptString(scanner, "Enter permission name (or 'done' to finish): ", true);
                        if ("done".equalsIgnoreCase(permName)) break;

                        String resource = ConsoleUtils.promptString(scanner, "Enter resource: ", true);
                        String permDesc = ConsoleUtils.promptString(scanner, "Enter description: ", true);

                        try {
                            Permission perm = new Permission(permName, resource, permDesc);
                            role.addPermission(perm);
                            System.out.println("Permission added.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            Optional<Role> roleOpt = sys.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            System.out.println(roleOpt.get().format());
        });

        parser.registerCommand("role-update", "Update role name/description", (scanner, sys) -> {
            String oldName = ConsoleUtils.promptString(scanner, "Enter role name to update: ", true);

            Optional<Role> roleOpt = sys.getRoleManager().findByName(oldName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            Role role = roleOpt.get();
            String newName = ConsoleUtils.promptString(scanner, "Enter new name (leave empty to keep current): ", false);
            String newDesc = ConsoleUtils.promptString(scanner, "Enter new description (leave empty to keep current): ", false);

            sys.getRoleManager().remove(role);
            Role updatedRole = new Role(newName.isEmpty() ? role.getName() : newName,
                    newDesc.isEmpty() ? role.getDescription() : newDesc);
            for (Permission p : role.getPermissions()) {
                updatedRole.addPermission(p);
            }
            sys.getRoleManager().add(updatedRole);
            System.out.println("Role updated successfully.");
        });

        parser.registerCommand("role-delete", "Delete a role", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            Optional<Role> roleOpt = sys.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            Role role = roleOpt.get();
            List<RoleAssignment> assignments = sys.getAssignmentManager().findByRole(role);
            if (!assignments.isEmpty()) {
                System.out.println("Role is assigned to " + assignments.size() + " user(s):");
                assignments.stream().map(RoleAssignment::user).forEach(u -> System.out.println("  - " + u.username()));
                boolean confirm = ConsoleUtils.promptYesNo(scanner, "Confirm deletion (yes/no): ");
                if (!confirm) {
                    System.out.println("Deletion cancelled.");
                    return;
                }
                for (RoleAssignment a : assignments) {
                    sys.getAssignmentManager().remove(a);
                }
            }

            sys.getRoleManager().remove(role);
            sys.getAuditLog().log("ROLE_DELETE", sys.getCurrentUser(), role.getName(),
                    "removedAssignments=" + assignments.size());
            System.out.println("Role deleted successfully.");
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, sys) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            String permName = ConsoleUtils.promptString(scanner, "Enter permission name: ", true);
            String resource = ConsoleUtils.promptString(scanner, "Enter resource: ", true);
            String description = ConsoleUtils.promptString(scanner, "Enter description: ", true);

            try {
                Permission perm = new Permission(permName, resource, description);
                sys.getRoleManager().addPermissionToRole(roleName, perm);
                System.out.println("Permission added to role.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, sys) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            Role role = roleOpt.get();
            Set<Permission> permissions = role.getPermissions();
            if (permissions.isEmpty()) {
                System.out.println("Role has no permissions.");
                return;
            }

            System.out.println("\nPermissions:");
            List<Permission> permList = new ArrayList<>(permissions);
            Permission toRemove = ConsoleUtils.promptChoice(scanner, "Choose permission to remove:", permList);
            sys.getRoleManager().removePermissionFromRole(roleName, toRemove);
            System.out.println("Permission removed successfully.");
        });

        parser.registerCommand("role-search", "Search roles", (scanner, sys) -> {
            System.out.println("\nSearch filters:");
            System.out.println("1. By name contains");
            System.out.println("2. Has specific permission");
            System.out.println("3. Has at least N permissions");
            int choice = ConsoleUtils.promptInt(scanner, "Choose filter (1-3): ", 1, 3);
            RoleFilter filter = null;

            switch (choice) {
                case 1:
                    filter = RoleFilters.byNameContains(
                            ConsoleUtils.promptString(scanner, "Enter name substring: ", true));
                    break;
                case 2:
                    String permName = ConsoleUtils.promptString(scanner, "Enter permission name: ", true);
                    String resource = ConsoleUtils.promptString(scanner, "Enter resource: ", true);
                    filter = RoleFilters.hasPermission(permName, resource);
                    break;
                case 3:
                    int n = ConsoleUtils.promptInt(scanner, "Enter minimum number of permissions: ", 0, Integer.MAX_VALUE);
                    filter = RoleFilters.hasAtLeastNPermissions(n);
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }

            List<Role> results = sys.getRoleManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("No roles found.");
            } else {
                System.out.println("\nFound " + results.size() + " role(s):");
                for (Role r : results) {
                    System.out.println("  " + r.getName() + " - " + r.getDescription());
                }
            }
            System.out.println();
        });
    }

    private void registerAssignmentCommands() {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();

            List<Role> roles = sys.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("No roles available.");
                return;
            }

            Role role = ConsoleUtils.promptChoice(scanner, "Available roles:", roles);
            String type = ConsoleUtils.promptString(scanner, "Assignment type (permanent/temporary): ", true).toLowerCase();
            String reason = ConsoleUtils.promptString(scanner, "Reason for assignment: ", false);

            AssignmentMetadata metadata = AssignmentMetadata.now(sys.getCurrentUser(), reason);

            try {
                if ("permanent".equals(type)) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    sys.getAssignmentManager().add(assignment);
                    sys.getAuditLog().log("ROLE_ASSIGN", sys.getCurrentUser(),
                            user.username() + " -> " + role.getName(),
                            "type=PERMANENT id=" + assignment.assignmentId());
                    System.out.println("Permanent assignment created.");
                } else if ("temporary".equals(type)) {
                    String expiresAt = ConsoleUtils.promptString(scanner, "Expiration date (yyyy-MM-dd HH:mm): ", true);
                    boolean autoRenew = ConsoleUtils.promptYesNo(scanner, "Auto-renew? (y/n): ");

                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    sys.getAssignmentManager().add(assignment);
                    sys.getAuditLog().log("ROLE_ASSIGN", sys.getCurrentUser(),
                            user.username() + " -> " + role.getName(),
                            "type=TEMPORARY id=" + assignment.assignmentId() + " expiresAt=" + assignment.getExpiresAt());
                    System.out.println("Temporary assignment created.");
                } else {
                    System.out.println("Invalid type.");
                }
            } catch (IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();

            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());

            if (assignments.isEmpty()) {
                System.out.println("No active assignments for this user.");
                return;
            }

            RoleAssignment a = ConsoleUtils.promptChoice(scanner, "Active assignments:", assignments);
            sys.getAssignmentManager().revokeAssignment(a.assignmentId());
            sys.getAuditLog().log("ROLE_REVOKE", sys.getCurrentUser(),
                    a.user().username() + " -> " + a.role().getName(),
                    "id=" + a.assignmentId() + " type=" + a.assignmentType());
            System.out.println("Assignment revoked.");
        });

        parser.registerCommand("assignment-list-user", "List user assignments", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(userOpt.get());
            if (assignments.isEmpty()) {
                System.out.println("No assignments for this user.");
                return;
            }

            System.out.println("\n=== Assignments for " + username + " ===");
            System.out.printf("%-30s %-15s %-15s %-20s\n", "Role", "Type", "Status", "Assigned At");
            System.out.println("-".repeat(85));
            for (RoleAssignment a : assignments) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("%-30s %-15s %-15s %-20s\n",
                        a.role().getName(),
                        a.assignmentType(),
                        status,
                        a.metadata().assignedAt());
            }
            System.out.println();
        });

        parser.registerCommand("assignment-list-role", "List users with role", (scanner, sys) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            List<RoleAssignment> assignments = sys.getAssignmentManager().findByRole(roleOpt.get());
            if (assignments.isEmpty()) {
                System.out.println("No users assigned to this role.");
                return;
            }

            System.out.println("\n=== Users with role " + roleName + " ===");
            for (RoleAssignment a : assignments) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  %s - %s [%s]\n", a.user().username(), status, a.assignmentType());
            }
            System.out.println();
        });

        parser.registerCommand("assignment-active", "List all active assignments", (scanner, sys) -> {
            List<RoleAssignment> active = sys.getAssignmentManager().getActiveAssignments();
            if (active.isEmpty()) {
                System.out.println("No active assignments.");
                return;
            }

            System.out.println("\n=== Active Assignments ===");
            System.out.printf("%-20s %-20s %-15s %-20s\n", "User", "Role", "Type", "Assigned At");
            System.out.println("-".repeat(80));
            for (RoleAssignment a : active) {
                System.out.printf("%-20s %-20s %-15s %-20s\n",
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        a.metadata().assignedAt());
            }
            System.out.println();
        });

        parser.registerCommand("assignment-expired", "List all expired assignments", (scanner, sys) -> {
            List<RoleAssignment> expired = sys.getAssignmentManager().getExpiredAssignments();
            if (expired.isEmpty()) {
                System.out.println("No expired/revoked assignments.");
                return;
            }

            System.out.println("\n=== Expired/Revoked Assignments ===");
            System.out.printf("%-20s %-20s %-15s\n", "User", "Role", "Type");
            System.out.println("-".repeat(60));
            for (RoleAssignment a : expired) {
                System.out.printf("%-20s %-20s %-15s\n",
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType());
            }
            System.out.println();
        });

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, sys) -> {
            String id = ConsoleUtils.promptString(scanner, "Enter assignment ID: ", true);

            Optional<RoleAssignment> assignmentOpt = sys.getAssignmentManager().findById(id);
            if (assignmentOpt.isEmpty()) {
                System.out.println("Assignment not found.");
                return;
            }

            if (!(assignmentOpt.get() instanceof TemporaryAssignment)) {
                System.out.println("Only temporary assignments can be extended.");
                return;
            }

            String newDate = ConsoleUtils.promptString(scanner, "New expiration date (yyyy-MM-dd HH:mm): ", true);

            try {
                sys.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                System.out.println("Assignment extended.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filters", (scanner, sys) -> {
            System.out.println("\nSearch filters:");
            System.out.println("1. By user");
            System.out.println("2. By role");
            System.out.println("3. By type (permanent/temporary)");
            System.out.println("4. By status (active/inactive)");
            System.out.println("5. Assigned after date");
            System.out.println("6. Expiring before date");
            int choice = ConsoleUtils.promptInt(scanner, "Choose filter (1-6): ", 1, 6);
            AssignmentFilter filter = null;

            switch (choice) {
                case 1:
                    filter = AssignmentFilters.byUsername(
                            ConsoleUtils.promptString(scanner, "Enter username: ", true));
                    break;
                case 2:
                    filter = AssignmentFilters.byRoleName(
                            ConsoleUtils.promptString(scanner, "Enter role name: ", true));
                    break;
                case 3:
                    filter = AssignmentFilters.byType(
                            ConsoleUtils.promptString(scanner, "Enter type (permanent/temporary): ", true).toUpperCase());
                    break;
                case 4:
                    boolean active = ConsoleUtils.promptYesNo(scanner, "Active only? (y/n): ");
                    filter = active ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
                    break;
                case 5:
                    filter = AssignmentFilters.assignedAfter(
                            ConsoleUtils.promptString(scanner, "Enter date (yyyy-MM-dd HH:mm): ", true));
                    break;
                case 6:
                    filter = AssignmentFilters.expiringBefore(
                            ConsoleUtils.promptString(scanner, "Enter date (yyyy-MM-dd HH:mm): ", true));
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }

            List<RoleAssignment> results = sys.getAssignmentManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("No assignments found.");
            } else {
                System.out.println("\nFound " + results.size() + " assignment(s):");
                for (RoleAssignment a : results) {
                    System.out.println("  " + a.user().username() + " -> " + a.role().getName() +
                            " [" + a.assignmentType() + "] " + (a.isActive() ? "ACTIVE" : "INACTIVE"));
                }
            }
            System.out.println();
        });
    }

    private void registerPermissionCommands() {
        parser.registerCommand("permissions-user", "View user permissions", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            Set<Permission> permissions = sys.getAssignmentManager().getUserPermissions(userOpt.get());
            if (permissions.isEmpty()) {
                System.out.println("No permissions for this user.");
                return;
            }

            System.out.println("\n=== Permissions for " + username + " ===");
            Map<String, List<Permission>> byResource = permissions.stream()
                    .collect(Collectors.groupingBy(Permission::resource));

            byResource.forEach((resource, perms) -> {
                System.out.println("\n" + resource.toUpperCase() + ":");
                perms.forEach(p -> System.out.println("  - " + p.name() + ": " + p.description()));
            });
            System.out.println();
        });

        parser.registerCommand("permissions-check", "Check if user has permission", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            String permName = ConsoleUtils.promptString(scanner, "Enter permission name: ", true);
            String resource = ConsoleUtils.promptString(scanner, "Enter resource: ", true);

            boolean hasPermission = sys.getAssignmentManager().userHasPermission(userOpt.get(), permName, resource);
            if (hasPermission) {
                System.out.println("User HAS permission: " + permName + " on " + resource);
            } else {
                System.out.println("User DOES NOT HAVE permission: " + permName + " on " + resource);
            }
        });
    }

    private void registerUtilityCommands() {
        parser.registerCommand("help", "Show this help", (scanner, sys) -> {
            parser.printHelp();
        });

        parser.registerCommand("audit-log", "Show audit log (and optionally save)", (scanner, sys) -> {
            sys.getAuditLog().printLog();
            String filename = ConsoleUtils.promptString(scanner, "Save to file? Enter filename or leave empty: ", false);
            if (!filename.isEmpty()) {
                try {
                    sys.getAuditLog().saveToFile(filename);
                    System.out.println("Audit log saved to " + filename);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-users", "Report: users with assigned roles", (scanner, sys) -> {
            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateUserReport(sys.getUserManager(), sys.getAssignmentManager());
            System.out.println(report);

            String filename = ConsoleUtils.promptString(scanner, "Save to file? Enter filename or leave empty: ", false);
            if (!filename.isEmpty()) {
                try {
                    generator.exportToFile(report, filename);
                    System.out.println("Report saved to " + filename);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-users-async", "Report: users (generate in background thread)", (scanner, sys) -> {
            sys.getBackgroundExecutor().execute(() -> {
                try {
                    ReportGenerator generator = new ReportGenerator();
                    String report = generator.generateUserReport(sys.getUserManager(), sys.getAssignmentManager());
                    synchronized (System.out) {
                        System.out.println(report);
                        System.out.println("[async] User report finished.");
                    }
                } catch (Exception e) {
                    synchronized (System.err) {
                        System.err.println("[async] User report failed: " + e.getMessage());
                    }
                }
            });
            System.out.println("User report generation started in the background.");
        });

        parser.registerCommand("save-async", "Save system data snapshot to file in background", (scanner, sys) -> {
            String filename = ConsoleUtils.promptString(scanner, "Enter filename: ", true);
            sys.getBackgroundExecutor().execute(() -> {
                try {
                    sys.saveDataToFile(filename);
                    synchronized (System.out) {
                        System.out.println("[async] Data saved to " + filename);
                    }
                } catch (Exception e) {
                    synchronized (System.err) {
                        System.err.println("[async] Save failed: " + e.getMessage());
                    }
                }
            });
            System.out.println("Data save started in the background.");
        });

        parser.registerCommand("report-roles", "Report: roles with user counts", (scanner, sys) -> {
            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateRoleReport(sys.getRoleManager(), sys.getAssignmentManager());
            System.out.println(report);

            String filename = ConsoleUtils.promptString(scanner, "Save to file? Enter filename or leave empty: ", false);
            if (!filename.isEmpty()) {
                try {
                    generator.exportToFile(report, filename);
                    System.out.println("Report saved to " + filename);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-matrix", "Report: permission matrix (users x resources)", (scanner, sys) -> {
            ReportGenerator generator = new ReportGenerator();
            String report = generator.generatePermissionMatrix(sys.getUserManager(), sys.getAssignmentManager());
            System.out.println(report);

            String filename = ConsoleUtils.promptString(scanner, "Save to file? Enter filename or leave empty: ", false);
            if (!filename.isEmpty()) {
                try {
                    generator.exportToFile(report, filename);
                    System.out.println("Report saved to " + filename);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("stats", "Show system statistics", (scanner, sys) -> {
            System.out.println(sys.generateStatistics());
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, sys) -> {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        });

        parser.registerCommand("exit", "Exit the program", (scanner, sys) -> {
            if (ConsoleUtils.promptYesNo(scanner, "Are you sure you want to exit? (y/n): ")) {
                System.out.println("Goodbye!");
                sys.shutdown();
                System.exit(0);
            }
        });
    }

    public CommandParser getParser() {
        return parser;
    }
}