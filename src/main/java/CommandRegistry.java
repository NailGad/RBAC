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
            System.out.println("\n=== Users ===");
            System.out.printf("%-20s %-25s %-30s\n", "Username", "Full Name", "Email");
            System.out.println("-".repeat(80));
            for (User u : users) {
                System.out.printf("%-20s %-25s %-30s\n", u.username(), u.fullName(), u.email());
            }
            System.out.println();
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter full name: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Enter email: ");
            String email = scanner.nextLine().trim();

            try {
                User user = User.create(username, fullName, email);
                sys.getUserManager().add(user);
                System.out.println("User created successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user details", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            User user = userOpt.get();
            System.out.println("\n=== User Details ===");
            System.out.println("Username: " + user.username());
            System.out.println("Full Name: " + user.fullName());
            System.out.println("Email: " + user.email());

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
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            if (!sys.getUserManager().exists(username)) {
                System.out.println("User not found.");
                return;
            }

            System.out.print("Enter new full name (leave empty to keep current): ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Enter new email (leave empty to keep current): ");
            String email = scanner.nextLine().trim();

            try {
                sys.getUserManager().update(username, fullName.isEmpty() ? null : fullName, email.isEmpty() ? null : email);
                System.out.println("User updated successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

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

            System.out.print("Confirm deletion (type 'yes'): ");
            String confirm = scanner.nextLine().trim();
            if (!"yes".equalsIgnoreCase(confirm)) {
                System.out.println("Deletion cancelled.");
                return;
            }

            for (RoleAssignment a : assignments) {
                sys.getAssignmentManager().remove(a);
            }
            sys.getUserManager().remove(user);
            System.out.println("User deleted successfully.");
        });

        parser.registerCommand("user-search", "Search users by filters", (scanner, sys) -> {
            System.out.println("\nSearch filters:");
            System.out.println("1. By username contains");
            System.out.println("2. By email contains");
            System.out.println("3. By email domain");
            System.out.println("4. By full name contains");
            System.out.print("Choose filter (1-4): ");

            String choice = scanner.nextLine().trim();
            UserFilter filter = null;

            switch (choice) {
                case "1":
                    System.out.print("Enter username substring: ");
                    filter = UserFilters.byUsernameContains(scanner.nextLine().trim());
                    break;
                case "2":
                    System.out.print("Enter email substring: ");
                    filter = UserFilters.byEmailContains(scanner.nextLine().trim());
                    break;
                case "3":
                    System.out.print("Enter email domain (e.g., @example.com): ");
                    filter = UserFilters.byEmailDomain(scanner.nextLine().trim());
                    break;
                case "4":
                    System.out.print("Enter full name substring: ");
                    filter = UserFilters.byFullNameContains(scanner.nextLine().trim());
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
            System.out.println("\n=== Roles ===");
            System.out.printf("%-20s %-10s %-40s\n", "Name", "Permissions", "Description");
            System.out.println("-".repeat(80));
            for (Role r : roles) {
                System.out.printf("%-20s %-10d %-40s\n", r.getName(), r.getPermissions().size(), r.getDescription());
            }
            System.out.println();
        });

        parser.registerCommand("role-create", "Create a new role", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Enter role description: ");
            String description = scanner.nextLine().trim();

            try {
                Role role = new Role(name, description);
                sys.getRoleManager().add(role);
                System.out.println("Role created successfully.");

                System.out.print("Do you want to add permissions now? (y/n): ");
                if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
                    while (true) {
                        System.out.print("Enter permission name (or 'done' to finish): ");
                        String permName = scanner.nextLine().trim();
                        if ("done".equalsIgnoreCase(permName)) break;

                        System.out.print("Enter resource: ");
                        String resource = scanner.nextLine().trim();
                        System.out.print("Enter description: ");
                        String permDesc = scanner.nextLine().trim();

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
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();

            Optional<Role> roleOpt = sys.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            System.out.println(roleOpt.get().format());
        });

        parser.registerCommand("role-update", "Update role name/description", (scanner, sys) -> {
            System.out.print("Enter role name to update: ");
            String oldName = scanner.nextLine().trim();

            Optional<Role> roleOpt = sys.getRoleManager().findByName(oldName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            Role role = roleOpt.get();
            System.out.print("Enter new name (leave empty to keep current): ");
            String newName = scanner.nextLine().trim();
            System.out.print("Enter new description (leave empty to keep current): ");
            String newDesc = scanner.nextLine().trim();

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
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();

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
                System.out.print("Confirm deletion (type 'yes'): ");
                if (!"yes".equalsIgnoreCase(scanner.nextLine().trim())) {
                    System.out.println("Deletion cancelled.");
                    return;
                }
                for (RoleAssignment a : assignments) {
                    sys.getAssignmentManager().remove(a);
                }
            }

            sys.getRoleManager().remove(role);
            System.out.println("Role deleted successfully.");
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String roleName = scanner.nextLine().trim();

            Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }

            System.out.print("Enter permission name: ");
            String permName = scanner.nextLine().trim();
            System.out.print("Enter resource: ");
            String resource = scanner.nextLine().trim();
            System.out.print("Enter description: ");
            String description = scanner.nextLine().trim();

            try {
                Permission perm = new Permission(permName, resource, description);
                sys.getRoleManager().addPermissionToRole(roleName, perm);
                System.out.println("Permission added to role.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String roleName = scanner.nextLine().trim();

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
            for (int i = 0; i < permList.size(); i++) {
                System.out.printf("  %d. %s\n", i + 1, permList.get(i).format());
            }

            System.out.print("Enter number to remove: ");
            try {
                int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (index >= 0 && index < permList.size()) {
                    sys.getRoleManager().removePermissionFromRole(roleName, permList.get(index));
                    System.out.println("Permission removed.");
                } else {
                    System.out.println("Invalid number.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        });

        parser.registerCommand("role-search", "Search roles", (scanner, sys) -> {
            System.out.println("\nSearch filters:");
            System.out.println("1. By name contains");
            System.out.println("2. Has specific permission");
            System.out.println("3. Has at least N permissions");
            System.out.print("Choose filter (1-3): ");

            String choice = scanner.nextLine().trim();
            RoleFilter filter = null;

            switch (choice) {
                case "1":
                    System.out.print("Enter name substring: ");
                    filter = RoleFilters.byNameContains(scanner.nextLine().trim());
                    break;
                case "2":
                    System.out.print("Enter permission name: ");
                    String permName = scanner.nextLine().trim();
                    System.out.print("Enter resource: ");
                    String resource = scanner.nextLine().trim();
                    filter = RoleFilters.hasPermission(permName, resource);
                    break;
                case "3":
                    System.out.print("Enter minimum number of permissions: ");
                    try {
                        int n = Integer.parseInt(scanner.nextLine().trim());
                        filter = RoleFilters.hasAtLeastNPermissions(n);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number.");
                        return;
                    }
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
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

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

            System.out.println("\nAvailable roles:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.printf("  %d. %s\n", i + 1, roles.get(i).getName());
            }
            System.out.print("Choose role number: ");
            try {
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx < 0 || idx >= roles.size()) {
                    System.out.println("Invalid choice.");
                    return;
                }
                Role role = roles.get(idx);

                System.out.print("Assignment type (permanent/temporary): ");
                String type = scanner.nextLine().trim().toLowerCase();

                System.out.print("Reason for assignment: ");
                String reason = scanner.nextLine().trim();

                AssignmentMetadata metadata = AssignmentMetadata.now(sys.getCurrentUser(), reason);

                if ("permanent".equals(type)) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    sys.getAssignmentManager().add(assignment);
                    System.out.println("Permanent assignment created.");
                } else if ("temporary".equals(type)) {
                    System.out.print("Expiration date (yyyy-MM-dd HH:mm): ");
                    String expiresAt = scanner.nextLine().trim();
                    System.out.print("Auto-renew? (y/n): ");
                    boolean autoRenew = "y".equalsIgnoreCase(scanner.nextLine().trim());

                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    sys.getAssignmentManager().add(assignment);
                    System.out.println("Temporary assignment created.");
                } else {
                    System.out.println("Invalid type.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            } catch (IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

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

            System.out.println("\nActive assignments:");
            for (int i = 0; i < assignments.size(); i++) {
                RoleAssignment a = assignments.get(i);
                System.out.printf("  %d. %s [%s]\n", i + 1, a.role().getName(), a.assignmentType());
            }
            System.out.print("Choose assignment to revoke: ");
            try {
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx >= 0 && idx < assignments.size()) {
                    sys.getAssignmentManager().revokeAssignment(assignments.get(idx).assignmentId());
                    System.out.println("Assignment revoked.");
                } else {
                    System.out.println("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        });

        parser.registerCommand("assignment-list-user", "List user assignments", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

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
            System.out.print("Enter role name: ");
            String roleName = scanner.nextLine().trim();

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
            System.out.print("Enter assignment ID: ");
            String id = scanner.nextLine().trim();

            Optional<RoleAssignment> assignmentOpt = sys.getAssignmentManager().findById(id);
            if (assignmentOpt.isEmpty()) {
                System.out.println("Assignment not found.");
                return;
            }

            if (!(assignmentOpt.get() instanceof TemporaryAssignment)) {
                System.out.println("Only temporary assignments can be extended.");
                return;
            }

            System.out.print("New expiration date (yyyy-MM-dd HH:mm): ");
            String newDate = scanner.nextLine().trim();

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
            System.out.print("Choose filter (1-6): ");

            String choice = scanner.nextLine().trim();
            AssignmentFilter filter = null;

            switch (choice) {
                case "1":
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine().trim();
                    filter = AssignmentFilters.byUsername(username);
                    break;
                case "2":
                    System.out.print("Enter role name: ");
                    String roleName = scanner.nextLine().trim();
                    filter = AssignmentFilters.byRoleName(roleName);
                    break;
                case "3":
                    System.out.print("Enter type (permanent/temporary): ");
                    filter = AssignmentFilters.byType(scanner.nextLine().trim().toUpperCase());
                    break;
                case "4":
                    System.out.print("Enter status (active/inactive): ");
                    String status = scanner.nextLine().trim();
                    if ("active".equalsIgnoreCase(status)) {
                        filter = AssignmentFilters.activeOnly();
                    } else {
                        filter = AssignmentFilters.inactiveOnly();
                    }
                    break;
                case "5":
                    System.out.print("Enter date (yyyy-MM-dd HH:mm): ");
                    filter = AssignmentFilters.assignedAfter(scanner.nextLine().trim());
                    break;
                case "6":
                    System.out.print("Enter date (yyyy-MM-dd HH:mm): ");
                    filter = AssignmentFilters.expiringBefore(scanner.nextLine().trim());
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
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

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
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            System.out.print("Enter permission name: ");
            String permName = scanner.nextLine().trim();
            System.out.print("Enter resource: ");
            String resource = scanner.nextLine().trim();

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

        parser.registerCommand("stats", "Show system statistics", (scanner, sys) -> {
            System.out.println(sys.generateStatistics());
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, sys) -> {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        });

        parser.registerCommand("exit", "Exit the program", (scanner, sys) -> {
            System.out.print("Are you sure you want to exit? (y/n): ");
            if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
                System.out.println("Goodbye!");
                System.exit(0);
            }
        });
    }

    public CommandParser getParser() {
        return parser;
    }
}