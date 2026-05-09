import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


public class ManagersConcurrencyTest {

    private static final int THREADS = 32;

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    private User user1;
    private Role role1;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        user1 = User.create("john_doe", "John Doe", "john@example.com");
        userManager.add(user1);

        Permission read = new Permission("READ", "users", "Read users");
        role1 = new Role("Admin", "Administrator");
        role1.addPermission(read);
        roleManager.add(role1);
    }

    @Test
    void userManager_concurrentUniqueAdds_allPresent() throws Exception {
        int perThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CyclicBarrier barrier = new CyclicBarrier(THREADS);
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final int tid = t;
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int i = 0; i < perThread; i++) {
                        String username = "u_" + tid + "_" + i;
                        userManager.add(User.create(username, "N", username + "@x.com"));
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            shutdownExecutor(executor);
        }
        assertEquals(THREADS * perThread + 1, userManager.count()); // + seed user
    }

    @Test
    void userManager_raceDuplicateUser_onlyOneWinner() throws Exception {
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger duplicateFailures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CyclicBarrier barrier = new CyclicBarrier(THREADS);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    try {
                        User u = User.create("dup_user", "Name", "dup@x.com");
                        userManager.add(u);
                        successes.incrementAndGet();
                    } catch (IllegalArgumentException ex) {
                        duplicateFailures.incrementAndGet();
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            shutdownExecutor(executor);
        }
        assertEquals(1, successes.get());
        assertEquals(THREADS - 1, duplicateFailures.get());
        assertEquals(2, userManager.count()); // john_doe + dup_user
    }

    @Test
    void roleManager_concurrentUniqueAdds_allPresent() throws Exception {
        int perThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CyclicBarrier barrier = new CyclicBarrier(THREADS);
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final int tid = t;
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int i = 0; i < perThread; i++) {
                        String name = "R_" + tid + "_" + i;
                        roleManager.add(new Role(name, "d"));
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            shutdownExecutor(executor);
        }
        assertEquals(THREADS * perThread + 1, roleManager.count());
    }

    @Test
    void assignmentManager_raceSameUserRole_onlyOneActiveAssignment() throws Exception {
        AtomicInteger accepted = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CyclicBarrier barrier = new CyclicBarrier(THREADS);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    try {
                        AssignmentMetadata meta =
                                AssignmentMetadata.now("admin", "t-" + Thread.currentThread().getId());
                        assignmentManager.add(new PermanentAssignment(user1, role1, meta));
                        accepted.incrementAndGet();
                    } catch (IllegalStateException ex) {
                        // duplicate active user+role
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            shutdownExecutor(executor);
        }
        assertEquals(1, accepted.get());
        assertEquals(1, assignmentManager.count());
        assertEquals(1, assignmentManager.getActiveAssignments().size());
    }

    private static void shutdownExecutor(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

}
