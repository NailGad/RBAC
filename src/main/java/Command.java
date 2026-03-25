@FunctionalInterface
public interface Command {
    void execute(Scanner scanner, RBACSystem system);
}