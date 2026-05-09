import java.util.*;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {

    private final Object lock = new Object();
    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    public RoleManager() {
    }

    @Override
    public void add(Role role) {
        synchronized (lock) {
            if (role == null) {
                throw new IllegalArgumentException("Role cannot be null");
            }

            String roleName = role.getName();
            if (rolesByName.containsKey(roleName)) {
                throw new IllegalArgumentException("Role with name '" + roleName + "' already exists");
            }

            rolesById.put(role.getId(), role);
            rolesByName.put(roleName, role);
        }
    }

    @Override
    public boolean remove(Role role) {
        synchronized (lock) {
            if (role == null) {
                return false;
            }

            boolean removed = rolesById.remove(role.getId()) != null;
            if (removed) {
                rolesByName.remove(role.getName());
            }
            return removed;
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        synchronized (lock) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(rolesById.get(id));
        }
    }

    @Override
    public List<Role> findAll() {
        synchronized (lock) {
            return new ArrayList<>(rolesById.values());
        }
    }

    @Override
    public int count() {
        synchronized (lock) {
            return rolesById.size();
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            rolesById.clear();
            rolesByName.clear();
        }
    }

    public Optional<Role> findByName(String name) {
        synchronized (lock) {
            if (name == null || name.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(rolesByName.get(name));
        }
    }

    public List<Role> findByFilter(RoleFilter filter) {
        synchronized (lock) {
            if (filter == null) {
                return findAllLocked();
            }

            List<Role> result = new ArrayList<>();
            for (Role role : rolesById.values()) {
                if (filter.test(role)) {
                    result.add(role);
                }
            }
            return result;
        }
    }

    private List<Role> findAllLocked() {
        return new ArrayList<>(rolesById.values());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        List<Role> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        List<Role> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(rolesById.values());
        }
        if (filter == null) {
            return snapshot;
        }
        return snapshot.parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        synchronized (lock) {
            return name != null && rolesByName.containsKey(name);
        }
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        synchronized (lock) {
            if (roleName == null || roleName.isBlank()) {
                throw new IllegalArgumentException("Role name cannot be empty");
            }
            if (permission == null) {
                throw new IllegalArgumentException("Permission cannot be null");
            }

            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Role with name '" + roleName + "' not found");
            }

            role.addPermission(permission);
        }
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        synchronized (lock) {
            if (roleName == null || roleName.isBlank()) {
                throw new IllegalArgumentException("Role name cannot be empty");
            }
            if (permission == null) {
                throw new IllegalArgumentException("Permission cannot be null");
            }

            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Role with name '" + roleName + "' not found");
            }

            role.removePermission(permission);
        }
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        synchronized (lock) {
            if (permissionName == null || resource == null) {
                return new ArrayList<>();
            }

            List<Role> result = new ArrayList<>();
            for (Role role : rolesById.values()) {
                if (role.hasPermission(permissionName, resource)) {
                    result.add(role);
                }
            }
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleManager that = (RoleManager) o;
        Map<String, Role> thisSnapshot;
        Map<String, Role> thatSnapshot;
        synchronized (lock) {
            thisSnapshot = new HashMap<>(rolesById);
        }
        synchronized (that.lock) {
            thatSnapshot = new HashMap<>(that.rolesById);
        }
        return thisSnapshot.equals(thatSnapshot);
    }

    @Override
    public int hashCode() {
        synchronized (lock) {
            return rolesById.hashCode();
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("RoleManager{roles=%d}", rolesById.size());
        }
    }
}
