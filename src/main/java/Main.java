import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {

        System.out.println("1. Создание через конструктор:");
        AssignmentMetadata meta1 = new AssignmentMetadata(
                "admin",
                "2026-01-15 10:30",
                "Initial setup"
        );
        System.out.println(meta1.format());
        System.out.println("  assignedBy: " + meta1.assignedBy());
        System.out.println("  assignedAt: " + meta1.assignedAt());
        System.out.println("  reason: " + meta1.reason());
        System.out.println();

        System.out.println("2. Создание через now():");
        AssignmentMetadata meta2 = AssignmentMetadata.now("john_doe", "Project start");
        System.out.println(meta2.format());
        System.out.println("  Текущая дата/время установлена автоматически");
        System.out.println();


        System.out.println("3. Без причины:");
        AssignmentMetadata meta3 = AssignmentMetadata.now("manager", null);
        System.out.println(meta3.format());
        System.out.println("  reason: " + meta3.reason() );
        System.out.println();


        System.out.println("4. Пустая причина:");
        AssignmentMetadata meta4 = new AssignmentMetadata(
                "admin",
                "2026-02-20 15:45",
                ""
        );
        System.out.println(meta4.format());
        System.out.println("  reason: " + meta4.reason());
        System.out.println();


        System.out.println("5. Ошибки валидации:");
        try {
            AssignmentMetadata m = new AssignmentMetadata("", "2024-01-01", "test");
            System.out.println("Должно было упасть");
        } catch (IllegalArgumentException e) {
            System.out.println("assignedBy пустой: " + e.getMessage());
        }

        // Ошибка: assignedAt пустой
        try {
            AssignmentMetadata m = new AssignmentMetadata("admin", "", "test");
            System.out.println("Должно было упасть");
        } catch (IllegalArgumentException e) {
            System.out.println("assignedAt пустой: " + e.getMessage());
        }

        // Ошибка: assignedBy = null
        try {
            AssignmentMetadata m = new AssignmentMetadata(null, "2024-01-01", "test");
            System.out.println("Должно было упасть");
        } catch (IllegalArgumentException e) {
            System.out.println("assignedBy = null: " + e.getMessage());
        }
        System.out.println();
    }
}