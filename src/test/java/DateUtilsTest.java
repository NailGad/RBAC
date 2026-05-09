import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilsTest {

    @Test
    void getCurrentDate_hasExpectedFormat() {
        String d = DateUtils.getCurrentDate();
        assertTrue(d.matches("^\\d{4}-\\d{2}-\\d{2}$"));
    }

    @Test
    void getCurrentDateTime_hasExpectedFormat() {
        String dt = DateUtils.getCurrentDateTime();
        assertTrue(dt.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"));
    }

    @Test
    void isBefore_isAfter_workForDateOnly() {
        assertTrue(DateUtils.isBefore("2026-01-01", "2026-01-02"));
        assertTrue(DateUtils.isAfter("2026-01-02", "2026-01-01"));
        assertFalse(DateUtils.isBefore("2026-01-02", "2026-01-01"));
        assertFalse(DateUtils.isAfter("2026-01-01", "2026-01-02"));
    }

    @Test
    void isBefore_isAfter_acceptProjectDateTimeFormat() {
        assertTrue(DateUtils.isBefore("2026-01-01 00:00", "2026-01-01 00:01"));
        assertTrue(DateUtils.isAfter("2026-01-01 00:02", "2026-01-01 00:01"));
    }

    @Test
    void addDays_worksAndPreservesInputKind() {
        assertEquals("2026-01-11", DateUtils.addDays("2026-01-01", 10));
        assertEquals("2026-01-11 00:00", DateUtils.addDays("2026-01-01 00:00", 10));
    }

    @Test
    void addDays_throwsOnInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.addDays("bad", 1));
        assertThrows(IllegalArgumentException.class, () -> DateUtils.addDays("2026-02-30", 1));
        assertThrows(IllegalArgumentException.class, () -> DateUtils.addDays("2026-13-01 00:00", 1));
    }

    @Test
    void formatRelativeTime_returnsSomethingMeaningful() {
        assertEquals("unknown", DateUtils.formatRelativeTime("bad"));
        assertNotNull(DateUtils.formatRelativeTime(DateUtils.getCurrentDate()));
    }
}

