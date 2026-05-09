import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLogTest {

    @Test
    void log_addsEntryAndNormalizes() {
        AuditLog log = new AuditLog();

        log.log("user_create", " Admin  ", " john_doe ", "  email=JOHN@EXAMPLE.COM  ");

        List<AuditLog.AuditEntry> all = log.getAll();
        assertEquals(1, all.size());
        AuditLog.AuditEntry e = all.get(0);
        assertEquals("USER_CREATE", e.action());
        assertEquals("Admin", e.performer());
        assertEquals("john_doe", e.target());
        assertEquals("email=john@example.com", e.details());
        assertNotNull(e.timestamp());
        assertFalse(e.timestamp().isBlank());
    }

    @Test
    void getByPerformer_filtersCaseInsensitive() {
        AuditLog log = new AuditLog();
        log.log("A", "admin", "t1", "");
        log.log("B", "system", "t2", "");
        log.log("C", "ADMIN", "t3", "");

        assertEquals(2, log.getByPerformer("AdMiN").size());
        assertEquals(1, log.getByPerformer("system").size());
        assertEquals(0, log.getByPerformer("unknown").size());
    }

    @Test
    void getByAction_filtersCaseInsensitive() {
        AuditLog log = new AuditLog();
        log.log("role_assign", "admin", "t1", "");
        log.log("ROLE_ASSIGN", "admin", "t2", "");
        log.log("role_revoke", "admin", "t3", "");

        assertEquals(2, log.getByAction("role_assign").size());
        assertEquals(1, log.getByAction("ROLE_REVOKE").size());
        assertEquals(0, log.getByAction("missing").size());
    }

    @Test
    void saveToFile_writesCsv(@TempDir Path tempDir) throws Exception {
        AuditLog log = new AuditLog();
        log.log("USER_CREATE", "admin", "john", "email=john@example.com");

        Path out = tempDir.resolve("audit.csv");
        log.saveToFile(out.toString());

        String content = Files.readString(out);
        assertTrue(content.startsWith("timestamp,action,performer,target,details\n"));
        assertTrue(content.contains(",USER_CREATE,"));
        assertTrue(content.contains(",admin,"));
        assertTrue(content.contains(",john,"));
        assertTrue(content.contains(",email=john@example.com\n"));
    }
}

