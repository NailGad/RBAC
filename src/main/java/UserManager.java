import java.util.*;

public class UserManager implements Repository<User> {

    private final Map<String, User> usersByUsername = new HashMap<>();

    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String username = user.username();
        if (usersByUsername.containsKey(username)) {
            throw new IllegalArgumentException("User with username '" + username + "' already exists");
        }

        usersByUsername.put(username, user);
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        return usersByUsername.remove(user.username()) != null;
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByUsername.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(usersByUsername.values());
    }

    @Override
    public int count() {
        return usersByUsername.size();
    }

    @Override
    public void clear() {
        usersByUsername.clear();
    }

    public Optional<User> findByUsername(String username) {
        return findById(username);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        for (User user : usersByUsername.values()) {
            if (user.email().equalsIgnoreCase(email)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }

        List<User> result = new ArrayList<>();
        for (User user : usersByUsername.values()) {
            if (filter.test(user)) {
                result.add(user);
            }
        }
        return result;
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        List<User> result = findByFilter(filter);

        if (sorter != null) {
            result.sort(sorter);
        }

        return result;
    }

    public boolean exists(String username) {
        return username != null && usersByUsername.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        User existingUser = usersByUsername.get(username);
        if (existingUser == null) {
            throw new IllegalArgumentException("User with username '" + username + "' not found");
        }

        String fullName = newFullName != null ? newFullName : existingUser.fullName();
        String email = newEmail != null ? newEmail : existingUser.email();

        User updatedUser = User.create(username, fullName, email);
        usersByUsername.put(username, updatedUser);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return usersByUsername.equals(that.usersByUsername);
    }

    @Override
    public int hashCode() {
        return usersByUsername.hashCode();
    }

    @Override
    public String toString() {
        return String.format("UserManager{users=%d}", usersByUsername.size());
    }
}