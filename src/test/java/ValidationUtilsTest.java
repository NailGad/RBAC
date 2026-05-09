import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilsTest {

    @Test
    void isValidUsername_valid() {
        assertTrue(ValidationUtils.isValidUsername("john_doe"));
        assertTrue(ValidationUtils.isValidUsername("ALICE_123"));
        assertTrue(ValidationUtils.isValidUsername("abc"));
    }

    @Test
    void isValidUsername_invalid() {
        assertFalse(ValidationUtils.isValidUsername(null));
        assertFalse(ValidationUtils.isValidUsername(""));
        assertFalse(ValidationUtils.isValidUsername("ab"));
        assertFalse(ValidationUtils.isValidUsername("a b"));
        assertFalse(ValidationUtils.isValidUsername("john-doe"));
        assertFalse(ValidationUtils.isValidUsername("this_username_is_way_too_long_123"));
    }

    @Test
    void isValidEmail_valid() {
        assertTrue(ValidationUtils.isValidEmail("john@example.com"));
        assertTrue(ValidationUtils.isValidEmail("ALICE@EXAMPLE.COM"));
        assertTrue(ValidationUtils.isValidEmail("a.b+c_1@sub.example.co"));
    }

    @Test
    void isValidEmail_invalid() {
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail("no-at-symbol"));
        assertFalse(ValidationUtils.isValidEmail("a@b"));
        assertFalse(ValidationUtils.isValidEmail("a@b."));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
    }

    @Test
    void isValidDate_valid() {
        assertTrue(ValidationUtils.isValidDate("2026-01-01 00:00"));
        assertTrue(ValidationUtils.isValidDate(" 2026-12-31 23:59 "));
    }

    @Test
    void isValidDate_invalid() {
        assertFalse(ValidationUtils.isValidDate(null));
        assertFalse(ValidationUtils.isValidDate(""));
        assertFalse(ValidationUtils.isValidDate("2026-1-1 0:0"));
        assertFalse(ValidationUtils.isValidDate("2026-01-01"));
        assertFalse(ValidationUtils.isValidDate("2026-13-01 00:00"));
        assertFalse(ValidationUtils.isValidDate("2026-02-30 00:00"));
    }

    @Test
    void normalizeString_trimsAndCollapsesWhitespace() {
        assertNull(ValidationUtils.normalizeString(null));
        assertEquals("John Doe", ValidationUtils.normalizeString("  John   Doe  "));
        assertEquals("a@b.com", ValidationUtils.normalizeString(" A@B.COM "));
    }

    @Test
    void requireNonEmpty_throwsOnBlank() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonEmpty(null, "field"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonEmpty("   ", "field"));
    }
}

