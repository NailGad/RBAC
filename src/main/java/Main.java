public class Main {
    public static void main(String[] args) {
        System.out.println("=== Создание пользователей ===\n");

        try {
            User user1 = User.create("john_doe", "John Doe", "john.doe@example.com");
            System.out.println("Успешно: " + user1.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        try {
            User user2 = User.create("jo", "John Doe", "john@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (короткий username): " + e.getMessage());
        }

        try {
            User user3 = User.create("john@doe", "John Doe", "john@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (спецсимволы в username): " + e.getMessage());
        }

        try {
            User user4 = User.create("jane_doe", "Jane Doe", "jane.example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (email без @): " + e.getMessage());
        }

        try {
            User user5 = User.create("bob_smith", "Bob Smith", "bob@example");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (email без точки после @): " + e.getMessage());
        }
    }
}