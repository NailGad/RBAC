import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;

public class UserManagerTest {

    private UserManager userManager;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        user1 = User.create("john_doe", "John Doe", "john@example.com");
        user2 = User.create("jane_smith", "Jane Smith", "jane@example.com");
        user3 = User.create("bob_johnson", "Bob Johnson", "bob@example.com");
    }

    @AfterEach
    void tearDown() {
        userManager.clear();
    }

    @Test
    void testAdd() {
        userManager.add(user1);
        assertEquals(1, userManager.count());
        assertTrue(userManager.exists("john_doe"));
    }

    @Test
    void testAddNull() {
        assertThrows(IllegalArgumentException.class, () -> userManager.add(null));
    }

    @Test
    void testAddDuplicate() {
        userManager.add(user1);
        User duplicate = User.create("john_doe", "John Doe 2", "john2@example.com");
        assertThrows(IllegalArgumentException.class, () -> userManager.add(duplicate));
    }

    @Test
    void testRemove() {
        userManager.add(user1);
        userManager.add(user2);
        assertTrue(userManager.remove(user1));
        assertEquals(1, userManager.count());
        assertFalse(userManager.exists("john_doe"));
    }

    @Test
    void testRemoveNonExisting() {
        assertFalse(userManager.remove(user1));
    }

    @Test
    void testRemoveNull() {
        assertFalse(userManager.remove(null));
    }

    @Test
    void testFindById() {
        userManager.add(user1);
        userManager.add(user2);
        assertTrue(userManager.findById("jane_smith").isPresent());
        assertFalse(userManager.findById("unknown").isPresent());
    }

    @Test
    void testFindByIdNull() {
        assertFalse(userManager.findById(null).isPresent());
        assertFalse(userManager.findById("").isPresent());
    }

    @Test
    void testFindAll() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);
        assertEquals(3, userManager.findAll().size());
    }

    @Test
    void testCount() {
        assertEquals(0, userManager.count());
        userManager.add(user1);
        assertEquals(1, userManager.count());
    }

    @Test
    void testClear() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.clear();
        assertEquals(0, userManager.count());
    }

    @Test
    void testFindByUsername() {
        userManager.add(user1);
        userManager.add(user2);
        assertTrue(userManager.findByUsername("jane_smith").isPresent());
        assertFalse(userManager.findByUsername("unknown").isPresent());
    }

    @Test
    void testFindByEmail() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(User.create("ALICE", "Alice Wonder", "ALICE@EXAMPLE.COM"));

        assertTrue(userManager.findByEmail("jane@example.com").isPresent());
        assertTrue(userManager.findByEmail("ALICE@example.com").isPresent());
        assertFalse(userManager.findByEmail("unknown@example.com").isPresent());
    }

    @Test
    void testExists() {
        userManager.add(user1);
        assertTrue(userManager.exists("john_doe"));
        assertFalse(userManager.exists("unknown"));
    }

    @Test
    void testUpdate() {
        userManager.add(user1);
        userManager.update("john_doe", "John Updated", "john.new@example.com");
        User updated = userManager.findById("john_doe").get();
        assertEquals("John Updated", updated.fullName());
        assertEquals("john.new@example.com", updated.email());
    }

    @Test
    void testUpdateNonExisting() {
        assertThrows(IllegalArgumentException.class,
                () -> userManager.update("unknown", "New Name", "new@email.com"));
    }

    @Test
    void testFindByFilter() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);

        UserFilter filter = UserFilters.byUsername("jane_smith");
        List<User> result = userManager.findByFilter(filter);
        assertEquals(1, result.size());
        assertEquals("jane_smith", result.get(0).username());
    }

    @Test
    void testFindByFilterByEmailDomain() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(User.create("test1", "Test One", "test1@gmail.com"));

        UserFilter filter = UserFilters.byEmailDomain("@gmail.com");
        assertEquals(1, userManager.findByFilter(filter).size());
    }

    @Test
    void testFindAllWithFilterAndSorter() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(User.create("adam", "Adam Adamson", "adam@example.com"));

        UserFilter filter = UserFilters.byEmailDomain("@example.com");
        List<User> result = userManager.findAll(filter, UserSorters.byUsername());
        assertEquals(3, result.size());
        assertEquals("adam", result.get(0).username());
    }

    @Test
    void testEquals() {
        UserManager m1 = new UserManager();
        UserManager m2 = new UserManager();
        assertEquals(m1, m2);

        m1.add(user1);
        assertNotEquals(m1, m2);

        m2.add(user1);
        assertEquals(m1, m2);
    }

    @Test
    void testToString() {
        UserManager m = new UserManager();
        assertEquals("UserManager{users=0}", m.toString());
        m.add(user1);
        assertEquals("UserManager{users=1}", m.toString());
    }
}