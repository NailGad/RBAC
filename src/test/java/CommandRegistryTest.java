import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.util.*;

public class CommandRegistryTest {

    private RBACSystem system;
    private CommandRegistry registry;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
        registry = new CommandRegistry(system);

        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.shutdown();
        }
        System.setOut(originalOut);
    }

    private Scanner createScannerWithInput(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    private String getOutput() {
        return outContent.toString();
    }

    private void clearOutput() {
        outContent.reset();
    }

    @Test
    void testHelpCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.executeCommand("help", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Available Commands"));
        assertTrue(output.contains("help"));
        assertTrue(output.contains("stats"));
        assertTrue(output.contains("exit"));
        assertTrue(output.contains("scheduler-start"));
        assertTrue(output.contains("scheduler-stop"));
    }

    @Test
    void testStatsCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.executeCommand("stats", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("SYSTEM STATISTICS"));
        assertTrue(output.contains("Users:"));
        assertTrue(output.contains("Roles:"));
        assertTrue(output.contains("Assignments:"));
    }

    @Test
    void testUserListCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.executeCommand("user-list", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("System Administrator"));
    }

    @Test
    void testUserCreateCommand() {
        String input = "newuser\nNew User\nnewuser@test.com\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-create", scanner, system);

        assertTrue(system.getUserManager().exists("newuser"));

        Optional<User> user = system.getUserManager().findByUsername("newuser");
        assertTrue(user.isPresent());
        assertEquals("New User", user.get().fullName());
        assertEquals("newuser@test.com", user.get().email());
    }

    @Test
    void testUserCreateWithInvalidData() {
        String input = "invalid user name\nNew User\nemail@test.com\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-create", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Error"));
        assertFalse(system.getUserManager().exists("invalid user name"));
    }

    @Test
    void testUserViewCommand() {
        String input = "admin\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-view", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("System Administrator"));
        assertTrue(output.contains("admin@system.com"));
    }

    @Test
    void testUserViewNonExistent() {
        String input = "nonexistent\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-view", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("User not found"));
    }

    @Test
    void testUserUpdateCommand() {
        system.getUserManager().add(User.create("updateuser", "Old Name", "old@test.com"));

        String input = "updateuser\nUpdated Name\nupdated@test.com\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-update", scanner, system);

        Optional<User> user = system.getUserManager().findByUsername("updateuser");
        assertTrue(user.isPresent());
        assertEquals("Updated Name", user.get().fullName());
        assertEquals("updated@test.com", user.get().email());
    }

    @Test
    void testUserDeleteCommand() {
        system.getUserManager().add(User.create("deleteuser", "To Delete", "delete@test.com"));

        String input = "deleteuser\nyes\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-delete", scanner, system);

        assertFalse(system.getUserManager().exists("deleteuser"));
    }

    @Test
    void testUserDeleteWithCancel() {
        system.getUserManager().add(User.create("canceluser", "To Cancel", "cancel@test.com"));

        String input = "canceluser\nno\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-delete", scanner, system);

        assertTrue(system.getUserManager().exists("canceluser"));
    }

    @Test
    void testUserSearchByUsername() {
        system.getUserManager().add(User.create("john_doe", "John Doe", "john@test.com"));
        system.getUserManager().add(User.create("jane_doe", "Jane Doe", "jane@test.com"));

        String input = "1\njohn\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-search", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("john_doe"));
        assertFalse(output.contains("jane_doe"));
    }

    @Test
    void testUserSearchByEmailDomain() {
        system.getUserManager().add(User.create("user1", "User One", "user1@gmail.com"));
        system.getUserManager().add(User.create("user2", "User Two", "user2@yahoo.com"));

        String input = "3\n@gmail.com\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("user-search", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("user1"));
        assertFalse(output.contains("user2"));
    }

    @Test
    void testRoleListCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.executeCommand("role-list", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("Manager"));
        assertTrue(output.contains("Viewer"));
    }

    @Test
    void testRoleCreateCommand() {
        String input = "TestRole\nTest Description\ndone\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-create", scanner, system);

        Optional<Role> role = system.getRoleManager().findByName("TestRole");
        assertTrue(role.isPresent());
        assertEquals("Test Description", role.get().getDescription());
    }

    @Test
    void testRoleCreateWithPermissions() {
        String input = "PowerRole\nPower Description\ny\nREAD\nusers\nRead users\ndone\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-create", scanner, system);

        Optional<Role> role = system.getRoleManager().findByName("PowerRole");
        assertTrue(role.isPresent());
        assertTrue(role.get().hasPermission("READ", "users"));
    }

    @Test
    void testRoleViewCommand() {
        String input = "Admin\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-view", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("Full system access"));
    }

    @Test
    void testRoleDeleteCommand() {
        Role testRole = new Role("DeleteMe", "To be deleted");
        system.getRoleManager().add(testRole);

        String input = "DeleteMe\nyes\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-delete", scanner, system);

        assertFalse(system.getRoleManager().exists("DeleteMe"));
    }

    @Test
    void testRoleAddPermissionCommand() {
        Role testRole = new Role("TestAddRole_" + System.currentTimeMillis(), "Role for testing add permission");
        system.getRoleManager().add(testRole);

        assertTrue(testRole.getPermissions().isEmpty());

        String input = testRole.getName() + "\nEXECUTE\nscripts\nExecute scripts\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-add-permission", scanner, system);

        Optional<Role> updatedRoleOpt = system.getRoleManager().findByName(testRole.getName());
        assertTrue(updatedRoleOpt.isPresent());
        Role updatedRole = updatedRoleOpt.get();

        assertTrue(updatedRole.hasPermission("EXECUTE", "scripts"));
        assertEquals(1, updatedRole.getPermissions().size());

        system.getRoleManager().remove(testRole);
    }

    @Test
    void testRoleRemovePermissionCommand() {
        Role testRole = new Role("TempRemoveRole_" + System.currentTimeMillis(), "Temporary role for testing");
        system.getRoleManager().add(testRole);

        Permission perm1 = new Permission("REMOVE_ME", "test1", "Permission to be removed");
        Permission perm2 = new Permission("KEEP_ME", "test2", "Permission to keep");
        testRole.addPermission(perm1);
        testRole.addPermission(perm2);

        assertTrue(testRole.hasPermission(perm1));
        assertTrue(testRole.hasPermission(perm2));
        assertEquals(2, testRole.getPermissions().size());

        String input = testRole.getName() + "\n1\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-remove-permission", scanner, system);

        Optional<Role> updatedRoleOpt = system.getRoleManager().findByName(testRole.getName());
        assertTrue(updatedRoleOpt.isPresent());
        Role updatedRole = updatedRoleOpt.get();

        assertFalse(updatedRole.hasPermission(perm1));
        assertTrue(updatedRole.hasPermission(perm2));
        assertEquals(1, updatedRole.getPermissions().size());

        system.getRoleManager().remove(testRole);
    }

    @Test
    void testRoleSearchByName() {
        String input = "1\nAdmin\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-search", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Admin"));
    }

    @Test
    void testRoleSearchByPermission() {
        String input = "2\nREAD\nusers\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("role-search", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Admin"));
    }

    @Test
    void testAssignRoleCommand() throws Exception {
        system.getUserManager().add(User.create("assignuser", "Assign User", "assign@test.com"));

        List<Role> roles = system.getRoleManager().findAll();
        int adminIndex = -1;
        for (int i = 0; i < roles.size(); i++) {
            if ("Admin".equals(roles.get(i).getName())) {
                adminIndex = i + 1;
                break;
            }
        }
        assertTrue(adminIndex > 0, "Admin role not found");

        String input = "assignuser\n" + adminIndex + "\npermanent\nTest assignment\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("assign-role", scanner, system);

        Optional<User> user = system.getUserManager().findByUsername("assignuser");
        assertTrue(user.isPresent());

        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user.get());
        assertFalse(assignments.isEmpty());
        assertEquals("Admin", assignments.get(0).role().getName());
    }

    @Test
    void testAssignTemporaryRoleCommand() throws Exception {
        system.getUserManager().add(User.create("tempuser", "Temp User", "temp@test.com"));

        List<Role> roles = system.getRoleManager().findAll();
        int viewerIndex = -1;
        for (int i = 0; i < roles.size(); i++) {
            if ("Viewer".equals(roles.get(i).getName())) {
                viewerIndex = i + 1;
                break;
            }
        }
        assertTrue(viewerIndex > 0);

        String input = "tempuser\n" + viewerIndex + "\ntemporary\nTemporary access\n2026-12-31 23:59\nn\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("assign-role", scanner, system);

        Optional<User> user = system.getUserManager().findByUsername("tempuser");
        assertTrue(user.isPresent());

        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user.get());
        assertFalse(assignments.isEmpty());
        assertTrue(assignments.get(0) instanceof TemporaryAssignment);
    }

    @Test
    void testRevokeRoleCommand() throws Exception {
        User revokeUser = User.create("revokeuser", "Revoke User", "revoke@test.com");
        system.getUserManager().add(revokeUser);

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(revokeUser,
                system.getRoleManager().findByName("Viewer").get(), meta);
        system.getAssignmentManager().add(assignment);

        String input = "revokeuser\n1\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("revoke-role", scanner, system);

        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(revokeUser);
        assertFalse(assignments.isEmpty());
        assertFalse(assignments.get(0).isActive());
    }

    @Test
    void testAssignmentListUserCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("admin\n");
        parser.executeCommand("assignment-list-user", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Admin"));
    }

    @Test
    void testAssignmentListRoleCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("Admin\n");
        parser.executeCommand("assignment-list-role", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("admin"));
    }

    @Test
    void testAssignmentActiveCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.executeCommand("assignment-active", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("Admin"));
    }

    @Test
    void testPermissionsUserCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("admin\n");
        parser.executeCommand("permissions-user", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("READ"));
        assertTrue(output.contains("WRITE"));
        assertTrue(output.contains("DELETE"));
    }

    @Test
    void testPermissionsCheckCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("admin\nREAD\nusers\n");
        parser.executeCommand("permissions-check", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("HAS permission"));
    }

    @Test
    void testPermissionsCheckNegative() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("admin\nNONEXISTENT\nusers\n");
        parser.executeCommand("permissions-check", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("DOES NOT HAVE"));
    }

    @Test
    void testClearCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.executeCommand("clear", scanner, system);

        assertNotNull(getOutput());
    }

    @Test
    void testUnknownCommand() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.executeCommand("unknowncommand", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("Unknown command"));
    }

    @Test
    void testParseAndExecute() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.parseAndExecute("stats", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("SYSTEM STATISTICS"));
    }

    @Test
    void testParseAndExecuteEmptyInput() {
        CommandParser parser = registry.getParser();
        Scanner scanner = createScannerWithInput("");
        parser.parseAndExecute("", scanner, system);

        assertEquals("", getOutput());
    }

    @Test
    void testAssignmentSearchByUser() {
        String input = "1\nadmin\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("assignment-search", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("admin"));
    }

    @Test
    void testAssignmentSearchByType() {
        String input = "3\npermanent\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("assignment-search", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("PERMANENT"));
    }

    @Test
    void testAssignmentSearchByStatus() {
        String input = "4\nactive\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("assignment-search", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("ACTIVE"));
    }

    @Test
    void testExtendTemporaryAssignment() throws Exception {
        User extendUser = User.create("extenduser", "Extend User", "extend@test.com");
        system.getUserManager().add(extendUser);

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        TemporaryAssignment temp = new TemporaryAssignment(extendUser,
                system.getRoleManager().findByName("Viewer").get(),
                meta, "2025-12-31 23:59", false);
        system.getAssignmentManager().add(temp);

        String input = temp.assignmentId() + "\n2026-12-31 23:59\n";
        Scanner scanner = createScannerWithInput(input);

        CommandParser parser = registry.getParser();
        parser.executeCommand("assignment-extend", scanner, system);

        String output = getOutput();
        assertTrue(output.contains("extended"));
    }
}