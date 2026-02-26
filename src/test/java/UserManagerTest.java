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

    // ============================================================
    // ТЕСТЫ МЕТОДОВ REPOSITORY
    // ============================================================

    @Test
    @DisplayName("Тест добавления пользователя")
    void testAdd() {
        userManager.add(user1);
        assertEquals(1, userManager.count());
        assertTrue(userManager.exists("john_doe"));
    }

    @Test
    @DisplayName("Тест добавления null пользователя")
    void testAddNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userManager.add(null);
        });
        assertEquals("User cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Тест добавления дубликата")
    void testAddDuplicate() {
        userManager.add(user1);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userManager.add(User.create("john_doe", "John Doe 2", "john2@example.com"));
        });
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Тест удаления пользователя")
    void testRemove() {
        userManager.add(user1);
        userManager.add(user2);

        boolean removed = userManager.remove(user1);
        assertTrue(removed);
        assertEquals(1, userManager.count());
        assertFalse(userManager.exists("john_doe"));
    }

    @Test
    @DisplayName("Тест удаления несуществующего пользователя")
    void testRemoveNonExisting() {
        boolean removed = userManager.remove(user1);
        assertFalse(removed);
    }

    @Test
    @DisplayName("Тест удаления null")
    void testRemoveNull() {
        boolean removed = userManager.remove(null);
        assertFalse(removed);
    }

    @Test
    @DisplayName("Тест поиска по ID")
    void testFindById() {
        userManager.add(user1);
        userManager.add(user2);

        Optional<User> found = userManager.findById("jane_smith");
        assertTrue(found.isPresent());
        assertEquals("jane_smith", found.get().username());

        Optional<User> notFound = userManager.findById("unknown");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Тест поиска по null ID")
    void testFindByIdNull() {
        Optional<User> result = userManager.findById(null);
        assertFalse(result.isPresent());

        result = userManager.findById("");
        assertFalse(result.isPresent());

        result = userManager.findById("   ");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Тест получения всех пользователей")
    void testFindAll() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);

        List<User> all = userManager.findAll();
        assertEquals(3, all.size());
        assertTrue(all.contains(user1));
        assertTrue(all.contains(user2));
        assertTrue(all.contains(user3));
    }

    @Test
    @DisplayName("Тест count()")
    void testCount() {
        assertEquals(0, userManager.count());

        userManager.add(user1);
        assertEquals(1, userManager.count());

        userManager.add(user2);
        assertEquals(2, userManager.count());

        userManager.remove(user1);
        assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("Тест clear()")
    void testClear() {
        userManager.add(user1);
        userManager.add(user2);
        assertEquals(2, userManager.count());

        userManager.clear();
        assertEquals(0, userManager.count());
        assertFalse(userManager.exists("john_doe"));
    }

    // ============================================================
    // ТЕСТЫ ДОПОЛНИТЕЛЬНЫХ МЕТОДОВ
    // ============================================================

    @Test
    @DisplayName("Тест findByUsername")
    void testFindByUsername() {
        userManager.add(user1);
        userManager.add(user2);

        Optional<User> found = userManager.findByUsername("jane_smith");
        assertTrue(found.isPresent());
        assertEquals("jane_smith", found.get().username());

        Optional<User> notFound = userManager.findByUsername("unknown");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Тест findByEmail")
    void testFindByEmail() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(User.create("ALICE", "Alice Wonder", "ALICE@EXAMPLE.COM"));

        Optional<User> found = userManager.findByEmail("jane@example.com");
        assertTrue(found.isPresent());
        assertEquals("jane_smith", found.get().username());

        // Тест case insensitive
        Optional<User> foundCaseInsensitive = userManager.findByEmail("ALICE@example.com");
        assertTrue(foundCaseInsensitive.isPresent());
        assertEquals("ALICE", foundCaseInsensitive.get().username());

        Optional<User> notFound = userManager.findByEmail("unknown@example.com");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Тест findByEmail с null/пустым email")
    void testFindByEmailInvalid() {
        userManager.add(user1);

        Optional<User> result = userManager.findByEmail(null);
        assertFalse(result.isPresent());

        result = userManager.findByEmail("");
        assertFalse(result.isPresent());

        result = userManager.findByEmail("   ");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Тест exists")
    void testExists() {
        userManager.add(user1);

        assertTrue(userManager.exists("john_doe"));
        assertFalse(userManager.exists("unknown"));
        assertFalse(userManager.exists(null));
        assertFalse(userManager.exists(""));
    }

    @Test
    @DisplayName("Тест update")
    void testUpdate() {
        userManager.add(user1);

        userManager.update("john_doe", "John Updated", "john.new@example.com");

        Optional<User> updated = userManager.findById("john_doe");
        assertTrue(updated.isPresent());
        assertEquals("John Updated", updated.get().fullName());
        assertEquals("john.new@example.com", updated.get().email());
    }

    @Test
    @DisplayName("Тест update только fullName")
    void testUpdateOnlyFullName() {
        userManager.add(user1);
        String originalEmail = user1.email();

        userManager.update("john_doe", "John Updated", null);

        Optional<User> updated = userManager.findById("john_doe");
        assertTrue(updated.isPresent());
        assertEquals("John Updated", updated.get().fullName());
        assertEquals(originalEmail, updated.get().email());
    }

    @Test
    @DisplayName("Тест update только email")
    void testUpdateOnlyEmail() {
        userManager.add(user1);
        String originalFullName = user1.fullName();

        userManager.update("john_doe", null, "john.new@example.com");

        Optional<User> updated = userManager.findById("john_doe");
        assertTrue(updated.isPresent());
        assertEquals(originalFullName, updated.get().fullName());
        assertEquals("john.new@example.com", updated.get().email());
    }

    @Test
    @DisplayName("Тест update несуществующего пользователя")
    void testUpdateNonExisting() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userManager.update("unknown", "New Name", "new@email.com");
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Тест update с пустым username")
    void testUpdateEmptyUsername() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userManager.update("", "New Name", "new@email.com");
        });
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    // ============================================================
    // ТЕСТЫ ФИЛЬТРАЦИИ
    // ============================================================

    @Test
    @DisplayName("Тест findByFilter с фильтром по username")
    void testFindByFilterByUsername() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);

        UserFilter filter = UserFilters.byUsername("jane_smith");
        List<User> result = userManager.findByFilter(filter);

        assertEquals(1, result.size());
        assertEquals("jane_smith", result.get(0).username());
    }

    @Test
    @DisplayName("Тест findByFilter с фильтром по домену email")
    void testFindByFilterByEmailDomain() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.add(User.create("test1", "Test One", "test1@gmail.com"));
        userManager.add(User.create("test2", "Test Two", "test2@gmail.com"));

        UserFilter filter = UserFilters.byEmailDomain("@gmail.com");
        List<User> result = userManager.findByFilter(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(u -> u.email().endsWith("@gmail.com")));
    }

    @Test
    @DisplayName("Тест findByFilter с null фильтром")
    void testFindByFilterNull() {
        userManager.add(user1);
        userManager.add(user2);

        List<User> result = userManager.findByFilter(null);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Тест findAll с фильтром и сортировкой")
    void testFindAllWithFilterAndSorter() {
        userManager.add(user1); // john_doe
        userManager.add(user2); // jane_smith
        userManager.add(User.create("adam", "Adam Adamson", "adam@example.com"));

        UserFilter filter = UserFilters.byEmailDomain("@example.com");
        List<User> result = userManager.findAll(filter, UserSorters.byUsername());

        assertEquals(3, result.size());
        assertEquals("adam", result.get(0).username()); // adam
        assertEquals("jane_smith", result.get(1).username()); // jane_smith
        assertEquals("john_doe", result.get(2).username()); // john_doe
    }

    @Test
    @DisplayName("Тест findAll с фильтром и без сортировки")
    void testFindAllWithFilterWithoutSorter() {
        userManager.add(user1);
        userManager.add(user2);

        UserFilter filter = UserFilters.byUsernameContains("j");
        List<User> result = userManager.findAll(filter, null);

        assertEquals(2, result.size());
        // Порядок может быть любым
    }

    // ============================================================
    // ТЕСТЫ EQUALS/HASHCODE/TOSTRING
    // ============================================================

    @Test
    @DisplayName("Тест equals")
    void testEquals() {
        UserManager manager1 = new UserManager();
        UserManager manager2 = new UserManager();

        assertEquals(manager1, manager2);

        manager1.add(user1);
        assertNotEquals(manager1, manager2);

        manager2.add(user1);
        assertEquals(manager1, manager2);
    }

    @Test
    @DisplayName("Тест hashCode")
    void testHashCode() {
        UserManager manager1 = new UserManager();
        UserManager manager2 = new UserManager();

        assertEquals(manager1.hashCode(), manager2.hashCode());

        manager1.add(user1);
        assertNotEquals(manager1.hashCode(), manager2.hashCode());
    }

    @Test
    @DisplayName("Тест toString")
    void testToString() {
        UserManager manager = new UserManager();
        assertEquals("UserManager{users=0}", manager.toString());

        manager.add(user1);
        assertEquals("UserManager{users=1}", manager.toString());
    }
}