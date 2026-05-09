import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReportGeneratorTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private ReportGenerator generator;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);
        generator = new ReportGenerator();

        User u = User.create("alice", "Alice", "alice@example.com");
        userManager.add(u);

        Permission p = new Permission("READ", "users", "Read");
        Role r = new Role("Viewer", "View");
        r.addPermission(p);
        roleManager.add(r);

        assignmentManager.add(new PermanentAssignment(
                u, r, AssignmentMetadata.now("admin", "setup")));
    }

    @Test
    void generateUserReport_usesParallelPath_sameAsBaselineStructure() {
        String report = generator.generateUserReport(userManager, assignmentManager);
        assertTrue(report.contains("=== USER REPORT ==="));
        assertTrue(report.contains("alice"));
        assertTrue(report.contains("Viewer"));
        assertTrue(report.contains("Total users: 1"));
    }

    @Test
    void generatePermissionMatrix_usesParallelPath_includesResourceAndPermission() {
        String matrix = generator.generatePermissionMatrix(userManager, assignmentManager);
        assertTrue(matrix.contains("=== PERMISSION MATRIX"));
        assertTrue(matrix.contains("alice"));
        assertTrue(matrix.contains("users"));
        assertTrue(matrix.contains("READ") || matrix.contains("read"));
    }
}
