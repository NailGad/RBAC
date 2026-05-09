import java.util.List;
import java.util.Scanner;

public final class ConsoleUtils {

    private ConsoleUtils() {}

    public static String promptString(Scanner scanner, String message, boolean required) {
        if (scanner == null) throw new IllegalArgumentException("scanner cannot be null");
        if (message == null) message = "";

        while (true) {
            if (!message.isBlank()) {
                System.out.print(message);
            }
            String input = scanner.nextLine();
            input = ValidationUtils.normalizeString(input);

            if (!required) {
                return input == null ? "" : input;
            }

            if (input != null && !input.isBlank()) {
                return input;
            }
            System.out.println("Ошибка: значение обязательно. Попробуйте ещё раз.");
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        if (scanner == null) throw new IllegalArgumentException("scanner cannot be null");
        if (min > max) throw new IllegalArgumentException("min cannot be greater than max");
        if (message == null) message = "";

        while (true) {
            if (!message.isBlank()) {
                System.out.print(message);
            }
            String raw = scanner.nextLine();
            raw = ValidationUtils.normalizeString(raw);

            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    System.out.printf("Ошибка: введите число в диапазоне %d..%d%n", min, max);
                    continue;
                }
                return value;
            } catch (Exception e) {
                System.out.println("Ошибка: введите корректное целое число.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        if (scanner == null) throw new IllegalArgumentException("scanner cannot be null");
        if (message == null) message = "";

        while (true) {
            if (!message.isBlank()) {
                System.out.print(message);
            }
            String input = scanner.nextLine();
            input = input == null ? "" : input.trim().toLowerCase();

            if (input.equals("y") || input.equals("yes") || input.equals("да") || input.equals("д")) {
                return true;
            }
            if (input.equals("n") || input.equals("no") || input.equals("нет") || input.equals("н")) {
                return false;
            }
            System.out.println("Ошибка: введите yes/no (y/n).");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (scanner == null) throw new IllegalArgumentException("scanner cannot be null");
        if (options == null || options.isEmpty()) throw new IllegalArgumentException("options cannot be empty");
        if (message == null) message = "";

        System.out.println(message);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, String.valueOf(options.get(i)));
        }

        int idx = promptInt(scanner, "Choose number: ", 1, options.size()) - 1;
        return options.get(idx);
    }
}

