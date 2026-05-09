import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;

public class AssignmentManagerTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private User user1;
    private User user2;
    private Role role1;
    private Role role2;
    private Permission perm1;
    private Permission perm2;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        user1 = User.create("john_doe", "John Doe", "john@example.com");
        user2 = User.create("jane_smith", "Jane Smith", "jane@example.com");
        userManager.add(user1);
        userManager.add(user2);

        perm1 = new Permission("READ", "users", "Read users");
        perm2 = new Permission("WRITE", "users", "Write users");

        role1 = new Role("Admin", "Administrator");
        role1.addPermission(perm1);
        role1.addPermission(perm2);

        role2 = new Role("Viewer", "Viewer");
        role2.addPermission(perm1);

        roleManager.add(role1);
        roleManager.add(role2);
    }

    @AfterEach
    void tearDown() {
        assignmentManager.clear();
    }

    @Test
    void testAdd() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, role1, meta);
        assignmentManager.add(assignment);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
    }

    @Test
    void testAddNull() {
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(null));
    }

    @Test
    void testAddWithNonExistingUser() {
        User nonExisting = User.create("unknown", "Unknown", "unknown@example.com");
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(nonExisting, role1, meta);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
    }

    @Test
    void testAddWithNonExistingRole() {
        Role nonExisting = new Role("Unknown", "Unknown");
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, nonExisting, meta);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
    }

    @Test
    void testAddDuplicateActiveAssignment() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second");

        PermanentAssignment a1 = new PermanentAssignment(user1, role1, meta1);
        PermanentAssignment a2 = new PermanentAssignment(user1, role1, meta2);

        assignmentManager.add(a1);
        assertThrows(IllegalStateException.class, () -> assignmentManager.add(a2));
    }

    @Test
    void testAddSameRoleAfterRevoke() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second");

        PermanentAssignment a1 = new PermanentAssignment(user1, role1, meta1);
        PermanentAssignment a2 = new PermanentAssignment(user1, role1, meta2);

        assignmentManager.add(a1);
        assignmentManager.revokeAssignment(a1.assignmentId());
        assignmentManager.add(a2);

        assertEquals(2, assignmentManager.count());
    }

    @Test
    void testRemove() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, role1, meta);
        assignmentManager.add(assignment);

        assertTrue(assignmentManager.remove(assignment));
        assertEquals(0, assignmentManager.count());
    }

    @Test
    void testFindById() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, role1, meta);
        assignmentManager.add(assignment);

        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
        assertFalse(assignmentManager.findById("unknown").isPresent());
    }

    @Test
    void testFindAll() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Test1");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Test2");

        assignmentManager.add(new PermanentAssignment(user1, role1, meta1));
        assignmentManager.add(new PermanentAssignment(user2, role2, meta2));

        assertEquals(2, assignmentManager.findAll().size());
    }

    @Test
    void testFindByUser() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Test1");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Test2");

        assignmentManager.add(new PermanentAssignment(user1, role1, meta1));
        assignmentManager.add(new PermanentAssignment(user1, role2, meta2));
        assignmentManager.add(new PermanentAssignment(user2, role1, AssignmentMetadata.now("admin", "Test3")));

        List<RoleAssignment> userAssignments = assignmentManager.findByUser(user1);
        assertEquals(2, userAssignments.size());
    }

    @Test
    void testFindByRole() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Test1");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Test2");

        assignmentManager.add(new PermanentAssignment(user1, role1, meta1));
        assignmentManager.add(new PermanentAssignment(user2, role1, meta2));
        assignmentManager.add(new PermanentAssignment(user1, role2, AssignmentMetadata.now("admin", "Test3")));

        List<RoleAssignment> roleAssignments = assignmentManager.findByRole(role1);
        assertEquals(2, roleAssignments.size());
    }

    @Test
    void testGetActiveAssignments() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Active");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Revoked");

        PermanentAssignment a1 = new PermanentAssignment(user1, role1, meta1);
        PermanentAssignment a2 = new PermanentAssignment(user2, role2, meta2);

        assignmentManager.add(a1);
        assignmentManager.add(a2);
        assignmentManager.revokeAssignment(a2.assignmentId());

        List<RoleAssignment> active = assignmentManager.getActiveAssignments();
        assertEquals(1, active.size());
        assertTrue(active.get(0).isActive());
    }

    @Test
    void testGetExpiredAssignments() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Active");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Expired");

        PermanentAssignment a1 = new PermanentAssignment(user1, role1, meta1);
        PermanentAssignment a2 = new PermanentAssignment(user2, role2, meta2);

        assignmentManager.add(a1);
        assignmentManager.add(a2);
        assignmentManager.revokeAssignment(a2.assignmentId());

        List<RoleAssignment> expired = assignmentManager.getExpiredAssignments();
        assertEquals(1, expired.size());
        assertFalse(expired.get(0).isActive());
    }

    @Test
    void testUserHasRole() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        assignmentManager.add(new PermanentAssignment(user1, role1, meta));

        assertTrue(assignmentManager.userHasRole(user1, role1));
        assertFalse(assignmentManager.userHasRole(user1, role2));
        assertFalse(assignmentManager.userHasRole(user2, role1));
    }

    @Test
    void testUserHasPermission() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        assignmentManager.add(new PermanentAssignment(user1, role1, meta));

        assertTrue(assignmentManager.userHasPermission(user1, "READ", "users"));
        assertTrue(assignmentManager.userHasPermission(user1, "WRITE", "users"));
        assertFalse(assignmentManager.userHasPermission(user1, "DELETE", "users"));
        assertFalse(assignmentManager.userHasPermission(user2, "READ", "users"));
    }

    @Test
    void testGetUserPermissions() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Test1");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Test2");

        assignmentManager.add(new PermanentAssignment(user1, role1, meta1));
        assignmentManager.add(new PermanentAssignment(user1, role2, meta2));

        Set<Permission> permissions = assignmentManager.getUserPermissions(user1);
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains(perm1));
        assertTrue(permissions.contains(perm2));
    }

    @Test
    void testRevokeAssignment() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, role1, meta);
        assignmentManager.add(assignment);

        assignmentManager.revokeAssignment(assignment.assignmentId());

        RoleAssignment revoked = assignmentManager.findById(assignment.assignmentId()).get();
        assertFalse(revoked.isActive());
    }

    @Test
    void testRevokeNonExistingAssignment() {
        assertThrows(IllegalArgumentException.class,
                () -> assignmentManager.revokeAssignment("unknown"));
    }

    @Test
    void testExtendTemporaryAssignment() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        TemporaryAssignment assignment = new TemporaryAssignment(
                user1, role1, meta, "2025-01-01 00:00", false);
        assignmentManager.add(assignment);

        assignmentManager.extendTemporaryAssignment(assignment.assignmentId(), "2026-01-01 00:00");

        TemporaryAssignment extended = (TemporaryAssignment) assignmentManager
                .findById(assignment.assignmentId()).get();
        assertEquals("2026-01-01 00:00", extended.getExpiresAt());
    }

    @Test
    void testDeactivateExpiredTemporaryByScheduler() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        TemporaryAssignment assignment = new TemporaryAssignment(
                user1, role1, meta, "2020-01-01 00:00", false);
        assignmentManager.add(assignment);

        assertFalse(assignment.isActive());
        assertEquals(1, assignmentManager.deactivateExpiredTemporaryByScheduler());

        TemporaryAssignment stored = (TemporaryAssignment) assignmentManager
                .findById(assignment.assignmentId()).get();
        assertTrue(stored.isInactiveByScheduler());
        assertEquals(0, assignmentManager.deactivateExpiredTemporaryByScheduler());
    }

    @Test
    void testExtendPermanentAssignment() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, role1, meta);
        assignmentManager.add(assignment);

        assertThrows(IllegalArgumentException.class,
                () -> assignmentManager.extendTemporaryAssignment(assignment.assignmentId(), "2026-01-01 00:00"));
    }

    @Test
    void testFindByFilter() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Test1");
        AssignmentMetadata meta2 = AssignmentMetadata.now("manager", "Test2");

        assignmentManager.add(new PermanentAssignment(user1, role1, meta1));
        assignmentManager.add(new PermanentAssignment(user2, role2, meta2));

        AssignmentFilter filter = AssignmentFilters.assignedBy("admin");
        List<RoleAssignment> result = assignmentManager.findByFilter(filter);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllWithSorter() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First");
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second");
        AssignmentMetadata meta3 = AssignmentMetadata.now("admin", "Third");

        assignmentManager.add(new PermanentAssignment(user2, role2, meta2));
        assignmentManager.add(new PermanentAssignment(user1, role1, meta1));
        assignmentManager.add(new PermanentAssignment(user1, role2, meta3));

        List<RoleAssignment> sorted = assignmentManager.findAll(null, AssignmentSorters.byUsername());
        assertEquals("jane_smith", sorted.get(0).user().username());
        assertEquals("john_doe", sorted.get(1).user().username());
    }

    @Test
    void testEquals() {
        AssignmentManager am1 = new AssignmentManager(userManager, roleManager);
        AssignmentManager am2 = new AssignmentManager(userManager, roleManager);
        assertEquals(am1, am2);

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        am1.add(new PermanentAssignment(user1, role1, meta));
        assertNotEquals(am1, am2);
    }

    @Test
    void testToString() {
        AssignmentManager am = new AssignmentManager(userManager, roleManager);
        assertEquals("AssignmentManager{assignments=0}", am.toString());

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        am.add(new PermanentAssignment(user1, role1, meta));
        assertEquals("AssignmentManager{assignments=1}", am.toString());
    }
}