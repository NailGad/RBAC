import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandRegistry registry = new CommandRegistry(system);
        CommandParser parser = registry.getParser();

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== RBAC System ===");
        System.out.println("Type 'help' for available commands.");

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            parser.parseAndExecute(input, scanner, system);
        }
    }
}