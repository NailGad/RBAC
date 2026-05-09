import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RBACMaintenanceSchedulerTest {

    private RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.shutdown();
        }
    }

    @Test
    void runMaintenanceTick_appendsSchedulerStatsToAuditLog() {
        system.runMaintenanceTick();
        assertFalse(system.getAuditLog().getAll().isEmpty());
        assertEquals(1, system.getAuditLog().getByAction("SCHEDULER_STATS").size());
        var entry = system.getAuditLog().getByAction("SCHEDULER_STATS").get(0);
        assertTrue(entry.details().contains("deactivated_temp="));
        assertTrue(entry.details().contains("users="));
    }

    @Test
    void startAndStopScheduler_doesNotThrow() {
        system.startMaintenanceScheduler(3600);
        system.stopMaintenanceScheduler();
    }
}
