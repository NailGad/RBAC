import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ ФИЛЬТРОВ РОЛЕЙ\n");

        // Создаем права
        Permission readUsers = new Permission("READ", "users", "Can read users");
        Permission writeUsers = new Permission("WRITE", "users", "Can write users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");
        Permission readReports = new Permission("READ", "reports", "Can read reports");
        Permission writeReports = new Permission("WRITE", "reports", "Can write reports");

        // Создаем тестовые роли
        List<Role> roles = new ArrayList<>();

        Role admin = new Role("Administrator", "Full system access");
        admin.addPermission(readUsers);
        admin.addPermission(writeUsers);
        admin.addPermission(deleteUsers);
        admin.addPermission(readReports);
        admin.addPermission(writeReports);
        roles.add(admin);

        Role editor = new Role("Editor", "Can edit content");
        editor.addPermission(readUsers);
        editor.addPermission(writeUsers);
        editor.addPermission(readReports);
        editor.addPermission(writeReports);
        roles.add(editor);

        Role viewer = new Role("Viewer", "Can view only");
        viewer.addPermission(readUsers);
        viewer.addPermission(readReports);
        roles.add(viewer);

        Role moderator = new Role("Moderator", "Can moderate users");
        moderator.addPermission(readUsers);
        moderator.addPermission(writeUsers);
        roles.add(moderator);

        Role guest = new Role("Guest", "Limited access");
        guest.addPermission(readReports);
        roles.add(guest);

        System.out.println("Все роли:");
        for (Role r : roles) {
            System.out.println("  " + r.getName() + " - " + r.getDescription());
        }
        System.out.println();

        System.out.println("1. Фильтр byName('Administrator'):");
        RoleFilter filter1 = RoleFilters.byName("Administrator");
        for (Role r : roles) {
            if (filter1.test(r)) {
                System.out.println(r.getName());
            }
        }
        System.out.println();

        // Тест 2: Фильтр по части имени
        System.out.println("2. Фильтр byNameContains('view'):");
        RoleFilter filter2 = RoleFilters.byNameContains("view");
        for (Role r : roles) {
            if (filter2.test(r)) {
                System.out.println(r.getName());
            }
        }
        System.out.println();

        System.out.println("3. Фильтр hasPermission(deleteUsers):");
        RoleFilter filter3 = RoleFilters.hasPermission(deleteUsers);
        for (Role r : roles) {
            if (filter3.test(r)) {
                System.out.println(r.getName());
            }
        }
        System.out.println();

        System.out.println("4. Фильтр hasPermission('DELETE', 'users'):");
        RoleFilter filter4 = RoleFilters.hasPermission("DELETE", "users");
        for (Role r : roles) {
            if (filter4.test(r)) {
                System.out.println(r.getName());
            }
        }
        System.out.println();

        System.out.println("5. Фильтр hasAtLeastNPermissions(4):");
        RoleFilter filter5 = RoleFilters.hasAtLeastNPermissions(4);
        for (Role r : roles) {
            if (filter5.test(r)) {
                System.out.println(r.getName() + " (прав: " + r.getPermissions().size() + ")");
            }
        }
        System.out.println();

        System.out.println("6. Фильтр hasAtLeastNPermissions(1):");
        RoleFilter filter6 = RoleFilters.hasAtLeastNPermissions(1);
        for (Role r : roles) {
            if (filter6.test(r)) {
                System.out.println(r.getName() + " (прав: " + r.getPermissions().size() + ")");
            }
        }
        System.out.println();

        // Тест 7: Комбинация AND
        System.out.println("7. Комбинация AND (byNameContains AND hasAtLeastNPermissions):");
        System.out.println("   Роли с именем содержащим 'e' И минимум 3 права");
        RoleFilter filter7 = RoleFilters.byNameContains("e")
                .and(RoleFilters.hasAtLeastNPermissions(3));
        for (Role r : roles) {
            if (filter7.test(r)) {
                System.out.println(r.getName() + " (прав: " + r.getPermissions().size() + ")");
            }
        }
        System.out.println();

        // Тест 8: Комбинация OR
        System.out.println("8. Комбинация OR (hasPermission OR byNameContains):");
        System.out.println("   Роли с правом DELETE на users ИЛИ именем содержащим 'view'");
        RoleFilter filter8 = RoleFilters.hasPermission("DELETE", "users")
                .or(RoleFilters.byNameContains("view"));
        for (Role r : roles) {
            if (filter8.test(r)) {
                System.out.println(r.getName());
            }
        }
        System.out.println();

        // Тест 9: Сложная комбинация
        System.out.println("9. Сложная комбинация:");
        System.out.println("   (byNameContains('e') AND hasPermission('READ', 'users')) OR hasAtLeastNPermissions(5)");
        RoleFilter filter9 = RoleFilters.byNameContains("e")
                .and(RoleFilters.hasPermission("READ", "users"))
                .or(RoleFilters.hasAtLeastNPermissions(5));
        for (Role r : roles) {
            if (filter9.test(r)) {
                System.out.println(r.getName());
            }
        }
    }
}