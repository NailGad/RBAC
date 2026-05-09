import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RBACSystemExportTest {

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
    void saveDataToFile_containsUsersRolesAssignments(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("dump.txt");
        system.saveDataToFile(out.toString());

        String content = Files.readString(out);
        assertTrue(content.startsWith("# RBAC data export v1\n"));
        assertTrue(content.contains("USER"));
        assertTrue(content.contains("admin"));
        assertTrue(content.contains("ROLE"));
        assertTrue(content.contains("Admin"));
        assertTrue(content.contains("ASSIGN"));
        assertTrue(content.contains("PERMANENT") || content.contains("PERM"));
    }
}
