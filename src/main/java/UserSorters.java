import java.util.Comparator;

public class UserSorters {

    public static Comparator<User> byUsername() {
        return (u1, u2) -> u1.username().compareToIgnoreCase(u2.username());
    }

    public static Comparator<User> byFullName() {
        return (u1, u2) -> u1.fullName().compareToIgnoreCase(u2.fullName());
    }

    public static Comparator<User> byEmail() {
        return (u1, u2) -> u1.email().compareToIgnoreCase(u2.email());
    }
}