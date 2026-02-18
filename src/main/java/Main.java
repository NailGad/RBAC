import java.util.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("1. Создание ролей:");

        Permission readUsers = new Permission("READ", "users", "Can read users");
        Permission writeUsers = new Permission("WRITE", "users", "Can write users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");

        Role admin = new Role("Administrator", "Full system access");
        System.out.println(admin.format());


        System.out.println("2. Добавление прав:");
        admin.addPermission(readUsers);
        admin.addPermission(writeUsers);
        admin.addPermission(deleteUsers);

        System.out.println(admin.format());



        System.out.println("3. Проверка наличия прав:");
        System.out.println("hasPermission(readUsers): " +
                admin.hasPermission(readUsers));
        System.out.println("hasPermission('READ', 'users'): " +
                admin.hasPermission("READ", "users"));
        System.out.println("hasPermission('WRITE', 'reports'): " +
                admin.hasPermission("WRITE", "reports"));
        System.out.println();


        System.out.println("4. Удаление права:");
        admin.removePermission(writeUsers);
        System.out.println("После удаления WRITE права:");
        System.out.println(admin.format());


        System.out.println("5. Неизменяемая копия прав:");
        Set<Permission> perms = admin.getPermissions();
        System.out.println("Права администратора (копия):");
        for (Permission p : perms) {
            System.out.println("  - " + p.format());
        }
        System.out.println();


        System.out.println("6. Сравнение ролей:");
        Role role1 = new Role("Manager", "Manager role");
        Role role2 = new Role("Manager", "Manager role");
        System.out.println("role1.equals(role2): " + role1.equals(role2));
        System.out.println("role1.toString(): " + role1);
        System.out.println("role2.toString(): " + role2);
    }
}