public class Main {
    public static void main(String[] args) {

        System.out.println("1. Успешное создание:");
        Permission p1 = new Permission("read", "users", "Can read users");
        System.out.println(p1.format());
        System.out.println("  name: " + p1.name() + " (авто upper case)");
        System.out.println("  resource: " + p1.resource() + " (авто lower case)\n");

        Permission p2 = new Permission("  delete  ", "  FILES  ", "  Can delete files  ");
        System.out.println(p2.format() + " (пробелы обрезаны)");
        System.out.println("  name: '" + p2.name() + "'");
        System.out.println("  resource: '" + p2.resource() + "'\n");


        System.out.println("2. Ошибки валидации:");
        try {
            Permission p = new Permission("READ USERS", "users", "desc");
        } catch (IllegalArgumentException e) {
            System.out.println("Name с пробелом: " + e.getMessage());
        }

        try {
            Permission p = new Permission("READ", "", "desc");
        } catch (IllegalArgumentException e) {
            System.out.println("Resource пустой: " + e.getMessage());
        }

        try {
            Permission p = new Permission("READ", "users", "");
        } catch (IllegalArgumentException e) {
            System.out.println("Description пустой: " + e.getMessage());
        }
        System.out.println();



        System.out.println("3. Тест matches():");
        Permission test = new Permission("EXECUTE", "scripts", "Run scripts");
        System.out.println("Тестовое право: " + test.format());
        System.out.println("  matches('EXEC', null): " + test.matches("EXEC", null));
        System.out.println("  matches(null, 'script'): " + test.matches(null, "script"));
        System.out.println("  matches('EXEC', 'script'): " + test.matches("EXEC", "script"));
        System.out.println("  matches('READ', 'script'): " + test.matches("READ", "script"));
        System.out.println();


        System.out.println("4. Поиск прав:");
        Permission[] perms = {
                new Permission("READ", "users", "Read users"),
                new Permission("WRITE", "users", "Write users"),
                new Permission("READ", "reports", "Read reports")
        };

        System.out.println("Все права на users:");
        for (Permission p : perms) {
            if (p.matches(null, "users")) {
                System.out.println(p.format());
            }
        }
    }
}