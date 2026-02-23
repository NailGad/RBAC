import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ ФИЛЬТРОВ ПОЛЬЗОВАТЕЛЕЙ\n");

        List<User> users = new ArrayList<>();
        users.add(User.create("john_doe", "John Doe", "john@gmail.com"));
        users.add(User.create("jane_smith", "Jane Smith", "jane@company.com"));
        users.add(User.create("bob_johnson", "Bob Johnson", "bob@company.com"));
        users.add(User.create("alice_wonder", "Alice Wonder", "alice@gmail.com"));
        users.add(User.create("admin", "Administrator", "admin@system.com"));

        System.out.println("Все пользователи:");
        users.forEach(u -> System.out.println("  " + u.format()));
        System.out.println();

        System.out.println("1. Фильтр byUsername('admin'):");
        UserFilter filter1 = UserFilters.byUsername("admin");

        for (User u : users) {
            if (filter1.test(u)) {
                System.out.println(u.format());
            }
        }
        System.out.println();

        System.out.println("2. Фильтр byUsernameContains('john'):");
        UserFilter filter2 = UserFilters.byUsernameContains("john");
        for(User u : users)
        {
            if(filter2.test(u)) {
                System.out.println(u.format());
            }
        }
        System.out.println();

        System.out.println("3. Фильтр byEmail('jane@company.com'):");
        UserFilter filter3 = UserFilters.byEmail("jane@company.com");
        for(User u : users)
        {
            if(filter3.test(u))
            {
                System.out.println(u.format());
            }
        }
        System.out.println();

        System.out.println("4. Фильтр byEmailDomain('@company.com'):");
        UserFilter filter4 = UserFilters.byEmailDomain("@company.com");
        for(User u : users)
        {
            if(filter4.test(u))
            {
                System.out.println(u.format());
            }
        }

        System.out.println("5. Фильтр byFullNameContains('Smith'):");
        UserFilter filter5 = UserFilters.byFullNameContains("Smith");
        for(User u : users)
        {
            if(filter5.test(u))
            {
                System.out.println(u.format());
            }
        }
        System.out.println();

        System.out.println("6. Комбинация AND (byEmailDomain AND byUsernameContains):");
        System.out.println("   Пользователи с доменом @company.com И именем содержащим 'j'");
        UserFilter filter6 = UserFilters.byEmailDomain("@company.com")
                .and(UserFilters.byUsernameContains("j"));
        for(User u : users)
        {
            if(filter6.test(u))
            {
                System.out.println(u.format());
            }
        }
        System.out.println();

        System.out.println("7. Комбинация OR (byEmailDomain OR byUsernameContains):");
        System.out.println("   Пользователи с доменом @gmail.com ИЛИ именем содержащим 'admin'");
        UserFilter filter7 = UserFilters.byEmailDomain("@gmail.com")
                .or(UserFilters.byUsernameContains("admin"));
        for(User u : users)
        {
            if(filter7.test(u))
            {
                System.out.println(u.format());
            }
        }
        System.out.println();

        System.out.println("8. Сложная комбинация:");
        System.out.println("   (byEmailDomain('@company.com') AND byFullNameContains('Smith')) OR byUsername('admin')");
        UserFilter filter8 = UserFilters.byEmailDomain("@company.com")
                .and(UserFilters.byFullNameContains("Smith"))
                .or(UserFilters.byUsername("admin"));
        for(User u : users)
        {
            if(filter8.test(u))
            {
                System.out.println(u.format());
            }
        }
    }
}