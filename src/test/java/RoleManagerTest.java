import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;

public class RoleManagerTest {

    private RoleManager roleManager;
    private Permission readUsers;
    private Permission writeUsers;
    private Permission deleteUsers;
    private Permission readReports;
    private Role adminRole;
    private Role editorRole;
    private Role viewerRole;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager();

        readUsers = new Permission("READ", "users", "Can read users");
        writeUsers = new Permission("WRITE", "users", "Can write users");
        deleteUsers = new Permission("DELETE", "users", "Can delete users");
        readReports = new Permission("READ", "reports", "Can read reports");

        adminRole = new Role("Administrator", "Full system access");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readReports);

        editorRole = new Role("Editor", "Can edit content");
        editorRole.addPermission(readUsers);
        editorRole.addPermission(writeUsers);
        editorRole.addPermission(readReports);

        viewerRole = new Role("Viewer", "Can view only");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readReports);
    }

    @AfterEach
    void tearDown() {
        roleManager.clear();
    }

    @Test
    void testAdd() {
        roleManager.add(adminRole);
        assertEquals(1, roleManager.count());
        assertTrue(roleManager.exists("Administrator"));
    }

    @Test
    void testAddNull() {
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(null));
    }

    @Test
    void testAddDuplicate() {
        roleManager.add(adminRole);
        Role anotherAdmin = new Role("Administrator", "Another admin");
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(anotherAdmin));
    }

    @Test
    void testRemove() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);

        boolean removed = roleManager.remove(adminRole);
        assertTrue(removed);
        assertEquals(1, roleManager.count());
        assertFalse(roleManager.exists("Administrator"));
        assertFalse(roleManager.findById(adminRole.getId()).isPresent());
        assertFalse(roleManager.findByName("Administrator").isPresent());
    }

    @Test
    void testRemoveNonExisting() {
        assertFalse(roleManager.remove(adminRole));
    }

    @Test
    void testFindById() {
        roleManager.add(adminRole);
        Optional<Role> found = roleManager.findById(adminRole.getId());
        assertTrue(found.isPresent());
        assertEquals("Administrator", found.get().getName());
        assertFalse(roleManager.findById("unknown").isPresent());
    }

    @Test
    void testFindAll() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);
        assertEquals(2, roleManager.findAll().size());
    }

    @Test
    void testCount() {
        assertEquals(0, roleManager.count());
        roleManager.add(adminRole);
        assertEquals(1, roleManager.count());
    }

    @Test
    void testClear() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);
        assertEquals(2, roleManager.count());
        roleManager.clear();
        assertEquals(0, roleManager.count());
    }

    @Test
    void testFindByName() {
        roleManager.add(adminRole);
        Optional<Role> found = roleManager.findByName("Administrator");
        assertTrue(found.isPresent());
        assertEquals("Administrator", found.get().getName());
        assertFalse(roleManager.findByName("Unknown").isPresent());
    }

    @Test
    void testExists() {
        roleManager.add(adminRole);
        assertTrue(roleManager.exists("Administrator"));
        assertFalse(roleManager.exists("Unknown"));
    }

    @Test
    void testAddPermissionToRole() {
        roleManager.add(adminRole);
        Permission newPerm = new Permission("EXECUTE", "scripts", "Execute");
        roleManager.addPermissionToRole("Administrator", newPerm);
        assertTrue(roleManager.findByName("Administrator").get().hasPermission(newPerm));
    }

    @Test
    void testRemovePermissionFromRole() {
        roleManager.add(adminRole);
        roleManager.removePermissionFromRole("Administrator", deleteUsers);
        assertFalse(roleManager.findByName("Administrator").get().hasPermission(deleteUsers));
    }

    @Test
    void testFindRolesWithPermission() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);
        roleManager.add(viewerRole);

        List<Role> result = roleManager.findRolesWithPermission("DELETE", "users");
        assertEquals(1, result.size());
        assertEquals("Administrator", result.get(0).getName());
    }

    @Test
    void testFindByFilter() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);
        roleManager.add(viewerRole);

        RoleFilter filter = RoleFilters.byNameContains("Editor");
        List<Role> result = roleManager.findByFilter(filter);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllWithSorter() {
        roleManager.add(viewerRole);
        roleManager.add(adminRole);
        roleManager.add(editorRole);

        List<Role> result = roleManager.findAll(null, RoleSorters.byName());
        assertEquals("Administrator", result.get(0).getName());
        assertEquals("Editor", result.get(1).getName());
        assertEquals("Viewer", result.get(2).getName());
    }

    @Test
    void testEquals() {
        RoleManager manager1 = new RoleManager();
        RoleManager manager2 = new RoleManager();
        assertEquals(manager1, manager2);

        manager1.add(adminRole);
        assertNotEquals(manager1, manager2);

        manager2.add(adminRole);
        assertEquals(manager1, manager2);
    }
}