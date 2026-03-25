import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        commands.put(name.toLowerCase(), command);
        commandDescriptions.put(name.toLowerCase(), description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command command = commands.get(commandName.toLowerCase());
        if (command == null) {
            System.out.println("Unknown command: " + commandName);
            System.out.println("Type 'help' to see available commands.");
            return;
        }
        try {
            command.execute(scanner, system);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }

    public void printHelp() {
        System.out.println("\n=== Available Commands ===");
        commands.keySet().stream().sorted().forEach(name -> {
            System.out.printf("  %-25s - %s\n", name, commandDescriptions.get(name));
        });
        System.out.println();
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isBlank()) {
            return;
        }

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        executeCommand(commandName, scanner, system);
    }

    public Map<String, Command> getCommands() {
        return commands;
    }
}